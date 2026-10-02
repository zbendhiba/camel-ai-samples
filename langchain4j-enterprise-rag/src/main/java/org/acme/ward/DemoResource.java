package org.acme.ward;

import java.util.Map;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The demo page. GET / renders it; POST /feed injects an HL7v2 message through the
 * real MLLP port with Camel's own mllp producer, so the full flow runs and the MLLP
 * acknowledgment comes back once the ingestion completed.
 */
@Path("/")
public class DemoResource {

    @Inject
    Template index;

    @Inject
    ProducerTemplate producer;

    @ConfigProperty(name = "ward.feed.port")
    int feedPort;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance page() {
        return index.instance();
    }

    @POST
    @Path("feed")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> feed(String hl7) {
        // the textarea sends \n, the wire wants \r
        String message = hl7.strip().replace("\r\n", "\r").replace('\n', '\r');
        // the mllp producer reports the acknowledgment in headers, not in the returned body
        var exchange = producer.request("mllp:localhost:" + feedPort,
                e -> e.getMessage().setBody(message));
        String ackCode = exchange.getMessage().getHeader("CamelMllpAcknowledgementType", String.class);
        String ack = exchange.getMessage().getHeader("CamelMllpAcknowledgementString", String.class);
        return Map.of(
                "ackCode", ackCode == null ? "??" : ackCode,
                "ack", ack == null ? "" : ack);
    }
}
