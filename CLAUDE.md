# Radicles – working notes for Claude

Radicles is a live-coding and performance environment for SuperCollider 3.14.1
(a Quark). Federico Reuben is its author, artistic lead and lead user; Claude is
senior engineer and project manager. This file holds what every coding
session needs.

**Design notes:** the decided ones are in `docs/` (start with
`docs/status.md` and `docs/design-principles.md`; the plan is
`docs/implementation-plan.md`, each step's note is in `docs/design/`). New
notes are drafted and discussed in the claude.ai Project "Radicles
development" and copied to `docs/` once decided, so a newer draft may exist
there; if `docs/` and a draft disagree, ask before building.

## How we work

- **Design before building.** Each step gets a short design note in the
  claude.ai Project with questions for Federico; build only once he has
  answered, then copy the decided note into `docs/design/` (with the code's
  PR or its own). Small fixes he asks for directly don't need one.
- **Branches and PRs.** Branch from `dev`; open PRs into `dev`; one PR per
  coherent change, each with its tests. Never push to or merge into `master`
  (the stable version for performances). Federico merges.
- **Claude may merge into `dev` itself** only when the PR changes no snapshot
  and the tests pass: test tooling, refactors, dead code, docs fixes. Anything
  a performer would hear or see, any name or message, and any change to what
  a preset or session keeps waits for Federico (agreed 18 Nov 2026).
- **PR descriptions** in plain language: what changes for the performer, the
  decisions it follows, tests, cloud and Mac results.
- **Commands people already type don't change** unless agreed. New behaviour
  comes with tests, checked to fail without the change.
- **Stop and ask** when a decision is Federico's (behaviour performers will
  notice, naming, anything hard to undo); otherwise take the sensible default
  and say so in the PR.
- **PR descriptions end with "Try this":** a few lines to type and what
  Federico should hear or see. He tries the behaviour; he isn't expected to
  read the diff.

## Releases: `dev` to `master`

`master` must not fall behind: Federico tests on the performance Mac, and
that only gets `master`. Raising this is Claude's job, not his (agreed 18 Nov
2026, after `master` had fallen 112 commits behind).

- **At the start of every session,** check
  `git rev-list --count origin/master..origin/dev`. If a step or a batch of
  fixes has landed since the last release, or it is more than about a week of
  work, say so first and offer to prepare the release.
- **At the end of every step or batch of fixes,** open a `dev` → `master` PR
  with a "try this on the performance Mac" list, and ask Federico when he can
  rehearse with it. He merges; the merge is tagged (`v0.x.y`, the same tag on
  RadiclesLibs).
- **No new step starts** until that question has been asked and answered. "Not
  this week, carry on" is an answer.
- Not in the days before a gig (see Two Macs).

## Which model does what

Federico leaves the session on Fable and does not switch models by hand
(agreed 18 Nov 2026). Claude decides: it does the Fable work itself and hands
Opus work to an Opus subagent (the Agent tool's `model: "opus"`), then checks
the result before it goes into a PR. Every planned piece of work names its
model (in `docs/implementation-plan.md`, and in the PR description: "Model:
Fable", "Model: Opus", or both with who did what). If a session is found
running on another model than Fable, say so before starting.

- **Fable** (the stronger model): design notes and anything that changes
  the plan; the engine (Assemblage, Block, buffers, anything asynchronous);
  the document model and sessions; the command parser; anything hard to undo;
  reviews.
- **Opus** (as a subagent): work whose result is easy to check: a bug fix
  with a failing test written first, test tooling, refactors whose snapshots
  don't change, dead code, docs and help files. The brief names the files and
  what done looks like; the subagent never runs the tests itself (only one
  test run at a time: the main session runs them).
- **A second look.** A PR that touches the engine, or is larger than a few
  hundred lines, gets an independent review by a fresh Fable session before
  it is merged, whoever wrote it. A fresh reader finds what the author's own
  tests don't look for.

## Two Macs

- **Dev Mac** (research-group laptop): all Radicles development and testing
  happen here. Federico doesn't use it for anything else while Claude Code
  runs.
- **Performance Mac** (Federico's own laptop, for rehearsals and gigs): runs
  `master`, plus a gig tag before gigs. New features reach it through a
  `dev` → `master` PR, then a pull and a class-library recompile there. Don't
  change it close to a gig.
- **RadiclesFiles is not synced** between the Macs. It lives in
  `~/Library/Application Support/SuperCollider/Extensions/RadiclesFiles`; if a
  test fails because a file is missing, name the file.
- **Ask before a full test run**, and never start one while the Mac is busy with
  other heavy work.

Dev Mac setup checklist:

1. Xcode command-line tools.
2. SuperCollider 3.14.1 and sc3-plugins.
3. The Radicles Quark as a git checkout on `dev`.
4. RadiclesLibs.
5. RadiclesFiles.
6. Homebrew, then `gh auth login`.
7. Claude Code, opened on the Quark folder.
8. The smoke test: `Tests/ci/run-headless.sh check smoke`.

## Federico's Mac

- Don't recompile the class library, run tests, or switch branches while he has
  Radicles running or is about to perform: ask first.
- His SuperCollider tabs hold his own work: never overwrite them.
- A Bluetooth headset or Zoom can change the audio device's sample rate and stop
  servers booting; the tests' muted server uses port 57150.
- For long runs keep the Mac awake: `caffeinate -dimsu -t <seconds>`.
- After testing a branch, say which branch the checkout is on, and that the IDE
  needs Language > Recompile Class Library to use it.

## Tests

Snapshot tests of the code each command posts, plus checks; see
`Tests/README.md`.

```
Tests/ci/run-headless.sh [check|update] [case] [test]
```

- Tests use the classes, cases and snapshots of the folder sclang compiles
  (`Radicles.mainPath`, the folder `Radicles.sc` was compiled from). By default
  that is the checkout in `downloaded-quarks/Radicles`.
- To test a branch without switching that checkout: make a worktree outside
  the Radicles folder, copy `sclang_conf.yaml` with the Radicles include path
  replaced by the worktree's, and run the worktree's own
  `Tests/ci/run-headless.sh` with `RADTEST_CONF` pointing at the copy. Its
  results go to the worktree's `Tests/results/`.
- **Never run two at once** (all runs share the test server port; each folder's
  results are deleted at the start of a run).
- New test: `update` to write its snapshot, read the snapshot, then `check`.
- A changed snapshot is a change in what performers see: intended, or a bug.
- A snapshot must not depend on timing (asynchronous posts land in another
  command's block on a slower or faster machine): give the command a fixed wait
  (`["cmd", 3]`) or check in `code:` instead.
- `known: "reason"` marks a reported problem; `gui: true` tests need windows
  (skipped in the cloud, run on the Mac); `docCheck` compares the document with
  the running system after every command.
- Federico performs at 48 kHz (the RAVE models' rate): set the Mac's audio
  device to 48 kHz for test runs. Two tests fail at 44.1 kHz.
- Full suite: about 500 tests, 2+ hours. Run the cases a change touches first;
  the full suite before opening a PR (a cloud session can run it).

## Code

- `Classes/Radicles/` commands (`RadiclesCmds*.sc`, registered by type
  signature in the call window); `Classes/Assemblage/` the mixer engine and GUI;
  `Classes/Block.sc` sources; `Classes/RadDoc.sc`, `RadUnit*.sc` the document
  model (units, presets, chains, strips, tracks, morphs, libraries).
- `Files/` is the Main library (SynthDefFiles with `->` specs; SynthFiles are
  derived). Other libraries are in the RadiclesLibs repo; selected with `imp`
  (`Radicles.excludeLibs`). The performer's data (presets, sound files,
  settings) is in RadiclesFiles, outside this repo (`Radicles.filesPath`).
- **Commands generate code.** Everything a command does is posted and run
  through `.radcode` (or `radpost` / `radpostcont`), so the post window shows
  what ran. Don't call Ndefs silently. (It is a transparent record, not a
  replayable one: Radicles' own lists are not in the posted code. Getting a
  state back is what sessions are for.)
- **Document model:** the Ndefs own the values; the document tracks the
  structure and everything a session needs, so sessions can be saved and
  loaded. Values are read from the running system when the document is read;
  hooks keep only structure in step; nothing is routed through the document
  (the "ownership switch" was dropped on 18 Nov 2026). Strips are keyed by
  name, never by position in Assemblage's lists.

## SuperCollider gotchas met so far

- Strings: compare with `==`, `includesEqual`, `indexOfEqual`; `includes` and
  `indexOf` use identity (library names, folder names).
- Nothing may follow `^` in a block (it won't compile).
- GUI work on `AppClock` (`defer`, `fork(AppClock)`); `Server.default.sync` only
  inside a Routine.
- `NodeProxy:lag` rebuilds the proxy (a crossfade): wait its fade time before
  setting values meant to glide.
- Qt windows placed before they are shown count the title bar differently
  (about 20 px); window position work is deferred to the GUI rebuild.
- A closure made in a loop and deferred sees the loop variable's last value
  if the variable is declared outside the loop: declare it inside.
- `.interpret` returns nil on a compile error and the code after it carries
  on.
- `Condition.wait` has no timeout: a completion function that is skipped on a
  failure path hangs the routine without a trace. Every failure path calls
  back.
- `NodeProxy:set` does not notify dependants; a window can't listen to an
  Ndef's values, it must compare or be told.
- Removing from a collection inside its own `do` skips items.
- Controls named by position (`vol1`, `vol2`) keep their old values in the
  node map when the function is rebuilt with fewer of them: set them again.
