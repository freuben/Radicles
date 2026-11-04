// Block presets (Document Model, step 2, PR 2): blksave and blkload, the
// same folders and reconciler as effect presets, a sound file or wavetable
// carried by name, nobuf, default, same definition only.

// remove every block preset a test saved (all are named radtest_*)
~radBlockPresetClean = {
	(RadUnit.presetRoot +/+ "block/*/radtest_*.scd").pathMatch.do{|path| File.delete(path) };
};

~radTest.("block-preset-save-file",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 330"],
	cmds: ["blksave 1 radtest_low"],
	check: {
		var path = RadUnit.presetPath(\block, \sine, \radtest_low), text, unit;
		text = File.exists(path).if({ File.readAllString(path) }, { "" });
		unit = RadUnit.readPreset(\block, \sine, \radtest_low);
		case
		{ text.isEmpty } { "no file at " ++ path }
		{ text.beginsWith("// Radicles unit preset · block sine · format 1 · saved ").not } { "header: " ++ text.keep(60) }
		{ unit != (kind: \block, def: \sine, args: (freq: 330, phase: 0, mul: 0.4)) } { "unit: " ++ unit.cs }
		{ true }
	},
	teardown: ~radBlockPresetClean
);

// blkload sets what differs
~radTest.("block-preset-load",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 330", "blksave 1 radtest_low", "blkset 1 freq 500"],
	cmds: ["blkload 1 radtest_low"],
	check: {
		var v = Ndef(\block1).get(\freq);
		(v == 330) or: { "freq " ++ v }
	},
	teardown: ~radBlockPresetClean
);

// default: the definition's own values
~radTest.("block-preset-default",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 500"],
	cmds: ["blkload 1 default"],
	check: {
		var v = Ndef(\block1).get(\freq);
		(v == 440) or: { "freq " ++ v }
	}
);

// a preset belongs to its definition: it loads into any block playing it,
// and not into a block playing something else
~radTest.("block-preset-other-block",
	setup: ["start 2", "blk 1 sine", "blk 2 sine", "blkset 1 freq 330", "blksave 1 radtest_low"],
	cmds: ["blkload 2 radtest_low"],
	check: {
		var v = Ndef(\block2).get(\freq);
		(v == 330) or: { "freq " ++ v }
	},
	teardown: ~radBlockPresetClean
);

~radTest.("block-preset-other-def",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 330", "blksave 1 radtest_low", "blk 2 noise"],
	cmds: ["blkload 2 radtest_low"],
	check: {
		var unit = RadUnit.readBlock(\block2);
		(unit[\def] == \noise) or: { "unit: " ++ unit.cs }
	},
	teardown: ~radBlockPresetClean
);

// a sound file by name and Play folder: loading plays the block again with it
~radTest.("block-preset-sound-file",
	setup: ["start 2", ["lp 1 marilyn1", 2], "blkset 1 rate 0.5", "blksave 1 radtest_m", ["lp 1 drums1", 4]],
	cmds: [["blkload 1 radtest_m", 3]],
	check: {
		var unit = RadUnit.readBlock(\block1), file = RadUnit.readPreset(\block, \loop, \radtest_m);
		case
		{ file[\buffer] != (play: \marilyn1, folder: BStore.playFolder) } { "preset: " ++ file.cs }
		{ unit[\buffer] != (play: \marilyn1, folder: BStore.playFolder) } { "buffer: " ++ unit[\buffer].cs }
		{ unit[\args][\rate] != 0.5 } { "rate: " ++ unit[\args].cs }
		{ true }
	},
	teardown: ~radBlockPresetClean
);

// nobuf: the preset's values, the block's own sound file
~radTest.("block-preset-nobuf",
	setup: ["start 2", ["lp 1 marilyn1", 2], "blkset 1 rate 0.5", "blksave 1 radtest_m", ["lp 1 drums1", 4]],
	cmds: [["blkload 1 radtest_m nobuf", 3]],
	check: {
		var unit = RadUnit.readBlock(\block1);
		case
		{ unit[\buffer] != (play: \drums1, folder: BStore.playFolder) } { "buffer: " ++ unit[\buffer].cs }
		{ unit[\args][\rate] != 0.5 } { "rate: " ++ unit[\args].cs }
		{ true }
	},
	teardown: ~radBlockPresetClean
);

// a wavetable by its name
~radTest.("block-preset-wavetable",
	setup: ["start 2", ["blk 1 osc1 sine1harm6", 2], "blksave 1 radtest_t", ["blk 1 osc1 sine2harm2", 2]],
	cmds: [["blkload 1 radtest_t", 3]],
	check: {
		var unit = RadUnit.readBlock(\block1);
		(unit[\data] == \sine1harm6 and: { unit[\buffer] == (alloc: Block.wavetableSize) }) or: { "unit: " ++ unit.cs }
	},
	teardown: ~radBlockPresetClean
);

// default keeps the block's sound file
~radTest.("block-preset-default-keeps-file",
	setup: ["start 2", ["lp 1 drums1", 2], "blkset 1 rate 0.5"],
	cmds: [["blkload 1 default", 2]],
	check: {
		var unit = RadUnit.readBlock(\block1);
		case
		{ unit[\buffer] != (play: \drums1, folder: BStore.playFolder) } { "buffer: " ++ unit[\buffer].cs }
		{ unit[\args][\rate] != 1 } { "rate: " ++ unit[\args].cs }
		{ true }
	}
);

// a modulator comes back with its own settings
~radTest.("block-preset-modulated",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq sin", "modblkset 1 freq [freq, 3]",
		"blksave 1 radtest_mod", "unmodblk 1 freq"],
	cmds: [["blkload 1 radtest_mod", 2]],
	check: {
		var unit = RadUnit.readBlock(\block1), mod = unit !? { unit[\mods] } !? {|m| m[\freq] };
		case
		{ mod.isNil } { "no modulator: " ++ unit.cs }
		{ mod[\type] != \sin } { "type: " ++ mod.cs }
		{ mod[\args][\freq] != 3 } { "modulator's own settings: " ++ mod.cs }
		{ true }
	},
	teardown: ~radBlockPresetClean
);

// a recorded buffer is not a file: the preset leaves it out, and a block
// playing one keeps it when a preset loads
~radTest.("block-preset-recorded-buffer",
	setup: ["start 2", "@ 2", ["blk 1 loop @1", 2], "blkset 1 rate 0.5", "blksave 1 radtest_r", "blkset 1 rate 2"],
	cmds: [["blkload 1 radtest_r", 2]],
	check: {
		var unit = RadUnit.readBlock(\block1), file = RadUnit.readPreset(\block, \loop, \radtest_r);
		case
		{ file.isNil } { "no preset" }
		{ file[\buffer].notNil } { "preset holds a buffer: " ++ file.cs }
		{ unit[\buffer] != (rec: 1) } { "buffer: " ++ unit[\buffer].cs }
		{ unit[\args][\rate] != 0.5 } { "rate: " ++ unit[\args].cs }
		{ true }
	},
	teardown: ~radBlockPresetClean
);

// recording blocks and patterns have no presets
~radTest.("block-preset-recording-def",
	setup: ["start 2"],
	check: {
		(RadUnit.recordsDef(\rec) and: { RadUnit.recordsDef(\recloop) } and: { RadUnit.recordsDef(\loop).not }
			and: { RadUnit.recordsDef(\sine).not }) or: { "recordsDef wrong" }
	}
);

~radTest.("block-preset-pattern-refused",
	setup: ["start 2", ["pn 1 test1", 2]],
	cmds: ["blksave 1 radtest_p"],
	check: {
		RadUnit.allPresets(\block).any{|pair| pair[1] == \radtest_p }.not or: { "a pattern was saved" }
	},
	//stop the pattern before the server quits: on the Mac, a note it sends
	//while the server is quitting posts a FAILURE IN SERVER
	teardown: {
		if(Ndef(\block1).source.notNil, { Radicles.cW.callFunc("st 1"); 1.wait });
		~radBlockPresetClean.();
	}
);

// the list of a definition's presets
~radTest.("block-preset-names",
	setup: ["start 2", "blk 1 sine", "blksave 1 radtest_a", "blksave 1 radtest_b"],
	check: {
		var names = RadUnit.presetNames(\block, \sine);
		(names.includes(\radtest_a) and: { names.includes(\radtest_b) }) or: { "names: " ++ names.cs }
	},
	teardown: ~radBlockPresetClean
);

// ---- keeping a recording (4 Nov 2026): blksave N name keep [file] ----

// remove the recordings a test kept (named radtest_*, or the rec_ file it noted)
~radKeepClean = {
	(RadUnit.recordingsPath +/+ "radtest_*.wav").pathMatch.do{|path| File.delete(path) };
	~radKeptFile !? {|name| File.delete(RadUnit.recordingsPath +/+ name ++ ".wav") };
	~radKeptFile = nil;
	~radBlockPresetClean.();
};

// keep writes the recording to recordings/<file>.wav, the block plays the
// file (same values), and the preset holds the file
~radTest.("block-preset-keep",
	setup: ["start 2", "@ 2", ["blk 1 loop @1", 2], "blkset 1 rate 0.5"],
	cmds: [["blksave 1 radtest_k keep radtest_take", 4]],
	check: {
		var form = (play: \radtest_take, folder: \recordings), unit = RadUnit.readBlock(\block1),
		file = RadUnit.readPreset(\block, \loop, \radtest_k);
		case
		{ File.exists(RadUnit.recordingsPath +/+ "radtest_take.wav").not } { "no file in recordings" }
		{ file.isNil } { "no preset" }
		{ file[\buffer] != form } { "preset: " ++ file.cs }
		{ unit[\buffer] != form } { "block: " ++ unit.cs }
		{ unit[\args][\rate] != 0.5 } { "rate: " ++ unit[\args].cs }
		{ true }
	},
	teardown: ~radKeepClean
);

// without a file name: rec_<date>_<time>
~radTest.("block-preset-keep-name",
	setup: ["start 2", "@ 2", ["blk 1 loop @1", 2]],
	//the posted code holds the date and time: not captured, so the snapshot stays the same
	code: {
		var capture = Radicles.postCapture;
		Radicles.postCapture = nil;
		Radicles.cW.callFunc("blksave 1 radtest_k keep"); 4.wait;
		Radicles.postCapture = capture;
	},
	check: {
		var file = RadUnit.readPreset(\block, \loop, \radtest_k), name = file !? { file[\buffer] } !? {|b| b[\play] };
		~radKeptFile = name;
		case
		{ name.isNil } { "preset: " ++ file.cs }
		{ name.asString.beginsWith("rec_").not } { "name: " ++ name }
		{ File.exists(RadUnit.recordingsPath +/+ name ++ ".wav").not } { "no file " ++ name }
		{ true }
	},
	teardown: ~radKeepClean
);

// the kept file comes back like any sound file
~radTest.("block-preset-keep-load",
	setup: ["start 2", "@ 2", ["blk 1 loop @1", 2], ["blksave 1 radtest_k keep radtest_take", 4], ["lp 1 drums1", 4]],
	cmds: [["blkload 1 radtest_k", 4]],
	check: {
		var unit = RadUnit.readBlock(\block1);
		(unit[\buffer] == (play: \radtest_take, folder: \recordings)) or: { "block: " ++ unit.cs }
	},
	teardown: ~radKeepClean
);

// ...and from a block playing a new recording: the preset's file wins
~radTest.("block-preset-keep-load-over-recording",
	setup: ["start 2", "@ 2", ["blk 1 loop @1", 2], ["blksave 1 radtest_k keep radtest_take", 4], ["blk 1 loop @1", 4]],
	cmds: [["blkload 1 radtest_k", 4]],
	check: {
		var unit = RadUnit.readBlock(\block1);
		(unit[\buffer] == (play: \radtest_take, folder: \recordings)) or: { "block: " ++ unit.cs }
	},
	teardown: ~radKeepClean
);

// nobuf keeps the recording
~radTest.("block-preset-keep-nobuf",
	setup: ["start 2", "@ 2", ["blk 1 loop @1", 2], ["blksave 1 radtest_k keep radtest_take", 4], ["blk 1 loop @1", 4]],
	cmds: [["blkload 1 radtest_k nobuf", 3]],
	check: {
		var unit = RadUnit.readBlock(\block1);
		(unit[\buffer] == (rec: 1)) or: { "block: " ++ unit.cs }
	},
	teardown: ~radKeepClean
);

// a block playing no recording: keep has nothing to keep, the preset is saved
~radTest.("block-preset-keep-nothing",
	setup: ["start 2", "blk 1 sine"],
	cmds: [["blksave 1 radtest_k keep", 1]],
	check: { RadUnit.readPreset(\block, \sine, \radtest_k).notNil or: { "not saved" } },
	teardown: ~radKeepClean
);

// a file that exists is not overwritten, and nothing is saved
~radTest.("block-preset-keep-exists",
	setup: ["start 2", "@ 2", ["blk 1 loop @1", 2]],
	code: {
		File.mkdir(RadUnit.recordingsPath);
		File.use(RadUnit.recordingsPath +/+ "radtest_take.wav", "w", {|f| f.write("x") });
		Radicles.cW.callFunc("blksave 1 radtest_k keep radtest_take"); 3.wait;
	},
	check: {
		case
		{ RadUnit.readPreset(\block, \loop, \radtest_k).notNil } { "saved anyway" }
		{ File.fileSize(RadUnit.recordingsPath +/+ "radtest_take.wav") != 1 } { "file overwritten" }
		{ RadUnit.readBlock(\block1)[\buffer] != (rec: 1) } { "block changed: " ++ RadUnit.readBlock(\block1).cs }
		{ true }
	},
	teardown: ~radKeepClean
);

// ---- the source window's PRESET menu and SAVE button (Mac: needs windows) ----

~radBlkWin = { (Radicles.aZ.sourceWindows ? []).detect{|w| w.isClosed.not and: { w.name.beginsWith("block1 ") } } };
~radBlkWinsClose = {
	(Radicles.aZ !? {|a| a.sourceWindows } ? []).copy.do{|w| w.close };
	Window.allWindows.select{|w| w.name.beginsWith("Save preset") }.do(_.close);
};

// the menu lists the definition's presets; choosing one loads it, like blkload
~radTest.("block-preset-menu",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 330", "blksave 1 radtest_menu", "blkset 1 freq 500", "blkgui 1"],
	code: {
		var menu;
		0.5.wait;
		menu = ~radBlkWin.() !? {|w| w.canvas.children.detect{|v| v.isKindOf(PopUpMenu) } };
		~radMenuItems = menu !? {|m| m.items };
		menu !? {|m| m.valueAction = ~radMenuItems.indexOfEqual("radtest_menu") };
		2.wait;
	},
	check: {
		case
		{ ~radMenuItems.isNil } { "no PRESET menu" }
		{ ~radMenuItems.keep(2) != ["PRESET", "default"] or: { ~radMenuItems.includesEqual("radtest_menu").not } } {
			"items: " ++ ~radMenuItems.cs }
		{ Ndef(\block1).get(\freq) != 330 } { "not loaded: " ++ Ndef(\block1).get(\freq) }
		{ ~radBlkWin.().isNil } { "the window did not come back" }
		{ true }
	},
	teardown: { ~radBlkWinsClose.(); ~radBlockPresetClean.() },
	gui: true
);

// SAVE asks for a name, saves like blksave, and the menu lists it
~radTest.("block-preset-save-button",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 330", "blkgui 1"],
	code: {
		var win, popup;
		0.5.wait;
		win = ~radBlkWin.();
		win.canvas.children.detect{|v| v.isKindOf(Button) and: { v.states[0][0] == "SAVE" } }.valueAction = 0;
		0.5.wait;
		popup = Window.allWindows.detect{|w| w.name.beginsWith("Save preset") };
		~radPopup = popup !? {|w| w.name };
		popup.view.children.detect{|v| v.isKindOf(TextField) }.string_("radtest_btn").doAction;
		1.wait;
		~radMenuItems = win.canvas.children.detect{|v| v.isKindOf(PopUpMenu) }.items;
	},
	check: {
		case
		{ ~radPopup != "Save preset · block1 | sine" } { "save window: " ++ ~radPopup.cs }
		{ RadUnit.readPreset(\block, \sine, \radtest_btn) !? {|u| u[\args][\freq] } != 330 } { "not saved" }
		{ ~radMenuItems.includesEqual("radtest_btn").not } { "menu: " ++ ~radMenuItems.cs }
		{ true }
	},
	teardown: { ~radBlkWinsClose.(); ~radBlockPresetClean.() },
	gui: true
);

// a block playing a recording: the SAVE window offers to keep it as a file
~radTest.("block-preset-save-button-keep",
	setup: ["start 2", "@ 2", ["blk 1 loop @1", 2], "blkgui 1"],
	code: {
		var win, popup, box, capture = Radicles.postCapture;
		0.5.wait;
		win = ~radBlkWin.();
		win.canvas.children.detect{|v| v.isKindOf(Button) and: { v.states[0][0] == "SAVE" } }.valueAction = 0;
		0.5.wait;
		popup = Window.allWindows.detect{|w| w.name.beginsWith("Save preset") };
		box = popup !? {|w| w.view.children.detect{|v| v.isKindOf(CheckBox) } };
		~radKeepBox = box.notNil;
		box !? {|b| b.value = true };
		//the posted code holds the file's date and time: not captured
		Radicles.postCapture = nil;
		popup.view.children.detect{|v| v.isKindOf(TextField) }.string_("radtest_kb").doAction;
		4.wait;
		Radicles.postCapture = capture;
	},
	check: {
		var file = RadUnit.readPreset(\block, \loop, \radtest_kb), name = file !? { file[\buffer] } !? {|b| b[\play] };
		~radKeptFile = name;
		case
		{ ~radKeepBox.not } { "no keep check box" }
		{ name.isNil } { "preset: " ++ file.cs }
		{ File.exists(RadUnit.recordingsPath +/+ name ++ ".wav").not } { "no file " ++ name }
		{ true }
	},
	teardown: { ~radBlkWinsClose.(); ~radKeepClean.() },
	gui: true
);
