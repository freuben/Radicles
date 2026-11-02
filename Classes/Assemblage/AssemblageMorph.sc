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
		unit = RadUnit.readPreset(\filter, def, presetName);
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
		if(this.isBypassed(ndefKey) and: { shape != \none }, {
			"The effect is bypassed: its values are set at once".postln;
			shape = \none;
		});
		{
			var routine = thisThread, restore, setFunc, fade, lagKeys, oldLags;
			this.prStopMorph(ndefKey); //an interrupted morph puts its times back first
			case
			{ shape == \xfade } {
				fade = Ndef(ndefKey).fadeTime;
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
}
