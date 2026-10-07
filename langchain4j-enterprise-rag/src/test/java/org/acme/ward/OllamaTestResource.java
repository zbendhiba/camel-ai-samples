package org.acme.ward;

import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * The models of the tests. By default a WireMock server plays the Ollama API with canned
 * answers, so the tests need no model; the demo itself runs against a real Ollama. With
 * {@code OLLAMA_BASE_URL} set, that real Ollama is used instead.
 *
 * The chat endpoint answers from canned responses. The embedding endpoint returns a
 * deterministic bag-of-words vector: texts sharing words land close together, so
 * retrieval keeps real ranking semantics ("glucose lab result" finds the glucose
 * summary) without any model.
 */
public class OllamaTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaTestResource.class);
    private static final String CHAT = "/api/chat";
    private static final String EMBED = "/api/embed";
    private static final int DIMENSIONS = 768;

    private WireMockServer server;

    @Override
    public Map<String, String> start() {
        String realBaseUrl = System.getenv("OLLAMA_BASE_URL");
        if (realBaseUrl != null && !realBaseUrl.isBlank()) {
            LOG.info("Using the Ollama server at {}", realBaseUrl);
            return Map.of();
        }

        LOG.info("Starting a fake Ollama server backed by WireMock");
        server = new WireMockServer(options().dynamicPort().extensions(new EmbedTransformer()));
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
        server.stubFor(post(urlPathEqualTo(EMBED))
                .willReturn(aResponse().withTransformers(EmbedTransformer.NAME)));

        return Map.of("quarkus.langchain4j.ollama.base-url", server.baseUrl());
    }

    /** Answers {@code /api/embed} with one bag-of-words vector per input text. */
    static class EmbedTransformer extends ResponseDefinitionTransformer {

        static final String NAME = "embed-transformer";
        static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition,
                FileSource files, Parameters parameters) {
            try {
                JsonNode input = MAPPER.readTree(request.getBodyAsString()).path("input");
                ObjectNode body = MAPPER.createObjectNode().put("model", "embeddinggemma:300m");
                ArrayNode embeddings = body.putArray("embeddings");
                if (input.isArray()) {
                    input.forEach(text -> embeddings.add(vector(text.asText())));
                } else {
                    embeddings.add(vector(input.asText()));
                }
                return new ResponseDefinitionBuilder()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(MAPPER.writeValueAsString(body))
                        .build();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean applyGlobally() {
            return false; // only the /api/embed stub names this transformer
        }

        private static ArrayNode vector(String text) {
            float[] v = new float[DIMENSIONS];
            for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (!token.isEmpty()) {
                    v[Math.floorMod(token.hashCode(), DIMENSIONS)] += 1;
                }
            }
            double norm = 0;
            for (float x : v) {
                norm += x * x;
            }
            norm = norm == 0 ? 1 : Math.sqrt(norm);
            ArrayNode array = MAPPER.createArrayNode();
            for (float x : v) {
                array.add(x / norm);
            }
            return array;
        }
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
