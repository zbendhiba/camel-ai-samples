package org.acme.ward;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Seeds the FHIR server with the Synthea(TM) patients at startup, so the records route
 * has a live system to poll. The bundles are FHIR transaction bundles: posting one makes
 * the server create the resources and resolve the references. Seeding is idempotent the
 * simple way: a server that already has patients is left alone.
 */
@ApplicationScoped
public class FhirSeeder {

    private static final Logger LOG = Logger.getLogger(FhirSeeder.class);
    private static final Pattern TOTAL = Pattern.compile("\"total\"\\s*:\\s*(\\d+)");

    @ConfigProperty(name = "ward.fhir.url")
    String fhirUrl;

    @ConfigProperty(name = "ward.seed.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "ward.seed.wait", defaultValue = "180s")
    Duration wait;

    @Inject
    ProducerTemplate producer;

    void onStart(@Observes StartupEvent event) throws Exception {
        if (enabled) {
            waitForFhir();
            seedIfEmpty();
        }
    }

    private void seedIfEmpty() throws Exception {
        int patients = patientCount();
        if (patients > 0) {
            LOG.infof("FHIR server already holds %d patients, skipping the seeding", patients);
            return;
        }
        int seeded = 0;
        for (String bundleFile : index()) {
            String bundle;
            try (InputStream in = getClass().getResourceAsStream("/data/synthea/" + bundleFile)) {
                bundle = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            producer.requestBody("fhir://transaction/withBundle?inBody=stringBundle"
                    + "&serverUrl=" + fhirUrl + "&fhirVersion=R4", bundle);
            seeded++;
        }
        LOG.infof("Seeded the FHIR server with %d Synthea bundles", seeded);
    }

    private String[] index() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/data/synthea/index.txt")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip().split("\n");
        }
    }

    private void waitForFhir() throws Exception {
        Instant deadline = Instant.now().plus(wait);
        try (HttpClient http = HttpClient.newHttpClient()) {
            while (true) {
                try {
                    HttpResponse<Void> response = http.send(
                            HttpRequest.newBuilder(URI.create(fhirUrl + "/metadata"))
                                    .timeout(Duration.ofSeconds(5)).build(),
                            HttpResponse.BodyHandlers.discarding());
                    if (response.statusCode() == 200) {
                        return;
                    }
                } catch (Exception waiting) {
                    // the container is still booting
                }
                if (Instant.now().isAfter(deadline)) {
                    throw new IllegalStateException("The FHIR server at " + fhirUrl
                            + " did not come up within " + wait);
                }
                LOG.info("Waiting for the FHIR server...");
                Thread.sleep(2000);
            }
        }
    }

    private int patientCount() throws Exception {
        try (HttpClient http = HttpClient.newHttpClient()) {
            String body = http.send(
                    HttpRequest.newBuilder(URI.create(fhirUrl + "/Patient?_summary=count")).build(),
                    HttpResponse.BodyHandlers.ofString()).body();
            Matcher matcher = TOTAL.matcher(body);
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
        }
    }
}
