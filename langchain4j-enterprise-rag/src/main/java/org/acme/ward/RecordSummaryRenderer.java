package org.acme.ward;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Headers;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;

/**
 * Turns one patient's FHIR record into the second kind of document: who they are, what
 * they have, what they take, what they react to. The typed HAPI FHIR model does the
 * reading; the display labels the standards carry make the text readable.
 *
 * The record is written as one fact per sentence, every sentence naming the patient
 * ("Marie Dupont has chronic congestive heart failure (since 2024)."). Two measured
 * reasons. Retrieval: a single fact drowns in a long comma-separated list (the one
 * heart-failure patient ranked 15th of 40 for "heart failure"); as its own sentence it
 * ranks first. Attribution: the splitter cuts long records into several segments, and
 * a segment of an anonymous list loses its patient, while a segment of these sentences
 * stays self-contained.
 *
 * The document id is {@code <patient>@<last-updated>}: an unchanged record keeps its id
 * and the ingest engine skips it, a changed record gets a new id and re-ingests. The
 * deduplication mechanism is the change detection.
 */
@ApplicationScoped
public class RecordSummaryRenderer {

    static final FhirContext FHIR = FhirContext.forR4Cached();

    @Inject
    ProducerTemplate producer;

    @ConfigProperty(name = "ward.fhir.url")
    String fhirUrl;

    public String render(Patient patient, @Headers Map<String, Object> headers) {
        String id = patient.getIdElement().getIdPart();
        List<Condition> conditions = searchByPatient(Condition.class, "Condition", id);
        List<MedicationRequest> medications = searchByPatient(MedicationRequest.class, "MedicationRequest", id);
        List<AllergyIntolerance> allergies = searchByPatient(AllergyIntolerance.class, "AllergyIntolerance", id);

        Date lastUpdated = lastUpdated(patient, conditions, medications, allergies);
        headers.put(WardRoutes.DOCUMENT_ID_HEADER, id + "@" + lastUpdated.toInstant());

        // Synthea appends digits to every name (Cristobal567 Montero62): strip them, or
        // the assistant answers with debug-looking names
        String given = patient.getNameFirstRep().getGivenAsSingleString().replaceAll("\\d", "");
        String family = patient.getNameFirstRep().getFamily().replaceAll("\\d", "");
        String subject = (given.isEmpty() ? family : given.split(" ")[0] + " " + family);

        StringBuilder text = new StringBuilder();
        text.append("Patient ").append(given)
                .append(' ').append(family)
                .append(" (id ").append(id)
                .append(", born ").append(new SimpleDateFormat("yyyy-MM-dd").format(patient.getBirthDate()))
                .append(", sex ").append(patient.getGender().getDisplay())
                .append(").\n");

        facts(text, subject, " has ", " has no active conditions on record", conditions.stream()
                .filter(c -> "active".equals(c.getClinicalStatus().getCodingFirstRep().getCode()))
                .map(c -> label(c.getCode().getCodingFirstRep().getDisplay())
                        + (c.hasRecordedDate() ? " (since " + (c.getRecordedDate().getYear() + 1900) + ")" : ""))
                .toList());

        facts(text, subject, " takes ", " takes no medications on record", medications.stream()
                .filter(m -> m.getStatus() == MedicationRequest.MedicationRequestStatus.ACTIVE)
                .map(m -> m.getMedicationCodeableConcept().getCodingFirstRep().getDisplay())
                .toList());

        facts(text, subject, " is allergic to ", " has no allergies on record", allergies.stream()
                .map(a -> label(a.getCode().getCodingFirstRep().getDisplay()))
                .toList());

        return text.toString();
    }

    /** One sentence per fact, each naming the patient: "Marie Dupont has anemia (since 2013)." */
    private static void facts(StringBuilder text, String subject, String verb, String none, List<String> items) {
        if (items.isEmpty()) {
            text.append(subject).append(none).append(".\n");
            return;
        }
        for (String item : items) {
            text.append(subject).append(verb).append(item).append(".\n");
        }
    }

    /** "Chronic congestive heart failure (disorder)" reads better without the SNOMED hint. */
    private static String label(String display) {
        return display.replaceAll("\\s*\\((disorder|finding|situation|person|organism|substance)\\)$", "");
    }

    private <T extends IBaseResource> List<T> searchByPatient(Class<T> type, String resource, String patientId) {
        IBaseBundle bundle = producer.requestBodyAndHeader(
                "fhir://search/searchByUrl?serverUrl=" + fhirUrl + "&fhirVersion=R4",
                null, "CamelFhir.url", resource + "?patient=" + patientId + "&_count=200", IBaseBundle.class);
        return BundleUtil.toListOfResourcesOfType(FHIR, bundle, type);
    }

    private static Date lastUpdated(Patient patient, List<?>... resourceLists) {
        Date latest = patient.getMeta().getLastUpdated();
        for (List<?> resources : resourceLists) {
            for (Object resource : resources) {
                Date candidate = ((IBaseResource) resource).getMeta().getLastUpdated();
                if (candidate != null && (latest == null || candidate.after(latest))) {
                    latest = candidate;
                }
            }
        }
        return latest == null ? new Date(0) : latest;
    }
}
