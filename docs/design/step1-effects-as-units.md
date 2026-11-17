# Step 1 Design Note – Document, Units and Effects

Nov 2, 2026 · @Federico

## Scope of step 1

The first step of the [Document Model – Implementation Plan](https://claude.ai/artifact/46gLjvaiyBK8MfwSKYnMXS). For Federico to review before any code is written.

**In step 1**

- The document class: structure, addresses, get and set, change times, notifications, writing and safe reading of files, comparing two documents.
- The unit interface, used first for effects: reading an effect's state from the running system, applying a state to an effect, morph policy, and a place for position.
- Effects in the document, kept up to date after every effect change.
- Effect presets in a new, readable file format (`fxsave` / `fxload`, same commands).
- Effect morph (new command) and a default morph time; effect bypass (new command and button).
- The client protocol, as a design only (built in step 5).

**Not in step 1:** blocks (step 2), strips (steps 3 and 4), windows as clients (step 5), sessions (step 6). The document does not yet own anything: in step 1 it is a faithful description of the effects, built from the running system (see section 5). Effects keep working exactly as today.

## Where effect state lives today

From reading `AssemblageFx.sc`, `AssemblagePresets.sc`, `AssemblageMod.sc` and `ModMap.sc`. An effect's state is spread over seven places:

| Where | What it holds |
| --- | --- |
| `Assemblage.filters` | a list of `[tag, name]`, e.g. `['filterTrack_1_2', 'pch']`, kept sorted by slot |
| `Assemblage.tracks` | per strip, the chain of `[ndefKey, function]` pairs, effects included |
| `Assemblage.specs` | per strip, the argument specs of each stage, effects included |
| `trackDataArr`, `trackBufferArr`, `filterBuff` | data an effect was given (the convolution reverb's impulse response) and buffers it uses |
| the effect's Ndef | current argument values (its node map), and its fade time (0.5 s) |
| `ModMap.modNodes`, `ModMap.modInfoArr` | for each modulated argument: the modulator Ndef, the target Ndef and argument, the value before modulation, and how the modulator was made (type, spec, extra arguments, function, mul, add, min, val, warp, lag); a modulator can itself be modulated |
| the library | the effect's function (`SynthFile`), specs (`SpecFile`) and description |

**Today's effect presets:** `fxsave` writes an array (effect name, argument values, buffer and data, modulators) with `PresetFile` into one file per kind under `Settings/Presets`. `fxload` only works when the slot already holds an effect of the same name; it then sets the values and rebuilds the modulators.

**Good news for addresses:** effect tags are named by slot number (`filterTrack_1_10`), not by position, so adding or removing an effect never renames another.

## The document

A new class, working name **`RadDoc`**, holding plain SuperCollider data (Events, Arrays, Symbols, Strings, Numbers), so it can be written as readable code, read back safely and compared.

**Structure in step 1** (only effects; later steps add the rest):

```
(
  format: 1,
  strips: (
    track1: (fx: (1: <unit>, 2: <unit>)),
    bus1:   (fx: (1: <unit>)),
    master: (fx: (1: <unit>))
  )
)
```

Effect slots are Integer keys, matching today's slot numbers.

**Addresses.** A path from the top: `track1.fx.2.args.pitchRatio`. In code it can be written as a Symbol or String with dots, or as an Array (`[\track1, \fx, 2, \args, \pitchRatio]`); `strips` is implied for strip names.

**API (first version)**

| Method | Does |
| --- | --- |
| `RadDoc.current` | the document of the running session |
| `doc.at(address)` | the value at an address, or nil |
| `doc.put(address, value)` | sets it, records the time, notifies |
| `doc.removeAt(address)` | removes it, records the time, notifies |
| `doc.changedAt(address)` | when that address last changed (kept beside the data, not in it) |
| `doc.copy` | a deep copy (later: scenes, undo) |
| `doc.asLiteral` | the data as readable SuperCollider code, one level per line |
| `RadDoc.read(path)` / `doc.write(path)` | read safely (section 6) and write a file with a header |
| `RadDoc.diff(a, b)` | the differences: added, removed or changed, each with its address and old and new values |

**Notifications** use SuperCollider's own dependants mechanism (`addDependant`, `changed`, `update`): every `put` or `removeAt` calls `changed(\put, address, value)`. Windows and the protocol subscribe this way in step 5; in step 1 only the tests listen. Notifications for one address are throttled when values arrive faster than the GUI rate.

## Units

A unit's state is an Event. The same shape will serve blocks (step 2) and points (step 8). An effect in slot 2 of track 1:

```
(
  kind: \filter, def: \pch,
  args: (pitchRatio: 0.5, pitchDispertion: 0, timeDispersion: 0.0001, winSize: 0.02),
  mods: (
    pitchDispertion: (type: \sin, spec: [0, 1], mul: 1, add: 0, args: (freq: 5, phase: 0))
  ),
  data: nil, buffer: nil,
  morphShape: \xfade, morphTime: nil,
  bypass: false,
  position: nil
)
```

- **`args`** holds only the arguments of the unit's own definition (from the library function's argument names), so values left in an Ndef by an earlier unit are not saved. For a modulated argument, `args` keeps the value it returns to when the modulation is removed.
- **`mods`** describes each modulator by what it is: type, spec and the other settings `ModMap` records, plus the modulator's own argument values (`modfxset`). A modulated modulator nests the same way. Modulators are never stored by their number (`mod3`), which can change.
- **`data` and `buffer`** name what the effect was given (for example the convolution reverb's impulse response), by name and path.
- **Functions** (a random value such as `{[0, 5, 7].choose}`) are stored as their source text, `(code: "{[0, 5, 7].choose}")`, and run again when applied, so random stays random.
- **Empty fields:** a SuperCollider Event cannot hold `nil` (putting `nil` removes the key), so a field with no value, such as `data` for most effects, is simply absent from the document and its files; reading it gives `nil` as before.
- **`morphShape` and `morphTime`** are the unit's own morph settings. `morphTime` nil means "use the global morph time" (2 seconds by default); a number sets this unit's own time, saved with its presets. `morphShape` is `\xfade` by default; units that cannot crossfade without an audible jump (sound-file and gated blocks, step 2) are fixed to `\glide`. See section 7.
- **`position`** is not used by effects: an effect processes what comes in and has no place in time of its own, so for effects it is always nil. It exists because every unit (effects, blocks, later points) shares one shape: blocks and points that play through time report a position and accept a start position (step 2). An effect that played a buffer through time, such as a looper, could use it in future.

**Reading a unit from the running system** (`RadUnit.read(tag)`): the name from `filters`; argument names from the library function; values from the Ndef's node map; modulators from `ModMap`; data and buffers from `trackDataArr` and `trackBufferArr`.

**Applying a state to a slot** (the first version of the reconciler, for one unit). It always goes through Assemblage's existing methods, so the bookkeeping, the GUIs and the posted code stay as they are:

| Situation | What happens |
| --- | --- |
| slot empty, or a different effect | `filter` inserts the effect with its arguments (a replaced effect crossfades by the Ndef's fade time) |
| same effect, different values | `set` for the changed arguments (or the morph, section 7) |
| a modulator added, changed or removed | the existing modulation and unmodulation methods |
| data or buffer differ | the effect is inserted again with the new data |

## Keeping the document up to date in step 1

In step 1 the document describes the effects; it does not own them. Values change in more ways than through Assemblage's methods: a controller mapped with `HIDMap` sets an effect's Ndef directly through generated code, and code typed in the IDE can too. So step 1 splits the work:

- **Structure by hooks:** which effects exist in which slots, their data and buffers, and their modulators are updated in the document at the end of the Assemblage methods that change them: `filter`, `removeFilter`, `removeTrackFilters`, `removeAllFilters`, the modulation and unmodulation methods for effects, `loadFxPreset` and the convolution-reverb path. Each hook runs only after the operation has finished.
- **Values when read:** argument values are read from the Ndefs whenever the document is read for a purpose (saving a preset, a query, a comparison). This catches values set by controllers, the IDE or anything else, and adds nothing to fader or MIDI latency.

**Consistency check in the tests:** after every command in the whole suite, the document (refreshed) is compared with one rebuilt from scratch from the running system. A missing hook shows up as a difference, named by its address.

**Looking ahead to step 5:** when the document becomes the owner, controller mappings and other direct sets will go through it as well; until then nothing about how they work changes.

## Preset files

**Presets belong to their effect,** like a plugin's presets: one readable file per preset, in a folder per kind and per effect, for example `Settings/Presets/units/filter/pch/warm-shift.scd` (blocks later in `units/block/<name>/`):

```
// Radicles unit preset · format 1 · filter pch · saved 2026-11-02 10:14
(
  format: 1, type: \unit,
  unit: (
    kind: \filter, def: \pch,
    args: (pitchRatio: 0.5, pitchDispertion: 0, timeDispersion: 0.0001, winSize: 0.02),
    mods: (), data: nil, buffer: nil,
    morphShape: \xfade, morphTime: 4, position: nil
  )
)
```

One file per preset (instead of today's one file per kind) makes presets easy to read, edit, copy, share through the UserLib on GitHub, and compare in version control.

**Commands keep their form:** `fxsave t 1 2 warm-shift` writes the preset into the folder of the effect in that slot; `fxload t 1 2 warm-shift` applies it to that slot through the reconciler (section 4). As today, the slot must already hold the same effect: a `pch` preset loads only onto a `pch`. Listing presets shows only those for the effect in question, and the effect window gets a PRESET menu with that effect's presets, as in a plugin. If no new-format preset has that name, `fxload` falls back to today's presets, so nothing that works now stops working. Saving over an existing name asks first, as today.

**Safe reading (revised while building PR 1, 2 Nov):** files are never compiled or run. A small parser of its own, `RadLiteral`, reads the text and accepts only data: Events, Arrays, numbers, Symbols, Strings, Chars, `nil`, `true`, `false` and comments. Anything else is refused with the line and what was found, for example `line 3: unexpected name 'Server'`. The compile check first planned here turned out not to be enough when tested in SuperCollider 3.14: environment variables (`~x`, `~x = 1`), `thisProcess`, `currentEnvironment` and operators such as `+`, `++` or `.neg` compile without showing up as method calls. With the parser, nothing in a file can run. Functions the performer saved (random values) live inside as text and only run when applied, through the normal code path.

**Format version** in every file from the first one, with the converter framework from the proposal (none needed yet).

## Effect morph

**Command `fxmorph`,** in the same forms as `fxload`, with an optional time and an optional shape: `fxmorph t 1 2 warm-shift` (the unit's or global morph time, crossfade), `fxmorph t 1 2 warm-shift 10` (10 seconds), `fxmorph t 1 2 warm-shift 10 glide` (10 seconds, glide).

**The morph time** comes from, in order: the time given in the command; else the unit's own `morphTime` (set locally with `morphtime t 1 2 5`, or loaded from a preset); else the global morph time, set with `morphtime 5` and 2 seconds by default (to be reviewed in testing). For glide, the same time is each argument's lag.

**What it does, by shape** (`xfade` unless the command says `glide`, or the unit is fixed to glide):

| Policy | Code it posts and runs |
| --- | --- |
| `\xfade` (default) | `Ndef('filterTrack_1_2').fadeTime = 10;` then one `xset` with every changed argument, so it is a single crossfade; the fade time goes back to 0.5 when the morph ends |
| `\glide` | `lag` set to 10 for each changed argument, then one `set`; the earlier lag times are restored afterwards |
| `\none` | the values are set at once, with a note that this unit does not morph |

- **A different effect in the preset:** the new effect replaces the old with the Ndef's fade time set to the morph time, so the two crossfade.
- **Modulators:** a modulator the preset adds or changes is mapped with `xset` (as `ModMap` already does), so it fades in with the rest; one the preset removes returns to its value the same way.
- **A command during a morph** wins for its own argument; the morph continues for the others.
- **Fade time is always restored,** also if the morph is interrupted; a test checks it.

**Later (step 7):** the morph position as a 0–1 control, so a fader moves an effect between two presets; for plain values this is an interpolation between the two sets of numbers.

## Bypass

New in step 1, on Federico's suggestion: bypass an effect for auditioning, or as a musical gesture, without removing it.

**How it works (proposed):** bypassing routes the chain around the effect, so the stage after it reads the stage before it, exactly as if the effect were removed, while the effect's Ndef stays in place with all its settings. The re-routing crossfades over the Ndef's fade time, so it does not click. Un-bypassing routes the effect back in the same way.

- **Effect code unchanged:** no effect function has to be modified, and posted code only changes for bypass itself.
- **CPU and state:** a bypassed effect is paused (`Ndef(...).pause`), so it uses no CPU; resuming keeps its state, so a delay or reverb continues from where it was rather than starting empty. (Question 7.)
- **In the document:** `bypass: true` in the slot's unit, so the document always knows. It is saved in strip presets and sessions, not in effect presets, so loading an effect preset never bypasses or un-bypasses an effect.
- **Commands:** working name `fxbypass t 1 2` toggles; `fxbypass t 1 2 1` and `fxbypass t 1 2 0` set it; the same forms as other effect commands (`b`, `m`, mixer position).
- **Button:** a BYPASS button in the effect window next to REMOVE FILTER, lit while bypassed; the mixer's effect slot can show it too (step 5, when windows are rebuilt; a simple marker sooner if wanted).
- **Tests:** bypass and un-bypass keep the effect's settings; the chain routes around it; bypass state round-trips through a preset; the effect window button (Mac).

## Client protocol (design only)

Designed now so the document fits it; built in step 5. OSC messages, the same whether the client is on the audio computer or another one. Values that are structures travel as readable SuperCollider literals (checked with the same safe reading as preset files); single numbers travel as numbers.

| Direction | Message | Meaning |
| --- | --- | --- |
| in | `/radicles/hello` name key | a client introduces itself; remote clients must be allowed |
| in | `/radicles/cmd` source "command" | a Radicles command, with its source (`typed`, `button`, `menu`, `gui`), run through the code path like any other |
| in | `/radicles/get` "address" | ask for part of the document (`""` for all of it) |
| in | `/radicles/watch` "address" | receive changes under that address (`""` for all) |
| in | `/radicles/specs` def | ask for a unit's argument names and specs, to build its window |
| out | `/radicles/state` "address" "literal" | the requested part of the document |
| out | `/radicles/changed` "address" value | one change, after it has happened |
| out | `/radicles/code` channel "code" | posted code, as today's OSC code sharing (`Radicles.oscCode`) already sends it |
| out | `/radicles/meters` values… | meter values at the GUI rate, only to clients that ask |

Addresses such as `track1.fx.2.args.pitchRatio` also map onto OSC paths (`/track1/fx/2/pitchRatio`) for control surfaces; that comes later.

## Tests and done when

**New tests**

- **Document:** get, put, remove, change times, notifications, deep copy, write and read back, the safe reader refusing a file with a method call or a function, and diff.
- **Unit round trips for every effect in the Main library:** insert it, set a few values, `fxsave`, start a fresh SuperCollider, `fxload` into an empty slot and into a slot with another effect, compare the unit with the saved one.
- **Mixed cases:** two slots on one track, a bus and the master, a modulated argument, a modulated modulator, the convolution reverb with its impulse response, a slot that held another effect before (no leftover arguments), a random value stored as a function.
- **Morph:** `fxmorph` with each policy reaches the preset's values; the fade time and lag times are restored, also when interrupted; a different effect crossfades; posted code checked by snapshot.
- **Consistency check** after every command of the whole suite (section 5).
- **Old presets:** `fxload` of a preset saved with today's format still works.

**Done when**

- All of the above pass in the cloud, and on the Mac for the effect window.
- No existing snapshot changes, except `fxsave` and `fxload`, whose posted code changes by design (shown in the PR).
- `fx` and `fxset` take no longer than before (timed).
- Federico has tried `fxsave`, `fxload` and `fxmorph` on the Mac.

**PRs, in order:** (1) the document and the safe reader; (2) units and effects in the document, with the consistency check; (3) the new preset files; (4) bypass; (5) `fxmorph` and `morphtime`.

## Questions for Federico

Answered by Federico, 2 Nov:

- [x] **Morph shape:** chosen in the command (`xfade` by default, or `glide`); units that cannot crossfade cleanly are fixed to glide.
- [x] **`fxload`:** like a plugin preset, native to the effect: the slot must already hold the same effect; the effect window lists that effect's presets.
- [x] **Folder:** `Settings/Presets/units/filter/<effect>/` (and `units/block/<block>/` in step 2), in `RadiclesFiles` for your own presets. Added 3 Nov: an effect from a library (RAVE, ...) can also have presets in that library, `RadiclesLibs/<lib>/Settings/Presets/units/filter/<effect>/`, so they are shared with the library. Both are read and listed (the PRESET menu marks a library's, e.g. `warm · RAVE`); your own wins when both have a name. `fxsave` writes your own; `fxsave <effect> <preset> <library>` (e.g. `fxsave nnt raveSetting1 RAVE`) copies one of yours into the effect's library, refusing another library, and asking before replacing. Not in the SAVE window (for advanced users). Effects of the Main library keep their presets in `RadiclesFiles`. The same rule for blocks (step 2) and strip presets later.
- [x] **Morph command:** `fxmorph t 1 2 <preset> [time] [shape]`; time alone is the common case.
- [x] **Saving over an existing preset:** ask first.
- [x] **Names:** `RadDoc` and `RadUnit`.
- [x] **Bypass:** a bypassed effect is paused (no CPU).
- [x] **Bypass in presets:** only in strip presets and sessions, not in effect presets.
- [x] **Morph time:** saved with each preset, settable per unit (`morphtime t 1 2 5`) and globally (`morphtime 5`), global default 2 seconds.

Ready for PR 1 (the document and the safe reader).

Once these are answered, PR 1 (the document and the safe reader) can start.
