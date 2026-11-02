// Effect presets in the new format (Document Model, step 1, PR 3): one
// readable file per preset in Settings/Presets/units/filter/<effect>/,
// fxsave and fxload through the reconciler, old presets still load.

// remove every preset a test saved (all are named radtest_*)
~radPresetClean = {
	(RadUnit.presetRoot +/+ "filter/*/radtest_*.scd").pathMatch.do{|path| File.delete(path) };
	PresetFile.remove(\filter, 'radtest_old', false, path: 0, post: false);
};

~radTest.("preset-save-file",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5"],
	cmds: ["fxsave t 1 1 radtest_warm"],
	check: {
		var path = RadUnit.presetPath(\filter, \pch, \radtest_warm), text, unit;
		text = File.exists(path).if({ File.readAllString(path) }, { "" });
		unit = RadUnit.readPreset(\filter, \pch, \radtest_warm);
		case
		{ text.isEmpty } { "no file at " ++ path }
		{ text.beginsWith("// Radicles unit preset · filter pch · format 1 · saved ").not } { "header: " ++ text.keep(60) }
		{ unit != (kind: \filter, def: \pch,
			args: (pitchRatio: 0.5, pitchDispertion: 0.0, timeDispersion: 0.0001, winSize: 0.02)) } { "unit: " ++ unit.cs }
		{ true }
	},
	teardown: ~radPresetClean
);

// fxload sets only what differs, in one line of posted code
~radTest.("preset-load",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxset t 1 1 winSize 0.1",
		"fxsave t 1 1 radtest_warm", "fxset t 1 1 pitchRatio 2"],
	cmds: ["fxload t 1 1 radtest_warm"],
	check: {
		var v = Ndef(\filterTrack_1_1).get(\pitchRatio), w = Ndef(\filterTrack_1_1).get(\winSize);
		((v == 0.5) and: { w == 0.1 }) or: { "pitchRatio " ++ v ++ ", winSize " ++ w }
	},
	teardown: ~radPresetClean
);

// a preset belongs to its effect: it loads into any slot holding that effect
~radTest.("preset-load-other-slot",
	setup: ["start 2 1", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxsave t 1 1 radtest_warm",
		"fx b 1 2 pch"],
	cmds: ["fxload b 1 2 radtest_warm"],
	check: { (Ndef(\filterBus_1_2).get(\pitchRatio) == 0.5) or: { "bus 1 slot 2: " ++ Ndef(\filterBus_1_2).get(\pitchRatio) } },
	teardown: ~radPresetClean
);

// and only there: another effect in the slot is left alone, with a reason
~radTest.("preset-load-other-effect",
	setup: ["start 2", "fx t 1 pch", "fxsave t 1 1 radtest_warm", "fx t 2 dl1", "fxset t 2 1 delayTime 0.2"],
	cmds: ["fxload t 2 1 radtest_warm"],
	check: {
		(Ndef(\filterTrack_2_1).get(\delayTime) == 0.2 and: { RadUnit.read(\filterTrack_2_1)[\def] == \dl1 })
		or: { "slot changed: " ++ RadUnit.read(\filterTrack_2_1).cs }
	},
	teardown: ~radPresetClean
);

// a modulator, with its own settings, comes back as it was saved
~radTest.("preset-load-modulator",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxset t 1 1 pitchDispertion sin"],
	code: {
		var saved;
		Ndef(\mod1).set(\freq, 3);
		Radicles.cW.callFunc("fxsave t 1 1 radtest_mod"); 0.5.wait;
		Radicles.cW.callFunc("unmodfx t 1 1 pitchDispertion"); 1.wait;
		Radicles.cW.callFunc("fxset t 1 1 pitchRatio 2"); 0.5.wait;
		Radicles.cW.callFunc("fxload t 1 1 radtest_mod"); 2.wait;
	},
	check: {
		var now = RadUnit.read(\filterTrack_1_1), saved = RadUnit.readPreset(\filter, \pch, \radtest_mod);
		var diff = RadDoc.diff(saved, now, 1e-6);
		diff.isEmpty or: { "differences: " ++ diff.collect{|d| d[\address] ++ " " ++ d[\old].cs ++ " -> " ++ d[\new].cs }.join("; ") }
	},
	teardown: ~radPresetClean
);

// a preset without a modulator removes one, returning the argument to the preset's value
~radTest.("preset-load-removes-modulator",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchDispertion 0.3", "fxsave t 1 1 radtest_plain",
		"fxset t 1 1 pitchDispertion sin"],
	cmds: ["fxload t 1 1 radtest_plain"],
	check: {
		var unit = RadUnit.read(\filterTrack_1_1);
		(unit[\mods].isNil and: { unit[\args][\pitchDispertion] == 0.3 }) or: { "unit: " ++ unit.cs }
	},
	teardown: ~radPresetClean
);

// saving over an existing preset asks first; with no window to ask in, it is not replaced
~radTest.("preset-save-exists",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxsave t 1 1 radtest_warm",
		"fxset t 1 1 pitchRatio 2"],
	cmds: ["fxsave t 1 1 radtest_warm"],
	check: {
		var unit = RadUnit.readPreset(\filter, \pch, \radtest_warm);
		(unit[\args][\pitchRatio] == 0.5) or: { "replaced: " ++ unit[\args].cs }
	},
	teardown: ~radPresetClean
);

// a preset saved in the old format (one file for all effect presets) still loads
~radTest.("preset-load-old-format",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5"],
	code: {
		Radicles.aZ.prepWriteFxPreset(\filterTrack_1_1, \radtest_old);
		Radicles.cW.callFunc("fxset t 1 1 pitchRatio 2"); 0.5.wait;
		Radicles.cW.callFunc("fxload t 1 1 radtest_old"); 2.wait;
	},
	check: { (Ndef(\filterTrack_1_1).get(\pitchRatio) == 0.5) or: { "pitchRatio " ++ Ndef(\filterTrack_1_1).get(\pitchRatio) } },
	teardown: ~radPresetClean
);

// listing: fxload alone lists every preset, fxload <effect> those of one effect
~radTest.("preset-list",
	setup: ["start 2", "fx t 1 pch", "fxsave t 1 1 radtest_b", "fxsave t 1 1 radtest_a",
		"fx t 2 dl1", "fxsave t 2 1 radtest_c"],
	check: {
		var all = Radicles.fxPresetList.select{|p| p[1].asString.beginsWith("radtest_") };
		(all == [[\dl1, \radtest_c], [\pch, \radtest_a], [\pch, \radtest_b]]
			and: { RadUnit.presetNames(\filter, \pch).select{|n| n.asString.beginsWith("radtest_") } == [\radtest_a, \radtest_b] })
		or: { "listed: " ++ all.cs }
	},
	teardown: ~radPresetClean
);

// a preset file is only ever data: one with code in it is refused, and nothing runs
~radTest.("preset-unsafe-file",
	setup: ["start 2", "fx t 1 pch"],
	code: {
		var path = RadUnit.presetPath(\filter, \pch, \radtest_bad);
		File.mkdir(path.dirname);
		File.use(path, "w", {|f| f.write("(format: 1, type: \\unit, unit: (kind: \\filter, def: \\pch, args: (pitchRatio: ~radPwned = 3)))") });
		~radPwned = nil;
		Radicles.cW.callFunc("fxload t 1 1 radtest_bad"); 1.wait;
	},
	check: {
		(~radPwned.isNil and: { Ndef(\filterTrack_1_1).get(\pitchRatio) != 3 }) or: { "the file ran" }
	},
	teardown: ~radPresetClean
);

// round trips for every effect in the Main library, in groups: insert it, set
// its arguments, fxsave, change them, fxload, compare with the saved preset
~radPresetRoundTrip = {|defs|
	var problems = [];
	defs.do{|def|
		var tag = \filterTrack_1_1, specs, setAll, saved, now, diff;
		Radicles.cW.callFunc("fx t 1 1 " ++ def); 2.wait;
		specs = SpecFile.read(\filter, def, false) ? [];
		setAll = {|pos|
			specs.do{|item|
				var spec = item[1].asSpec, value = spec.map(pos);
				if(item[2].notNil, { value = item[2].(value) });
				if(value.isNumber, { Ndef(tag).set(item[0], value) });
			};
		};
		setAll.(0.37); 0.2.wait;
		Radicles.cW.callFunc("fxsave t 1 1 radtest_rt"); 0.3.wait;
		setAll.(0.81); 0.2.wait;
		Radicles.cW.callFunc("fxload t 1 1 radtest_rt"); 1.5.wait;
		saved = RadUnit.readPreset(\filter, def, \radtest_rt);
		now = RadUnit.read(tag);
		diff = if(saved.isNil, { [(address: "no preset")] }, { RadDoc.diff(saved, now, 1e-6) });
		if(diff.notEmpty, { problems = problems.add(def ++ ": " ++ diff.collect(_[\address]).join(", ")) });
	};
	~radRoundTripProblems = problems;
};
~radPresetDefs = [
	\cbg, \chr1, \chr2, \dl1, \dl2, \gvrb1, \gvrb2, \gvrb3, \gvrb4, \gvrb5, \fvrb,
	\harm, \harm2, \pch, \pch2, \pch3, \pch4, \pch5, \bpf, \eq8, \hpf, \lpf,
	\cmp, \lmt, \amp, \gain, \cut1, \cut2, \cut3, \gate, \ds1, \ds2, \ds3, \ds4, \ds5, \sclip, \rm1, \rm2, \rm3,
	\frz1, \frz2, \mga, \sdf, \srt, \srv, \ssq, \cvi1, \cvi2, \cvt1, \cvt2, \mmi, \mmt
];
~radPresetDefs.clump(9).do{|defs, i|
	~radTest.("preset-roundtrip-" ++ (i + 1),
		setup: ["start 2"],
		code: { ~radPresetRoundTrip.(defs) },
		check: { ~radRoundTripProblems.isEmpty or: { ~radRoundTripProblems.join("; ") } },
		teardown: ~radPresetClean
	);
};

// the effect window's PRESET menu lists this effect's presets; choosing one
// loads it into the slot, like fxload (Mac: needs windows)
~radTest.("preset-menu",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxsave t 1 1 radtest_menu",
		"fxset t 1 1 pitchRatio 2"],
	code: {
		var win;
		Radicles.aZ.filterGUI(\track, 1, 1);
		0.5.wait;
		win = Radicles.aZ.filtersWindow.last;
		~radMenu = win.canvas.children.detect{|v| v.isKindOf(PopUpMenu) };
		~radMenuItems = ~radMenu !? {|m| m.items };
		~radMenu.valueAction = ~radMenuItems.indexOfEqual("radtest_menu");
		2.wait;
	},
	check: {
		case
		{ ~radMenuItems.isNil } { "no PRESET menu" }
		{ ~radMenuItems.first != "PRESET" or: { ~radMenuItems.includesEqual("radtest_menu").not } } { "items: " ++ ~radMenuItems.cs }
		{ Ndef(\filterTrack_1_1).get(\pitchRatio) != 0.5 } { "not loaded: " ++ Ndef(\filterTrack_1_1).get(\pitchRatio) }
		{ true }
	},
	teardown: {
		(Radicles.aZ !? {|a| a.filtersWindow } ? []).copy.do{|w| w.close };
		~radPresetClean.();
	},
	gui: true
);

// default: every effect's own default values, with its modulators removed
~radTest.("preset-default",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxset t 1 1 winSize 0.1", "fxset t 1 1 pitchDispertion sin"],
	cmds: [["fxload t 1 1 default", 2]],
	check: {
		var unit = RadUnit.read(\filterTrack_1_1);
		(unit[\args] == (pitchRatio: 1.0, pitchDispertion: 0.0, timeDispersion: 0.0001, winSize: 0.02) and: { unit[\mods].isNil })
		or: { "unit: " ++ unit.cs }
	}
);

// default cannot be saved over, and is not a file
~radTest.("preset-default-reserved",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5"],
	cmds: ["fxsave t 1 1 default"],
	check: { File.exists(RadUnit.presetPath(\filter, \pch, \default)).not or: { "a default file was written" } }
);

// fxmorph to default
~radTest.("preset-default-morph",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5"],
	cmds: [["fxmorph t 1 1 default 1", 2.5]],
	check: { (Ndef(\filterTrack_1_1).get(\pitchRatio) == 1.0 and: { Ndef(\filterTrack_1_1).fadeTime == 0.5 })
		or: { "pitchRatio " ++ Ndef(\filterTrack_1_1).get(\pitchRatio) } }
);

// the effect window's SAVE button asks for a name, saves, and the PRESET
// menu lists it (Mac)
~radTest.("preset-save-button",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5"],
	code: {
		var win, popup;
		Radicles.aZ.filterGUI(\track, 1, 1); 0.5.wait;
		win = Radicles.aZ.filtersWindow.last;
		win.canvas.children.detect{|v| v.isKindOf(Button) and: { v.states[0][0] == "SAVE" } }.valueAction = 0;
		0.5.wait;
		popup = Window.allWindows.detect{|w| w.name.beginsWith("Save preset") };
		~radPopup = popup.notNil;
		popup.view.children.detect{|v| v.isKindOf(TextField) }.string_("radtest_btn").doAction;
		1.wait;
		~radMenuItems = win.canvas.children.detect{|v| v.isKindOf(PopUpMenu) }.items;
	},
	check: {
		case
		{ ~radPopup.not } { "no Save preset window" }
		{ RadUnit.readPreset(\filter, \pch, \radtest_btn).isNil } { "not saved" }
		{ ~radMenuItems.keep(2) != ["PRESET", "default"] or: { ~radMenuItems.includesEqual("radtest_btn").not } } {
			"menu: " ++ ~radMenuItems.cs }
		{ true }
	},
	teardown: {
		(Radicles.aZ !? {|a| a.filtersWindow } ? []).copy.do{|w| w.close };
		Window.allWindows.select{|w| w.name.beginsWith("Save preset") }.do(_.close);
		~radPresetClean.();
	},
	gui: true
);

// a window rebuilt after a change (here, modulating an argument) stays where
// it was; another effect's window is not opened by it (Mac)
~radTest.("fx-window-stays-in-place",
	setup: ["start 2", "fx t 1 pch", "fx t 2 dl1"],
	code: {
		var win;
		Radicles.aZ.filterGUI(\track, 1, 1); 0.5.wait;
		win = Radicles.aZ.filtersWindow.last;
		win.bounds = win.bounds.moveTo(400, 300); 0.5.wait;
		~radBefore = win.bounds.origin;
		Radicles.cW.callFunc("fxset t 1 1 pitchDispertion sin"); 2.wait;
		Radicles.cW.callFunc("fxset t 2 1 delayTime sin"); 2.wait;
		~radWins = Radicles.aZ.filtersWindow.select{|w| w.isClosed.not };
		~radAfter = ~radWins.detect{|w| w.name.beginsWith("filterTrack_1_1 ") } !? {|w| w.bounds.origin };
	},
	check: {
		case
		{ ~radAfter.isNil } { "the window was not reopened" }
		{ (~radAfter.x - ~radBefore.x).abs > 2 or: { (~radAfter.y - ~radBefore.y).abs > 2 } } {
			"moved from " ++ ~radBefore ++ " to " ++ ~radAfter }
		{ ~radWins.size != 1 } { "windows open: " ++ ~radWins.collect(_.name).cs }
		{ true }
	},
	teardown: { (Radicles.aZ !? {|a| a.filtersWindow } ? []).copy.do{|w| w.close } },
	gui: true
);

// every effect's window shows all of its rows: nothing is cut off on the
// right, labels and values fit their text, and the window is tall enough (Mac)
~radTest.("fx-window-sizes",
	setup: ["start 2"],
	code: {
		var problems = [];
		~radPresetDefs.do{|def|
			var win, canvas, font = Font("Monaco", 8);
			Radicles.cW.callFunc("fx t 1 1 " ++ def); 1.5.wait;
			Radicles.aZ.filterGUI(\track, 1, 1); 0.5.wait;
			win = Radicles.aZ.filtersWindow.last;
			canvas = win.canvas;
			canvas.children.do{|v|
				if(v.bounds.right > (win.bounds.width + 1), { problems = problems.add(def ++ ": cut off on the right") });
				if(v.bounds.bottom > (canvas.bounds.height + 1), { problems = problems.add(def ++ ": below the canvas") });
				if(v.isKindOf(StaticText) and: { v.string.notNil } and: {
					v.string.bounds(font).width > (v.bounds.width + 1) }, {
					problems = problems.add(def ++ ": '" ++ v.string ++ "' does not fit") });
			};
			if(canvas.bounds.height > (win.bounds.height + 1) and: { win.bounds.height < 600 }, {
				problems = problems.add(def ++ ": window shorter than its rows") });
			win.close; 0.2.wait;
		};
		~radSizeProblems = problems.asSet.asArray.sort;
	},
	check: { ~radSizeProblems.isEmpty or: { ~radSizeProblems.keep(8).join("; ") } },
	teardown: { (Radicles.aZ !? {|a| a.filtersWindow } ? []).copy.do{|w| w.close } },
	gui: true
);
