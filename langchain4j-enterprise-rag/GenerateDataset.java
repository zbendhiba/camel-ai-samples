///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+

import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Generates the synthetic patient dataset with Synthea™, the Synthetic Patient
 * Population Simulator (https://github.com/synthetichealth/synthea, Apache-2.0).
 *
 * The pinned all-in-one release jar is used rather than the Maven artifact: Synthea
 * 4.0.0 declares org.graalvm.js:js:25.0.1, which only exists as a pom on Maven Central,
 * so plain dependency resolution fails. The release jar bundles everything.
 *
 * The run is fully seeded (seed, clinician seed and reference date), so the dataset is
 * reproducible: same Synthea version, same options, same patients. Output: FHIR R4
 * transaction bundles in src/main/resources/data/synthea/, ready to POST as-is to a HAPI FHIR server.
 *
 * Run it from the module root: jbang GenerateDataset.java
 */
public class GenerateDataset {

    static final String SYNTHEA_VERSION = "v4.0.0";
    static final String SYNTHEA_JAR_URL = "https://github.com/synthetichealth/synthea/releases/download/"
            + SYNTHEA_VERSION + "/synthea-with-dependencies.jar";

    static final Path JAR = Path.of("target/synthea-with-dependencies.jar");
    static final Path WORK = Path.of("target/synthea-work");
    static final Path OUT = Path.of("src/main/resources/data/synthea");

    public static void main(String... args) throws Exception {
        downloadSyntheaIfMissing();
        deleteRecursively(WORK);

        runSynthea(
                "-s", "42", "-cs", "42", "-r", "20261001",
                "-p", "20", "-a", "25-70",
                "--exporter.baseDirectory=" + WORK,
                "--exporter.years_of_history=2",
                "--exporter.fhir.included_resources=Patient,Encounter,Condition,MedicationRequest,AllergyIntolerance,Immunization",
                "--exporter.hospital.fhir.export=false",
                "--exporter.practitioner.fhir.export=false");

        // Keep only living patients: a deceased record makes no sense on a live ward feed.
        deleteRecursively(OUT);
        Files.createDirectories(OUT);
        long kept;
        try (Stream<Path> bundles = Files.list(WORK.resolve("fhir"))) {
            kept = bundles.filter(p -> p.toString().endsWith(".json"))
                    .filter(GenerateDataset::isAlive)
                    .peek(p -> copyTo(p, OUT))
                    .count();
        }
        System.out.println("Dataset: " + kept + " living patients in " + OUT);
    }

    static void runSynthea(String... syntheaArgs) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(
                new java.net.URL[] { JAR.toUri().toURL() }, ClassLoader.getPlatformClassLoader())) {
            Thread.currentThread().setContextClassLoader(loader);
            Class.forName("App", true, loader)
                    .getMethod("main", String[].class)
                    .invoke(null, (Object) syntheaArgs);
        }
    }

    static void downloadSyntheaIfMissing() throws Exception {
        if (Files.exists(JAR)) {
            return;
        }
        Files.createDirectories(JAR.getParent());
        System.out.println("Downloading Synthea " + SYNTHEA_VERSION + " (~200 MB, once)...");
        try (HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL).build()) {
            HttpResponse<Path> response = http.send(
                    HttpRequest.newBuilder(URI.create(SYNTHEA_JAR_URL)).build(),
                    HttpResponse.BodyHandlers.ofFile(JAR));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(JAR);
                throw new IOException("Download failed with HTTP " + response.statusCode());
            }
        }
    }

    static boolean isAlive(Path bundle) {
        try {
            return !Files.readString(bundle).contains("\"deceasedDateTime\"");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void copyTo(Path file, Path dir) {
        try {
            Files.copy(file, dir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> tree = Files.walk(dir)) {
            tree.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
