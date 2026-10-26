// Examples from the teaching handout "Intro to Radicles and SC" (Jan 2020),
// in handout order. Sound files drums1 etc. come from RadiclesFiles/SoundFiles/Play/0.

~radTest.("start-2", cmds: ["start 2"]);
~radTest.("start-2-2", cmds: ["start 2 2"]);
~radTest.("start-2-2-2", cmds: ["start 2 2 2"]);

~radTest.("mix-nomix", setup: ["start 2"], cmds: ["mix", "nomix"], gui: true);

~radTest.("play-loop-stop",
	setup: ["start 2"],
	cmds: ["pl 1 drums1", "st 1", "lp 1 drums1", "st 1"]
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
