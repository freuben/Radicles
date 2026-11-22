# Where the work stands

22 Nov 2026 (after release `v0.4.0`, the mixer ramps). The longer working log is the claude.ai Project doc
`claude/start-here.md`; this page is the short version.

## How we work

- Design notes and decisions: the claude.ai Project "Radicles development",
  copied here once decided (see [README.md](README.md)).
- Implementation and testing: Claude Code on whichever Mac Federico is using
  (folder = `Quark("Radicles").localPath`), following `CLAUDE.md`. Very long
  test runs can go to a cloud session.
- PRs into `dev`; Federico merges (Claude merges tests, refactors and docs
  that change no snapshot); `master` only moves when Federico merges `dev`,
  and each release is tagged on both repos.
- Ideation and development are two kinds of conversation (20 Nov): nothing
  is built until Federico says so.

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
| Time | `fade N` reaches everything, own fade times (first PR); loading a preset is a crossfade: `fxload` / `blkload` / `csload` / `tload` take a time and `xfade`, `glide` or `none`, and the `morph` words and own morph times are removed (second PR, [design/time-fade-crossfade-glide.md](design/time-fade-crossfade-glide.md)); the mixer's moves are ramps: volume, trim, sends and pan move as steady moves with a typed time per control (`vollag`, `trimlag`, `sndlag`, `panlag`, all at once with `mixlag`) and a native time for the window, MIDI, HID and OSC (`ctllag`) ([design/mixer-ramps.md](design/mixer-ramps.md)); lags on blocks and effects are still to discuss | fades built 19–20 Nov (#112–#118); mixer ramps built 21 Nov (#121–#125) |
| Review | read-only review of code, plans and workflow (18 Nov); the order below follows it | done |
| Fixes | the bugs the review confirmed (names with a dash, ranges with a modulator, send levels, block numbers, track and bus removal), and removing spelled `rm` | done, #105–#110; **release 1** is `v0.2.0` (19 Nov, #111) |
| Tests | automatic runs on GitHub for every PR, in four parallel parts (about 45 minutes); a server that exits during a test is reported as that | done, #104, #116; `doctor` not built |
| 5a | one structural command at a time; timeouts; errors reach the performer | next; no design note yet |
| 5b | structure by name (one record per strip), as far as sessions need | then |
| 6 | sessions, with autosave and rebuild after a crash | then |
| 8, 7, 5c, 5d | points (after sessions); scenes, session morph, undo; the windows as observers; the protocol, network and browser UI | later; order decided when sessions are done |

Releases: `v0.2.0` (19 Nov, #111: steps 1 to 4, `rm`, the removal fixes), `v0.3.0` (20 Nov, #119: the fades, loads as crossfades), `v0.4.0` (21 Nov, #127: the mixer ramps). `master` is at `v0.4.0`; `dev` has nothing a performer would notice beyond it.


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
- Lags on blocks and effects: the last open part of the Time step, next in
  ideation. The proposal on hold is in
  [design/time-fade-crossfade-glide.md](design/time-fade-crossfade-glide.md).
- The native time's default (`ctllag`, 50 ms): to be judged by ear in
  rehearsal; 20 ms is the other candidate.
- Design notes for 5a, then sessions.
- The review report (`docs/reviews/2026-11-review.md`) is not committed
  until Federico has read it.
- Whether the `Plane` classes stay as stubs until the points design note.
- `Radicles.quark` still says version 0.0.4; the tags are at `v0.4.0`.
- Issues #42 (points and planes), #43 (installation guide link), #44
  (channel modes), #45 (sharing and replaying code logs).

## Test suite

About 650 tests in 42 cases. Every PR runs the full suite on GitHub: about
600 run there (the last run, on #125: 587 pass, 15 known) and 45 that need
windows are skipped; those run on the dev Mac. The last full Mac run was on
18 Nov (#101, about 2 h 20); since then the Mac runs the cases a change
touches. Two tests fail when the Mac's audio device is at 44.1 kHz instead
of 48.
