// Source windows (dev, Oct 2026): an argument window for a track's source (a
// block), built by the same code as the effect windows. Opened from the
// source button under a track's input menu, or with 'blkgui'. Most tests need
// Qt, so they run on the Mac only.

~radAZ = { Radicles.aZ };
~radSrcWins = { (~radAZ.().sourceWindows ? []) };
~radSrcWin = {|key| ~radSrcWins.().detect{|w| w.name.beginsWith(key.asString ++ " ") } };
~radKids = {|win, class| win.canvas.children.select{|v| v.isKindOf(class) } };
~radCloseWins = {
	~radSrcWins.().copy.do{|w| w.close };
	(~radAZ.() !? {|a| a.filtersWindow } ? []).copy.do{|w| w.close };
};

// the effect window is built by the shared code and looks as before:
// a REMOVE FILTER button and a slider per argument with a spec
~radTest.("fx-window-shared-builder",
	setup: ["start 2", "fx t 1 pch"],
	code: {
		var win;
		~radAZ.().filterGUI(\track, 1, 1);
		0.5.wait;
		win = ~radAZ.().filtersWindow.last;
		~radFxButton = ~radKids.(win, Button).collect(_.string);
		~radFxSliders = ~radKids.(win, Slider);
		~radFxSliders[0].valueAction = 0.5;
	},
	check: {
		((~radFxButton == ["R E M O V E   F I L T E R"]) and: { ~radFxSliders.size == 4 })
		or: { "buttons: " ++ ~radFxButton.cs ++ ", sliders: " ++ ~radFxSliders.size }
	},
	teardown: ~radCloseWins,
	gui: true
);

// blkgui opens the block's window: CLOSE WINDOW and a slider per argument;
// a slider posts on radpostcont
~radTest.("blkgui-opens-window",
	setup: ["start 2", "blk 1 sine"],
	cmds: ["blkgui 1"],
	code: {
		var win;
		0.5.wait;
		win = ~radSrcWin.(\block1);
		~radSrcName = win !? {|w| w.name };
		~radSrcButton = win !? {|w| ~radKids.(w, Button).collect(_.string) };
		~radSrcSliders = win !? {|w| ~radKids.(w, Slider) } ? [];
		~radSrcSliders.first !? {|sl| sl.valueAction = 0.5 };
	},
	check: {
		((~radSrcName == "block1 | sine") and: { ~radSrcButton == ["C L O S E   W I N D O W"] }
			and: { ~radSrcSliders.size == 3 })
		or: { "window: " ++ ~radSrcName.cs ++ ", buttons: " ++ ~radSrcButton.cs
			++ ", sliders: " ++ ~radSrcSliders.size }
	},
	teardown: ~radCloseWins,
	gui: true
);

// the CLOSE WINDOW button closes the window and leaves the block playing
~radTest.("close-window-button",
	setup: ["start 2", "blk 1 sine", "blkgui 1"],
	code: {
		0.5.wait;
		~radSrcWin.(\block1) !? {|w| ~radKids.(w, Button).first.doAction };
		0.5.wait;
	},
	check: {
		(~radSrcWin.(\block1).isNil and: { Ndef('block1').source.notNil })
		or: { "window still open, or block stopped" }
	},
	teardown: ~radCloseWins,
	gui: true
);

// no block playing: a warning, no window (no Qt needed)
~radTest.("blkgui-no-block",
	setup: ["start 2"],
	cmds: ["blkgui 1"],
	check: { ~radSrcWins.().isEmpty or: { "a window opened" } }
);

// the source button under track 1's input menu shows its source and opens its window
~radTest.("source-button-opens-window",
	setup: ["start 2", "blk 1 sine", "mix"],
	code: {
		~radSrcLabel = ~radAZ.().instVarAt(\srcButtonArr)[0].string;
		~radAZ.().instVarAt(\srcButtonArr)[0].doAction;
		0.5.wait;
	},
	check: {
		((~radSrcLabel == "block1") and: { ~radSrcWin.(\block1).notNil })
		or: { "label: " ++ ~radSrcLabel.cs ++ ", window: " ++ ~radSrcWin.(\block1).notNil }
	},
	teardown: { ~radCloseWins.(); Radicles.cW.callFunc("nomix") },
	gui: true
);

// buses and the master have no source button
~radTest.("no-source-button-on-buses",
	setup: ["start 2 1", "mix"],
	code: { ~radSrcKinds = ~radAZ.().instVarAt(\srcButtonArr).collect{|v| v.class.name } },
	check: {
		(~radSrcKinds == [\Button, \Button, \StaticText, \StaticText])
		or: { "kinds: " ++ ~radSrcKinds.cs }
	},
	teardown: { Radicles.cW.callFunc("nomix") },
	gui: true
);

// changing track 1's source while its window is open swaps the window
~radTest.("source-change-swaps-window",
	setup: ["start 2", "blk 1 sine", "blk 2 sine", "mix"],
	code: {
		~radAZ.().openTrackSource(1);
		0.5.wait;
		~radAZ.().setTrackIn(1, 1);   // input menu item 1: block2
		1.wait;
		~radSrcLabel = ~radAZ.().instVarAt(\srcButtonArr)[0].string;
	},
	check: {
		(~radSrcWin.(\block1).isNil and: { ~radSrcWin.(\block2).notNil } and: { ~radSrcLabel == "block2" })
		or: { "block1 open: " ++ ~radSrcWin.(\block1).notNil ++ ", block2 open: "
			++ ~radSrcWin.(\block2).notNil ++ ", label: " ++ ~radSrcLabel.cs }
	},
	teardown: { ~radCloseWins.(); Radicles.cW.callFunc("nomix") },
	gui: true
);

// a new block in the same slot rebuilds the window for its arguments
~radTest.("new-block-rebuilds-window",
	setup: ["start 2", "blk 1 sine", "blkgui 1"],
	cmds: [["blk 1 sinekey", 2]],
	check: {
		(~radSrcWin.(\block1) !? {|w| w.name }) == "block1 | sinekey"
		or: { "window: " ++ (~radSrcWin.(\block1) !? {|w| w.name }).cs }
	},
	teardown: ~radCloseWins,
	gui: true
);

// stopping the block closes its window
~radTest.("stop-closes-window",
	setup: ["start 2", "blk 1 sine", "blkgui 1"],
	cmds: [["st 1", 1]],
	check: { ~radSrcWin.(\block1).isNil or: { "window still open" } },
	teardown: ~radCloseWins,
	gui: true
);

// modulating an argument redraws the window with that argument greyed out
~radTest.("modulation-greys-argument",
	setup: ["start 2", "blk 1 sine", "blkgui 1"],
	cmds: [["blkset 1 freq sin", 2]],
	check: {
		var sliders = ~radSrcWin.(\block1) !? {|w| ~radKids.(w, Slider) } ? [];
		(sliders.size == 3 and: { sliders.count{|sl| sl.enabled.not } == 1 })
		or: { "sliders: " ++ sliders.size ++ ", disabled: " ++ sliders.count{|sl| sl.enabled.not } }
	},
	teardown: ~radCloseWins,
	gui: true
);
