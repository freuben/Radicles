// Radicles: track presets (Document Model, step 4, PR 2). A track preset
// holds a whole channel strip: its effect chain (as cssave writes it), trim,
// volume, mute, pan, lags, mixer modulators, its sends (by bus) and its
// source (the blocks feeding it, as blksave writes them; a list, so points
// can join later), and its output to the master (on or off, as when saved;
// 17 Nov 2026). Never solo or record arm: those change other strips or the
// session. One folder for every strip:
//   <RadiclesFiles>/Settings/Presets/units/track/<name>.scd   (your own)
//   <RadiclesLibs>/<lib>/Settings/Presets/units/track/<name>.scd (read too)
//   tsave t 1 vox           track 1 -> your track presets (b 1, m, or a mixer position)
//   tload t 3 vox           the preset onto track 3: source, chain, values, sends
//   tload t 3 vox 10 glide  ... over 10 seconds, values gliding (a transition:
//                           see RadUnitTrackMorph.sc; none or 0: at once)
//   tload t 3 vox nosrc     ... keeping track 3's source
//   tload t 3 vox nofx      ... keeping track 3's effects
//   tload t 3 default       a clean strip, its source stopped (nosrc keeps it)
//   tload                   lists track presets
// Loading at once (none, or a time of 0) changes only what differs, in this
// order (design note, 16 Nov): loads running on the strip stop; a preset that mutes mutes first; the source;
// the chain (as csload); trim, volume, pan, lags, mixer modulators; the
// sends; a preset that unmutes unmutes last. Decisions (16 Nov 2026):
// - the source has no block number: it goes to the block(s) feeding the
//   strip it is loaded onto, which play the preset's definition if they play
//   another (unlike blkload); a track nothing feeds gets a new block,
//   connected (as blk add, blk N <> N); a track fed only by an Ndef of your
//   own keeps it (the source is left out and named); a block that also feeds
//   another track is left out and named
// - a preset saved with no source stops the blocks feeding the track
// - sends the preset doesn't have are removed; a send to a bus the session
//   doesn't have is left out and named
// - a live input (audioin block) comes back with its hardware input
// - default: trim 0, volume 0 dB, pan centre, unmuted, no lags, no
//   modulators, an empty chain, no sends, output to the master, and no
//   source: the blocks feeding the track stop, as a preset saved with no
//   source (18 Nov 2026; before, the source was left alone)
// - the output to the master comes back as it was saved (17 Nov 2026): a
//   preset that doesn't go to the master turns it off first, one that does
//   turns it on last; a preset saved before then leaves it as it is
// Parts the target strip can't have (a source on a bus or the master, sends
// on the master) are left out silently.

+ RadUnit {

	*trackPresetDir { ^this.presetRoot +/+ "track" }

	// where tsave writes: your own track presets
	*trackPresetPath {|name| ^this.trackPresetDir +/+ name.asString ++ ".scd" }

	*trackPresetDirs {
		^[this.trackPresetDir] ++ this.libraries.collect{|lib| this.libraryPresetRoot(lib) +/+ "track" }
	}

	// the file of a track preset (your own first), or nil
	*findTrackPresetPath {|name|
		if(this.validPresetName(name).not, { ^nil });
		^this.trackPresetDirs.collect{|dir| dir +/+ name.asString ++ ".scd" }.detect{|path| File.exists(path) }
	}

	*trackPresetExists {|name|
		^(name.asSymbol == \default) or: { this.findTrackPresetPath(name).notNil }
	}

	*trackPresetNames {
		^this.trackPresetDirs.collect{|dir|
			(dir +/+ "*.scd").pathMatch.collect{|path| path.basename.splitext[0].asSymbol }
		}.flatten.asSet.asArray.sort
	}

	*postTrackPresets {
		var names = this.trackPresetNames;
		("Track presets: default" ++ if(names.isEmpty, { "" }, { ", " ++ names.join(", ") })).postln;
	}

	// \track, \bus or \master, from a strip's name
	*stripKind {|name|
		name = name.asString;
		^case
		{ name == "master" } { \master }
		{ name.beginsWith("bus") } { \bus }
		{ \track }
	}

	// the keys of a strip unit a track preset holds (beside chain and source)
	// (output: \master or \none; the master strip has none)
	*trackValueKeys { ^[\trim, \volume, \mute, \pan, \lags, \mods, \sends, \output] }

	// ---- a strip as a track preset ----

	// the track preset of a strip, read from the running system, or nil if
	// the mixer has no such strip
	*readTrack {|type, num, az|
		var name = this.stripName(type, num), unit, track, source;
		az = az ? Radicles.aZ;
		unit = this.readStrip(name, az);
		if(unit.isNil, { ^nil });
		track = (from: this.stripKind(name), channels: unit[\channels], space: unit[\space]);
		this.trackValueKeys.do{|key| unit[key] !? {|value| track[key] = value } };
		if(this.stripKind(name) == \master, { track.removeAt(\sends) });
		track[\chain] = this.readChain(type, num, az);
		if(this.stripKind(name) == \track, {
			source = [];
			(unit[\input] ? []).do{|key|
				var block;
				if(this.isBlockTag(key), {
					block = this.readBlock(key);
					case
					{ block.isNil } { ("tsave: " ++ key ++ " plays a pattern or nothing; left out").postln }
					{ this.recordsDef(block[\def]) } {
						("tsave: " ++ key ++ " records (" ++ block[\def] ++ "); left out").postln }
					{ source = source.add(this.presetUnit(this.blockPresetUnit(block))) };
				}, {
					("tsave: " ++ name ++ " is fed by Ndef " ++ key.cs ++ ", which is not a block; left out").postln;
				});
			};
			//saved even when empty: a preset with no source stops the track's
			//source when loaded (16 Nov 2026)
			track[\source] = source;
		});
		^track
	}

	// write a track preset file; returns the path
	*writeTrackPreset {|track, name|
		var path = this.trackPresetPath(name), about;
		about = "track preset · " ++ track[\from] ++ " · " ++ ((track[\chain] ? ())[\fx] ? ()).size ++ " effects";
		if((track[\source] ? []).notEmpty, { about = about ++ " · source " ++ track[\source].collect(_[\def]).join(", ") });
		RadDoc((format: RadDoc.formatVersion, type: \track, track: track)).write(path, about);
		^path
	}

	// the clean strip of tload default: no source (18 Nov 2026)
	*defaultTrack {
		^(default: true, trim: 0, volume: 0, mute: 0, chain: (fx: ()), sends: (), output: \master, source: [])
	}

	// the track in a preset file (default: a clean strip), or nil with the
	// reason posted. Effects and blocks this Radicles doesn't have are left
	// out and named.
	*readTrackPreset {|name|
		var path, data, track, bad;
		if(name.asSymbol == \default and: { this.findTrackPresetPath(name).isNil }, { ^this.defaultTrack });
		path = this.findTrackPresetPath(name);
		if(path.isNil, { ("No track preset " ++ name).warn; ^nil });
		data = RadDoc.readData(File.readAllString(path), path);
		if(data.isNil, { ^nil });
		track = data[\track];
		if(data[\type] != \track or: { track.isKindOf(Dictionary).not }, {
			("RadUnit: " ++ path ++ " is not a track preset").warn; ^nil });
		track[\chain] = this.prValidChain(track[\chain] ? (fx: ()), "Track preset " ++ name);
		track[\output] !? {|out|
			if([\master, \none].includes(out).not, {
				("Track preset " ++ name ++ ": output " ++ out.cs ++ " is not master or none; left out").warn;
				track.removeAt(\output);
			});
		};
		track[\source] !? {|source|
			bad = source.select{|unit|
				unit.isKindOf(Dictionary).not or: { unit[\kind] != \block } or: {
					(try { SynthFile.read(\block, unit[\def]) } { nil }).isNil } };
			if(bad.notEmpty, {
				("Track preset " ++ name ++ ": left out source(s) " ++
					bad.collect{|unit| (unit !? { unit[\def] }).asString }.join(", ") ++ " (not a block this Radicles has)").warn;
				track[\source] = source.reject{|unit| bad.includes(unit) };
			});
		};
		^track
	}

	// ---- saving and loading ----

	// tsave: asks before replacing one of your own (refused where there is no
	// window to ask in). Returns the track saved, or nil when refused.
	*saveTrackPreset {|type, num, name, action, az|
		var track, write;
		az = az ? Radicles.aZ;
		if(az.isNil, { "could not find assemblage".warn; ^nil });
		if(this.validPresetName(name).not, {
			("Preset names use letters, digits, - and _ only: " ++ name).warn; ^nil });
		if(name.asSymbol == \default, {
			"default is a clean strip (tload t 1 default); choose another name".warn; ^nil });
		track = this.readTrack(type, num, az);
		if(track.isNil, { ("No " ++ this.stripName(type, num)).warn; ^nil });
		write = {
			var path = this.writeTrackPreset(track, name);
			("Saved track preset " ++ name ++ ": " ++ path).postln;
			action.(path);
		};
		if(File.exists(this.trackPresetPath(name)), {
			if(GUI.current.notNil, {
				{ Window.warnQuestion("Track preset " ++ name ++ " exists. Replace it?", write) }.defer;
			}, {
				("Track preset " ++ name ++ " exists; not replaced (no window to ask in)").warn;
			});
		}, write);
		^track
	}

	// tload: the preset onto the strip, as a transition (morphTrack, in
	// RadUnitTrackMorph.sc): time in seconds, else the strip's own fade time;
	// shape \xfade, \glide or \none, else the preset's morphShape, else
	// \xfade; a time of 0 or \none loads at once (applyTrack). Returns false
	// when there is no such preset or strip.
	*loadTrackPreset {|type, num, name, nosrc = false, nofx = false, action, az, time, shape|
		var track;
		az = az ? Radicles.aZ;
		if(az.isNil, { "could not find assemblage".warn; ^false });
		if(az.getThisTrack(type, num).isNil, { ("No " ++ this.stripName(type, num)).warn; ^false });
		if(this.trackPresetExists(name).not, { ("No track preset " ++ name).warn; ^false });
		track = this.readTrackPreset(name);
		if(track.isNil, { ^false });
		//the libraries its effects, blocks and wavetables come from
		this.useLibraries(this.trackLibraries(track, nosrc, nofx), "tload");
		^this.morphTrack(type, num, track, time, shape, nosrc, nofx, action, az)
	}

	// bring a strip to a track preset, changing only what differs, in the
	// order above. Call inside a Routine. Returns the parts left out.
	*applyTrack {|type, num, track, nosrc = false, nofx = false, az, stopTrack = true|
		var name = this.stripName(type, num), kind = this.stripKind(name), current, left = [];
		az = az ? Radicles.aZ;
		current = this.readStrip(name, az);
		if(current.isNil, { ("No " ++ name).warn; ^nil });
		// 1. loads running on the strip stop where they are
		// (stopTrack false: called from a track load, which goes on)
		this.prStopStripMorphs(type, num, az, stopTrack);
		// 2. a preset that mutes mutes first
		if(track[\mute] == 1 and: { current[\mute] != 1 }, { az.setMute(type, num, 1) });
		// a preset that doesn't go to the master turns the output off first
		if(track[\output] == \none, { this.prSetOutput(type, num, \none, az) });
		// 3. the source
		if(nosrc.not and: { kind == \track } and: { track[\source].notNil }, {
			left = left ++ this.prApplySource(num, track[\source], az);
		});
		// 4. the chain
		if(nofx.not, { this.applyChain(type, num, track[\chain] ? (fx: ()), az) });
		// 5. the strip's own values
		left = left ++ this.prApplyStripValues(type, num, track, az);
		// 6. the sends
		if(kind != \master, { left = left ++ this.prApplySends(type, num, track[\sends] ? (), az) });
		// 7. a preset that unmutes unmutes last
		if(track[\mute] != 1 and: { this.readStrip(name, az) !? { |s| s[\mute] == 1 } ? false }, { az.setMute(type, num, 0) });
		// a preset that goes to the master turns the output on last
		if(track[\output] == \master, { this.prSetOutput(type, num, \master, az) });
		Server.default.sync;
		{ az.mixWinBool({ az.refreshMixGUI }); az.refreshFunc }.defer;
		if(left.notEmpty, { ("tload " ++ name ++ ": left out " ++ left.join("; ")).warn });
		^left
	}

	// the strip's output to the master on (\master) or off (\none), through
	// the mixer's own setter (as outmenu), only when it differs. The master
	// strip has no output to the master.
	*prSetOutput {|type, num, output, az|
		var name = this.stripName(type, num), current;
		if(this.stripKind(name) == \master, { ^this });
		current = this.readStrip(name, az) !? {|strip| strip[\output] };
		if(current != output, {
			az.setTrackOut(type, num, if(output == \master, { 1 }, { 0 }));
			Server.default.sync;
		});
	}

	// stop the loads running on a strip: its chain's, its slots', and those
	// of the blocks feeding it
	*prStopStripMorphs {|type, num, az, stopTrack = true|
		var name = this.stripName(type, num);
		if(stopTrack, { this.prStopTrackMorph(name) });
		az.prStopMorph(name);
		(az.filters ? []).do{|item| if(az.filterTagOnStrip(item[0], type, num), { az.prStopMorph(item[0]) }) };
		(this.readInput(name, az) ? []).do{|key| if(this.isBlockTag(key), { this.prStopBlockMorph(key) }) };
	}

	// ---- the source ----

	// the blocks feeding a track, as numbers; a track nothing feeds gets a new
	// block, connected (as blk add and blk N <> N). Returns [blocks, left].
	*prSourceBlocks {|num, az|
		var name = this.stripName(\track, num), input = this.readInput(name, az), blocks, cond;
		if(input.isNil, {
			cond = Condition(false);
			Block.add(Ndef(this.stripKeys(name)[0]).numChannels, {|ndef|
				az.input(ndef, \track, num);
				cond.test = true; cond.signal;
			});
			cond.wait;
			Server.default.sync;
			input = this.readInput(name, az) ? [];
		});
		blocks = input.select{|key| this.isBlockTag(key) };
		if(blocks.isEmpty, {
			^[[], [name ++ " is fed by " ++ input.collect{|key| "Ndef(" ++ key.cs ++ ")" }.join(", ") ++
				", not a block: source not loaded"]] });
		^[blocks.collect{|key| key.asString.divNumStr[1].asInteger }, []]
	}

	// does block (a number) also feed a track other than track num?
	*prBlockShared {|block, num, az|
		var key = this.blockTag(block);
		^this.stripNames(az).any{|name|
			(name != this.stripName(\track, num)) and: { (this.readInput(name, az) ? []).includes(key) } }
	}

	// the preset's source onto the blocks feeding track num, in input order.
	// Returns what was left out.
	*prApplySource {|num, source, az|
		var name = this.stripName(\track, num), found, blocks, left;
		if(source.isEmpty, {
			//saved with no source: the track ends as it was saved (16 Nov 2026)
			(this.readInput(name, az) ? []).do{|key|
				var block;
				if(this.isBlockTag(key), {
					block = key.asString.divNumStr[1].asInteger;
					if(this.prBlockShared(block, num, az).not and: { this.readBlock(key).notNil }, {
						Block.stop(block); Server.default.sync;
					});
				});
			};
			^[]
		});
		found = this.prSourceBlocks(num, az);
		blocks = found[0]; left = found[1];
		source.do{|unit, index|
			var block = blocks[index], current;
			//what the preset doesn't carry (a recording saved without keep), a
			//block playing the same definition keeps, as blkload does
			current = block !? { this.readBlock(this.blockTag(block)) };
			if(current.notNil and: { current[\def] == unit[\def] }, {
				unit = unit.copy;
				[\buffer, \data].do{|key| if(unit[key].isNil and: { current[key].notNil }, { unit[key] = current[key] }) };
			});
			case
			{ block.isNil } { if(blocks.notEmpty, { left = left.add("source " ++ (index + 1) ++ " (" ++ unit[\def] ++
				"): " ++ name ++ " has " ++ blocks.size ++ " block(s)") }) }
			{ this.prBlockShared(block, num, az) } {
				left = left.add("block " ++ block ++ " also feeds another track: source " ++ unit[\def] ++ " not loaded") }
			{
				if(this.applyBlock(block, unit, anyDef: true), {
					Server.default.sync;
					{ az.updateSourceWin(this.blockTag(block)) }.defer;
				}, {
					left = left.add("block " ++ block ++ ": source " ++ unit[\def] ++ " not loaded");
				});
			};
		};
		^left
	}

	// ---- the strip's own values ----

	// trim, volume and pan (with their modulators) through the mixer's own
	// setters, so the mixer window follows; then the lags. Returns what was
	// left out.
	*prApplyStripValues {|type, num, track, az|
		var name = this.stripName(type, num), inKey, key, spaceKey, current, curMods, tgtMods, left = [];
		var pan, panKeys, panDefaults, curLags, tgtLags, setLag;
		#inKey, key, spaceKey = this.stripKeys(name);
		current = this.readStrip(name, az);
		curMods = current[\mods] ? ();
		tgtMods = if(track[\default] == true, { () }, { track[\mods] ? () });
		// trim and volume
		this.prApplyArgsMods(Ndef(inKey), (args: (trim: current[\trim]), mods: curMods[\trim] !? {|m| (trim: m) } ? ()),
			(args: (trim: track[\trim] ? 0), mods: tgtMods[\trim] !? {|m| (trim: m) } ? ()),
			{|ndef, pairs| az.setTrim(type, num, pairs[1]) });
		this.prApplyArgsMods(Ndef(key), (args: (volume: current[\volume]), mods: curMods[\volume] !? {|m| (volume: m) } ? ()),
			(args: (volume: track[\volume] ? 0), mods: tgtMods[\volume] !? {|m| (volume: m) } ? ()),
			{|ndef, pairs| az.setVolume(type, num, pairs[1]) });
		// pan, by the panner's own argument names
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
				var keys = Ndef(spaceKey).getKeysValues.collect(_[0]);
				pairs.pairsDo{|k, v| az.setPan(type, num, v, keys.indexOf(k) + 1) } });
		// lags: those the preset has, and 0 for those it hasn't
		curLags = current[\lags] ? ();
		tgtLags = if(track[\default] == true, { () }, { track[\lags] ? () });
		if((curLags[\volume] ? Assemblage.shortLag) != (tgtLags[\volume] ? Assemblage.shortLag), {
			az.setVolumeLag(type, num, tgtLags[\volume] ? Assemblage.shortLag) });
		if((curLags[\trim] ? Assemblage.shortLag) != (tgtLags[\trim] ? Assemblage.shortLag), {
			az.setTrimLag(type, num, tgtLags[\trim] ? Assemblage.shortLag) });
		panKeys.do{|k|
			var cur = (curLags[\pan] ? ())[k] ? 0, tgt = (tgtLags[\pan] ? ())[k] ? 0, keys;
			if(cur != tgt, {
				keys = Ndef(spaceKey).getKeysValues.collect(_[0]);
				az.setPanLag(type, num, tgt, keys.indexOf(k) + 1);
			});
		};
		Server.default.sync;
		^left
	}

	// ---- the sends ----

	// the send slot a strip uses for a bus (slots follow bus order), or nil
	*prSendSlot {|type, num, bus, az|
		var ndef = Ndef(this.stripName(type, num)), buses = [];
		(az.busArr ? []).do{|item|
			if(item[0].notNil and: { item[1].notNil } and: { item[1].includes(ndef) }, {
				buses = buses.add(item[0].asString.divNumStr[1].asInteger) });
		};
		^buses.indexOf(bus) !? {|i| i + 1 }
	}

	// the bus Ndef and its level and lag keys for a strip's send to bus
	*prSendKeys {|type, num, bus, az|
		var busTag = ("busIn" ++ bus).asSymbol, members, index;
		members = (az.busArr ? []).detect{|item| item[0] == busTag } !? {|item| item[1] } ? [];
		index = members.indexOf(Ndef(this.stripName(type, num)));
		if(index.isNil, { ^nil });
		^[Ndef(busTag), ("vol" ++ (index + 1)).asSymbol, ("lag" ++ (index + 1)).asSymbol]
	}

	// bring a strip's sends to sends ((busN: (level, lag, mod))): sends the
	// preset doesn't have are removed, sends to buses the session doesn't have
	// are left out and named. The strip's output to the master is not touched.
	// Returns what was left out.
	*prApplySends {|type, num, sends, az|
		var name = this.stripName(type, num), current = this.readSends(name, az) ? (), left = [], cond = Condition(false);
		var busNum = {|bus| bus.asString.divNumStr[1].asInteger };
		// sends the preset doesn't have, highest bus first
		current.keys.asArray.sort{|a, b| busNum.(a) > busNum.(b) }.do{|bus|
			if(sends[bus].isNil, {
				if(current[bus][\mod].notNil, { this.prUnmodSend(type, num, busNum.(bus), sends, az) });
				az.removeBus(num, busNum.(bus), type);
				Server.default.sync;
			});
		};
		// the preset's sends, lowest bus first
		sends.keys.asArray.sort{|a, b| busNum.(a) < busNum.(b) }.do{|bus|
			var send = sends[bus], keys, cur, mods;
			case
			{ bus == name } { left = left.add("send to " ++ bus ++ " (a bus never sends to itself)") }
			{ this.stripNames(az).includes(bus).not } { left = left.add("send to " ++ bus ++ " (no " ++ bus ++ " here)") }
			{
				if(current[bus].isNil, {
					cond.test = false;
					az.bus(num, busNum.(bus), send[\level] ? -inf, type, { cond.test = true; cond.signal });
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
					if(send[\mod].isNil and: { RadDoc.diff((v: cur[\level]), (v: send[\level] ? -inf)).notEmpty }, {
						az.setSendKnob(type, num, this.prSendSlot(type, num, busNum.(bus), az), send[\level] ? -inf);
					});
					//the send's own time after its level, as for trim and volume:
					//the level moves over the time the send has now
					this.prSendLag(type, num, busNum.(bus), cur, send, az);
					if(send[\mod].notNil and: { cur[\mod].isNil }, {
						this.prModSend(type, num, busNum.(bus), keys, send, az);
					});
				});
			};
		};
		Server.default.sync;
		^left
	}

	// a send's own time for typed values (sndlag), when the preset's differs
	*prSendLag {|type, num, bus, cur, send, az|
		if((cur[\lag] ? Assemblage.shortLag) != (send[\lag] ? Assemblage.shortLag), {
			az.setSendVolLag(type, num, bus, send[\lag] ? Assemblage.shortLag) });
	}

	// take a send's modulator off, the level going to the preset's (or -inf)
	*prUnmodSend {|type, num, bus, sends, az|
		var keys = this.prSendKeys(type, num, bus, az), level;
		if(keys.isNil, { ^this });
		level = (sends[("bus" ++ bus).asSymbol] ? ())[\level] ? -inf;
		ModMap.unmap(keys[0], keys[1], level);
		az.prForgetModSend(type, num, this.prSendSlot(type, num, bus, az));
		Server.default.sync;
	}

	// a send's modulator, by what it is
	*prModSend {|type, num, bus, keys, send, az|
		var mod = send[\mod], modNdef, modCurrent;
		if(mod.isKindOf(Dictionary).not or: { mod[\type].isNil }, { ^this });
		modNdef = ModMap.map(keys[0], keys[1], mod[\type], mod[\spec], nil, this.prFunc(mod[\func]), lag: mod[\lag]);
		if(modNdef.isKindOf(NodeProxy), {
			modCurrent = this.readArgs(modNdef, nil);
			this.prApplyArgsMods(modNdef, (args: modCurrent[0], mods: modCurrent[1]), mod);
			az.prAddModSend(type, num, this.prSendSlot(type, num, bus, az), modNdef);
		});
		Server.default.sync;
	}
}
