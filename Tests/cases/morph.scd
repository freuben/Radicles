// An effect preset loaded over a typed time (Document Model, step 1, PR 5;
// "Time in Radicles", 19 Nov 2026): fxload moves an effect to one of its
// presets over time (xfade, glide or none); the fade time and lags always go
// back. Without a time, the effect's own fade time is used. (These tests
// were written for fxload, which fxload replaced; see also
// load-transition.scd.)

~radMorphClean = {
	(RadUnit.presetRoot +/+ "filter/*/radtest_*.scd").pathMatch.do{|path| File.delete(path) };
};
// a preset radtest_m for pch with pitchRatio 0.5, then pitchRatio back to 2
~radMorphSetup = ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxsave t 1 1 radtest_m",
	"fxset t 1 1 pitchRatio 2"];
~radRatio = { Ndef(\filterTrack_1_1).get(\pitchRatio) };
~radFade = { Ndef(\filterTrack_1_1).fadeTime };
~radLag = { Ndef(\filterTrack_1_1).nodeMap.ratesFor([\pitchRatio]) !? {|r| r[0] } };

// xfade: fade time set to the morph time, one xset, fade time back after
~radTest.("morph-xfade",
	setup: ~radMorphSetup,
	code: {
		Radicles.cW.callFunc("fxload t 1 1 radtest_m 3"); 1.wait;
		~radMid = [~radFade.(), ~radRatio.()];
		3.wait;
	},
	check: {
		case
		{ ~radMid[0] != 3 } { "fade time during the morph: " ++ ~radMid[0] }
		{ ~radRatio.() != 0.5 } { "pitchRatio " ++ ~radRatio.() }
		{ ~radFade.() != 0.5 } { "fade time after: " ++ ~radFade.() }
		{ true }
	},
	teardown: ~radMorphClean
);

// glide: the changed argument gets the morph time as its lag, then one set;
// the earlier lag (none) goes back after
~radTest.("morph-glide",
	setup: ~radMorphSetup,
	code: {
		Radicles.cW.callFunc("fxload t 1 1 radtest_m 3 glide"); 1.5.wait;
		~radMid = [~radLag.(), ~radFade.()];
		3.wait;
	},
	check: {
		case
		{ ~radMid[0] != 3 } { "lag during the morph: " ++ ~radMid[0].cs }
		{ ~radMid[1] != 0.5 } { "glide changed the fade time: " ++ ~radMid[1] }
		{ ~radRatio.() != 0.5 } { "pitchRatio " ++ ~radRatio.() }
		{ ~radLag.().notNil } { "lag after: " ++ ~radLag.().cs }
		{ true }
	},
	teardown: ~radMorphClean
);

// glide puts back a lag the argument had before
~radTest.("morph-glide-restores-lag",
	setup: ~radMorphSetup ++ ["fxlag t 1 1 pitchRatio 0.2"],
	cmds: [["fxload t 1 1 radtest_m 1 glide", 3]],
	check: { (~radLag.() == 0.2 and: { ~radRatio.() == 0.5 }) or: { "lag " ++ ~radLag.().cs ++ ", pitchRatio " ++ ~radRatio.() } },
	teardown: ~radMorphClean
);

~radTest.("morph-none",
	setup: ~radMorphSetup,
	cmds: [["fxload t 1 1 radtest_m 3 none", 1.5]],
	check: { (~radRatio.() == 0.5 and: { ~radFade.() == 0.5 }) or: { "pitchRatio " ++ ~radRatio.() ++ ", fade " ++ ~radFade.() } },
	teardown: ~radMorphClean
);

// the time: the command's, else the effect's own fade time; nothing else
~radTest.("morph-time-own-fade",
	setup: ~radMorphSetup ++ ["fade fx t 1 1 3"],
	code: { Radicles.cW.callFunc("fxload t 1 1 radtest_m"); 1.wait; ~radMid = ~radFade.(); 3.wait },
	check: {
		(~radMid == 3 and: { ~radFade.() == 3 } and: { ~radRatio.() == 0.5 })
		or: { "fade during " ++ ~radMid ++ ", after " ++ ~radFade.() ++ ", pitchRatio " ++ ~radRatio.() }
	},
	teardown: ~radMorphClean
);

~radTest.("morph-time-default",
	setup: ~radMorphSetup,
	code: { Radicles.cW.callFunc("fxload t 1 1 radtest_m"); 0.2.wait; ~radMid = ~radFade.(); 1.5.wait },
	check: {
		(~radMid == 0.5 and: { ~radFade.() == 0.5 } and: { ~radRatio.() == 0.5 })
		or: { "fade during " ++ ~radMid ++ ", after " ++ ~radFade.() ++ ", pitchRatio " ++ ~radRatio.() }
	},
	teardown: ~radMorphClean
);

// a typed time wins over the effect's own fade time, which comes back
~radTest.("morph-time-command",
	setup: ~radMorphSetup ++ ["fade fx t 1 1 5"],
	code: { Radicles.cW.callFunc("fxload t 1 1 radtest_m 1.5"); 0.5.wait; ~radMid = ~radFade.(); 2.wait },
	check: { (~radMid == 1.5 and: { ~radFade.() == 5 }) or: { "fade during " ++ ~radMid ++ ", after " ++ ~radFade.() } },
	teardown: ~radMorphClean
);

// another load interrupts the first: its fade time goes back first, the
// second preset wins, and the fade time is back at the end
~radTest.("morph-interrupted",
	setup: ~radMorphSetup ++ ["fxset t 1 1 pitchRatio 0.25", "fxsave t 1 1 radtest_q", "fxset t 1 1 pitchRatio 2"],
	code: {
		Radicles.cW.callFunc("fxload t 1 1 radtest_m 6"); 1.wait;
		Radicles.cW.callFunc("fxload t 1 1 radtest_q 1"); 0.5.wait;
		~radMid = ~radFade.();
		2.wait;
	},
	check: {
		case
		{ ~radMid != 1 } { "fade during the second morph: " ++ ~radMid }
		{ ~radRatio.() != 0.25 } { "pitchRatio " ++ ~radRatio.() }
		{ ~radFade.() != 0.5 } { "fade time after: " ++ ~radFade.() }
		{ true }
	},
	teardown: ~radMorphClean
);

~radTest.("morph-glide-interrupted",
	setup: ~radMorphSetup ++ ["fxset t 1 1 pitchRatio 0.25", "fxsave t 1 1 radtest_q", "fxset t 1 1 pitchRatio 2"],
	code: {
		Radicles.cW.callFunc("fxload t 1 1 radtest_m 6 glide"); 1.5.wait;
		Radicles.cW.callFunc("fxload t 1 1 radtest_q 1"); 3.wait;
	},
	check: { (~radLag.().isNil and: { ~radFade.() == 0.5 } and: { ~radRatio.() == 0.25 }) or: {
		"lag " ++ ~radLag.().cs ++ ", fade " ++ ~radFade.() ++ ", pitchRatio " ++ ~radRatio.() } },
	teardown: ~radMorphClean
);

// a command during the load wins for its own argument
~radTest.("morph-command-wins",
	setup: ~radMorphSetup,
	code: {
		Radicles.cW.callFunc("fxload t 1 1 radtest_m 3"); 1.wait;
		Radicles.cW.callFunc("fxset t 1 1 pitchRatio 1.5"); 3.wait;
	},
	check: { (~radRatio.() == 1.5 and: { ~radFade.() == 0.5 }) or: { "pitchRatio " ++ ~radRatio.() ++ ", fade " ++ ~radFade.() } },
	teardown: ~radMorphClean
);

// removing the effect during a load stops the load
~radTest.("morph-effect-removed",
	setup: ~radMorphSetup,
	code: {
		Radicles.cW.callFunc("fxload t 1 1 radtest_m 4"); 1.wait;
		Radicles.cW.callFunc("fx rm t 1 1"); 5.wait;
	},
	check: { (Radicles.aZ.filters ? []).isEmpty or: { "the effect is still there: " ++ Radicles.aZ.filters.cs } },
	teardown: ~radMorphClean
);

// a modulator the preset has fades in with the rest; the unit ends as the preset
~radTest.("morph-modulator",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchDispertion sin", "fxsave t 1 1 radtest_mod",
		"unmodfx t 1 1 pitchDispertion"],
	cmds: [["fxload t 1 1 radtest_mod 1", 3]],
	check: {
		var diff = RadDoc.diff(RadUnit.readPreset(\filter, \pch, \radtest_mod), RadUnit.read(\filterTrack_1_1), 1e-6);
		(diff.isEmpty and: { ~radFade.() == 0.5 }) or: { "differences: " ++ diff.collect(_[\address]).join(", ") ++ ", fade " ++ ~radFade.() }
	},
	teardown: ~radMorphClean
);

// a bypassed effect: values are set at once, and it stays bypassed
~radTest.("morph-bypassed",
	setup: ~radMorphSetup ++ [["fxbypass t 1 1", 3]],
	cmds: [["fxload t 1 1 radtest_m 3", 1.5]],
	check: {
		(~radRatio.() == 0.5 and: { ~radFade.() == 0.5 } and: { Radicles.aZ.isBypassed(\filterTrack_1_1) })
		or: { "pitchRatio " ++ ~radRatio.() ++ ", fade " ++ ~radFade.() }
	},
	teardown: ~radMorphClean
);

// by mixer position, and on the master
~radTest.("morph-forms",
	setup: ["start 2", "fx t 2 pch", "fx m 1 pch", "fxset t 2 1 pitchRatio 0.5", "fxsave t 2 1 radtest_m",
		"fxset t 2 1 pitchRatio 2", "fxset m 1 1 pitchRatio 2"],
	cmds: [["fxload 2 1 radtest_m 1", 2], ["fxload m 1 radtest_m 1", 2]],
	check: {
		(Ndef(\filterTrack_2_1).get(\pitchRatio) == 0.5 and: { Ndef(\filterMaster_1_1).get(\pitchRatio) == 0.5 })
		or: { "track 2 " ++ Ndef(\filterTrack_2_1).get(\pitchRatio) ++ ", master " ++ Ndef(\filterMaster_1_1).get(\pitchRatio) }
	},
	teardown: ~radMorphClean
);

// nothing happens, with a reason: no such preset, an unknown word, an empty slot
~radTest.("morph-refused",
	setup: ~radMorphSetup,
	cmds: ["fxload t 1 1 radtest_nope", "fxload t 1 1 radtest_m 1 wobble", "fxload t 1 2 radtest_m"],
	check: { (~radRatio.() == 2 and: { ~radFade.() == 0.5 }) or: { "pitchRatio " ++ ~radRatio.() ++ ", fade " ++ ~radFade.() } },
	teardown: ~radMorphClean
);

// already at the preset: nothing is posted and nothing waits
~radTest.("morph-already-there",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxsave t 1 1 radtest_m"],
	cmds: [["fxload t 1 1 radtest_m 3", 1]],
	check: { (~radFade.() == 0.5) or: { "fade " ++ ~radFade.() } },
	teardown: ~radMorphClean
);
