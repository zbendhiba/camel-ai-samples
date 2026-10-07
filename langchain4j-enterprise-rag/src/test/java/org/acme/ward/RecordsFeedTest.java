package org.acme.ward;

import java.time.Duration;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

/**
 * The records flow end to end: the Dev Service FHIR server is seeded with the Synthea
 * patients at startup, the patient-records route polls it, and the record summaries
 * become searchable.
 */
@QuarkusTest
@WithTestResource(OllamaTestResource.class)
@WithTestResource(FreeFeedPortTestResource.class)
class RecordsFeedTest {

    @Test
    void seededRecordsBecomeSearchable() {
        // Wilfredo's kidney disease is a seeded Synthea fact; finding it proves the
        // whole chain: seeding, polling, rendering, embedding, storing
        Awaitility.await().atMost(Duration.ofMinutes(3)).untilAsserted(() -> given()
                .queryParam("q", "chronic kidney disease")
                .get("/search")
                .then()
                .statusCode(200)
                .body("text.flatten()", hasItem(containsString("has Chronic kidney disease"))));
    }
}
