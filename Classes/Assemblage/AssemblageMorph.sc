// Assemblage: effect morph and morph time (Document Model, step 1, PR 5).
// fxmorph moves an effect to one of its presets over time, through the
// reconciler (RadUnit.apply):
//   \xfade (default): the Ndef's fade time is set to the morph time, then one
//     xset with every changed argument, a single crossfade; the fade time
//     goes back afterwards.
//   \glide: each changed argument gets the morph time as its lag, then one
//     set; the earlier lags go back afterwards.
//   \none: the values are set at once.
// Modulators the preset adds, changes or removes fade with the rest (ModMap
// maps with xset over the fade time). The fade time and lags always go
// back, also when a morph is interrupted by another or the effect is removed.
// Part of the Assemblage class: the class and its variables are in Assemblage.sc.

+ Assemblage {

	// the morph time a slot was given (morphtime t 1 2 5, or from a preset),
	// or nil for the global one (RadUnit.morphTime)
	morphTimeOf {arg filterTag; ^morphTimes !? {|m| m[filterTag] } }

	// give a slot its own morph time; nil goes back to the global one
	setMorphTime {arg filterTag, time;
		morphTimes = morphTimes ?? { IdentityDictionary.new };
		if(time.isNil, { morphTimes.removeAt(filterTag) }, { morphTimes[filterTag] = time });
		RadUnit.sync(filterTag, this); //document (step 1)
	}

	// forget a slot's morph settings and stop a morph in it (its effect was
	// removed or replaced by another)
	prClearMorph {arg filterTag;
		this.prStopMorph(filterTag);
		morphTimes !? {|m| m.removeAt(filterTag) };
	}

	// stop a running morph in a slot, putting the fade time and lags back
	prStopMorph {arg filterTag;
		var morph = morphs !? {|m| m[filterTag] };
		if(morph.notNil, {
			morph[\routine].stop;
			morph[\restore].value;
			morphs.removeAt(filterTag);
		});
	}

	// the part of a unit a morph changes
	prMorphed {arg unit;
		^(args: unit[\args], mods: unit[\mods], data: unit[\data], buffer: unit[\buffer])
	}

	// morph the effect at ndefKey to one of its presets (new format)
	morphUnitPreset {arg ndefKey, presetName, time, shape, action;
		var def, unit, current;
		def = (filters ? []).detect{|item| item[0] == ndefKey } !? {|item| item[1] };
		if(def.isNil, { "No effect in this slot".warn; ^this });
		if(RadUnit.presetExists(\filter, def, presetName).not, {
			("No preset " ++ presetName ++ " for " ++ def ++
				" (fxmorph uses presets saved with fxsave)").warn; ^this });
		unit = RadUnit.presetFor(ndefKey, def, presetName, this);
		if(unit.isNil, { ^this });
		//already there: nothing to morph (a morph time the preset has still
		//goes to the slot)
		current = RadUnit.read(ndefKey, this);
		if(RadDoc.diff(this.prMorphed(current), this.prMorphed(unit), 1e-9).isEmpty, {
			unit[\morphTime] !? {|t| if(t != current[\morphTime], { this.setMorphTime(ndefKey, t) }) };
			action.(); ^this
		});
		//the time: the command's, the preset's, the slot's, else the global one
		time = (time ?? { unit[\morphTime] } ?? { this.morphTimeOf(ndefKey) } ?? { RadUnit.morphTime }).max(0);
		shape = (shape ?? { unit[\morphShape] } ? \xfade).asSymbol;
		if([\xfade, \glide, \none].includes(shape).not, {
			("fxmorph: shape is xfade, glide or none, not " ++ shape).warn; ^this });
		this.morphUnit(ndefKey, unit, time, shape, action);
	}

	// morph the effect at ndefKey to unit (same effect) over time, with shape
	// \xfade, \glide or \none (fxmorph, and csmorph for each slot that keeps
	// its effect)
	morphUnit {arg ndefKey, unit, time, shape, action, fadeBack;
		if(this.isBypassed(ndefKey) and: { shape != \none }, {
			"The effect is bypassed: its values are set at once".postln;
			shape = \none;
		});
		{
			var routine = thisThread, restore, setFunc, fade, lagKeys, oldLags;
			this.prStopMorph(ndefKey); //an interrupted morph puts its times back first
			case
			{ shape == \xfade } {
				//csmorph passes the fade time from before it changed it
				fade = fadeBack ?? { Ndef(ndefKey).fadeTime };
				restore = { ("Ndef(" ++ ndefKey.cs ++ ").fadeTime = " ++ fade.cs ++ ";").radcode };
				("Ndef(" ++ ndefKey.cs ++ ").fadeTime = " ++ time.cs ++ ";").radcode;
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

	// ---- chain morph (Document Model, step 3, PR 3) ----

	// a strip's own chain morph time (csmorphtime t 1 5), or nil
	chainMorphTimeOf {arg strip; ^chainMorphTimes !? {|m| m[strip.asSymbol] } }

	setChainMorphTime {arg strip, time;
		chainMorphTimes = chainMorphTimes ?? { IdentityDictionary.new };
		if(time.isNil, { chainMorphTimes.removeAt(strip.asSymbol) }, { chainMorphTimes[strip.asSymbol] = time });
	}

	// morph a strip's chain to chain over time, slot by slot (agreed 2 Nov):
	// the same effect morphs as fxmorph does (shape xfade or glide); another
	// effect crossfades from the old one to the new; a slot the preset leaves
	// empty fades out and is removed; a slot filled only in the preset fades
	// in; bypass follows, fading. Slots that do not differ are not touched.
	// For the crossfades, the mixer's fade time is the morph time while the
	// morph runs (Radicles.fadeTime), then goes back.
	morphChain {arg type, num, chain, time, shape, action;
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
		if(changed.isEmpty, {
			chain[\morphTime] !? {|t| this.setChainMorphTime(strip, t) };
			action.(); ^this
		});
		//the time: the command's, the preset's, the strip's, else the global one
		time = (time ?? { chain[\morphTime] } ?? { this.chainMorphTimeOf(strip) } ?? { RadUnit.morphTime }).max(0);
		shape = (shape ? \xfade).asSymbol;
		if([\xfade, \glide, \none].includes(shape).not, {
			("csmorph: shape is xfade, glide or none, not " ++ shape).warn; ^this });
		{
			var routine = thisThread, restore, oldFade, slotFades = (), cond = Condition(false), wait;
			wait = { cond.wait; cond.test = false };
			//a new morph on the strip stops the one running, and those of its
			//slots, putting their times back first
			this.prStopMorph(strip);
			current.keys.do{|slot| this.prStopMorph(RadUnit.chainTag(type, num, slot)) };
			oldFade = Radicles.fadeTime;
			same.do{|slot| slotFades[slot] = Ndef(RadUnit.chainTag(type, num, slot)).fadeTime };
			if(shape != \none, {
				Radicles.fadeTime = time;
				this.globFadeTime;
				restore = { Radicles.fadeTime = oldFade; this.globFadeTime };
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
			// 3. slots that keep their effect morph as fxmorph does
			same.do{|slot|
				var unit = target[slot], tag = RadUnit.chainTag(type, num, slot);
				if(RadDoc.diff(this.prMorphed(current[slot]), this.prMorphed(unit), 1e-9).notEmpty, {
					this.morphUnit(tag, unit, time, shape, fadeBack: slotFades[slot]);
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
			chain[\morphTime] !? {|t| this.setChainMorphTime(strip, t) };
			if(shape != \none, { time.wait });
			//done: the mixer's fade time goes back
			if(morphs !? {|m| m[strip] } !? {|m| m[\routine] === routine } ? false, {
				restore.value;
				morphs.removeAt(strip);
			});
			action.();
		}.fork(AppClock);
	}
}
