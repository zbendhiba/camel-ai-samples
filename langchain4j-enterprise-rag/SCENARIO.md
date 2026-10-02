# Enterprise RAG: scenario

Working document for a new example, `langchain4j-enterprise-rag`. Follow-up to
[`langchain4j-ingest-rag`](https://github.com/apache/camel-quarkus-examples/pull/580).

## The problem

The intro example ingests a folder of Markdown files. That is a hello world.

Real company knowledge does not sit in folders. It moves through systems, in formats
only integration software can read. A hospital is the sharpest case: admissions and
lab results arrive as HL7v2 messages over MLLP, patient records live in a FHIR server.
A raw HL7v2 message looks like this:

```
MSH|^~\&|LAB|ACME^HOSP|EHR|ACME|20261002||ORU^R01|MSG0042|P|2.4
PID|1||PAT-123||DUPONT^MARIE||19560312|F
OBX|1|NM|2345-7^GLUCOSE^LN||182|mg/dL|70-99|H|||F
```

No AI stack can embed that and get anything useful back. There is no file to convert,
no document to load. The document has to be **built** from live systems, and rebuilt
every time they change. That is an integration problem. Camel's problem.

### HL7v2 for the rest of us

HL7v2 is a pipe-separated format from 1987 where column positions carry the meaning.
One line per segment, named by its first three letters; `^` splits a column into parts.

| Line | What it says |
|---|---|
| `MSH\|...\|LAB\|ACME^HOSP\|EHR\|...\|ORU^R01\|MSG0042\|...` | The envelope: the lab of ACME hospital tells the EHR "here is a lab result" (`ORU^R01`), message number MSG0042 |
| `PID\|1\|\|PAT-123\|\|DUPONT^MARIE\|\|19560312\|F` | The patient: id PAT-123, name Dupont Marie, born 1956-03-12, female |
| `OBX\|1\|NM\|2345-7^GLUCOSE^LN\|\|182\|mg/dL\|70-99\|H\|...` | One result: glucose (LOINC 2345-7) is 182 mg/dL, normal range 70-99, flagged `H` for high |

The whole message says: *the lab reports that Marie Dupont's glucose is 182, above
normal*. The same information as a FHIR R4 `Observation`, in one line instead of
twenty-five of JSON.

### And FHIR, in one paragraph

[FHIR](https://hl7.org/fhir/) (Fast Healthcare Interoperability Resources) is HL7's
modern standard: it breaks medical data down into small, reusable building blocks
called **resources**, such as `Patient`, `Observation`, `Condition` or
`MedicationRequest`, exchanged as JSON over a REST API. A patient's record is a graph
of resources referencing each other. Where HL7v2 is the wire format of the hospital's
event feeds, FHIR is the API of its records.

## The story

A hospital ward runs an assistant for its clinicians. The knowledge is the patients'
own data. All data is synthetic.

Hospitals run both worlds side by side, and will for years: the event feeds (labs,
admissions) speak HL7v2, the records and APIs speak FHIR. This example deliberately
keeps both, as they are. Converting the v2 feed to FHIR is possible (the official
V2-to-FHIR mapping, or flows like the
[`fhir/` example](https://github.com/apache/camel-quarkus-examples/tree/main/fhir),
which stores HL7v2 patients into a HAPI server), but that serves a different concern:
keeping the system of record up to date. Here the record already exists; we build
knowledge from both sources without converting either.

Two ingestion routes, two kinds of documents, one vector store:

| Route | Source | Document produced | Pace |
|---|---|---|---|
| `ward` | HL7v2 over MLLP, continuous | **Event summary**: "Glucose: 182 mg/dL, flagged HIGH", id `PAT-123@MSG0001` | Real time, as the feed beats |
| `records` | FHIR R4, polled with the `fhir` component | **Record summary**: active conditions, medications, allergies of a patient, id `PAT-123@<lastUpdated>` | Slow, when a record changes |

The two formats are never converted into each other. They converge on the same
target: plain text a model can embed. Retrieval mixes both document kinds naturally:
"which patients had abnormal labs?" matches events, "any patients on anticoagulants?"
matches records, and every answer cites the documents it stands on.

## Ingestion: Camel builds the documents

**The `ward` route (events).** An HL7v2 message arrives on the MLLP port, the `hl7`
data format parses it, and a **mapping** renders one clinical summary per event: the
parsed message is encoded to HL7's standard XML form, and an XSLT shapes the document.
The mapping is configuration, not code: what the knowledge base says about an event is
adjusted in the stylesheet, without touching Java. The document id is version-aware
(`PAT-123@MSG0001`): the ingest engine is first-write-wins, every new event is a new
document. The vector store follows the ward in real time.

**The `records` route (context).** The `fhir` consumer polls the FHIR server for
new and updated resources and renders one record summary per patient: who they are,
active conditions, current medications, allergies. When a record changes, its summary
re-ingests under a new `@<lastUpdated>` id.

No file is read anywhere in the flow, and nothing is converted between HL7 and FHIR.

### From the wire to the vector store

What one event looks like at each end. In: a single MLLP frame, HL7v2 ER7.

```
MSH|^~\&|LAB|ACME^HOSP|EHR|ACME|20261002093000||ORU^R01|MSG0001|P|2.4
PID|1||PAT-123||Dupont^Marie||19560312|F
OBR|1|||24331-1^Metabolic panel^LN
OBX|1|NM|2345-7^Glucose^LN||182|mg/dL|70-99|H|||F
OBX|2|NM|2823-3^Potassium^LN||4.1|mmol/L|3.5-5.2||||F
```

Out: the document the embedding model actually sees, rendered by the mapping.

```
Patient Marie Dupont (id PAT-123, born 1956-03-12, sex F).
Event ORU^R01 at 20261002093000.
Lab results:
- Glucose: 182 mg/dL (reference 70-99), flagged HIGH
- Potassium: 4.1 mmol/L (reference 3.5-5.2)
```

Codes become words on the way through: `H` reads "flagged HIGH", `HH` reads
"flagged CRITICALLY HIGH", the reference range sits next to the value. That is the
point of the mapping: a question like "which patients had abnormal labs?" can only
match text that says abnormal things in plain language.

The `records` route does the same for the other world. In: FHIR R4 resources, as the
server returns them (excerpt; one `Condition` and one `MedicationRequest` out of a
patient's record):

```json
{ "resourceType": "Condition",
  "clinicalStatus": { "coding": [{ "code": "active" }] },
  "code": { "coding": [{ "system": "http://snomed.info/sct",
                         "code": "44054006", "display": "Type 2 diabetes mellitus" }] },
  "subject": { "reference": "Patient/PAT-123" },
  "recordedDate": "2019-03-18" }

{ "resourceType": "MedicationRequest", "status": "active",
  "medicationCodeableConcept": { "coding": [{ "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
                                              "code": "860975", "display": "Metformin 500 MG" }] },
  "subject": { "reference": "Patient/PAT-123" } }
```

Out: one record summary per patient, the same plain-language shape as the events.

```
Patient Marie Dupont (id PAT-123, born 1956-03-12, sex F).
Active conditions: type 2 diabetes mellitus (2019), hypertension (2021).
Current medications: metformin 500mg, lisinopril 10mg.
Allergies: penicillin.
```

Same lesson on both routes: coded structure in (`44054006`, `860975`), words out
("type 2 diabetes", "metformin"). The display labels the standards carry are what
makes the documents readable.

What Qdrant stores per document: the text split into overlapping segments (800
characters, 80 overlap), each with its 384-dimension MiniLM vector and this payload
(a real one, straight from the Qdrant console):

```json
{ "index": "0",
  "text_segment": "Patient Paul Martin (id PAT-456, born 1970-11-20, sex M).\nEvent ADT^A01 at 20261003080000.\nAdmitted to CARD1, reason: Chest pain.",
  "camel_ingest_pipeline": "ward",
  "camel_ingest_document_id": "PAT-456@DEMO1" }
```

`camel_ingest_pipeline` says which route produced the document, `camel_ingest_document_id`
is the version-aware citation, `index` numbers the segments of a split document.

**Cross-cutting, the parts a hospital would actually require:**

- A **dead-letter channel** on the feed, backed by a Kafka topic. The ingest
  extension has none in this increment: a failed record is logged and dropped.
  A silently lost lab result is not acceptable. The Camel route in front parks
  every failed message on the dead letter topic: retry, audit, alert.
- A **Kafka idempotent repository** (`KafkaIdempotentRepository`, camel-kafka).
  The default register is in-memory: every restart re-embeds everything. With
  Kafka it survives restarts, and one broker covers both safety concerns.
- Every summary carries metadata: `patient`, `ward`, `event-type`. Retrieval filters
  on it, answers cite their source through `camel_ingest_document_id`.

## RAG: plain Quarkus LangChain4j

Deliberately standard. The point of the example is that the ingestion is where the
work was.

- Vector store: Qdrant, through `quarkus-langchain4j-qdrant` and its Dev Service.
- Embeddings: in-process all-MiniLM-L6-v2 (384 dimensions), shared by ingestion
  and retrieval. No API key.
- Chat model: Ollama, as in the intro example.
- The ward AI service: `@RegisterAiService` with a `RetrievalAugmentor` supporting a
  metadata filter (one patient, one ward). An AI service, not an agent: one grounded
  question-answer flow, no tools, no multi-agent loop.
- **Output guardrails** against hallucination: an answer must be grounded in the
  retrieved segments and cite its sources (`camel_ingest_document_id`), or it is
  refused. Quarkus LangChain4j's guardrail API, used as designed.

**The right questions.** Retrieval-augmented answers are for what a structured query
cannot do: cross-document, temporal and similarity questions.

- "What happened on the ward overnight?"
- "Which patients had abnormal labs today?"
- "Have we seen similar presentations recently?"

A single-patient factual lookup ("is this patient on anticoagulants?") stays a FHIR
query: deterministic, no model in the path. The record summaries make the assistant
aware of that context, they do not replace the system of record.

## Flow

![Enterprise RAG · Patient Timeline](./langchain4j-enterprise-rag-memo-schema.svg)

## The data: Synthea™, generated by us

All patient data is synthetic, produced with [Synthea™](https://github.com/synthetichealth/synthea),
MITRE's Synthetic Patient Population Simulator. Synthea™ is Java and Apache-2.0
licensed, the same license as Camel, so provenance is clean for an ASF repo (the
pre-generated `synthea-sample-data` zips carry no license file, so we do not copy them).

- `GenerateDataset.java`, a JBang script, downloads the pinned Synthea™ release and
  generates 20 patients with fixed seeds and a fixed reference date
  (`jbang GenerateDataset.java`), as slim FHIR R4 bundles (~2 MB) committed with the
  example. Same version, same options, same patients: reproducible and extendable.
- At startup, the bundles seed the HAPI FHIR server: the `records` route then has a
  live system to poll.
- The HL7v2 messages are **not** taken from the internet: sample messages found online
  reference patients that do not exist in our FHIR server, and the knowledge base
  should tell one coherent story per patient across both routes. The feeder builds
  valid ADT and ORU messages with HAPI (the library the `hl7` component already uses),
  using the ids of the seeded Synthea™ patients. Same ids on both sides, plausible lab
  values, a coherent flow.

## Running it: real systems, no stand-ins

The example runs against the real thing:

- **Ollama** on the developer's machine, same as the intro example.
- **HAPI FHIR server** as a container, the same image the `fhir/` example uses.
- **Qdrant and Kafka** started by Dev Services.
- **A feeder** sends the generated HL7v2 messages to the MLLP port (a small script or
  `camel jbang` route), so the demo has a live pulse: start the app, watch summaries
  appear, ask questions about patients that did not exist a minute ago.

WireMock appears **only** in the JVM and native tests, standing in for Ollama so CI
runs without a GPU (the `OllamaTestResource` pattern from the intro example). The
MLLP feed is plain TCP and is fed directly from the tests; FHIR runs as a
testcontainer there too.

## Endpoints

Same names as the intro example, so readers can follow the progression:

| Endpoint | Shows |
|---|---|
| `GET /search?q=` | Raw retrieval: closest segments, score, patient, document id |
| `GET /search?q=&patient=` | The same, filtered by patient |
| `GET /chat/preview?q=` | The augmented prompt, no LLM call |
| `GET /chat?q=` | The grounded answer, with citations |

## Phasing

1. ~~Scaffold, Qdrant, assistant, the MLLP route with HL7 parsing, the XSLT summary
   mapping, with route tests and an end-to-end test.~~ **Done.**
2. The `records` route: HAPI FHIR container, Synthea™ seeding at startup, the `fhir`
   polling consumer, record summaries.
3. Kafka: dead letter topic, idempotent repository, metadata filtering, citations,
   output guardrails.
4. The feeder, README.adoc with the flow diagram, `examples.json` entry, native
   build, polish.

Each phase leaves the example buildable and demoable.

## Open questions

- Streaming chat? The intro example is synchronous. Streaming reads more like a real
  product but complicates the test setup.
- Where this lives in the end: drafted in `camel-ai-samples` for now; later
  `camel-quarkus-examples` or a dedicated blueprint repo.
