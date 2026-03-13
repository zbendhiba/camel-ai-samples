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
5. Export the credentials as environment variables:

```bash
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWD="xxxx xxxx xxxx xxxx"
```

## Prerequisites

Install the [Camel JBang CLI](https://camel.apache.org/manual/camel-jbang.html) version 4.18.0:

```bash
jbang app install -Dcamel.jbang.version=4.18.0 camel@apache/camel
```

## Running

```bash
cd email-triage-agent
camel run email-triage.camel.yaml --dev --logging-level=info
```

## Status

Work in progress — prototyping with Kaoto and Camel YAML DSL.
