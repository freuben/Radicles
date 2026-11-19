# Step 2 Design Note – Blocks as Units

> **Changed on 19 Nov 2026:** the `morph` commands and the morph times described here are gone. Loading a preset is itself the transition (`fxload`, `blkload`, `csload`, `tload` take a time and `xfade`, `glide` or `none`); see [Time in Radicles](time-fade-crossfade-glide.md). The rest of this note stands.

Nov 3, 2026 · @Federico

## Scope of step 2

Step 2 puts blocks (track sources: `sine`, `play`, `loop`, wavetables, ...) in the document as units, with block presets and block morph, reusing what step 1 built for effects. A draft for Federico to review before any code.

**In step 2**

- Blocks in the document, kept up to date: `blk`, `pl`, `lp`, `st`, `blkset` and friends, `modblkset`, `unmodblk`, the source menu.
- Buffers and data by name: a sound file by name and path, an allocated buffer by its size, a wavetable by its name (this also fixes the wavetable round trip found in the audit).
- Block modulation with each modulator's own settings (lost today, kept by the step 1 unit reader).
- Block presets: `blksave` / `blkload`, `default`, folders and the library rule from step 1.
- Block morph: `blkmorph`, with the morph time from step 1; sound-file and gated blocks glide instead of crossfading.
- Position for blocks that play through time (sound files): reported, and a start position on load.
- The source window: PRESET and SAVE, like the effect window.

**Not in step 2**: patterns (`blk 1 pattern ...`, which are Pdefs, not Ndefs; question 3), which block feeds which track (step 4, strips with source), recording buffers' contents (step 6, sessions), the mixer and windows as clients (step 5).

**Blocks today** (`Block.sc`, 1,005 lines). A block is `Ndef('block1')`, made by `Block.add`. `Block.play` loads its buffer (through `BStore`), puts the library function into the Ndef and records `liveBlocks[i] = [tag, name, bufferID, data]`. Values live in the Ndef's node map; modulators in `ModMap`. The Main library has 38 blocks: 13 use a buffer (sound files, PV, cue, wavetables, VOsc, recording), 10 have envelopes or gates, 8 are triggered (Impulse, Dust), the rest are plain oscillators and inputs.

## A block as a unit

A block is the same kind of unit as an effect, with `kind: \block`, so `RadUnit.read`, `RadUnit.apply`, presets and morph work on it with few changes. The new part is how a block names its buffer or data.

```
blocks: (
  block1: (kind: \block, def: \loop,
    args: (rate: 1, amp: 0.5),
    mods: (rate: (def: \sine, args: (freq: 0.2, mul: 0.1, add: 1))),
    buffer: (play: "loops/drums.wav"),
    position: 3.2),
  block2: (kind: \block, def: \osc1,
    args: (freq: 220), data: \harmonic1),
  block3: (kind: \block, def: \sine,
    args: (freq: 440, amp: 0.2), morphTime: 4)
)
```

**Address.** `blocks.block1`, after the Ndef (`Ndef('block1')`). `blk 1` already means block1, so commands and the document agree.

**Buffer, by name, never by number.** Buffer numbers change from one run to the next, so the unit says what to load:

- `(play: path)` for a sound file read into memory (`play`, `loop`, `playbuf`, PV blocks), path relative to the sound folder when it is inside it.
- `(cue: path)` for a file streamed from disk.
- `(alloc: frames)` for an empty buffer of a size (`rec`, `recloop`).

`BStore.setBufferID` already takes these forms (`[\play, \audio, name]`, `[\cue, \cueN, name]`, `[\alloc, \allocN, [size]]`), so reading a unit back means calling it with the same form.

**Data.** A wavetable or other `DataFile` by name: `data: \harmonic1`. Today `liveBlocks` keeps the loaded array, which is why the audit found the wavetable round trip broken.

**Kept from step 1**: `args` (all of them, as for effects), `mods` with each modulator's own settings, and `morphTime` when the slot has its own. There is no `bypass`: a block is stopped with `blk 1 stop`, and a stopped block is simply not in `blocks`.

**Reading** uses the block's function (the library's `SynthFile` entry) for argument names, the Ndef's node map for values, `ModMap` for modulators and `liveBlocks[i]` for the block name, buffer form and data name. That means `liveBlocks` needs to keep the buffer form and data name, not only the buffer number and array (one small change in `Block.play`).

## Keeping the document up to date

As with effects, the document follows the live state through a few hooks that call `RadUnit.sync`, plus a check that compares the two. No command writes to the document directly.

| Where | When | Hook |
| --- | --- | --- |
| `Block.play` | a block starts or changes definition | sync, after the start-ticket check (#51), so a superseded start never writes |
| `Block.stop` | a block stops | sync (the unit goes) |
| `Block.remove`, `removeArr`, `removeAll`, `clear` | blocks go | `RadUnit.syncBlocks` |
| `ModMap` map / unmap | a modulator is added, changed or removed | `syncModTarget` (step 1) now also reaches blocks |
| `blkset`, `blkxset`, `blklag` ... | values change | none: as for effects, values are read from the Ndef when the document is read (`RadUnit.refresh`), which also catches values set from HID maps or the IDE |

**The order matters for buffers.** `Block.play` loads the buffer asynchronously (`BStore` with a callback). The sync happens when the Ndef has its source and `liveBlocks` its entry, not when the command is typed, so a block whose file fails to load is not in the document.

**Consistency check.** `RadUnit.check` (the `docCheck` test option) gains a block pass: every running block Ndef has a unit and vice versa, with the same definition, values, modulators and buffer form. Tests that start or stop blocks run with it on.

**Cost.** `blkset` and the other value commands run no document code at all; `blk` reads the block once, taking argument names from the function already in its Ndef (no file read). Timing of `blk` and `blkset` is checked on the Mac, as for `fx`.

## Block presets

Block presets work like effect presets: same file format, same folders with `block` in place of `filter`, same `default`, same library rule. Only the commands and the buffer question are new.

```
blksave 1 warmLoop             // block1's unit -> your presets for its definition
blkload 1 warmLoop             // reconcile block1 to it (same definition)
blkload 1 default              // the definition's defaults, no modulators
blkload                        // list block presets (all definitions)
blkload loop                   // list the presets of loop
blksave loop warmLoop MyLib    // copy your preset into loop's library
```

**Folders.** `RadiclesFiles/Settings/Presets/units/block/<def>/<name>.scd` for yours; `RadiclesLibs/<lib>/Settings/Presets/units/block/<def>/` for a library's. Your own wins over a library preset of the same name; `RadUnit.libraryOf(\block, def)` reads `Files/SynthFiles/Block.scd` of Main and each library.

**Loading reuses the reconciler.** `RadUnit.apply(\block1, unit, ...)` already sets only what differs and maps, changes or removes modulators. For blocks it also compares the buffer form and data name: different ones go through `Block.play` with the new buffer (question 1 decides whether a preset carries one at all).

**Same definition only** (decided 3 Nov). As with effects, a preset of `loop` loads into a block that is playing `loop`; otherwise `blkload` refuses with a reason (`block 1 plays sine; warmLoop is a preset for loop`). Starting a block from a preset can be added later without changing anything else.

**What a preset leaves out.** `position` (where a sound file is) and the block number: a preset describes a sound, not a moment.

## Block morph

`blkmorph 1 warmLoop 4` moves block 1 to a preset over 4 seconds, with step 1's machinery (`morphUnitPreset`, time order, interrupted morphs restoring their times). What changes is the default shape: a crossfade suits some blocks and breaks others.

**Crossfade is the default for every block** (decided 3 Nov), as for effects, with no per-block table to keep up as blocks are added. `blkmorph 1 warmLoop 4 glide` or `none` when the crossfade isn't the sound wanted.

- **Sound files.** A crossfade builds a second copy of the block; it starts at the position the block reports (see Position), so a loop carries on from where it was instead of jumping to its start.
- **Envelope and plucked blocks** (`sinenv`, `karplus`, ...) play their attack again in the new copy; that is accepted for the default, and `glide` avoids it.
- **Recording blocks** are not morphed (question 6).

To check on the Mac: whether `glide` restarts a sound-file block (in step 1 the lag rebuilds the Ndef); if it does, glide on those blocks also starts the new copy at the reported position.

**A different buffer or table.** Morphing to a preset with another sound file (if presets carry buffers) can't glide: it is a crossfade between two blocks playing the two files, each with its own position. That is the same machinery step 3's strip morph needs, so step 2 refuses it with a reason and step 3 adds it.

**Modulators** fade with the rest, as for effects (ModMap maps with xset over the fade time).

## Position

A block that plays a sound file has a place in it, and the document should say where, so a session can come back to the same place and a client can draw a playhead. There are two ways to know it; question 4 asks which.

**A. From the server (phase on a control bus).** The block's function writes its play position (the `Phasor` or `PlayBuf` phase, divided by the sample rate) to a control bus; the language reads the bus when asked (`Bus.get`, asynchronous) or polls it for a playhead.

- Exact, also with rate changes, modulated rate and loops.
- Needs a small change in every sound-file block definition (10 in Main, and library blocks that want it), and a bus per block.

**B. From the language (an estimate).** Remember when the block started, at what rate, and the file's length; compute the position when asked.

- No change to block definitions; works for library blocks as they are.
- Wrong as soon as the rate is modulated or changed with a lag, and drifts over long loops.

**Decided 3 Nov: A.** Main's sound-file blocks get the addition (a phase written to a control bus); library blocks can add it the same way. Blocks that don't report have `position: nil`. Reading position is on request (`blkget 1 position`, a crossfade, saving a session), never on every sync, so it costs nothing in normal use.

**Starting at a position.** Loading a session puts `startPos` (most sound-file blocks already have it) to the saved place. A preset never carries a position.

## The source window

The block's window (`blkgui 1`, the source menu) gets the same header as the effect window: PRESET and SAVE, built by the same `prArgWinGUI` header views, so the size and position fixes from #73 apply to it too.

- **PRESET**: `default` first, then your presets and the library's (`name · LIB`); choosing one is `blkload`.
- **SAVE**: the small name window from #73, saving like `blksave`.
- **No BYPASS**: blocks have none. **STOP** stays where it is.
- The window is rebuilt in place after a load or morph, and only if it is open (the rule #73 brought in for effect windows).

The test `source-gui/fx-window-shared-builder` already checks that the two windows share a builder; it gains the block header, and a Mac test opens every Main block's window and checks sizes, like `fx-window-sizes`.

## Tests, done when, and PRs

**Tests** (new `Tests/cases/block-units.scd`, `block-presets.scd`, `block-morph.scd`, all with `docCheck`):

- Round trips (read a unit, rebuild from it, read again: equal) for sine, a sound file (`play`), a loop, a wavetable (`osc1`, the audit's broken case), an allocated buffer (`rec`), a modulated block, and a block whose modulator is modulated.
- Hooks: start, change definition, set/xset/lag, modulate and unmodulate, stop, remove, clear; `block-start-race` (#51) with the doc check on, so a superseded start never reaches the document.
- Presets: save, load, `default`, list, library copy and the refusals, your own winning over a library's; the PRESET menu and SAVE button on the Mac.
- Morph: each row of the policy table (shape chosen, refusals), time order, interrupted morph, already there.
- Position (once question 4 is answered): reported within a tolerance after a known time, and a start position honoured.
- Timing of `blk` and `blkset` on the Mac, before and after.

**Done when** every Main block reads back into the document and rebuilds from it, block presets and morph work from commands and the window, the cloud suite and the Mac tests pass with no existing snapshot changed, and Federico has tried it by ear.

**PRs**, small, each into `dev`, in this order:

1. Blocks in the document: unit shape, `liveBlocks` keeps buffer form and data name, hooks, check, round-trip tests.
2. Block presets: `blksave` / `blkload` (with `nobuf`), `default`, folders, library rule.
3. Position: Main's sound-file blocks report their phase on a control bus; `blkget 1 position`.
4. Block morph: `blkmorph`, crossfade by default, starting sound files at the reported position (needs 3).
5. The source window: PRESET and SAVE.

## Risks

- **Asynchronous buffers.** A sync that runs before the buffer is loaded would record the wrong state. Mitigation: sync from `Block.play`'s callback, after the ticket check, and test with the race test on.
- **Sound folders that differ between machines.** A path stored in a preset or session may not exist on another computer. Mitigation: store paths relative to the sound folder when possible, and warn (not fail) when a file is missing.
- **Glide on a sound-file block rebuilds it.** In step 1, `lag` rebuilds the effect (the old copy fades out). For a sound file that is a restart. Mitigation: check on the Mac whether `lag` restarts `PlayBuf`; if it does, set the lags when the block starts (from the definition's specs) so a morph only has to `set`.
- **Patterns share the block number.** `blk 1 pattern` uses a Pdef, not an Ndef; the hooks must skip it cleanly until patterns are in scope.
- **Timing.** Every `blkset` now syncs. Mitigation: names from the stored function, measure on the Mac.

## Questions for Federico

1. **Does a block preset carry its sound file or table?** *Decided 3 Nov:* yes. A preset carries the sound file (its name and its path relative to the sound folder) or the wavetable name. `blkload 1 warmLoop nobuf` loads the settings and keeps the block's current file.
2. **Loading into a stopped or different block.** *Decided 3 Nov:* like effects, the block must already play the preset's definition.
3. **Patterns:** *Decided 3 Nov:* left out of step 2.
4. **Position:** *Decided 3 Nov:* A, the block reports its position on a control bus.
5. **The morph shape.** *Decided 3 Nov:* crossfade is the default for every block, as for effects; no per-block table. `glide` (or `none`) in the command when wanted.
6. **Recording blocks:** *Decided 3 Nov:* kept out of presets and morph until sessions (step 6).

All six questions are answered (3 Nov): the note is ready for PR 1.

Reply with your answers here or in a comment; the first PR (blocks in the document) does not depend on any of them.
