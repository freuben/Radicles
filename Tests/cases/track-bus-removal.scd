// Adding and removing tracks and buses (review of 18 Nov 2026, and
// Federico's decisions that day): a removed track takes its source with it,
// a new track's block takes the track's number, and removing something that
// isn't there warns and does nothing. (start N makes N tracks and one bus.)

~radStripNow = {|name| RadUnit.refresh(RadDoc.current.copy).at(name) ? () };
~radStrips = { Radicles.aZ.mixTrackNames.copy };
~radBlocks = { Block.ndefs.collect(_.key) };

// a removed track takes its block with it; the others keep theirs
~radTest.("track-remove-takes-block",
	setup: ["start 3", "blk 2 sine"],
	cmds: [["t rmvn [2]", 6]],
	check: {
		case
		{ ~radStrips.() != [\track1, \track3, \bus1, \master] } { "strips: " ++ ~radStrips.().cs }
		{ ~radBlocks.() != [\block1, \block3] } { "blocks: " ++ ~radBlocks.().cs }
		{ (Radicles.aZ.inputs ? []).any{|item| item[0] == \inTrack2 } } { "track 2's input is still listed" }
		{ RadUnit.readInput(\track1) != [\block1] } { "track 1 input: " ++ RadUnit.readInput(\track1).cs }
		{ RadUnit.readInput(\track3) != [\block3] } { "track 3 input: " ++ RadUnit.readInput(\track3).cs }
		{ true }
	}
);

// ... with its effects, its sends and a playing source
~radTest.("track-remove-with-fx-and-sends",
	setup: ["start 2 1", "fx t 2 pch", "fx t 2 2 dl1", "snd t 1 1 1 -6", "snd t 2 1 1 -12", "blk 2 sine"],
	cmds: [["t rmvn [2]", 8]],
	check: {
		case
		{ ~radStrips.() != [\track1, \bus1, \master] } { "strips: " ++ ~radStrips.().cs }
		{ (Radicles.aZ.filters ? []).notEmpty } { "effects left: " ++ Radicles.aZ.filters.cs }
		{ ~radBlocks.() != [\block1] } { "blocks: " ++ ~radBlocks.().cs }
		{ ~radStripNow.(\track1)[\sends] != (bus1: (level: -6)) } { "track 1 sends: " ++ ~radStripNow.(\track1)[\sends].cs }
		{ true }
	}
);

// a block that also feeds another track stays when one of its tracks goes
~radTest.("track-remove-shared-block-stays",
	setup: ["start 2", "blk 1 sine", ["blk 1 <> 2", 3]],
	cmds: [["t rmvn [1]", 6]],
	check: {
		case
		{ ~radStrips.() != [\track2, \bus1, \master] } { "strips: " ++ ~radStrips.().cs }
		{ ~radBlocks.().includes(\block1).not } { "block 1 was removed: " ++ ~radBlocks.().cs }
		{ RadUnit.readInput(\track2) != [\block1] } { "track 2 input: " ++ RadUnit.readInput(\track2).cs }
		{ (RadUnit.readBlock(\block1) ? ())[\def] != \sine } { "block 1 no longer plays" }
		{ true }
	}
);

// a track added after a removal: the next number, with the block of that number
~radTest.("track-add-after-remove",
	setup: ["start 3", ["t rmvn [2]", 6]],
	cmds: [["t add", 6]],
	check: {
		case
		{ ~radStrips.() != [\track1, \track3, \track4, \bus1, \master] } { "strips: " ++ ~radStrips.().cs }
		{ ~radBlocks.() != [\block1, \block3, \block4] } { "blocks: " ++ ~radBlocks.().cs }
		{ RadUnit.readInput(\track4) != [\block4] } { "track 4 input: " ++ RadUnit.readInput(\track4).cs }
		{ true }
	}
);

// ... also when the block numbers had run ahead of the track numbers
~radTest.("track-add-block-takes-track-number",
	setup: ["start 2", ["blk 5 sine", 5], ["blk remove 5", 2]],
	cmds: [["t add", 6]],
	check: {
		case
		{ ~radStrips.() != [\track1, \track2, \track3, \bus1, \master] } { "strips: " ++ ~radStrips.().cs }
		{ RadUnit.readInput(\track3) != [\block3] } { "track 3 input: " ++ RadUnit.readInput(\track3).cs }
		{ true }
	}
);

// a track or bus that isn't there: a warning, nothing changes, no error
~radTest.("track-remove-missing-warns",
	setup: ["start 2 1"],
	cmds: [["t rmvn [7]", 3], ["b rmvn [4]", 3]],
	check: {
		case
		{ ~radStrips.() != [\track1, \track2, \bus1, \master] } { "strips: " ++ ~radStrips.().cs }
		{ ~radBlocks.() != [\block1, \block2] } { "blocks: " ++ ~radBlocks.().cs }
		{ true }
	}
);

// a bus nothing sends to, removed while another bus has sends (this failed
// partway: the lists were edited, then an error)
~radTest.("bus-remove-no-sends",
	setup: ["start 2 2", "snd t 1 1 1 -6"],
	cmds: [["b rmvn [2]", 6]],
	check: {
		case
		{ ~radStrips.() != [\track1, \track2, \bus1, \master] } { "strips: " ++ ~radStrips.().cs }
		{ ~radStripNow.(\track1)[\sends] != (bus1: (level: -6)) } { "track 1 sends: " ++ ~radStripNow.(\track1)[\sends].cs }
		{ true }
	}
);

// a bus with sends: the sends go with it; a bus added afterwards works
~radTest.("bus-remove-with-sends",
	setup: ["start 2 2", "snd t 1 1 1 -6", "snd t 2 1 1 -12", "snd t 1 2 2 -9"],
	cmds: [["b rmvn [1]", 8], ["b add", 5], ["snd t 2 1 3 -3", 3]],
	check: {
		case
		{ ~radStrips.() != [\track1, \track2, \bus2, \bus3, \master] } { "strips: " ++ ~radStrips.().cs }
		{ ~radStripNow.(\track1)[\sends] != (bus2: (level: -9)) } { "track 1 sends: " ++ ~radStripNow.(\track1)[\sends].cs }
		{ ~radStripNow.(\track2)[\sends] != (bus3: (level: -3)) } { "track 2 sends: " ++ ~radStripNow.(\track2)[\sends].cs }
		{ true }
	}
);
