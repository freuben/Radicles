// Radicles: strip values in the document (Document Model, step 3, PR 1).
// Each strip of the mixer (track1, bus2, master) has, at its own address,
// its values beside the effects it already holds (step 1):
//   track1: (kind: \strip, channels: 1, space: \pan2,
//     trim: 0, volume: -6, mute: 0, pan: (pan: -0.3),
//     lags: (volume: 2), mods: (volume: (type: \sin, ...)),
//     solo: 0, output: \master, fx: (1: ..., 2: ...))
// As for effects, values are read from the running system when the document
// is read (RadUnit.refresh), so faders, knobs, controllers and IDE lines need
// no hook. Hooks only follow which strips exist (syncStrips). channels and
// space are fixed when a strip is made (until #44). The master has no solo
// and no output. Lists Assemblage keeps by position (soloStates,
// outputSettings) are read through the strip's name.
// Step 4 (PR 1) adds what a track preset needs beside these: input (what
// feeds a track, by Ndef key: [\block1]) and sends (by bus, never by send
// slot: (bus1: (level: -12, lag: 0.5, mod: (...)))), read the same way.

+ RadUnit {

	// the keys of a strip unit that are values (refreshed when read), and
	// those that are its structure (kept up to date by syncStrips)
	*stripValueKeys { ^[\trim, \volume, \mute, \pan, \lags, \mods, \solo, \output, \input, \sends] }
	*stripStructureKeys { ^[\kind, \channels, \space] }

	// the strips of the running mixer, by name
	*stripNames {|az|
		az = az ? Radicles.aZ;
		^if(az.isNil, { [] }, { (az.trackNames ? []).copy })
	}

	// a strip's Ndef keys: [inTrack1, track1, spaceTrack1]
	*stripKeys {|name|
		var cap = name.asString.capitalise;
		^[("in" ++ cap).asSymbol, name.asSymbol, ("space" ++ cap).asSymbol]
	}

	// the unit of a strip (without its effects), read from the running
	// system, or nil if the mixer has no such strip
	*readStrip {|name, az|
		var inKey, key, spaceKey, trim, level, pan, unit, lags = (), mods = (), index, out;
		az = az ? Radicles.aZ;
		name = name.asSymbol;
		if(this.stripNames(az).includes(name).not, { ^nil });
		#inKey, key, spaceKey = this.stripKeys(name);
		trim = this.readArgs(Ndef(inKey), [\trim, \lagTime]);
		level = this.readArgs(Ndef(key), [\volume, \lagTime, \mute]);
		//the panner's own arguments; wet* are the master's limiter slots
		pan = this.readArgs(Ndef(spaceKey), nil);
		pan = pan.collect{|dict| dict.reject{|v, k| k.asString.beginsWith("wet") } };
		unit = (kind: \strip, channels: Ndef(inKey).numChannels,
			space: (az.space ? []).detect{|item| item[0] == spaceKey } !? {|item| item[1] },
			trim: trim[0][\trim] ? 0, volume: level[0][\volume] ? 0, mute: level[0][\mute] ? 0,
			pan: pan[0]);
		//vollag sets the volume's lagTime; trimlag and panlag set the Ndefs'
		//own lags (NodeProxy:lag)
		if((level[0][\lagTime] ? 0) != 0, { lags[\volume] = level[0][\lagTime] });
		this.prNdefLag(Ndef(inKey), \trim) !? {|lag| lags[\trim] = lag };
		if(lags[\trim].isNil and: { (trim[0][\lagTime] ? 0) != 0 }, { lags[\trim] = trim[0][\lagTime] });
		pan[0].keys.do{|key|
			this.prNdefLag(Ndef(spaceKey), key) !? {|lag|
				lags[\pan] = (lags[\pan] ? ()).put(key, lag) } };
		if(lags.notEmpty, { unit[\lags] = lags });
		trim[1][\trim] !? {|mod| mods[\trim] = mod };
		level[1][\volume] !? {|mod| mods[\volume] = mod };
		if(pan[1].notEmpty, { mods[\pan] = pan[1] });
		if(mods.notEmpty, { unit[\mods] = mods });
		//what feeds a track, and where every strip but the master sends (step 4)
		this.readInput(name, az) !? {|input| unit[\input] = input };
		this.readSends(name, az) !? {|sends| unit[\sends] = sends };
		if(name != \master, {
			index = (az.mixTrackNames ? []).indexOf(name);
			unit[\solo] = index !? {|i| (az.soloStates ? [])[i] } ? 0;
			out = index !? {|i| (az.outputSettings ? [])[i] };
			unit[\output] = if(out == \master, { \master }, { \none });
		});
		^unit
	}

	// what feeds a track: the Ndef keys of its input ([\block1], or several
	// in input order), or nil when nothing does. Buses (fed by sends) and the
	// master have no input of their own here.
	*readInput {|name, az|
		var tag, entry, keys;
		az = az ? Radicles.aZ;
		if(name.asString.beginsWith("track").not, { ^nil });
		tag = this.stripKeys(name)[0];
		entry = (az.inputs ? []).detect{|item| item[0] == tag };
		if(entry.isNil or: { entry[1].isNil }, { ^nil });
		keys = entry[1].asArray.select{|item| item.isKindOf(NodeProxy) }.collect(_.key);
		^if(keys.isEmpty, { nil }, { keys })
	}

	// where a strip sends: (busN: (level: dB, lag: s, mod: (...))), keyed by
	// bus, since send slots follow bus order and differ between strips; nil
	// when it sends nowhere. Read from the bus Ndefs (volK, lagK), so send
	// knobs, MIDI and IDE lines need no hook.
	*readSends {|name, az|
		var sends = (), ndef;
		az = az ? Radicles.aZ;
		if(name.asSymbol == \master, { ^nil });
		ndef = Ndef(name.asSymbol);
		(az.busArr ? []).do{|item|
			var busTag = item[0], members = item[1], index, volKey, lagKey, read, send;
			if(busTag.notNil and: { members.notNil }, {
				index = members.indexOf(ndef);
				if(index.notNil, {
					volKey = ("vol" ++ (index + 1)).asSymbol;
					lagKey = ("lag" ++ (index + 1)).asSymbol;
					read = this.readArgs(Ndef(busTag), [volKey, lagKey]);
					send = (level: read[0][volKey] ? -inf);
					if((read[0][lagKey] ? 0) != 0, { send[\lag] = read[0][lagKey] });
					read[1][volKey] !? {|mod| send[\mod] = mod };
					sends[("bus" ++ busTag.asString.divNumStr[1]).asSymbol] = send;
				});
			});
		};
		^if(sends.isEmpty, { nil }, { sends })
	}

	// the lag set on an Ndef's argument with NodeProxy:lag, or nil when none
	*prNdefLag {|ndef, key|
		var lag = ndef.nodeMap !? {|map| map.ratesFor([key]) } !? {|rates| rates[0] };
		^if(lag.isNumber and: { lag != 0 }, { lag }, { nil })
	}

	// copy a fresh strip's values into a document's strip unit (in place)
	*prRefreshStrip {|unit, fresh|
		this.stripValueKeys.do{|key|
			if(fresh[key].notNil, { unit[key] = fresh[key] }, { unit.removeAt(key) });
		};
	}

	// every strip: add or remove strip units, or replace one whose structure
	// changed, so the document has the strips of the running mixer; a strip's
	// effects are kept
	*syncStrips {|az|
		var doc = RadDoc.current, names = this.stripNames(az);
		(doc.at(\strips) ? ()).keys.asArray.do{|name|
			if(names.includes(name).not, { doc.removeAt([\strips, name], true) });
		};
		names.do{|name|
			var old = doc.at([\strips, name]), fresh = this.readStrip(name, az);
			if(fresh.notNil and: {
				old.isNil or: { this.stripStructureKeys.any{|key| old[key] != fresh[key] } } }, {
				old !? { old[\fx] } !? {|fx| fresh[\fx] = fx };
				doc.put([\strips, name], fresh);
			});
		};
	}
}
