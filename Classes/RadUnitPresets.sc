// Radicles: unit presets and applying a unit to a slot (Document Model,
// step 1, PR 3). A preset belongs to its unit's definition, like a plugin's
// presets: one readable file per preset in
//   <RadiclesFiles>/Settings/Presets/units/<kind>/<def>/<name>.scd
// (the performer's own), and for a definition from a library (RAVE, ...)
// also in that library, so presets can be shared with it:
//   <RadiclesLibs>/<lib>/Settings/Presets/units/<kind>/<def>/<name>.scd
// fxsave writes the performer's own; fxsave <def> <name> <lib> copies one
// into the library. Reading looks in both; the performer's own wins.
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

	// where fxsave writes: the performer's own presets
	*presetPath {|kind, def, name| ^this.presetDir(kind, def) +/+ name.asString ++ ".scd" }

	// ---- libraries ----

	// the library folders in RadiclesLibs
	*libraries {
		^(Radicles.libPath !? {|path| PathName(path).folders } ? [])
		.reject{|folder| folder.folderName.beginsWith(".") }.collect(_.folderName)
	}

	// the library a definition comes from: "Main", a library's name, or nil
	*libraryOf {|kind, def|
		var file = kind.asString.firstToUpper ++ ".scd", has;
		has = {|path| File.exists(path) and: {
			(try { path.load } { nil }) !? {|arr| arr.any{|pair| pair.isArray and: { pair[0] == def.asSymbol } } } ? false } };
		if(Radicles.mainPath.notNil and: { has.(Radicles.mainPath +/+ "Files" +/+ "SynthFiles" +/+ file) }, { ^"Main" });
		^this.libraries.detect{|lib| has.(Radicles.libPath +/+ lib +/+ "Files" +/+ "SynthFiles" +/+ file) }
	}

	*libraryPresetRoot {|lib| ^Radicles.libPath +/+ lib +/+ "Settings" +/+ "Presets" +/+ "units" }

	// the presets folder of a definition in its library (nil for Main)
	*libraryPresetDir {|kind, def|
		var lib = this.libraryOf(kind, def);
		^if(lib.isNil or: { lib == "Main" }, { nil }, { this.libraryPresetRoot(lib) +/+ kind.asString +/+ def.asString })
	}

	// where a definition's presets are read from: the performer's own first
	*presetDirs {|kind, def| ^[this.presetDir(kind, def), this.libraryPresetDir(kind, def)].reject(_.isNil) }

	// the file of a preset (the performer's own if both have it), or nil
	*findPresetPath {|kind, def, name|
		if(this.validPresetName(name).not, { ^nil });
		^this.presetDirs(kind, def).collect{|dir| dir +/+ name.asString ++ ".scd" }.detect{|path| File.exists(path) }
	}

	// \own, the library's name, or nil
	*presetSource {|kind, def, name|
		var path = this.findPresetPath(kind, def, name);
		^case
		{ path.isNil } { nil }
		{ path == this.presetPath(kind, def, name) } { \own }
		{ this.libraryOf(kind, def) }
	}

	// copy the performer's own preset into the effect's library, to share it
	// with the library (fxsave <def> <name> <lib>); asks before replacing
	*publishPreset {|kind, def, name, lib, action|
		var from = this.presetPath(kind, def, name), owner = this.libraryOf(kind, def), to, copy;
		lib = lib.asString;
		case
		{ this.libraries.includesEqual(lib).not } {
			("No library " ++ lib ++ " (libraries: " ++ this.libraries.join(", ") ++ ")").warn }
		{ owner.isNil } { ("No effect " ++ def).warn }
		{ owner != lib } { (def ++ " is from " ++ owner ++ ", not " ++ lib ++
			"; its presets can only go into " ++ owner).warn }
		{ File.exists(from).not } { ("No preset " ++ name ++ " of your own for " ++ def ++
			" (save it first with fxsave t 1 1 " ++ name ++ ")").warn }
		{
			to = this.libraryPresetRoot(lib) +/+ kind.asString +/+ def.asString +/+ name.asString ++ ".scd";
			copy = {
				File.mkdir(to.dirname);
				File.use(to, "w", {|f| f.write(File.readAllString(from)) });
				("Preset " ++ name ++ " for " ++ def ++ " is now in " ++ lib ++ ": " ++ to).postln;
				action.(to);
			};
			if(File.exists(to), {
				if(GUI.current.notNil, {
					{ Window.warnQuestion(lib ++ " already has a preset " ++ name ++ " for " ++ def ++
						". Replace it?", copy) }.defer;
				}, {
					(lib ++ " already has a preset " ++ name ++ " for " ++ def ++
						"; not replaced (no window to ask in)").warn;
				});
			}, copy);
		};
	}

	// "default" is every effect's preset of its own default values (no file)
	*presetExists {|kind, def, name|
		if(name.asSymbol == \default, { ^true });
		^this.findPresetPath(kind, def, name).notNil
	}

	// the default preset: the definition's own default values, no modulators
	*defaultUnit {|kind, def|
		var func = SynthFile.read(kind, def), args = ();
		if(func.respondsTo(\argNames), {
			[func.argNames.asArray, func.defaultArgs.asArray].flop.do{|pair|
				if(pair[1].notNil, { args[pair[0]] = this.plain(pair[1]) });
			};
		});
		^(kind: kind, def: def, args: args)
	}

	// the unit a preset gives the effect at tag; for the default preset the
	// effect keeps its data and buffer (a convolution reverb's impulse response)
	*presetFor {|tag, def, name, az|
		var unit = this.readPreset(\filter, def, name), current;
		//a preset saved before Nov 2026 holds a reverb's data as a list
		if(unit.notNil and: { unit[\data].notNil }, { unit[\data] = this.dataForm(this.dataArg(unit[\data])) });
		if(unit.notNil and: { name.asSymbol == \default }, {
			current = this.read(tag, az) ? ();
			[\data, \buffer].do{|key| current[key] !? {|value| unit[key] = value } };
		});
		^unit
	}

	// the names of the presets of one definition (own and library), sorted
	*presetNames {|kind, def|
		^this.presetDirs(kind, def).collect{|dir|
			(dir +/+ "*.scd").pathMatch.collect{|path| PathName(path).fileNameWithoutExtension.asSymbol }
		}.flatten(1).asSet.asArray.sort
	}

	// every unit preset of a kind (own and in libraries), as [def, name] pairs
	*allPresets {|kind = \filter|
		var roots = [this.presetRoot] ++ this.libraries.collect{|lib| this.libraryPresetRoot(lib) };
		^roots.collect{|root|
			(root +/+ kind.asString +/+ "*").pathMatch.collect{|dir|
				var def = PathName(dir).folderName.asSymbol;
				this.presetNames(kind, def).collect{|name| [def, name] }
			}.flatten(1)
		}.flatten(1).asSet.asArray.sort{|a, b| (a[0] ++ " " ++ a[1]) < (b[0] ++ " " ++ b[1]) }
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
		var path = this.findPresetPath(kind, def, name) ? this.presetPath(kind, def, name), data, unit;
		if(name.asSymbol == \default and: { File.exists(path).not }, { ^this.defaultUnit(kind, def) });
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
	// Routine. Returns true when applied. setFunc, if given, sends the effect's
	// changed plain values instead of one posted set: {|ndef, pairs| ... }
	// (fxmorph uses it for xset, or lag then set).
	*apply {|tag, unit, az, setFunc|
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
				unit[\buffer], this.dataArg(unit[\data]), { cond.test = true; cond.signal }, true);
			cond.wait;
			current = this.read(tag, az);
		});
		this.prApplyArgsMods(Ndef(tag), current, unit, setFunc);
		//a preset with its own morph time gives it to the slot; one without
		//leaves the slot's as it is
		if(unit[\morphTime].notNil and: { unit[\morphTime] != current[\morphTime] }, {
			az.setMorphTime(tag, unit[\morphTime]);
		});
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

	*prApplyArgsMods {|ndef, current, target, setFunc|
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
			if(setFunc.notNil, { setFunc.(ndef, pairs) }, {
				("Ndef(" ++ ndef.key.cs ++ ").set(" ++ pairs.collect(_.cs).join(", ") ++ ");").radcode;
			});
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
