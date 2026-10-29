// Source menu (dev, Oct 2026): a track's source slot works like an fx slot.
// Empty, it opens a block menu; blocks with a buffer get a second list (sound
// files, recording buffers, wavetables, loaded buffers). Each choice runs the
// matching typed command, so the menu tests post the same code as the typed
// tests next to them. Menu tests need Qt (Mac). vosc and patterns: later.

~radAZ = { Radicles.aZ };
~radMenuList = { ~radAZ.().instVarAt(\srcMenuWindow) !? {|w| w.view.children[0] } };
// pick an item in the open source menu by its words
~radPick = {|words|
	var list = ~radMenuList.(), index;
	index = list !? {|l| l.items.indexOfEqual(words) };
	if(index.notNil, { list.valueAction = index }, { ("menu has no " ++ words.cs).warn });
	0.3.wait;
};
~radCloseMenu = { ~radAZ.() !? {|a| a.prCloseSourceMenu }; (~radAZ.() !? {|a| a.sourceWindows } ? []).copy.do(_.close) };

// what each block does with its buffer, read from its code
~radTest.("buffer-kinds",
	check: {
		var kinds = [\sine, \loop, \cue, \osc1, \shaperSine, \vosc].collect{|b| Block.bufferKind(b) };
		(kinds == [nil, \play, \cue, \wavetable, \wavetable, \vosc]) or: { "kinds: " ++ kinds.cs }
	}
);

// typed: a wavetable block with a wavetable makes a 2048-sample buffer filled with it
~radTest.("typed-wavetable",
	setup: ["start 2"],
	cmds: [["blk 1 osc1 sine1harm6", 2]],
	check: {
		var live = Block.liveBlocks[0];
		(live.notNil and: { live[1] == 'osc1' } and: { BStore.bstores.last.numFrames == 2048 })
		or: { "live: " ++ live.cs }
	}
);

// typed: a recording buffer in blk, as pl and lp take it
~radTest.("typed-recording-buffer",
	setup: ["start 2", "@ 2"],
	cmds: [["blk 1 loop @1", 2]],
	check: {
		(Block.liveBlocks[0] !? {|l| l[1] } == 'loop') or: { "live: " ++ Block.liveBlocks[0].cs }
	}
);

// typed: a loaded buffer by its tag (here the wavetable buffer block 1 made)
~radTest.("typed-loaded-buffer",
	setup: ["start 2", ["blk 1 osc1 sine1harm6", 2]],
	code: {
		~radTag = (BStore.bstoreIDs.detect{|id| id[0] == \alloc } ? [nil, \none])[1];
		Radicles.cW.callFunc("blk 2 osc1 " ++ ~radTag);
		2.wait;
	},
	check: {
		var allocs = BStore.bstoreIDs.select{|id| id[0] == \alloc };
		(Block.liveBlocks[1] !? {|l| l[1] } == 'osc1' and: { allocs.size == 1 })
		or: { "tag: " ++ ~radTag ++ ", allocs: " ++ allocs.size ++ ", live: " ++ Block.liveBlocks[1].cs }
	}
);

// menu: a block without a buffer plays straight away (same code as 'blk 1 sine')
~radTest.("menu-block",
	setup: ["start 2", "mix"],
	code: {
		~radAZ.().blockMenu(1, Rect(100, 100, 140, 240));
		0.3.wait;
		~radPick.("sine");
		1.wait;
		~radSlot = ~radAZ.().instVarAt(\srcButtonArr)[0].string;
	},
	check: {
		((Block.liveBlocks[0] !? {|l| l[1] }) == 'sine' and: { ~radSlot == "sine" } and: { ~radMenuList.().isNil })
		or: { "live: " ++ Block.liveBlocks[0].cs ++ ", slot: " ++ ~radSlot.cs }
	},
	teardown: { ~radCloseMenu.(); Radicles.cW.callFunc("nomix") },
	gui: true
);

// menu: loop, then a sound file (same code as 'blk 1 loop drums1')
~radTest.("menu-sound-file",
	setup: ["start 2"],
	code: {
		~radAZ.().blockMenu(1, Rect(100, 100, 140, 240));
		0.3.wait;
		~radPick.("loop");
		~radItems = ~radMenuList.() !? {|l| l.items };
		~radPick.("drums1");
		2.wait;
	},
	check: {
		((Block.liveBlocks[0] !? {|l| l[1] }) == 'loop' and: { ~radItems.includesEqual("drums1") })
		or: { "live: " ++ Block.liveBlocks[0].cs ++ ", items: " ++ ~radItems.cs }
	},
	teardown: ~radCloseMenu,
	gui: true
);

// menu: osc1, then a wavetable (same code as 'blk 1 osc1 sine1harm6')
~radTest.("menu-wavetable",
	setup: ["start 2"],
	code: {
		~radAZ.().blockMenu(1, Rect(100, 100, 140, 240));
		0.3.wait;
		~radPick.("osc1");
		~radPick.("sine1harm6");
		2.wait;
	},
	check: {
		((Block.liveBlocks[0] !? {|l| l[1] }) == 'osc1') or: { "live: " ++ Block.liveBlocks[0].cs }
	},
	teardown: ~radCloseMenu,
	gui: true
);

// vosc is left out of the menu for now
~radTest.("menu-leaves-out-vosc",
	setup: ["start 2"],
	code: {
		~radAZ.().blockMenu(1, Rect(100, 100, 140, 240));
		0.3.wait;
		~radItems = ~radMenuList.() !? {|l| l.items } ? [];
	},
	check: {
		(~radItems.includesEqual("sine") and: { ~radItems.includesEqual("vosc").not })
		or: { "items: " ++ ~radItems.cs }
	},
	teardown: ~radCloseMenu,
	gui: true
);

// the slot empties when the block stops; an empty slot opens the menu
~radTest.("slot-empties-on-stop",
	setup: ["start 2", "blk 1 sine", "mix"],
	cmds: [["st 1", 2]],
	code: {
		~radSlot = ~radAZ.().instVarAt(\srcButtonArr)[0].string;
		~radAZ.().instVarAt(\srcButtonArr)[0].doAction;
		0.5.wait;
		~radMenuOpen = ~radMenuList.().notNil;
	},
	check: {
		((~radSlot == "") and: { ~radMenuOpen }) or: { "slot: " ++ ~radSlot.cs ++ ", menu open: " ++ ~radMenuOpen }
	},
	teardown: { ~radCloseMenu.(); Radicles.cW.callFunc("nomix") },
	gui: true
);
