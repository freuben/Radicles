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

// The source decides the channel: typed and menu on radpost (volumes -1, -2),
// gui, hid and osc on radpostcont (-3, -4, -5).
~radTest.("radcode-sources",
	setup: ["start 2"],
	code: {
		[\typed, \menu, \gui, \hid, \osc].do{|source, i|
			Radicles.withSource(source, {
				("Ndef('track1').set('volume', " ++ (i.neg - 1) ++ ");").radcode(\radpostcont);
			});
		};
	},
	check: { Ndef('track1').get('volume') == -5 }
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
