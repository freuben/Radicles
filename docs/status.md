# Where the work stands

17 Nov 2026 (after #95). The longer working log is the claude.ai Project doc
`claude/start-here.md`; this page is the short version.

## How we work

- Design notes and decisions: the claude.ai Project "Radicles development",
  copied here once decided (see [README.md](README.md)).
- Implementation and testing: Claude Code on whichever Mac Federico is using
  (folder = `Quark("Radicles").localPath`), following `CLAUDE.md`. Very long
  test runs can go to a cloud session.
- PRs into `dev`; Federico merges; `master` only moves when Federico merges
  `dev`.

## Document Model: steps

See [implementation-plan.md](implementation-plan.md) for what each step
contains.

| Step | What | State |
| --- | --- | --- |
| Before 1 | test harness (#64 and earlier), central code path `.radcode` | done |
| 1 | the document (`RadDoc`, safe reader `RadLiteral`), effects as units, `fxsave` / `fxload` / `fxmorph`, bypass, library presets | done, #67–#74 |
| 2 | blocks as units, `blksave` / `blkload` / `blkmorph`, block position, recordings kept with `keep` | done, #75–#84 |
| 3 | strip values in the document, `cssave` / `csload` / `csmorph` / `csmorphtime` | done, #85–#87 |
| 4 | track presets `tsave` / `tload` / `tmorph` / `tmorphtime`; presets load the libraries they need | done, #89–#92 |
| 5 | the GUI as a client of the document (the mixer and windows rebuilt; window positions are handled here) | next |
| 6 | sessions | later |
| 7 | scenes, session morph, undo, browser UI | later |
| 8 | the event layer: points, planes, sparks, sieves (#42) | later |

## Decided along the way that later steps must keep

- Window position jumps after a preset load (#88, closed): not patched;
  handled when the windows are rebuilt in step 5.
- Several blocks feeding one track (`blkget 1 1`): deferred.
- Channel modes and panners (#44): `channels` and `space` stay read-only in
  the document until #44 is designed.
- Paths in presets and sessions are always relative to the Radicles folders.

## Open

- Workflow points still to discuss: working on two Macs, parallel sessions.
  (Memory, settled 17 Nov: claude.ai memory is a bonus for Project chats and
  is never relied on; anything that must be remembered goes in `CLAUDE.md`,
  `docs/` or the Project's start-here doc.)
- Step 5 design note.
- Issues #42 (points and planes), #43 (installation guide link), #44
  (channel modes), #45 (sharing and replaying code logs).

## Test suite

504 tests; the full cloud run takes about 2 h 20 (last: 451 pass, 13 known,
40 skipped as GUI-only, 0 fail). GUI tests run on the Mac.
