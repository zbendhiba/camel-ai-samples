# Email Triage Agent

A personal AI agent that reads your Gmail inbox, classifies emails into categories (urgent, informational, purchase, shipping, etc.), summarizes their content, and automatically moves them to the corresponding Gmail label.

## What It Does

The agent polls your Gmail inbox for unread emails, sends each one to a local LLM (Ollama) for classification, and moves the email to a matching Gmail label based on the result.

Categories:
- **URGENT**:Requires immediate action (deadlines, incidents, escalations)
- **ACTION_REQUIRED**:Needs a response but is not time-sensitive
- **INFORMATIONAL**:FYI, newsletters, automated notifications
- **SUSPICIOUS**:Spam, phishing attempts, unsolicited offers
- **PURCHASE**:Purchase confirmations, invoices, receipts
- **SHIPPING**:Package tracking, delivery notifications

## Camel Components Used

- **camel-google-mail-stream**:Poll Gmail inbox for unread emails
- **camel-google-mail**:Move emails to labels via the Gmail API
- **camel-langchain4j-agent**:AI-powered email classification and summarization (via Forage + Ollama)
- **camel-jsonpath**:Extract structured fields from LLM JSON responses
- **commons-text**:HTML entity decoding via `StringEscapeUtils.unescapeHtml4()`

## Architecture

- Camel JBang + LangChain4j + Forage for the agent runtime
- Kaoto for visual route design
- Ollama for local LLM inference
- No database required:connects directly to Gmail via OAuth2

## Project Structure

- **email-triage.camel.yaml**:Main Camel routes:
  - `triage-email-main-agent`:Reads Gmail, cleans the body, sends it to the LLM, extracts the category
  - `handle-triaged-email`:Moves the email to the matching Gmail label
  - `draft-reply`:Generates and saves a draft reply

  The routes use two built-in Camel DataType Transformers from `camel-google-mail`: `google-mail:update-message-labels` (add/remove labels) and `google-mail:draft` (create a draft reply). These transformers are available in Camel 4.19.0-SNAPSHOT and will ship with the 4.19 release.
- **HtmlDecodeFunction.java**:Custom Camel Simple function `${htmlDecode()}` that sanitizes email content before sending it to the LLM. See [Clean Email Subject and Body](#clean-email-subject-and-body) for details.
- **forage-agent-factory.properties**:Forage configuration for the Ollama LLM agent (model name, base URL)
- **application.properties**:Gmail OAuth2 credentials template (both consumer and producer)

## Prerequisites

### Ollama

Install [Ollama](https://ollama.com/) and pull the model:

```bash
ollama pull gemma3:4b
```

I tested several local LLMs. Gemma3:4b won. See the [blog post](https://zinebbendhiba.com/posts/i-tested-local-llms-to-triage-my-gmail-here-s-what-worked/) for the full comparison.

If your machine has more resources, you can use larger models (e.g. `gemma3:12b`, `qwen3.5:27b`). You can also use cloud-based models like Gemini or GPT by updating `forage-agent-factory.properties`.

### Maven Snapshots Repository

Forage is not yet released in a final version. You need to add the Central Portal Snapshots repository to your Maven `~/.m2/settings.xml`:

```xml
<repository>
    <id>central-portal-snapshots</id>
    <name>Central Portal Snapshots</name>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <releases>
        <enabled>false</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

### Camel JBang CLI

Install the [Camel JBang CLI](https://camel.apache.org/manual/camel-jbang.html). This example requires Camel 4.19.0-SNAPSHOT until 4.19 is released, because it uses DataType Transformers that are not yet available in a stable release:

```bash
jbang app install -Dcamel.jbang.version=4.19.0-SNAPSHOT camel@apache/camel
```

### Forage Plugin

Install the Forage plugin for Camel JBang:

```bash
camel plugin add --gav io.kaoto.forage:camel-jbang-plugin-forage:1.1-SNAPSHOT forage
```

## Gmail API Setup

### 1. Create a Google Cloud project

1. Go to the [Google Cloud Console](https://console.cloud.google.com/apis/dashboard)
2. If you don't have a project yet, create one (e.g. `triage-email`)
3. Select the project from the project dropdown

### 2. Configure the OAuth consent screen

1. Go to **APIs & Services** > **OAuth consent screen**
2. Fill in the form if no consent screen is configured yet
3. Go to **Audience** > **Add test users** and add your Gmail address
4. You can publish the application later:test mode is fine for now

### 3. Create OAuth2 credentials

1. Go to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **OAuth client ID**
3. Select **Web application** as the application type
4. Under **Authorized redirect URIs**, add: `https://developers.google.com/oauthplayground`
5. Click **Create** and save the **Client ID** and **Client Secret**

### 4. Get a Refresh Token

1. Go to the [OAuth 2.0 Playground](https://developers.google.com/oauthplayground/)
2. Click the gear icon (Settings) in the top right corner
3. Check **Use your own OAuth credentials**
4. Enter your **Client ID** and **Client Secret**
5. In the list on the left, select **Gmail API v1** and check `https://mail.google.com/`
6. Click **Authorize APIs** and sign in with your Google account
7. If you see "Google has not verified this application", click **Continue**:this is normal in dev/test mode
8. Confirm the permissions when prompted
9. Click **Exchange authorization code for tokens**
10. Copy the **Refresh Token**

### 5. Enable the Gmail API

On the first run, you may get a `PERMISSION_DENIED` error with a message like:

> Gmail API has not been used in project XYZ before or it is disabled.

Go to the URL provided in the error message and click **Enable** to activate the Gmail API for your project. Wait a few minutes and retry.

### 6. Create Gmail labels

The agent moves triaged emails into Gmail labels based on their category. You need to create these labels manually in your Gmail account:

1. Go to [Gmail](https://mail.google.com/)
2. In the left sidebar, scroll down and click **More** > **Create new label**
3. Create the following labels:
   - `URGENT`
   - `ACTION_REQUIRED`
   - `INFORMATIONAL`
   - `SUSPICIOUS`
   - `PURCHASE`
   - `SHIPPING`

### 7. Configure credentials

Add your OAuth2 credentials to `application.properties` for both the stream consumer and the producer:

```properties
# Consumer (reads inbox)
camel.component.google-mail-stream.clientId=your-client-id
camel.component.google-mail-stream.clientSecret=your-client-secret
camel.component.google-mail-stream.refreshToken=your-refresh-token

# Producer (moves emails to labels)
camel.component.google-mail.clientId=your-client-id
camel.component.google-mail.clientSecret=your-client-secret
camel.component.google-mail.refreshToken=your-refresh-token
```

## Running

```bash
cd email-triage-agent
camel forage run * --dependency=mvn:org.apache.commons:commons-text:1.13.0
```

## Design Notes

### Clean Email Subject and Body

Email subjects can contain HTML entities (e.g. `&apos;` in "OpenAI&apos;s Privacy Policy"). Email bodies are polluted with massive tracking URLs that can be thousands of characters long. Without cleaning, this noise overwhelms the LLM and breaks JSON output.

A custom Camel Simple function `${htmlDecode()}` (`HtmlDecodeFunction.java`) handles all the cleaning. Both subject and body are cleaned by chaining this function with the Simple `~>` operator:

```yaml
- setVariable:
    name: cleanedEmailBody
    simple:
      expression: ${body} ~> ${htmlDecode()}
```

The function performs the following steps:
1. **Strip HTML tags**:removes any remaining HTML markup (`<[^>]+>`)
2. **Strip URLs**:removes tracking URLs in parentheses (`( http://... )`) and bare URLs, which can be thousands of characters of noise
3. **Decode `&apos;`**:handled manually because `StringEscapeUtils.unescapeHtml4()` from commons-text does not support this XML entity
4. **Decode HTML entities**:converts `&amp;`, `&lt;`, `&gt;`, `&quot;`, and all other HTML4 entities via commons-text
5. **Collapse whitespace**:replaces multiple consecutive spaces left by tag/URL stripping

### URL Stripping as a Security Measure

Stripping URLs also defends against indirect prompt injection. A crafted email could embed malicious instructions inside a URL. By removing all URLs before classification, the LLM only sees text content, never links.

### LLM Output Format

Some local models (e.g. Gemma3) wrap JSON responses in markdown code fences even when told not to. Adding explicit negative instructions in the prompt ("Do NOT wrap it in \`\`\`json\`\`\` or any markdown") fixed this without needing post-processing.

### Defense in Depth for needsReply

A regex filter acts as a safety net on top of the LLM. Even if the model says `needsReply=true`, the agent won't draft a reply if the sender matches `noreply`, `notifications`, `jira`, `github`, etc.

### Fire-and-Forget for Draft Replies

The main route uses Camel's Wire Tap pattern to move the email to its label immediately and generate the draft reply asynchronously. The inbox gets organized in seconds. The draft appears a few seconds later.

