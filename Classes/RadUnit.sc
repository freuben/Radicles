// Radicles: units (Document Model, step 1). A unit's state is an Event:
//   (kind: \filter, def: \pch, args: (...), mods: (...), data: ..., buffer: ...)
// RadUnit reads effects from the running system, keeps the session's
// document (RadDoc.current) in step with them, and builds a document from
// scratch for the consistency check in the tests.
// In step 1 the document describes the effects; it does not own them.

RadUnit {

	// a value as plain data: numbers, Symbols, Strings, Booleans, nil and
	// Arrays of them stay as they are; a Function is kept as its code, so a
	// random value stays random; anything else is kept as its code too
	*plain {|value|
		^case
		{ value.isNil or: { value.isNumber } or: { value.isKindOf(Symbol) }
			or: { value.isString } or: { value == true } or: { value == false } } { value }
		{ value.isKindOf(Function) } { (code: value.cs) }
		{ value.isKindOf(SequenceableCollection) } { value.asArray.collect{|v| this.plain(v) } }
		{ (code: value.cs) }
	}

	// the document address of an effect tag: filterTrack_1_2 -> [\strips, \track1, \fx, 2]
	*address {|tag|
		var parts = tag.asString.split($_), type = parts[0].replace("filter", "").toLower;
		var strip = if(type == "master", { \master }, { (type ++ parts[1]).asSymbol });
		^[\strips, strip, \fx, parts[2].asInteger]
	}

	// the unit of the effect with this tag, read from the running system, or nil
	*read {|tag, az|
		var entry, name, func, argNames, unit, argsMods, extra;
		az = az ? Radicles.aZ;
		if(az.isNil or: { az.filters.isNil }, { ^nil });
		entry = az.filters.detect{|item| item[0] == tag };
		if(entry.isNil, { ^nil });
		name = entry[1];
		func = SynthFile.read(\filter, name);
		argNames = if(func.respondsTo(\argNames), { func.argNames.asArray }, { [] });
		argsMods = this.readArgs(Ndef(tag), argNames);
		unit = (kind: \filter, def: name, args: argsMods[0]);
		if(argsMods[1].notEmpty, { unit[\mods] = argsMods[1] });
		extra = (az.trackDataArr ? []).detect{|item| item[0] == tag };
		if(extra.notNil and: { extra[1].notNil }, { unit[\data] = this.plain(extra[1]) });
		extra = (az.trackBufferArr ? []).detect{|item| item[0] == tag };
		if(extra.notNil and: { extra[1].notNil }, { unit[\buffer] = this.plain(extra[1]) });
		^unit
	}

	// [args, mods] of an Ndef: only the arguments in argNames (all if nil);
	// a modulated argument keeps the value it returns to in args, and its
	// modulator, described by what it is, in mods
	*readArgs {|ndef, argNames|
		var args = (), mods = (), entry;
		ndef.getKeysValues.do{|pair|
			var key = pair[0], value = pair[1];
			if(argNames.isNil or: { argNames.includes(key) }, {
				if(value.isKindOf(NodeProxy), {
					entry = (ModMap.modNodes ? []).detect{|item|
						(item[0] == value) and: { item[1] == ndef } and: { item[2] == key } };
					if(entry.notNil, { args[key] = this.plain(entry[3]) });
					mods[key] = this.readMod(value);
				}, {
					args[key] = this.plain(value);
				});
			});
		};
		^[args, mods]
	}

	// a modulator by what it is (never by its number, which can change):
	// type, spec (with mul, add, min, val and warp already applied), its
	// function and lag if any, its own argument values, and its own modulators
	*readMod {|modNdef|
		var index, info, desc, argsMods;
		index = (ModMap.modNodes ? []).detectIndex{|item| item[0] == modNdef };
		if(index.isNil, { ^(code: modNdef.source.cs) });
		info = ModMap.modInfoArr[index];
		desc = (type: info[1], spec: this.plain(info[2]));
		if(info[4].notNil, { desc[\func] = this.plain(info[4]) });
		if(info[10].notNil, { desc[\lag] = this.plain(info[10]) });
		argsMods = this.readArgs(modNdef, nil);
		desc[\args] = argsMods[0];
		if(argsMods[1].notEmpty, { desc[\mods] = argsMods[1] });
		^desc
	}

	// a new document holding every effect of the running system
	*fromSystem {|az|
		var doc = RadDoc.new;
		az = az ? Radicles.aZ;
		if(az.notNil, {
			(az.filters ? []).do{|item|
				var unit = this.read(item[0], az);
				if(unit.notNil, { doc.put(this.address(item[0]), unit) });
			};
		});
		^doc
	}

	// update the values (arguments and modulators' arguments) of the effects
	// in a document from the running system; which effects and modulators
	// exist is left as it is (that is kept up to date by the hooks)
	*refresh {|doc, az|
		doc = doc ? RadDoc.current;
		az = az ? Radicles.aZ;
		if(az.isNil, { ^doc });
		(az.filters ? []).do{|item|
			var address = this.address(item[0]), unit = doc.at(address), fresh;
			if(unit.notNil and: { unit[\def] == item[1] }, {
				fresh = this.read(item[0], az);
				if(fresh.notNil, { this.prRefreshValues(unit, fresh) });
			});
		};
		^doc
	}

	*prRefreshValues {|unit, fresh|
		unit[\args] = fresh[\args];
		(unit[\mods] ? ()).keysValuesDo{|key, mod|
			var freshMod = (fresh[\mods] ? ())[key];
			if(freshMod.notNil and: { mod.isKindOf(Dictionary) } and: { freshMod[\args].notNil }, {
				this.prRefreshValues(mod, freshMod) });
		};
	}

	// the differences between the session's document (values refreshed) and
	// one built from scratch: empty when every change reached the document
	*check {|doc, az|
		doc = (doc ? RadDoc.current).copy;
		^RadDoc.diff(this.refresh(doc, az), this.fromSystem(az))
	}

	// ---- hooks: keep RadDoc.current in step with the effects ----

	// one effect: put its unit at its address, or remove it if it is gone
	*sync {|tag, az|
		var doc = RadDoc.current, address = this.address(tag), unit = this.read(tag, az);
		if(unit.isNil, {
			if(doc.at(address).notNil, { doc.removeAt(address, true) });
		}, {
			if(RadDoc.diff(doc.at(address) ? (), unit).notEmpty, { doc.put(address, unit) });
		});
	}

	// every effect: add, change or remove units so the document matches
	*syncAll {|az|
		var doc = RadDoc.current, tags;
		az = az ? Radicles.aZ;
		tags = if(az.isNil, { [] }, { (az.filters ? []).collect(_[0]) });
		(doc.at(\strips) ? ()).keys.asArray.do{|strip|
			(doc.at([\strips, strip, \fx]) ? ()).keys.asArray.do{|slot|
				var address = [\strips, strip, \fx, slot];
				if(tags.any{|tag| this.address(tag) == address }.not, { doc.removeAt(address, true) });
			};
		};
		tags.do{|tag| this.sync(tag, az) };
	}

	// after a modulator was added or removed on an Ndef: if the chain it
	// belongs to ends at an effect, update that effect's unit
	*syncModTarget {|ndef|
		var target = ndef, entry, guard = 0;
		while({
			entry = (ModMap.modNodes ? []).detect{|item| item[0] == target };
			entry.notNil and: { guard < 50 }
		}, { target = entry[1]; guard = guard + 1 });
		if(target.respondsTo(\key) and: { target.key.asString.beginsWith("filter") }, {
			this.sync(target.key);
		});
	}
}
