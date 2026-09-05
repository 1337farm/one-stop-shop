# Architecture

| Component | Core Technology | Primary Responsibility |
|---|---|---|
| Native Host & Container | Kotlin, PRoot (ARM64), Ubuntu Base Core rootfs | Isolated Linux userland running on unrooted Android devices with zero-drop foreground service lifecycle. |
| Agent Orchestration Daemon | Rust (tokio, rig-core, static MUSL aarch64) | Type-safe LLM tool routing, process execution, WebSocket API, and autonomous execution loop. |
| Context & Task Tracking | Markdown / JSONL / LMDB | Visual, human-steerable Kanban board and surgical CLI context extraction. |
| Dynamic Memory Engine | llama.cpp (1B–2B GGUF) or cheap cloud LLM pass | Heuristic evaluation of agent progress, context pruning, and automated macro-milestone retrospectives. |
| Workspace Frontend | HTML5 / TypeScript / CSS (WebView) | Decoupled client communicating strictly over localhost WebSockets for cross-platform portability. |