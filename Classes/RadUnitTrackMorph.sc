// Radicles: a track preset loaded as a transition (Document Model, step 4,
// PR 3; "Time in Radicles", 19 Nov 2026). tload moves every part of a strip
// to a track preset over one time, so everything arrives together (decided
// 16 Nov 2026); each part moves as its own command does:
//   chain: as csload (slot rules of step 3, shape xfade or glide)
//   source, same block definition and file: as blkload
//   source, another definition or file: crossfade (the block's fade time is
//     the load's time while it plays the new one)
//   trim, volume, pan: crossfade (one xset each, the stage's fade time being
//     the load's time while it runs), so they move on the same curve as the
//     effects and the source. A lag moves fast and then creeps: the level
//     arrived long before the rest (Federico, 19 Nov 2026). With glide they
//     glide as before (a lag over the time, then the strip's own lag back).
//   a track that ends with no source (tload t 1 default) fades out whole:
//     its volume crossfades to silence over the time, then it is emptied and
//     set at once, unheard. A track with no source that gets one is built at
//     once in silence and its volume fades up (19 Nov 2026). Not with nosrc.
//   sends: glide; a send only in the preset is made at -inf and glides up,
//     a send only on the strip glides to -inf and is removed at the end
//   mixer and send modulators: at the start (they fade with the Ndef's own
//     fade time, as effect modulators do)
//   mute: off at the start, on at the end
//   output to the master (as saved; 17 Nov 2026): on at the start, off at
//     the end, so the strip is never silent partway through
// The time: the command's, else the strip's own fade time
// (Ndef(\track3).fadeTime; fade t 3 4). The shape: the command's, else the
// preset's morphShape, else xfade. Shape none, or a time of 0, loads at once
// (RadUnit.applyTrack). A new tload, csload or blkload on the strip stops a
// running one where it is, putting the lags and fade times back.
//   tload t 3 vox [time] [xfade|glide|none] [nosrc] [nofx]

+ RadUnit {

	*prTrackMorphs { ^trackMorphs ?? { trackMorphs = IdentityDictionary.new } }

	// stop a running track load on a strip, putting its times back
	*prStopTrackMorph {|name|
		var morph = this.prTrackMorphs[name.asSymbol];
		if(morph.notNil, {
			morph[\routine].stop;
			morph[\subs].do{|sub| sub.stop };
			morph[\restore].value;
			trackMorphs.removeAt(name.asSymbol);
		});
	}

	// the volume set by the performer on a strip while its whole-track
	// fade-out is on its way: they have changed their mind. The load stops
	// and the track stays as it is, its source and effects on (Federico, 20
	// Nov 2026). Before, the level came back and the track was emptied anyway
	// when the time was up, while it sounded. Called before the value is
	// set, by every way a performer sets a volume: Assemblage:setVolume (the
	// vol commands), the mixer's fader, and a MIDI, HID or OSC control mapped
	// to the volume (HIDMap). Not during a fade-in or a part-by-part load:
	// there the new volume is one more value. A modulator on the volume,
	// removed when the fade-out began, does not come back.
	*volumeSetDuringLoad {|type, num| this.volumeSetOnStrip(this.stripName(type, num)) }

	// the same by the strip's name, which is also its Ndef's key (any other
	// name has no load running, so nothing happens)
	*volumeSetOnStrip {|name|
		var morph;
		name = name.asSymbol;
		morph = this.prTrackMorphs[name];
		if(morph.notNil and: { morph[\silentAt].notNil }, {
			this.prStopTrackMorph(name);
			("tload stopped: " ++ name ++ " stays as it is").postln;
		});
	}

	// bring a strip to track over time (tload). Returns true when it starts.
	*morphTrack {|type, num, track, time, shape, nosrc = false, nofx = false, action, az|
		var name = this.stripName(type, num), kind = this.stripKind(name);
		az = az ? Radicles.aZ;
		//the time: the command's, else the strip's own fade time (a morphTime
		//in a preset saved before 19 Nov 2026 is ignored)
		time = (time ?? { Radicles.ownFade(name) } ? 0).max(0);
		shape = (shape ?? { track[\morphShape] } ? \xfade).asSymbol;
		if([\xfade, \glide, \none].includes(shape).not, {
			("tload: shape is xfade, glide or none, not " ++ shape).warn; ^false });
		if(shape == \none or: { time == 0 }, {
			{
				this.prStopTrackMorph(name);
				this.applyTrack(type, num, track, nosrc, nofx, az);
				action.();
			}.fork(AppClock);
			^true
		});
		//a track that ends, or starts, with no source fades as a whole
		if(kind == \track and: { nosrc.not } and: { track[\source].notNil }, {
			var whole = this.prWholeFade(num, track, az);
			if(whole.notNil, {
				{ this.prFadeWholeTrack(whole, type, num, track, time, nofx, action, az) }.fork(AppClock);
				^true
			});
		});
		{
			var routine = thisThread, restores = List.new, subs = List.new, atEnd = List.new, current, left = [], morph;
			this.prStopTrackMorph(name);
			this.prStopStripMorphs(type, num, az);
			morph = (routine: routine, subs: subs, restore: { restores.do(_.value) });
			this.prTrackMorphs[name] = morph;
			current = this.readStrip(name, az);
			// mute: off at the start
			if(track[\mute] != 1 and: { current[\mute] == 1 }, { az.setMute(type, num, 0) });
			// output to the master: on at the start
			if(track[\output] == \master, { this.prSetOutput(type, num, \master, az) });
			// the chain moves on its own, over the same time. Its structure
			// first: a stage rebuilt for an added or removed effect is set to
			// the values it had when the rebuild was asked for, so the strip's
			// values move only once the chain is built. A chain load stopped
			// by another also calls back. A flag with a limit, not a
			// Condition: should a build die on an error, the values still
			// move (an effect with buffers that is replaced is built once
			// the old one has faded, so the limit is past the load's time).
			if(nofx.not, {
				var chainBuilt = false, waited = 0;
				az.morphChain(type, num, track[\chain] ? (fx: ()), time, shape, quiet: true, built: { chainBuilt = true });
				while({ chainBuilt.not and: { waited < (time + 10) } }, { 0.05.wait; waited = waited + 0.05 });
				if(chainBuilt.not, { ("tload " ++ name ++ ": the effects were not ready in time").warn });
			});
			// the source
			if(nosrc.not and: { kind == \track } and: { track[\source].notNil }, {
				left = left ++ this.prMorphSource(num, track[\source], time, shape, restores, az);
			});
			// trim, volume, pan
			left = left ++ this.prGlideStripValues(type, num, track, time, restores, subs, az, shape);
			// sends
			if(kind != \master, {
				left = left ++ this.prGlideSends(type, num, track[\sends] ? (), time, restores, atEnd, az);
			});
			Server.default.sync;
			{ az.mixWinBool({ az.refreshMixGUI }); az.refreshFunc }.defer;
			if(left.notEmpty, { ("tload " ++ name ++ ": left out " ++ left.join("; ")).warn });
			time.wait;
			// removed sends go; mute: on at the end; output: off at the end
			atEnd.do(_.value);
			if(track[\mute] == 1 and: { current[\mute] != 1 }, { az.setMute(type, num, 1) });
			if(track[\output] == \none, { this.prSetOutput(type, num, \none, az) });
			//done: the lags and fade times go back (once the panner's glide,
			//which starts a fade time later, is over)
			subs.do{|sub| while({ sub.isPlaying }, { 0.05.wait }) };
			if(this.prTrackMorphs[name] === morph, {
				restores.do(_.value);
				trackMorphs.removeAt(name);
			});
			Server.default.sync;
			{ az.mixWinBool({ az.refreshMixGUI }); az.refreshFunc }.defer;
			action.();
		}.fork(AppClock);
		^true
	}

	// ---- a whole track fading out or in ----

	// \out when the track sounds and the preset has no source (every block
	// feeding it would stop), \in when nothing sounds on the track and the
	// preset has a source, else nil
	*prWholeFade {|num, track, az|
		var name = this.stripName(\track, num), input = this.readInput(name, az) ? [], playing;
		if(input.any{|key| this.isBlockTag(key).not }, { ^nil }); //fed by something else
		playing = input.select{|key| Ndef(key).source.notNil };
		if(track[\source].isEmpty, {
			//only when every block that plays will really be stopped (a
			//pattern, or a block that also feeds another track, plays on:
			//the track must not come back with it at full level)
			^if(playing.notEmpty and: { playing.every{|key| this.readBlock(key).notNil and: {
				this.prBlockShared(key.asString.divNumStr[1].asInteger, num, az).not } } }, { \out }, { nil })
		});
		^if(playing.isEmpty, { \in }, { nil })
	}

	// the track's volume crossfades to silence over time, then the preset is
	// applied at once, unheard (\out); or the preset is applied at once with
	// the volume at silence, then the volume crossfades up over time (\in)
	*prFadeWholeTrack {|way, type, num, track, time, nofx, action, az|
		var name = this.stripName(type, num), key = this.stripKeys(name)[1], latency = (Server.default.latency ? 0.2) + 0.1;
		var routine = thisThread, restores = List.new, morph, current, silent, volume = track[\volume] ? 0;
		var volMod = if(track[\default] == true, { nil }, { (track[\mods] ? ())[\volume] });
		var before = this.prTrackMorphs[name], silentAt, going, tail;
		//applyTrack stops the loads running on the strip, but not this one
		//(it stays on the list, so another tload or t rm still stops it)
		var apply = { this.applyTrack(type, num, silent, false, nofx, az, false) };
		var fade = {|value|
			Radicles.holdFade(key, time);
			restores.add({ Radicles.releaseFade(key) });
			("Ndef(" ++ key.cs ++ ").xset('volume', " ++ value.cs ++ ");").radcode;
			{ az.mixWinBool({ az.refreshMixGUI }) }.defer;
			if(value == -inf, { morph[\silentAt] = Main.elapsedTime + time + latency });
			(time + latency).wait;
			morph.removeAt(\silentAt);
			restores.do(_.value); restores.clear;
		};
		//a fade-out already on its way on this strip (the same line typed
		//again, or another preset with no source): its volume reads -inf
		//from the start, so the time it has left is taken from the load
		silentAt = before !? {|m| m[\silentAt] };
		this.prStopTrackMorph(name);
		this.prStopStripMorphs(type, num, az);
		morph = (routine: routine, subs: [], restore: { restores.do(_.value) });
		this.prTrackMorphs[name] = morph;
		current = this.readStrip(name, az);
		//a modulator on the volume goes first: the fade sets the volume itself
		(current[\mods] ? ())[\volume] !? { ModMap.unmap(Ndef(key), \volume, current[\volume]) };
		//the preset with a silent volume; its own volume (and a modulator on
		//it) comes at the end
		silent = track.copy;
		silent[\volume] = -inf;
		silent[\mods] = (track[\mods] ? ()).copy;
		silent[\mods].removeAt(\volume);
		if(way == \out, {
			//what will go, for the longest of their fade times: the blocks
			//(Block.stop fades over the block's own) and the effects
			going = (this.readInput(name, az) ? []).select{|k| this.isBlockTag(k) };
			if(nofx.not, { going = going ++ (az.filters ? []).collect(_[0]).select{|tag| az.filterTagOnStrip(tag, type, num) } });
			tail = (going.collect{|k| Radicles.ownFade(k) } ++ [Radicles.ownFade(name)]).maxItem;
			case
			//nothing to fade when the track is muted
			{ current[\mute] == 1 } { }
			{ silentAt.notNil } {
				morph[\silentAt] = silentAt;
				(silentAt - Main.elapsedTime).max(0).wait;
				morph.removeAt(\silentAt);
			}
			//or already silent
			{ current[\volume] != -inf } { fade.(-inf) };
			apply.();
			//what was removed has gone before the volume comes back
			(tail + latency).wait;
			if(this.readStrip(name, az).notNil, { this.prApplyStripValues(type, num, track, az) });
		}, {
			//silent first, over a tenth of a second: a block that was just
			//stopped, or an effect's tail, may still sound, and a cut clicks
			if(current[\volume] != -inf, { az.setVolume(type, num, -inf, 0.1); 0.15.wait });
			apply.();
			Server.default.sync;
			latency.wait;
			if(volume != -inf, { fade.(volume) });
			if(volMod.notNil, { this.prApplyStripValues(type, num, track, az) });
		});
		if(this.prTrackMorphs[name] === morph, { trackMorphs.removeAt(name) });
		Server.default.sync;
		{ az.mixWinBool({ az.refreshMixGUI }); az.refreshFunc }.defer;
		action.();
	}

	// ---- the source ----

	*prMorphSource {|num, source, time, shape, restores, az|
		var name = this.stripName(\track, num), found, blocks, left;
		if(source.isEmpty, {
			//saved with no source: the blocks feeding the track fade out
			(this.readInput(name, az) ? []).do{|key|
				var block;
				if(this.isBlockTag(key), {
					block = key.asString.divNumStr[1].asInteger;
					if(this.prBlockShared(block, num, az).not and: { this.readBlock(key).notNil }, {
						//over the load's time (not given when that is the block's
						//own fade time: Block.stop then has nothing to set)
						Block.stop(block, if(Radicles.ownFade(key) != time, { time }, { nil }));
					});
				});
			};
			^[]
		});
		found = this.prSourceBlocks(num, az);
		blocks = found[0]; left = found[1];
		source.do{|unit, index|
			var block = blocks[index], tag, current, fade;
			case
			{ block.isNil } { if(blocks.notEmpty, { left = left.add("source " ++ (index + 1) ++ " (" ++ unit[\def] ++
				"): " ++ name ++ " has " ++ blocks.size ++ " block(s)") }) }
			{ this.prBlockShared(block, num, az) } {
				left = left.add("block " ++ block ++ " also feeds another track: source " ++ unit[\def] ++ " not loaded") }
			{
				tag = this.blockTag(block);
				current = this.readBlock(tag);
				if(current.notNil and: { current[\def] == unit[\def] }, {
					unit = unit.copy;
					[\buffer, \data].do{|key| if(unit[key].isNil and: { current[key].notNil }, { unit[key] = current[key] }) };
				});
				if(current.notNil and: { current[\def] == unit[\def] } and: {
					RadDoc.diff((data: current[\data], buffer: current[\buffer]),
						(data: unit[\data], buffer: unit[\buffer])).isEmpty }, {
					//the same block and file: as blkload
					this.morphBlockUnit(block, unit, time, shape);
				}, {
					//another block or file: a crossfade over the load's time
					//(nothing to set when that is the block's own fade time)
					this.prStopBlockMorph(tag);
					Radicles.holdFade(tag, time);
					restores.add({ Radicles.releaseFade(tag) });
					if(this.applyBlock(block, unit, anyDef: true), {
						Server.default.sync;
						{ az.updateSourceWin(tag) }.defer;
					}, {
						left = left.add("block " ++ block ++ ": source " ++ unit[\def] ++ " not loaded");
					});
				});
			};
		};
		^left
	}

	// ---- trim, volume, pan ----

	*prGlideStripValues {|type, num, track, time, restores, subs, az, shape = \glide|
		var name = this.stripName(type, num), inKey, key, spaceKey, current, curMods, tgtMods, left = [];
		var pan, panKeys, panDefaults, curLags, tgtLags, trimLag, xfade = shape != \glide;
		//a crossfade: the stage's fade time is the load's while it runs
		var hold = {|stage|
			Radicles.holdFade(stage, time);
			restores.add({ Radicles.releaseFade(stage) });
		};
		#inKey, key, spaceKey = this.stripKeys(name);
		current = this.readStrip(name, az);
		curMods = current[\mods] ? ();
		tgtMods = if(track[\default] == true, { () }, { track[\mods] ? () });
		curLags = current[\lags] ? ();
		tgtLags = if(track[\default] == true, { () }, { track[\lags] ? () });
		// trim: its lagTime is the load's time, then goes back
		trimLag = Ndef(inKey).get(\lagTime) ? 0;
		this.prApplyArgsMods(Ndef(inKey), (args: (trim: current[\trim]), mods: curMods[\trim] !? {|m| (trim: m) } ? ()),
			(args: (trim: track[\trim] ? 0), mods: tgtMods[\trim] !? {|m| (trim: m) } ? ()),
			{|ndef, pairs|
				if(xfade, {
					hold.(inKey);
					("Ndef(" ++ inKey.cs ++ ").xset('trim', " ++ pairs[1].cs ++ ");").radcode;
				}, {
					("Ndef(" ++ inKey.cs ++ ").set('lagTime', " ++ time.cs ++ ");").radcode(\radpostcont);
					az.setTrim(type, num, pairs[1]);
					restores.add({ ("Ndef(" ++ inKey.cs ++ ").set('lagTime', " ++ trimLag.cs ++ ");").radcode(\radpostcont) });
				});
			});
		// volume: one steady move over the load's time (the strip's own time,
		// vollag, is not used by it; the preset's is set below)
		this.prApplyArgsMods(Ndef(key), (args: (volume: current[\volume]), mods: curMods[\volume] !? {|m| (volume: m) } ? ()),
			(args: (volume: track[\volume] ? 0), mods: tgtMods[\volume] !? {|m| (volume: m) } ? ()),
			{|ndef, pairs|
				if(xfade, {
					hold.(key);
					("Ndef(" ++ key.cs ++ ").xset('volume', " ++ pairs[1].cs ++ ");").radcode;
				}, {
					az.setVolume(type, num, pairs[1], time);
				});
			});
		//the preset's own volume time
		if((curLags[\volume] ? Assemblage.shortLag) != (tgtLags[\volume] ? Assemblage.shortLag), {
			az.setVolumeLag(type, num, tgtLags[\volume] ? Assemblage.shortLag) });
		if((curLags[\trim] ? 0) != (tgtLags[\trim] ? 0), { az.setTrimLag(type, num, tgtLags[\trim] ? 0) });
		// pan: the panner's own lag (NodeProxy:lag rebuilds the panner, so the
		// values change once the old copy has faded out), then the preset's
		panKeys = (current[\pan] ? ()).keys;
		panDefaults = Ndef(spaceKey).source !? {|func| func.respondsTo(\def).if({ ().putPairs(func.def.keyValuePairsFromArgs) }) } ? ();
		pan = if(track[\default] == true or: { track[\pan].isNil }, {
			().putPairs(panKeys.asArray.collect{|k| [k, panDefaults[k] ? 0] }.flatten)
		}, { track[\pan] });
		pan.keys.do{|k| if(panKeys.includes(k).not, {
			left = left.add("pan " ++ k ++ " (" ++ name ++ "'s panner has no such argument)") }) };
		pan = pan.select{|v, k| panKeys.includes(k) };
		this.prApplyArgsMods(Ndef(spaceKey), (args: current[\pan] ? (), mods: curMods[\pan] ? ()),
			(args: pan, mods: (tgtMods[\pan] ? ()).select{|v, k| panKeys.includes(k) }),
			{|ndef, pairs|
				var keys = pairs[0, 2..], ndefKeys = Ndef(spaceKey).getKeysValues.collect(_[0]);
				var tgtPanLags = tgtLags[\pan] ? ();
				if(xfade, {
					hold.(spaceKey);
					("Ndef(" ++ spaceKey.cs ++ ").xset(" ++ pairs.collect(_.cs).join(", ") ++ ");").radcode;
					//the preset's pan lags, where they differ, once the fade is over
					restores.add({
						keys.do{|k| if(((curLags[\pan] ? ())[k] ? 0) != (tgtPanLags[k] ? 0), {
							az.setPanLag(type, num, tgtPanLags[k] ? 0, ndefKeys.indexOf(k) + 1) }) };
					});
				}, {
				subs.add({
					("Ndef(" ++ spaceKey.cs ++ ").lag(" ++ keys.collect{|k| [k, time] }.flatten.collect(_.cs).join(", ") ++ ");").radcode;
					Server.default.sync;
					Ndef(spaceKey).fadeTime.wait;
					pairs.pairsDo{|k, v| az.setPan(type, num, v, ndefKeys.indexOf(k) + 1) };
					time.wait;
				}.fork(AppClock));
				restores.add({
					keys.do{|k| az.setPanLag(type, num, tgtPanLags[k] ? 0, ndefKeys.indexOf(k) + 1) };
				});
				});
			});
		^left
	}

	// ---- sends ----

	*prGlideSends {|type, num, sends, time, restores, atEnd, az|
		var name = this.stripName(type, num), current = this.readSends(name, az) ? (), left = [], cond = Condition(false);
		var busNum = {|bus| bus.asString.divNumStr[1].asInteger };
		// sends the preset doesn't have glide to -inf, then go
		current.keys.asArray.sort{|a, b| busNum.(a) > busNum.(b) }.do{|bus|
			if(sends[bus].isNil, {
				if(current[bus][\mod].notNil, { this.prUnmodSend(type, num, busNum.(bus), sends, az) });
				az.setSendKnob(type, num, this.prSendSlot(type, num, busNum.(bus), az), -inf, time);
				atEnd.add({
					if((this.readSends(name, az) ? ())[bus].notNil, {
						az.removeBus(num, busNum.(bus), type);
					});
				});
			});
		};
		// the preset's sends glide to their levels
		sends.keys.asArray.sort{|a, b| busNum.(a) < busNum.(b) }.do{|bus|
			var send = sends[bus], keys, cur;
			case
			{ bus == name } { left = left.add("send to " ++ bus ++ " (a bus never sends to itself)") }
			{ this.stripNames(az).includes(bus).not } { left = left.add("send to " ++ bus ++ " (no " ++ bus ++ " here)") }
			{
				if(current[bus].isNil, {
					cond.test = false;
					az.bus(num, busNum.(bus), -inf, type, { cond.test = true; cond.signal });
					cond.wait;
					Server.default.sync;
				});
				keys = this.prSendKeys(type, num, busNum.(bus), az);
				cur = this.readSends(name, az) !? {|s| s[bus] } ? ();
				if(keys.notNil, {
					if(cur[\mod].notNil and: { send[\mod].isNil or: { this.prModDiffers(cur[\mod], send[\mod]) } }, {
						this.prUnmodSend(type, num, busNum.(bus), sends, az);
						cur.removeAt(\mod);
					});
					if(send[\mod].isNil, {
						az.setSendKnob(type, num, this.prSendSlot(type, num, busNum.(bus), az), send[\level] ? -inf, time);
						restores.add({
							this.prSendSlot(type, num, busNum.(bus), az) !? {|slot|
								az.setSendKnob(type, num, slot, send[\level] ? -inf, send[\lag] ? 0) };
						});
					}, {
						//a modulated send: the level under the modulator is set
						//at once (it is what comes back when the modulator goes)
						if(cur[\mod].isNil, {
							if((cur[\lag] ? 0) != (send[\lag] ? 0) or: {
								RadDoc.diff((v: cur[\level]), (v: send[\level] ? -inf)).notEmpty }, {
								az.setSendKnob(type, num, this.prSendSlot(type, num, busNum.(bus), az),
									send[\level] ? -inf, send[\lag] ? 0);
							});
							this.prModSend(type, num, busNum.(bus), keys, send, az);
						});
					});
				});
			};
		};
		Server.default.sync;
		^left
	}
}
