// A chain preset loaded over time (Document Model, step 3, PR 3; "Time in
// Radicles", 19 Nov 2026): csload moves a strip's effects to a chain preset
// over a time, slot by slot: the same effect moves as fxload does, another
// effect crossfades, a slot the preset leaves empty fades out and goes, a
// new slot fades in, bypass follows. Without a time, the strip's own fade
// time is used. (These tests were written for csmorph, which csload
// replaced; see also load-transition.scd.)

~radCmClean = {|names| names.do{|name| File.delete(RadUnit.chainPresetPath(name)) } };
~radCmChain = {|strip| (RadUnit.refresh(RadDoc.current.copy).at([strip, \fx]) ? ()) };
// everything a test changes comes back: the mixer's fade time, every strip
// Ndef's fade time, no morph left running
~radCmSettled = {
	var az = Radicles.aZ, fades = az.masterNdefs.flat.collect(_.fadeTime).asSet;
	case
	{ Radicles.fadeTime != 0.5 } { "Radicles.fadeTime: " ++ Radicles.fadeTime }
	{ fades != Set[0.5] } { "strip fade times: " ++ fades.cs }
	{ nil };
};

~radTest.("csmorph-same-effect",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "cssave t 1 radtest_cm1", "fxset t 1 1 pitchRatio 2"],
	cmds: [["csload t 1 radtest_cm1 1", 3]],
	check: {
		~radCmSettled.() ?? {
			(Ndef(\filterTrack_1_1).get(\pitchRatio) == 0.5) or: { "pitchRatio: " ++ Ndef(\filterTrack_1_1).get(\pitchRatio) } }
	},
	teardown: { ~radCmClean.(['radtest_cm1']) }
);

~radTest.("csmorph-glide",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "cssave t 1 radtest_cm2", "fxset t 1 1 pitchRatio 2"],
	cmds: [["csload t 1 radtest_cm2 1 glide", 4]],
	check: {
		~radCmSettled.() ?? {
			(Ndef(\filterTrack_1_1).get(\pitchRatio) == 0.5) or: { "pitchRatio: " ++ Ndef(\filterTrack_1_1).get(\pitchRatio) } }
	},
	teardown: { ~radCmClean.(['radtest_cm2']) }
);

// another effect in a slot crossfades to the preset's
~radTest.("csmorph-other-effect",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "cssave t 1 radtest_cm3", "fx t 1 1 lpf"],
	cmds: [["csload t 1 radtest_cm3 1", 4]],
	check: {
		var fx = ~radCmChain.(\track1);
		~radCmSettled.() ?? {
			(fx.collect(_[\def]) == (1: \pch) and: { Ndef(\filterTrack_1_1).get(\pitchRatio) == 0.5 }) or: { "track1: " ++ fx.cs } }
	},
	teardown: { ~radCmClean.(['radtest_cm3']) }
);

// a slot the preset leaves empty fades out and goes; one only in the preset
// fades in
~radTest.("csmorph-remove-and-insert",
	setup: ["start 2", "fx t 1 lpf", "fx t 1 3 dl1", "cssave t 1 radtest_cm4", "fx rm t 1 3", "fx t 1 2 hpf"],
	cmds: [["csload t 1 radtest_cm4 1", 5]],
	check: {
		var fx = ~radCmChain.(\track1).collect(_[\def]);
		~radCmSettled.() ?? { (fx == (1: \lpf, 3: \dl1)) or: { "track1: " ++ fx.cs } }
	},
	teardown: { ~radCmClean.(['radtest_cm4']) }
);

~radTest.("csmorph-bypass",
	setup: ["start 2", "fx t 1 pch", "fx t 1 2 dl1", ["fxbypass t 1 2", 2], "cssave t 1 radtest_cm5", ["fxbypass t 1 2 0", 2]],
	cmds: [["csload t 1 radtest_cm5 1", 4]],
	check: {
		~radCmSettled.() ?? {
			(Radicles.aZ.isBypassed(\filterTrack_1_2) and: { Radicles.aZ.isBypassed(\filterTrack_1_1).not })
			or: { "bypassed: " ++ Radicles.aZ.bypassed.cs } }
	},
	teardown: { ~radCmClean.(['radtest_cm5']) }
);

// none: everything at once
~radTest.("csmorph-none",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "cssave t 1 radtest_cm6", "fx t 1 1 lpf"],
	cmds: [["csload t 1 radtest_cm6 5 none", 2]],
	check: {
		var fx = ~radCmChain.(\track1).collect(_[\def]);
		~radCmSettled.() ?? { (fx == (1: \pch)) or: { "track1: " ++ fx.cs } }
	},
	teardown: { ~radCmClean.(['radtest_cm6']) }
);

// the strip's own fade time when the command gives none (fade t 1 1.5 sets
// the strip's Ndefs, not its effects'): the effect crossfades over it, then
// has its own fade time again
~radTest.("csmorph-strip-time",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "cssave t 1 radtest_cm7", "fxset t 1 1 pitchRatio 2",
		"fade t 1 1.5"],
	code: {
		Radicles.cW.callFunc("csload t 1 radtest_cm7"); 0.5.wait;
		~radFadeDuring = Ndef(\filterTrack_1_1).fadeTime;
		3.wait;
	},
	check: {
		var after = [\filterTrack_1_1, \track1, \inTrack1, \spaceTrack1].collect{|key| Ndef(key).fadeTime };
		case
		{ ~radFadeDuring != 1.5 } { "the effect's fade time during: " ++ ~radFadeDuring }
		{ after != [0.5, 1.5, 1.5, 1.5] } { "fade times after: " ++ after.cs }
		{ Ndef(\filterTrack_1_1).get(\pitchRatio) != 0.5 } { "pitchRatio: " ++ Ndef(\filterTrack_1_1).get(\pitchRatio) }
		{ true }
	},
	teardown: { ~radCmClean.(['radtest_cm7']) }
);

// a second csload on the same strip stops the first where it is
~radTest.("csmorph-interrupted",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "cssave t 1 radtest_cm9a",
		"fxset t 1 1 pitchRatio 0.8", "cssave t 1 radtest_cm9b", "fxset t 1 1 pitchRatio 2"],
	code: {
		Radicles.cW.callFunc("csload t 1 radtest_cm9a 4"); 1.wait;
		Radicles.cW.callFunc("csload t 1 radtest_cm9b 1"); 3.wait;
	},
	check: {
		~radCmSettled.() ?? {
			(Ndef(\filterTrack_1_1).get(\pitchRatio) == 0.8) or: { "pitchRatio: " ++ Ndef(\filterTrack_1_1).get(\pitchRatio) } }
	},
	teardown: { ~radCmClean.(['radtest_cm9a', 'radtest_cm9b']) }
);

// already there: nothing posted, nothing changed
~radTest.("csmorph-already-there",
	setup: ["start 2", "fx t 1 pch", "cssave t 1 radtest_cm10"],
	cmds: [["csload t 1 radtest_cm10 1", 2]],
	check: { ~radCmSettled.() ? true },
	teardown: { ~radCmClean.(['radtest_cm10']) }
);

// onto the master, like any strip
~radTest.("csmorph-master",
	setup: ["start 2", "fx t 1 lpf", "cssave t 1 radtest_cm11"],
	cmds: [["csload m radtest_cm11 1", 4]],
	check: {
		var fx = ~radCmChain.(\master).collect(_[\def]);
		~radCmSettled.() ?? { (fx == (1: \lpf)) or: { "master: " ++ fx.cs } }
	},
	teardown: { ~radCmClean.(['radtest_cm11']) }
);
