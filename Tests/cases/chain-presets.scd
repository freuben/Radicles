// Effect-chain presets (Document Model, step 3, PR 2): cssave / csload save
// and load a strip's effects (values, modulators, bypass) as one readable
// file in Settings/Presets/units/chain, onto any strip, changing only what
// differs. Older chain presets still load.

~radChainClean = {|names|
	names.do{|name| File.delete(RadUnit.chainPresetPath(name)) };
};
// a strip's chain as the document has it, values refreshed
~radChainOf = {|strip| (RadUnit.refresh(RadDoc.current.copy).at([strip, \fx]) ? ()) };
// a chain without what belongs to a slot rather than to its effect
~radChainPlain = {|fx| fx.collect{|unit| unit.reject{|v, k| [\position, \morphTime].includes(k) } } };

~radTest.("chain-save-file",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fx t 1 2 dl1", "fxset t 1 1 pitchDispertion sin"],
	cmds: ["cssave t 1 radtest_chain"],
	check: {
		var saved = RadUnit.readChainPreset('radtest_chain'), fx = saved !? { saved[\fx] };
		case
		{ File.exists(RadUnit.chainPresetPath('radtest_chain')).not } { "no file" }
		{ fx.keys != Set[1, 2] } { "slots: " ++ fx.cs }
		{ fx[1][\def] != \pch or: { fx[1][\args][\pitchRatio] != 0.5 } } { "slot 1: " ++ fx[1].cs }
		{ fx[1][\mods].isNil or: { fx[1][\mods][\pitchDispertion][\type] != \sin } } { "slot 1 mods: " ++ fx[1].cs }
		{ fx[2][\def] != \dl1 } { "slot 2: " ++ fx[2].cs }
		{ true }
	},
	teardown: { ~radChainClean.(['radtest_chain']) }
);

// onto an empty track: the same effects, values and modulators
~radTest.("chain-load-other-track",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fx t 1 2 dl1",
		"fxset t 1 1 pitchDispertion sin", "cssave t 1 radtest_chain2"],
	cmds: [["csload t 2 radtest_chain2", 8]],
	check: {
		var one = ~radChainPlain.(~radChainOf.(\track1)), two = ~radChainPlain.(~radChainOf.(\track2));
		//a modulator's range is computed again, so allow for rounding (as fxload does)
		RadDoc.diff(one, two, 1e-9).isEmpty or: { "differences: " ++ RadDoc.diff(one, two, 1e-9).collect(_[\address]).cs }
	},
	teardown: { ~radChainClean.(['radtest_chain2']) }
);

// onto a track with other effects: slots with another effect are replaced,
// slots the preset leaves empty are removed
~radTest.("chain-load-replaces",
	setup: ["start 2", "fx t 1 pch", "fx t 1 2 dl1", "cssave t 1 radtest_chain3",
		"fx t 2 lpf", "fx t 2 3 hpf"],
	cmds: [["csload t 2 radtest_chain3", 8]],
	check: {
		var fx = ~radChainOf.(\track2).collect(_[\def]);
		(fx == (1: \pch, 2: \dl1)) or: { "track2: " ++ fx.cs }
	},
	teardown: { ~radChainClean.(['radtest_chain3']) }
);

// a slot with the same effect keeps it and takes the preset's values
~radTest.("chain-load-same-effect",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "cssave t 1 radtest_chain4",
		"fx t 2 pch", "fxset t 2 1 pitchRatio 2"],
	cmds: [["csload t 2 radtest_chain4", 6]],
	check: { (Ndef(\filterTrack_2_1).get(\pitchRatio) == 0.5) or: {
		"pitchRatio: " ++ Ndef(\filterTrack_2_1).get(\pitchRatio) } },
	teardown: { ~radChainClean.(['radtest_chain4']) }
);

~radTest.("chain-default",
	setup: ["start 2", "fx t 1 pch", "fx t 1 2 dl1"],
	cmds: [["csload t 1 default", 6]],
	check: { ~radChainOf.(\track1).isEmpty or: { "track1: " ++ ~radChainOf.(\track1).cs } }
);

// a chain loads onto a bus and onto the master, like any strip
~radTest.("chain-bus-and-master",
	setup: ["start 2 1", "fx t 1 lpf", "fx t 1 2 hpf", "cssave t 1 radtest_chain5"],
	cmds: [["csload b 1 radtest_chain5", 8], ["csload m radtest_chain5", 8]],
	check: {
		var bus = ~radChainOf.(\bus1).collect(_[\def]), master = ~radChainOf.(\master).collect(_[\def]);
		(bus == (1: \lpf, 2: \hpf) and: { master == (1: \lpf, 2: \hpf) }) or: { "bus1: " ++ bus.cs ++ ", master: " ++ master.cs }
	},
	teardown: { ~radChainClean.(['radtest_chain5']) }
);

// a chain preset saved before Nov 2026 (PresetFile \track) still loads
~radTest.("chain-old-format",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5"],
	code: {
		Radicles.aZ.writeTrackPreset(\track, 1, 'radtest_oldchain'); 1.wait;
		Radicles.cW.callFunc("csload t 2 radtest_oldchain"); 8.wait;
	},
	check: {
		var fx = ~radChainOf.(\track2);
		(fx[1] !? { fx[1][\def] } == \pch and: { Ndef(\filterTrack_2_1).get(\pitchRatio) == 0.5 }) or: { "track2: " ++ fx.cs }
	},
	teardown: { PresetFile.remove(\track, 'radtest_oldchain', false, path: 0, post: false) }
);

// saving over one of your own asks; with no window to ask in it is refused
~radTest.("chain-save-exists",
	setup: ["start 2", "fx t 1 pch", "cssave t 1 radtest_chain6", "fx t 1 1 dl1"],
	cmds: ["cssave t 1 radtest_chain6"],
	check: {
		(RadUnit.readChainPreset('radtest_chain6')[\fx][1][\def] == \pch) or: { "replaced" }
	},
	teardown: { ~radChainClean.(['radtest_chain6']) }
);

~radTest.("chain-list",
	setup: ["start 2", "fx t 1 pch", "cssave t 1 radtest_chain7"],
	code: { Radicles.cW.callFunc("csload"); 1.wait; ~radNames = RadUnit.chainPresetNames },
	check: { ~radNames.includes('radtest_chain7') or: { "names: " ++ ~radNames.cs } },
	teardown: { ~radChainClean.(['radtest_chain7']) }
);

~radTest.("chain-load-missing",
	setup: ["start 2", "fx t 1 pch"],
	cmds: ["csload t 1 radtest_nochain"],
	check: { (~radChainOf.(\track1).collect(_[\def]) == (1: \pch)) or: { "track1: " ++ ~radChainOf.(\track1).cs } }
);
