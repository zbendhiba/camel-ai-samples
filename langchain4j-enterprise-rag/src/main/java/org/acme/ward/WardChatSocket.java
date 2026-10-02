package org.acme.ward;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import jakarta.inject.Inject;

/**
 * The chatbot socket: one question in, one grounded answer out. Returning a plain
 * String makes the callback blocking, so it runs on a worker thread while the AI
 * service calls the model.
 */
@WebSocket(path = "/ws/chat", endpointId = "ward-chat")
public class WardChatSocket {

    @Inject
    WardAiService aiService;

    @OnTextMessage
    public String onQuestion(String question) {
        return aiService.answer(question, WardAiService.now());
    }
}
