# Local LLM Testing for Email Triage with Structured Output

Date: 2026-04-15

## Goal

Find a local LLM that works with OpenAI-style structured output (`json_schema` in `response_format`) on Ollama's `/v1/chat/completions` endpoint, used by the `camel-openai` component.

## Test Emails

Four test emails were used consistently across all models:

1. **Security Alert**: From security@company.com, CVE detected in production, patch within 24 hours. Expected: `SECURITY_ALERT`, `needsReply: false`
2. **Server Down**: From boss@company.com, production server crashed, customers affected, need help NOW. Expected: `URGENT`, `needsReply: true`
3. **Newsletter**: From newsletter@techcompany.com, weekly tech digest. Expected: `INFORMATIONAL`, `needsReply: false`
4. **Lunch**: From alice@company.com, lunch tomorrow at noon. Expected: `ACTION_REQUIRED`, `needsReply: true`

## Approach 1: Classification Logic in JSON Schema Descriptions

In this approach, all classification rules (category definitions, tiebreaker rules, needsReply semantics) are embedded in the JSON schema field descriptions. The prompt is minimal ("Classify the following email" + email content). No system prompt, no few-shot examples.

This is how OpenAI's structured output documentation recommends using `json_schema`. It works well with GPT-5 and other large cloud models.

### Results

| Model | Size | Security Alert | Server Down | Newsletter | Lunch | Score |
|---|---|---|---|---|---|---|
| gemma4:e4b | 4B | hallucinated | hallucinated | hallucinated | hallucinated | 0/4 |
| gemma3:4b | 4B | hallucinated | hallucinated | hallucinated | hallucinated | 0/4 |
| qwen3:4b | 4B | wrong | wrong | wrong | wrong | 0/4 |
| qwen3:8b | 8B | SECURITY_ALERT ✅ | SECURITY_ALERT ❌ | INFORMATIONAL ✅ | SECURITY_ALERT ❌ | 2/4 |
| qwen3.5:4b | 4B | wrong | wrong | wrong | wrong | 0/4 |
| qwen3.5:9b | 9B | wrong | wrong | wrong | wrong | 0/4 |
| mistral:7b | 7B | wrong | wrong | wrong | wrong | 0/4 |
| llama3.1:8b | 8B | wrong | wrong | wrong | wrong | 0/4 |
| ministral-3:8b | 8B | SECURITY_ALERT ✅ | URGENT ✅ | ACTION_REQUIRED ❌ | INFORMATIONAL ❌ | 2/4 |
| qwen3-vl:8b | 8B | SECURITY_ALERT ✅ | URGENT ✅ | INFORMATIONAL ✅ | SECURITY_ALERT ❌ | 3/4 |

**qwen3-vl:8b was the best at 3/4**, but inconsistent across runs (second run dropped to 1/4).

### Root Cause

All models exhibit a **rationale/category disconnection**: the rationale text correctly describes the email (e.g., "work-related scheduling request for lunch") but the category enum value is chosen independently (e.g., `SECURITY_ALERT` or `SHIPPING`).

The rationale and the enum are generated as separate token sequences. On Ollama's `/v1/` endpoint, the constrained decoding for enum values does not condition on the previously generated rationale text. The model writes good reasoning, then picks a random valid enum value.

### Gemma-specific issues

Gemma models (gemma3, gemma4) have additional known issues with Ollama structured output:
- Ollama issue #15288: Gemma4 on `/v1/` endpoint returns empty content with text in reasoning field
- Ollama issue #15260: `think=false` breaks structured output format constraint

### Other attempts that did not help

- **Adding needsReply reasoning to the rationale description**: Made results worse (1/4 instead of 3/4 with qwen3-vl:8b)
- **Two-step calls** (first call for rationale, second call to extract category from rationale): Showed promise (2/2 correct before interruption) but doubles latency and API calls
- **Renaming SUSPICIOUS to SECURITY_ALERT**: Helped qwen3:8b on the specific security alert test case, but did not fix the general disconnection problem

## Approach 2: System Prompt + Few-Shot Examples (ibek's pattern)

Inspired by [ibek/camel-openai-patterns/classify-leaf-node](https://github.com/ibek/camel-openai-patterns/tree/main/generative-parsing/classify-leaf-node).

Key changes from Approach 1:
- **System prompt** via `CamelOpenAISystemMessage` header with category definitions and classification rules
- **Few-shot examples** loaded from a file, showing 3 correct classifications
- **Low temperature** (0.15) to reduce randomness
- **Simplified schema descriptions**: no classification logic in the schema, just "Step-by-step reasoning about the classification." and "The email category."

### Results with qwen3-vl:8b

#### Curl tests (4 test emails)

| Security Alert | Server Down | Newsletter | Lunch | Score |
|---|---|---|---|---|
| SECURITY_ALERT ✅ | ACTION_REQUIRED ⚠️ | INFORMATIONAL ✅ | ACTION_REQUIRED ✅ | 4/4 |

Server down was classified as ACTION_REQUIRED instead of URGENT, which is debatable (the boss is asking you to do something). All needsReply values were correct.

#### Real Gmail test: Run 1 (7 emails)

| Email | Category | Correct | Draft Reply |
|---|---|---|---|
| Get updates about ChatGPT | INFORMATIONAL | ✅ | no (correct) |
| Alerte de sécurité (x4) | SECURITY_ALERT | ✅ | no (correct) |
| Lunch on Wednesday? | ACTION_REQUIRED | ✅ | yes (correct) |
| DevFest talk - need your confirmation | ACTION_REQUIRED | ✅ | yes (correct) |
| Q3 Platform Migration | ACTION_REQUIRED | ✅ | no (correct) |

**7/7 correct on category, 2/2 correct on draft replies.** Works with French emails too.

#### Real Gmail test: Run 2 (5 emails)

Inconsistent. "Lunch on Wednesday?" regressed to SECURITY_ALERT. Also very slow (~40s per email).

| Email | Category | Correct |
|---|---|---|
| Get updates about ChatGPT | INFORMATIONAL | ✅ |
| Alerte de sécurité | SECURITY_ALERT | ✅ |
| Lunch on Wednesday? | SECURITY_ALERT | ❌ |
| DevFest talk - need your confirmation | ACTION_REQUIRED | ✅ |
| Q3 Platform Migration | ACTION_REQUIRED | ✅ |

**Verdict**: qwen3-vl:8b works but is too slow and not consistent enough.

### Results with ministral-3:8b

After adding 2 more few-shot examples (social/casual request, shipping notification) for a total of 5 examples.

#### Real Gmail test: Run 1 (10 emails)

| Email | Category | Correct | Draft Reply |
|---|---|---|---|
| Get updates about ChatGPT | INFORMATIONAL | ✅ | no (correct) |
| Alerte de sécurité | SECURITY_ALERT | ✅ | no (correct) |
| Lunch on Wednesday? | ACTION_REQUIRED | ✅ | yes (correct) |
| DevFest talk - need your confirmation | ACTION_REQUIRED | ✅ | yes (correct) |
| Q3 Platform Migration | ACTION_REQUIRED | ✅ | yes (hallucinated) |
| test 3 | INFORMATIONAL | ✅ | no (correct) |
| Take care of your mind this Health Day | INFORMATIONAL | ✅ | no (correct) |
| Jump into Jira with Ease | INFORMATIONAL | ✅ | no (correct) |
| JIRA Database Latency Spike (x3) | INFORMATIONAL | ✅ | no (correct) |

**10/10 correct on triage category.** Fast (~5 seconds per email). Works with French emails.

Draft reply hallucination: for the Q3 Platform Migration email (long, detailed email with specific names, dates, action items), ministral-3:8b fabricated commitments and claimed the user was managing people mentioned in the email. The draft prompt says "Do NOT fabricate information" but the model ignores it. This is a known limitation of small models for free-form text generation.

#### Real Gmail test: Run 2 (12 emails, after adding team-wide few-shot example)

After adding a 6th few-shot example for team-wide announcements (ACTION_REQUIRED, needsReply: false).

| Email | Category | Correct |
|---|---|---|
| Get updates about ChatGPT | INFORMATIONAL | ✅ |
| Alerte de sécurité | SECURITY_ALERT | ✅ |
| Lunch on Wednesday? | ACTION_REQUIRED | ✅ |
| DevFest talk - need your confirmation | ACTION_REQUIRED | ✅ |
| Q3 Platform Migration | ACTION_REQUIRED | ✅ |
| test 3 | INFORMATIONAL | ✅ |
| Take care of your mind this Health Day | INFORMATIONAL | ✅ |
| Jump into Jira with Ease | INFORMATIONAL | ✅ |
| JIRA Database Latency Spike (x2) | URGENT | ⚠️ |
| JIRA Database Latency Spike (x2) | INFORMATIONAL | ✅ |

**10/12 on triage.** The JIRA "Database Latency Spike in Production" emails were inconsistent: 2x URGENT, 2x INFORMATIONAL. This is genuinely ambiguous: it's an automated JIRA notification (INFORMATIONAL) but the content describes a production incident (URGENT). No draft reply for Q3 Platform Migration this time, confirming the team-wide few-shot example fixed the needsReply issue.

**Verdict**: ministral-3:8b is excellent for triage (fast, accurate, consistent). For draft replies, a larger model like gemma4 produces better results.

### Results with gemma4:e4b

Same system prompt + few-shot pattern, same 5 examples.

#### Real Gmail test (8 emails)

| Email | Category | Correct | Speed |
|---|---|---|---|
| Get updates about ChatGPT | INFORMATIONAL | ✅ | ~4s |
| Alerte de sécurité | SECURITY_ALERT | ✅ | ~4s |
| Lunch on Wednesday? | ACTION_REQUIRED | ✅ | ~5s |
| DevFest talk - need your confirmation | ACTION_REQUIRED | ✅ | ~4s |
| Q3 Platform Migration | ACTION_REQUIRED | ✅ | ~13s |
| test 3 | INFORMATIONAL | ✅ | ~4s |
| Take care of your mind this Health Day | INFORMATIONAL | ✅ | ~5s |
| Jump into Jira with Ease | INFORMATIONAL | ✅ | ~5s |

**8/8 correct on triage category.** Fast (~4-5s per email, ~13s for long emails). Works with French emails. Draft replies not hallucinated.

**Verdict**: gemma4:e4b with the system prompt + few-shot pattern works great for both triage and draft replies. Best overall local model.

### Results with granite4:3b-h

#### Real Gmail test (17 emails)

| Email | Category | Correct |
|---|---|---|
| Get updates about ChatGPT | INFORMATIONAL | ✅ |
| Alerte de sécurité | SECURITY_ALERT | ✅ |
| Lunch on Wednesday? | ACTION_REQUIRED | ✅ |
| DevFest talk - need your confirmation | ACTION_REQUIRED | ✅ |
| Q3 Platform Migration | ACTION_REQUIRED | ✅ |
| test 3 | INFORMATIONAL | ✅ |
| Take care of your mind this Health Day | INFORMATIONAL | ✅ |
| Jump into Jira with Ease | INFORMATIONAL | ✅ |
| JIRA Database Latency Spike (x7) | URGENT | ⚠️ |
| JIRA Database Latency Spike (x1) | INFORMATIONAL | ✅ |

**8/8 on non-JIRA emails.** Fast (~4-5s per email). JIRA "Database Latency Spike" strongly biased toward URGENT (7/8). Draft reply triggered for Q3 Migration (needsReply not fixed).

### Results with granite4:7b-a1b-h

#### Real Gmail test (22 emails)

| Email | Category | Correct |
|---|---|---|
| Get updates about ChatGPT | INFORMATIONAL | ✅ |
| Alerte de sécurité (x2) | SECURITY_ALERT | ✅ |
| Lunch on Wednesday? | ACTION_REQUIRED | ✅ |
| DevFest talk - need your confirmation | ACTION_REQUIRED | ✅ |
| Q3 Platform Migration | ACTION_REQUIRED | ✅ |
| test 3 | INFORMATIONAL | ✅ |
| Take care of your mind this Health Day | INFORMATIONAL | ✅ |
| Jump into Jira with Ease | INFORMATIONAL | ✅ |
| JIRA Database Latency Spike (x10) | ACTION_REQUIRED | ⚠️ |
| Admin invited you to join Jira | INFORMATIONAL | ✅ |
| Passez en revue les paramètres Google | INFORMATIONAL | ✅ |
| Conditions d'utilisation | INFORMATIONAL | ✅ |
| Politique de Confidentialité | INFORMATIONAL | ✅ |
| Mise à jour règlement inactivité | INFORMATIONAL | ✅ |
| Vérifiez paramètres confidentialité | INFORMATIONAL | ✅ |
| Thank you for your purchase! | PURCHASE | ✅ |

**12/12 on non-JIRA emails** including PURCHASE and many French emails. Fast (~4s per email). JIRA "Database Latency Spike" consistently ACTION_REQUIRED (not INFORMATIONAL but at least consistent). Draft reply triggered for Q3 Migration.

### granite4:32b-a9b-h

Too slow even with `--think=false`. Not practical for local use.

## Conclusion

Small local models (4B-9B) on Ollama's `/v1/` endpoint **do not work** with classification logic embedded in JSON schema field descriptions. The rationale and enum values are generated independently, leading to correct reasoning but wrong categories.

The **system prompt + few-shot examples + low temperature** pattern fixes this. Moving classification rules and examples to the system prompt, and keeping schema descriptions minimal, allows small models to correctly map their reasoning to enum values.

### Recommended setup for local LLMs

- **Model**: gemma4:e4b via Ollama (best overall: accurate triage + good draft replies)
- **Alternative**: ministral-3:8b via Ollama (fastest triage, but hallucinates on draft replies)
- **Pattern**: System prompt with category definitions, classification rules, and few-shot examples
- **Temperature**: 0.15
- **Schema**: Minimal descriptions, format only (no classification logic in schema)
- **Few-shot examples**: 6 examples covering different categories (INFORMATIONAL, ACTION_REQUIRED, SECURITY_ALERT, SHIPPING, social/casual requests, and team-wide announcements)

### Key insight

With local models on Ollama, classification logic must go in the **system prompt**, not in JSON schema field descriptions. The schema should only define the output format (types, enum values). Few-shot examples in the system prompt are critical for consistent results.