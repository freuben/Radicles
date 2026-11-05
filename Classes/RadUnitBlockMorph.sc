// Radicles: block morph (Document Model, step 2, PR 4). blkmorph moves a
// block to one of its presets over time, as fxmorph does for effects:
//   blkmorph 1 warmLoop             over the global morph time (morphtime 5)
//   blkmorph 1 warmLoop 4           over 4 seconds
//   blkmorph 1 warmLoop 4 glide     glide the values (or none: at once)
//   blkmorph 1 default 4            to the definition's default values
// Shapes (decided 3 Nov 2026: crossfade is the default for every block):
//   \xfade: the Ndef's fade time becomes the morph time, then one xset, a
//     single crossfade between the block as it was and as the preset has it.
//   \glide: each changed value gets the morph time as its lag, then one set.
//   \none: the values are set at once.
// A sound-file block that reports its position (RadPosition) keeps its
// place: before anything makes a new copy of the block (the xset, a lag,
// a modulator), startPos is set to where the file is, so the new copy
// carries on from there; once the morph is over startPos goes back
// (PlayBuf reads it only when it starts or is triggered, so that is silent).
// Refused, with the reason: a preset with another sound file or table
// (crossfading two files is step 3's strip morph), recording blocks and
// patterns (no presets), a block playing another definition.

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

	*prStopBlockMorph {|tag|
		var morph = blockMorphs !? {|m| m[tag] };
		if(morph.notNil, {
			morph[\routine].stop;
			morph[\restore].value;
			blockMorphs.removeAt(tag);
		});
	}

	// blkmorph N name [time [shape]]: returns true when the morph starts
	*morphBlockPreset {|block, name, time, shape, action|
		var tag = this.blockTag(block), current, unit, keepPlace, startPos;
		current = this.prPresetBlock(block, "morph");
		if(current.isNil, { ^false });
		unit = this.blockPresetFor(block, current[\def], name);
		if(unit.isNil, { ^false });
		if(RadDoc.diff((data: current[\data], buffer: current[\buffer]),
			(data: unit[\data], buffer: unit[\buffer])).notEmpty, {
			("blkmorph: " ++ name ++ " has another sound file or table than block " ++ block ++
				"; morphing between two files comes later (blkload " ++ block ++ " " ++ name ++
				" loads it, with a crossfade)").warn;
			^false
		});
		//already there
		if(RadDoc.diff((args: current[\args], mods: current[\mods]), (args: unit[\args], mods: unit[\mods]), 1e-9).isEmpty, {
			action.(); ^true
		});
		time = (time ?? { unit[\morphTime] } ?? { morphTime }).max(0);
		shape = (shape ?? { unit[\morphShape] } ? \xfade).asSymbol;
		if([\xfade, \glide, \none].includes(shape).not, {
			("blkmorph: shape is xfade, glide or none, not " ++ shape).warn; ^false });
		//a sound file keeps its place: startPos is the place while the morph
		//makes new copies, and the preset's afterwards
		keepPlace = shape != \none and: { Block.reportsPosition(block) }
		and: { (current[\args] ? ()).includesKey(\startPos) };
		startPos = (unit[\args] ? ()).at(\startPos) ?? { (current[\args] ? ()).at(\startPos) };
		if(keepPlace, { unit = unit.deepCopy; unit[\args].removeAt(\startPos) });
		{
			var routine = thisThread, restore, setFunc, fade, lagKeys, oldLags, ndef = Ndef(tag), putPlace;
			this.prStopBlockMorph(tag); //an interrupted morph puts its times back first
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
				fade = ndef.fadeTime;
				restore = {
					("Ndef(" ++ tag.cs ++ ").fadeTime = " ++ fade.cs ++ ";").radcode;
					putPlace.();
				};
				("Ndef(" ++ tag.cs ++ ").fadeTime = " ++ time.cs ++ ";").radcode;
				setFunc = {|ndef, pairs|
					("Ndef(" ++ ndef.key.cs ++ ").xset(" ++ pairs.collect(_.cs).join(", ") ++ ");").radcode;
				};
			}
			{ shape == \glide } {
				restore = {
					if(lagKeys.notNil, {
						//putting the lags back rebuilds the block too: keep the place
						if(keepPlace, { this.prKeepPlace(block) });
						("Ndef(" ++ tag.cs ++ ").lag(" ++
							[lagKeys, oldLags].flop.flatten.collect(_.cs).join(", ") ++ ");").radcode;
						if(keepPlace, { Server.default.sync; ndef.fadeTime.wait });
					});
					putPlace.();
				};
				setFunc = {|ndef, pairs|
					lagKeys = pairs[0, 2..];
					oldLags = ndef.nodeMap.ratesFor(lagKeys) ?? { nil ! lagKeys.size };
					("Ndef(" ++ ndef.key.cs ++ ").lag(" ++
						lagKeys.collect{|key| [key, time] }.flatten.collect(_.cs).join(", ") ++ ");").radcode;
					//the lag rebuilds the block: let the old copy, which has no lag,
					//fade out before the values change
					Server.default.sync;
					ndef.fadeTime.wait;
					("Ndef(" ++ ndef.key.cs ++ ").set(" ++ pairs.collect(_.cs).join(", ") ++ ");").radcode;
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
				restore.value;
				blockMorphs.removeAt(tag);
			});
			action.();
		}.fork(AppClock);
		^true
	}
}
