# Phase 3: Dynamic Macro-Memory & Milestone Distillation

## Dynamic Heuristic Evaluator
 * Reject rigid turn counters; deploy a lightweight 1B to 2B model (e.g., Llama-3.2-1B or Qwen-2.5-1.5B via llama.cpp ARM NEON builds) or a cheap cloud LLM pass.
 * Stream ongoing agent execution traces to this evaluator, which acts as a semantic gatekeeper to detect architectural shifts, resolved blockers, or errors.
 * Prune noisy terminal retries and compilation noise from the primary context window once evaluated.

## Milestone Retrospectives
 * Trigger an automated summarization pass whenever a task hits a designated milestone on the work board.
 * Condense session context into macro-level architectural decisions and system invariants, appending them to a persistent AGENTS.md file or a local LanceDB columnar store.
 * Enforce surgical retrieval during active development: require the primary agent to query ripgrep or indexed macro-memory files rather than stuffing entire raw files into context.