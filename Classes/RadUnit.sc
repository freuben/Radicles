// Radicles: units (Document Model, steps 1 and 2). A unit's state is an Event:
//   (kind: \filter, def: \pch, args: (...), mods: (...), data: ..., buffer: ...)
//   (kind: \block, def: \loop, args: (...), buffer: (play: 'drums1', folder: 0))
// RadUnit reads effects (strips.track1.fx.2) and blocks (blocks.block1) from
// the running system, keeps the session's document (RadDoc.current) in step
// with them, and builds a document from scratch for the consistency check in
// the tests. The document describes effects and blocks; it does not own them.
// Patterns (blk 1 pattern ...) are not in the document yet.

RadUnit {
	// the global morph time in seconds (morphtime 5), used when neither the
	// command nor the unit gives one (Document Model, step 1, PR 5)
	classvar <>morphTime = 2;

	// running block morphs: tag -> (routine:, restore:) (step 2, PR 4)
	classvar blockMorphs;

	// seconds from asking where a block plays to its new copy starting
	// (blkmorph), on top of the server's latency (Ndefs send their messages
	// that much ahead); measured in the tests
	classvar <>placeLead = -0.025;

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

	// an effect's data as the unit keeps it. A convolution reverb's impulse
	// response by its name in the IR folder (never a path or buffer number):
	//   [\convrev, \ortf_s1r1, 2048] -> (ir: \ortf_s1r1, fftsize: 2048)
	// other data as plain data
	*dataForm {|data|
		var form;
		if(data.isArray and: { data[0] == \convrev }, {
			form = (ir: data[1]);
			data[2] !? {|size| form[\fftsize] = size };
			data[3] !? {|input| form[\input] = this.plain(input) };
			^form
		});
		^this.plain(data)
	}

	// what the effect takes from a unit's data (also the list presets saved
	// before Nov 2026 hold): (ir: name, fftsize: n) -> [\convrev, name, n]
	*dataArg {|data|
		if(data.isKindOf(Dictionary) and: { data[\ir].notNil }, {
			^[\convrev, data[\ir], data[\fftsize] ? 2048] ++ (data[\input] !? {|input| [input] } ? [])
		});
		^data
	}

	// a block's Ndef key: block1, block12 (not recblock1)
	*isBlockTag {|tag| ^"^block[0-9]+$".matchRegexp(tag.asString) }

	// the document address of an effect or block tag:
	// filterTrack_1_2 -> [\strips, \track1, \fx, 2], block1 -> [\blocks, \block1]
	*address {|tag|
		var parts, type, strip;
		if(this.isBlockTag(tag), { ^[\blocks, tag.asSymbol] });
		parts = tag.asString.split($_);
		type = parts[0].replace("filter", "").toLower;
		strip = if(type == "master", { \master }, { (type ++ parts[1]).asSymbol });
		^[\strips, strip, \fx, parts[2].asInteger]
	}

	// the unit of the effect or block with this tag, read from the running
	// system, or nil
	*read {|tag, az|
		var entry, name, func, argNames, unit, argsMods, extra;
		if(this.isBlockTag(tag), { ^this.readBlock(tag) });
		az = az ? Radicles.aZ;
		if(az.isNil or: { az.filters.isNil }, { ^nil });
		entry = az.filters.detect{|item| item[0] == tag };
		if(entry.isNil, { ^nil });
		name = entry[1];
		//the effect's function as its strip holds it (no file read on every
		//change); the library's if the strip does not have it
		(az.tracks ? []).do{|strip| strip.do{|stage|
			if(func.isNil and: { stage.isArray } and: { stage[0] == tag }, {
				func = if(stage[1].isArray, { stage[1][0] }, { stage[1] }) }) } };
		if(func.respondsTo(\argNames).not, { func = SynthFile.read(\filter, name) });
		argNames = if(func.respondsTo(\argNames), { func.argNames.asArray }, { [] });
		argsMods = this.readArgs(Ndef(tag), argNames);
		unit = (kind: \filter, def: name, args: argsMods[0]);
		if(argsMods[1].notEmpty, { unit[\mods] = argsMods[1] });
		extra = (az.trackDataArr ? []).detect{|item| item[0] == tag };
		if(extra.notNil and: { extra[1].notNil }, { unit[\data] = this.dataForm(extra[1]) });
		extra = (az.trackBufferArr ? []).detect{|item| item[0] == tag };
		if(extra.notNil and: { extra[1].notNil }, { unit[\buffer] = this.plain(extra[1]) });
		if(az.isBypassed(tag), { unit[\bypass] = true });
		(az.morphTimes !? {|m| m[tag] }) !? {|time| unit[\morphTime] = time };
		^unit
	}

	// the unit of a playing block (Document Model, step 2), or nil if the block
	// is stopped, plays a pattern or does not exist. Argument names come from
	// the block's function as its Ndef holds it, values from the Ndef, the
	// block's name, data and buffer from what Block.play recorded
	*readBlock {|tag|
		var entry, func, argNames, argsMods, unit, buffer;
		tag = tag.asSymbol;
		entry = (Block.liveBlocks ? []).detect{|item| item.notNil and: { item[0] == tag } };
		if(entry.isNil or: { entry[1] == \pattern }, { ^nil });
		func = Ndef(tag).source;
		if(func.isKindOf(Function).not, { ^nil });
		argNames = func.argNames.asArray;
		argsMods = this.readArgs(Ndef(tag), argNames);
		unit = (kind: \block, def: entry[1], args: argsMods[0]);
		if(argsMods[1].notEmpty, { unit[\mods] = argsMods[1] });
		if(entry[3].notNil, { unit[\data] = this.plain(entry[3]) });
		buffer = Block.bufferFormOf(tag);
		if(buffer.notNil, { unit[\buffer] = buffer });
		^unit
	}

	// the tags of the blocks that are playing a block (not a pattern)
	*blockTags {
		^(Block.liveBlocks ? []).select{|item| item.notNil and: { item[1] != \pattern } }
		.collect(_[0]).select{|tag| Ndef(tag).source.isKindOf(Function) }
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

	// a new document holding every effect and block of the running system
	*fromSystem {|az|
		var doc = RadDoc.new;
		az = az ? Radicles.aZ;
		if(az.notNil, {
			(az.filters ? []).do{|item|
				var unit = this.read(item[0], az);
				if(unit.notNil, { doc.put(this.address(item[0]), unit) });
			};
		});
		this.blockTags.do{|tag|
			var unit = this.readBlock(tag);
			if(unit.notNil, { doc.put(this.address(tag), unit) });
		};
		^doc
	}

	// update the values (arguments and modulators' arguments) of the effects
	// and blocks in a document from the running system; which effects, blocks
	// and modulators exist is left as it is (that is kept up to date by the hooks)
	*refresh {|doc, az|
		doc = doc ? RadDoc.current;
		az = az ? Radicles.aZ;
		(doc.at(\blocks) ? ()).keysValuesDo{|tag, unit|
			var fresh = this.readBlock(tag);
			if(fresh.notNil and: { unit.isKindOf(Dictionary) } and: { unit[\def] == fresh[\def] }, {
				this.prRefreshValues(unit, fresh) });
		};
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

	// ---- hooks: keep RadDoc.current in step with the effects and blocks ----

	// one effect or block: put its unit at its address, or remove it if it is gone
	*sync {|tag, az|
		var doc = RadDoc.current, address = this.address(tag), unit = this.read(tag, az);
		if(unit.isNil, {
			if(doc.at(address).notNil, { this.prRemove(doc, address) });
		}, {
			if(RadDoc.diff(doc.at(address) ? (), unit).notEmpty, { doc.put(address, unit) });
		});
	}

	// remove a unit; for blocks also the blocks Event when it is left empty
	// (a document without blocks has none, like a new one)
	*prRemove {|doc, address|
		doc.removeAt(address, true);
		if(address[0] == \blocks and: { (doc.at(\blocks) ? ()).isEmpty }, { doc.removeAt(\blocks) });
	}

	// every block: add, change or remove units so the document matches
	*syncBlocks {
		var doc = RadDoc.current, tags = this.blockTags;
		(doc.at(\blocks) ? ()).keys.asArray.do{|tag|
			if(tags.includes(tag).not, { this.prRemove(doc, [\blocks, tag]) });
		};
		tags.do{|tag| this.sync(tag) };
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
	// belongs to ends at an effect or a block, update that unit
	*syncModTarget {|ndef|
		var target = ndef, entry, guard = 0;
		while({
			entry = (ModMap.modNodes ? []).detect{|item| item[0] == target };
			entry.notNil and: { guard < 50 }
		}, { target = entry[1]; guard = guard + 1 });
		if(target.respondsTo(\key) and: {
			target.key.asString.beginsWith("filter") or: { this.isBlockTag(target.key) } }, {
			this.sync(target.key);
		});
	}
}
