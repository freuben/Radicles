# Lags on blocks and effects

Status: **decided 22 Nov 2026** in conversation with Federico ("Build the
lags"). It closes the last open part of the Time step (the mixer's part is
[mixer-ramps.md](mixer-ramps.md)). What each PR built is added under
"Planned PRs" as it lands.

## The idea

A parameter of an effect or a block can travel to a typed value over a time,
as the mixer's values do, but only when the performer has asked for it. It is
a nice feature, not a central one, so it must cost nothing when it is not
used and must not change how library files are written.

## Decided

1. **Built in, but off.** A parameter that can ramp is made with its ramp at
   a time of 0. At 0 the value passes through untouched, so nothing sounds
   different until a time is set. Setting a time never rebuilds the unit
   (today `fxlag` and `blklag` rebuild it: a sound file can restart, a reverb
   starts empty).
2. **Which parameters can ramp: those with a spec.** The library's spec (the
   part after `->`) is the declaration. Not ramped: a parameter with no
   spec, a spec with a step, a spec with a function, and any value that is
   not a number.
3. **A move follows the spec's scale and arrives on time** (the mixer's
   principle): an exponential spec moves evenly in pitch, a linear one evenly
   in value.
4. **One time per parameter.** "On" means the parameter's time is above 0;
   "off" and "excluded" mean 0. So on, off, excluded and included all live in
   the unit's own values, and presets and sessions keep them with no second
   list.
5. **Library files do not change.** Radicles adds the ramp around the unit's
   function when it builds it, as it does for the panners.
6. **Scopes, each a time for what is on within it:**
   - `fxlag N`: all effects. `blklag N`: all blocks. Default 0.5 s each.
   - `mixlag N`: the mixer only (as built).
   - `tlag`: one track: its effects, its block and its mixer values.
   - `lag N`: the whole session.
   - The mixer has no "off": `tlag` and `lag` set every mixer value in their
     scope, but only the effect and block parameters that are on.
   - A later, wider command overwrites the times it covers (the rule of
     `fade N`). An excluded parameter (time 0) is not on, so it stays
     excluded.
7. **Per effect** (blocks the same way with `blklag`):
   - `fxlag t 1 2 on`: on, with the effects' lag time; `fxlag t 1 2 off`;
     `blklag 1 on`, `blklag 1 off` (the words, because `blklag 1` and
     `blklag 4` would be the same form);
   - `fxlag t 1 2 4`: its parameters that are on take 4;
   - `fxlag t 1 2 freq 4`: one parameter;
   - `fxlag t 1 2 exc [freq,amp]` or `exc [1,2]`: those off;
   - `fxlag t 1 2 inc [pitchRatio,mix]` or `inc [3,4]`: only those on;
   - lists both ways: times by position, and pairs of name and time.
8. **`glide` in a load follows the same rule:** values that can ramp travel
   on their spec's scale over the load's time; the others jump **at the
   end**.
9. **The window's sliders, MIDI, HID and OSC use the native time**
   (`ctllag`), never the typed time.
10. **Look-ups move to `fxgetlag`** (it exists) and a `blkgetlag` by the same
    pattern. Today `fxlag t 1 2` posts the effect's parameter names and
    `fxlag t 1 2 4` posts the lag of its fourth parameter; both get the new
    meanings above.
11. **A time typed after a value is for that one move,** as on the mixer:
    `fxset t 1 2 freq 800 4`, and the same on `blkset`. It works on a
    parameter that is off too; the parameter's own time is unchanged.

## Checked (22 Nov)

- **Processor cost of ramps on everything** (offline renders, dev Mac, 120 s
  at 48 kHz, 30 units, three runs each): 180 ramped parameters add about
  0.13% of one core; moving on an exponential scale adds nothing measurable.
  Not measured: moving values, real-time running, the performance Mac, the
  time to build a larger unit.
- **The Main library as written** (about 650 specs): about 435 are a plain
  range and 70 have a scale: these ramp. 11 have a step and 131 have a
  function: these do not. Of the 131, 41 are only a small offset on an
  exponential range and could glide if a mark allowed it. RadiclesLibs was
  not surveyed.

## Planned PRs (Model: Fable for all; each gets a second look)

1. **Effects** (built): the ramp built in and off (`Assemblage.rampUnit`,
   added when an effect is made; times are the controls `xLag` and `xRamp`
   after the effect's own); `fxlag` on, off, per effect, per parameter,
   `exc`, `inc`; `fxlag N` (it came into this part because "on" needs it;
   a session keeps it from part 3); `fxset` with a time; `fxgetlag`; the
   window's sliders and mapped controls; modulators; presets keep which
   parameters are on. Defaults taken:
   - a list by position is `fxlags t 1 2 [4,1,0,2]`; `fxlag t 1 2 [...]`
     keeps its pairs (number or name, then time), as it read them before;
   - a preset saved with no lags leaves the effect's times alone;
   - sliders and mapped controls use `ctllag` only on a parameter that is on;
   - a dB spec moves by the fader's law, as the mixer's volume does.
2. **Blocks** (built): the same ramp when a block is made (`Block.play`);
   `blklag` on, off, per block, per parameter, `exc`, `inc`; `blklags` by
   position; `blklag N` with its own time (`Radicles.blkLag`, 0.5 s; a
   session keeps it from part 3); `blkset` with a time; `blkgetlag`; the
   source window; modulators by number.
3. **Scopes:** `fxlag N`, `blklag N`, `tlag`, `lag N`; what a session keeps.
4. **Loads:** `glide` follows the rule; values that cannot travel jump at
   the end.

## To check while building

- Whether the call window reads a list of bare names (`[freq,amp]`) as it is.
- How today's list form of `fxlag` (`fxlag t 1 2 [...]`) is laid out, so the
  new lists do not contradict it.
- `fxlagn`, `fxlags`, `lagfx`, `lagsfx`, `blklagn`: which stay, which become
  forms of the above.

## Known consequences

- Every effect and block with a rampable parameter is posted inside a short
  wrapper, so most snapshots change.
- Each rampable parameter gets time controls; presets and sessions carry
  them, and a session keeps the scope times as it keeps `mixlag`.
- A modulator mapped to a parameter that is on takes its ramp off while it
  is mapped (as in the mixer).
- Parameters whose spec has a function do not glide (`pch`'s `pitchRatio`,
  for one). Later, if missed: a mark in the spec that allows it, a mark for
  "never ramp", and specs for units that have none. None is needed to start.
- A unit that smooths a value inside its own function (`pch2`'s `lag`) keeps
  doing so.
