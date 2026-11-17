// Radicles: track morph (Document Model, step 4, PR 3). tmorph moves every
// part of a strip to a track preset over one morph time, so everything
// arrives together (decided 16 Nov 2026); each part morphs as its own
// command does:
//   chain: as csmorph (slot rules of step 3, shape xfade or glide)
//   source, same block definition and file: as blkmorph
//   source, another definition or file: crossfade (the block's fade time is
//     the morph time while it plays the new one)
//   trim, volume, pan: glide (a lag over the morph time, then the strip's
//     own lag goes back)
//   sends: glide; a send only in the preset is made at -inf and glides up,
//     a send only on the strip glides to -inf and is removed at the end
//   mixer and send modulators: as with tload, at the start (they fade with
//     the Ndef's own fade time, as effect modulators do)
//   mute: off at the start, on at the end
// The time: the command's, else the preset's, else the strip's own
// (tmorphtime), else the global morphtime. shape none is a tload. A new
// tmorph, tload, csmorph or blkmorph on the strip stops a running one where
// it is, putting the lags and fade times back.
//   tmorph t 3 vox [time] [xfade|glide|none] [nosrc] [nofx]
//   tmorphtime t 3 5  /  tmorphtime t 3  /  tmorphtime t 3 default

+ RadUnit {

	*prTrackMorphs { ^trackMorphs ?? { trackMorphs = IdentityDictionary.new } }

	// stop a running track morph on a strip, putting its times back
	*prStopTrackMorph {|name|
		var morph = this.prTrackMorphs[name.asSymbol];
		if(morph.notNil, {
			morph[\routine].stop;
			morph[\subs].do{|sub| sub.stop };
			morph[\restore].value;
			trackMorphs.removeAt(name.asSymbol);
		});
	}

	// tmorph: returns false when there is no such preset or strip
	*morphTrackPreset {|type, num, name, time, shape, nosrc = false, nofx = false, action, az|
		var track;
		az = az ? Radicles.aZ;
		if(az.isNil, { "could not find assemblage".warn; ^false });
		if(az.getThisTrack(type, num).isNil, { ("No " ++ this.stripName(type, num)).warn; ^false });
		if(this.trackPresetExists(name).not, { ("No track preset " ++ name).warn; ^false });
		track = this.readTrackPreset(name);
		if(track.isNil, { ^false });
		this.useLibraries(this.trackLibraries(track, nosrc, nofx), "tmorph");
		^this.morphTrack(type, num, track, time, shape, nosrc, nofx, action, az)
	}

	// morph a strip to track over time. Returns true when it starts.
	*morphTrack {|type, num, track, time, shape, nosrc = false, nofx = false, action, az|
		var name = this.stripName(type, num), kind = this.stripKind(name);
		az = az ? Radicles.aZ;
		time = (time ?? { track[\morphTime] } ?? { az.trackMorphTimeOf(name) } ?? { morphTime }).max(0);
		shape = (shape ? \xfade).asSymbol;
		if([\xfade, \glide, \none].includes(shape).not, {
			("tmorph: shape is xfade, glide or none, not " ++ shape).warn; ^false });
		if(shape == \none or: { time == 0 }, {
			{
				this.prStopTrackMorph(name);
				this.applyTrack(type, num, track, nosrc, nofx, az);
				action.();
			}.fork(AppClock);
			^true
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
			// the chain morphs on its own, over the same time
			if(nofx.not, { az.morphChain(type, num, track[\chain] ? (fx: ()), time, shape) });
			// the source
			if(nosrc.not and: { kind == \track } and: { track[\source].notNil }, {
				left = left ++ this.prMorphSource(num, track[\source], time, shape, restores, az);
			});
			// trim, volume, pan
			left = left ++ this.prGlideStripValues(type, num, track, time, restores, subs, az);
			// sends
			if(kind != \master, {
				left = left ++ this.prGlideSends(type, num, track[\sends] ? (), time, restores, atEnd, az);
			});
			track[\morphTime] !? {|t| az.setTrackMorphTime(name, t) };
			Server.default.sync;
			{ az.mixWinBool({ az.refreshMixGUI }); az.refreshFunc }.defer;
			if(left.notEmpty, { ("tmorph " ++ name ++ ": left out " ++ left.join("; ")).warn });
			time.wait;
			// removed sends go; mute: on at the end
			atEnd.do(_.value);
			if(track[\mute] == 1 and: { current[\mute] != 1 }, { az.setMute(type, num, 1) });
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
						Block.stop(block, time);
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
					//the same block and file: as blkmorph
					this.morphBlockUnit(block, unit, time, shape);
				}, {
					//another block or file: a crossfade over the morph time
					fade = Ndef(tag).fadeTime;
					("Ndef(" ++ tag.cs ++ ").fadeTime = " ++ time.cs ++ ";").radcode;
					restores.add({ ("Ndef(" ++ tag.cs ++ ").fadeTime = " ++ fade.cs ++ ";").radcode });
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

	*prGlideStripValues {|type, num, track, time, restores, subs, az|
		var name = this.stripName(type, num), inKey, key, spaceKey, current, curMods, tgtMods, left = [];
		var pan, panKeys, panDefaults, curLags, tgtLags, trimLag;
		#inKey, key, spaceKey = this.stripKeys(name);
		current = this.readStrip(name, az);
		curMods = current[\mods] ? ();
		tgtMods = if(track[\default] == true, { () }, { track[\mods] ? () });
		curLags = current[\lags] ? ();
		tgtLags = if(track[\default] == true, { () }, { track[\lags] ? () });
		// trim: its lagTime is the morph time, then goes back
		trimLag = Ndef(inKey).get(\lagTime) ? 0;
		this.prApplyArgsMods(Ndef(inKey), (args: (trim: current[\trim]), mods: curMods[\trim] !? {|m| (trim: m) } ? ()),
			(args: (trim: track[\trim] ? 0), mods: tgtMods[\trim] !? {|m| (trim: m) } ? ()),
			{|ndef, pairs|
				("Ndef(" ++ inKey.cs ++ ").set('lagTime', " ++ time.cs ++ ");").radcode(\radpostcont);
				az.setTrim(type, num, pairs[1]);
				restores.add({ ("Ndef(" ++ inKey.cs ++ ").set('lagTime', " ++ trimLag.cs ++ ");").radcode(\radpostcont) });
			});
		// volume: its lagTime is the morph time, then the preset's volume lag
		this.prApplyArgsMods(Ndef(key), (args: (volume: current[\volume]), mods: curMods[\volume] !? {|m| (volume: m) } ? ()),
			(args: (volume: track[\volume] ? 0), mods: tgtMods[\volume] !? {|m| (volume: m) } ? ()),
			{|ndef, pairs|
				az.setVolume(type, num, pairs[1], time);
				restores.add({ az.setVolumeLag(type, num, tgtLags[\volume] ? 0) });
			});
		//a volume that doesn't change still takes the preset's lag at the end
		if((curLags[\volume] ? 0) != (tgtLags[\volume] ? 0), {
			restores.add({ az.setVolumeLag(type, num, tgtLags[\volume] ? 0) });
		});
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
						if(cur[\mod].isNil, { this.prModSend(type, num, busNum.(bus), keys, send, az) });
					});
				});
			};
		};
		Server.default.sync;
		^left
	}
}
