package org.acme.ward;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The full phase-1 flow: an ORU^R01 lab result arrives over MLLP, is rendered and
 * ingested, becomes searchable, and grounds the ward AI service's answer.
 */
@QuarkusTest
@WithTestResource(OllamaTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WardFeedTest {

    static final char START_BLOCK = 0x0b;
    static final char END_BLOCK = 0x1c;
    static final char CARRIAGE_RETURN = 0x0d;

    static final String ORU = String.join("\r",
            "MSH|^~\\&|LAB|ACME^HOSP|EHR|ACME|20261002093000||ORU^R01|MSG0001|P|2.4",
            "PID|1||PAT-123||Dupont^Marie||19560312|F",
            "OBR|1|||24331-1^Metabolic panel^LN",
            "OBX|1|NM|2345-7^Glucose^LN||182|mg/dL|70-99|H|||F",
            "OBX|2|NM|2823-3^Potassium^LN||4.1|mmol/L|3.5-5.2||||F");

    @ConfigProperty(name = "ward.feed.port")
    int feedPort;

    @Test
    @Order(1)
    void labResultBecomesSearchable() throws Exception {
        String ack = sendOverMllp(ORU);
        assertTrue(ack.contains("MSH"), "the MLLP consumer should acknowledge the message");

        Awaitility.await().atMost(Duration.ofMinutes(2)).untilAsserted(() -> given()
                .queryParam("q", "glucose lab result")
                .get("/search")
                .then()
                .statusCode(200)
                .body("document", hasItem("PAT-123@MSG0001"))
                .body("text.flatten()", hasItem(containsString("Glucose: 182 mg/dL"))));
    }

    @Test
    @Order(2)
    void chatAnswersGroundedInTheFeed() {
        // the WireMock "model" only knows this answer when the retrieved summary reached
        // the prompt, so a correct answer proves the retrieval step worked
        given()
                .queryParam("q", "Which patients had abnormal labs today?")
                .get("/chat")
                .then()
                .statusCode(200)
                .body(containsString("182 mg/dL"));
    }

    @Test
    @Order(3)
    void previewShowsTheAugmentedPrompt() {
        given()
                .queryParam("q", "Which patients had abnormal labs today?")
                .get("/chat/preview")
                .then()
                .statusCode(200)
                .body(containsString("Glucose: 182 mg/dL"));
    }

    String sendOverMllp(String hl7) throws Exception {
        try (Socket socket = new Socket("localhost", feedPort)) {
            OutputStream out = socket.getOutputStream();
            out.write(START_BLOCK);
            out.write(hl7.getBytes(StandardCharsets.UTF_8));
            out.write(END_BLOCK);
            out.write(CARRIAGE_RETURN);
            out.flush();

            InputStream in = socket.getInputStream();
            StringBuilder ack = new StringBuilder();
            int c;
            while ((c = in.read()) != -1 && c != END_BLOCK) {
                ack.append((char) c);
            }
            return ack.toString();
        }
    }
}
