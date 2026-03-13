# Email Triage Agent

A personal AI agent that reads your Gmail inbox, classifies emails by urgency, summarizes content, and can draft replies.

## Concept

Ask the agent things like:
- "What's urgent in my inbox?"
- "Summarize my unread emails from today"
- "Draft a reply to the email from Alice about the deadline"

The agent reads unread emails, classifies them by urgency, summarizes content, and can take action.

## Camel Components Used

- **camel-google-mail** — Read inbox and manage labels via Gmail API
- **camel-langchain4j-agent** — AI-powered email classification and summarization

## Architecture

- Camel JBang + LangChain4j + Forage for the agent runtime
- Kaoto for visual route design
- No database required — connects directly to Gmail via OAuth2

## Prerequisites

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

Install the [Camel JBang CLI](https://camel.apache.org/manual/camel-jbang.html) version 4.18.0:

```bash
jbang app install -Dcamel.jbang.version=4.18.0 camel@apache/camel
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
4. You can publish the application later — test mode is fine for now

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
7. If you see "Google has not verified this application", click **Continue** — this is normal in dev/test mode
8. Confirm the permissions when prompted
9. Click **Exchange authorization code for tokens**
10. Copy the **Refresh Token**

### 5. Enable the Gmail API

On the first run, you may get a `PERMISSION_DENIED` error with a message like:

> Gmail API has not been used in project XYZ before or it is disabled.

Go to the URL provided in the error message and click **Enable** to activate the Gmail API for your project. Wait a few minutes and retry.

### 6. Configure credentials

Add your OAuth2 credentials to `application.properties`:

```properties
camel.component.google-mail-stream.client-id=your-client-id
camel.component.google-mail-stream.client-secret=your-client-secret
camel.component.google-mail-stream.refresh-token=your-refresh-token
```

## Running

```bash
cd email-triage-agent
camel forage run *
```

## Status

Work in progress — prototyping with Kaoto and Camel YAML DSL.
