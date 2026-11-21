// Mixer ramps (Nov 2026, docs/design/mixer-ramps.md): a strip's volume moves
// as a steady fader move. Typed values move over the strip's own time
// (vollag; a short smooth move by default); the mixer window's fader and
// mapped controls (MIDI, HID, OSC) over the short native time (ctllag). The
// time of a move is sent with the value only when the strip's last move had
// another, so the usual line is still set('volume', x).

~radMrCtl = {|key| Radicles.aZ.stripControl(Ndef(\track1), key) };
// the level of a strip's output (track 1's when none is named), read from the server
~radMrLevel = {|key = \track1, chan = 0|
	var bus = Bus.control(Server.default, 1), value, cond = Condition.new, synth;
	//the RMS over the last 0.1 s (a follower ripples with the sine)
	synth = { Out.kr(bus, A2K.kr((RunningSum.ar(InFeedback.ar(Ndef(key).bus.index + chan, 1).squared, 4800) / 4800).sqrt)) }.play;
	//the meter must be running before its 0.3 s: on a slow machine it was
	//not, and the level read 0
	Server.default.sync;
	0.3.wait;
	bus.get{|v| value = v; cond.unhang };
	cond.hang;
	synth.free; bus.free;
	value
};

// a typed vol posts what it always did, and the document has no lag for a
// strip that was never given one
~radTest.("vol-posts-as-before",
	setup: ["start 2"],
	cmds: ["vol t 1 -12", "vol t 1 -6"],
	check: {
		var one = RadUnit.readStrip(\track1);
		case
		{ ~radMrCtl.(\volume) != -6 } { "volume " ++ ~radMrCtl.(\volume) }
		{ one[\lags].notNil } { "lags in the document: " ++ one[\lags].cs }
		{ true }
	}
);

// vollag is the strip's own time for typed values: the next vol moves over it
~radTest.("vollag-is-the-typed-time",
	setup: ["start 2"],
	cmds: ["vollag t 1 2", "vol t 1 -12", "vol t 1 -6"],
	check: {
		var one = RadUnit.readStrip(\track1);
		case
		{ ~radMrCtl.(\lagTime) != 2 or: { ~radMrCtl.(\ramp) != 2 } } { "lagTime " ++ ~radMrCtl.(\lagTime) ++ ", ramp " ++ ~radMrCtl.(\ramp) }
		{ (one[\lags] ? ())[\volume] != 2 } { "document: " ++ one[\lags].cs }
		{ true }
	}
);

// a time given with the value is for that move only: the next plain vol
// moves over the strip's own time again
~radTest.("time-with-a-value-is-one-move",
	setup: ["start 2"],
	code: {
		Radicles.aZ.setVolume(\track, 1, -12, 3); 0.2.wait;
		~radMrOne = [~radMrCtl.(\ramp), ~radMrCtl.(\lagTime)];
		Radicles.cW.callFunc("vol t 1 -6"); 0.2.wait },
	check: {
		case
		{ ~radMrOne != [3, 0.05] } { "after the timed move: ramp, lagTime " ++ ~radMrOne.cs }
		{ ~radMrCtl.(\ramp) != 0.05 } { "the next vol moved over " ++ ~radMrCtl.(\ramp) }
		{ RadUnit.readStrip(\track1)[\lags].notNil } { "the time stuck: " ++ RadUnit.readStrip(\track1)[\lags].cs }
		{ true }
	}
);

// the sound: 0 dB to silence over 2 s is a steady fader move. Half way the
// fader is half way down, which is a quarter of the amplitude (-12 dB); at
// the end it is silent; and it comes back.
~radTest.("volume-is-a-steady-fader-move",
	setup: ["start 2", "blk 1 sine"],
	code: {
		1.wait;
		~radMrFull = ~radMrLevel.();
		Radicles.aZ.setVolume(\track, 1, -inf, 2);
		//the move starts a server latency (0.2 s) later and the reading takes
		//0.3 s: this one is taken 1 s into the move
		0.9.wait;
		~radMrHalf = ~radMrLevel.();
		1.2.wait;
		~radMrEnd = ~radMrLevel.();
		Radicles.cW.callFunc("vol t 1 0"); 0.3.wait;
		~radMrBack = ~radMrLevel.() },
	check: {
		var half = ~radMrHalf / ~radMrFull.max(0.000001);
		case
		{ ~radMrFull < 0.001 } { "no sound to measure: " ++ ~radMrFull }
		{ half < 0.15 or: { half > 0.4 } } { "half way the level is " ++ half.round(0.01) ++ " of full (a steady fader move: 0.25)" }
		{ ~radMrEnd > (~radMrFull * 0.01) } { "not silent at the end: " ++ (~radMrEnd / ~radMrFull).round(0.001) }
		{ (~radMrBack / ~radMrFull - 1).abs > 0.1 } { "did not come back: " ++ (~radMrBack / ~radMrFull).round(0.01) }
		{ true }
	},
	noWarnings: true
);

// a mapped control (OSC here, as MIDI and HID) moves over the native time,
// whatever the strip's own time is
~radTest.("mapped-control-uses-ctllag",
	setup: ["start 2", "vollag t 1 2", "vol t 1 -12"],
	code: {
		HIDMap.map(Ndef('track1'), \volume, \osc, [-60, 0], ['/radtestmrvol']);
		0.2.wait;
		NetAddr.localAddr.sendMsg('/radtestmrvol', 0.5); 0.5.wait },
	check: {
		case
		{ ~radMrCtl.(\volume) != -30 } { "volume " ++ ~radMrCtl.(\volume) }
		{ ~radMrCtl.(\ramp) != 0.05 } { "moved over " ++ ~radMrCtl.(\ramp) }
		{ ~radMrCtl.(\lagTime) != 2 } { "the strip's own time changed: " ++ ~radMrCtl.(\lagTime) }
		{ true }
	},
	teardown: { OSCdef.freeAll }
);

// the mixer window's fader too (Mac only: needs the window)
~radMrViews = {|view| [view] ++ (view.children ? []).collect{|child| ~radMrViews.(child) }.flat };
~radTest.("fader-uses-ctllag",
	setup: ["start 1", "vollag t 1 2", "vol t 1 -inf", ["mix", 1.5]],
	code: {
		var fader = ~radMrViews.(Radicles.aZ.mixerWin.canvas).detect{|v| v.isKindOf(Slider) and: { v.value == 0 } };
		~radMrFader = fader.notNil;
		fader !? { fader.valueAction = 0.5 };
		0.5.wait },
	check: {
		case
		{ ~radMrFader.not } { "no fader at the bottom" }
		{ ~radMrCtl.(\volume) == -inf } { "the fader did not move the volume" }
		{ ~radMrCtl.(\ramp) != 0.05 } { "moved over " ++ ~radMrCtl.(\ramp) }
		{ true }
	},
	gui: true
);

// ctllag sets and posts the native time
~radTest.("ctllag",
	setup: ["start 2", "vollag t 1 2", "vol t 1 -12"],
	cmds: ["ctllag", "ctllag 0.02", "ctllag"],
	code: {
		HIDMap.map(Ndef('track1'), \volume, \osc, [-60, 0], ['/radtestmrctl']);
		0.2.wait;
		NetAddr.localAddr.sendMsg('/radtestmrctl', 0.5); 0.5.wait },
	check: { (Radicles.ctlLag == 0.02 and: { ~radMrCtl.(\ramp) == 0.02 }) or: { "ctlLag " ++ Radicles.ctlLag ++ ", moved over " ++ ~radMrCtl.(\ramp) } },
	teardown: { OSCdef.freeAll; Radicles.ctlLag = 0.05 }
);

// a modulator on the volume: the ramp's time is 0 while it is mapped, or it
// would be dulled; the next typed value moves over the strip's time again
~radTest.("modulator-takes-the-ramp-off",
	setup: ["start 2"],
	cmds: [["vol t 1 sin", 2]],
	check: { (~radMrCtl.(\ramp) == 0) or: { "ramp " ++ ~radMrCtl.(\ramp) } }
);

// a modulator mapped while a slow move is under way: the sound stays a
// number (a ramp whose time is set to 0 mid-move gives infinity), and the
// same value typed again with another time does not send the time
~radTest.("ramp-time-never-changes-mid-move",
	setup: ["start 2", "blk 1 sine", "vollag t 1 4"],
	code: {
		Radicles.cW.callFunc("vol t 1 -20"); 0.6.wait;
		Radicles.cW.callFunc("vol t 1 sin"); 1.wait;
		~radMrMod = ~radMrLevel.();
		Radicles.cW.callFunc("vol t 2 -20"); 0.3.wait;
		Radicles.cW.callFunc("vollag t 2 0"); Radicles.cW.callFunc("vol t 2 -20"); 0.3.wait;
		~radMrSame = Ndef(\track2).nodeMap.at(\ramp) },
	check: {
		case
		{ ~radMrMod.isNaN or: { ~radMrMod.abs == inf } or: { ~radMrMod > 2 } } { "the level with the modulator: " ++ ~radMrMod }
		{ ~radMrSame.notNil } { "a time was sent with a value the strip already had: " ++ ~radMrSame }
		{ true }
	}
);

// ---- trim and sends (part 2) ----
// A strip's trim moves evenly in dB over the strip's own time (trimlag); a
// send's level is a steady fader move over the send's own time (sndlag). A
// time typed with sndset is for that move only.

~radMrIn = {|key| Radicles.aZ.stripControl(Ndef(\inTrack1), key) };
~radMrSend = {|key| Ndef(\busIn1).nodeMap.at(key) };

// trimlag is the strip's own time for typed trims, kept as a control of the
// strip (it used to rebuild the strip's input with Ndef.lag)
~radTest.("trimlag-is-the-typed-time",
	setup: ["start 2"],
	cmds: ["trim t 1 3", "trimlag t 1 2", "trim t 1 -3", "trim t 1 0"],
	check: {
		var one = RadUnit.readStrip(\track1);
		case
		{ ~radMrIn.(\lagTime) != 2 or: { ~radMrIn.(\ramp) != 2 } } { "lagTime " ++ ~radMrIn.(\lagTime) ++ ", ramp " ++ ~radMrIn.(\ramp) }
		{ (Ndef(\inTrack1).nodeMap.ratesFor([\trim]) ? [nil])[0].isNumber } { "the input was given an Ndef lag" }
		{ (one[\lags] ? ())[\trim] != 2 } { "document: " ++ one[\lags].cs }
		{ true }
	}
);

// the sound: trim 0 to -20 dB over 2 s is even in dB. Half way it is at
// -10 dB (0.32 of the amplitude); at the end at -20 dB (0.1).
~radTest.("trim-is-even-in-db",
	setup: ["start 2", "blk 1 sine"],
	code: {
		1.wait;
		~radMrFull = ~radMrLevel.();
		Radicles.aZ.setTrim(\track, 1, -20, 2);
		0.9.wait;
		~radMrHalf = ~radMrLevel.();
		1.2.wait;
		~radMrEnd = ~radMrLevel.() },
	check: {
		var half = ~radMrHalf / ~radMrFull.max(0.000001), end = ~radMrEnd / ~radMrFull.max(0.000001);
		case
		{ ~radMrFull < 0.001 } { "no sound to measure: " ++ ~radMrFull }
		{ half < 0.24 or: { half > 0.42 } } { "half way the level is " ++ half.round(0.01) ++ " of full (even in dB: 0.32)" }
		{ (end - 0.1).abs > 0.02 } { "at the end " ++ end.round(0.01) ++ " of full (-20 dB: 0.1)" }
		{ ~radMrIn.(\lagTime) != 0.05 } { "the time stuck to the strip: " ++ ~radMrIn.(\lagTime) }
		{ true }
	},
	noWarnings: true
);

// a time typed with sndset is for that move only: the next plain sndset
// moves over the send's own time, and no time is kept
~radTest.("sndset-time-is-one-move",
	setup: ["start 2 1", ["snd t 1 1 1 -6", 2]],
	cmds: ["sndset t 1 1 -12 3", "sndset t 1 1 -9"],
	check: {
		var send = (RadUnit.readSends(\track1) ? ())[\bus1] ? ();
		case
		{ ~radMrSend.(\vol1) != -9 } { "level " ++ ~radMrSend.(\vol1) }
		{ ~radMrSend.(\ramp1) != 0.05 } { "the next sndset moved over " ++ ~radMrSend.(\ramp1) }
		{ send[\lag].notNil } { "the time stuck: " ++ send.cs }
		{ true }
	}
);

// sndlag is the send's own time for typed values
~radTest.("sndlag-is-the-typed-time",
	setup: ["start 2 1", ["snd t 1 1 1 -6", 2]],
	cmds: ["sndlag t 1 1 2", "sndset t 1 1 -12", "sndlag t 2 1 5", "sndlag t 1 3 5"],
	check: {
		var send = (RadUnit.readSends(\track1) ? ())[\bus1] ? ();
		case
		{ ~radMrSend.(\lag1) != 2 or: { ~radMrSend.(\ramp1) != 2 } } { "lag1 " ++ ~radMrSend.(\lag1) ++ ", ramp1 " ++ ~radMrSend.(\ramp1) }
		{ send[\lag] != 2 } { "document: " ++ send.cs }
		{ true }
	}
);

// the sound: a send from 0 dB to silence over 2 s is a steady fader move,
// as the volume is (a quarter of the amplitude half way)
~radTest.("send-is-a-steady-fader-move",
	setup: ["start 2 1", "blk 1 sine", ["snd t 1 1 1 0", 2]],
	code: {
		1.wait;
		~radMrFull = ~radMrLevel.(\bus1);
		Radicles.aZ.setSendKnob(\track, 1, 1, -inf, 2);
		0.9.wait;
		~radMrHalf = ~radMrLevel.(\bus1);
		1.2.wait;
		~radMrEnd = ~radMrLevel.(\bus1) },
	check: {
		var half = ~radMrHalf / ~radMrFull.max(0.000001);
		case
		{ ~radMrFull < 0.001 } { "no sound to measure: " ++ ~radMrFull }
		{ half < 0.15 or: { half > 0.4 } } { "half way the level is " ++ half.round(0.01) ++ " of full (a steady fader move: 0.25)" }
		{ ~radMrEnd > (~radMrFull * 0.01) } { "not silent at the end: " ++ (~radMrEnd / ~radMrFull).round(0.001) }
		{ true }
	},
	noWarnings: true
);

// mapped controls on a trim and on a send move over the native time,
// whatever the typed times are
~radTest.("mapped-trim-and-send-use-ctllag",
	setup: ["start 2 1", ["snd t 1 1 1 -6", 2], "trimlag t 1 2", "trim t 1 3", "sndlag t 1 1 2", "sndset t 1 1 -12"],
	code: {
		HIDMap.map(Ndef('inTrack1'), \trim, \osc, [-20, 20], ['/radtestmrtrim']);
		HIDMap.map(Ndef('busIn1'), \vol1, \osc, [-60, 0], ['/radtestmrsnd']);
		0.2.wait;
		NetAddr.localAddr.sendMsg('/radtestmrtrim', 0.75);
		NetAddr.localAddr.sendMsg('/radtestmrsnd', 0.5); 0.5.wait },
	check: {
		case
		{ ~radMrIn.(\trim) != 10 } { "trim " ++ ~radMrIn.(\trim) }
		{ ~radMrIn.(\ramp) != 0.05 } { "the trim moved over " ++ ~radMrIn.(\ramp) }
		{ ~radMrSend.(\vol1) != -30 } { "send " ++ ~radMrSend.(\vol1) }
		{ ~radMrSend.(\ramp1) != 0.05 } { "the send moved over " ++ ~radMrSend.(\ramp1) }
		{ ~radMrIn.(\lagTime) != 2 or: { ~radMrSend.(\lag1) != 2 } } { "a typed time changed: " ++ [~radMrIn.(\lagTime), ~radMrSend.(\lag1)].cs }
		{ true }
	},
	teardown: { OSCdef.freeAll }
);

// a modulator on a trim or on a send takes that ramp off too
~radTest.("modulator-takes-trim-and-send-ramps-off",
	setup: ["start 2 1", ["snd t 1 1 1 -6", 2]],
	cmds: [["trim t 1 sin", 2], ["sndset t 1 1 sin", 2]],
	check: { (~radMrIn.(\ramp) == 0 and: { ~radMrSend.(\ramp1) == 0 }) or: { "ramps " ++ [~radMrIn.(\ramp), ~radMrSend.(\ramp1)].cs } }
);

// when a sender leaves a bus, the senders after it move up a place: their
// own times and the time of their last move go with them
~radTest.("send-renumbered-keeps-its-time",
	setup: ["start 3 1", ["snd t 1 1 1 -6", 2], ["snd t 2 1 1 -12", 2], "sndlag t 2 1 2", "sndset t 2 1 -15"],
	cmds: [["snd t 1 1 0", 3]],
	check: {
		case
		{ ~radMrSend.(\vol1) != -15 } { "track 2's level is " ++ ~radMrSend.(\vol1) }
		{ ~radMrSend.(\lag1) != 2 or: { ~radMrSend.(\ramp1) != 2 } } { "lag1 " ++ ~radMrSend.(\lag1) ++ ", ramp1 " ++ ~radMrSend.(\ramp1) }
		{ ~radMrSend.(\lag2).notNil or: { ~radMrSend.(\ramp2).notNil } } { "the place left over kept " ++ [~radMrSend.(\lag2), ~radMrSend.(\ramp2)].cs }
		{ true }
	}
);

// snd on a send that is already there is a typed move too: over the send's
// own time, not the time of its last move
~radTest.("snd-again-uses-the-sends-time",
	setup: ["start 2 1", ["snd t 1 1 1 -6", 2], ["sndset t 1 1 -12 3", 0.5]],
	cmds: [["snd t 1 1 1 -9", 2]],
	check: {
		case
		{ ~radMrSend.(\vol1) != -9 } { "level " ++ ~radMrSend.(\vol1) }
		{ ~radMrSend.(\ramp1) != 0.05 } { "snd moved over " ++ ~radMrSend.(\ramp1) }
		{ true }
	}
);

// when the last sender leaves a bus its level and times go with it: the next
// sender starts from the defaults
~radTest.("last-sender-leaves-nothing-behind",
	setup: ["start 2 1", ["snd t 1 1 1 -6", 2], "sndlag t 1 1 2", "sndset t 1 1 -12"],
	cmds: [["snd t 1 1 0", 3], ["snd t 2 1 1 -20", 2], "sndset t 2 1 -15"],
	check: {
		var send = (RadUnit.readSends(\track2) ? ())[\bus1] ? ();
		case
		{ ~radMrSend.(\vol1) != -15 } { "track 2's level is " ++ ~radMrSend.(\vol1) }
		{ ~radMrSend.(\lag1).notNil and: { ~radMrSend.(\lag1) != 0.05 } } { "track 2 took over track 1's sndlag: " ++ ~radMrSend.(\lag1) }
		{ ~radMrSend.(\ramp1).notNil and: { ~radMrSend.(\ramp1) != 0.05 } } { "track 2 moved over " ++ ~radMrSend.(\ramp1) }
		{ send[\lag].notNil } { "document: " ++ send.cs }
		{ true }
	}
);

// ---- pan (part 3): every control of a panner moves evenly in position ----

~radMrPan = {|key, ndef = \spaceTrack1| Ndef(ndef).nodeMap.at(key) };

// a typed pan posts what it always did, and the document has no pan times
// and none of the panner's time controls
~radTest.("pan-posts-as-before",
	setup: ["start 2"],
	cmds: ["pan t 1 -0.5", "pan t 1 0.3"],
	check: {
		var one = RadUnit.readStrip(\track1);
		case
		{ one[\pan] != (pan: 0.3) } { "pan in the document: " ++ one[\pan].cs }
		{ one[\lags].notNil } { "lags in the document: " ++ one[\lags].cs }
		{ true }
	}
);

// panlag is the time for typed values of the panner's controls
~radTest.("panlag-is-the-typed-time",
	setup: ["start 2"],
	cmds: ["panlag t 1 2", "pan t 1 -0.5"],
	check: {
		var one = RadUnit.readStrip(\track1);
		case
		{ ~radMrPan.(\panLag) != 2 or: { ~radMrPan.(\panRamp) != 2 } } { "panLag " ++ ~radMrPan.(\panLag) ++ ", panRamp " ++ ~radMrPan.(\panRamp) }
		{ one[\lags] != (pan: (pan: 2)) } { "document: " ++ one[\lags].cs }
		{ ~radMrPan.(\panLag, \spaceTrack2).notNil } { "track 2 got a time too: " ++ ~radMrPan.(\panLag, \spaceTrack2) }
		{ true }
	}
);

// a time typed after the value is for that move only (pan t 1 2 0.5 used to
// mean the panner's second control)
~radTest.("pan-time-is-one-move",
	setup: ["start 2"],
	cmds: ["pan t 1 -0.5 3", "pan t 1 0.5", "pan 2 0.25 3"],
	check: {
		var one = RadUnit.readStrip(\track1);
		case
		{ ~radMrPan.(\pan) != 0.5 } { "pan " ++ ~radMrPan.(\pan) }
		{ ~radMrPan.(\panRamp) != 0.05 } { "the next pan moved over " ++ ~radMrPan.(\panRamp) }
		{ one[\lags].notNil } { "the time stuck: " ++ one[\lags].cs }
		{ ~radMrPan.(\pan, \spaceTrack2) != 0.25 or: { ~radMrPan.(\panRamp, \spaceTrack2) != 3 } } {
			"track 2 by its number in the mixer: " ++ [~radMrPan.(\pan, \spaceTrack2), ~radMrPan.(\panRamp, \spaceTrack2)].cs }
		{ true }
	}
);

// the sound: from hard left to hard right over 2 s, half way the sound is
// in the middle (a lag would be nearly at the right by then)
~radTest.("pan-is-even-in-position",
	setup: ["start 2", "blk 1 sine", ["pan t 1 -1", 1]],
	code: {
		Radicles.aZ.setPan(\track, 1, 1, 1, 2);
		0.75.wait;
		~radMrLeft = ~radMrLevel.(\spaceTrack1, 0);
		Radicles.aZ.setPan(\track, 1, -1, 1, 0); 0.3.wait;
		Radicles.aZ.setPan(\track, 1, 1, 1, 2);
		0.75.wait;
		~radMrRight = ~radMrLevel.(\spaceTrack1, 1);
		1.5.wait;
		~radMrEnd = ~radMrLevel.(\spaceTrack1, 0) },
	check: {
		var ratio = ~radMrLeft / ~radMrRight.max(0.000001);
		case
		{ ~radMrRight < 0.001 } { "no sound to measure: " ++ ~radMrRight }
		{ ratio < 0.6 or: { ratio > 1.6 } } { "half way, left is " ++ ratio.round(0.01) ++ " of right (in the middle: 1)" }
		{ ~radMrEnd > (~radMrRight * 0.05) } { "not hard right at the end: left is " ++ (~radMrEnd / ~radMrRight).round(0.001) ++ " of the middle's right" }
		{ true }
	},
	noWarnings: true
);

// a four-channel system: the panner's controls by their names, each with its
// own time for one move; panlag is the time of them all
~radTest.("pan-controls-by-name",
	setup: ["start 1 1 4"],
	cmds: ["panx t 1 -0.5", "pany t 1 0.5 3", "panlag t 1 2", "panx 1 0.25", "pany t 1 -0.25"],
	check: {
		var one = RadUnit.readStrip(\track1);
		case
		{ one[\pan] != (panx: 0.25, pany: -0.25) } { "pan in the document: " ++ one[\pan].cs }
		{ ~radMrPan.(\panxRamp) != 2 or: { ~radMrPan.(\panyRamp) != 2 } } { "moved over " ++ [~radMrPan.(\panxRamp), ~radMrPan.(\panyRamp)].cs }
		{ one[\lags] != (pan: (panx: 2, pany: 2)) } { "document: " ++ one[\lags].cs }
		{ true }
	}
);

// a name the strip's panner does not have says so and changes nothing
~radTest.("pan-control-not-there",
	setup: ["start 2"],
	cmds: ["width t 1 3", "panx t 1 0.5"],
	check: { RadUnit.readStrip(\track1)[\pan] == (pan: 0) or: { "pan: " ++ RadUnit.readStrip(\track1)[\pan].cs } }
);

// a mapped control on a pan moves over the native time; a modulator takes
// the ramp off
~radTest.("mapped-and-modulated-pan",
	setup: ["start 2", "panlag t 1 2", "pan t 1 -0.5", "panlag t 2 2", "pan t 2 -0.5"],
	code: {
		HIDMap.map(Ndef('spaceTrack1'), \pan, \osc, [-1, 1], ['/radtestmrpan']);
		0.2.wait;
		NetAddr.localAddr.sendMsg('/radtestmrpan', 0.75); 0.5.wait;
		Radicles.cW.callFunc("pan t 2 sin"); 2.wait },
	check: {
		case
		{ ~radMrPan.(\pan) != 0.5 } { "pan " ++ ~radMrPan.(\pan) }
		{ ~radMrPan.(\panRamp) != 0.05 } { "the pan moved over " ++ ~radMrPan.(\panRamp) }
		{ ~radMrPan.(\panLag) != 2 } { "the typed time changed: " ++ ~radMrPan.(\panLag) }
		{ ~radMrPan.(\panRamp, \spaceTrack2) != 0 } { "the modulated pan's ramp is " ++ ~radMrPan.(\panRamp, \spaceTrack2) }
		{ true }
	},
	teardown: { OSCdef.freeAll }
);

// a value outside the control's range is not set, and says so (pan t 1 2 0.5
// used to mean the panner's second control)
~radTest.("pan-outside-its-range",
	setup: ["start 2", "pan t 1 0.3"],
	cmds: ["pan t 1 2 0.5"],
	check: { ~radMrPan.(\pan) == 0.3 or: { "pan " ++ ~radMrPan.(\pan) } }
);

// a pan control by its number is one of the panner's own controls, never one
// of its times
~radTest.("pan-number-is-a-pan-control",
	setup: ["start 2"],
	cmds: [["pan t 1 2 sin", 2], "panlag t 1 2 4"],
	check: {
		(~radMrPan.(\panLag).isNil and: { ~radMrPan.(\panRamp).isNil }) or: {
			"times: " ++ [~radMrPan.(\panLag), ~radMrPan.(\panRamp)].cs } }
);

// ---- part 4: a time typed after vol and trim, and mixlag ----

// a time typed after the value is for that one move: the strip's own time
// (vollag, trimlag) is not changed, and the next plain value uses it again
~radTest.("vol-and-trim-time-is-one-move",
	setup: ["start 2"],
	cmds: ["vol t 1 -12 3", "vol t 1 0", "vol 2 -6 2", "vol t 1-2 -20 1", "vol [1,2] -10 4",
		"trim t 1 -3 2", "trim t 1 0", "trim 2 3 1.5"],
	check: {
		case
		{ ~radMrCtl.(\volume) != -10 or: { ~radMrCtl.(\ramp) != 4 } } { "track 1: " ++ [~radMrCtl.(\volume), ~radMrCtl.(\ramp)].cs }
		{ ~radMrCtl.(\lagTime) != 0.05 } { "the volume's own time changed: " ++ ~radMrCtl.(\lagTime) }
		{ Ndef(\track2).nodeMap.at(\volume) != -10 } { "track 2: " ++ Ndef(\track2).nodeMap.at(\volume) }
		{ ~radMrIn.(\trim) != 0 or: { ~radMrIn.(\ramp) != 0.05 } } { "trim: " ++ [~radMrIn.(\trim), ~radMrIn.(\ramp)].cs }
		{ ~radMrIn.(\lagTime) != 0.05 } { "the trim's own time changed: " ++ ~radMrIn.(\lagTime) }
		{ Ndef(\inTrack2).nodeMap.at(\ramp) != 1.5 } { "track 2's trim moved over " ++ Ndef(\inTrack2).nodeMap.at(\ramp) }
		{ true }
	}
);

// the master has no number: the number after the value is its time
~radTest.("master-value-and-time",
	setup: ["start 2"],
	cmds: ["vol m -12 4", "trim m -3 2", "pan m 0.5 3", "vol m 0"],
	check: {
		case
		{ Ndef(\master).nodeMap.at(\volume) != 0 or: { Ndef(\master).nodeMap.at(\ramp) != 0.05 } } {
			"master: " ++ [Ndef(\master).nodeMap.at(\volume), Ndef(\master).nodeMap.at(\ramp)].cs }
		{ Ndef(\inMaster).nodeMap.at(\trim) != -3 or: { Ndef(\inMaster).nodeMap.at(\ramp) != 2 } } {
			"trim: " ++ [Ndef(\inMaster).nodeMap.at(\trim), Ndef(\inMaster).nodeMap.at(\ramp)].cs }
		{ ~radMrPan.(\pan, \spaceMaster) != 0.5 or: { ~radMrPan.(\panRamp, \spaceMaster) != 3 } } {
			"pan: " ++ [~radMrPan.(\pan, \spaceMaster), ~radMrPan.(\panRamp, \spaceMaster)].cs }
		{ true }
	}
);

// mixlag N is the time of typed values for every control of every strip and
// every send; a lag typed afterwards changes one again, and a later mixlag
// overwrites it (the rule of fade N). mixlag alone posts it.
~radTest.("mixlag-sets-every-typed-time",
	setup: ["start 2 1", ["snd t 1 1 1 -6", 2]],
	cmds: ["mixlag", "mixlag 2", "mixlag", "vol t 1 -12", "vollag t 1 0.5", "vol t 1 0"],
	code: {
		~radMrOne = [~radMrCtl.(\lagTime), ~radMrCtl.(\ramp), ~radMrIn.(\lagTime), ~radMrPan.(\panLag), ~radMrSend.(\lag1),
			Ndef(\master).nodeMap.at(\lagTime), Ndef(\inBus1).nodeMap.at(\lagTime), Ndef(\spaceMaster).nodeMap.at(\panLag)];
		Radicles.setMixLag(3);
		Radicles.aZ.setVolume(\track, 1, -6);
		0.2.wait },
	check: {
		case
		{ ~radMrOne != [0.5, 0.5, 2, 2, 2, 2, 2, 2] } { "after mixlag 2 and vollag 0.5: " ++ ~radMrOne.cs }
		{ Radicles.mixLag != 3 } { "mixLag " ++ Radicles.mixLag }
		{ ~radMrCtl.(\lagTime) != 3 or: { ~radMrCtl.(\ramp) != 3 } } { "a later mixlag did not overwrite: " ++ [~radMrCtl.(\lagTime), ~radMrCtl.(\ramp)].cs }
		{ ~radMrSend.(\lag1) != 3 } { "the send's time: " ++ ~radMrSend.(\lag1) }
		{ true }
	},
	teardown: { Radicles.mixLag = 0.05 }
);

// what is made after mixlag N starts with N: a new mixer, a track added to
// it, and a new send
~radTest.("mixlag-is-for-what-is-made-later",
	setup: ["mixlag 2", "start 2 1"],
	cmds: [["t add 1", 3], ["snd t 3 1 1 -6", 2], "vol t 3 -12"],
	check: {
		var times = [~radMrCtl.(\lagTime), ~radMrIn.(\lagTime), ~radMrPan.(\panLag), ~radMrPan.(\panLag, \spaceMaster),
			Ndef(\track3).nodeMap.at(\lagTime), Ndef(\inTrack3).nodeMap.at(\lagTime), ~radMrPan.(\panLag, \spaceTrack3),
			~radMrSend.(\lag1), Ndef(\track3).nodeMap.at(\ramp)];
		var doc = RadUnit.readStrip(\track3);
		case
		{ times != (2 ! 9) } { "times: " ++ times.cs }
		{ (doc[\lags] ? ())[\volume] != 2 } { "document: " ++ doc[\lags].cs }
		{ true }
	},
	teardown: { Radicles.mixLag = 0.05 }
);
