// Effect and block windows stay where they are: after loading a preset, a
// modulator added, or a block played again, the same window stays open and
// its rows show the new values (16 Nov 2026: a block window moved 20 px on
// every rebuild, an effect window jumped and came back). Mac only (GUI).

~radWpOpen = { (Radicles.aZ.filtersWindow ? []).select{|w| w.isClosed.not } };
~radWpSrc = { (Radicles.aZ.sourceWindows ? []).select{|w| w.isClosed.not } };
~radWpMoved = {|before, after|
	case
	{ after.isNil } { "the window was not reopened" }
	{ (after.x - before.x).abs > 2 or: { (after.y - before.y).abs > 2 } } { "moved from " ++ before ++ " to " ++ after }
	{ nil }
};
// the value texts shown in a window's rows
~radWpTexts = {|win|
	var texts = [], walk = {|view| view.children.do{|child|
		if(child.isKindOf(StaticText), { texts = texts.add(child.string) }); walk.(child) } };
	walk.(win.canvas ? win);
	texts
};
~radWpClose = {
	(Radicles.aZ !? {|a| a.filtersWindow } ? []).copy.do{|w| w.close };
	(Radicles.aZ !? {|a| a.sourceWindows } ? []).copy.do{|w| w.close };
};

~radTest.("fx-window-stays-on-preset-load",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fxsave t 1 1 radtest_wp"],
	code: {
		var win;
		//what these tests check is where the windows are, not the code posted
		Radicles.postCapture = nil;
		Radicles.aZ.filterGUI(\track, 1, 1); 0.5.wait;
		win = ~radWpOpen.().last;
		win.bounds = win.bounds.moveTo(400, 300); 0.5.wait;
		~radBefore = win.bounds.origin;
		Radicles.cW.callFunc("fxset t 1 1 pitchRatio 2"); 1.wait;
		Radicles.cW.callFunc("fxload t 1 1 radtest_wp");
		//as soon as the new window is there: does it open in place?
		~radEarly = nil;
		60.do{
			if(~radEarly.isNil, {
				~radWpOpen.().last !? {|w| if(w !== win, {
					0.05.wait;
					if(w.isClosed.not, { ~radEarly = w.bounds.origin });
				}) };
				0.05.wait;
			});
		};
		1.wait;
		~radAfter = ~radWpOpen.().last !? {|w| w.bounds.origin };
		~radSame = (~radWpOpen.().last === win) and: { win.isClosed.not };
		~radShown = ~radWpTexts.(win);
	},
	check: {
		case
		{ ~radSame.not } { "the window was closed and opened again" }
		{ ~radShown.includesEqual("0.5").not } { "the window shows " ++ ~radShown.cs ++ ", not pitchRatio 0.5" }
		{ ~radWpMoved.(~radBefore, ~radAfter) ?? {
			//a new window (none is expected now) must not open elsewhere either
			~radEarly !? {|early| ~radWpMoved.(~radBefore, early) !? {|m| "jumped: " ++ m } } ? true } }
	},
	teardown: {
		~radWpClose.();
		File.delete(RadUnit.presetPath(\filter, \pch, 'radtest_wp'));
	},
	gui: true
);

~radTest.("block-window-stays-on-preset-load",
	setup: ["start 2", "blk 1 sine", "blksave 1 radtest_wpb"],
	code: {
		var win;
		//what these tests check is where the windows are, not the code posted
		Radicles.postCapture = nil;
		Radicles.cW.callFunc("blkgui 1"); 1.wait;
		win = ~radWpSrc.().last;
		win.bounds = win.bounds.moveTo(400, 300); 0.5.wait;
		~radBefore = win.bounds.origin;
		~radFreq = Ndef(\block1).get(\freq);
		Radicles.cW.callFunc("blkset 1 freq 300"); 1.wait;
		Radicles.cW.callFunc("blkload 1 radtest_wpb"); 3.wait;
		~radAfter = ~radWpSrc.().last !? {|w| w.bounds.origin };
		~radSame = (~radWpSrc.().last === win) and: { win.isClosed.not };
		~radShown = ~radWpTexts.(win);
	},
	check: {
		case
		{ ~radSame.not } { "the window was closed and opened again" }
		{ ~radShown.includesEqual(~radFreq.asString.copyRange(0, 7)).not } {
			"the window shows " ++ ~radShown.cs ++ ", not freq " ++ ~radFreq }
		{ ~radWpMoved.(~radBefore, ~radAfter) ? true }
	},
	teardown: {
		~radWpClose.();
		File.delete(RadUnit.presetPath(\block, \sine, 'radtest_wpb'));
	},
	gui: true
);

// rebuilt three times: a window that moved 20 px each time ends 60 px away
~radTest.("block-window-stays-after-rebuilds",
	setup: ["start 2", "blk 1 sine"],
	code: {
		var win;
		//what these tests check is where the windows are, not the code posted
		Radicles.postCapture = nil;
		Radicles.cW.callFunc("blkgui 1"); 1.wait;
		win = ~radWpSrc.().last;
		win.bounds = win.bounds.moveTo(400, 300); 0.5.wait;
		~radBefore = win.bounds.origin;
		3.do{ Radicles.aZ.updateSourceWin(\block1); 1.wait };
		~radAfter = ~radWpSrc.().last !? {|w| w.bounds.origin };
		~radSame = (~radWpSrc.().last === win) and: { win.isClosed.not };
	},
	check: { if(~radSame.not, { "the window was closed and opened again" }, { ~radWpMoved.(~radBefore, ~radAfter) ? true }) },
	teardown: { ~radWpClose.() },
	gui: true
);
