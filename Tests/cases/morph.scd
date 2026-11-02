// Effect morph and morph time (Document Model, step 1, PR 5): fxmorph moves
// an effect to one of its presets over time (xfade, glide or none); the fade
// time and lags always go back; morphtime sets the global and a slot's time.

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
		Radicles.cW.callFunc("fxmorph t 1 1 radtest_m 3"); 1.wait;
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
		Radicles.cW.callFunc("fxmorph t 1 1 radtest_m 3 glide"); 1.5.wait;
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
	cmds: [["fxmorph t 1 1 radtest_m 1 glide", 3]],
	check: { (~radLag.() == 0.2 and: { ~radRatio.() == 0.5 }) or: { "lag " ++ ~radLag.().cs ++ ", pitchRatio " ++ ~radRatio.() } },
	teardown: ~radMorphClean
);

~radTest.("morph-none",
	setup: ~radMorphSetup,
	cmds: [["fxmorph t 1 1 radtest_m 3 none", 1.5]],
	check: { (~radRatio.() == 0.5 and: { ~radFade.() == 0.5 }) or: { "pitchRatio " ++ ~radRatio.() ++ ", fade " ++ ~radFade.() } },
	teardown: ~radMorphClean
);

// the time: the command's, else the preset's, else the slot's, else the global one
~radTest.("morph-time-global",
	setup: ~radMorphSetup ++ ["morphtime 4"],
	code: { Radicles.cW.callFunc("fxmorph t 1 1 radtest_m"); 1.wait; ~radMid = ~radFade.(); 4.wait },
	check: { (~radMid == 4 and: { ~radFade.() == 0.5 }) or: { "fade during " ++ ~radMid ++ ", after " ++ ~radFade.() } },
	teardown: ~radMorphClean
);

~radTest.("morph-time-default",
	setup: ~radMorphSetup,
	code: { Radicles.cW.callFunc("fxmorph t 1 1 radtest_m"); 1.wait; ~radMid = ~radFade.(); 2.wait },
	check: { (~radMid == 2) or: { "fade during " ++ ~radMid } },
	teardown: ~radMorphClean
);

~radTest.("morph-time-slot",
	setup: ~radMorphSetup ++ ["morphtime 4", "morphtime t 1 1 3"],
	code: { Radicles.cW.callFunc("fxmorph t 1 1 radtest_m"); 1.wait; ~radMid = ~radFade.(); 3.wait },
	check: { (~radMid == 3) or: { "fade during " ++ ~radMid } },
	teardown: ~radMorphClean
);

~radTest.("morph-time-command",
	setup: ~radMorphSetup ++ ["morphtime t 1 1 5"],
	code: { Radicles.cW.callFunc("fxmorph t 1 1 radtest_m 1.5"); 0.5.wait; ~radMid = ~radFade.(); 2.wait },
	check: { (~radMid == 1.5 and: { ~radFade.() == 0.5 }) or: { "fade during " ++ ~radMid ++ ", after " ++ ~radFade.() } },
	teardown: ~radMorphClean
);

// a slot's morph time is in its unit and saved with its presets; a preset
// with a morph time gives it to the slot it is loaded or morphed into, and
// that time is used for the morph
~radTest.("morph-time-in-presets",
	setup: ["start 2", "fx t 1 pch", "fx t 2 pch", "morphtime t 1 1 3", "fxset t 1 1 pitchRatio 0.5",
		"fxsave t 1 1 radtest_mt"],
	code: { Radicles.cW.callFunc("fxmorph t 2 1 radtest_mt"); 1.wait; ~radMid = Ndef(\filterTrack_2_1).fadeTime; 3.wait },
	check: {
		var saved = RadUnit.readPreset(\filter, \pch, \radtest_mt);
		case
		{ RadUnit.read(\filterTrack_1_1)[\morphTime] != 3 } { "unit: " ++ RadUnit.read(\filterTrack_1_1).cs }
		{ saved[\morphTime] != 3 } { "preset: " ++ saved.cs }
		{ ~radMid != 3 } { "morph used " ++ ~radMid }
		{ Radicles.aZ.morphTimeOf(\filterTrack_2_1) != 3 } { "track 2 slot: " ++ Radicles.aZ.morphTimeOf(\filterTrack_2_1) }
		{ true }
	},
	teardown: ~radMorphClean
);

~radTest.("morphtime-default-and-post",
	setup: ["start 2", "fx t 1 pch", "morphtime t 1 1 3"],
	cmds: ["morphtime t 1 1 default", "morphtime t 1 1", "morphtime"],
	check: { Radicles.aZ.morphTimeOf(\filterTrack_1_1).isNil or: { "still " ++ Radicles.aZ.morphTimeOf(\filterTrack_1_1) } }
);

// another morph interrupts the first: its fade time goes back first, the
// second preset wins, and the fade time is back at the end
~radTest.("morph-interrupted",
	setup: ~radMorphSetup ++ ["fxset t 1 1 pitchRatio 0.25", "fxsave t 1 1 radtest_q", "fxset t 1 1 pitchRatio 2"],
	code: {
		Radicles.cW.callFunc("fxmorph t 1 1 radtest_m 6"); 1.wait;
		Radicles.cW.callFunc("fxmorph t 1 1 radtest_q 1"); 0.5.wait;
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
		Radicles.cW.callFunc("fxmorph t 1 1 radtest_m 6 glide"); 1.5.wait;
		Radicles.cW.callFunc("fxmorph t 1 1 radtest_q 1"); 3.wait;
	},
	check: { (~radLag.().isNil and: { ~radFade.() == 0.5 } and: { ~radRatio.() == 0.25 }) or: {
		"lag " ++ ~radLag.().cs ++ ", fade " ++ ~radFade.() ++ ", pitchRatio " ++ ~radRatio.() } },
	teardown: ~radMorphClean
);

// a command during the morph wins for its own argument
~radTest.("morph-command-wins",
	setup: ~radMorphSetup,
	code: {
		Radicles.cW.callFunc("fxmorph t 1 1 radtest_m 3"); 1.wait;
		Radicles.cW.callFunc("fxset t 1 1 pitchRatio 1.5"); 3.wait;
	},
	check: { (~radRatio.() == 1.5 and: { ~radFade.() == 0.5 }) or: { "pitchRatio " ++ ~radRatio.() ++ ", fade " ++ ~radFade.() } },
	teardown: ~radMorphClean
);

// removing the effect during a morph stops it and forgets the slot's morph time
~radTest.("morph-effect-removed",
	setup: ~radMorphSetup ++ ["morphtime t 1 1 4"],
	code: {
		Radicles.cW.callFunc("fxmorph t 1 1 radtest_m"); 1.wait;
		Radicles.cW.callFunc("fxclear t 1 1"); 5.wait;
	},
	check: { Radicles.aZ.morphTimeOf(\filterTrack_1_1).isNil or: { "slot morph time kept" } },
	teardown: ~radMorphClean
);

// a modulator the preset has fades in with the rest; the unit ends as the preset
~radTest.("morph-modulator",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchDispertion sin", "fxsave t 1 1 radtest_mod",
		"unmodfx t 1 1 pitchDispertion"],
	cmds: [["fxmorph t 1 1 radtest_mod 1", 3]],
	check: {
		var diff = RadDoc.diff(RadUnit.readPreset(\filter, \pch, \radtest_mod), RadUnit.read(\filterTrack_1_1), 1e-6);
		(diff.isEmpty and: { ~radFade.() == 0.5 }) or: { "differences: " ++ diff.collect(_[\address]).join(", ") ++ ", fade " ++ ~radFade.() }
	},
	teardown: ~radMorphClean
);

// a bypassed effect: values are set at once, and it stays bypassed
~radTest.("morph-bypassed",
	setup: ~radMorphSetup ++ [["fxbypass t 1 1", 3]],
	cmds: [["fxmorph t 1 1 radtest_m 3", 1.5]],
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
	cmds: [["fxmorph 2 1 radtest_m 1", 2], ["fxmorph m 1 radtest_m 1", 2]],
	check: {
		(Ndef(\filterTrack_2_1).get(\pitchRatio) == 0.5 and: { Ndef(\filterMaster_1_1).get(\pitchRatio) == 0.5 })
		or: { "track 2 " ++ Ndef(\filterTrack_2_1).get(\pitchRatio) ++ ", master " ++ Ndef(\filterMaster_1_1).get(\pitchRatio) }
	},
	teardown: ~radMorphClean
);

// nothing happens, with a reason: no such preset, an unknown shape, an empty slot
~radTest.("morph-refused",
	setup: ~radMorphSetup,
	cmds: ["fxmorph t 1 1 radtest_nope", "fxmorph t 1 1 radtest_m 1 wobble", "fxmorph t 1 2 radtest_m"],
	check: { (~radRatio.() == 2 and: { ~radFade.() == 0.5 }) or: { "pitchRatio " ++ ~radRatio.() ++ ", fade " ++ ~radFade.() } },
	teardown: ~radMorphClean
);

// already at the preset: nothing is posted and nothing waits
~radTest.("morph-already-there",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxsave t 1 1 radtest_m"],
	cmds: [["fxmorph t 1 1 radtest_m 3", 1]],
	check: { (~radFade.() == 0.5) or: { "fade " ++ ~radFade.() } },
	teardown: ~radMorphClean
);
