package org.acme.ward;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * The demo page, through the app: the page renders, and POST /feed pushes a message
 * through the real MLLP port and reports the acknowledgment.
 */
@QuarkusTest
@WithTestResource(OllamaTestResource.class)
@WithTestResource(FreeFeedPortTestResource.class)
class DemoPageTest {

    @Test
    void pageRenders() {
        given()
                .get("/")
                .then()
                .statusCode(200)
                .body(containsString("THE FEED"))
                .body(containsString("/ws/ward"));
    }

    @Test
    void feedEndpointSendsOverMllpAndReturnsTheAck() {
        given()
                .contentType("text/plain")
                .body(String.join("\n",
                        "MSH|^~\\&|LAB|ACME^HOSP|EHR|ACME|20261003110000||ORU^R01|DEMOTEST|P|2.4",
                        "PID|1||PAT-123||Dupont^Marie||19560312|F",
                        "OBR|1|||24331-1^Metabolic panel^LN",
                        "OBX|1|NM|2345-7^Glucose^LN||95|mg/dL|70-99||||F"))
                .post("/feed")
                .then()
                .statusCode(200)
                .body("ackCode", equalTo("AA"));
    }
}
