# Mixer moves: ramps, typed time and native time

Status: **decided with Federico on 20 Nov 2026 and being built** in four PRs
(see "Planned PRs"). Lags on blocks
and effects are not covered: that is a separate conversation.

## The idea

Every timed move in the mixer is an ideal hand on the control: it moves the
fader or knob evenly from where it is to the value asked for, and arrives
exactly on time. The curve is not a separate choice; it is the scale of the
control itself.

| Control | Its scale | A steady move sounds like |
|---|---|---|
| Volume | dB fader scale, the bottom is silence | a steady fader move |
| Sends | the same fader scale as volume | a steady fader move |
| Trim | -23 to +23 dB, even in dB | an even change in loudness |
| Pan | even in position | even movement across the space |

## Decided

1. **Ramps, not lags or crossfades.** A lag moves fast and then creeps, and
   has no exact arrival time. A crossfade (`xset`) cannot be used by a fader
   or MIDI, can bump when redirected, and cannot be used on a bus's shared
   input (sends).
2. **`tload` is the exception.** Its level crossfades with the rest of the
   track: one setting replaces another.
3. **`glide` in a load** moves the strip's values as steady moves over the
   load's time.
4. **Two times.**
   - **Typed time:** for values typed in the call window. Per control:
     `vollag`, `panlag`, `trimlag`, and a new `sndlag`. `mixlag N` sets them
     all; a lag typed afterwards changes one again, and a later `mixlag`
     overwrites it (the rule of `fade N`).
   - **Native time:** for the mixer window's faders and knobs, MIDI, HID and
     OSC. Very short (about 50 ms), only to remove clicks and steps. One
     setting for the whole mixer (`ctllag N`), adjustable, rarely touched. A slow typed
     time never slows the hands.
5. **A time typed after a value belongs to that one move.**
   `vol t 1 -12 4` takes 4 seconds; the next plain `vol t 1 0` uses the
   strip's own typed time. Today the 4 sticks to the strip. The same for
   `pan`, `trim` and `sndset`.
6. **A new strip's typed time is the short smooth move,** not 0. A lag of 0
   still gives a true jump.
7. **`panxlag` is removed.**
8. **No per-move curves.** A different feel is a different scale on the
   control, which the window's fader and a MIDI fader then share.
9. **How they meet.** A native move replaces a typed move at once, from
   wherever the value is; a typed move starts from where the hand left it.

## How it would be built: checked 20 Nov (offline renders and reading)

**Use SuperCollider's own linear `VarLag` (`warp: \lin`), not a ramp written
from `EnvGen`.** This reverses Claude's first proposal. The renders showed:

| | Arrives on time, redirects without a jump | Two values 1.4 ms apart | A modulator on the control |
|---|---|---|---|
| `Ramp` | no (reads its input once per ramp time) | | |
| A ramp from `EnvGen`, restarted on a change | yes | **stops at the first value** | **flattened completely** |
| `VarLag`, default form (the same construction inside) | yes | **stops at the first value** | **flattened completely** |
| `VarLag` with `warp: \lin` (its own UGen) | yes | ends at the right value | untouched at time 0; dulled at 50 ms |

The `EnvGen` forms restart on a trigger, and a trigger cannot fire in two
neighbouring control blocks; the linear `VarLag` compares its input every
block. Since no per-move curves are wanted (decision 8), the linear form is
all that is needed.

Other results:

- **The fader scale inside the strip:** position squared times `6.dbamp`
  gives exactly the values of the volume spec (`[-inf, 6, \db]`). The strip
  can keep receiving dB: it converts to position, ramps, and squares. So the
  Ndef, the document and presets still hold dB.
- **To silence and back:** 0 dB to `-inf` over 2 s, then to -12 over 1 s: no
  invalid numbers, no step in the gain. A quarter of the way down it is at
  -5 dB, half way -12 dB, three quarters -24 dB.
- **A stream of controller values** (100 in a second) with a 50 ms native
  time: smooth, and the level runs about 40 ms behind the hand. Today's lag
  at 50 ms runs about 3 ms behind. A shorter native time (10 to 20 ms) may
  feel better; to try by ear.
- **Modulators:** with a 50 ms time a 10 Hz modulator keeps a third of its
  swing; at time 0 it passes untouched. So a control's time must be 0 while
  a modulator is mapped to it (the mapping sets it, unmapping puts it back).
- **Sends** use the volume's fader scale (the send knob reads the same spec).
  Each sender has its own level and lag control on the bus's input (`vol1`,
  `lag1`, ...), so a ramp per sender works where a crossfade could not.
- **Pan:** every panner's position controls have an even range (`pan`,
  `panx`, `pany`, `pan1`, `pan2`, `elevation`, ...). Today `panlag` and
  `trimlag` use `Ndef.lag`, which rebuilds that part of the strip; a ramp
  inside the strip removes that rebuild. The panners come from the library
  (`Files/SynthDefFiles/Space.scd`, about twenty), so Radicles adds the ramp
  to their position controls when it builds one. On the ring panners
  (`panAz`) -1 and 1 are the same place: a ramp from 0.9 to -0.9 goes the
  long way round, as a lag does today.
- **Telling typed from native:** each already sets the value in its own
  place (typed: `Assemblage:setVolume` and its siblings; the window: the
  fader's and knob's action; MIDI, HID, OSC: the function `HIDMap` posts), so
  each can send its own time with the value. The line posted when a control
  is mapped would change.

Not checked: a running server (everything above is offline), and how the
ramp behaves inside `tload`'s crossfade of the strip.

## Found when development started (20 Nov, evening)

Claude had said that `vol t 1 -12 4` exists today and that its time sticks to
the strip. That was wrong. Read in `RadiclesCmdsMix.sc`:

- **`vol` and `trim` take no time today,** in any of their forms. A typed time
  after the value is new; a trailing number is free in every form.
- **`pan` cannot take a trailing time:** `pan t 1 2 0.5` already means "pan
  control 2 to 0.5" (for panners with more than one control), so
  `pan t 1 0.5 4` is taken.
- **Only `sndset` has a time today** (`sndset t 1 2 -6 4`), and that one does
  stick: it sets the sender's lag on the bus.
- `vollag` exists and sets the strip's `lagTime`; `panlag` and `trimlag`
  rebuild part of the strip (`Ndef.lag`).

Decision 5 (a typed time belongs to one move) still stands as a decision, but
how the time is typed is open.

## How a time is typed (Federico, 20 Nov, evening)

A trailing number, and the command follows the control's name:

```
vol t 1 -12 4      trim t 1 -3 4      sndset t 1 2 -6 4
pan t 1 -0.5 4     panx t 1 -0.5 4    pany t 1 -0.5 4
```

Removed: `pan t 1 2 0.5` (pan control number 2) and today's `panx`
(`panx t 2 0.5`: pan control 2 on all tracks). No test uses either.

Every other panner control is a command of its own name too, with a time at
the end (Federico, 20 Nov): `pan1`, `pan2`, `width`, `orientation`,
`elevation` / `elev`, `azimuth` / `azi`, `rho`, `dirmul`. Built in the pan PR.

## Open

Nothing.

Decided since the first draft (20 Nov): the native time's command is
`ctllag`; every panner control ramps with `panlag`, the settings too (`width`,
`orientation`, `rho`, `dirmul`); the native time's default is 50 ms for now,
to be judged by ear.

## Planned PRs (Model: Fable for all; each gets a second look)

1. **Volume:** the ramp in the strip; `vol` without a time uses the strip's
   typed time (`vollag`, default 50 ms); the fader and mapped controls use
   `ctllag`; a modulator on the volume sets its time to 0; `glide` in `tload`
   for the volume. The strip's posted function changes, so about 150
   snapshots change by one line.
2. **Trim and sends:** the same, with `sndlag` (`sndlag t 1 2 4`: track,
   send slot, time; slot 1 when left out). `trimlag` no longer rebuilds the
   strip's input. A time typed with `sndset` is for that one move (decision
   5), so it no longer sticks to the send.
3. **Pan:** the ramp on every panner control, added when Radicles builds a
   panner from the library (the panner's own function is wrapped; its
   controls keep their names and each gets `xLag` and `xRamp`, as in
   `panLag`, `panRamp`). The commands follow the control's name, with a time
   for one move at the end: `pan`, `panx`, `pany`, `pan1`, `pan2`, `width`,
   `orientation`, `elevation`, `elev`, `azimuth`, `azi`, `rho`, `dirmul`.
   `panlag t 1 4` sets the typed time of all of a strip's pan controls,
   `panlag t 1 2 4` of its second one. Removed: `panxlag`, `pan t 1 2 0.5`
   and the old `panx`. Defaults taken in the build:
   - each name matches only the control of that name (`elev` is not
     `elevation`: the two panner families use different units);
   - a value outside the control's range is refused with a warning;
   - the `split` panner's controls are not ramped (their values are
     functions of the channel count);
   - the fixed 0.1 s lag inside the `bfenc` panners is gone, the ramp
     replaces it;
   - loading a mixer preset moves volume, trim and pan over the short time,
     not over the time of each strip's last move.
4. **`mixlag`, a time after `vol` and `trim`, and what a session keeps.**
   - `mixlag N` sets the typed time of every control of every strip and of
     every send, and of everything made later (a new mixer, an added track,
     a new send), as `fade N` does for fade times. `mixlag` alone posts it.
     `vollag`, `trimlag`, `panlag` and `sndlag` typed afterwards change one
     control again; a later `mixlag` overwrites them.
   - `vol t 1 -12 4`, `vol 1 -12 4`, `vol t 1-3 -12 4`, `vol [1,3] -12 4`,
     and the same for `trim`. The master has no number, so `vol m -12 4`,
     `trim m -3 2` and `pan m 0.5 3` are a value and its time. There is no
     timed form for "all tracks" (`vol t -12 4` already means track -12).
   - A session (`save`, `load`) keeps `mixlag` and `ctllag`; the times of
     single controls were already among the mixer's values. Sessions saved
     before this have neither, and loading one leaves both as they are.
   - A track preset keeps a control's typed time when it is not 50 ms, and
     brings back exactly that: loading a preset without one gives 50 ms,
     whatever `mixlag` is.

## Known consequences

- Taking over with a MIDI fader during a typed move jumps (smoothly, over the
  native time) to the fader's physical position.
- Many snapshots change: the mixer commands post different code.
- Sessions keep the typed times per control and the mixer's native time.
