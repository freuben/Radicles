// Radicles: the current state as Radicles commands (prototype, Sessions and
// Scenes proposal, phase 1). Read-only: `state` posts the commands that would
// rebuild the current session; nothing is saved or changed.
// Covered so far: layout, mixer levels, effects and their settings, blocks and
// their settings, tempo. Not yet: sends, modulation (posted as comments),
// added stereo tracks' channels, buffers' details, controllers, own code.

+ Radicles {

	*stateCommands {
		var cmds = [], az = aZ;
		var fmt = {|v| if(v.isFloat, { v.round(0.0001) }, { v }).asString };
		// arguments whose value differs from the Ndef's own default
		var changed = {|key|
			var obj = Ndef(key).objects.first, defaults = (), pairs = [];
			if(obj.notNil and: { obj.respondsTo(\controlNames) }, {
				obj.controlNames.do{|cn| defaults[cn.name] = cn.defaultValue };
			});
			Ndef(key).controlKeysValues.pairsDo{|k, v|
				if(defaults[k].isNil or: { v != defaults[k] }, { pairs = pairs.add([k, v]) });
			};
			pairs
		};
		var typeLetter = (track: "t", bus: "b", master: "m");
		if(az.isNil, { ^["// no session running (start a mixer with start)"] });

		// layout
		cmds = cmds.add("start " ++ az.getTracks.size ++ " " ++ az.getBuses.size);
		az.mixTrackNdefs.do{|n|
			if(n.numChannels != 1 and: { n.key != \master }, {
				cmds = cmds.add("// " ++ n.key ++ " has " ++ n.numChannels ++ " channels (not yet)");
			});
		};
		if(bpm.notNil, { cmds = cmds ++ ["clock", "bpm " ++ fmt.(bpm)] });

		// mixer: volume, pan, trim, mute, solo
		az.mixTrackNames.do{|name, index|
			var info = name.asString.divNumStr, type = info[0].asSymbol, num = info[1];
			var ref = typeLetter[type] ++ if(type == \master, { "" }, { " " ++ num });
			var space = ("space" ++ name.asString.firstToUpper).asSymbol;
			var input = ("in" ++ name.asString.firstToUpper.replace(num.asString, ""))
				.asSymbol;
			var vol = Ndef(name).get(\volume), pan, trim;
			input = if(type == \master, { \inMaster }, { (input ++ num).asSymbol });
			pan = Ndef(space).get(\pan);
			trim = Ndef(input).get(\trim);
			case
			{ vol.isKindOf(NodeProxy) } { cmds = cmds.add("// vol " ++ ref ++ " is modulated (not yet)") }
			{ vol.notNil and: { vol != 0 } } { cmds = cmds.add("vol " ++ ref ++ " " ++ fmt.(vol)) };
			if(pan.isNumber and: { pan != 0 }, { cmds = cmds.add("pan " ++ ref ++ " " ++ fmt.(pan)) });
			if(trim.isNumber and: { trim != 0 }, { cmds = cmds.add("trim " ++ ref ++ " " ++ fmt.(trim)) });
			if(az.muteStates.notNil and: { az.muteStates[index] == 1 }, { cmds = cmds.add("mute " ++ ref) });
			if(az.soloStates.notNil and: { az.soloStates[index] == 1 }, { cmds = cmds.add("solo " ++ ref) });
		};
		cmds = cmds.add("// sends: not yet");

		// effects, in slot order, with the settings that differ from defaults
		(az.filters ? []).do{|item|
			var tag = item[0], name = item[1], info = az.convFilterTag(tag);
			var ref = typeLetter[info[0]] ++ if(info[0] == \master, { "" }, { " " ++ info[1] });
			cmds = cmds.add("fx " ++ ref ++ " " ++ info[2] ++ " " ++ name);
			changed.(tag).do{|kv|
				if(kv[1].isKindOf(NodeProxy), {
					cmds = cmds.add("// fx " ++ ref ++ " " ++ info[2] ++ " " ++ kv[0] ++ " is modulated (not yet)");
				}, {
					cmds = cmds.add("fxset " ++ ref ++ " " ++ info[2] ++ " " ++ kv[0] ++ " " ++ fmt.(kv[1]));
				});
			};
		};

		// blocks and their settings
		(Block.liveBlocks ? []).do{|item, index|
			var key, settings, plain, modded;
			if(item.notNil, {
				key = item[0];
				cmds = cmds.add("blk " ++ (index + 1) ++ " " ++ item[1] ++
					if(item[2].notNil, { " // buffer " ++ item[2].cs }, { "" }));
				settings = changed.(key);
				plain = settings.reject{|kv| kv[1].isKindOf(NodeProxy) };
				modded = settings.select{|kv| kv[1].isKindOf(NodeProxy) };
				if(plain.notEmpty, {
					cmds = cmds.add("blksetn " ++ (index + 1) ++ " " ++
						plain.collect{|kv| kv[0] ++ ", " ++ fmt.(kv[1]) }.join(", ")
						.addFirst($[).add($]));
				});
				modded.do{|kv|
					cmds = cmds.add("// blk " ++ (index + 1) ++ " " ++ kv[0] ++ " is modulated (not yet)");
				};
			});
		};
		^cmds
	}

	*prStateCmds {
		cW.add(\state, [\str], {|str1|
			Radicles.stateCommands.do{|line| line.postln };
		}, "state: post the current session as commands (prototype, nothing is saved)");
	}
}
