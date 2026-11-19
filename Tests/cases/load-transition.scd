// Loading a preset is a transition ("Time in Radicles", 19 Nov 2026).
// fxload, blkload, csload and tload crossfade by default over the fade time
// of the thing loaded onto, and post nothing about the fade time then. After
// the preset's name, in any order: a time in seconds (for this load only:
// the fade time is set to it, and comes back), xfade, glide or none. A time
// of 0, or none, sets the values at once. The morph words (fxmorph,
// blkmorph, csmorph, tmorph, morphtime, ...) are gone, and a morphTime in an
// older preset file is ignored.

~radLtClean = {
	(RadUnit.presetRoot +/+ "filter/*/radtest_lt*.scd").pathMatch.do{|path| File.delete(path) };
	(RadUnit.presetRoot +/+ "block/*/radtest_lt*.scd").pathMatch.do{|path| File.delete(path) };
	['radtest_ltc', 'radtest_ltcold'].do{|name| File.delete(RadUnit.chainPresetPath(name)) };
	['radtest_ltt', 'radtest_lttold'].do{|name| File.delete(RadUnit.trackPresetPath(name)) };
};
~radLtFades = {|keys| keys.collect{|key| Ndef(key).fadeTime } };
~radLtStrip1 = [\inTrack1, \filterTrack_1_1, \track1, \spaceTrack1];
~radLtRatio = { Ndef(\filterTrack_1_1).get(\pitchRatio) };
~radLtLag = {|key, name| Ndef(key).nodeMap.ratesFor([name]) !? {|r| r[0] } };
~radLtStripNow = {|name| RadUnit.refresh(RadDoc.current.copy).at(name) ? () };

// ---- fxload ----

// a preset radtest_lt for pch with pitchRatio 0.5, then pitchRatio back to 2
~radLtFx = ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxsave t 1 1 radtest_lt",
	"fxset t 1 1 pitchRatio 2"];

// by default: one xset, a crossfade over the effect's own fade time; no
// fade time lines
~radTest.("fxload-default-crossfades",
	setup: ~radLtFx,
	cmds: [["fxload t 1 1 radtest_lt", 2]],
	check: {
		(~radLtRatio.() == 0.5 and: { Ndef(\filterTrack_1_1).fadeTime == 0.5 })
		or: { "pitchRatio " ++ ~radLtRatio.() ++ ", fade " ++ Ndef(\filterTrack_1_1).fadeTime }
	},
	teardown: ~radLtClean
);

// the effect's own fade time is the time: still no fade time lines
~radTest.("fxload-own-fade-time",
	setup: ~radLtFx ++ ["fade fx t 1 1 2"],
	cmds: [["fxload t 1 1 radtest_lt", 3]],
	check: {
		(~radLtRatio.() == 0.5 and: { Ndef(\filterTrack_1_1).fadeTime == 2 })
		or: { "pitchRatio " ++ ~radLtRatio.() ++ ", fade " ++ Ndef(\filterTrack_1_1).fadeTime }
	},
	teardown: ~radLtClean
);

// a typed time: the fade time is set to it, one xset, and it is back after
~radTest.("fxload-typed-time",
	setup: ~radLtFx,
	cmds: [["fxload t 1 1 radtest_lt 2", 0.7]],
	code: { ~radLtMid = Ndef(\filterTrack_1_1).fadeTime; 2.5.wait },
	check: {
		case
		{ ~radLtMid != 2 } { "fade time during the load: " ++ ~radLtMid }
		{ ~radLtRatio.() != 0.5 } { "pitchRatio " ++ ~radLtRatio.() }
		{ Ndef(\filterTrack_1_1).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\filterTrack_1_1).fadeTime }
		{ true }
	},
	teardown: ~radLtClean
);

// none, and a time of 0: one plain set, at once
~radTest.("fxload-none-and-zero",
	setup: ~radLtFx,
	cmds: [["fxload t 1 1 radtest_lt none", 1], ["fxset t 1 1 pitchRatio 2", 1], ["fxload t 1 1 radtest_lt 0", 1]],
	check: {
		(~radLtRatio.() == 0.5 and: { Ndef(\filterTrack_1_1).fadeTime == 0.5 })
		or: { "pitchRatio " ++ ~radLtRatio.() ++ ", fade " ++ Ndef(\filterTrack_1_1).fadeTime }
	},
	teardown: ~radLtClean
);

// glide, the words in any order: a lag over the typed time, one set, the lag
// goes back; the fade time is not touched
~radTest.("fxload-glide-words-any-order",
	setup: ~radLtFx,
	cmds: [["fxload t 1 1 radtest_lt glide 1", 1.2]],
	code: { ~radLtMid = [~radLtLag.(\filterTrack_1_1, \pitchRatio), Ndef(\filterTrack_1_1).fadeTime]; 2.5.wait },
	check: {
		case
		{ ~radLtMid != [1, 0.5] } { "lag and fade time during the load: " ++ ~radLtMid.cs }
		{ ~radLtRatio.() != 0.5 } { "pitchRatio " ++ ~radLtRatio.() }
		{ ~radLtLag.(\filterTrack_1_1, \pitchRatio).notNil } { "lag after: " ++ ~radLtLag.(\filterTrack_1_1, \pitchRatio).cs }
		{ true }
	},
	teardown: ~radLtClean
);

// ---- blkload ----

~radLtBlk = ["start 2", "blk 1 sine", "blkset 1 freq 500", "blksave 1 radtest_lt", "blkset 1 freq 330"];

~radTest.("blkload-default-crossfades",
	setup: ~radLtBlk,
	cmds: [["blkload 1 radtest_lt", 2]],
	check: {
		(Ndef(\block1).get(\freq) == 500 and: { Ndef(\block1).fadeTime == 0.5 })
		or: { "freq " ++ Ndef(\block1).get(\freq) ++ ", fade " ++ Ndef(\block1).fadeTime }
	},
	teardown: ~radLtClean
);

~radTest.("blkload-typed-time",
	setup: ~radLtBlk,
	cmds: [["blkload 1 radtest_lt 2", 0.7]],
	code: { ~radLtMid = Ndef(\block1).fadeTime; 2.5.wait },
	check: {
		case
		{ ~radLtMid != 2 } { "fade time during the load: " ++ ~radLtMid }
		{ Ndef(\block1).get(\freq) != 500 } { "freq " ++ Ndef(\block1).get(\freq) }
		{ Ndef(\block1).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\block1).fadeTime }
		{ true }
	},
	teardown: ~radLtClean
);

~radTest.("blkload-none-and-zero",
	setup: ~radLtBlk,
	cmds: [["blkload 1 radtest_lt none", 1], ["blkset 1 freq 330", 1], ["blkload 1 radtest_lt 0", 1]],
	check: {
		(Ndef(\block1).get(\freq) == 500 and: { Ndef(\block1).fadeTime == 0.5 })
		or: { "freq " ++ Ndef(\block1).get(\freq) ++ ", fade " ++ Ndef(\block1).fadeTime }
	},
	teardown: ~radLtClean
);

// glide with nobuf, the words in any order
~radTest.("blkload-glide-nobuf-any-order",
	setup: ~radLtBlk,
	cmds: [["blkload 1 radtest_lt nobuf 1 glide", 4]],
	check: {
		case
		{ Ndef(\block1).get(\freq) != 500 } { "freq " ++ Ndef(\block1).get(\freq) }
		{ ~radLtLag.(\block1, \freq).notNil } { "lag after: " ++ ~radLtLag.(\block1, \freq).cs }
		{ Ndef(\block1).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\block1).fadeTime }
		{ true }
	},
	teardown: ~radLtClean
);

// a word that is not a time, a shape or nobuf: nothing is loaded
~radTest.("blkload-unknown-word",
	setup: ~radLtBlk,
	cmds: ["blkload 1 radtest_lt sideways", "blkload 1 radtest_lt 2 sideways"],
	check: { (Ndef(\block1).get(\freq) == 330) or: { "freq " ++ Ndef(\block1).get(\freq) } },
	teardown: ~radLtClean
);

// ---- csload ----

~radLtChain = ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "cssave t 1 radtest_ltc",
	"fxset t 1 1 pitchRatio 2"];

// by default: over the strip's own fade time, which every stage already
// has, so only the xset is posted
~radTest.("csload-default-crossfades",
	setup: ~radLtChain,
	cmds: [["csload t 1 radtest_ltc", 2]],
	check: {
		var fades = ~radLtFades.(~radLtStrip1);
		(~radLtRatio.() == 0.5 and: { fades.every(_ == 0.5) })
		or: { "pitchRatio " ++ ~radLtRatio.() ++ ", fade times " ++ fades.cs }
	},
	teardown: ~radLtClean
);

// a typed time: the strip's stages crossfade over it while the load runs,
// then have their own fade time again (the panner keeps its own: no load
// rebuilds it); other strips are not touched
~radTest.("csload-typed-time",
	setup: ~radLtChain ++ ["fade t 2 3"],
	cmds: [["csload t 1 radtest_ltc 2", 0.7]],
	code: { ~radLtMid = ~radLtFades.(~radLtStrip1 ++ [\track2]); 2.5.wait },
	check: {
		var fades = ~radLtFades.(~radLtStrip1 ++ [\track2]);
		case
		{ ~radLtMid != [2, 2, 2, 0.5, 3] } { "fade times during the load: " ++ ~radLtMid.cs }
		{ ~radLtRatio.() != 0.5 } { "pitchRatio " ++ ~radLtRatio.() }
		{ fades != [0.5, 0.5, 0.5, 0.5, 3] } { "fade times after: " ++ fades.cs }
		{ Radicles.fadeTime != 0.5 } { "the session fade: " ++ Radicles.fadeTime }
		{ true }
	},
	teardown: ~radLtClean
);

~radTest.("csload-none-and-zero",
	setup: ~radLtChain,
	cmds: [["csload t 1 radtest_ltc none", 2], ["fxset t 1 1 pitchRatio 2", 1], ["csload t 1 radtest_ltc 0", 2]],
	check: {
		var fades = ~radLtFades.(~radLtStrip1);
		(~radLtRatio.() == 0.5 and: { fades.every(_ == 0.5) })
		or: { "pitchRatio " ++ ~radLtRatio.() ++ ", fade times " ++ fades.cs }
	},
	teardown: ~radLtClean
);

~radTest.("csload-glide",
	setup: ~radLtChain,
	cmds: [["csload t 1 radtest_ltc glide", 4]],
	check: {
		var fades = ~radLtFades.(~radLtStrip1);
		case
		{ ~radLtRatio.() != 0.5 } { "pitchRatio " ++ ~radLtRatio.() }
		{ ~radLtLag.(\filterTrack_1_1, \pitchRatio).notNil } { "lag after: " ++ ~radLtLag.(\filterTrack_1_1, \pitchRatio).cs }
		{ fades.any(_ != 0.5) } { "fade times after: " ++ fades.cs }
		{ true }
	},
	teardown: ~radLtClean
);

// ---- tload ----

// track 2 saved with an effect value and a volume; track 1 has the same
// effect with another value
~radLtTrack = ["start 2", "fx t 2 pch", "fxset t 2 1 pitchRatio 0.5", "vol t 2 -6", "tsave t 2 radtest_ltt",
	"fx t 1 pch", "fxset t 1 1 pitchRatio 2"];

// by default: the effect and the volume crossfade (one xset each), over the
// strip's own fade time; no fade time lines
~radTest.("tload-default-transition",
	setup: ~radLtTrack,
	cmds: [["tload t 1 radtest_ltt", 3]],
	check: {
		var one = ~radLtStripNow.(\track1), fades = ~radLtFades.(~radLtStrip1);
		case
		{ ~radLtRatio.() != 0.5 } { "pitchRatio " ++ ~radLtRatio.() }
		{ one[\volume] != -6 or: { one[\lags].notNil } } { "track1: " ++ one.cs }
		{ fades.any(_ != 0.5) } { "fade times after: " ++ fades.cs }
		{ true }
	},
	teardown: ~radLtClean
);

// a typed time, with nofx, the words in any order: the volume crossfades
// over it (the strip's fade time is the load's meanwhile, its volume lag is
// not touched), the effects are left alone
~radTest.("tload-typed-time-nofx-any-order",
	setup: ~radLtTrack,
	code: {
		Radicles.cW.callFunc("tload t 1 radtest_ltt nofx 2 xfade"); 0.8.wait;
		~radLtMid = [Ndef(\track1).fadeTime, Ndef(\track1).get(\lagTime) ? 0]; 3.wait },
	check: {
		var one = ~radLtStripNow.(\track1);
		case
		{ ~radLtMid != [2, 0] } { "track1's fade time and volume lag during the load: " ++ ~radLtMid }
		{ Ndef(\track1).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\track1).fadeTime }
		{ ~radLtRatio.() != 2 } { "nofx: pitchRatio " ++ ~radLtRatio.() }
		{ one[\volume] != -6 or: { one[\lags].notNil } } { "track1: " ++ one.cs }
		{ true }
	},
	teardown: ~radLtClean
);

~radTest.("tload-none-and-zero",
	setup: ~radLtTrack,
	cmds: [["tload t 1 radtest_ltt none", 3], ["vol t 1 0", 1], ["fxset t 1 1 pitchRatio 2", 1], ["tload t 1 radtest_ltt 0", 3]],
	check: {
		var one = ~radLtStripNow.(\track1);
		(~radLtRatio.() == 0.5 and: { one[\volume] == -6 } and: { one[\lags].isNil })
		or: { "pitchRatio " ++ ~radLtRatio.() ++ ", track1: " ++ one.cs }
	},
	teardown: ~radLtClean
);

~radTest.("tload-glide",
	setup: ~radLtTrack,
	cmds: [["tload t 1 radtest_ltt 1 glide", 5]],
	check: {
		var one = ~radLtStripNow.(\track1), fades = ~radLtFades.(~radLtStrip1);
		case
		{ ~radLtRatio.() != 0.5 } { "pitchRatio " ++ ~radLtRatio.() }
		{ ~radLtLag.(\filterTrack_1_1, \pitchRatio).notNil } { "lag after: " ++ ~radLtLag.(\filterTrack_1_1, \pitchRatio).cs }
		{ one[\volume] != -6 or: { one[\lags].notNil } } { "track1: " ++ one.cs }
		{ fades.any(_ != 0.5) } { "fade times after: " ++ fades.cs }
		{ true }
	},
	teardown: ~radLtClean
);

// ---- a morphTime in an older preset file ----

// effect, block, chain and track presets written with a morphTime (as
// before 19 Nov 2026) load without a warning; the time is ignored (each
// load takes the fade time, 0.5 s) and nothing keeps it
~radTest.("old-morph-time-ignored",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 2", "blk 1 sine", "blkset 1 freq 330", "fx t 2 pch"],
	code: {
		var fx, blk, chain, track;
		fx = RadUnit.read(\filterTrack_1_1).deepCopy;
		fx[\args][\pitchRatio] = 0.5; fx[\morphTime] = 7;
		RadUnit.writePreset(fx, \radtest_ltold);
		blk = RadUnit.readBlock(\block1).deepCopy;
		blk[\args][\freq] = 500; blk[\morphTime] = 7;
		RadUnit.writePreset(blk, \radtest_ltold);
		chain = RadUnit.readChain(\track, 2).deepCopy;
		chain[\fx][1][\args][\pitchRatio] = 0.25; chain[\fx][1][\morphTime] = 7; chain[\morphTime] = 7;
		RadUnit.writeChainPreset(chain, \radtest_ltcold);
		track = RadUnit.readTrack(\track, 2).deepCopy;
		track[\volume] = -9; track[\morphTime] = 7;
		track[\chain][\fx][1][\args][\pitchRatio] = 0.75;
		RadUnit.writeTrackPreset(track, \radtest_lttold);
		~radLtFiles = [RadUnit.readPreset(\filter, \pch, \radtest_ltold)[\morphTime],
			RadUnit.readPreset(\block, \sine, \radtest_ltold)[\morphTime],
			RadUnit.readChainPreset(\radtest_ltcold)[\morphTime], RadUnit.readTrackPreset(\radtest_lttold)[\morphTime]];
		Radicles.cW.callFunc("fxload t 1 1 radtest_ltold");
		Radicles.cW.callFunc("blkload 1 radtest_ltold");
		0.2.wait;
		~radLtMid = ~radLtFades.([\filterTrack_1_1, \block1]);
		1.8.wait;
		~radLtFirst = [~radLtRatio.(), Ndef(\block1).get(\freq)];
		Radicles.cW.callFunc("csload t 2 radtest_ltcold"); 2.wait;
		~radLtSecond = Ndef(\filterTrack_2_1).get(\pitchRatio);
		Radicles.cW.callFunc("tload t 2 radtest_lttold"); 3.wait;
	},
	check: {
		var two = ~radLtStripNow.(\track2);
		var fades = ~radLtFades.([\filterTrack_1_1, \block1, \filterTrack_2_1, \track2]);
		case
		{ ~radLtFiles != [7, 7, 7, 7] } { "the files' morphTime: " ++ ~radLtFiles.cs }
		{ ~radLtMid != [0.5, 0.5] } { "fade times during the loads: " ++ ~radLtMid.cs }
		{ ~radLtFirst != [0.5, 500] } { "fxload, blkload: " ++ ~radLtFirst.cs }
		{ ~radLtSecond != 0.25 } { "csload: pitchRatio " ++ ~radLtSecond }
		{ two[\volume] != -9 or: { Ndef(\filterTrack_2_1).get(\pitchRatio) != 0.75 } } { "tload: " ++ two.cs }
		{ fades.any(_ != 0.5) } { "fade times after: " ++ fades.cs }
		{ [RadUnit.read(\filterTrack_1_1), RadUnit.readBlock(\block1), RadUnit.read(\filterTrack_2_1), two,
			RadUnit.readChain(\track, 2), RadUnit.readTrack(\track, 2)].any{|unit| unit.includesKey(\morphTime) } } {
			"a morphTime was kept" }
		{ two.includesKey(\chainMorphTime) or: { two.includesKey(\trackMorphTime) } } {
			"the document keeps a morph time: " ++ two.cs }
		{ true }
	},
	noWarnings: true,
	teardown: ~radLtClean
);

// ---- the morph words are gone ----

// nothing changes (each is an unknown command)
~radTest.("morph-words-removed",
	setup: ~radLtFx ++ ["blk 1 sine", "blkset 1 freq 500", "blksave 1 radtest_lt", "blkset 1 freq 330",
		"cssave t 2 radtest_ltc", "tsave t 2 radtest_ltt"],
	cmds: ["fxmorph t 1 1 radtest_lt", "fxmorph t 1 1 radtest_lt 1 glide", "blkmorph 1 radtest_lt", "csmorph t 1 radtest_ltc",
		"tmorph t 1 radtest_ltt", "morphtime 5", "morphtime t 1 1 5", "blkmorphtime 1 5", "csmorphtime t 1 5",
		"tmorphtime t 1 5"],
	check: {
		var fades = ~radLtFades.(~radLtStrip1 ++ [\block1]);
		case
		{ ~radLtRatio.() != 2 } { "pitchRatio " ++ ~radLtRatio.() }
		{ Ndef(\block1).get(\freq) != 330 } { "freq " ++ Ndef(\block1).get(\freq) }
		{ (RadUnit.readChain(\track, 1)[\fx] ? ()).size != 1 } { "track 1's chain: " ++ RadUnit.readChain(\track, 1).cs }
		{ fades.any(_ != 0.5) } { "fade times: " ++ fades.cs }
		{ true }
	},
	teardown: ~radLtClean
);

// ---- one load during another, fade during a load, a block stopped or removed ----
// (found by the second look, 19 Nov 2026)

// a second load typed while a timed one runs, without a time of its own:
// over the effect's own fade time, not the first load's
~radTest.("fxload-during-timed-load-uses-own-fade",
	setup: ~radLtFx ++ ["fxset t 1 1 pitchRatio 0.25", "fxsave t 1 1 radtest_lt2", "fxset t 1 1 pitchRatio 2"],
	code: {
		Radicles.cW.callFunc("fxload t 1 1 radtest_lt 6"); 1.wait;
		Radicles.cW.callFunc("fxload t 1 1 radtest_lt2"); 0.4.wait;
		~radLtMid = Ndef(\filterTrack_1_1).fadeTime; 1.5.wait;
	},
	check: {
		case
		{ ~radLtMid != 0.5 } { "fade time during the second load: " ++ ~radLtMid }
		{ ~radLtRatio.() != 0.25 } { "pitchRatio " ++ ~radLtRatio.() }
		{ Ndef(\filterTrack_1_1).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\filterTrack_1_1).fadeTime }
		{ true }
	},
	teardown: ~radLtClean
);

// the same for a block
~radTest.("blkload-during-timed-load-uses-own-fade",
	setup: ~radLtBlk ++ ["blkset 1 freq 700", "blksave 1 radtest_lt2", "blkset 1 freq 330"],
	code: {
		Radicles.cW.callFunc("blkload 1 radtest_lt 6"); 1.wait;
		Radicles.cW.callFunc("blkload 1 radtest_lt2"); 0.4.wait;
		~radLtMid = Ndef(\block1).fadeTime; 1.5.wait;
	},
	check: {
		case
		{ ~radLtMid != 0.5 } { "fade time during the second load: " ++ ~radLtMid }
		{ Ndef(\block1).get(\freq) != 700 } { "freq " ++ Ndef(\block1).get(\freq) }
		{ Ndef(\block1).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\block1).fadeTime }
		{ true }
	},
	teardown: ~radLtClean
);

// fade N typed while a timed load runs stays when the load ends
~radTest.("fade-during-fxload-stays",
	setup: ~radLtFx,
	cmds: [["fxload t 1 1 radtest_lt 2", 0.5], ["fade 4", 3]],
	check: {
		(Ndef(\filterTrack_1_1).fadeTime == 4 and: { ~radLtRatio.() == 0.5 })
		or: { "fade " ++ Ndef(\filterTrack_1_1).fadeTime ++ ", pitchRatio " ++ ~radLtRatio.() }
	},
	teardown: { Radicles.setFade(0.5); ~radLtClean.() }
);

// fade t 1 3 typed while a chain load runs stays on the strip; the effect,
// which was not given one, goes back to its own
~radTest.("fade-strip-during-csload-stays",
	setup: ~radLtChain,
	cmds: [["csload t 1 radtest_ltc 2", 0.5], ["fade t 1 3", 3]],
	check: {
		var fades = ~radLtFades.(~radLtStrip1);
		(fades == [3, 0.5, 3, 3]) or: { "fade times: " ++ fades.cs }
	},
	teardown: ~radLtClean
);

// a block removed while a load runs on it: the load ends there, nothing is
// posted on the block afterwards
~radTest.("blk-rm-during-blkload",
	setup: ~radLtBlk,
	cmds: [["blkload 1 radtest_lt 3 glide", 1], ["blk rm 1", 4]],
	check: { Block.ndefOf(1).isNil or: { "block 1 is still there" } },
	teardown: ~radLtClean
);

// a block stopped while a timed load runs on it fades over its own fade time
~radTest.("blk-stop-during-blkload",
	setup: ~radLtBlk,
	cmds: [["blkload 1 radtest_lt 6", 1], ["blk stop 1", 2]],
	check: { (Ndef(\block1).fadeTime == 0.5) or: { "fade time: " ++ Ndef(\block1).fadeTime } },
	teardown: ~radLtClean
);

// glide in a chain load: the values start moving after the effect's own
// fade time, not after the load's time (they arrived at twice the time)
~radTest.("csload-glide-starts-at-once",
	setup: ~radLtChain,
	code: { Radicles.cW.callFunc("csload t 1 radtest_ltc 3 glide"); 1.5.wait; ~radLtMid = ~radLtRatio.(); 3.5.wait },
	check: {
		var fades = ~radLtFades.(~radLtStrip1);
		case
		{ ~radLtMid != 0.5 } { "pitchRatio 1.5 s into the load: " ++ ~radLtMid }
		{ ~radLtLag.(\filterTrack_1_1, \pitchRatio).notNil } { "lag after: " ++ ~radLtLag.(\filterTrack_1_1, \pitchRatio).cs }
		{ fades.any(_ != 0.5) } { "fade times after: " ++ fades.cs }
		{ true }
	},
	teardown: ~radLtClean
);

// a track load that also changes the chain: the pan starts moving with the
// volume, once the chain is built, not after the load's time
~radTest.("tload-pan-with-chain-starts-at-once",
	setup: ["start 2", "fx t 2 pch", "fxset t 2 1 pitchRatio 0.5", "pan t 2 -0.5", "vol t 2 -6", "tsave t 2 radtest_ltt"],
	code: { Radicles.cW.callFunc("tload t 1 radtest_ltt 3"); 1.8.wait; ~radLtMid = Ndef(\spaceTrack1).get(\pan); 3.5.wait },
	check: {
		var one = ~radLtStripNow.(\track1), fades = ~radLtFades.(~radLtStrip1);
		case
		{ ~radLtMid != -0.5 } { "pan 1.8 s into the load: " ++ ~radLtMid }
		{ one[\volume] != -6 or: { one[\lags].notNil } } { "track1: " ++ one.cs }
		{ fades.any(_ != 0.5) } { "fade times after: " ++ fades.cs }
		{ true }
	},
	teardown: ~radLtClean
);

// ---- the strip's values crossfade; a whole track fades out or in ----
// (19 Nov 2026: a lag moves fast and then creeps, so the level arrived long
// before the effects and the source)

// trim, volume and pan each crossfade with one xset; the strip's stages take
// the load's time and get their own back; no lag is set
~radTest.("tload-strip-values-crossfade",
	setup: ["start 2", "vol t 2 -6", "pan t 2 -0.5", "trim t 2 -3", "tsave t 2 radtest_ltt"],
	cmds: [["tload t 1 radtest_ltt 1", 4]],
	check: {
		var one = ~radLtStripNow.(\track1), fades = ~radLtFades.([\inTrack1, \track1, \spaceTrack1]);
		case
		{ one[\volume] != -6 or: { one[\trim] != -3 } or: { one[\lags].notNil } } { "track1: " ++ one.cs }
		{ Ndef(\spaceTrack1).get(\pan) != -0.5 } { "pan " ++ Ndef(\spaceTrack1).get(\pan) }
		{ fades.any(_ != 0.5) } { "fade times after: " ++ fades.cs }
		{ true }
	},
	teardown: ~radLtClean
);

// with glide they glide as before: a lag over the time, then it goes back
~radTest.("tload-strip-values-glide",
	setup: ["start 2", "vol t 2 -6", "tsave t 2 radtest_ltt"],
	code: {
		Radicles.cW.callFunc("tload t 1 radtest_ltt 2 glide"); 0.8.wait;
		~radLtMid = [Ndef(\track1).fadeTime, Ndef(\track1).get(\lagTime) ? 0]; 3.wait },
	check: {
		var one = ~radLtStripNow.(\track1);
		case
		{ ~radLtMid != [0.5, 2] } { "track1's fade time and volume lag during the load: " ++ ~radLtMid }
		{ one[\volume] != -6 or: { one[\lags].notNil } } { "track1: " ++ one.cs }
		{ true }
	},
	teardown: ~radLtClean
);

// a track that ends with no source fades out whole: one xset of the volume
// to silence over the time, then the block and the effect go and the
// preset's values are set
~radLtWhole = ["start 2", "blk 1 sine", "fx t 1 pch", "vol t 1 -6", "pan t 1 -0.5"];
~radLtFx1 = { Radicles.aZ.findFilterTag(\track, 1, 1).notNil };
~radTest.("tload-default-fades-out-whole",
	setup: ~radLtWhole,
	cmds: [["tload t 1 default 1", 5]],
	check: {
		var one = ~radLtStripNow.(\track1);
		case
		{ ~radLtFx1.() } { "the effect is still there" }
		{ Ndef(\block1).source.notNil } { "block 1 still plays" }
		{ one[\volume] != 0 or: { one[\lags].notNil } } { "track1: " ++ one.cs }
		{ Ndef(\spaceTrack1).get(\pan) != 0 } { "pan " ++ Ndef(\spaceTrack1).get(\pan) }
		{ Ndef(\track1).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\track1).fadeTime }
		{ true }
	}
);

// during the fade the effect and the block are still there
~radTest.("tload-default-keeps-effects-while-fading",
	setup: ~radLtWhole,
	code: {
		Radicles.cW.callFunc("tload t 1 default 3"); 1.5.wait;
		~radLtMid = [~radLtFx1.(), Ndef(\block1).source.notNil, Ndef(\track1).get(\volume), Ndef(\track1).fadeTime,
			Ndef(\spaceTrack1).get(\pan)];
		4.wait },
	check: {
		case
		{ ~radLtMid != [true, true, -inf, 3, -0.5] } { "1.5 s into the load (effect, block, volume, fade time, pan): " ++ ~radLtMid.cs }
		{ ~radLtFx1.() or: { Ndef(\block1).source.notNil } } { "not emptied at the end" }
		{ Ndef(\track1).get(\volume) != 0 } { "volume after: " ++ Ndef(\track1).get(\volume) }
		{ true }
	}
);

// with nosrc the source keeps playing: the track does not fade out, the
// effect goes as in any chain load
~radTest.("tload-default-nosrc-does-not-fade-out",
	setup: ~radLtWhole,
	code: {
		Radicles.cW.callFunc("tload t 1 default 3 nosrc"); 1.5.wait;
		~radLtMid = Ndef(\track1).get(\volume);
		4.wait },
	check: {
		case
		{ ~radLtMid != 0 } { "volume 1.5 s into the load: " ++ ~radLtMid }
		{ Ndef(\block1).source.isNil } { "block 1 stopped" }
		{ ~radLtFx1.() } { "the effect is still there" }
		{ true }
	}
);

// a track with no source that gets one is built at once with its volume at
// silence, then the volume crossfades up over the time
~radTest.("tload-onto-empty-fades-in-whole",
	setup: ~radLtWhole ++ ["tsave t 1 radtest_ltt"],
	cmds: [["tload t 2 radtest_ltt 1", 5]],
	check: {
		var two = ~radLtStripNow.(\track2);
		case
		{ Radicles.aZ.findFilterTag(\track, 2, 1).isNil } { "no effect on track 2" }
		{ (RadUnit.readInput(\track2, Radicles.aZ) ? []).isEmpty } { "nothing feeds track 2" }
		{ two[\volume] != -6 or: { two[\lags].notNil } } { "track2: " ++ two.cs }
		{ Ndef(\track2).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\track2).fadeTime }
		{ true }
	},
	teardown: ~radLtClean
);

// the volume is at silence while the track is built, and on its way up after
~radTest.("tload-onto-empty-silent-while-built",
	setup: ~radLtWhole ++ ["tsave t 1 radtest_ltt"],
	code: {
		Radicles.cW.callFunc("tload t 2 radtest_ltt 3"); 0.15.wait;
		~radLtMid = [Ndef(\track2).get(\volume)];
		1.6.wait;
		~radLtMid = ~radLtMid ++ [Ndef(\track2).get(\volume), Ndef(\track2).fadeTime];
		4.wait },
	check: {
		case
		{ ~radLtMid != [-inf, -6, 3] } { "volume at the start, then volume and fade time during the fade: " ++ ~radLtMid.cs }
		{ Ndef(\track2).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\track2).fadeTime }
		{ true }
	},
	teardown: ~radLtClean
);

// another load during a whole-track fade-out stops it and puts the fade
// time back
~radTest.("tload-during-fade-out",
	setup: ~radLtWhole ++ ["tsave t 1 radtest_ltt"],
	code: {
		Radicles.cW.callFunc("tload t 1 default 4"); 1.wait;
		Radicles.cW.callFunc("tload t 1 radtest_ltt none"); 3.wait },
	check: {
		var one = ~radLtStripNow.(\track1);
		case
		{ Ndef(\track1).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\track1).fadeTime }
		{ one[\volume] != -6 } { "track1: " ++ one.cs }
		{ ~radLtFx1.().not or: { Ndef(\block1).source.isNil } } { "the effect or the block went" }
		{ true }
	},
	teardown: ~radLtClean
);
