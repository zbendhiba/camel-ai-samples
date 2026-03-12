# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A monorepo of sample projects demonstrating Apache Camel + AI integrations, built on Quarkus (Java 21, Maven). Each subdirectory is a standalone Quarkus application with its own `pom.xml` and Maven wrapper.

**Current focus:** Building new agent demos using Apache Camel together with Kaoto and Forage. The existing projects below are legacy examples kept for reference but are not the active work.

## Related Local Projects

- **Kaoto** — `/Users/zbendhib/dev/kaoto` — Visual editor for Camel routes.
- **Forage** — `/Users/zbendhib/dev/forage` — Agent framework for Camel.
- **Camel components** — `/Users/zbendhib/dev/camel/components` — 333 components available as potential agent tools.
- **Wanaku** — `/Users/zbendhib/dev/wanaku/capabilities` — MCP router that can expose Camel routes and other services as MCP tools.

## Active Projects

- **email-triage-agent** — Personal AI agent that reads IMAP mailbox, classifies urgency, summarizes emails, and can forward summaries to Slack. Tools: `camel-mail`, `camel-slack`, `camel-pdf`.
- **travel-alert-agent** — AI agent that monitors travel advisories (RSS) and weather, notifies via Telegram or Slack. Tools: `camel-rss`, `camel-weather`, `camel-telegram`/`camel-slack`.

## Legacy Projects (kept for reference)

- **camel-jira-ai** — Camel routes that fetch JIRA issues and generate AI summaries via LangChain4j (OpenAI). Uses Camel's `langchain4j-chat` and `langchain4j-tools` components to create RAG-style JIRA summarization with tool-calling support.
- **camel-jira-mcp** — MCP (Model Context Protocol) server exposing Camel routes as tools via `quarkus-mcp-server-stdio`. Demonstrates bridging MCP tool calls to Camel `ProducerTemplate`.
- **camel-python-samples/camel-python-dumb1** — Minimal Camel REST route called from a Python script. Intended as a native executable.

## Build & Run Commands

All commands must be run from within each project directory (e.g., `cd camel-jira-ai`).

```bash
# Dev mode (hot reload)
./mvnw compile quarkus:dev

# Package
./mvnw package

# Run tests
./mvnw test

# Native build
./mvnw package -Dnative

# Native build without GraalVM (uses container)
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## Environment Variables (camel-jira-ai)

Requires JIRA Cloud credentials:
```
JIRA_PROJECT_KEY, JIRA_URL, JIRA_USERNAME, JIRA_ACCESS_TOKEN
```

## Camel MCP Server

A Camel MCP server is configured (`.mcp.json`) and provides access to the full Apache Camel catalog documentation. Use it when creating or editing Camel routes to:
- Browse components, dataformats, EIPs, kamelets, and languages
- Look up component options and EIP documentation
- Validate and transform Camel routes
- Get route context and hardening suggestions

## Key Architecture Notes

- There is no parent POM — each project is fully independent with its own Quarkus BOM imports.
- Camel routes are defined as `RouteBuilder` subclasses annotated with `@ApplicationScoped`.
- camel-jira-ai uses LangChain4j's RAG aggregator strategy to enrich AI prompts with JIRA issue data, and `langchain4j-tools` routes to expose Camel endpoints as callable AI tools.
- camel-jira-mcp uses the `@Tool`/`@ToolArg` annotations from `quarkus-mcp-server` and invokes Camel routes via `ProducerTemplate`.
- Integration tests are skipped by default (`skipITs=true`); enabled only in the `native` profile.
