// The rm spelling for removing things: t rm, t rmn, b rm, b rmn, blk rm and
// fxrm do what t rmv, t rmvn, b rmv, b rmvn, blk remove and fxclear do (the
// old spellings still work: handouts and saved command files use them).
// Removing tracks and buses is asynchronous, so those commands wait.

// the mixer's tracks, or buses, by name
~radStrips = {|kind| Radicles.aZ.mixTrackNames.select{|name| name.asString.contains(kind) }.collect(_.asSymbol) };
// whether an effect slot is on, by its tag (e.g. \filterTrack_1_1)
~radHasFx = {|tag| (Radicles.aZ.filters ? []).any{|item| item[0] == tag } };

// tracks
~radTest.("rm-track",
	setup: ["start 3"],
	cmds: [["t rm", 4]],
	check: { (~radStrips.("track") == [\track1, \track2]) or: { "tracks: " ++ ~radStrips.("track").cs } }
);
~radTest.("rm-track-old-spelling-too",
	setup: ["start 3"],
	cmds: [["t rm", 4], ["t rmv", 4]],
	check: { (~radStrips.("track") == [\track1]) or: { "tracks: " ++ ~radStrips.("track").cs } }
);
~radTest.("rmn-track-number",
	setup: ["start 4"],
	cmds: [["t rmn 2", 8]],
	check: { (~radStrips.("track") == [\track1, \track2]) or: { "tracks: " ++ ~radStrips.("track").cs } }
);
~radTest.("rmn-track-array",
	setup: ["start 4"],
	cmds: [["t rmn [2,3]", 8]],
	check: { (~radStrips.("track") == [\track1, \track4]) or: { "tracks: " ++ ~radStrips.("track").cs } }
);
~radTest.("rmn-track-old-spelling-too",
	setup: ["start 4"],
	cmds: [["t rmn 1", 4], ["t rmvn 1", 4]],
	check: { (~radStrips.("track") == [\track1, \track2]) or: { "tracks: " ++ ~radStrips.("track").cs } }
);

// buses
~radTest.("rm-bus",
	setup: ["start 2 3"],
	cmds: [["b rm", 4]],
	check: { (~radStrips.("bus") == [\bus1, \bus2]) or: { "buses: " ++ ~radStrips.("bus").cs } }
);
~radTest.("rmn-bus-number",
	setup: ["start 2 3"],
	cmds: [["b rmn 2", 8]],
	check: { (~radStrips.("bus") == [\bus1]) or: { "buses: " ++ ~radStrips.("bus").cs } }
);
~radTest.("rmn-bus-array",
	setup: ["start 2 3"],
	cmds: [["b rmn [1,3]", 8]],
	check: { (~radStrips.("bus") == [\bus2]) or: { "buses: " ++ ~radStrips.("bus").cs } }
);

// blocks (only that the block goes: what the track is connected to
// afterwards is another change)
~radTest.("rm-block",
	setup: ["start 2", "blk 1 sine"],
	cmds: ["blk rm 1"],
	check: { Block.ndefs.collect(_.key).includes(\block1).not or: { "blocks: " ++ Block.ndefs.collect(_.key).cs } }
);
~radTest.("rm-block-old-spelling-too",
	setup: ["start 2", "blk 1 sine"],
	cmds: ["blk remove 1"],
	check: { Block.ndefs.collect(_.key).includes(\block1).not or: { "blocks: " ++ Block.ndefs.collect(_.key).cs } }
);

// effects: the four forms of fxclear
~radTest.("fxrm-track-slot",
	setup: ["start 2", "fx t 1 pch", "fx t 2 pch"],
	cmds: ["fxrm t 1 1", "fxclear t 2 1"],
	check: {
		case
		{ ~radHasFx.(\filterTrack_1_1) } { "fxrm left: " ++ Radicles.aZ.filters.cs }
		{ ~radHasFx.(\filterTrack_2_1) } { "fxclear left: " ++ Radicles.aZ.filters.cs }
		{ true }
	}
);
~radTest.("fxrm-mix-position",
	setup: ["start 2", "fx t 1 pch", "fx t 2 2 dl1"],
	cmds: ["fxrm 1", "fxrm 2 2"],
	check: { (~radHasFx.(\filterTrack_1_1) or: { ~radHasFx.(\filterTrack_2_2) }).not
		or: { "filters left: " ++ Radicles.aZ.filters.cs } }
);
~radTest.("fxrm-master",
	setup: ["start 2", "fx m pch", "fx m 2 dl1"],
	cmds: ["fxrm m 1", "fxrm m 1 2"],
	check: { (~radHasFx.(\filterMaster_1_1) or: { ~radHasFx.(\filterMaster_1_2) }).not
		or: { "filters left: " ++ Radicles.aZ.filters.cs } }
);
