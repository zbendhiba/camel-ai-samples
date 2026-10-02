package org.acme.ward;

import ca.uhn.fhir.util.BundleUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Patient;

/**
 * The records route: the fhir consumer polls the server for patients, and each record
 * becomes one summary document. Change detection is free: an unchanged record keeps its
 * {@code <patient>@<last-updated>} id and the ingest engine's deduplication skips it;
 * only new and changed records are re-embedded.
 */
@ApplicationScoped
public class RecordsRoutes extends RouteBuilder {

    private final RecordSummaryRenderer renderer;
    private final WardLivePublisher livePublisher;

    RecordsRoutes(RecordSummaryRenderer renderer, WardLivePublisher livePublisher) {
        this.renderer = renderer;
        this.livePublisher = livePublisher;
    }

    @Override
    public void configure() {
        from("fhir://search/searchByUrl?url=Patient&serverUrl={{ward.fhir.url}}&fhirVersion=R4"
                + "&delay={{ward.records.poll-interval}}&startScheduler=true&sendEmptyMessageWhenIdle=false")
                .routeId("patient-records")
                .process(e -> e.getMessage().setBody(
                        BundleUtil.toListOfResourcesOfType(RecordSummaryRenderer.FHIR,
                                e.getMessage().getBody(IBaseBundle.class), Patient.class)))
                .split(body())
                .bean(renderer)
                .setVariable("summary", body())
                .to("direct:records-ingest")
                .bean(livePublisher,
                        "publish(${variable.summary}, ${header." + WardRoutes.DOCUMENT_ID_HEADER + "}, ${body})")
                .log("Record ${header." + WardRoutes.DOCUMENT_ID_HEADER + "}: ${body}");
    }
}
