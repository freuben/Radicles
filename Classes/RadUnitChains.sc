// Radicles: effect-chain presets (Document Model, step 3, PR 2). A chain
// preset holds a strip's effects: each slot's effect unit as fxsave writes it
// (values, modulators, buffer or data by name), whether the slot is bypassed,
// and the slot's own morph time. Nothing else of the strip (its trim,
// volume, pan, sends and source go with track presets, step 4). One folder
// for every strip, since a chain belongs to no definition:
//   <RadiclesFiles>/Settings/Presets/units/chain/<name>.scd   (your own)
//   <RadiclesLibs>/<lib>/Settings/Presets/units/chain/<name>.scd (read too)
//   cssave t 1 warmVox      track 1's chain -> your presets
//   csload t 3 warmVox      the chain onto track 3 (any strip: a bus, the master)
//   csload t 3 default      an empty chain: every effect on track 3 removed
//   csload                  lists chain presets
// Loading changes only what differs: a slot holding the same effect gets the
// preset's values and modulators (as fxload does), a slot with another effect
// or none gets the preset's effect, a slot the preset leaves empty is
// removed, and bypass follows the preset. Chain presets saved before Nov 2026
// (PresetFile \track) still load, as they did.

+ RadUnit {

	*chainPresetDir { ^this.presetRoot +/+ "chain" }

	// where cssave writes: your own chain presets
	*chainPresetPath {|name| ^this.chainPresetDir +/+ name.asString ++ ".scd" }

	*chainPresetDirs {
		^[this.chainPresetDir] ++ this.libraries.collect{|lib| this.libraryPresetRoot(lib) +/+ "chain" }
	}

	// the file of a chain preset (your own first), or nil
	*findChainPresetPath {|name|
		if(this.validPresetName(name).not, { ^nil });
		^this.chainPresetDirs.collect{|dir| dir +/+ name.asString ++ ".scd" }.detect{|path| File.exists(path) }
	}

	// a chain preset saved before Nov 2026 (PresetFile \track)?
	*isOldChainPreset {|name|
		^(try { PresetFile.read(\track) } { nil } ? []).includes(name.asSymbol)
	}

	*chainPresetExists {|name|
		^name.asSymbol == \default or: { this.findChainPresetPath(name).notNil }
	}

	// the names of the chain presets (own and in libraries), sorted
	*chainPresetNames {
		^this.chainPresetDirs.collect{|dir|
			(dir +/+ "*.scd").pathMatch.collect{|path| PathName(path).fileNameWithoutExtension.asSymbol }
		}.flatten(1).asSet.asArray.sort
	}

	// posts the chain presets, and the old ones that still load
	*postChainPresets {
		var names = this.chainPresetNames, old = (try { PresetFile.read(\track) } { nil } ? [])
		.reject{|name| names.includes(name) };
		("Chain presets: " ++ if(names.isEmpty, { "none" }, { names.join(", ") })).postln;
		if(old.notEmpty, { ("Older chain presets (they load as before): " ++ old.join(", ")).postln });
		^names
	}

	// ---- a strip's chain ----

	*chainTag {|type, num, slot|
		^("filter" ++ type.asString.capitalise ++ "_" ++ num ++ "_" ++ slot).asSymbol
	}

	// the chain of a strip: (fx: (slot: unit, ...)), the units as the document
	// has them (with bypass and the slot's morph time)
	*readChain {|type, num, az|
		var fx = ();
		az = az ? Radicles.aZ;
		if(az.isNil, { ^(fx: fx) });
		(az.filters ? []).do{|item|
			if(az.filterTagOnStrip(item[0], type, num), {
				this.read(item[0], az) !? {|unit|
					unit.removeAt(\position);
					fx[az.convFilterTag(item[0])[2].asInteger] = unit;
				};
			});
		};
		^(fx: fx) ++ (az.chainMorphTimeOf(this.stripName(type, num)) !? {|time| (morphTime: time) } ? ())
	}

	// write a chain as a preset file; returns the path
	*writeChainPreset {|chain, name|
		var path = this.chainPresetPath(name);
		RadDoc((format: RadDoc.formatVersion, type: \chain, chain: chain))
		.write(path, "chain preset · " ++ (chain[\fx] ? ()).size ++ " effects");
		^path
	}

	// the chain in a preset file (default: an empty chain), or nil with the
	// reason posted
	*readChainPreset {|name|
		var path, data, chain, bad;
		if(name.asSymbol == \default and: { this.findChainPresetPath(name).isNil }, { ^(fx: ()) });
		path = this.findChainPresetPath(name);
		if(path.isNil, { ("No chain preset " ++ name).warn; ^nil });
		data = RadDoc.readData(File.readAllString(path), path);
		if(data.isNil, { ^nil });
		chain = data[\chain];
		if(data[\type] != \chain or: { chain.isKindOf(Dictionary).not }, {
			("RadUnit: " ++ path ++ " is not a chain preset").warn; ^nil });
		chain[\fx] = chain[\fx] ? ();
		bad = [];
		chain[\fx].keysValuesDo{|slot, unit|
			if(slot.isInteger.not or: { unit.isKindOf(Dictionary).not } or: { unit[\kind] != \filter } or: {
				(try { SynthFile.read(\filter, unit[\def]) } { nil }).isNil }, { bad = bad.add(slot) });
		};
		if(bad.notEmpty, {
			("Chain preset " ++ name ++ ": left out slot(s) " ++ bad.collect(_.asString).sort.join(", ") ++
				" (not an effect this Radicles has)").warn;
			bad.do{|slot| chain[\fx].removeAt(slot) };
		});
		^chain
	}

	// ---- saving and loading ----

	// cssave: asks before replacing one of your own (refused where there is
	// no window to ask in). Returns the chain saved, or nil when refused.
	*saveChainPreset {|type, num, name, action, az|
		var chain, write;
		az = az ? Radicles.aZ;
		if(az.isNil, { "could not find assemblage".warn; ^nil });
		if(this.validPresetName(name).not, {
			("Preset names use letters, digits, - and _ only: " ++ name).warn; ^nil });
		if(name.asSymbol == \default, {
			"default is an empty chain (csload t 1 default); choose another name".warn; ^nil });
		chain = this.readChain(type, num, az);
		if(chain[\fx].isEmpty, { ("No effects on " ++ this.stripName(type, num) ++ "; saving an empty chain").postln });
		write = {
			var path = this.writeChainPreset(chain, name);
			("Saved chain preset " ++ name ++ ": " ++ path).postln;
			action.(path);
		};
		if(File.exists(this.chainPresetPath(name)), {
			if(GUI.current.notNil, {
				{ Window.warnQuestion("Chain preset " ++ name ++ " exists. Replace it?", write) }.defer;
			}, {
				("Chain preset " ++ name ++ " exists; not replaced (no window to ask in)").warn;
			});
		}, write);
		^chain
	}

	*stripName {|type, num| ^if(type.asSymbol == \master, { \master }, { (type.asString ++ num).asSymbol }) }

	// csload: the preset's chain onto the strip (or an old preset as before).
	// Returns false when there is no such preset.
	*loadChainPreset {|type, num, name, action, az|
		var chain;
		az = az ? Radicles.aZ;
		if(az.isNil, { "could not find assemblage".warn; ^false });
		if(az.getThisTrack(type, num).isNil, { ("No " ++ this.stripName(type, num)).warn; ^false });
		if(this.chainPresetExists(name).not, {
			if(this.isOldChainPreset(name), { az.loadTrackPreset(type, num, name); action.(); ^true });
			("No chain preset " ++ name).warn; ^false;
		});
		chain = this.readChainPreset(name);
		if(chain.isNil, { ^false });
		{
			this.applyChain(type, num, chain, az);
			action.();
		}.fork(AppClock);
		^true
	}

	// the keys of args in the order of the effect's arguments (others after)
	*prArgOrder {|def, args|
		var names = (try { SynthFile.read(\filter, def).argNames.asArray } { nil } ? []), keys = args.keys.asArray;
		^names.select{|name| keys.includes(name) } ++ keys.reject{|key| names.includes(key) }.sort
	}

	// bring a strip's chain to chain, changing only what differs. Call inside
	// a Routine.
	*applyChain {|type, num, chain, az|
		var current, target = chain[\fx] ? (), cond = Condition(false), wait;
		az = az ? Radicles.aZ;
		wait = { cond.wait; cond.test = false };
		current = this.readChain(type, num, az)[\fx];
		// 1. slots the preset leaves empty
		current.keys.asArray.sort.reverseDo{|slot|
			if(target[slot].isNil, {
				az.removeFilter(type, num, slot, { cond.test = true; cond.signal });
				wait.();
			});
		};
		// 2. the preset's slots, in order
		target.keys.asArray.sort.do{|slot|
			var unit = target[slot], cur = current[slot], tag = this.chainTag(type, num, slot), pairs = [];
			if(cur.isNil or: { cur[\def] != unit[\def] }, {
				//the preset's plain values, in the effect's own order; the slot
				//is filled as fx fills it (insert when it holds another effect)
				this.prArgOrder(unit[\def], unit[\args] ? ()).do{|key|
					var value = unit[\args][key];
					if(value.isKindOf(Dictionary).not, { pairs = pairs ++ [key, value] }) };
				az.filter(type, num, slot, unit[\def], pairs, unit[\buffer], this.dataArg(unit[\data]),
					{ cond.test = true; cond.signal }, az.findFilterTag(type, num, slot).notNil);
				wait.();
			});
			this.apply(tag, unit, az);
			if((unit[\bypass] == true) != az.isBypassed(tag), {
				az.bypassFx(type, num, slot, (unit[\bypass] == true).binaryValue, { cond.test = true; cond.signal });
				wait.();
			});
			Server.default.sync;
			{ az.updateFxWin(tag) }.defer;
		};
		//a preset with its own morph time gives it to the strip (as fxload does)
		chain[\morphTime] !? {|time| az.setChainMorphTime(this.stripName(type, num), time) };
		{ az.refreshFunc }.defer;
	}

	// csmorph: morph the strip's chain to a preset (Assemblage:morphChain).
	// Returns false when there is no such preset.
	*morphChainPreset {|type, num, name, time, shape, action, az|
		var chain;
		az = az ? Radicles.aZ;
		if(az.isNil, { "could not find assemblage".warn; ^false });
		if(az.getThisTrack(type, num).isNil, { ("No " ++ this.stripName(type, num)).warn; ^false });
		if(this.chainPresetExists(name).not, {
			("No chain preset " ++ name ++ (this.isOldChainPreset(name).if(
				{ " in the new format (csmorph uses presets saved with cssave since Nov 2026)" }, { "" }))).warn;
			^false });
		chain = this.readChainPreset(name);
		if(chain.isNil, { ^false });
		az.morphChain(type, num, chain, time, shape, action);
		^true
	}
}
