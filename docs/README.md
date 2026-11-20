# Radicles design notes

The decided design of Radicles, kept next to the code so that anyone working
on it (Federico, Claude in a chat, Claude Code on a Mac) reads the same thing.

## How notes get here

1. A new design note is written and discussed in the claude.ai Project
   "Radicles development". That copy is the working draft: comments,
   questions and changes happen there.
2. Once Federico has answered its questions, the decided version is copied
   here as Markdown, in a PR into `dev`, usually together with the code it
   describes.
3. If a decision changes later, the claude.ai copy is updated first, then
   the file here in the next PR.

So: **the files here are what has been decided; the claude.ai docs may hold
newer drafts.** When the two disagree, ask before building on either.

## What is here

| File | What it is |
| --- | --- |
| [status.md](status.md) | Where the work stands: steps done, what is next, open points |
| [decisions.md](decisions.md) | One line per decision, newest first, with where it is written up |
| [design-principles.md](design-principles.md) | The 14 principles every change must keep, the command conventions, and the development checklist |
| [terms-of-reference.md](terms-of-reference.md) | Who decides what, and how Federico and Claude work together |
| [implementation-plan.md](implementation-plan.md) | The Document Model plan, steps 1–8, with risks and "done when" for each |
| [design/step1-effects-as-units.md](design/step1-effects-as-units.md) | Step 1: the document, units, effect presets, `fxmorph`, bypass |
| [design/step2-blocks-as-units.md](design/step2-blocks-as-units.md) | Step 2: blocks as units, block presets, `blkmorph`, position |
| [design/step3-effect-chains.md](design/step3-effect-chains.md) | Step 3: strip values in the document, `cssave` / `csload` / `csmorph` |
| [design/step4-track-presets.md](design/step4-track-presets.md) | Step 4: `tsave` / `tload` / `tmorph` |
| [design/time-fade-crossfade-glide.md](design/time-fade-crossfade-glide.md) | Time: `fade`, loading a preset as a crossfade (it replaces the `morph` commands of steps 1–4); lag is on hold |

## Notes on the copies

- Links inside the notes point to the claude.ai versions (`claude.ai/artifact/...`);
  the table above says which file each one is.
- Diagrams don't export from claude.ai, so they are redrawn here in Mermaid
  (GitHub draws them): two in the principles, one in step 3. When a diagram
  changes in claude.ai, its Mermaid copy is updated with the note.
- The first line under each title (date and `@Federico`) is the note's date
  and owner.
