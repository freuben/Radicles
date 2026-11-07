// Effects in the document (Document Model, step 1, PR 2): RadUnit reads an
// effect's state from the running system, and hooks in Assemblage and ModMap
// keep RadDoc.current in step. Every test in the suite also runs the
// consistency check after each command (Tests/child.scd).

// the document's unit at an address, values refreshed from the running system
~radUnitAt = {|address| RadUnit.refresh(RadDoc.current.copy).at(address) };

~radTest.("unit-effect",
	setup: ["start 2 1"],
	cmds: ["fx t 1 pch", "fxset t 1 1 pitchRatio 0.5"],
	check: {
		var unit = ~radUnitAt.("track1.fx.1");
		(unit == (kind: \filter, def: \pch,
			args: (pitchRatio: 0.5, pitchDispertion: 0.0, timeDispersion: 0.0001, winSize: 0.02)))
		or: { "unit: " ++ unit.cs }
	}
);

// two slots on a track, a bus and the master, each at its own address
~radTest.("unit-addresses",
	setup: ["start 2 1"],
	cmds: ["fx t 1 pch", "fx t 1 2 dl1", "fx b 1 lpf", "fx m hpf"],
	check: {
		var fx = { |strip| (RadDoc.current.at([strip, \fx]) ? ()).collect(_[\def]) };
		var got = (track1: fx.(\track1), bus1: fx.(\bus1), master: fx.(\master));
		(got == (track1: (1: \pch, 2: \dl1), bus1: (1: \lpf), master: (1: \hpf)))
		or: { "effects: " ++ got.cs }
	}
);

// a slot that held another effect keeps only the new effect's arguments
~radTest.("unit-replaced-effect",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5"],
	cmds: ["fx t 1 1 dl1"],
	check: {
		var unit = ~radUnitAt.("track1.fx.1");
		(unit[\def] == \dl1 and: { unit[\args].keys == Set[\delayTime, \decayTime] })
		or: { "unit: " ++ unit.cs }
	}
);

// a modulated argument: its modulator by what it is, and the value it returns to
~radTest.("unit-modulated",
	setup: ["start 2", "fx t 1 pch"],
	cmds: ["fxset t 1 1 pitchDispertion sin"],
	check: {
		var unit = ~radUnitAt.("track1.fx.1"), mod = unit !? { unit[\mods] } !? { |m| m[\pitchDispertion] };
		case
		{ mod.isNil } { "no modulator: " ++ unit.cs }
		{ unit[\args][\pitchDispertion] != 0.0 } { "value to return to: " ++ unit[\args][\pitchDispertion].cs }
		{ mod[\type] != \sin } { "type: " ++ mod.cs }
		{ mod[\args][\freq].isNumber.not } { "modulator arguments: " ++ mod.cs }
		{ mod.keys.includes(\key) or: { mod.cs.find("mod1").notNil } } { "stored by number: " ++ mod.cs }
		{ true }
	}
);

// a modulator's own settings, and a modulated modulator, nest in the unit
~radTest.("unit-modulated-modulator",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchDispertion sin"],
	code: {
		Ndef(\mod1).set(\freq, 3);
		ModMap.map(Ndef(\mod1), \phase, \sin, [0, 1]);
	},
	check: {
		var mod = ~radUnitAt.("track1.fx.1")[\mods][\pitchDispertion];
		case
		{ mod[\args][\freq] != 3 } { "modulator setting: " ++ mod[\args].cs }
		{ mod[\mods].isNil or: { mod[\mods][\phase].isNil } } { "nested modulator missing: " ++ mod.cs }
		{ mod[\mods][\phase][\type] != \sin } { "nested type: " ++ mod[\mods].cs }
		{ true }
	}
);

~radTest.("unit-unmodulated",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchDispertion sin"],
	cmds: ["unmodfx t 1 1 pitchDispertion"],
	check: {
		var unit = ~radUnitAt.("track1.fx.1");
		(unit[\mods].isNil and: { unit[\args][\pitchDispertion] == 0.0 }) or: { "unit: " ++ unit.cs }
	}
);

// removing effects removes their units, and the fx of strips left with none
// (the strips themselves stay: step 3)
~radTest.("unit-removed",
	setup: ["start 2 1", "fx t 1 pch", "fx t 1 2 dl1", "fx t 2 lpf"],
	cmds: ["fxclear t 1 1", "csfree t 2"],
	check: {
		var fx = RadDoc.current.at(\strips).collect(_[\fx]).reject(_.isNil);
		(fx == (track1: (2: RadDoc.current.at("track1.fx.2")))
			and: { RadDoc.current.at("track1.fx.2.def") == \dl1 })
		or: { "effects: " ++ fx.cs }
	}
);

// a removed track takes its effects with it
~radTest.("unit-track-removed",
	setup: ["start 3", "fx t 3 pch"],
	cmds: ["t rmvn 1"],
	check: { RadDoc.current.at(\track3).isNil or: { "track3: " ++ RadDoc.current.at(\track3).cs } }
);

// stopping the mixer leaves no effects in the document
~radTest.("unit-mixer-killed",
	setup: ["start 2", "fx t 1 pch"],
	code: { Radicles.aZ.kill; 3.wait },
	check: { RadDoc.current.at(\strips).isEmpty or: { "strips: " ++ RadDoc.current.at(\strips).cs } }
);

// structure changes notify; values are read when needed, so fxset adds nothing
// to fader or controller latency
~radTest.("unit-notifications",
	setup: ["start 2"],
	code: {
		~radHeard = [];
		~radListener = {|doc, what, path| ~radHeard = ~radHeard.add([what, RadDoc.addressString(path)]) };
		RadDoc.current.addDependant(~radListener);
		Radicles.cW.callFunc("fx t 1 pch"); 2.wait;
		Radicles.cW.callFunc("fxset t 1 1 pitchRatio 0.5"); 1.wait;
		Radicles.cW.callFunc("fxclear t 1 1"); 2.wait;
		RadDoc.current.removeDependant(~radListener);
	},
	check: {
		(~radHeard == [[\put, "track1.fx.1"], [\removeAt, "track1.fx.1"]]) or: { "heard: " ++ ~radHeard.cs }
	}
);

// a document with effects and modulators writes as code and reads back the same
~radTest.("unit-document-file",
	setup: ["start 2 1", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxset t 1 1 pitchDispertion sin",
		"fx b 1 lpf"],
	check: {
		var doc = RadUnit.refresh(RadDoc.current.copy), path = "/tmp/radtest_doc/units.scd", back;
		doc.write(path);
		back = RadDoc.read(path);
		File.delete(path);
		if(back.isNil, { "not read" }, {
			RadDoc.diff(doc, back).isEmpty or: { "differences: " ++ RadDoc.diff(doc, back).collect(_[\address]).cs }
		})
	}
);
