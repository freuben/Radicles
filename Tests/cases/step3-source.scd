// Step 3, PR 1: source tracking and .radcode. Nothing in Radicles uses
// .radcode yet; these tests check the pieces on their own.

// With no source, .radcode posts on the channel the method asks for.
~radTest.("radcode-no-source",
	setup: ["start 2"],
	code: {
		"Ndef('track1').set('volume', -3);".radcode;
		"Ndef('track1').set('volume', -4);".radcode(\radpostcont);
	},
	check: { Ndef('track1').get('volume') == -4 }
);

// The source decides the channel: typed, menu and button on radpost
// (volumes -1, -2, -3), gui, hid and osc on radpostcont (-4, -5, -6).
~radTest.("radcode-sources",
	setup: ["start 2"],
	code: {
		[\typed, \menu, \button, \gui, \hid, \osc].do{|source, i|
			Radicles.withSource(source, {
				("Ndef('track1').set('volume', " ++ (i.neg - 1) ++ ");").radcode(\radpostcont);
			});
		};
	},
	check: { Ndef('track1').get('volume') == -6 }
);

// Routines started inside withSource keep the source; ~variables written
// inside it land in the surrounding environment; outside, there is no source.
~radTest.("source-inherited-by-routines",
	code: {
		~radSrcSeen = nil;
		Radicles.withSource(\gui, {
			fork { 0.1.wait; fork { 0.1.wait; ~radSrcSeen = Radicles.source } };
		});
		Radicles.withSource(\typed, { ~radSrcWritten = 1 });
		0.5.wait;
	},
	check: { (~radSrcSeen == \gui) and: { ~radSrcWritten == 1 } and: { Radicles.source.isNil } }
);

// A GUI or controller action sets its source only when there is none: a typed
// command that presses a mixer button (mute t 1) stays typed.
~radTest.("action-keeps-existing-source",
	code: {
		var action = Radicles.srcFunc(\gui, { Radicles.source });
		~radSrcAlone = action.value;
		~radSrcTyped = Radicles.withSource(\typed, { action.value });
	},
	check: { (~radSrcAlone == \gui) and: { ~radSrcTyped == \typed } }
);

// Commands typed in the call window have the source \typed; a command run by
// a controller keeps the controller's source. The capture hook receives it.
~radTest.("typed-command-source",
	setup: ["start 2"],
	code: {
		var capture = Radicles.postCapture;
		~radSrcLog = [];
		Radicles.postCapture = {|str, channel, source|
			~radSrcLog = ~radSrcLog.add(source);
			capture.value(str, channel, source);
		};
		Radicles.cW.callFunc("vol t 1 -10");
		Radicles.withSource(\hid, { Radicles.cW.callFunc("vol t 2 -12") });
	},
	check: { (~radSrcLog.asSet == Set[\typed, \hid]) and: { Ndef('track2').get('volume') == -12 } }
);

// A controller mapping (here OSC) runs its code with the source \osc.
~radTest.("controller-source",
	setup: ["start 2"],
	code: {
		var capture = Radicles.postCapture;
		~radSrcLog = [];
		Radicles.postCapture = {|str, channel, source|
			~radSrcLog = ~radSrcLog.add(source);
			capture.value(str, channel, source);
		};
		HIDMap.map(Ndef('track1'), \volume, \osc, [-60, 0], ['/radtestvol']);
		0.2.wait;
		NetAddr.localAddr.sendMsg('/radtestvol', 0.5);
		0.5.wait;
	},
	check: { ~radSrcLog.includes(\osc) and: { Ndef('track1').get('volume') == -30 } },
	teardown: { OSCdef.freeAll }
);

// GUI widgets get their source from their kind (Mac only: needs Qt).
~radTest.("gui-widget-sources",
	code: { ~radKinds = [PopUpMenu(), ListView(), Button(), Knob(), Slider(), TextField()].collect(_.radSourceKind) },
	check: { ~radKinds == [\menu, \menu, \button, \gui, \gui, \gui] },
	gui: true
);

// PR 2: the same mixer method posts by its source. A slider moving volume
// stays on radpostcont (-7); a button or a typed command goes to radpost (-8, -9).
~radTest.("mixer-method-by-source",
	setup: ["start 2"],
	code: {
		Radicles.withSource(\gui, { Radicles.aZ.setVolume(\track, 1, -7) });
		Radicles.withSource(\button, { Radicles.aZ.setVolume(\track, 1, -8) });
		Radicles.withSource(\typed, { Radicles.aZ.setVolume(\track, 1, -9) });
	},
	check: { Ndef('track1').get('volume') == -9 }
);

// Radicles.defer keeps the source (plain defer loses it), so a command a
// controller toggles still counts as the controller's.
~radTest.("defer-keeps-source",
	code: {
		~radSrcDeferred = nil;
		Radicles.withSource(\hid, { Radicles.defer({ ~radSrcDeferred = Radicles.source }) });
		0.3.wait;
	},
	check: { ~radSrcDeferred == \hid }
);

// PR 3 (Federico, 27 Oct, option B): a controller mapped to a command with no
// value in it (a trigger, like a MIDI button running fx 1 pch) posts like the
// command typed (main document, source \hidcmd); a controller whose value goes
// into the command ($) is continuous control (post window, \hid).
~radTest.("controller-mapped-commands",
	setup: ["start 2"],
	cmds: ["(osc /radtestfx <> fx 1 pch)", "(osc /radtestvol [-60,0] <> vol t 1 $)"],
	code: {
		var capture = Radicles.postCapture;
		~radSrcLog = [];
		Radicles.postCapture = {|str, channel, source|
			~radSrcLog = ~radSrcLog.add(source);
			capture.value(str, channel, source);
		};
		NetAddr.localAddr.sendMsg('/radtestfx', 1);
		1.5.wait;
		NetAddr.localAddr.sendMsg('/radtestvol', 0.5);
		1.wait;
	},
	check: { (~radSrcLog.asSet == Set[\hidcmd, \hid]) and: { Ndef('track1').get('volume') == -30 }
		or: { "sources " ++ ~radSrcLog.asSet ++ ", volume " ++ Ndef('track1').get('volume') } },
	teardown: { OSCdef.freeAll }
);
