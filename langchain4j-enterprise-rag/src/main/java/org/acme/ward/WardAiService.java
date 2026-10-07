package org.acme.ward;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The ward AI service: one grounded question-answer flow, no tools, no agent loop.
 * Retrieval-augmented answers are for what a structured query cannot do: cross-document,
 * temporal and similarity questions. A single-patient factual lookup stays a FHIR query.
 *
 * No chat memory, and not only because each question stands alone: the default memory
 * stores every past question together with its retrieved segments, so a few turns fill
 * the model's context window and the answer gets truncated to nothing.
 */
@RegisterAiService(retrievalAugmentor = WardRetrievalAugmentor.class,
        chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class)
@ApplicationScoped
public interface WardAiService {

    @SystemMessage("""
            You assist the clinicians of a hospital ward. Now is {now}.
            Use ONLY the clinical summaries provided together with the question.
            Events carry their date and time: use them to answer time questions
            such as "today", "this morning" or "overnight".
            If the summaries do not contain the answer, reply: I don't have that information.
            Name the patients and events your answer is based on. Be concise.""")
    String answer(@UserMessage String question, String now);

    /**
     * The model has no clock: "today" and "overnight" only mean something when the
     * prompt says what time it is. Callers pass this as the {@code now} argument.
     */
    static String now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toString().replace('T', ' ');
    }
}
