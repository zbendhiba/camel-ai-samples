# Travel Alert Assistant

An AI agent that monitors travel advisories and weather conditions, and can notify you via Telegram or Slack.

## Concept

Ask the agent things like:
- "Should I still fly to Madrid on Friday?"
- "What's the weather like in Tokyo this week?"
- "Are there any travel advisories for Egypt?"

The agent checks weather and RSS feeds for disruptions, responds with a summary, and optionally sends a notification.

## Camel Components Used as Tools

- **camel-rss** — Fetch travel advisories from RSS feeds
- **camel-weather** — Check destination weather conditions
- **camel-telegram** or **camel-slack** — Send alerts and summaries

## Architecture

- Quarkus + Camel + LangChain4j (or Forage) for the agent runtime
- Camel routes exposed as AI tools (via langchain4j-tools or Wanaku MCP)
- Kaoto for visual route design
- No database required — all read-only external APIs + one notification channel

## Status

Not yet implemented.
