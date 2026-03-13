# Email Triage Agent

A personal AI agent that reads your mailbox, summarizes unread emails, classifies urgency, and can draft replies or forward summaries to Slack.

## Concept

Ask the agent things like:
- "What's urgent in my inbox?"
- "Summarize my unread emails from today"
- "Draft a reply to the email from Alice about the deadline"
- "Forward a summary of today's emails to my Slack channel"

The agent reads unread emails, classifies them by urgency, summarizes content, and can take action.

## Camel Components Used as Tools

- **camel-mail** — Read inbox via IMAP, send replies via SMTP
- **camel-slack** — Post summaries to a Slack channel
- **camel-pdf** — Parse email attachments (PDFs)

## Architecture

- Quarkus + Camel + LangChain4j (or Forage) for the agent runtime
- Camel routes exposed as AI tools (via langchain4j-tools or Wanaku MCP)
- Kaoto for visual route design
- No database required — connects to an existing IMAP mailbox

## Gmail Setup

1. Log in to your Google account at https://myaccount.google.com/security
2. Make sure **2-Step Verification** is enabled. If not, enable it first.
3. Once 2FA is active, go to https://myaccount.google.com/apppasswords
4. Create a new App Password named `camel-email-agent`
5. Copy the generated 16-character password into `application.properties`:

```properties
mail.username=your-email@gmail.com
mail.password=xxxx xxxx xxxx xxxx
```

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

## Running

```bash
cd email-triage-agent
camel forage run *
```

## Status

Work in progress — prototyping with Kaoto and Camel YAML DSL.
