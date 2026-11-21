// Mixer ramps (Nov 2026, docs/design/mixer-ramps.md): a strip's volume moves
// as a steady fader move. Typed values move over the strip's own time
// (vollag; a short smooth move by default); the mixer window's fader and
// mapped controls (MIDI, HID, OSC) over the short native time (ctllag). The
// time of a move is sent with the value only when the strip's last move had
// another, so the usual line is still set('volume', x).

~radMrCtl = {|key| Radicles.aZ.stripControl(Ndef(\track1), key) };
// the level of track 1's output, read from the server
~radMrLevel = {
	var bus = Bus.control(Server.default, 1), value, cond = Condition.new, synth;
	//the RMS over the last 0.1 s (a follower ripples with the sine)
	synth = { Out.kr(bus, A2K.kr((RunningSum.ar(InFeedback.ar(Ndef(\track1).bus.index, 1).squared, 4800) / 4800).sqrt)) }.play;
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
