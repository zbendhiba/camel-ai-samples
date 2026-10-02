package org.acme.ward;

import java.util.List;
import java.util.Map;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestService;

@Path("/search")
public class SearchResource {

    @Inject
    EmbeddingStore<TextSegment> store;

    @Inject
    EmbeddingModel model;

    /** The raw retrieval step: the segments closest to the question, with their source document. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> search(@QueryParam("q") String question) {
        return store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(model.embed(question).content())
                .maxResults(3)
                .build())
                .matches().stream()
                .map(match -> Map.<String, Object> of(
                        "score", match.score(),
                        "document", String.valueOf(match.embedded().metadata().getString(IngestService.METADATA_DOCUMENT_ID)),
                        "text", match.embedded().text()))
                .toList();
    }
}
