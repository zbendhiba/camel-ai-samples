package org.acme.ward;

import java.util.List;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.query.Metadata;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/chat")
public class ChatResource {

    @Inject
    WardAiService aiService;

    @Inject
    WardRetrievalAugmentor augmentor;

    /** Asks the ward AI service; the answer is grounded in the retrieved summaries. */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String chat(@QueryParam("q") String question) {
        return aiService.answer(question, WardAiService.now());
    }

    /** The prompt the AI service would send: the question plus the retrieved context. No LLM is called. */
    @GET
    @Path("/preview")
    @Produces(MediaType.TEXT_PLAIN)
    public String preview(@QueryParam("q") String question) {
        UserMessage message = UserMessage.from(question);
        AugmentationResult result = augmentor.get().augment(
                new AugmentationRequest(message, Metadata.from(message, "preview", List.of())));
        return ((UserMessage) result.chatMessage()).singleText();
    }
}
