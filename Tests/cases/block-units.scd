// Blocks in the document (Document Model, step 2, PR 1): RadUnit reads a
// playing block (blocks.block1) from the running system, and hooks in Block
// and ModMap keep RadDoc.current in step. Every test also runs the
// consistency check after each command (Tests/child.scd). A buffer is kept
// by what it is (a file's name and Play folder, a size, a recording buffer),
// never by its number, so a block can be played again from its unit.

// the document's unit at an address, values refreshed from the running system
~radUnitAt = {|address| RadUnit.refresh(RadDoc.current.copy).at(address) };
// play a unit on another block (as a preset or a session will), then read it
~radReplay = {|unit, block|
	var cond = Condition(false);
	Block.play(block, unit[\def], Block.bufferArgOf(unit[\buffer]),
		unit[\args].asKeyValuePairs, unit[\data], action: { cond.test = true; cond.signal });
	cond.wait;
	2.wait;
	RadUnit.readBlock(("block" ++ block).asSymbol)
};
// a unit and its replay are the same, apart from what can't be (nothing yet)
~radSame = {|a, b|
	var diff = RadDoc.diff(a, b, 1e-6);
	diff.isEmpty or: { diff.collect{|d| d[\address] ++ " " ++ d[\old].cs ++ " -> " ++ d[\new].cs }.join("; ") }
};

~radTest.("block-unit-sine",
	setup: ["start 2"],
	cmds: ["blk 1 sine", "blkset 1 freq 330"],
	check: {
		var unit = ~radUnitAt.("blocks.block1");
		(unit.notNil and: { unit[\kind] == \block } and: { unit[\def] == \sine }
			and: { unit[\args][\freq] == 330 } and: { unit[\buffer].isNil } and: { unit[\data].isNil })
		or: { "unit: " ++ unit.cs }
	}
);

// a stopped block leaves the document; the blocks Event goes with the last one
~radTest.("block-unit-stop",
	setup: ["start 2", "blk 1 sine", "blk 2 sine"],
	cmds: ["st 1", "st 2"],
	check: {
		RadDoc.current.at(\blocks).isNil or: { "blocks: " ++ RadDoc.current.at(\blocks).cs }
	}
);

// another block in the same block: only its own arguments
~radTest.("block-unit-replaced",
	setup: ["start 2", "blk 1 sinekey", "blkset 1 key 5"],
	cmds: ["blk 1 sine"],
	check: {
		var unit = ~radUnitAt.("blocks.block1");
		(unit[\def] == \sine and: { unit[\args].includesKey(\key).not }) or: { "unit: " ++ unit.cs }
	}
);

// a sound file by its name and Play folder, never by its buffer number
~radTest.("block-unit-sound-file",
	setup: ["start 2"],
	cmds: [["lp 1 marilyn1", 2]],
	check: {
		var unit = ~radUnitAt.("blocks.block1");
		(unit[\def] == \loop and: { unit[\buffer] == (play: \marilyn1, folder: BStore.playFolder) })
		or: { "unit: " ++ unit.cs }
	}
);

// played again from its unit on another block: the same unit, its own buffer
~radTest.("block-unit-sound-file-replay",
	setup: ["start 2", ["lp 1 marilyn1", 2], "blkset 1 rate 0.5"],
	code: { ~radResult = ~radSame.(RadUnit.readBlock(\block1), ~radReplay.(RadUnit.readBlock(\block1), 2)) },
	check: { ~radResult }
);

// a wavetable: the table by its name, the buffer by its size (the save/load
// audit found wavetable blocks could not be played again)
~radTest.("block-unit-wavetable",
	setup: ["start 2"],
	cmds: [["blk 1 osc1 sine1harm6", 2]],
	check: {
		var unit = ~radUnitAt.("blocks.block1");
		(unit[\def] == \osc1 and: { unit[\data] == \sine1harm6 }
			and: { unit[\buffer] == (alloc: Block.wavetableSize) })
		or: { "unit: " ++ unit.cs }
	}
);

~radTest.("block-unit-wavetable-replay",
	setup: ["start 2", ["blk 1 osc1 sine1harm6", 2]],
	code: { ~radResult = ~radSame.(RadUnit.readBlock(\block1), ~radReplay.(RadUnit.readBlock(\block1), 2)) },
	check: { ~radResult }
);

// a recording buffer by its number in @
~radTest.("block-unit-recording-buffer",
	setup: ["start 2", "@ 2"],
	cmds: [["blk 1 loop @1", 2]],
	check: {
		var unit = ~radUnitAt.("blocks.block1");
		(unit[\buffer] == (rec: 1)) or: { "unit: " ++ unit.cs }
	}
);

// a modulated argument: its modulator by what it is, with its own settings,
// and the value it returns to (sessions lost the modulator's settings)
~radTest.("block-unit-modulated",
	setup: ["start 2", "blk 1 sine"],
	cmds: ["blkset 1 freq sin", "modblkset 1 freq [freq, 3]"],
	check: {
		var unit = ~radUnitAt.("blocks.block1"), mod = unit !? { unit[\mods] } !? {|m| m[\freq] };
		case
		{ mod.isNil } { "no modulator: " ++ unit.cs }
		{ mod[\type] != \sin } { "type: " ++ mod.cs }
		{ mod[\args][\freq] != 3 } { "modulator's own settings: " ++ mod.cs }
		{ unit[\args][\freq].isNumber.not } { "value to return to: " ++ unit.cs }
		{ true }
	}
);

~radTest.("block-unit-unmodulated",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq sin"],
	cmds: ["unmodblk 1 freq"],
	check: {
		var unit = ~radUnitAt.("blocks.block1");
		unit[\mods].isNil or: { "unit: " ++ unit.cs }
	}
);

// a modulated modulator nests in the unit (no command makes one yet: code)
~radTest.("block-unit-modulated-modulator",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq sin"],
	code: { ModMap.map(Ndef(\mod1), \phase, \sin, [0, 1]) },
	check: {
		var unit = ~radUnitAt.("blocks.block1");
		var inner = unit !? { unit[\mods] } !? {|m| m[\freq] } !? {|m| m[\mods] } !? {|m| m[\phase] };
		inner.notNil or: { "unit: " ++ unit.cs }
	}
);

// removing blocks and clearing them all
~radTest.("block-unit-remove",
	setup: ["start 2", "blk 1 sine", "blk 2 sine"],
	cmds: ["blk remove 1"],
	check: {
		var blocks = RadDoc.current.at(\blocks) ? ();
		(blocks.keys == Set[\block2]) or: { "blocks: " ++ blocks.keys.cs }
	}
);

// patterns are not in the document yet
~radTest.("block-unit-pattern-skipped",
	setup: ["start 2", "blk 1 sine"],
	cmds: [["pn 1 test1", 2]],
	check: { RadDoc.current.at(\blocks).isNil or: { RadDoc.current.at(\blocks).cs } }
);

// the document as a person reads it
~radTest.("block-unit-literal",
	setup: ["start 2", ["lp 1 marilyn1", 2], "blk 2 sine"],
	check: {
		var text = RadDoc.current.copy.asLiteral;
		(text.find("blocks: (").notNil and: { text.find("block1: (").notNil }
			and: { text.find("play: \\marilyn1").notNil })
		or: { text }
	}
);
