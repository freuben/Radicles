# Where the work stands

18 Nov 2026 (after #102 and the review). The longer working log is the claude.ai Project doc
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
| 4 | track presets `tsave` / `tload` / `tmorph` / `tmorphtime`; presets load the libraries they need; the output to the master and `tload default` | done, #89–#92, #101, #102 |
| Time | `fade N` reaches everything, own fade times (first PR); loading a preset is a crossfade: `fxload` / `blkload` / `csload` / `tload` take a time and `xfade`, `glide` or `none`, and the `morph` words and own morph times are removed (second PR, [design/time-fade-crossfade-glide.md](design/time-fade-crossfade-glide.md)); lag is on hold | fade part built, 19 Nov |
| Review | read-only review of code, plans and workflow (18 Nov); the order below follows it | done |
| Fixes | the bugs the review confirmed, then **release 1** to `master` | next |
| Tests | automatic runs on GitHub, parallel workers, `doctor` | next, beside the fixes |
| 5a | one structural command at a time; timeouts; errors reach the performer | then |
| 5b | structure by name (one record per strip), as far as sessions need | then |
| 6 | sessions, with autosave and rebuild after a crash | then |
| 8, 7, 5c, 5d | points (after sessions); scenes, session morph, undo; the windows as observers; the protocol, network and browser UI | later; order decided when sessions are done |

`master` is at #66 (1 Nov); everything from step 1 on is only on `dev` until release 1.

## Decided along the way that later steps must keep

- Window position jumps after a preset load (#88, closed): not patched;
  handled when the windows are rebuilt (5c).
- The Ndefs own the values; the document tracks structure and what a session
  needs; nothing is routed through it (18 Nov).
- Several blocks feeding one track (`blkget 1 1`): deferred.
- Channel modes and panners (#44): `channels` and `space` stay read-only in
  the document until #44 is designed.
- Paths in presets and sessions are always relative to the Radicles folders.

## Open

- Workflow points still to discuss: where design notes are drafted (the
  review suggests the repo only), parallel sessions (possible once each test
  run has its own port).
  (Memory, settled 17 Nov: claude.ai memory is a bonus for Project chats and
  is never relied on; anything that must be remembered goes in `CLAUDE.md`,
  `docs/` or the Project's start-here doc.)
- Design notes for 5a, then sessions.
- Whether the `Plane` classes stay as stubs until the points design note.
- The version number for release 1 (the terms of reference say tags start at
  0.1.0; `Radicles.quark` says 0.0.4).
- Issues #42 (points and planes), #43 (installation guide link), #44
  (channel modes), #45 (sharing and replaying code logs).

## Test suite

518 tests. The last full Mac run (18 Nov, 516 tests, on #101) took about
2 h 20: 500 pass, 13 known, 3 fail (two from the Mac's audio device at
44.1 kHz instead of 48, one intermittent). The cloud run skips about 40
GUI-only tests.
