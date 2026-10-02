package org.acme.ward;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestResult;
import org.jboss.logging.Logger;

/**
 * Broadcasts each ingested summary to the demo page. Called from the hospital-feed
 * route; a slow or broken browser connection must never fail an ingestion, so every
 * send failure is logged and swallowed.
 */
@ApplicationScoped
public class WardLivePublisher {

    private static final Logger LOG = Logger.getLogger(WardLivePublisher.class);

    @Inject
    OpenConnections connections;

    @Inject
    ObjectMapper mapper;

    public void publish(String summary, String documentId, Object ingestResult) {
        // in the advised route tests the pipeline is mocked and the body is not an IngestResult
        String outcome = ingestResult instanceof IngestResult result
                ? String.valueOf(result.outcome())
                : "MOCKED";
        String json;
        try {
            json = mapper.writeValueAsString(Map.of(
                    "documentId", documentId,
                    "outcome", outcome,
                    "summary", summary));
        } catch (Exception e) {
            LOG.warn("Could not serialize the live update", e);
            return;
        }
        for (WebSocketConnection connection : connections.findByEndpointId(WardLiveSocket.ENDPOINT_ID)) {
            try {
                connection.sendTextAndAwait(json);
            } catch (Exception e) {
                LOG.debugf("Dropping live update for connection %s: %s", connection.id(), e.getMessage());
            }
        }
    }
}
