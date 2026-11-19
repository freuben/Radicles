// fade (design note "Time in Radicles", 19 Nov 2026): fade N is the fade
// time of everything running and everything made later; one thing can be
// given its own (fade t 1 4, fade blk 1 4, fade fx t 1 2 4), until the next
// fade N; a time typed in a command is for that command only.

// the fade times of some Ndefs, by key
~radFades = {|keys| keys.collect{|key| Ndef(key).fadeTime } };
~radFadeAll = [\block1, \inTrack1, \track1, \spaceTrack1, \filterTrack_1_1, \track2, \bus1, \spaceBus1, \master, \inMaster];

~radTest.("fade-reaches-everything",
	setup: ["start 2 1", "blk 1 sine", "fx t 1 pch"],
	cmds: ["fade 4"],
	check: {
		var fades = ~radFades.(~radFadeAll);
		(fades.every(_ == 4) and: { Radicles.fadeTime == 4 }) or: { "fade times: " ++ fades.cs ++ ", session " ++ Radicles.fadeTime }
	}
);
~radTest.("fade-new-things",
	setup: ["fade 3", "start 2 1", "blk 1 sine", "fx t 1 pch"],
	cmds: [["t add", 3]],
	check: {
		var fades = ~radFades.(~radFadeAll ++ [\track3]);
		fades.every(_ == 3) or: { "fade times: " ++ fades.cs }
	}
);
~radTest.("fade-local",
	setup: ["start 2 1", "blk 1 sine", "blk 2 sine", "fx t 1 pch", "fx m pch", "snd t 1 1 1"],
	cmds: ["fade t 1 2", "fade blk 1 6", "fade fx t 1 1 3", "fade m 1", "fade b 1 5", "fade fx m 1 7"],
	check: {
		var got = ~radFades.([\inTrack1, \track1, \spaceTrack1, \block1, \filterTrack_1_1, \master, \inMaster, \bus1, \busIn1, \filterMaster_1_1, \track2, \block2]);
		(got == [2, 2, 2, 6, 3, 1, 1, 5, 5, 7, 0.5, 0.5]) or: { "fade times: " ++ got.cs }
	}
);
~radTest.("fade-global-overwrites-local",
	setup: ["start 2", "blk 1 sine", "fade blk 1 6", "fade t 2 3"],
	cmds: ["fade 2"],
	check: {
		var got = ~radFades.([\block1, \track2, \track1]);
		(got == [2, 2, 2]) or: { "fade times: " ++ got.cs }
	}
);
~radTest.("fade-missing-warns",
	setup: ["start 2", "blk 1 sine"],
	cmds: ["fade t 9 4", "fade b 1 4", "fade blk 9 4", "fade fx t 1 3 4", "fade zz 1 4"],
	check: {
		var got = ~radFades.([\block1, \track1, \track2, \master]);
		(got == [0.5, 0.5, 0.5, 0.5]) or: { "fade times: " ++ got.cs }
	}
);
// a time typed in a command is for that command only
~radTest.("fade-stop-time-is-one-off",
	setup: ["start 2", "blk 1 sine"],
	cmds: [["blk stop 1 2", 3.5]],
	check: { (Ndef(\block1).fadeTime == 0.5) or: { "block1's fade time after blk stop 1 2: " ++ Ndef(\block1).fadeTime } }
);
~radTest.("fade-stop-keeps-own-time",
	setup: ["start 2", "blk 1 sine", "fade blk 1 1"],
	cmds: [["blk stop 1 2", 3.5]],
	check: { (Ndef(\block1).fadeTime == 1) or: { "block1's fade time after blk stop 1 2: " ++ Ndef(\block1).fadeTime } }
);
// removing fades over the thing's own fade time
~radTest.("fade-remove-own-time",
	setup: ["start 3", "fx t 2 pch", "fade t 2 1.5", "fade fx t 2 1 2"],
	cmds: [["t rm 2", 8]],
	check: { (Radicles.aZ.mixTrackNames.includes(\track2).not) or: { "tracks: " ++ Radicles.aZ.mixTrackNames.cs } }
);
