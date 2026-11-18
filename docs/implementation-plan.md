# Document Model – Implementation Plan

Oct 31, 2026 · @Federico

## How we work

The detailed plan for the [Document Model proposal](https://claude.ai/artifact/KMM18iQEfQFWmQfFjsETPF): what each step does, how we know it is done, and what could go wrong. The migration changes the heart of a working instrument, so these rules apply to every step.

1. **A short design note before code.** Each step starts with one page (the data it adds to the document, the interface, the commands) that Federico reviews. Code follows only after agreement.
2. **Commands do not change.** Command names and arguments stay as they are; only what happens inside them changes. Performers' habits, handouts and saved command files keep working.
3. **Posted code does not change** unless a step says so. The 265 tests already snapshot the code every command posts; a refactor that changes a snapshot is a bug until explained.
4. **Shadow first, then switch.** Each part of the document is first kept alongside the existing lists and checked against them after every command (in the tests). Only when they always agree does the document become the owner and the old list go.
5. **Round trips at every level.** Unit presets, strip presets and sessions are each saved, loaded in a fresh SuperCollider and compared with the fingerprint from the session tests. A step is done when its round trips come back identical.
6. **Small PRs into `dev`, one concern each,** with the full cloud suite and, where windows are touched, a Mac run before merging. `master` only moves when Federico merges `dev`, never in gig weeks, and every merge is tagged so it can be rolled back.
7. **No slower commands.** Typical commands are timed before and after each step; a step must not add noticeable latency to typing, faders or MIDI.
8. **Findings are written down,** in the project notes and issues, including what was tried and did not work.

## Before step 1

- [ ] **The 6 Nov gig** on master as it is today (tag `gig-2026-11-06` as fallback).
- [ ] **Merge PR #64** (session tests, headless `load`, missing-bus warning) after the Mac run of the window tests.
- [ ] **Merge the limiter fix** (`load` and output changes keep the master's `clip2` limiter), after a listen on the Mac.
- [ ] **A full Mac run on `dev`** as the baseline: every test, including the window tests, passing or known.
- [ ] **Timing baseline:** time a set of typical commands (`vol`, `fx`, `fxset`, `blk`, a fader move, a MIDI-mapped control) so later steps can be compared.
- [ ] **Tag `pre-docmodel`** on `dev`, the point to return to if a step goes wrong.
- [ ] **Design note for step 1** (document API, unit interface, preset file format, and the Radicles protocol for clients), reviewed by Federico.

## Risks across all steps

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| **Behaviour changes by accident** | a refactor of a working instrument can break things performers rely on | rules 2–4: commands unchanged, posted-code snapshots, shadow mode; small PRs; tags for rollback |
| **Two sources of truth during the migration** | the document and the old lists can disagree, and bugs hide in the gap | shadow mode with a consistency check after every command in the tests; each step removes the lists it replaces, so the overlap is short |
| **Asynchronous code** | Radicles' commands run in routines with `server.sync`; a document updated before an operation finishes (or when it fails) describes something that is not there | update the document only when an operation has succeeded; one writer at a time; failures leave the document unchanged and are reported |
| **Latency in performance** | the reconciler runs on every change, including faders and MIDI at many changes per second | commands change one address at a time and are applied directly (a patch), not by comparing whole documents; full comparison only for load, scenes and undo; timing checks (rule 7) |
| **Morph by crossfade (`xset`) restarts synths** | `xset` makes a new synth: a sound-file block restarts from its start position, a gated envelope retriggers, a delay or reverb starts empty | each unit declares how it can be morphed: crossfade, glide (`lag`) only, or not at all; sound-file blocks and gated blocks glide or keep their synth |
| **CPU during morphs** | crossfading many units at once runs two copies of each for the fade time | morph only the units that change; glide plain values instead of crossfading; measure CPU on the Mac in steps 1, 3 and 7 |
| **Loading files that run code** | a readable SuperCollider file is loaded by interpreting it; a shared session could contain any code | the document is read as data only (a check that it contains only Events, Arrays, Symbols, Strings and Numbers before it is used); `code.scd` is only run after asking, for sessions from someone else |
| **Library definitions change** | a preset or session saved with an older `pch` may name arguments the current one no longer has | unknown arguments are skipped with a note; new arguments take their defaults; a warning option (off by default) |
| **Stable addresses** | presets, mappings and modulation refer to addresses; if they shift, things point at the wrong place | effect slots are already named by number, not position (adding or removing does not renumber); strips by name; modulators by what they drive, never by their number |
| **Design that does not fit later parts** | points, planes and #44 (channel modes) are not designed in detail yet | the unit interface includes position and a place for points from the start; a test unit that plays through time checks the interface early; strip channel and space fields stay generic until #44 |
| **Window code is hard to test** | the mixer GUI is one 1,092-line method and only runs on the Mac | windows become a client in step 5, once effects, blocks and whole strips (with their sources) are in the document; Mac runs before each merge that touches them |
| **Scope creep** | each step invites "while we are here" changes | the design note lists what is in and out; anything else becomes an issue |

## Step 1: foundations and effects as units

**Goal:** the document exists, effects live in it as units, and effect presets and effect morphs work in the new format.

**Tasks**

1. **The document class:** a tree of Events and Arrays with addresses (`track1.fx.2.pitchRatio`): get, set, the time of the last change at each address, a format version, write to a readable file and read back as data only, and compare two documents (the list of differences).
2. **The unit interface:** made from a library definition; current arguments filtered to that definition's own controls (leftover values from an earlier unit are left out); modulated arguments as mapping plus modulator settings; state out and in; how it can be morphed (crossfade, glide or not at all); an optional position, declared but not yet used.
3. **Effects in shadow mode:** wherever an effect is added, changed, modulated or removed today, the document is updated too, after the operation succeeds. In the tests, after every command, the document is checked against `filters` and the effect Ndefs' controls.
4. **Effect presets in the new format:** `fxsave` / `fxload` (same commands) write and read a unit preset as a readable file in the Presets folder, with format version and timestamp.
5. **Effect morph:** a new command (working name `fxmorph`) moves an effect to a preset over a time: `xset` crossfade by default, glide where the unit says so; the fade time is restored afterwards.
6. **Reconciler, first version, for one unit:** apply a unit state with `set`/`xset` for changed arguments, or replace the source with a crossfade if the unit is different.
7. **The fx window** shows the new values after a preset or morph (through the existing update).

**Done when**

- Every effect in the Main library comes back identical from a preset round trip (save, fresh SuperCollider, load, compare), plus mixed cases: two slots, a modulated argument, a bus and the master.
- Shadow mode agrees with `filters` across the whole suite.
- No posted-code snapshot changes, except where `fxsave` / `fxload` post differently by design.
- Timing of `fx` and `fxset` unchanged.

**Risks and mitigations**

| Risk | Mitigation |
| --- | --- |
| The document says an effect exists when its Ndef failed to build | write to the document in the completion of the operation, not before; shadow check catches it |
| Effect Ndefs carry extra controls (filter roles such as `wet`) that are not the effect's own | take arguments from the definition's controls only |
| A modulated argument in a preset points at a modulator number that will differ on load | store the modulator's kind and settings; loading creates a new modulator |
| `xset` changes an effect's fade time (effects use 0.5 s) and leaves it changed | the morph saves and restores the fade time; test for it |
| Reverbs and delays crossfade from an empty copy, which can sound like a dip | mark such units as glide by default; listen on the Mac |
| The document API is designed too big or too small for later steps | keep it minimal; the design note includes a dry run on a block and a test "point" unit |
| Loading a preset file runs code | read it as data only (see risks across all steps) |

## Step 2: blocks as units

**Goal:** blocks live in the document as units, with their buffers or data and their modulation; block presets and block morphs work.

**Tasks**

1. **Blocks in shadow mode:** `blk`, `pl`, `lp`, `st`, `blkset`, `blkxset`, `blklag`, `modblkset`, `unmodblk` and the source menu update the document; checked against `Block.liveBlocks` and the block Ndefs.
2. **Buffers and data by name and path:** a sound file as name plus path; an allocated buffer by its size; a wavetable by its name and size. This fixes the wavetable reload bug found in the audit.
3. **Block modulation:** mapping plus the modulator's own settings (the audit found these settings are lost today).
4. **Position for sound-file blocks:** the block reports where it is playing (for example a phase written to a control bus) and can start from a position (`startPos` exists). Used later by `savetime`; built now so blocks do not need reworking.
5. **Block presets** (working names `blksave` / `blkload`) and **block morph** (`blkmorph`).
6. **The source window** shows the new values after a preset or morph.

**Done when**

- Round trips identical for a sine block, a block with a sound file, a looped sound file, a wavetable block, a block with a modulated argument, and a block whose slot had another block before (no leftover arguments).
- Shadow mode agrees with `liveBlocks` across the suite, including `block-start-race`.
- Timing of `blk` and `blkset` unchanged.

**Risks and mitigations**

| Risk | Mitigation |
| --- | --- |
| Blocks start asynchronously, and a newer `pl` or `st` cancels one still loading (#51, #53); the document can record a block that never started | write to the document when the block has actually started; the existing race tests run with the shadow check |
| A crossfade on a sound-file block restarts playback from its start, an audible jump; gated blocks retrigger | sound-file and gated blocks morph by glide or `set` only (their unit says so) |
| Buffer numbers differ between sessions | never store buffer numbers, only names, paths and sizes |
| A sound file is missing or has moved | a clear message naming the file and the path tried; the block is left empty, the rest loads |
| Reporting position costs CPU or a bus per block | only units that play through time report it; one control bus per such block; measure |
| Modulator settings are spread across `ModMap` lists | read them through one function that is tested against `modInfoArr` |

## Step 3: effect chains and strip values

**Goal:** a strip's effect chain can be saved, loaded and morphed as one unit (`cssave`, `csload`, `csmorph`), and the strip's own values are in the document, ready for track presets in step 4. Split agreed 6 Nov, after comparing DAWs: chain presets like Logic's channel strip settings, track presets like Pro Tools track presets. Design note: [Step 3 Design Note – Effect Chains and Strip Values](https://claude.ai/artifact/6V1gAJeGmMoXE6B4dKBAJQ).

**Tasks**

1. **Strip values in the document,** keyed by strip name (`track1`, `bus2`, `master`): trim, volume, pan (the panner's arguments by name), mute, solo, output, lags and mixer modulators, read from the running system; channels and panner type read only (#44). Checked against `mixTrackNames`, `muteStates`, `soloStates`, `outputSettings` and the strip Ndefs.
2. **Chain presets** in the new format: `cssave` / `csload` (same commands, same meaning), `default` (an empty chain), loading onto another strip, old presets still loading.
3. **Chain morph** (`csmorph`, `csmorphtime`): the strip's chain is compared slot by slot with the preset (agreed 2 Nov). The same effect morphs as `fxmorph` does; a different effect crossfades; a slot empty in the preset fades out and is removed; a slot filled only in the preset is inserted and fades in; bypass follows the preset.
4. **The mixer window** shows the loaded chain (through the functions `fx` and `clearfx` use); the chain PRESET menu waits for step 5 (decided 6 Nov).

**Done when**

- Chain round trips identical for a mono and a stereo track, a bus and the master, each with effects, a modulator and a bypassed slot; a chain saved from one strip loads onto another.
- The strip values agree with the running system across the suite, including the tests that add and remove tracks and buses.
- Mac run of the mixer tests passes; a chain morph checked by ear.

**Risks and mitigations**

| Risk | Mitigation |
| --- | --- |
| `muteStates`, `soloStates` and `recStates` are lists by position; adding or removing a track shifts them (the cause of several earlier bugs) | the document keys strips by name; the shadow check covers adding and removing tracks |
| Solo involves other strips (soloing one silences the rest) | decide whether a strip preset carries solo (question for Federico); if it does, loading it applies the same rule as the `solo` command |
| Channel modes and panners are to be rethought (#44) | strip fields for channels and space stay generic; revisit when #44 is designed |
| A strip morph with many effect slots crossfades all of them at once | morph only units that differ; measure CPU on the Mac |
| Mixer window state and the document disagree after a preset | the window is updated through the same functions commands use; Mac tests |

## Step 4: track presets

**Goal:** `tsave` / `tload` / `tmorph` (and tmorphtime) save, load and morph a whole channel: the effect chain (step 3), trim, volume, pan, mute, the mixer modulators, the sends (to buses that exist) and the source with its settings (a block now, points later). Never in a preset: solo, record arm. The output to the master is kept as saved (changed 17 Nov); `tload default` goes to the master with no source (changed 18 Nov).

Design note: [Step 4 Design Note – Track Presets](https://claude.ai/artifact/JJHSG5A3Cy8mKT88tePYKm) (16 Nov, for review).

**Tasks**

1. **The strip's input in the document:** which block (or hardware input, or other source) feeds the strip, as the input menu and `in` set it.
2. **Track presets:** `tsave t 1 name` holds the chain in `cssave` form, the strip values, sends, and the source's units in `blksave` form (a list, so points fit later); `tload` starts the source with its buffer or data, then applies the rest; `tload t 1 name nosrc` / `nofx` load parts. Buses (no source) and the master (no source, no sends) use the same commands.
3. **Track morph** (`tmorph`): the chain morphs as `csmorph`; volume, pan and trim glide; the source morphs as `blkmorph`, or crossfades if it is a different block; unmute at the start, mute at the end.
4. **The source window and the mixer's source slot** follow the change (the hooks from #58 and #59).

**Done when**

- Round trips identical for strips with a sine block, a sound-file block and a wavetable block, with and without effects; loading without `src` leaves the running source untouched.
- Mac run of the source-window and source-menu tests passes.

**Risks and mitigations**

| Risk | Mitigation |
| --- | --- |
| Loading a source that needs a buffer takes time; the strip settings arrive before the sound | fixed order: buffer, then block, then strip settings, each confirmed by the server; the block starts silent and fades in |
| Block slots are numbered by track (`block1` feeds `track1`); a preset loaded on another track must use that track's block | rewrite block addresses with the strip's, tested by loading onto a different track |
| Later sources (points with planes, sparks and sieves) carry much more | the source part of a strip preset is a list of units and their data, not a single block, so points fit in step 8 |

## Step 5: the GUI as a client

**Goal:** Assemblage is split into engine (Ndefs), model (the document) and GUI; the GUI generates itself from the document and talks to the core only through the Radicles protocol, first on the same computer, then over the network. See "Architecture: core and clients" in the proposal.

**Tasks**

1. **The protocol (OSC),** designed in the step 1 note and built here: commands in (Radicles commands with their source: typed, button, menu, slider), queries in (the document, a part of it, a unit's specs), and out: the document on connect, then each change (address and value), the posted code, and the meters.
2. **Ownership switch** for effects, blocks and strips with their sources: once their shadow checks have agreed across the suite and on the Mac, commands write only to the document and the reconciler applies it; the old lists go.
3. **Change notifications** from the document, throttled to the GUI update rate (`updateFreq`, 10 per second).
4. **The windows rebuilt as a client,** smallest first: the effect window and source window (already generated from specs by the shared builder), then the mixer (`mixGUI`, 1,092 lines, split as it is moved, like Assemblage was). A GUI action sends a command; the window changes only when the document's change comes back.
5. **Over the network:** the same GUI run on a second computer against the core on the audio computer, with the CallWindow's posted code and the meters following.

**Done when**

- No GUI code reads or sets an Ndef directly, and no state is kept outside the document except in Ndefs and the server.
- All tests pass on the cloud and the Mac; the bytecode and command-table checks used for the earlier splits confirm commands are unchanged.
- The mixer runs on a second computer against the audio computer in a rehearsal.

**Risks and mitigations**

| Risk | Mitigation |
| --- | --- |
| The largest and least-tested part of the code | split into small PRs; smaller windows first; Mac runs before each merge; Federico tries each window before it is merged |
| A fader feels laggy because the window waits for the change to come back | the moving fader updates itself immediately; the returning change only corrects it if they differ; measured locally and over WiFi |
| Several clients (two GUIs, a phone) disagree | the core's document is the only truth; every client gets every change; a client that reconnects asks for the whole document again |
| The network drops during a performance | the core keeps playing on its own (it never depends on a client); the CallWindow and controllers on the audio computer keep working; a client shows that it is disconnected and resynchronises on reconnect |
| Anyone on the same network could send commands | the core accepts clients only after they are allowed (a list of addresses, or a simple shared key); off by default for remote clients |
| Meters over the network use bandwidth | meters are sent at the GUI rate and only to clients that ask for them |
| Removing a list breaks a caller nobody remembered | search for every use before removing; the shadow period shows which callers read it |
| Window behaviour performers rely on changes subtly | the new windows are compared side by side with the old ones on the Mac before the old ones are removed |

## Step 6: sessions

**Goal:** a whole session is saved as a folder and recalled with every setting, checked after loading.

**Tasks**

1. **Everything global in the document:** buses and master (including the limiter as a master setting), sends (bus, level, `sndry`), outputs and recording settings, modulators on the mixer and sends, buffers in the BStore, tempo and clock, libraries, controller mappings (HID, MIDI, OSC, by address).
2. **Saving:** the session folder with `session.scd` always, plus anything recall depends on (buffers a block plays, hand-made Ndefs a track takes as input); optional parts with `savebuf`, `savecode`, `codelog`, `savestate` and later `savetime`; timestamps in every file.
3. **Loading:** if a session is running, ask, fade it out, clear it; then rebuild in a fixed order, each stage confirmed by the server: buffers and files, strips and units, routing and sends, modulators, controller mappings, fade in.
4. **The check after load:** compare the loaded document with the saved one; post "all settings identical" or the differences; report missing files, libraries, UGens, a different sample rate or channel count.
5. **Format versions and converters:** the version in every file, and the framework for converters with sample files in the tests (no converter is needed yet).
6. **`save` / `load` switch to the new format;** the session tests from PR #64 move from known to pass one by one.

**Done when**

- All session round trips identical, including the seven known gaps from the audit (added tracks, tempo, libraries, wavetable blocks, modulator settings, loading over a running session, the limiter).
- A real performance session of Federico's saves and loads on the Mac with "all settings identical".

**Risks and mitigations**

| Risk | Mitigation |
| --- | --- |
| Steps of the load undo each other (as the limiter did) | one ordered rebuild with server confirmation at each stage; the check after load catches anything that slips |
| Loading takes long (many buffers) and the performer cannot tell what is happening | progress posted per stage; buffers load before anything sounds |
| A part fails to load (missing file, unknown unit) | continue with the rest, report each failure clearly; never stop halfway silently |
| Recorded buffers are large and take time to write | writing happens in the background after saving the document; `savebuf` is optional; only buffers the session plays are always written |
| Hand-made Ndefs are hard to tell apart from Radicles' own (modulators, recording, `masterOut`) | Radicles keeps a list of the Ndefs it makes; everything else is "own code"; tested with hand-made Ndefs that feed a track |
| Paths differ between computers (Federico's Mac, a student's laptop) | paths inside RadiclesFiles and the libraries are stored relative to them; other paths as given, with a clear message if not found |
| The session folder in UserLib is shared on GitHub with large buffers or a research log | `.gitignore` in UserLib for `buffers/` and `log/`, matching the decision that the log never leaves the computer |
| Controller devices are not connected when a session loads | mappings are restored and wait for the device; a note lists devices not found |
| Loading a session from someone else runs their code | `code.scd` runs only after asking; the document is read as data only |

## Step 7: scenes, session morph, undo, browser UI

**Goal:** the performance features at session level, all built on the reconciler.

**Tasks**

1. **Change in place:** load a session or scene into a running session by changing only what differs (the full comparison of two documents, turned into the smallest set of operations).
2. **Session morph** over a time, or with the morph position as a 0–1 control mappable to a fader: prepare (buffers, new parts silent), morph (crossfades, glides, fades in and out), clean up.
3. **Scenes and marks:** save the current document as a numbered scene with its time (`scene`, `recall`, `mark`).
4. **Undo and redo:** previous documents kept (100 by default, configurable); a fader drag, knob turn or MIDI movement grouped into one step; a new action clears redo; undo and redo post their code.
5. **Autosave and crash recovery:** save the document every few minutes; after a crash, load it and replay the research log since then.
6. **`savetime`:** positions from the units that report them (step 2), saved to `time.scd` and restored on load.
7. **Browser UI:** a client for iPads and iPhones on the same protocol, through a bridge from OSC to WebSockets (existing tools such as Open Stage Control as a starting point); its own short design note.

**Done when**

- Scene recall, morph and undo round trips: the running session ends identical to the target document.
- Morphs between Federico's real scenes sound right on the Mac, without CPU overload.

**Risks and mitigations**

| Risk | Mitigation |
| --- | --- |
| A command arrives in the middle of a morph | rule: the command wins for its address; the morph continues for the rest (or the performer can stop it) |
| CPU doubles while every unit crossfades | morph only what differs; glide values instead of crossfading; stagger large morphs; measure on the Mac |
| Undo surprises: random values, recorded audio | random functions are re-run (they stay generative); recorded audio is not undone; this is stated in the help |
| The reconciler posts a flood of code for a big change | group the posted code for a scene or undo under one heading; test what is posted |
| Undo history grows without limit | capped at the configured number of steps |
| The bridge for browsers adds a dependency outside SuperCollider and another point of failure | keep the bridge thin and optional; everything works without it; the SC GUI and controllers remain the reliable path in performance |

## Step 8: the event layer

**Goal:** points, planes, sparks and sieves (#42) are built directly on the model.

**Tasks**

1. **Its own design note,** from the [Planes, Sparks and Points draft](https://claude.ai/artifact/XP71vZMe6NtrBA2fhYJMqS).
2. **Points as units** (with position), **planes, sparks and sieves** as document parts with addresses; DStore as part of the stores.
3. **Strips with a point as source** in presets (step 4) and sessions (step 6).

**Risks and mitigations**

| Risk | Mitigation |
| --- | --- |
| The unit interface from step 1 does not fit points | the step 1 design note includes a dry run with a test point; position is in the interface from the start |
| Planes and sparks change many times per second, unlike settings | the document stores their setup (sources, sieves, mappings), not their stream of values; positions go to `savetime` |

## Questions for Federico

- [ ] **Solo in strip presets:** should a strip preset carry solo (which silences other strips when loaded), or only mute?
- [ ] **Morph by kind of unit:** reverbs and delays glide by default (a crossfade starts from an empty copy); sound-file and gated blocks never crossfade. Agreed, or listen first?
- [ ] **A command during a morph:** the command wins for its address and the morph continues for the rest. Agreed?
- [ ] **Names of new commands:** `fxmorph`, `blksave`, `blkload`, `blkmorph`, `csmorph`, and `cssave t 1 name src` for strip presets with source.
- [ ] **Loading someone else's session:** ask before running its `code.scd`?
- [ ] **Pace:** one step at a time with a pause for trying it in rehearsal before the next, or several steps before a merge to master?

Related: [Document Model – Proposal](https://claude.ai/artifact/KMM18iQEfQFWmQfFjsETPF), [Sessions and Scenes proposal](https://claude.ai/artifact/5ZskpNcddw9XD19akqH4Qa) (audit), PR #64, issues #42, #44, #45, #63.
