// Examples from the teaching handout "Radicles, Mixer Commands, Recording,
// manipulating" (Jan 2020), in handout order. Where the handout's wording
// differs from the help file, the handout's form is tested as written.

var vol = {|key| Ndef(key).get('volume') };

// Two ways of addressing tracks
~radTest.("vol-by-type", setup: ["start 2 2"], cmds: ["vol t 1 -10"],
	check: { (vol.('track1') == -10) or: { "track1 volume " ++ vol.('track1') } });
~radTest.("pan-by-type", setup: ["start 2 2"], cmds: ["pan b 2 -0.5"]);
~radTest.("vol-by-position", setup: ["start 2 2"], cmds: ["vol 4 -3"],
	check: { (vol.('bus2') == -3) or: { "bus2 volume " ++ vol.('bus2') } });

// Ranges and arrays
~radTest.("ranges-and-arrays",
	setup: ["start 5 2"],
	cmds: ["vol b 1-2 -inf", "pan 1-4 -0.5", "vol t [3,5] -10", "trim [1,3,5] 5"],
	check: {
		var ok = (vol.('bus1') == -inf) and: { vol.('bus2') == -inf }
			and: { vol.('track3') == -10 } and: { vol.('track5') == -10 };
		ok or: { "volumes: " ++ [vol.('bus1'), vol.('bus2'), vol.('track3'), vol.('track5')].cs }
	}
);

// Lags
~radTest.("vol-lag",
	setup: ["start 2"],
	cmds: ["vollag 1 10", "vol 1 -inf", "vol 1 0", "vollag 1 0", "vol 1 -inf"]
);

// Effects
~radTest.("list-effects", setup: ["start 2"], cmds: ["fxs"]);
~radTest.("add-effects",
	setup: ["start 2 2"],
	cmds: ["fx b 1 pch", "fx 1 pch", "fx 1 2 dl1", "fx b 1 2 harm"],
	check: {
		var tags = Radicles.aZ.filters.flop[0];
		['filterBus_1_1', 'filterTrack_1_1', 'filterTrack_1_2', 'filterBus_1_2'].every{|t| tags.includes(t) }
			or: { "filters: " ++ Radicles.aZ.filters.cs }
	}
);

~radTest.("fxset-query-and-set",
	setup: ["start 2 2", "fx 1 pch", "fx b 1 pch"],
	cmds: ["fxset 1 1", "fxset b 1 1", "fxset 1 1 1", "fxset 1 1 1 0.5", "fxset b 1 1 2",
		"fxset 1 1 pitchRatio 0.5", "fxset b 1 pitchDispertion 2"],
	check: { (Ndef('filterTrack_1_1').get('pitchRatio') == 0.5) or: { "pitchRatio " ++ Ndef('filterTrack_1_1').get('pitchRatio') } }
);

~radTest.("setfx-by-order",
	setup: ["start 2", "fx 1 pch"],
	cmds: ["setfx", "setfx 1", "setfx 1 1", "setfx 1 1 1 0.5", "setfx 1 1 pitchRatio 2"],
	noWarnings: true,
	known: "outdated handout: setting is now setfx 1 1 0.5 / setfx 1 pitchRatio 2"
);
~radTest.("setfx-by-order-current",
	setup: ["start 2", "fx 1 pch"],
	cmds: ["setfx 1 1 0.5", "setfx 1 pitchRatio 2"],
	noWarnings: true,
	check: { (Ndef('filterTrack_1_1').get('pitchRatio') == 2) or: { "pitchRatio " ++ Ndef('filterTrack_1_1').get('pitchRatio') } }
);

~radTest.("fx-lags",
	setup: ["start 2", "fx t 1 pch", "fx 1 2 dl1"],
	cmds: ["fxlag t 1 10", "fxlag 1 2 5", "fxlagn t 1 2 10"],
	noWarnings: true,
	known: "outdated handout: fxlag t 1 10 now needs a slot (fxlag t 1 1 10)"
);
~radTest.("fx-lags-current",
	setup: ["start 2", "fx t 1 pch", "fx 1 2 dl1"],
	cmds: ["fxlag t 1 1 10", "fxlag 1 2 5", "fxlagn t 1 2 10"],
	noWarnings: true
);

~radTest.("fx-clear",
	setup: ["start 2", "fx t 1 pch", "fx 2 2 dl1", "fx 2 3 pch"],
	cmds: ["fxclear t 1", "fxclear 2 2", "clear fx 1"],
	noWarnings: true,
	known: "outdated handout: clear fx 1 is now clearfx 1"
);
~radTest.("fx-clear-current",
	setup: ["start 2", "fx t 1 pch", "fx 2 2 dl1", "fx 2 3 pch"],
	cmds: ["fxclear t 1", "fxclear 2 2", "clearfx 1"],
	noWarnings: true,
	check: { (Radicles.aZ.filters.isNil or: { Radicles.aZ.filters.isEmpty }) or: { "filters left: " ++ Radicles.aZ.filters.cs } }
);

// Sends
~radTest.("sends",
	setup: ["start 2 2"],
	cmds: ["snd t 1 1", "snd t 1 2 2", "snd t 1 2 2 -10", "snd 2 1", "snd 2 2 2", "snd 2 2 2 -10"]
);
~radTest.("sends-dry",
	setup: ["start 2 2"],
	cmds: ["sndry t 2 2 2", "sndry 2 2 2", "sndry 2 2 2 -10"]
);
~radTest.("set-sends",
	setup: ["start 2 2", "snd t 1 1", "snd t 2 2 2"],
	cmds: ["setsnd t 1 0", "setsnd t 1 1 0", "setsnd t 1 1 0 1", "setsnd 2 2 -10", "setsnd 2 2 -10 1"],
	noWarnings: true,
	known: "outdated handout and help file: setsnd is now sndset"
);
~radTest.("set-sends-current",
	setup: ["start 2 2", "snd t 1 1", "snd t 2 2 2"],
	cmds: ["sndset t 1 0", "sndset t 1 1 0", "sndset t 1 1 0 1", "sndset 2 2 -10", "sndset 2 2 -10 1"],
	noWarnings: true
);

// Buffers
~radTest.("buffer-create",
	setup: ["start 2"],
	cmds: ["@", "@ 2", "@ 2 2", "@pv 2", "@pv 2 2", "@n 2 2", "@n 2 2 2", "@npv 2 2", "@npv 2 2 2", "@"]
);
~radTest.("buffer-record",
	setup: ["start 2", "@ 2"],
	cmds: ["sr @1", "sr @1 2", "sr @1 2 0.5 0.5", "sr @1 t 1", "sr @1 t 2 0.5 0.5"]
);
~radTest.("buffer-loop-record-overdub",
	setup: ["start 2", "@ 2"],
	cmds: ["lr @1", "lr @1 2", "lr @1 2 0.5 0.5", "or @1", "or @1 2", "ol @1", "ol @1 2"]
);
~radTest.("buffer-play",
	setup: ["start 2", "@ 2"],
	cmds: ["lp 1 @1", "pl 2 @1", "lp 1 @1 0.5", "pl 2 @1 2"]
);
~radTest.("buffer-play-pv",
	setup: ["start 2", "@pv 2"],
	cmds: ["lppv 1 @1", "plpv 2 @1", "lppv 1 @1 0.5", "plpv 2 @1 2"]
);
~radTest.("buffer-free", setup: ["start 2", "@ 2"], cmds: ["fr @1"]);
~radTest.("buffer-timed-record",
	setup: ["start 2", "@ 2"],
	cmds: ["tsr @1", "tsr @1 2", "tsr @1 2 0.5 0.5", "tsr @1 t 1", "tsr @1 t 2 0.5 0.5",
		"tlr @1", "tlr @1 2", "tlr @1 2 0.5 0.5", "tor @1", "tor @1 2", "tol @1", "tol @1 2"]
);
~radTest.("buffer-timed-loop",
	setup: ["start 2", "@ 2", ["tsr @1", 1]],
	cmds: ["lt 1", "lt 2"]
);

// Clock and scheduling
~radTest.("clock-bpm-beats",
	setup: ["start 2"],
	cmds: ["clock", "bpm 80", ["beats 4", 2], "beats"]
);
~radTest.("scheduled-commands",
	setup: ["start 2", "clock", "@ 2", ["tsr @1", 1]],
	cmds: [["(^ lt 1)", 2], ["(4 ^ lt 1)", 5]]
);

// Simultaneous commands
~radTest.("simultaneous-commands",
	setup: ["start 2"],
	cmds: ["(lp 1 drums1 & lp 2 marilyn2 & fx 1 pch)"]
);
