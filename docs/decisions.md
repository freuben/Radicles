# Decisions

One line per decision, newest first: the date, what was decided, and where
it is written up. Design notes and PRs hold the reasoning; this page is the
index, so a decision can be found without reading a conversation.

| Date | Decision | Where |
| --- | --- | --- |
| 18 Nov 2026 | The Ndefs own the values; the document tracks structure and what a session needs. The "ownership switch" is dropped. | `implementation-plan.md`, "Order of work after the 18 Nov review" |
| 18 Nov 2026 | Sessions before the GUI rebuild; the GUI later, as a client (another computer, perhaps a browser). Points after sessions. | same |
| 18 Nov 2026 | A command during a load or a morph: structural commands wait their turn; a value command wins for its own parameter; a new load or morph on the same strip stops the old one. | same; built in 5a |
| 18 Nov 2026 | After a server crash the session should be rebuilt (autosave and recovery move into the sessions step). | same |
| 18 Nov 2026 | `dev` is released to `master` after every step; Claude raises it. | `CLAUDE.md`, "Releases" |
| 18 Nov 2026 | Every piece of work names its model: Fable for design, the engine, sessions, the parser and reviews; Opus for work that is easy to check. The session stays on Fable; Claude hands Opus work to Opus subagents itself. | `CLAUDE.md`, "Which model does what" |
| 18 Nov 2026 | Claude may merge into `dev` itself when no snapshot changes and the tests pass. | `CLAUDE.md`, "How we work" |
| 18 Nov 2026 | Tests run automatically on GitHub (Actions). | plan, row D |
| 18 Nov 2026 | Performances and test runs are at 48 kHz. | `CLAUDE.md`, "Tests" |
| 18 Nov 2026 | `tload default`: no source, output to the master; `nosrc` keeps the source. | `design/step4-track-presets.md`, #102 |
| 17 Nov 2026 | A track preset keeps the output to the master as it was saved. | `design/step4-track-presets.md`, #101 |

Earlier decisions are in the design notes of each step (`design/`), under
"Decisions" or "Questions for Federico".
