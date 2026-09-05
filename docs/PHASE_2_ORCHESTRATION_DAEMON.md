# Phase 2: Custom Rust Agent Orchestration Daemon

## Core Architecture & Provider Integration
 * Replace external orchestrator binaries with an in-house static Rust binary compiled for aarch64-unknown-linux-musl deployed inside the container's /usr/local/bin/.
 * Integrate rig-core to manage unified API completions across DeepSeek, OpenRouter, and local OpenAI-compatible endpoints.
 * Implement an asynchronous JSON-RPC / WebSocket transport over tokio to serve state changes and stream tokens to the frontend client.

## Tool Execution Engine
 * Derive tools using typed Rust structs (rig_core::tool::PortableTool) for compile-time schema validation and zero-cost serialization.
 * Host CLI Dispatcher: Directly invoke native Linux utilities (ripgrep, git, bash) via tokio::process::Command inside the PRoot environment, capturing structured stdout/stderr.
 * WASM Safety Sandbox: Embed wasmi or wasmtime inside the Rust daemon with strict fuel consumption (CPU instruction budgeting) and memory limits to execute ad-hoc, untrusted agent data-transformation scripts.