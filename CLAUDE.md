# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Purpose

This is a documentation repository for interview preparation. It contains a single Markdown document describing a real-world microservices architecture migration story using the STAR method (Situation → Task → Action → Result).

## Primary Document

**`architecture and decoupling.md`** — A detailed technical narrative of extracting a Reporting microservice from a Java monolith. Covers:

- Java 11 / Spring MVC monolith with shared PostgreSQL schema
- 10-step decoupling process (Strangler Fig → Kafka events → CQRS → Elasticsearch read model)
- Resulting Spring Boot 3.x microservice (Kafka, Elasticsearch, Redis, Kubernetes)
- 11 Mermaid diagrams throughout (C4 context, sequence, architecture, Kafka pipeline, K8s topology)
- Outcome metrics: 120s → <2s report time, 30x deployment frequency

## Mermaid Diagrams

All diagrams use standard Mermaid syntax. To preview:
- **VS Code**: install the "Mermaid Preview" or "Markdown Preview Mermaid Support" extension
- **GitHub/GitLab**: renders automatically in markdown preview
- One diagram uses `C4Context` — requires the C4 Mermaid plugin or a compatible renderer


## Claude Code Workflow

- Always use the **Context7 MCP** proactively when you need library/API documentation, code generation, or setup steps — don't wait to be explicitly asked
- When generating commit messages, do NOT add `Co-Authored-By: Claude` trailers
- When asked to `commit`, generate a semantic commit message (max 80 characters), stage relevant changes, and create the commit — no `Co-Authored-By` trailer
- When opening a URL in the browser that returns raw JSON, always apply pretty-print with syntax highlighting using `page.evaluate()` to inject a dark-themed HTML page with colored keys, strings, numbers, and nulls — do not wait to be asked
