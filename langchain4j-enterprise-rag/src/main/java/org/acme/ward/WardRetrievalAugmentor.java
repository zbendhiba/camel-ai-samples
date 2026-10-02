package org.acme.ward;

import java.util.function.Supplier;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The retrieval side of the AI service: the summaries closest to the question are
 * fetched from Qdrant and attached to the prompt.
 */
@ApplicationScoped
public class WardRetrievalAugmentor implements Supplier<RetrievalAugmentor> {

    private final EmbeddingStoreContentRetriever retriever;

    WardRetrievalAugmentor(EmbeddingStore store, EmbeddingModel model) {
        retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(model)
                .maxResults(5)
                .build();
    }

    @Override
    public RetrievalAugmentor get() {
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(retriever)
                .build();
    }
}
