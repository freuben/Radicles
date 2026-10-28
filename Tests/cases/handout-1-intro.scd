// Examples from the teaching handout "Intro to Radicles and SC" (Jan 2020),
// in handout order. Sound files drums1 etc. come from RadiclesFiles/SoundFiles/Play/0.

~radTest.("start-2", cmds: ["start 2"]);
~radTest.("start-2-2", cmds: ["start 2 2"]);
// The panner depends on a strip's channels in and the output channels:
// mono tracks and buses into 2 channels use pan2, the stereo master bal2,
// the same as start 2.
~radTest.("start-2-2-2", cmds: ["start 2 2 2"],
	check: {
		var sp = Radicles.aZ.space.collect{|e| [e[0].asSymbol, e[1].asSymbol] };
		var want = [['spaceMaster', 'bal2'], ['spaceTrack1', 'pan2'], ['spaceTrack2', 'pan2'],
			['spaceBus1', 'pan2'], ['spaceBus2', 'pan2']];
		want.every{|w| sp.includesEqual(w) } or: { "spaces: " ++ sp.cs }
	});
// More output channels: mono strips use that output's panner, the master plays directly.
~radTest.("start-2-2-4", cmds: ["start 2 2 4"],
	check: {
		var sp = Radicles.aZ.space.collect{|e| [e[0].asSymbol, e[1].asSymbol] };
		[['spaceMaster', 'dir'], ['spaceTrack1', 'pan4'], ['spaceBus1', 'pan4']].every{|w| sp.includesEqual(w) }
			or: { "spaces: " ++ sp.cs }
	});
// start builds the mixer in steps that pause for the server: wait a fixed 5 s
// before checking (a 1 s quiet gap was occasionally too short on the Mac)
~radTest.("start-2-panners", cmds: [["start 2", 5]],
	check: {
		var sp = Radicles.aZ.space.collect{|e| [e[0].asSymbol, e[1].asSymbol] };
		[['spaceMaster', 'bal2'], ['spaceTrack1', 'pan2'], ['spaceBus1', 'pan2']].every{|w| sp.includesEqual(w) }
			or: { "spaces: " ++ sp.cs }
	});

~radTest.("mix-nomix", setup: ["start 2"], cmds: ["mix", "nomix"], gui: true);

~radTest.("play-loop-stop",
	setup: ["start 2"],
	// st frees the buffer a moment after stopping: wait, so the next lp
	// reads a new buffer instead of sometimes reusing the old one
	cmds: ["pl 1 drums1", ["st 1", 3], "lp 1 drums1", ["st 1", 3]]
);

~radTest.("play-folder-and-list",
	setup: ["start 2"],
	cmds: ["pl 1", "pl", "pl 0"]
);

~radTest.("add-tracks-and-buses",
	setup: ["start 2"],
	cmds: ["t add", "b add", "t add 2"],
	check: {
		var envir = Ndef.dictFor(Server.default).envir;
		['track3', 'bus2', 'track4'].every{|k| envir[k].notNil } or: { "missing tracks or buses" }
	}
);

~radTest.("add-several-tracks",
	setup: ["start 2"],
	cmds: ["t addn 2", "b addn 2 2"],
	check: {
		var envir = Ndef.dictFor(Server.default).envir;
		['track3', 'track4', 'bus2', 'bus3'].every{|k| envir[k].notNil } or: { "missing tracks or buses" }
	}
);

~radTest.("live-input",
	setup: ["start 3"],
	cmds: ["in 1 1", "st 1"]
);

~radTest.("call-window-colours",
	setup: ["start 2"],
	cmds: ["color DeepPink", "colortext black", "selcolor", "selcolortext"],
	gui: true
);
