# Radicles tests

These tests check that Radicles commands keep generating the same SuperCollider code, and that the code runs. Each test types a few commands, records everything posted on the two channels, and compares it with an approved snapshot:

- `radpost`: discrete actions (commands, mute, effects), shown in the main document, post window and OSC.
- `radpostcont`: continuous control (knobs, faders, controllers), shown in the post window only.

A test fails if the generated code differs from its snapshot, if SuperCollider posts an error, or if its check (for example "track 1's volume is -10") is false.

## Run the tests on a Mac

In the SuperCollider IDE:

```
~radTestMode = \check;   // \update rewrites the snapshots instead
~radTestCase = nil;      // or one file from cases/, e.g. "step1-regressions"
~radTestOnly = nil;      // or one test name
(Radicles.mainPath +/+ "Tests/run.scd").load;
```

Each test runs in its own fresh `sclang` with a muted server on port 57150, so your own session and server are not touched. Some tests briefly open a mixer window. A summary is posted at the end, and logs are in `Tests/results/`.

## Run the tests in the cloud workspace (Linux, no sound card)

```
Tests/ci/build-sc-linux.sh     # once per workspace, ~25 min
Tests/ci/run-headless.sh       # check
Tests/ci/run-headless.sh update step1-regressions
```

The headless build has no GUI and no neural-model extensions. Tests marked `gui: true` are skipped there. The placeholder classes in `ci/headless-classes` are copied outside the Quark, so users never compile them.

## Write a test

Add it to a file in `cases/`:

```
~radTest.("vol-track",
	setup: ["start 2"],              // run first, not snapshotted
	cmds: ["vol t 1 -10"],           // snapshotted
	check: { Ndef('track1').get('volume') == -10 }
);
```

Other options: `code:` (actions with no command, such as moving a knob from code), `teardown:` (remove presets the test saved, named `radtest_*`), `gui: true`, `noWarnings: true`. A command entry can be `["cmd", seconds]` to wait a fixed time.

Then run it in update mode to write its snapshot, read the snapshot, and commit it.

## When a snapshot changes

A changed snapshot means the generated code changed. In a pull request it shows as red (old) and green (new) lines next to the code change. That is how a change in what performers see gets approved: merge if it is intended, comment if not. Refactors must leave every snapshot unchanged.

Snapshots are approved on SuperCollider 3.14.1. Older versions print arrays with inner spaces (`[ 1, 2 ]`), so some snapshots differ cosmetically there.
