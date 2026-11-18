// Removing things is spelled rm: t rm, t rm N, t rm [..], t rm N-M, t rmn N
// (and the same for b), blk rm N, fx rm (four forms), fx rml N, and fxl lists
// the active effects. The old spellings (rmv, rmvn, blk remove, fxclear,
// clearfx) are gone. Removing tracks and buses is asynchronous, so those
// commands wait.

// the mixer's tracks, or buses, by name
~radStrips = {|kind| Radicles.aZ.mixTrackNames.select{|name| name.asString.contains(kind) }.collect(_.asSymbol) };
// whether an effect slot is on, by its tag (e.g. \filterTrack_1_1)
~radHasFx = {|tag| (Radicles.aZ.filters ? []).any{|item| item[0] == tag } };
// the active effects' tags
~radFxTags = { (Radicles.aZ.filters ? []).collect{|item| item[0] } };

// tracks
~radTest.("rm-track",
	setup: ["start 3"],
	cmds: [["t rm", 4]],
	check: { (~radStrips.("track") == [\track1, \track2]) or: { "tracks: " ++ ~radStrips.("track").cs } }
);
~radTest.("rm-track-number",
	setup: ["start 3"],
	cmds: [["t rm 2", 6]],
	check: { (~radStrips.("track") == [\track1, \track3]) or: { "tracks: " ++ ~radStrips.("track").cs } }
);
~radTest.("rm-track-array",
	setup: ["start 4"],
	cmds: [["t rm [2,3]", 8]],
	check: { (~radStrips.("track") == [\track1, \track4]) or: { "tracks: " ++ ~radStrips.("track").cs } }
);
~radTest.("rm-track-range",
	setup: ["start 5"],
	cmds: [["t rm 2-4", 10]],
	check: { (~radStrips.("track") == [\track1, \track5]) or: { "tracks: " ++ ~radStrips.("track").cs } }
);
~radTest.("rmn-track",
	setup: ["start 4"],
	cmds: [["t rmn 2", 8]],
	check: { (~radStrips.("track") == [\track1, \track2]) or: { "tracks: " ++ ~radStrips.("track").cs } }
);

// buses
~radTest.("rm-bus",
	setup: ["start 2 3"],
	cmds: [["b rm", 4]],
	check: { (~radStrips.("bus") == [\bus1, \bus2]) or: { "buses: " ++ ~radStrips.("bus").cs } }
);
~radTest.("rm-bus-number",
	setup: ["start 2 3"],
	cmds: [["b rm 2", 6]],
	check: { (~radStrips.("bus") == [\bus1, \bus3]) or: { "buses: " ++ ~radStrips.("bus").cs } }
);
~radTest.("rm-bus-array",
	setup: ["start 2 3"],
	cmds: [["b rm [1,3]", 8]],
	check: { (~radStrips.("bus") == [\bus2]) or: { "buses: " ++ ~radStrips.("bus").cs } }
);
~radTest.("rm-bus-range",
	setup: ["start 2 4"],
	cmds: [["b rm 2-3", 8]],
	check: { (~radStrips.("bus") == [\bus1, \bus4]) or: { "buses: " ++ ~radStrips.("bus").cs } }
);
~radTest.("rmn-bus",
	setup: ["start 2 3"],
	cmds: [["b rmn 2", 8]],
	check: { (~radStrips.("bus") == [\bus1]) or: { "buses: " ++ ~radStrips.("bus").cs } }
);

// blocks (only that the block goes: what the track is connected to
// afterwards is another change)
~radTest.("rm-block",
	setup: ["start 2", "blk 1 sine"],
	cmds: ["blk rm 1"],
	check: { Block.ndefs.collect(_.key).includes(\block1).not or: { "blocks: " ++ Block.ndefs.collect(_.key).cs } }
);

// effects: fx rm by trackType and number, or by mixer position
~radTest.("fx-rm-track-slot",
	setup: ["start 2", "fx t 1 pch", "fx t 1 2 dl1", "fx t 2 pch"],
	cmds: ["fx rm t 1 2"],
	check: { (~radFxTags.() == [\filterTrack_1_1, \filterTrack_2_1]) or: { "filters: " ++ ~radFxTags.().cs } }
);
~radTest.("fx-rm-bus-slot",
	setup: ["start 2 1", "fx b 1 pch"],
	cmds: ["fx rm b 1 1"],
	check: { ~radHasFx.(\filterBus_1_1).not or: { "filters: " ++ ~radFxTags.().cs } }
);
~radTest.("fx-rm-master",
	setup: ["start 2", "fx m pch", "fx m 2 dl1"],
	cmds: ["fx rm m 1"],
	check: { (~radFxTags.() == [\filterMaster_1_2]) or: { "filters: " ++ ~radFxTags.().cs } }
);
~radTest.("fx-rm-mix-position-slot",
	setup: ["start 2", "fx t 2 pch", "fx t 2 2 dl1"],
	cmds: ["fx rm 2 2"],
	check: { (~radFxTags.() == [\filterTrack_2_1]) or: { "filters: " ++ ~radFxTags.().cs } }
);
~radTest.("fx-rm-mix-position",
	setup: ["start 2", "fx t 1 pch", "fx t 2 pch"],
	cmds: ["fx rm 2"],
	check: { (~radFxTags.() == [\filterTrack_1_1]) or: { "filters: " ++ ~radFxTags.().cs } }
);

// effects: fx rml N removes number N of the list fxl posts (the list is
// sorted by strip type, then number and slot: buses, master, tracks)
~radTest.("fx-rml",
	setup: ["start 2", "fx t 1 pch", "fx t 1 2 dl1", "fx t 1 3 pch"],
	cmds: ["fx rml 2"],
	check: { (~radFxTags.() == [\filterTrack_1_1, \filterTrack_1_3]) or: { "filters: " ++ ~radFxTags.().cs } }
);
~radTest.("fx-rml-out-of-range",
	setup: ["start 2", "fx t 1 pch"],
	cmds: ["fx rml 5"],
	check: { (~radFxTags.() == [\filterTrack_1_1]) or: { "filters: " ++ ~radFxTags.().cs } }
);
~radTest.("fxl",
	setup: ["start 2 1", "fx t 1 pch", "fx t 1 2 dl1", "fx b 1 pch", "fx m pch"],
	cmds: ["fxl"]
);
~radTest.("fxl-no-effects",
	setup: ["start 2"],
	cmds: ["fxl"]
);

// the old spellings are gone: they leave tracks, effects and blocks as they were
~radTest.("old-spellings-do-nothing",
	setup: ["start 3", "fx t 1 pch", "blk 1 sine"],
	cmds: [["t rmv", 4], ["t rmvn 1", 4], "fxclear t 1 1", "fxrm t 1 1", "clearfx 1", "blk remove 1"],
	check: {
		case
		{ ~radStrips.("track") != [\track1, \track2, \track3] } { "tracks: " ++ ~radStrips.("track").cs }
		{ ~radHasFx.(\filterTrack_1_1).not } { "filters: " ++ ~radFxTags.().cs }
		{ Block.ndefs.collect(_.key).includes(\block1).not } { "blocks: " ++ Block.ndefs.collect(_.key).cs }
		{ true }
	}
);
