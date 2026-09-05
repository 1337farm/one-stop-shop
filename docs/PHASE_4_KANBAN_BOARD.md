# Phase 4: Hybrid Kanban Steering Board & CLI Pipeline

## Board State Engine
 * Model tasks, blockers, in-review states, and completions as structured Markdown files with YAML frontmatter stored in a .kanban/ directory or an append-only LMDB metadata store.
 * Provide lock-free, zero-copy concurrent reads for both the web interface and background CLI processes.

## Steering & Autonomous Execution
 * Autonomous Worker Mode: The Rust daemon polls the board, picks up pending tasks, creates isolated copy-on-write workspace branches, and begins execution.
 * Human-in-the-Loop Steering: Expose visual injection points in the frontend UI allowing developers to re-order cards, pause active runs, edit constraints, or inject prompt steers directly into the agent's active execution loop.
 * Status Transitions: The agent automatically updates task metadata from In Progress to In Review upon passing localized test scripts inside PRoot.