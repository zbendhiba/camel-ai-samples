package org.acme.ward;

import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.util.BundleUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Patient;

/**
 * The records route: the initial load of the knowledge base. A one-shot bootstrap
 * route seeds the FHIR server, then runs one full scan: every patient record becomes
 * one summary document, id {@code <patient>@<last-updated>}.
 *
 * This example stops there, deliberately. In production the full scan runs once, and
 * staying in sync afterwards is change tracking, not re-scanning: see "Keeping the
 * records in sync" in the README for the Camel options (polling {@code _history}
 * with a persisted cutoff, FHIR Subscriptions, or CDC on the record system's
 * database).
 */
@ApplicationScoped
public class RecordsRoutes extends RouteBuilder {

    private final FhirSeeder seeder;
    private final RecordSummaryRenderer renderer;
    private final WardLivePublisher livePublisher;
    private final ProducerTemplate producer;

    RecordsRoutes(FhirSeeder seeder, RecordSummaryRenderer renderer, WardLivePublisher livePublisher,
            ProducerTemplate producer) {
        this.seeder = seeder;
        this.renderer = renderer;
        this.livePublisher = livePublisher;
        this.producer = producer;
    }

    @Override
    public void configure() {
        // the demo bootstrap, in order: wait for the record system, seed it, load it.
        // repeatCount=1 fires exactly once; the seeder does its own waiting, so no
        // guessed delay is needed
        from("timer:bootstrap?repeatCount=1")
                .routeId("ward-bootstrap")
                .bean(seeder, "seed")
                .to("direct:records-initial-load");

        from("direct:records-initial-load")
                .routeId("patient-records")
                .to("fhir://search/searchByUrl?url=Patient&serverUrl={{ward.fhir.url}}&fhirVersion=R4")
                .process(this::collectAllPatients)
                .split(body())
                .bean(renderer)
                .setVariable("summary", body())
                .to("direct:records-ingest")
                .bean(livePublisher,
                        "publish(${variable.summary}, ${header." + WardRoutes.DOCUMENT_ID_HEADER + "}, ${body})")
                .log("Record ${header." + WardRoutes.DOCUMENT_ID_HEADER + "}: ${body}");
    }

    /** FHIR servers page their search results: walk the pages, collect every patient. */
    private void collectAllPatients(Exchange exchange) {
        List<Patient> patients = new ArrayList<>();
        IBaseBundle page = exchange.getMessage().getBody(IBaseBundle.class);
        while (page != null) {
            patients.addAll(BundleUtil.toListOfResourcesOfType(RecordSummaryRenderer.FHIR, page, Patient.class));
            page = BundleUtil.getLinkUrlOfType(RecordSummaryRenderer.FHIR, page, "next") == null ? null
                    : producer.requestBodyAndHeader(
                            "fhir://load-page/next?serverUrl={{ward.fhir.url}}&fhirVersion=R4",
                            null, "CamelFhir.bundle", page, IBaseBundle.class);
        }
        exchange.getMessage().setBody(patients);
    }
}
