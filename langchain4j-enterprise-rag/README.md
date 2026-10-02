# langchain4j-enterprise-rag

Enterprise RAG, the integration way. Camel ingests a hospital's HL7v2 feed, enriches
it with FHIR, and keeps a Qdrant vector store in sync. A Quarkus LangChain4j AI
service answers ward questions, grounded and with citations.

The full scenario and the flow diagram are in [SCENARIO.md](SCENARIO.md).

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## The dataset

All patient data is synthetic, generated with
[Synthea™](https://github.com/synthetichealth/synthea), the Synthetic Patient
Population Simulator (Apache-2.0, by MITRE).

- `src/main/resources/data/synthea/` holds 20 patients as **FHIR R4 transaction
  bundles** in JSON. Every entry carries a POST request, so seeding the HAPI FHIR
  server is a plain POST of each file to the server root: the server creates the
  resources and resolves the references between them. No transformation needed.
- These files are **not** ingested into the vector store. They only bootstrap the
  demo's FHIR server, the "live system" the example talks to. The knowledge flows
  in through the HL7v2 feed and the FHIR queries, never from files.
- The dataset is reproducible: fixed seeds, fixed reference date, pinned Synthea™
  version. Regenerate or extend it with:

  ```shell script
  jbang GenerateDataset.java
  ```

  The script downloads the pinned Synthea™ release once (~200 MB, into `target/`),
  keeps only living patients, and slims each bundle to the resources the example
  uses: Patient, Encounter, Condition, MedicationRequest, AllergyIntolerance,
  Immunization (~2 MB in total instead of tens of MB).

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/langchain4j-enterprise-rag-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- Camel FHIR ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/fhir.html)): Exchange information in the healthcare domain using the FHIR (Fast Healthcare Interoperability Resources) standard
- Camel HL7 ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/hl7.html)): Marshal and unmarshal HL7 (Health Care) model objects using the HL7 MLLP codec
- Camel MLLP ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/mllp.html)): Communicate with external systems using the MLLP protocol
- Camel LangChain4j Ingest ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/langchain4j-ingest.html)): Declarative AI document ingestion: point a knowledge base at a folder via configuration; splitting, embedding and storing are handled under the hood
- LangChain4j Qdrant embedding store ([guide](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)): Provides the Qdrant Embedding store for LangChain4j
- LangChain4j Ollama ([guide](https://docs.quarkiverse.io/quarkus-langchain4j/dev/guide-ollama.html)): Provides the basic integration of Ollama with LangChain4j
