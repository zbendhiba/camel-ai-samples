package org.acme.ward;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * The chat model of the tests. By default a WireMock server plays the Ollama API with canned
 * answers, so the tests need no model; the demo itself runs against a real Ollama. With
 * {@code OLLAMA_BASE_URL} set, that real Ollama is used instead.
 */
public class OllamaTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaTestResource.class);
    private static final String CHAT = "/api/chat";

    private WireMockServer server;

    @Override
    public Map<String, String> start() {
        String realBaseUrl = System.getenv("OLLAMA_BASE_URL");
        if (realBaseUrl != null && !realBaseUrl.isBlank()) {
            LOG.info("Using the Ollama server at {}", realBaseUrl);
            return Map.of();
        }

        LOG.info("Starting a fake Ollama server backed by WireMock");
        server = new WireMockServer(options().dynamicPort());
        server.start();
        // the "model" knows the answer only when the retrieved summary text is part of the
        // request, as it would be after augmentation; a bare question gets the fallback below,
        // so a correct answer in the tests proves that retrieval grounded the prompt
        server.stubFor(post(urlPathEqualTo(CHAT))
                .withRequestBody(containing("Glucose: 182 mg/dL"))
                .willReturn(okJson(answer(
                        "Marie Dupont (PAT-123) had a glucose of 182 mg/dL, flagged HIGH against a 70-99 reference."))));
        server.stubFor(post(urlPathEqualTo(CHAT))
                .atPriority(10)
                .willReturn(okJson(answer("I don't have that information."))));

        return Map.of("quarkus.langchain4j.ollama.base-url", server.baseUrl());
    }

    private static String answer(String content) {
        return """
                {
                  "model": "gemma4:e4b",
                  "created_at": "2026-01-01T00:00:00Z",
                  "message": { "role": "assistant", "content": "%s" },
                  "done": true,
                  "done_reason": "stop",
                  "prompt_eval_count": 1,
                  "eval_count": 1
                }
                """.formatted(content);
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
