# Radicles – working notes for Claude

Radicles is a live-coding and performance environment for SuperCollider 3.14.1
(a Quark). Federico Reuben is its author, artistic lead and lead user; Claude is
senior engineer and project manager. Design notes, decisions and the
step-by-step plan live in the claude.ai Project "Radicles development" (docs
`claude/start-here.md`, `claude/radicles-codebase-overview.md`, and the design
notes linked from them). This file holds what every coding session needs.

## How we work

- **Design before building.** Each step gets a short design note in the
  claude.ai Project with questions for Federico; build only once he has
  answered. Small fixes he asks for directly don't need one.
- **Branches and PRs.** Branch from `dev`; open PRs into `dev`; one PR per
  coherent change, each with its tests. Never push to or merge into `master`
  (the stable version for performances). Federico merges.
- **PR descriptions** in plain language: what changes for the performer, the
  decisions it follows, tests, cloud and Mac results.
- **Commands people already type don't change** unless agreed. New behaviour
  comes with tests, checked to fail without the change.
- **Stop and ask** when a decision is Federico's (behaviour performers will
  notice, naming, anything hard to undo); otherwise take the sensible default
  and say so in the PR.

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

- Run from the checkout SuperCollider compiles (`Quark("Radicles").localPath`).
  To test another worktree, point `RADTEST_CONF` at an sclang config that
  includes it.
- **Never run two at once** (the results folder is shared and deleted at start;
  the test server port is shared).
- New test: `update` to write its snapshot, read the snapshot, then `check`.
- A changed snapshot is a change in what performers see: intended, or a bug.
- A snapshot must not depend on timing (asynchronous posts land in another
  command's block on a slower or faster machine): give the command a fixed wait
  (`["cmd", 3]`) or check in `code:` instead.
- `known: "reason"` marks a reported problem; `gui: true` tests need windows
  (skipped in the cloud, run on the Mac); `docCheck` compares the document with
  the running system after every command.
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
  through `.radcode` (or `radpost` / `radpostcont`), so the post window is a
  replayable record. Don't call Ndefs silently.
- **Document model:** values are read from the running system when the
  document is read; hooks keep only structure in step; strips are keyed by
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
  (about 20 px); window position work is deferred to the GUI rebuild (step 5).
