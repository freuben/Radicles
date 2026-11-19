// Commands typed with the mixer window open take other paths than without
// it (found by Federico on 18 Nov 2026: all three passed without the window).

~radTest.("gui-blk-rm",
	setup: ["start 3", "mix", "blk 1 sine", "blk 2 sine", "blk 3 sine"],
	cmds: [["blk rm 2", 2], ["blk rm 1", 2]],
	gui: true,
	check: { (Block.ndefs.collect(_.key) == [\block3]) or: { "blocks: " ++ Block.ndefs.collect(_.key).cs } }
);
~radTest.("gui-t-rm",
	setup: ["start 5", "mix", "blk 1 sine", "blk 3 sine", "blk 5 sine"],
	cmds: [["t rm 3", 6], ["t rm", 6]],
	gui: true,
	// the level meters ran on while the window was rebuilt: ERROR 'at' not understood
	check: { (Radicles.aZ.mixTrackNames.select{|n| n.asString.beginsWith("track") } == [\track1, \track2, \track4])
		or: { "tracks: " ++ Radicles.aZ.mixTrackNames.cs } }
);
~radTest.("gui-snd-level",
	setup: ["start 3 3", "mix"],
	cmds: [["snd t 1 1 1", 2], ["snd t 3 1 3 -12", 2]],
	gui: true,
	check: { (Ndef(\busIn3).get(\vol1) == -12) or: { "vol1: " ++ Ndef(\busIn3).get(\vol1) } }
);
