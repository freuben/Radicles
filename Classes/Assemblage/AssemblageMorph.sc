// Assemblage: loading an effect or a chain preset as a transition (Document
// Model, step 1, PR 5; "Time in Radicles", 19 Nov 2026). fxload and csload
// bring an effect or a chain to a preset over a time, through the reconciler
// (RadUnit.apply). The time is the command's, else the fade time of the thing
// loaded onto (the effect's Ndef; for a chain, the strip's). The shape is the
// command's, else the preset's morphShape, else xfade:
//   \xfade (default): one xset with every changed argument, a single
//     crossfade over the Ndef's fade time. Only when another time was typed
//     is the fade time set to it first, and put back afterwards.
//   \glide: each changed argument gets the time as its lag, then one set;
//     the earlier lags go back afterwards.
//   \none (or a time of 0): the values are set at once.
// Modulators the preset adds, changes or removes fade with the rest (ModMap
// maps with xset over the fade time). The fade time and lags always go
// back, also when a load is interrupted by another or the effect is removed.
// Part of the Assemblage class: the class and its variables are in Assemblage.sc.

+ Assemblage {

	// stop a load running in a slot (its effect was removed or replaced by
	// another)
	prClearMorph {arg filterTag;
		this.prStopMorph(filterTag);
	}

	// stop a running load in a slot, putting the fade time and lags back
	prStopMorph {arg filterTag;
		var morph = morphs !? {|m| m[filterTag] };
		if(morph.notNil, {
			morph[\routine].stop;
			morph[\restore].value;
			morphs.removeAt(filterTag);
		});
	}

	// the part of a unit a load changes
	prMorphed {arg unit;
		^(args: unit[\args], mods: unit[\mods], data: unit[\data], buffer: unit[\buffer])
	}

	// bring the effect at ndefKey to unit (same effect) over time, with shape
	// \xfade, \glide or \none (fxload, and csload for each slot that keeps
	// its effect). fadeBack: the fade time the Ndef goes back to, when the
	// caller has already changed it (csload). quiet: don't say that a
	// bypassed effect is set at once (a load with no time or shape typed).
	morphUnit {arg ndefKey, unit, time, shape, action, fadeBack, quiet = false;
		if(this.isBypassed(ndefKey) and: { shape != \none }, {
			if(quiet.not, { "The effect is bypassed: its values are set at once".postln });
			shape = \none;
		});
		{
			var routine = thisThread, restore, setFunc, fade, lagKeys, oldLags;
			this.prStopMorph(ndefKey); //an interrupted load puts its times back first
			case
			{ shape == \xfade } {
				//csload passes the fade time the slot goes back to
				fade = fadeBack ?? { Ndef(ndefKey).fadeTime };
				//the Ndef's own fade time: nothing to set or to put back
				if(time != fade, {
					restore = { ("Ndef(" ++ ndefKey.cs ++ ").fadeTime = " ++ fade.cs ++ ";").radcode };
					("Ndef(" ++ ndefKey.cs ++ ").fadeTime = " ++ time.cs ++ ";").radcode;
				}, { restore = {} });
				setFunc = {|ndef, pairs|
					("Ndef(" ++ ndef.key.cs ++ ").xset(" ++ pairs.collect(_.cs).join(", ") ++ ");").radcode;
				};
			}
			{ shape == \glide } {
				restore = {
					if(lagKeys.notNil, {
						("Ndef(" ++ ndefKey.cs ++ ").lag(" ++
							[lagKeys, oldLags].flop.flatten.collect(_.cs).join(", ") ++ ");").radcode;
					});
				};
				setFunc = {|ndef, pairs|
					lagKeys = pairs[0, 2..];
					oldLags = ndef.nodeMap.ratesFor(lagKeys) ?? { nil ! lagKeys.size };
					("Ndef(" ++ ndef.key.cs ++ ").lag(" ++
						lagKeys.collect{|key| [key, time] }.flatten.collect(_.cs).join(", ") ++ ");").radcode;
					//the lag rebuilds the effect: let the old copy, which has no lag,
					//fade out before the values change
					server.sync;
					ndef.fadeTime.wait;
					("Ndef(" ++ ndef.key.cs ++ ").set(" ++ pairs.collect(_.cs).join(", ") ++ ");").radcode;
				};
			}
			{ restore = {} };
			morphs = morphs ?? { IdentityDictionary.new };
			morphs[ndefKey] = (routine: routine, restore: restore);
			if(RadUnit.apply(ndefKey, unit, this, setFunc), {
				server.sync;
				this.updateFxWin(ndefKey);
				if(shape != \none, { time.wait });
			});
			//done (or not applied): put the times back
			if(morphs !? {|m| m[ndefKey] } !? {|m| m[\routine] === routine } ? false, {
				restore.value;
				morphs.removeAt(ndefKey);
			});
			action.();
		}.fork(AppClock);
	}

	// ---- chain load (Document Model, step 3, PR 3) ----

	// the Ndef keys of a strip's stages: its input, its effects, the strip
	// and its panner (empty when there is no such strip)
	prStripStageKeys {arg type, num;
		^(this.getThisTrack(type, num) ? []).collect{|item| item[0] }
	}

	// bring a strip's chain to chain over time, slot by slot (agreed 2 Nov):
	// the same effect moves as fxload does (shape xfade or glide); another
	// effect crossfades from the old one to the new; a slot the preset leaves
	// empty fades out and is removed; a slot filled only in the preset fades
	// in; bypass follows. Slots that do not differ are not touched.
	// The time: the command's, else the strip's own fade time
	// (Ndef(\track1).fadeTime). Every stage of the strip crossfades over that
	// time while the load runs: a stage with another fade time is set to it,
	// and put back afterwards (posted; nothing is posted when they agree).
	morphChain {arg type, num, chain, time, shape, action, quiet = false;
		var strip = RadUnit.stripName(type, num), current, target = chain[\fx] ? (), slots, same, changed;
		current = RadUnit.readChain(type, num, this)[\fx];
		//which slots differ, and how
		slots = (current.keys ++ target.keys).asArray.asSet.asArray.sort;
		same = slots.select{|slot| current[slot].notNil and: { target[slot].notNil } and: {
			current[slot][\def] == target[slot][\def] } };
		changed = slots.select{|slot|
			var cur = current[slot], tgt = target[slot];
			cur.isNil or: { tgt.isNil } or: { cur[\def] != tgt[\def] } or: {
				(cur[\bypass] == true) != (tgt[\bypass] == true) } or: {
				RadDoc.diff(this.prMorphed(cur), this.prMorphed(tgt), 1e-9).notEmpty } };
		if(changed.isEmpty, { action.(); ^this });
		//the time: the command's, else the strip's own fade time
		time = (time ?? { Ndef(strip).fadeTime } ? 0).max(0);
		shape = (shape ?? { chain[\morphShape] } ? \xfade).asSymbol;
		if([\xfade, \glide, \none].includes(shape).not, {
			("csload: shape is xfade, glide or none, not " ++ shape).warn; ^this });
		if(time == 0, { shape = \none });
		{
			var routine = thisThread, restore, fades = [], cond = Condition(false), wait;
			wait = { cond.wait; cond.test = false };
			//a new load on the strip stops the one running, and those of its
			//slots, putting their times back first
			this.prStopMorph(strip);
			current.keys.do{|slot| this.prStopMorph(RadUnit.chainTag(type, num, slot)) };
			if(shape != \none, {
				this.prStripStageKeys(type, num).do{|key|
					var fade = Ndef(key).fadeTime;
					if(fade != time, {
						fades = fades.add([key, fade]);
						("Ndef(" ++ key.cs ++ ").fadeTime = " ++ time.cs ++ ";").radcode;
					});
				};
				restore = {
					//only the stages still there, and not one given another
					//fade time meanwhile
					var keys = this.prStripStageKeys(type, num);
					fades.do{|pair|
						if(keys.includes(pair[0]) and: { Ndef(pair[0]).fadeTime == time }, {
							("Ndef(" ++ pair[0].cs ++ ").fadeTime = " ++ pair[1].cs ++ ";").radcode;
						});
					};
				};
			}, { restore = {} });
			morphs = morphs ?? { IdentityDictionary.new };
			morphs[strip] = (routine: routine, restore: restore);
			// 1. slots the preset leaves empty fade out and go
			changed.reverseDo{|slot|
				if(target[slot].isNil, {
					this.removeFilter(type, num, slot, { cond.test = true; cond.signal });
					wait.();
				});
			};
			// 2. slots with another effect, or none yet: the preset's effect
			// crossfades in, with its values
			changed.do{|slot|
				var unit = target[slot], cur = current[slot], tag = RadUnit.chainTag(type, num, slot), pairs = [];
				if(unit.notNil and: { cur.isNil or: { cur[\def] != unit[\def] } }, {
					RadUnit.prArgOrder(unit[\def], unit[\args] ? ()).do{|key|
						var value = unit[\args][key];
						if(value.isKindOf(Dictionary).not, { pairs = pairs ++ [key, value] }) };
					this.filter(type, num, slot, unit[\def], pairs, unit[\buffer], RadUnit.dataArg(unit[\data]),
						{ cond.test = true; cond.signal }, this.findFilterTag(type, num, slot).notNil);
					wait.();
					RadUnit.apply(tag, unit, this); //its modulators
				});
			};
			// 3. slots that keep their effect move as fxload does; their fade
			// time is already the load's, and goes back with the strip's
			same.do{|slot|
				var unit = target[slot], tag = RadUnit.chainTag(type, num, slot);
				if(RadDoc.diff(this.prMorphed(current[slot]), this.prMorphed(unit), 1e-9).notEmpty, {
					this.morphUnit(tag, unit, time, shape, fadeBack: time, quiet: quiet);
				});
			};
			// 4. bypass follows the preset
			changed.do{|slot|
				var unit = target[slot], tag = RadUnit.chainTag(type, num, slot);
				if(unit.notNil and: { (unit[\bypass] == true) != this.isBypassed(tag) }, {
					this.bypassFx(type, num, slot, (unit[\bypass] == true).binaryValue);
				});
			};
			server.sync;
			changed.do{|slot| this.updateFxWin(RadUnit.chainTag(type, num, slot)) };
			this.refreshFunc;
			if(shape != \none, { time.wait });
			//done: the stages' fade times go back
			if(morphs !? {|m| m[strip] } !? {|m| m[\routine] === routine } ? false, {
				restore.value;
				morphs.removeAt(strip);
			});
			action.();
		}.fork(AppClock);
	}
}
