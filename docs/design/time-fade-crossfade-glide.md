# Time in Radicles: fade, crossfade, glide

Status, 19 Nov 2026:

- **Fade and crossfade: decided,** and built in two PRs. The first (`fade N`
  for everything, own fade times, one-off times) is done; the second (loading
  a preset is a crossfade, the `morph` words removed) is next.
- **Lag, glide times and `mixlag`: on hold.** Federico wants to think about
  them more. The lag commands stay as they are, and a load with `glide`
  glides over the fade time (or the time typed) until this is decided. The
  sections on lag below are a proposal, not a decision.
- Local fade spellings agreed: `fade t 1 4`, `fade b 1 4`, `fade m 4`,
  `fade blk 1 4`, `fade fx t 1 2 4`, `fade fx m 1 4`.

Model: Fable.

## Why

Trying release 1 by ear showed that time is handled by several rules that
don't agree: `fade 4` only reached things made after it, `sndset` ignored it,
presets load at once but morph over another time, and each kind of thing has
its own lag and morph-time commands. Federico asked for one clear design to
stick to, then a pass over the code to make it consistent and remove the rest.

## How JITLib works

Read from the class library of SuperCollider 3.14.1 (`JITLib/ProxySpace/`).

- **`fadeTime` is the crossfade time of one proxy.** It is used when the
  source is replaced (old and new overlap), when it is set to nil, and by
  `free`, `release`, `clear`, `end`. It is stored as a value in the proxy's
  node map; the default is 0.02 s (`NodeProxy.sc:86–92`).
- **`ProxySpace.fadeTime = 4` reaches every proxy in the space, existing and
  new** (`ProxySpace.sc:57–59`). So "fade applies to everything running" is
  JITLib's own behaviour for a space; Radicles just never called it.
- **`set` is immediate.** No fade, no glide.
- **`xset` is a crossfade to new values:** a second copy of the synth starts
  with the new values and the two overlap for `fadeTime` (`NodeProxy.sc:353`).
- **`lag` is something else.** `proxy.lag(\freq, 2)` gives one control a
  smoothing time and **rebuilds the synth** to do it (`NodeProxy.sc:250–253`).
  It has nothing to do with `fadeTime`, it is per control, and the rebuild is
  itself a crossfade.
- **A fade starts one server latency (0.2 s) after it is asked for.** JITLib's
  own `end` waits `fadeTime + latency` before it stops the monitor
  (`NodeProxy.sc:38–44`). Radicles freed buffers at `fadeTime`: the cut heard
  with `tload 1 default`, fixed in #110.

So one correction to the starting point: in JITLib `fadeTime` governs fades
and crossfades, **not** glides. Using one number for both is a rule Radicles
can choose (I think it should), but it is ours, not JITLib's.

## What Radicles has now

Five kinds of time, set in different places:

| Time | Default | Set by | Reaches |
|---|---|---|---|
| Session fade (`Radicles.fadeTime`) | 0.5 | `fade N` | only what is made afterwards |
| Each Ndef's own `fadeTime` | copied from the session fade when made | `blk stop 1 8`, morphs (for their duration) | that Ndef |
| Global morph time (`RadUnit.morphTime`) | 2 | `morphtime N` | `fxmorph`, `blkmorph`, `csmorph`, `tmorph` |
| Own morph times | none | `morphtime t 1 2 5`, `blkmorphtime`, `csmorphtime`, `tmorphtime`, or inside a preset | one slot, block, chain or strip |
| Lags | 0 | `vollag`, `panlag`, `panxlag`, `trimlag`, a time after `sndset`; `fxlag`, `fxlagn`, `fxlags`, `lagfx`, `lagsfx`, `blklag`, `blklagn`, `ndeflag` | one strip value or one parameter |

A morph picks its time from four places in order: the command, the preset,
the slot's own, the global one. It picks its shape (`xfade`, `glide`, `none`)
from the command, then the preset, then `xfade`.

Loading and morphing are separate commands: `fxload`, `blkload`, `csload`,
`tload` set everything at once; `fxmorph`, `blkmorph`, `csmorph`, `tmorph` do
the same over time. That is about 30 command names and 100 forms concerned
with time.

## First assessment (before Federico's answers; he agreed with the four disagreements)

**Where I agree with Federico**

1. **One session time.** `fade N` should be the pace of change of the whole
   system, and reach everything running, effects included. I suggested leaving
   effects out; with one time there is no reason to.
2. **The mixer is different.** Volume, pan, trim and sends are played by hand,
   from the GUI and from MIDI. They should not follow `fade`.
3. **One mixer lag** instead of `vollag`, `panlag`, `trimlag`, send lags.

**Where I disagree**

1. **Not a different default per kind of thing.** The proposal was: blocks and
   effects glide, chains and tracks crossfade, except when a buffer is
   involved. I recommend **crossfade as the default everywhere, glide as a
   word you add.** Reasons:
   - A crossfade works for every change: values, another sound file, another
     effect, a switch, a bypass. A glide only works for numbers that can move
     smoothly, so "glide by default" always needs a list of exceptions, and
     the performer has to know which case they are in.
   - Some glides sound wrong unless you want them: a delay time gliding bends
     the pitch, a pitch ratio gliding is a glissando. As an effect it is good;
     as what happens when you didn't ask, it isn't.
   - A glide is not cheaper or cleaner: giving a control a lag rebuilds the
     synth, which is a short crossfade anyway.
   - It is the rule already decided on 3 Nov ("crossfade is the default for
     every block") and built in all four morphs.
   - It is JITLib's own model: old fades out while new fades in.

   Glide stays one word away (`fxload t 1 1 warm glide`), and a preset can
   say it prefers to glide (see question 3).

2. **A track cannot "crossfade" as a whole, and that is fine.** On a strip the
   source and the effects crossfade; volume, pan, trim and sends glide over the
   same time, because a gain crossfading with itself is just a glide. `tmorph`
   already works this way. So the honest rule is: *sounds crossfade, mixer
   values glide, over one time, arriving together.*

3. **Loading and morphing should be one thing.** If a preset load takes the
   session fade by default, `fxload` and `fxmorph` do the same job. I'd keep
   the `load` words and remove the `morph` words. "Morph" is then free for
   what step 7 means by it: a fader that moves between two presets.

4. **Own morph times should go.** Four places to look for a time is the kind
   of rule nobody can keep in their head on stage, and sessions would have to
   save all of them. Two places are enough: the time typed in the command,
   else `fade`.

## Decided (Federico, 19 Nov 2026)

- **Two times, as in JITLib: `fade` and `lag`.** `fade` is the time of fades
  and crossfades; `lag` is the time parameter values take to glide. Both
  default to 0.5 s.
- **Global and local.** `fade N` and `lag N` set the time for everything.
  A single thing can be given its own fade or lag afterwards. A later
  `fade N` or `lag N` overwrites those again (as `ProxySpace.fadeTime` does).
- **The mixer is apart:** its values are immediate, smoothed by one
  `mixlag N` (default 0.05 s). `lag` does not touch it.
- **Crossfade is the default** for every preset load; `glide` is a word you
  add. A preset may say which it prefers (`morphShape`); it carries no time.
- **Loading and morphing are one thing.** The `morph` words and the own morph
  times are removed, with no aliases.
- **A time typed in a command** wins for that command only.
- **Parameters keep separate lags;** a load with `glide` overrides them for
  that transition.

## The rules

1. **`fade N`** sets the fade time of everything running and everything made
   later: blocks, effects, tracks, buses, the master. `fade` alone posts it.
   Default 0.5 s.
2. **`lag N`** sets the glide time of the parameters of every block and
   effect, running and later. `lag` alone posts it. Default 0.5 s.
3. **Local times** live in the Ndef itself, where JITLib keeps them, so the
   document reads them like any other value and a session saves them. Nothing
   in Radicles keeps a second list. Proposed spellings (question 2):
   `fade t 1 4`, `fade b 1 4`, `fade m 4`, `fade blk 1 4`, `fade fx t 1 2 4`;
   `lag blk 1 4`, `lag fx t 1 2 4`; one parameter: `fxlag t 1 2 freq 4`,
   `blklag 1 freq 4` (these exist).
4. **Global overwrites local.** After `fade 2`, everything fades over 2 s,
   whatever it had.
5. **A time typed in a command is for that command only:** `blk stop 1 8`,
   `fxload t 1 1 warm 10`, `t rm 2 4`. The thing keeps the time it had.
6. **Loading a preset is a transition.**
   - Default: a crossfade over the unit's fade time.
   - With `glide`: the values glide over the session `lag`, whatever lags the
     parameters have; their own lags come back afterwards.
   - A time of 0 sets the values at once.
   - Another definition, sound file or table always crossfades; `glide` says
     so and crossfades. A sound file keeps its place.
   - Chain and track presets: every part moves over one time (the session
     fade, or the time typed) and arrives together. Sounds crossfade; volume,
     pan, trim and sends glide over that same time, not over `lag` or
     `mixlag`.
7. **Mixer values are immediate:** `vol`, `pan`, `trim`, `sndset`, mute, solo,
   from the call window, the GUI and MIDI, smoothed by `mixlag N` (default
   0.05 s). A time typed after a value is a one-off glide: `vol t 1 -12 4`.
8. **Removing is a fade out** over the thing's fade time (or the time typed);
   buffers are freed when the fade has really ended.

## What I think of it

It is a better design than my one-number proposal. It follows JITLib's own
two concepts, so the times live where JITLib keeps them and Radicles needs no
lists of its own; "global overwrites local" is one sentence to remember; and
it answers the worry I raised (quick entries and slow changes) without a
second kind of morph time.

Three things I'd still watch, the first one serious.

### The open point: what `lag` does to a parameter

"`lag N` sets the glide time of every parameter" means that from then on
`fxset t 1 1 freq 500` glides to 500 over the lag time instead of jumping.
That is new behaviour (today a set is immediate unless that parameter was
given a lag), and I think it is good: a typed value arrives smoothly, as a
source arrives with a fade. But it has consequences that need a decision:

- **In JITLib, changing a lag rebuilds the synth.** `fade 4` costs nothing (it
  sets a value). `lag 4`, built the JITLib way, rebuilds every block and
  effect at once: each one crossfades with a new copy of itself, delays and
  reverbs start again with empty tails, sound files have to be put back at
  their place. Typed in the middle of a piece, `lag 4` would be an audible
  event. Giving one parameter its own lag has the same cost for that unit.
- **A lag smooths whatever arrives at the parameter, including a modulator.**
  A parameter with a 0.5 s lag flattens a 10 Hz LFO mapped to it. So a
  modulated parameter must have its lag taken off while it is modulated.
- **Some parameters must never glide:** a trigger, a buffer or table number, a
  switch, a value that is rounded to steps (`pch`'s `pitchRatio` rounds to
  semitones before it is used). A lag on those gives wrong sound, not smooth
  sound. The definitions don't say today which parameters these are.
- **A MIDI knob on a parameter with a 0.5 s lag feels slow.** That is what the
  local lag is for (`fxlag t 1 1 freq 0.05`), but it has to be remembered.

There are two ways to build it:

| | A. JITLib's lag (control rates) | B. A lag control in each unit |
|---|---|---|
| How | `Ndef.lag(\freq, 4)`, as `fxlag` does now | Radicles writes each unit's function with its continuous parameters as `freq.lag(lagTime)`, as the mixer strips already do |
| `lag N` | rebuilds every unit (audible) | one `set` per unit: silent, instant |
| Local lag | per parameter, native | per unit; per parameter needs one extra control each |
| Parameters that must not glide | must be listed either way | must be listed either way |
| Cost | none to build, it exists | the code generator changes; every effect and block snapshot changes once |

**My recommendation: B,** with the lag per unit and, for a parameter that
needs its own, one extra control. It makes `lag N` as cheap and silent as
`fade N`, it is the pattern the mixer already uses, and a load with `glide`
no longer needs a rebuild before it can start. But I would not decide it on
paper: **a one-day trial first** (Fable), on `pch`, `dl1` and a sound-file
block, to hear what A's rebuild sounds like under `lag N`, to check B with a
modulator and a MIDI knob, and to find how the definitions can mark the
parameters that must not glide. The note gets its last section from that.

### Two smaller points

- **`fxset` gliding by default is a change performers will notice.** Anyone
  who wants a jump types `lag 0` or gives the parameter a lag of 0. I think
  that is right, but it should be a decision, not a side effect. Question 1.
- **Local times are easy to lose.** `fade 2` wipes every local fade, by
  design. Fine on stage; in a session it means the order of lines matters
  (global first, then local). Sessions save the times each thing has, so a
  loaded session comes back right whatever the order was.

## What changes for the performer

| Now | Proposed |
|---|---|
| `fade 4` reaches new things only | reaches everything, and posts the lines |
| no global lag | `lag 4`: every block and effect parameter glides over 4 s |
| `fxset t 1 1 freq 500` jumps | glides over the lag (0.5 s by default) |
| `fxload`, `blkload`, `csload`, `tload` set at once | transition: crossfade over the fade; `glide` and a time may follow |
| `fxmorph`, `blkmorph`, `csmorph`, `tmorph` | removed |
| `morphtime`, `blkmorphtime`, `csmorphtime`, `tmorphtime`, a morph time inside a preset | removed; a preset file that has one is still read, the time is ignored |
| a preset's `morphShape` | kept |
| `vollag`, `panlag`, `panxlag`, `trimlag` per strip | removed; `mixlag N` for the whole mixer |
| `sndset t 1 1 -12` at once | the same; `sndset t 1 1 -12 4` glides once |
| `blk stop 1 8` leaves block 1 with an 8 s fade | the 8 s is for that stop only; `fade blk 1 8` to keep it |
| `fxlag`, `blklag` (one parameter) | kept |
| `fxlagn`, `fxlags`, `lagfx`, `lagsfx`, `fxgetlag`, `getlagfx`, `blklagn`, `ndeflag` | question 3 |

## Costs and risks

- **A preset load is no longer instant by default** (a 0.5 s crossfade), and a
  typed value no longer jumps. Both are one word or one setting away.
- **A crossfade doubles the CPU of that unit while it lasts.** Long fades on
  heavy effects (RAVE models) cost; `glide` avoids it.
- **Feedback and delay effects** start the new copy empty, so a crossfade is
  heard as the old tail fading while the new one builds.
- **Many snapshots change.** About five pieces of work: the lag trial; `fade`
  reaching everything, with local fades; `lag` (as the trial decides); load
  as transition and the morph words removed; `mixlag`. Engine work, so Fable,
  each with a second look.
- **Sessions:** two session times, one mixer lag, and the local times read
  from the Ndefs.

## Questions for Federico

1. **`fxset` and `blkset` glide over the lag by default** (0.5 s)? That is
   what "lag sets the glide time of parameter values" means in practice. Or
   should a typed value still jump, and the lag only be used by loads with
   `glide` and by parameters given a lag on purpose?
2. **The spellings for local times:** `fade t 1 4`, `fade blk 1 4`,
   `fade fx t 1 2 4`, `lag blk 1 4`, `lag fx t 1 2 4`? Or your own.
3. **The other lag spellings** (`fxlagn`, `fxlags`, `lagfx`, `lagsfx`,
   `fxgetlag`, `getlagfx`, `blklagn`, `ndeflag`): remove, keeping `fxlag` and
   `blklag` (and a way to post a lag)?
4. **The one-day lag trial before the rest is decided:** yes?
5. **When:** release 1 as it stands tonight, then this (my recommendation), or
   hold the release?
