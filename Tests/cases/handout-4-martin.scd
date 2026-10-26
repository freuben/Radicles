// Examples from Martin's Radicles revision handout ("Everything You Wanted to
// Know About Radicles", Jan 2020) that are not already in the other handouts.

~radTest.("start-2-1", cmds: ["start 2 1"]);
~radTest.("start-defaults", cmds: ["start"]);

~radTest.("remove-tracks",
	setup: ["start 4 1"],
	cmds: ["t rmvn [2,3]"]
);

~radTest.("stop-with-fade",
	setup: ["start 2", "lp 2 drums1"],
	cmds: ["st 2 8"]
);

~radTest.("record-mix",
	setup: ["start 2"],
	cmds: ["prec", ["rec", 1], "srec"],
	gui: true,
	note: "writes a recording into RadiclesFiles/SoundFiles/Record"
);

~radTest.("channel-strip-presets",
	setup: ["start 2", "fx t 1 pch"],
	cmds: ["cssave t 1 radtest_martin", "csload t 2 radtest_martin"],
	teardown: { PresetFile.remove(\track, 'radtest_martin', false, path: 0, post: false) }
);

~radTest.("buffers-and-loops",
	setup: ["start 3"],
	cmds: ["@n 2 30", "lp 1 @1", "sr @1 1", "or @1 1", "sr @2 t 3"]
);

~radTest.("timed-record-and-stop",
	setup: ["start 2", "@ 4"],
	cmds: [["tsr @1 1", 1], "lt 1"]
);

~radTest.("beat-synced",
	setup: ["start 2", "clock", "bpm 120", "@ 2"],
	cmds: [["(1 ^ sr @1 1)", 3]]
);
