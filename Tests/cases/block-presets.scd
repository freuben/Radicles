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
	setup: ["start 2", "pn 1 test1"],
	cmds: ["blksave 1 radtest_p"],
	check: {
		RadUnit.allPresets(\block).any{|pair| pair[1] == \radtest_p }.not or: { "a pattern was saved" }
	},
	teardown: ~radBlockPresetClean
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
