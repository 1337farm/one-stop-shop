# Phase 5: Client-Server Decoupling & Cross-Platform Roadmap

## Phase Breakdown & Milestones
| Phase | Milestone Target | Core Deliverables |
|---|---|---|
| Phase 1 | Container & Asset Bootstrap | Kotlin asset extractor, PRoot rootfs unpacker, Foreground Service wake-lock implementation, OAuth credential injector. |
| Phase 2 | Native Rust Orchestrator | rig-core loop, CLI process executor (ripgrep/git), WebSocket server, and structured tool handling. |
| Phase 3 | Kanban & Workspace UI | Visual drag-and-drop board, terminal output stream, and manual human-steering injection panel. |
| Phase 4 | Macro-Memory Engine | Local llama.cpp daemon, heuristic evaluation gatekeeper, and automated AGENTS.md milestone retros. |
| Phase 5 | Desktop & Cross-Platform | Decouple UI via Tauri for desktop (macOS/Linux/Windows) and establish remote-workspace protocol for iOS. |

## Platform Portability Strategy
 * Maintain a strict boundary: the frontend client communicates exclusively through localhost WebSockets, utilizing relative paths without Android-specific API bridges.
 * Deploy the identical web interface across Desktop via Tauri or Electron by launching the Rust daemon as a native background process.
 * Support server/cloud hosting via Docker containers for remote browser access.
 * For iOS deployment, run the interface as a thin client connecting over secure WebSockets/SSH to a remote daemon, avoiding Apple App Store restrictions against local user-space container emulation.