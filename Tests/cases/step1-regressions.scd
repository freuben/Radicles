// Step 1 regression tests: one per bug fixed in October 2026 (PRs #32, #34, #35, #36).
// Each test repeats the reproduction steps from its GitHub issue, so the bug
// cannot come back unnoticed. Names start with the bug number from the review.
// Presets and files created here are named radtest_* and removed by teardown.

var a = { Radicles.aZ };

// Bug 1 (#4): clearing a send while a modulator is active called a missing method.
~radTest.("b01-clear-send-with-modulator",
	setup: ["start 2", "snd t 1 1", "vol t 1 sin"],
	cmds: ["snd t 1 1 0"],
	noWarnings: true
);

// Bug 3 (#6): with no buses, tracks could not be added (and the mixer could not open).
~radTest.("b03-add-track-without-buses",
	setup: ["start 2 0"],
	cmds: ["t add"],
	check: {
		var envir = Ndef.dictFor(Server.default).envir;
		(envir['track3'].notNil and: { envir['track3'].source.notNil }) or: { "track3 was not created" }
	}
);
~radTest.("b03-mixer-without-buses",
	setup: ["start 2 0"],
	cmds: ["mix"],
	check: { a.().mixerWin.notNil or: { "mixer did not open" } },
	gui: true
);

// Bug 4 (#7): save failed if the mixer had never been opened.
~radTest.("b04-save-without-mixer",
	setup: ["start 2 0"],
	cmds: ["save radtest_b4"],
	check: { PresetFile.read(\radicles, 'radtest_b4').notNil or: { "project not saved" } },
	teardown: { PresetFile.remove(\radicles, 'radtest_b4', false, path: 0, post: false) }
);

// Bug 5 (#8): track 1 matched track 10, so presets picked up track 10's effects.
~radTest.("b05-track1-preset-ignores-track10",
	setup: ["start 11 1", "fx t 1 pch", "fx t 10 pch"],
	cmds: ["cssave t 1 radtest_b5"],
	check: {
		var p = PresetFile.read(\track, 'radtest_b5').cs;
		(p.contains("filterTrack_1_1") and: { p.contains("filterTrack_10").not }) or: { "preset: " ++ p.keep(200) }
	},
	teardown: { PresetFile.remove(\track, 'radtest_b5', false, path: 0, post: false) }
);

// Bug 6 (#9): effect slots sorted as text, so slot 10 came before slot 2.
~radTest.("b06-slot-10-after-slot-2",
	setup: ["start 4 2"],
	cmds: ["fx t 3 2 pch", "fx t 3 10 pch"],
	check: {
		var tags = a.().filters.flop[0];
		(tags.indexOf('filterTrack_3_2') < tags.indexOf('filterTrack_3_10')) or: { "order: " ++ tags.cs }
	}
);

// Bug 7 (#10): with the mixer closed, clearing a send warned "Bus not found" and left it on.
~radTest.("b07-clear-send-mixer-closed",
	setup: ["start 4 2", "snd t 1 1 1"],
	cmds: ["snd t 1 1 0"],
	noWarnings: true
);

// Bug 9 (#12): presets never restored solo. GUI only for now: loading a mixer
// preset rebuilds the mixer GUI even when the mixer is closed (open question).
~radTest.("b09-preset-restores-solo",
	setup: ["start 2", "mix", "solo t 1"],
	gui: true,
	code: {
		a.().writeTracksPreset('radtest_b9');
		1.wait;
		Radicles.cW.callFunc("unsolo t 1");
		1.wait;
		a.().loadTracksPreset('radtest_b9');
		5.wait;
	},
	check: { (a.().soloStates[0] == 1) or: { "soloStates: " ++ a.().soloStates.cs } },
	teardown: { PresetFile.remove(\tracks, 'radtest_b9', false, path: 0, post: false) }
);

// Bug 10 (#13): pan on a track with an effect failed while the mixer was open.
~radTest.("b10-pan-with-effect-mixer-open",
	setup: ["start 2", "mix", "fx t 1 pch"],
	cmds: ["pan t 1 0.5"],
	gui: true
);

// Bug 11 (#14): loading a track preset over a track with effects ran the next
// step before the old effects were cleaned up.
~radTest.("b11-load-preset-over-effects",
	setup: ["start 4 2", "fx t 1 pch", "cssave t 1 radtest_b11", "fx t 3 pch"],
	cmds: [["csload t 3 radtest_b11", 6]],
	check: {
		var tags = a.().filters.flop[0];
		(tags.select{|t| t.asString.beginsWith("filterTrack_3_") }.size == 1) or: { "filters: " ++ a.().filters.cs }
	},
	teardown: { PresetFile.remove(\track, 'radtest_b11', false, path: 0, post: false) }
);

// Bug 12 (#15): unmodulating a parameter matched a modulator on a different effect.
// The second command unmodulates by name, which failed until 26 Oct (_BasicAt).
~radTest.("b12-unmod-only-matching-effect",
	setup: ["start 2", "fx t 1 pch", "fx t 2 pch", "fxset t 1 1 pitchRatio sin", "fxset t 2 1 pitchDispertion sin"],
	cmds: ["unmodfx t 1 1 pitchDispertion", "unmodfx t 1 1 pitchRatio"],
	check: { ModMap.modNodes.any{|m| m[1] == Ndef('filterTrack_1_1') }.not or: { "track 1 still modulated" } }
);

// unmodfx by argument number (the other documented form).
~radTest.("unmod-by-number",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 1 sin"],
	cmds: ["unmodfx t 1 1 1"],
	check: { ModMap.modNodes.any{|m| m[1] == Ndef('filterTrack_1_1') }.not or: { "track 1 still modulated" } }
);

// Bug 13 (#16): saving a mixer preset failed after a bus had been removed.
~radTest.("b13-preset-after-bus-removed",
	setup: ["start 2 2", "b rmv"],
	code: { a.().writeTracksPreset('radtest_b13'); 1.wait },
	check: { PresetFile.read(\tracks, 'radtest_b13').notNil or: { "preset not saved" } },
	teardown: { PresetFile.remove(\tracks, 'radtest_b13', false, path: 0, post: false) }
);

// Bug 14 (#17) and the history folder path: replacing a history did nothing,
// and history was written to a misspelled folder.
~radTest.("b14-history-replace",
	setup: ["start 2"],
	code: {
		var path = Radicles.filesPath +/+ "Settings/CallWindow/History/radtest_b14.scd";
		Radicles.cW.startHistory("radtest_b14", true);
		Radicles.cW.callFunc("vol t 1 -3");
		1.wait;
		Radicles.cW.stopHistory;
		Radicles.cW.startHistory("radtest_b14", true);
		0.5.wait;
		~b14 = [File.exists(path), File.fileSize(path), Radicles.cW.recordHistory];
		Radicles.cW.stopHistory;
	},
	check: { (~b14 == [true, 0, true]) or: { "[exists, size, recording]: " ++ ~b14.cs } },
	teardown: { File.delete(Radicles.filesPath +/+ "Settings/CallWindow/History/radtest_b14.scd") }
);

// Bug 15 (#18): setVolume with db: false. The Ndef must get the dB value.
~radTest.("b15-set-volume-amplitude",
	setup: ["start 2"],
	code: { a.().setVolume(\track, 1, 0.5, db: false) },
	check: {
		var v = Ndef('track1').get('volume');
		((v - 0.5.ampdb).abs < 0.001) or: { "volume is " ++ v }
	}
);

// #33: PresetFile.remove without a post argument failed with "Non Boolean in test".
~radTest.("i33-preset-remove-default-post",
	code: {
		PresetFile.write(\track, 'radtest_i33', [\track, []], false, post: false);
		PresetFile.remove(\track, 'radtest_i33', false, path: 0);
	},
	check: { PresetFile.read(\track, 'radtest_i33').isNil or: { "preset still there" } },
	teardown: { PresetFile.remove(\track, 'radtest_i33', false, path: 0, post: false) }
);
