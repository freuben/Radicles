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

+ RadUnit {

	// the keys of a strip unit that are values (refreshed when read), and
	// those that are its structure (kept up to date by syncStrips)
	*stripValueKeys { ^[\trim, \volume, \mute, \pan, \lags, \mods, \solo, \output, \chainMorphTime] }
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
		//the chain's own morph time (csmorphtime, step 3 PR 3)
		if(az.respondsTo(\chainMorphTimeOf), { az.chainMorphTimeOf(name) !? {|time| unit[\chainMorphTime] = time } });
		if(name != \master, {
			index = (az.mixTrackNames ? []).indexOf(name);
			unit[\solo] = index !? {|i| (az.soloStates ? [])[i] } ? 0;
			out = index !? {|i| (az.outputSettings ? [])[i] };
			unit[\output] = if(out == \master, { \master }, { \none });
		});
		^unit
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
