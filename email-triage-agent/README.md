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

## Status

Not yet implemented.
