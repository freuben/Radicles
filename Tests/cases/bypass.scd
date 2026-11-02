// Effect bypass (Document Model, step 1, PR 4): fxbypass routes the strip
// around an effect, which keeps its settings and is paused; the unit shows
// bypass: true; effect presets never carry it.

// what the Ndef key reads: true if its source reads Ndef(from)
~radReads = {|key, from| Ndef(key).source.cs.find("Ndef.ar(" ++ from.cs).notNil };
~radUnitOf = {|tag| RadUnit.read(tag) };
~radBypassClean = {
	(RadUnit.presetRoot +/+ "filter/*/radtest_*.scd").pathMatch.do{|path| File.delete(path) };
};

~radTest.("bypass-routes-around",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5"],
	cmds: [["fxbypass t 1 1", 3]],
	check: {
		case
		{ ~radReads.(\track1, \filterTrack_1_1) } { "track1 still reads the effect" }
		{ ~radReads.(\track1, \inTrack1).not } { "track1 does not read inTrack1" }
		{ Ndef(\filterTrack_1_1).paused.not } { "the effect is not paused" }
		{ Ndef(\filterTrack_1_1).get(\pitchRatio) != 0.5 } { "settings changed" }
		{ ~radUnitOf.(\filterTrack_1_1)[\bypass] != true } { "unit: " ++ ~radUnitOf.(\filterTrack_1_1).cs }
		{ true }
	}
);

~radTest.("bypass-back-in",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", ["fxbypass t 1 1", 3]],
	cmds: [["fxbypass t 1 1 0", 3]],
	check: {
		case
		{ ~radReads.(\track1, \filterTrack_1_1).not } { "track1 does not read the effect" }
		{ ~radReads.(\filterTrack_1_1, \inTrack1).not } { "the effect does not read inTrack1" }
		{ Ndef(\filterTrack_1_1).paused } { "the effect is still paused" }
		{ Ndef(\filterTrack_1_1).get(\pitchRatio) != 0.5 } { "settings changed" }
		{ ~radUnitOf.(\filterTrack_1_1)[\bypass].notNil } { "unit: " ++ ~radUnitOf.(\filterTrack_1_1).cs }
		{ true }
	}
);

// without a state fxbypass toggles; with one it sets, and setting the same
// state again does nothing (no posted code)
~radTest.("bypass-toggle-and-set",
	setup: ["start 2", "fx t 1 pch"],
	cmds: [["fxbypass t 1 1", 3], ["fxbypass t 1 1", 3], ["fxbypass t 1 1 1", 3], ["fxbypass t 1 1 1", 2]],
	check: {
		(Ndef(\filterTrack_1_1).paused and: { ~radReads.(\track1, \inTrack1) }) or: { "not bypassed at the end" }
	}
);

// the middle of three effects: the third reads the first
~radTest.("bypass-middle-slot",
	setup: ["start 2", "fx t 1 1 pch", "fx t 1 2 dl1", "fx t 1 3 lpf"],
	cmds: [["fxbypass t 1 2", 3]],
	check: {
		case
		{ ~radReads.(\filterTrack_1_3, \filterTrack_1_1).not } { "slot 3 does not read slot 1" }
		{ ~radReads.(\track1, \filterTrack_1_3).not } { "track1 does not read slot 3" }
		{ Ndef(\filterTrack_1_2).paused.not } { "slot 2 not paused" }
		{ Ndef(\filterTrack_1_1).paused or: { Ndef(\filterTrack_1_3).paused } } { "another slot paused" }
		{ true }
	}
);

// two bypassed next to each other; bringing one back routes it between its
// neighbours that sound, and the other stays bypassed
~radTest.("bypass-two-adjacent",
	setup: ["start 2", "fx t 1 1 pch", "fx t 1 2 dl1", "fx t 1 3 lpf", ["fxbypass t 1 1", 3], ["fxbypass t 1 2", 3]],
	cmds: [["fxbypass t 1 1 0", 3]],
	check: {
		case
		{ ~radReads.(\filterTrack_1_1, \inTrack1).not } { "slot 1 does not read inTrack1" }
		{ ~radReads.(\filterTrack_1_3, \filterTrack_1_1).not } { "slot 3 does not read slot 1" }
		{ Ndef(\filterTrack_1_2).paused.not } { "slot 2 is no longer bypassed" }
		{ Radicles.aZ.bypassed != [\filterTrack_1_2] } { "bypassed: " ++ Radicles.aZ.bypassed.cs }
		{ true }
	}
);

~radTest.("bypass-bus-and-master",
	setup: ["start 2 1", "fx b 1 pch", "fx m 1 dl1"],
	cmds: [["fxbypass b 1 1", 3], ["fxbypass m 1", 3]],
	check: {
		case
		{ ~radReads.(\bus1, \inBus1).not } { "bus1 does not read inBus1" }
		{ ~radReads.(\master, \inMaster).not } { "master does not read inMaster" }
		{ Ndef(\filterBus_1_1).paused.not or: { Ndef(\filterMaster_1_1).paused.not } } { "not paused" }
		{ true }
	}
);

// by mixer position, like the other effect commands
~radTest.("bypass-mixer-position",
	setup: ["start 2", "fx t 2 pch"],
	cmds: [["fxbypass 2 1 1", 3]],
	check: { (Ndef(\filterTrack_2_1).paused and: { ~radReads.(\track2, \inTrack2) }) or: { "track 2 slot 1 not bypassed" } }
);

~radTest.("bypass-empty-slot",
	setup: ["start 2", "fx t 1 pch"],
	cmds: ["fxbypass t 1 2"],
	check: { Radicles.aZ.bypassed.isNil or: { "bypassed: " ++ Radicles.aZ.bypassed.cs } }
);

// adding an effect to another slot re-routes the strip, still around the bypassed one
~radTest.("bypass-then-add-another",
	setup: ["start 2", "fx t 1 1 pch", ["fxbypass t 1 1", 3]],
	cmds: ["fx t 1 2 dl1"],
	check: {
		case
		{ ~radReads.(\filterTrack_1_2, \inTrack1).not } { "slot 2 does not read inTrack1" }
		{ ~radReads.(\track1, \filterTrack_1_2).not } { "track1 does not read slot 2" }
		{ Ndef(\filterTrack_1_1).paused.not } { "slot 1 no longer paused" }
		{ true }
	}
);

// removing a bypassed effect forgets the bypass; a new effect there sounds
~radTest.("bypass-removed",
	setup: ["start 2", "fx t 1 1 pch", ["fxbypass t 1 1", 3]],
	cmds: ["fxclear t 1 1", "fx t 1 1 dl1"],
	check: {
		case
		{ (Radicles.aZ.bypassed ? []).notEmpty } { "bypassed: " ++ Radicles.aZ.bypassed.cs }
		{ ~radReads.(\track1, \filterTrack_1_1).not } { "track1 does not read the new effect" }
		{ Ndef(\filterTrack_1_1).paused } { "the new effect is paused" }
		{ true }
	}
);

// another effect replacing a bypassed one is not bypassed
~radTest.("bypass-replaced",
	setup: ["start 2", "fx t 1 1 pch", ["fxbypass t 1 1", 3]],
	cmds: ["fx t 1 1 dl1"],
	check: {
		case
		{ Radicles.aZ.isBypassed(\filterTrack_1_1) } { "still bypassed" }
		{ ~radReads.(\track1, \filterTrack_1_1).not } { "track1 does not read the new effect" }
		{ Ndef(\filterTrack_1_1).paused } { "the new effect is paused" }
		{ true }
	}
);

// a bypassed effect can still be set; its settings are there when it comes back
~radTest.("bypass-set-while-bypassed",
	setup: ["start 2", "fx t 1 pch", ["fxbypass t 1 1", 3], "fxset t 1 1 pitchRatio 0.25"],
	cmds: [["fxbypass t 1 1 0", 3]],
	check: { (Ndef(\filterTrack_1_1).get(\pitchRatio) == 0.25) or: { "pitchRatio " ++ Ndef(\filterTrack_1_1).get(\pitchRatio) } }
);

// a modulator added while bypassed is there when the effect comes back
~radTest.("bypass-modulated-while-bypassed",
	setup: ["start 2", "fx t 1 pch", ["fxbypass t 1 1", 3], "fxset t 1 1 pitchDispertion sin"],
	cmds: [["fxbypass t 1 1 0", 3]],
	check: {
		var unit = ~radUnitOf.(\filterTrack_1_1);
		((unit[\mods] !? {|m| m[\pitchDispertion] } !? {|m| m[\type] }) == \sin and: { ~radReads.(\track1, \filterTrack_1_1) })
		or: { "unit: " ++ unit.cs }
	}
);

// effect presets never carry bypass: saving a bypassed effect writes no
// bypass, and loading a preset leaves the slot bypassed
~radTest.("bypass-and-effect-presets",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", ["fxbypass t 1 1", 3], "fxsave t 1 1 radtest_byp",
		"fxset t 1 1 pitchRatio 2"],
	cmds: ["fxload t 1 1 radtest_byp"],
	check: {
		var saved = RadUnit.readPreset(\filter, \pch, \radtest_byp);
		case
		{ saved.isNil } { "no preset" }
		{ saved[\bypass].notNil } { "the preset holds bypass" }
		{ Ndef(\filterTrack_1_1).get(\pitchRatio) != 0.5 } { "not loaded" }
		{ Radicles.aZ.isBypassed(\filterTrack_1_1).not or: { Ndef(\filterTrack_1_1).paused.not } } { "no longer bypassed" }
		{ true }
	},
	teardown: ~radBypassClean
);

// the effect window's BYPASS button: pressing it bypasses; it is lit while
// bypassed, also when bypassed by command (Mac: needs windows)
~radTest.("bypass-button",
	setup: ["start 2", "fx t 1 pch"],
	code: {
		var win, button;
		Radicles.aZ.filterGUI(\track, 1, 1); 0.5.wait;
		win = Radicles.aZ.filtersWindow.last;
		button = win.canvas.children.detect{|v| v.isKindOf(Button) and: { v.states.size == 2 } };
		~radBypassButton = button;
		button.valueAction = 1; 3.wait;
		~radPressed = Ndef(\filterTrack_1_1).paused;
		Radicles.cW.callFunc("fxbypass t 1 1 0"); 3.wait;
		~radAfterCmd = button.value;
	},
	check: {
		case
		{ ~radBypassButton.isNil } { "no BYPASS button" }
		{ ~radBypassButton.states[0][0] != "BYPASS" } { "button: " ++ ~radBypassButton.states.cs }
		{ ~radPressed.not } { "pressing it did not bypass" }
		{ ~radAfterCmd != 0 } { "still lit after fxbypass t 1 1 0" }
		{ true }
	},
	teardown: { (Radicles.aZ !? {|a| a.filtersWindow } ? []).copy.do{|w| w.close } },
	gui: true
);

// coming back does not rebuild the effect (a delay or reverb keeps what it
// holds), unless the stage before it changed while it was bypassed
~radTest.("bypass-back-keeps-effect-running",
	setup: ["start 2", "fx t 1 pch", ["fxbypass t 1 1", 3]],
	code: {
		~radSynthBefore = Ndef(\filterTrack_1_1).objects.first.nodeID;
		Radicles.cW.callFunc("fxbypass t 1 1 0"); 3.wait;
		~radSynthAfter = Ndef(\filterTrack_1_1).objects.first.nodeID;
	},
	check: { (~radSynthBefore == ~radSynthAfter) or: { "the effect was rebuilt" } }
);

~radTest.("bypass-back-after-earlier-slot-added",
	setup: ["start 2", "fx t 1 2 pch", ["fxbypass t 1 2", 3], "fx t 1 1 dl1"],
	cmds: [["fxbypass t 1 2 0", 3]],
	check: {
		case
		{ ~radReads.(\filterTrack_1_2, \filterTrack_1_1).not } { "slot 2 does not read slot 1" }
		{ ~radReads.(\track1, \filterTrack_1_2).not } { "track1 does not read slot 2" }
		{ true }
	}
);

// strip presets (cssave / csload) keep which slots are bypassed: saved as a
// third element that older code ignores; loading bypasses those slots again
~radTest.("bypass-strip-preset",
	setup: ["start 2", "fx t 1 1 pch", "fx t 1 2 dl1", "fxset t 1 1 pitchRatio 0.5", ["fxbypass t 1 2", 3],
		"cssave t 1 radtest_bypstrip"],
	cmds: [["csload t 2 radtest_bypstrip", 8]],
	check: {
		var saved = PresetFile.read(\track, 'radtest_bypstrip');
		case
		{ saved[2] != (bypass: [2]) } { "saved: " ++ saved[2].cs }
		{ Radicles.aZ.isBypassed(\filterTrack_2_2).not } { "track 2 slot 2 not bypassed" }
		{ Radicles.aZ.isBypassed(\filterTrack_2_1) } { "track 2 slot 1 bypassed" }
		{ ~radReads.(\track2, \filterTrack_2_1).not } { "track2 does not read slot 1" }
		{ Ndef(\filterTrack_2_1).get(\pitchRatio) != 0.5 } { "settings not loaded" }
		{ true }
	},
	teardown: { PresetFile.remove(\track, 'radtest_bypstrip', false, path: 0, post: false) }
);

// a strip with nothing bypassed is saved exactly as before (two elements)
~radTest.("bypass-strip-preset-none",
	setup: ["start 2", "fx t 1 pch"],
	cmds: ["cssave t 1 radtest_bypnone"],
	check: { (PresetFile.read(\track, 'radtest_bypnone').size == 2) or: { "saved: " ++ PresetFile.read(\track, 'radtest_bypnone').cs } },
	teardown: { PresetFile.remove(\track, 'radtest_bypnone', false, path: 0, post: false) }
);
