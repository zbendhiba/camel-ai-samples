package org.acme.ward;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the EXISTING patient-records route in isolation: AdviceWith swaps the patient
 * search for a canned result and the ingest pipeline for a mock; the page walking, the
 * record rendering and the document id run for real. The renderer's lookups go to the
 * Dev Service FHIR server, which knows nothing about this patient, so the record reads
 * "none on record" everywhere.
 */
@QuarkusTest
@WithTestResource(FreeFeedPortTestResource.class)
public class RecordsRouteTest extends CamelQuarkusTestSupport {

    @BeforeEach
    void advisePatientRecords() throws Exception {
        // the one-shot bootstrap runs in the background: take its trigger away, or a
        // late bootstrap could send a second, real load through the advised route
        AdviceWith.adviceWith(this.context, "ward-bootstrap", route ->
                route.replaceFromWith("direct:bootstrap-off"));
        AdviceWith.adviceWith(this.context, "patient-records", route -> {
            route.weaveByToUri("fhir://search*").replace()
                    .process(e -> e.getMessage().setBody(searchResultWith()));
            route.weaveByToUri("direct:records-ingest").replace().to("mock:records-ingested");
        });
    }

    @Test
    void aPatientBecomesARecordSummary() throws Exception {
        MockEndpoint ingested = getMockEndpoint("mock:records-ingested");
        ingested.expectedMessageCount(1);
        ingested.message(0).header(WardRoutes.DOCUMENT_ID_HEADER).startsWith("TEST-1@");
        ingested.message(0).body(String.class)
                .contains("Patient Marie Dupont (id TEST-1, born 1956-03-12, sex Female).");
        ingested.message(0).body(String.class).contains("Marie Dupont has no active conditions on record.");
        ingested.message(0).body(String.class).contains("Marie Dupont has no allergies on record.");

        template.sendBody("direct:records-initial-load", null);
        ingested.assertIsSatisfied(30000);
    }

    /** What the patient search would return: one page, one patient, no next link. */
    static Bundle searchResultWith() {
        Patient patient = new Patient();
        patient.setId("Patient/TEST-1");
        patient.addName().setFamily("Dupont").addGiven("Marie");
        patient.setBirthDateElement(new DateType("1956-03-12"));
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);

        Bundle result = new Bundle();
        result.setType(Bundle.BundleType.SEARCHSET);
        result.addEntry().setResource(patient);
        return result;
    }
}
