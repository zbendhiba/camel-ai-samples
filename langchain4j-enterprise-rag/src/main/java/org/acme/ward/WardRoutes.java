package org.acme.ward;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.component.hl7.HL7.hl7terser;

/**
 * The hospital feed. HL7v2 messages (ADT admissions, ORU lab results) arrive over MLLP,
 * are parsed, encoded to the standard HL7 XML form, and mapped by plain XSLT to one
 * readable clinical summary per event. The MLLP consumer acknowledges each message once
 * the exchange completes, so a failed ingestion is reported back to the sender.
 */
@ApplicationScoped
public class WardRoutes extends RouteBuilder {

    /** The header the langchain4j-ingest pipeline reads the document id from. */
    public static final String DOCUMENT_ID_HEADER = "CamelLangChain4jIngestDocumentId";

    private final WardLivePublisher livePublisher;

    // injecting the CDI bean and passing the instance to .bean(...) matters: with a Class
    // reference Camel instantiates it reflectively and the bean's @Inject fields stay null
    WardRoutes(WardLivePublisher livePublisher) {
        this.livePublisher = livePublisher;
    }

    @Override
    public void configure() {
        from("mllp:0.0.0.0:{{ward.feed.port}}")
                .routeId("hospital-feed")
                .unmarshal().hl7(false)
                // a new event is a new document version: first-write-wins per id
                .setHeader("patientId", hl7terser("/.PID-3-1"))
                .setHeader("eventId", hl7terser("/.MSH-10"))
                .setHeader(DOCUMENT_ID_HEADER, simple("${header.patientId}@${header.eventId}"))
                // HL7v2 has a standard XML encoding; the summary mapping is config, not code
                .bean(Hl7XmlEncoder.class)
                .to("xslt-saxon:mapping/ward-summary.xsl")
                .setVariable("summary", body())
                .to("direct:ward-ingest")
                // push the ingested summary to the demo page's live view
                .bean(livePublisher,
                        "publish(${variable.summary}, ${header." + DOCUMENT_ID_HEADER + "}, ${body})")
                .log("Ingested ${header." + DOCUMENT_ID_HEADER + "}: ${body}");
    }
}
