package org.acme.ward;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the EXISTING hospital-feed route (defined in WardRoutes) in isolation, the Camel
 * way: AdviceWith swaps the MLLP consumer for a direct endpoint and the ingest pipeline
 * for a mock, and everything in between runs for real - the hl7 unmarshalling, the
 * hl7terser headers, the XML encoding and the XSLT mapping. A broken mapping fails here
 * in milliseconds, without a socket, an embedding model or a vector store.
 */
@QuarkusTest
@WithTestResource(FreeFeedPortTestResource.class)
public class WardRouteTest extends CamelQuarkusTestSupport {

    @BeforeEach
    void adviseHospitalFeed() throws Exception {
        AdviceWith.adviceWith(this.context, "hospital-feed", route -> {
            route.replaceFromWith("direct:feed-test");
            route.weaveByToUri("direct:ward-ingest").replace().to("mock:ingested");
        });
    }

    @Test
    void oruRendersPatientEventAndEachObservation() throws Exception {
        MockEndpoint ingested = getMockEndpoint("mock:ingested");
        ingested.expectedMessageCount(1);
        ingested.expectedHeaderReceived(WardRoutes.DOCUMENT_ID_HEADER, "PAT-123@MSG0001");
        ingested.expectedBodiesReceived("""
                Patient Marie Dupont (id PAT-123, born 1956-03-12, sex F).
                Event ORU^R01 on 2026-10-02 at 09:30.
                Lab results:
                - Glucose: 182 mg/dL (reference 70-99), flagged HIGH
                - Potassium: 4.1 mmol/L (reference 3.5-5.2)
                This patient has abnormal lab results: Glucose.
                """);

        template.sendBody("direct:feed-test", String.join("\r",
                "MSH|^~\\&|LAB|ACME^HOSP|EHR|ACME|20261002093000||ORU^R01|MSG0001|P|2.4",
                "PID|1||PAT-123||Dupont^Marie||19560312|F",
                "OBR|1|||24331-1^Metabolic panel^LN",
                "OBX|1|NM|2345-7^Glucose^LN||182|mg/dL|70-99|H|||F",
                "OBX|2|NM|2823-3^Potassium^LN||4.1|mmol/L|3.5-5.2||||F"));

        ingested.assertIsSatisfied(10000);
    }

    @Test
    void adtRendersTheAdmissionWithWardAndReason() throws Exception {
        MockEndpoint ingested = getMockEndpoint("mock:ingested");
        ingested.expectedMessageCount(1);
        ingested.expectedHeaderReceived(WardRoutes.DOCUMENT_ID_HEADER, "PAT-456@MSG0002");
        ingested.expectedBodiesReceived("""
                Patient Paul Martin (id PAT-456, born 1970-11-20, sex M).
                Event ADT^A01 on 2026-10-02 at 08:00.
                Admitted to CARD1, reason: Chest pain.
                """);

        template.sendBody("direct:feed-test", String.join("\r",
                "MSH|^~\\&|ADT|ACME^HOSP|EHR|ACME|20261002080000||ADT^A01|MSG0002|P|2.4",
                "EVN|A01|20261002080000",
                "PID|1||PAT-456||Martin^Paul||19701120|M",
                "PV1|1|I|CARD1",
                "PV2|||^Chest pain"));

        ingested.assertIsSatisfied(10000);
    }

    @Test
    void criticalFlagsAreSpelledOut() throws Exception {
        MockEndpoint ingested = getMockEndpoint("mock:ingested");
        ingested.expectedMessageCount(1);
        ingested.message(0).body(String.class)
                .contains("- Potassium: 6.9 mmol/L (reference 3.5-5.2), flagged CRITICALLY HIGH");

        template.sendBody("direct:feed-test", String.join("\r",
                "MSH|^~\\&|LAB|ACME^HOSP|EHR|ACME|20261002093000||ORU^R01|MSG0003|P|2.4",
                "PID|1||PAT-123||Dupont^Marie||19560312|F",
                "OBR|1|||24331-1^Metabolic panel^LN",
                "OBX|1|NM|2823-3^Potassium^LN||6.9|mmol/L|3.5-5.2|HH|||F"));

        ingested.assertIsSatisfied(10000);
    }

    @Test
    void missingUnitsRangeAndFlagLeaveNoDebris() throws Exception {
        MockEndpoint ingested = getMockEndpoint("mock:ingested");
        ingested.expectedMessageCount(1);
        ingested.message(0).body(String.class).contains("- Blood group: A positive\n");
        ingested.message(0).body(String.class).not().contains("(reference");
        ingested.message(0).body(String.class).not().contains("flagged");

        template.sendBody("direct:feed-test", String.join("\r",
                "MSH|^~\\&|LAB|ACME^HOSP|EHR|ACME|20261002093000||ORU^R01|MSG0004|P|2.4",
                "PID|1||PAT-123||Dupont^Marie||19560312|F",
                "OBR|1|||24331-1^Metabolic panel^LN",
                "OBX|1|ST|722-3^Blood group^LN||A positive||||||F"));

        ingested.assertIsSatisfied(10000);
    }
}
