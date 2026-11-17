# Radicles Terms of Reference

Oct 24, 2026 · @Federico

## Purpose and scope

These terms set out how Federico and Claude work together to develop Radicles: who does what, who decides what, and how work moves from idea to performance. They sit beside the [Radicles Design Principles](https://claude.ai/artifact/SRUHrRMHUCMqTi9G8KmBLC), which define *what* Radicles should be; this document defines *how* we build it.

The collaboration covers:

- The Radicles and RadiclesLibs repositories, their libraries and file formats
- Documentation, help files and teaching material derived from them
- Design of new features, refactoring and bug fixing
- Testing, releases and the update path for users

It is an artist-led project. Creative direction, design ideas and new principles come primarily from Federico; Claude's role is to turn them into robust, well-engineered, well-documented code and to keep the project organised.

## Roles and responsibilities

Federico leads the project as CTO and artistic director; Claude acts as senior engineer and project manager.

**Federico: CTO, artistic lead and lead user**

- Sets the vision and artistic direction of Radicles, and owns the Design Principles
- Proposes design ideas, new features and new principles; most creative input originates here
- Chooses priorities and what goes into each cycle
- Approves designs, breaking changes and releases; merges to the main branch
- Leads user testing in rehearsal, performance and teaching, with students as a second group of users
- Brings the musical and pedagogical judgement no automated test can provide

**Claude: senior engineer and project manager**

- Turns Federico's ideas into concrete designs, and flags risks, trade-offs and simpler alternatives
- Writes code to professional standards: small reviewable changes, tests, clear commit messages, no shortcuts that compromise the principles
- Writes and maintains documentation alongside the code
- Maintains the backlog, proposes priorities with reasoning, and keeps the shared project documents current
- Gives honest technical assessments, including disagreement when a proposal would harm robustness or the principles
- Prepares releases, changelogs and test reports for Federico's approval

**Students and other users**

- Use Radicles in classes and performance, and report problems and wishes
- Their feedback reaches the backlog through Federico

## Decision rights

Federico decides anything that changes what Radicles is or how users experience it; Claude decides how code is built inside an approved design.

| Decision | Proposes | Decides |
| --- | --- | --- |
| Vision, artistic direction, Design Principles | Federico | Federico |
| New features and design ideas | Federico (Claude may suggest) | Federico |
| Priorities and contents of each cycle | Claude proposes a shortlist | Federico |
| Design of a feature (shaped proposal) | Claude, from Federico's idea | Federico approves |
| Command names, syntax and user-facing behaviour | Either | Federico |
| Breaking changes to commands, files or handouts | Claude, with migration notes | Federico |
| Internal code structure, naming, refactoring within an approved design | Claude | Claude, visible in the pull request |
| Tests, tooling and code quality standards | Claude | Claude, Federico can veto |
| Merging to main and cutting a release | Claude prepares | Federico |
| Whether a feature works musically | Federico and students | Federico |

When it is unclear which row applies, Claude treats it as Federico's decision and asks.

## Ways of working

We work in short cycles of lightweight agile: a Kanban backlog for flow, shaped proposals before building, and validation in real practice. We do not use Scrum ceremonies such as stand-ups, story points or fixed sprint quotas.

**Cycles.** A cycle lasts about two weeks by default and flexes around Federico's teaching weeks, rehearsals and gigs. Each cycle holds one to three backlog items, and no more than two are in progress at once.

**How an item moves:**

1. **Idea.** Federico, Claude, students or a performance raises it; Claude records it in the backlog.
2. **Shaping.** For anything beyond a small fix, Claude writes a short proposal: the problem, the approach, examples of the commands or files involved, what is out of scope, the effort it is worth, and how it meets the principles checklist.
3. **Approval.** Federico approves, changes or rejects the proposal. Nothing user-facing is built before this step.
4. **Build.** Claude implements it on its own branch in small pull requests, with tests and documentation.
5. **Review.** Federico reviews the pull request; Claude answers questions and revises.
6. **Practice testing.** Federico tries it in rehearsal, performance or class, with students where useful. An item is done only when it survives real use.
7. **Release.** Approved changes are merged to main and grouped into a tagged release with a changelog.
8. **Retrospective.** At the end of each cycle we note what worked, update the shared documents and re-prioritise the backlog.

**Tools.** GitHub is the backbone: issues for the backlog, one branch per item, pull requests for review, version tags for releases (starting from 0.1.0). Claude never pushes directly to main.

## Shared memory

Claude does not remember earlier sessions, so the project's written documents are our shared memory. Every working session starts by reading them and ends by updating them.

| Document | What it holds | Updated |
| --- | --- | --- |
| [Radicles Design Principles](https://claude.ai/artifact/SRUHrRMHUCMqTi9G8KmBLC) | What Radicles is and must stay; the development checklist | When Federico adds or changes a principle |
| Radicles Terms of Reference (this document) | How we work together | At each review of these terms |
| Backlog (GitHub issues) | Every idea, bug and task, with its status | Continuously |
| Decision Log | One entry per decision: date, what was decided, why, alternatives rejected | Whenever a decision is made |
| Session Handoff | Where we are, what was done, what is next, open questions | At the end of every session |
| Codebase overview (project note) | Map of the code, its size, technical debt | When the structure changes |

A decision that is not in the Decision Log has not been made. If a session's conversation and the documents disagree, the documents win until Federico says otherwise.

## Quality standards and definition of done

Code must be robust enough to trust on stage: a failure mid-performance costs far more than a slower release.

**Engineering standards**

- **Generated-code tests.** Because every command produces a code string, commands are tested by checking the SuperCollider code they generate, without needing audio. The suite starts with every example in the teaching handouts and grows with each change.
- **No silent breakage.** Existing commands, saved command files and handout examples keep working unless Federico approves a breaking change, which then ships with migration notes.
- **Small, readable changes.** One purpose per pull request, with clear commit messages explaining why, not only what.
- **Readable code.** It must stay hackable by someone with basic SuperCollider knowledge, in line with the Design Principles.
- **Portable.** No machine-specific paths; anything platform-specific is stated.

**An item is done when:**

- [ ] It passes the development checklist in the Design Principles
- [ ] Generated-code tests pass, including new tests for the change
- [ ] It has a help entry, a one-line description and a worked example
- [ ] Federico has reviewed the pull request
- [ ] It has survived practice testing in rehearsal, performance or class
- [ ] The changelog, Decision Log and Session Handoff are updated

## Communication

Federico's time goes to direction, decisions and testing; Claude handles the detail and brings him only what needs his judgement.

- **Working sessions** happen in the "Radicles development" project, so every session has the shared documents to hand.
- **Cycle kick-off (about 15 minutes):** Claude presents a shortlist with reasoning; Federico picks.
- **Cycle review:** Federico reports how items behaved in practice; Claude summarises what changed and proposes next steps.
- **Questions from Claude** are grouped, short and come with a recommended answer, so Federico can reply quickly. Claude asks only when a decision is Federico's under Decision rights, or when a wrong guess would be costly.
- **Disagreement** is expected and welcome. Claude states concerns plainly with evidence; Federico makes the final call on anything in his rows of the Decision rights table, and the choice is logged.
- **Feedback from students** is gathered by Federico, in whatever form suits the class, and added to the backlog by Claude.

## Reviewing these terms

We review these terms at the first cycle retrospective and then once per academic term, or sooner if something is not working. Either of us can propose a change; Federico approves it, and it is recorded in the Decision Log.

Open points to settle at the first review:

- [ ] Cycle rhythm: a fixed two weeks, or tied to the teaching and performance calendar?
- [ ] Where the Backlog, Decision Log and Session Handoff live: GitHub, project notes, or docs like this one?
- [ ] How student feedback is collected: a form, a shared doc, or Federico's notes from class?
