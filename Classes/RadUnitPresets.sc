// Radicles: unit presets and applying a unit to a slot (Document Model,
// step 1, PR 3). A preset belongs to its unit's definition, like a plugin's
// presets: one readable file per preset in
//   <RadiclesFiles>/Settings/Presets/units/<kind>/<def>/<name>.scd
// Files are read with RadLiteral, so a preset is only ever data. A function
// the performer saved (a modulator's) is kept as code and runs when the
// preset is applied, posted like any other code.

+ RadUnit {

	*presetRoot { ^Radicles.filesPath +/+ "Settings" +/+ "Presets" +/+ "units" }

	*presetDir {|kind, def| ^this.presetRoot +/+ kind.asString +/+ def.asString }

	// a preset name is a file name: letters, digits, - and _ only
	*validPresetName {|name|
		name = name.asString;
		^name.size > 0 and: { name.every{|c| c.isAlphaNum or: { c == $- } or: { c == $_ } } }
	}

	*presetPath {|kind, def, name| ^this.presetDir(kind, def) +/+ name.asString ++ ".scd" }

	*presetExists {|kind, def, name|
		^this.validPresetName(name) and: { File.exists(this.presetPath(kind, def, name)) }
	}

	// the names of the presets of one definition, sorted
	*presetNames {|kind, def|
		^(this.presetDir(kind, def) +/+ "*.scd").pathMatch
		.collect{|path| PathName(path).fileNameWithoutExtension.asSymbol }.sort
	}

	// every unit preset of a kind, as [def, name] pairs
	*allPresets {|kind = \filter|
		^(this.presetRoot +/+ kind.asString +/+ "*").pathMatch.collect{|dir|
			var def = PathName(dir).folderName.asSymbol;
			this.presetNames(kind, def).collect{|name| [def, name] }
		}.flatten(1)
	}

	// what a preset holds: the unit without the slot's own state (bypass,
	// position), which belongs to strip presets and sessions
	*presetUnit {|unit|
		var copy = unit.deepCopy;
		[\bypass, \position].do{|key| copy.removeAt(key) };
		^copy
	}

	// write a unit as a preset file; returns the path
	*writePreset {|unit, name|
		var path = this.presetPath(unit[\kind], unit[\def], name);
		RadDoc((format: RadDoc.formatVersion, type: \unit, unit: this.presetUnit(unit)))
		.write(path, "unit preset · " ++ unit[\kind] ++ " " ++ unit[\def]);
		^path
	}

	// the unit in a preset file, or nil with a warning
	*readPreset {|kind, def, name|
		var path = this.presetPath(kind, def, name), data, unit;
		if(this.presetExists(kind, def, name).not, {
			("No preset '" ++ name ++ "' for " ++ def).warn; ^nil });
		data = RadDoc.readData(File.readAllString(path), path);
		if(data.isNil, { ^nil });
		unit = data[\unit];
		if(data[\type] != \unit or: { unit.isKindOf(Dictionary).not }, {
			("RadUnit: " ++ path ++ " is not a unit preset").warn; ^nil });
		if(unit[\kind] != kind or: { unit[\def] != def }, {
			("RadUnit: " ++ path ++ " is a preset for " ++ unit[\kind] ++ " " ++ unit[\def]).warn; ^nil });
		^unit
	}

	// ---- applying a unit to a slot (the reconciler, first version) ----
	// Brings the effect at tag to the state described by unit, through
	// Assemblage's and ModMap's own methods, so bookkeeping, windows and
	// posted code stay as they are. Changes only what differs. Call inside a
	// Routine. Returns true when applied.
	*apply {|tag, unit, az|
		var current, info, cond, pairs;
		az = az ? Radicles.aZ;
		current = this.read(tag, az);
		if(current.isNil, { ("No effect at " ++ RadDoc.addressString(this.address(tag))).warn; ^false });
		if(current[\def] != unit[\def], {
			("This slot holds " ++ current[\def] ++ "; the preset is for " ++ unit[\def]).warn; ^false });
		// different data or buffer (the convolution reverb's impulse response):
		// insert the effect again, with the preset's values
		if(RadDoc.diff((data: current[\data], buffer: current[\buffer]),
			(data: unit[\data], buffer: unit[\buffer])).notEmpty, {
			info = az.convFilterTag(tag);
			pairs = [];
			(unit[\args] ? ()).keysValuesDo{|key, value|
				if(value.isKindOf(Dictionary).not, { pairs = pairs ++ [key, value] }) };
			cond = Condition(false);
			az.filter(info[0], info[1].asInteger, info[2].asInteger, unit[\def], pairs,
				unit[\buffer], unit[\data], { cond.test = true; cond.signal }, true);
			cond.wait;
			current = this.read(tag, az);
		});
		this.prApplyArgsMods(Ndef(tag), current, unit);
		^true
	}

	// does a modulator need to be made again (not just its values set)?
	*prModDiffers {|a, b|
		var what = {|m| (type: m[\type], spec: m[\spec], func: m[\func], lag: m[\lag]) };
		if(a.isKindOf(Dictionary).not or: { b.isKindOf(Dictionary).not }, { ^true });
		^RadDoc.diff(what.(a), what.(b), 1e-9).notEmpty
	}

	*prFunc {|value|
		^if(value.isKindOf(Dictionary) and: { value[\code].isString }, { value[\code].interpret }, { value })
	}

	*prApplyArgsMods {|ndef, current, target|
		var curMods = current[\mods] ? (), tgtMods = target[\mods] ? (), tgtArgs = target[\args] ? ();
		var fresh, freshArgs, freshMods, pairs = [];
		// 1. modulators the target does not have, or has made differently:
		// unmap, returning the argument to the target's value
		curMods.keysValuesDo{|key, mod|
			if(tgtMods[key].isNil or: { this.prModDiffers(mod, tgtMods[key]) }, {
				ModMap.unmap(ndef, key, tgtArgs[key] ? (current[\args] ? ())[key]);
			});
		};
		fresh = this.readArgs(ndef, nil);
		freshArgs = fresh[0]; freshMods = fresh[1];
		// 2. plain values that differ, in one set (a modulated argument kept
		// as it is keeps its modulator)
		tgtArgs.keysValuesDo{|key, value|
			if(freshMods[key].isNil and: { value.isKindOf(Dictionary).not } and: {
				RadDoc.diff((v: freshArgs[key]), (v: value)).notEmpty }, {
				pairs = pairs ++ [key, value];
			});
		};
		if(pairs.notEmpty, {
			("Ndef(" ++ ndef.key.cs ++ ").set(" ++ pairs.collect(_.cs).join(", ") ++ ");").radcode;
		});
		// 3. modulators: make the new ones, then set each modulator's own
		// values and its own modulators the same way
		tgtMods.keysValuesDo{|key, mod|
			var modNdef, modCurrent;
			if(mod.isKindOf(Dictionary) and: { mod[\type].notNil }, {
				if(freshMods[key].isNil, {
					modNdef = ModMap.map(ndef, key, mod[\type], mod[\spec], nil, this.prFunc(mod[\func]),
						lag: mod[\lag]);
				}, {
					modNdef = ndef.get(key);
				});
				if(modNdef.isKindOf(NodeProxy), {
					modCurrent = this.readArgs(modNdef, nil);
					this.prApplyArgsMods(modNdef, (args: modCurrent[0], mods: modCurrent[1]), mod);
				});
			});
		};
	}
}
