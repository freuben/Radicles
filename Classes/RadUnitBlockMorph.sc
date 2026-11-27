// Radicles: a block preset loaded as a transition (Document Model, step 2,
// PR 4; "Time in Radicles", 19 Nov 2026). blkload moves a block to one of
// its presets over time, as fxload does for effects:
//   blkload 1 warmLoop             a crossfade over block 1's own fade time
//   blkload 1 warmLoop 4           over 4 seconds
//   blkload 1 warmLoop 4 glide     glide the values
//   blkload 1 warmLoop none        at once (as a time of 0)
//   blkload 1 default 4            to the definition's default values
// Time: the command's, else the block's own fade time (fade blk 1 4).
// Shape: the command's, else the preset's morphShape, else xfade (decided
// 3 Nov 2026: crossfade is the default for every block):
//   \xfade: one xset, a single crossfade between the block as it was and as
//     the preset has it, over the Ndef's fade time. Only when another time
//     was typed is the fade time set to it first, and put back afterwards.
//   \glide: each changed value that can travel (it has a ramp: a plain
//     spec) moves over the time on its spec's scale, with nothing rebuilt;
//     the others jump at the end.
//   \none: the values are set at once.
// A sound-file block that reports its position (RadPosition) keeps its
// place: before anything makes a new copy of the block (the xset, a
// modulator), startPos is set to where the file is, so the new copy
// carries on from there; once the load is over startPos goes back
// (PlayBuf reads it only when it starts or is triggered, so that is silent).
// A preset with another sound file or table plays the block again with it
// (RadUnit.loadBlockPreset): a crossfade whatever the shape, over the time
// typed or the block's fade time, and the file starts where the preset says.

+ RadUnit {

	// where block N's file will be when a message sent now reaches the
	// server, as a startPos (0-1), or nil if the block reports no position.
	// Reads the position twice to know how fast the file moves (rate and its
	// modulators included). Call inside a Routine.
	*prPlaceNow {|block|
		var read, a, b, dur, step, speed, pos, buf;
		if(Block.reportsPosition(block).not, { ^nil });
		buf = Block.bufferOf(block);
		dur = buf !? { buf.duration };
		if(dur.isNil or: { dur <= 0 }, { ^nil });
		read = {
			var cond = Condition(false), value;
			Block.position(block, {|v| value = v; cond.test = true; cond.signal });
			cond.wait;
			[value, Main.elapsedTime]
		};
		a = read.(); 0.05.wait; b = read.();
		if(a[0].isNil or: { b[0].isNil }, { ^nil });
		//a loop that wrapped between the two reads
		step = b[0] - a[0];
		if(step < (dur.neg / 2), { step = step + dur });
		if(step > (dur / 2), { step = step - dur });
		speed = step / (b[1] - a[1]).max(1e-3);
		pos = b[0] + (speed * (Main.elapsedTime - b[1] + (Server.default.latency ? 0) + placeLead));
		^(pos.wrap(0, dur) / dur)
	}

	// set block N's startPos to where its file is now (posted, a plain set:
	// the running copy doesn't hear it, the next copy starts there); returns
	// whether it did. Call inside a Routine.
	*prKeepPlace {|block|
		var tag = this.blockTag(block), place = this.prPlaceNow(block);
		if(place.isNil, { ^false });
		("Ndef(" ++ tag.cs ++ ").set('startPos', " ++ place.round(1e-6).cs ++ ");").radcode;
		^true
	}

	// stop a load running on a block, putting its fade time back
	// (not when the block is being removed)
	*prStopBlockMorph {|tag, restore = true|
		var morph = blockMorphs !? {|m| m[tag] };
		if(morph.notNil, {
			morph[\routine].stop;
			if(restore, { morph[\restore].value });
			blockMorphs.removeAt(tag);
		});
	}

	// bring block N to unit (same definition, file and table) over time with
	// shape: blkload, and tload for a track's source (step 4)
	*morphBlockUnit {|block, unit, time, shape, action|
		var tag = this.blockTag(block), current = this.readBlock(tag), keepPlace, startPos;
		if(current.isNil, { ("No block " ++ block ++ " playing").warn; ^false });
		//a sound file keeps its place: startPos is the place while the load
		//makes new copies, and the preset's afterwards
		keepPlace = shape != \none and: { Block.reportsPosition(block) }
		and: { (current[\args] ? ()).includesKey(\startPos) };
		startPos = (unit[\args] ? ()).at(\startPos) ?? { (current[\args] ? ()).at(\startPos) };
		if(keepPlace, { unit = unit.deepCopy; unit[\args].removeAt(\startPos) });
		{
			var routine = thisThread, restore, setFunc, fade, later, before, ndef = Ndef(tag), putPlace;
			this.prStopBlockMorph(tag); //an interrupted load puts its times back first
			putPlace = {
				if(keepPlace, {
					("Ndef(" ++ tag.cs ++ ").set('startPos', " ++ startPos.cs ++ ");").radcode });
			};
			if(keepPlace, {
				keepPlace = this.prKeepPlace(block);
				//no position after all: the preset's startPos goes with the rest
				if(keepPlace.not and: { startPos.notNil }, { unit[\args][\startPos] = startPos });
			});
			case
			{ shape == \xfade } {
				//the fade time is the load's while it runs (nothing is set or
				//put back when it is the block's own)
				Radicles.holdFade(tag, time);
				restore = { Radicles.releaseFade(tag); putPlace.() };
				setFunc = {|ndef, pairs|
					("Ndef(" ++ ndef.key.cs ++ ").xset(" ++ pairs.collect(_.cs).join(", ") ++ ");").radcode;
				};
			}
			{ shape == \glide } {
				//nothing is rebuilt and no time is changed: only the place goes back
				restore = { putPlace.() };
				setFunc = {|ndef, pairs|
					var split = Assemblage.glideSplit(ndef.key, pairs);
					later = split[1];
					if(split[0].notEmpty, { Assemblage.unitSetCode(ndef.key, split[0], time).radcode });
					before = Assemblage.glideStart(ndef.key, later);
				};
			}
			{ restore = { putPlace.() } };
			blockMorphs = blockMorphs ?? { IdentityDictionary.new };
			blockMorphs[tag] = (routine: routine, restore: restore);
			if(this.applyBlock(block, unit, setFunc), {
				Server.default.sync;
				Radicles.aZ !? {|az| az.updateSourceWin(tag) };
				if(shape != \none, { time.wait });
			});
			//done (or not applied): put the times back
			if(blockMorphs !? {|m| m[tag] } !? {|m| m[\routine] === routine } ? false, {
				//a glide's values that cannot travel jump now, at the end
				//(not one set or mapped since the load began: glideLater)
				if(later.notNil and: { later.notEmpty }, {
					later = Assemblage.glideLater(tag, later, before);
					if(later.notEmpty, {
						Assemblage.unitSetCode(tag, later, 0).radcode;
						Radicles.aZ !? {|az| az.updateSourceWin(tag) } }) });
				restore.value;
				blockMorphs.removeAt(tag);
			});
			action.();
		}.fork(AppClock);
		^true
	}
}
