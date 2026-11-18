// The call window's parser: how a typed word becomes a number, a range, an
// array or a name.

~radParserClean = {
	(RadUnit.presetRoot +/+ "filter/*/radtest_*.scd").pathMatch.do{|path| File.delete(path) };
};

// a name with a dash is a name: it was taken for a range (warm-shift ->
// [warm, shift]) and the command failed (review, 18 Nov 2026)
~radTest.("dash-name-preset",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5"],
	cmds: ["fxsave t 1 1 radtest_warm-shift", "fxset t 1 1 pitchRatio 2", ["fxload t 1 1 radtest_warm-shift", 3]],
	check: {
		case
		{ File.exists(RadUnit.presetPath(\filter, \pch, 'radtest_warm-shift')).not } { "the preset was not saved" }
		{ Ndef(\filterTrack_1_1).get(\pitchRatio) != 0.5 } { "pitchRatio " ++ Ndef(\filterTrack_1_1).get(\pitchRatio) }
		{ true }
	},
	teardown: ~radParserClean
);

// a name with a dash and digits (kick-01), and one that starts with a digit
~radTest.("dash-name-digits",
	setup: ["start 2", "fx t 1 pch"],
	cmds: ["fxsave t 1 1 radtest_kick-01", "fxsave t 1 1 radtest_2-b"],
	check: {
		['radtest_kick-01', 'radtest_2-b'].every{|name| File.exists(RadUnit.presetPath(\filter, \pch, name)) }
		or: { "presets: " ++ RadUnit.presetNames(\filter, \pch).cs }
	},
	teardown: ~radParserClean
);

// two whole numbers with a dash are still a range, and a dash at the start
// is still a negative number
~radTest.("dash-range",
	setup: ["start 3 2"],
	cmds: ["vol 1-3 -6", "vol b 1-2 -inf", "trim t 2-3 4"],
	check: {
		var vols = [\track1, \track2, \track3, \bus1, \bus2].collect{|key| Ndef(key).get(\volume) };
		var trims = [\inTrack1, \inTrack2, \inTrack3].collect{|key| Ndef(key).get(\trim) ? 0 };
		case
		{ vols != [-6, -6, -6, -inf, -inf] } { "volumes: " ++ vols.cs }
		{ trims != [0, 4, 4] } { "trims: " ++ trims.cs }
		{ true }
	}
);
