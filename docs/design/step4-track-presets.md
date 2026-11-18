# Step 4 Design Note – Track Presets

Nov 16, 2026 · @Federico

## Summary

Step 4 saves, loads and morphs a whole channel strip as one preset with `tsave`, `tload`, `tmorph` and `tmorphtime`. A track preset wraps the step 3 chain preset with the strip's values, its sends and its source. It works for tracks, buses and the master (`tsave b 1 …`, `tsave m …`).

| Layer | Commands | Holds | Step |
| --- | --- | --- | --- |
| Effect | `fxsave` / `fxload` / `fxmorph` | one effect | 1, done |
| Block | `blksave` / `blkload` / `blkmorph` | one block | 2, done |
| Effect chain | `cssave` / `csload` / `csmorph` | the strip's effects, modulators, bypass | 3, done |
| Track | `tsave` / `tload` / `tmorph` | the chain, trim, volume, pan, mute, mixer modulators, sends, the source | 4 |
| Scene | later | every strip, the buses, the master | 7 |

Nothing new is invented for the parts: the chain is written as `cssave` writes it, the source as `blksave` writes it. Loading and morphing reuse `csload`/`csmorph` and `blkload`/`blkmorph`. What is new: the strip's **sends** and **source** go into the document (PR 1), and one file and one set of commands tie the parts together.

Commands people use today don't change. Questions for you are at the end; PR 1 doesn't depend on them.

## Commands

The four commands take the same strip forms as the chain commands: `t 1`, `b 2`, `m`, and a mixer position (`tload 3 name`).

| Command | What it does |
| --- | --- |
| `tsave t 1 vox` | track 1 → your track presets (asks before replacing one) |
| `tload t 3 vox` | the preset onto track 3: source, chain, values, sends |
| `tload t 3 vox nosrc` | everything except the source (the track keeps what it plays) |
| `tload t 3 vox nofx` | everything except the chain (the track keeps its effects) |
| `tload t 3 default` | a clean strip, its source stopped (question 2; changed 18 Nov); `nosrc` keeps the source |
| `tload` / `tload t` | lists track presets |
| `tmorph t 3 vox [time] [xfade\|glide\|none]` | morphs track 3 to the preset (see Morph) |
| `tmorphtime t 3 5` | track 3's own morph time; `tmorphtime t 3` posts it, `default` clears it |

- **Morph time:** the command's, else the preset's, else the track's own (`tmorphtime`), else the global `morphtime`, as for effects, blocks and chains.
- **`nosrc` and `nofx`** can be combined (values and sends only). They work with `tmorph` too.
- **Posted code:** `tload` posts what it does, as `csload` and `blkload` do now: the `blk` line for the source, the `fx` lines for the chain, then `vol`, `pan`, `trim`, `mute` and `snd`. A track preset can be read back as the commands that make it.

## What a track preset holds

A track preset holds everything that shapes the strip's sound and nothing that changes other strips or the session's routing.

| Part | In the preset | How it is written |
| --- | --- | --- |
| Effect chain | yes | as `cssave` writes it: each slot's unit, modulators, bypass, the chain's morph time |
| Trim, volume, mute | yes | dB values, as in the document since step 3 |
| Pan | yes | the panner's own arguments by name (`pan`, `pos`, …) |
| Lags (`vollag`, `trimlag`, `panlag`) | yes | only those that are not 0 |
| Mixer modulators (trim, volume, pan) | yes | by what they are, as for effects |
| Sends | yes | per bus: level, lag, modulator (to buses that exist when loading) |
| Source | yes | the blocks feeding the strip, each as `blksave` writes it; a list, so points can join later |
| Track morph time | yes | when set with `tmorphtime` |
| Solo | **never** | it silences other strips |
| Record arm | **never** | a session setting |
| Output (master or none) | yes (changed 17 Nov, see below) | `output: \master` or `\none`, as when saved |
| Channels, panner type | for information | fixed when the strip is made (until #44) |

By strip:

- **Track:** all of the above.
- **Bus:** no source (its input is the sends of other strips). Its own sends to other buses are kept.
- **Master:** no source, no sends. Its hardware outputs and limiter stay session settings.

## File format and folder

One readable file per preset, in the same format as the other unit presets, read with the same safe reader:

- Your own: `RadiclesFiles/Settings/Presets/units/track/<name>.scd`
- Libraries: `RadiclesLibs/<lib>/Settings/Presets/units/track/<name>.scd` (read too; your own wins on a name clash)

```
// track preset · track · 2 effects · source sine
(format: 1, type: \track, track: (
	from: \track, channels: 1, space: \pan2,     // for information
	trim: 0, volume: -6, mute: 0,
	pan: (pan: -0.3),
	lags: (volume: 2),
	mods: (volume: (type: \sin, ...)),
	sends: (bus1: (level: -12, lag: 0.5)),        // by bus, not by send slot
	chain: (fx: (1: (kind: \filter, def: \lpf, ...), 2: (...)), morphTime: 3),
	source: [ (kind: \block, def: \sine, args: (freq: 220), morphTime: 4) ],
	morphTime: 5                                  // tmorphtime, when set
))
```

- **Sends by bus.** Send slots follow bus order, so slot 1 can mean bus 1 on one strip and bus 2 on another. The bus name is what the sound depends on.
- **The source keeps no block number.** It goes to whichever block feeds the strip it is loaded onto (see Loading).
- **No old format.** Before this step there were no single-strip presets with values. Old whole-mixer presets (`PresetFile \tracks`) belong to sessions (step 6).

## Loading

`tload` changes only what differs, in an order that avoids hearing a half-loaded strip:

1. Stop any morph running on the strip (as `csload` does).
2. If the preset mutes the strip, mute it now.
3. **Source:** start the block with its sound file, table or data, and wait until it plays. A sound file can take a moment to load, so this comes first.
4. **Chain:** as `csload`. Same effect in a slot → new values; another effect → replaced; a slot the preset leaves empty → removed.
5. **Values:** trim, volume, pan, lags, mixer modulators.
6. **Sends:** set, added or removed to match the preset.
7. If the preset unmutes the strip, unmute it last.

**Which block plays the source.** A preset's source has no block number. On `tload t 3`, the source goes to the block feeding track 3, the way `blk 3 <> 3` or `start` connected it:

- **Same definition:** loaded like `blkload`: values, modulators, and the file or table the preset carries.
- **Another definition:** the block plays the new one (`Block.play`, a crossfade), then gets the preset's values. Unlike `blkload`, a track preset may change what the block plays, since the track's sound is the point.
- **Nothing feeds track 3** (rare: `start` and `t add` both connect a block; it happens when a track's input was taken away): a block is added and connected, as `blk add` then `blk 3 <> 3` would do, and plays the source. **Only an Ndef of your own feeds it** (`ndef x <> 3`): the source is skipped and named, so `tload` never disconnects code you wrote.
- **The block also feeds another track:** skipped and named, since loading it would change the other track too.
- **Several blocks feed the track:** matched in input order; extra ones are left as they are.
- **A preset with no source** stops the track's source, so the track ends as it was saved (decided 16 Nov).
- **Recordings and patterns:** as in step 2. A recording comes back only if it was kept with `keep`; patterns are not saved.

**Sends.** A send to bus 2 is made only if the session has a bus 2; otherwise it is skipped and named. Making buses is a session job (step 6). A bus never sends to itself. Sends the strip has but the preset doesn't are removed (question 5).

**Pan across panners.** Carried by argument name; an argument the target's panner doesn't have is skipped and named (a `pan2` preset on a `bal2` strip).

**Another kind of strip.** A track preset loads onto a bus or the master: parts the target can't have (source; sends on the master) are skipped silently, since that is expected. The chain adapts to the channel count as `csload` does.

## Morph

`tmorph` moves every part of the strip to the preset over **one** morph time, so everything arrives together. Each part morphs the way its own command already does:

| Part | Difference | What happens |
| --- | --- | --- |
| Chain | any | as `csmorph`: slot by slot, using the slot rules of step 3 |
| Source | same block definition | as `blkmorph` (xfade by default, or glide; a sound file keeps its place) |
| Source | another definition | crossfade from the old block to the new one |
| Trim, volume, pan | other values | glide (a lag over the morph time, then the strip's own lag is put back) |
| Send | same bus, other level | glides |
| Send | in the preset only | made at −inf, glides up |
| Send | on the strip only | glides to −inf, then removed |
| Mixer modulators | differ | fade in or out, as effect modulators do |
| Mute | off → on | switches at the end |
| Mute | on → off | switches at the start |
| Any | the same | untouched |

- **Shape:** `xfade` or `glide` applies to the chain and the source; values and sends always glide. `none` jumps to the preset at once (a `tload`).
- **One time for all:** the per-slot, per-block and chain morph times are not used during a `tmorph` (question 6).
- **One morph per strip:** a new `tmorph`, `tload`, `csmorph` or `blkmorph` on the strip stops the running one where it is.
- **A command during a morph** wins for what it sets; the rest continues, as in steps 1–3.
- **CPU:** only differing parts are doubled during the morph. A changed source plus two changed slots runs three extra units for the morph time.

## In the document

Each strip gains three fields (input and sends in PR 1, trackMorphTime with tmorphtime in PR 3), so a track preset is the strip's part of the document with session-only fields left out:

```
track1: (
	kind: \strip, channels: 1, space: \pan2,
	trim: 0, volume: -6, mute: 0, pan: (pan: -0.3),
	lags: (...), mods: (...), fx: (...), chainMorphTime: 3,   // step 3
	input: [\block1],                         // new: what feeds the strip, by Ndef key
	sends: (bus1: (level: -12, lag: 0.5)),     // new: by bus
	trackMorphTime: 5,                         // new: tmorphtime, when set
	solo: 0, output: \master                   // solo never in a preset; output in it since 17 Nov
)
```

- **The blocks stay where they are** (`blocks.block1`, step 2). The strip records only which ones feed it; `tsave` copies their units into the preset.
- **Read as in step 3.** Send levels and lags are read from the bus Ndefs when the document is read, so send knobs and MIDI need no hook. Hooks only follow structure: an input connected (`Assemblage:input`), a send added or removed (`bus`, `removeBus`).
- **Send modulators** (`modsndset`) go in `sends.bus1.mod`, read from `modSendArr` by bus, never by position.
- **Consistency check** as in steps 1–3: after every test command, the document is compared with one built from scratch.

## PRs, tests and risks

Three PRs into `dev`, each with its tests, a full cloud run and a Mac run of the mixer and source-window tests:

1. **Sends and inputs in the document:** `input`, `sends` and `trackMorphTime` on each strip, hooks for inputs and sends, the consistency check across the suite. No visible change; doesn't depend on your answers.
2. **Track presets:** `tsave` / `tload` (with `nosrc`, `nofx`, `default`, the list), loading onto other strips, skipped parts named. The mixer window and the source window follow the change (hooks from #58 and #59).
3. **Track morph:** `tmorph` and `tmorphtime`, the morph table, a morph interrupted by a command and by another morph.

**Done when**

- Round trips are identical (save, change everything, load) for a mono track playing a synth block, a stereo track playing a sound file, a bus and the master. Each has two effects, a modulator, a send and a lag.
- A preset from track 1 loads onto track 3, onto a bus and onto the master, skipping exactly what it should and naming it.
- Edge cases are skipped and named: a missing bus, a panner without the argument, a block feeding two tracks, a track with nothing feeding it.
- `tmorph`: every row of the morph table has a test, plus two interruptions.
- Timing: `vol`, `pan`, `snd` take the same time as on `dev` (no hook on values). Reading the document with 8 tracks, 2 buses, 10 effects and 6 sends stays well under a second.
- You try `tload` and `tmorph` by ear on the Mac.

| Risk | What we do |
| --- | --- |
| A sound file takes time to load, so the chain and values would land before the source | the source loads first and the rest waits for it; a test checks the order of posted code |
| Send slots are kept by position in `busArr` | sends keyed by bus name in the document and presets; a test removes a bus in the middle |
| A morph of source + chain + sends doubles many units | only differing parts are doubled; measured on the Mac with a source change and four effects |
| The mixer window shows old knobs or sources after `tload` | load through the same methods `snd`, `fx` and `blk` use, which already update the windows |
| Window jumps on rebuild (16 Nov) | not handled here: comes with the GUI rebuild in step 5 |

## Questions for you

Each has a proposal; a yes is enough.

1. **A live input as the source.** A track fed by `in 1 <> blk 2` plays the `audioin` block. Should its preset bring back hardware input 1? *Proposal: yes.* It is a block like any other, and the input is part of the track's sound. Unlike the output, it doesn't change other strips.
2. **`tload t 1 default`.** *Proposal: a clean strip.* That means trim 0, volume 0 dB, pan centre, unmuted, no lags, no modulators, an empty chain and no sends, with the source left alone. This is like a new track, and like `csload default` for the chain.
3. **Nothing feeds the track.** *Proposal: skip the source and name it.* The other choice is to play the block with the track's number and connect it (`blk 3 <> 3`), as `start` would.
4. **A preset with no source** (saved from a track nothing fed). *Proposal: leave the track's source playing.* Stopping it during a performance seems too strong; `st` does that when wanted.
5. **Sends the preset doesn't have.** *Proposal: remove them.* A send is part of the strip's sound, as an empty slot in a chain preset removes the effect. The other choice is to keep them, so a preset never takes a send away.
6. **One morph time for everything in `tmorph`.** *Proposal: yes.* Every part arrives together. The chain's, slots' and block's own times are used only by their own commands.

## Decisions (16 Nov)

1. **Live input:** yes, a track preset brings back the `audioin` block and its hardware input.
2. **`tload default`:** a clean strip (trim 0, volume 0 dB, pan centre, unmuted, no lags, no modulators, empty chain, no sends); the source is left alone.
3. **Nothing feeds the track:** a block is added, connected and plays the source; a track fed by your own Ndef keeps it, and the source is skipped and named.
4. **A preset with no source** stops the track's source when loaded, so the track ends as it was saved.
5. **Sends the preset doesn't have** are removed.
6. **`tmorph`** uses one morph time for every part.

## Built (17 Nov)

Three PRs into `dev`, each based on the one before, so merge them in order:

| PR | What | Cloud suite | Mac |
| --- | --- | --- | --- |
| [#89](https://github.com/freuben/Radicles/pull/89) | sends and inputs in the document; `sndry` with a value turns the output off | 482: 429 pass, 13 known, 0 fail | strips pass |
| [#90](https://github.com/freuben/Radicles/pull/90) | `tsave` / `tload` (`nosrc`, `nofx`, `default`, list) | 495: 442 pass, 13 known, 0 fail | track-presets 13/13 |
| [#91](https://github.com/freuben/Radicles/pull/91) | `tmorph` / `tmorphtime` | 504: 451 pass, 13 known, 0 fail | track-morph 9/9, mixer, source-window and preset cases pass |

How the build differs from this note:

- **`input` and `sends` are read when the document is read,** like the other strip values, so there are no hooks for them.
- **Modulators during `tmorph`** (mixer and send) change at the start and fade with the Ndef's own fade time, as effect modulators do.
- **A `csmorph` or `blkmorph` during a `tmorph`** takes over its own part; the rest of the morph carries on.

## Changed (17 Nov): the output to the master

Federico: the output to the master should always be as it was when the track was saved. A strip that only sends (`sndry`) is the reason: loaded onto a track that goes to the master, its dry signal was suddenly heard.

- **`tsave`** saves `output: \master` or `\none` for tracks and buses; the master strip has no output to save.
- **`tload`** brings it back: a preset that doesn't go to the master turns the output off first (with mute); one that does turns it on last (with unmute).
- **`tmorph`:** on at the start, off at the end, so the strip is never silent partway through (as mute).
- **`tload default`** sends the strip to the master, with no sends.
- **A preset saved before** has no output and leaves the strip's as it is.
- Solo and record arm stay out of presets.

## Changed (18 Nov): `tload default` has no source

Federico: the default preset should go to the master and have no source on; leaving the source alone was confusing.

- **`tload t 1 default`** stops the blocks feeding the track, as a preset saved with no source does (decision 4). A block that also feeds another track keeps playing.
- **`tload t 1 default nosrc`** keeps the source, as with any preset.
- **`tmorph t 1 default`** fades the source out over the morph time.
- The output to the master is on, as decided on 17 Nov.
