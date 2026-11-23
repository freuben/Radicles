// Lags on effects (Nov 2026, docs/design/lags-blocks-effects.md): a parameter
// with a plain spec is made with its ramp built in and off. fxlag switches it
// on and gives it a time, with nothing rebuilt; a typed value then travels on
// the spec's scale and arrives on time. A parameter with no spec, a stepped
// spec or a spec with a function is left as it was.

~radUlCtl = {|key, tag = \filterTrack_1_1| Ndef(tag).nodeMap.at(key) };
~radUlLag = {|key, tag = \filterTrack_1_1| Assemblage.unitLag(Ndef(tag), key) };

// off: the effect has its time controls at 0, they are not among its
// parameters, and a typed value posts what it always did
~radTest.("built-in-and-off",
	setup: ["start 2", "fx t 1 lpf"],
	cmds: ["fxset t 1 1 freq 800", "fxset t 1 1 1 600", "fxset t 1 1 [freq, 500]", "fxgetlag t 1 1"],
	check: {
		var keys = Ndef(\filterTrack_1_1).controlKeys;
		case
		{ keys.includes(\freqLag).not or: { keys.includes(\freqRamp).not } } { "no time controls: " ++ keys.cs }
		{ Radicles.aZ.getFilterKeys(\filterTrack_1_1, \args) != [\freq, \lag] } {
			"parameters: " ++ Radicles.aZ.getFilterKeys(\filterTrack_1_1, \args).cs }
		{ ~radUlLag.(\freq) != 0 } { "freq is on: " ++ ~radUlLag.(\freq) }
		{ ~radUlCtl.(\freq) != 500 } { "freq " ++ ~radUlCtl.(\freq) }
		{ (~radUlCtl.(\freqRamp) ? 0) != 0 } { "a move had a time: " ++ ~radUlCtl.(\freqRamp) }
		{ RadUnit.read(\filterTrack_1_1)[\args].keys.asArray.sort != [\freq, \lag] } {
			"the document's values: " ++ RadUnit.read(\filterTrack_1_1)[\args].keys.asArray.sort.cs }
		{ RadUnit.read(\filterTrack_1_1)[\lags].notNil } { "the document has lags: " ++ RadUnit.read(\filterTrack_1_1)[\lags].cs }
		{ true }
	},
	noWarnings: true
);

// on gives every parameter that can travel the effects' lag time; a typed
// value then carries its time; off puts them back
~radTest.("on-and-off",
	setup: ["start 2", "fx t 1 lpf"],
	cmds: ["fxlag t 1 1 on", "fxset t 1 1 freq 800", "fxset t 1 1 freq 400", "fxlag t 1 1 off", "fxset t 1 1 freq 1000"],
	code: {
		~radUlOnDoc = nil;
		Radicles.cW.callFunc("fxlag 1 1 on"); 0.3.wait;
		~radUlOnDoc = RadUnit.read(\filterTrack_1_1)[\lags];
		Radicles.cW.callFunc("fxlag 1 1 off"); 0.3.wait },
	check: {
		case
		{ ~radUlOnDoc != (freq: 0.5, lag: 0.5) } { "the document's lags when on: " ++ ~radUlOnDoc.cs }
		{ ~radUlLag.(\freq) != 0 or: { ~radUlLag.(\lag) != 0 } } { "not off: " ++ [~radUlLag.(\freq), ~radUlLag.(\lag)].cs }
		{ ~radUlCtl.(\freq) != 1000 or: { ~radUlCtl.(\freqRamp) != 0 } } {
			"after off: " ++ [~radUlCtl.(\freq), ~radUlCtl.(\freqRamp)].cs }
		{ true }
	},
	noWarnings: true
);

// a number is for the parameters that are on; one parameter by name or by
// number; exc switches the listed ones off, inc leaves only the listed on
~radTest.("times-exc-and-inc",
	setup: ["start 2", "fx t 1 bpf"],
	cmds: ["fxlag t 1 1 on", "fxlag t 1 1 exc [freq, 2]", "fxlag t 1 1 4", "fxlag t 1 1 mix 1", "fxlag t 1 1 3 2"],
	code: {
		~radUlA = [\freq, \q, \vol, \mix, \lagTime].collect{|k| ~radUlLag.(k) };
		Radicles.cW.callFunc("fxlag t 1 1 inc [freq, vol]"); 0.3.wait;
		~radUlB = [\freq, \q, \vol, \mix, \lagTime].collect{|k| ~radUlLag.(k) };
		Radicles.cW.callFunc("fxlags t 1 1 [3, 0, 1]"); 0.3.wait;
		~radUlC = [\freq, \q, \vol, \mix, \lagTime].collect{|k| ~radUlLag.(k) } },
	check: {
		case
		{ ~radUlA != [0, 0, 2, 1, 4] } { "after exc, 4, mix 1, vol 2: " ++ ~radUlA.cs }
		{ ~radUlB != [0.5, 0, 2, 0, 0] } { "after inc [freq, vol]: " ++ ~radUlB.cs }
		{ ~radUlC != [3, 0, 1, 0, 0] } { "after fxlags [3, 0, 1]: " ++ ~radUlC.cs }
		{ true }
	},
	noWarnings: true
);

// a parameter whose spec has a function cannot travel: a warning, nothing set
~radTest.("cannot-travel-is-said",
	setup: ["start 2", "fx t 1 pch"],
	cmds: ["fxlag t 1 1 pitchRatio 2", "fxlag t 1 1 on", "fxset t 1 1 pitchRatio 2 3"],
	check: {
		var keys = Ndef(\filterTrack_1_1).controlKeys;
		case
		{ keys.includes(\pitchRatioLag) } { "pitchRatio has a time control" }
		{ keys.includes(\winSizeLag).not } { "winSize has no time control: " ++ keys.cs }
		{ ~radUlLag.(\winSize) != 0.5 } { "winSize is not on: " ++ ~radUlLag.(\winSize) }
		{ ~radUlCtl.(\pitchRatio) != 2 } { "pitchRatio " ++ ~radUlCtl.(\pitchRatio) }
		{ true }
	}
);

// fxlag N is for every effect parameter that is on, and for what is
// switched on later
~radTest.("fxlag-n-is-for-what-is-on",
	setup: ["start 2", "fx t 1 lpf", "fx t 1 2 dl1", "fxlag t 1 1 on"],
	cmds: ["fxlag 2", "fxlag t 1 2 on", "fxlag 3", "fxlag t 1 2 off", "fxlag 1"],
	check: {
		case
		{ Radicles.fxLag != 1 } { "Radicles.fxLag " ++ Radicles.fxLag }
		{ ~radUlLag.(\freq) != 1 } { "effect 1's freq " ++ ~radUlLag.(\freq) }
		{ ~radUlLag.(\delayTime, \filterTrack_1_2) != 0 } { "effect 2 is on: " ++ ~radUlLag.(\delayTime, \filterTrack_1_2) }
		{ true }
	},
	noWarnings: true,
	teardown: { Radicles.fxLag = 0.5 }
);

// a time typed after a value is for that one move, on a parameter that is
// off too
~radTest.("a-time-with-a-value-is-one-move",
	setup: ["start 2", "fx t 1 lpf"],
	cmds: ["fxset t 1 1 freq 800 3", "fxset t 1 1 freq 400", "fxset t 1 1 1 900 2", "fxset 1 1 freq 300 1"],
	check: {
		case
		{ ~radUlCtl.(\freq) != 300 or: { ~radUlCtl.(\freqRamp) != 1 } } { "freq: " ++ [~radUlCtl.(\freq), ~radUlCtl.(\freqRamp)].cs }
		{ ~radUlLag.(\freq) != 0 } { "the parameter's own time changed: " ++ ~radUlLag.(\freq) }
		{ true }
	},
	noWarnings: true
);

// a move on an exponential spec is even in pitch: half way through it is at
// the geometric middle (measured on the running server)
~radTest.("exponential-spec-moves-evenly-in-pitch",
	setup: ["start 2"],
	code: {
		var bus = Bus.control(Server.default, 1), synth, cond = Condition.new;
		synth = ("{arg freq = 100, freqRamp = 0; Out.kr(" ++ bus.index ++ ", "
			++ Assemblage.rampCode(\freq, [20, 20000, \exp]) ++ ") }").interpret.play;
		Server.default.sync; 0.3.wait;
		Server.default.makeBundle(nil, { synth.set(\freq, 1600, \freqRamp, 2) });
		1.wait;
		bus.get{|v| ~radUlHalf = v; cond.unhang }; cond.hang;
		1.3.wait; cond = Condition.new;
		bus.get{|v| ~radUlEnd = v; cond.unhang }; cond.hang;
		Server.default.makeBundle(nil, { synth.set(\freq, 200, \freqRamp, 0) });
		0.3.wait; cond = Condition.new;
		bus.get{|v| ~radUlJump = v; cond.unhang }; cond.hang;
		synth.free; bus.free },
	check: {
		case
		//even in pitch: 400 at half way (even in value would be 850)
		{ ~radUlHalf < 250 or: { ~radUlHalf > 600 } } { "half way: " ++ ~radUlHalf.round(1) ++ " (even in pitch: 400)" }
		{ (~radUlEnd - 1600).abs > 1 } { "at the end: " ++ ~radUlEnd }
		{ ~radUlJump != 200 } { "with no time the value is not passed as it is: " ++ ~radUlJump }
		{ true }
	}
);

// a mapped control moves a parameter that is on over the native time, and
// one that is off as before
~radTest.("mapped-control-uses-ctllag-when-on",
	setup: ["start 2", "fx t 1 bpf", "fxlag t 1 1 freq 4"],
	code: {
		HIDMap.map(Ndef(\filterTrack_1_1), \freq, \osc, [100, 1100], ['/radtestulfreq']);
		HIDMap.map(Ndef(\filterTrack_1_1), \mix, \osc, [0, 1], ['/radtestulmix']);
		0.2.wait;
		NetAddr.localAddr.sendMsg('/radtestulfreq', 0.5);
		NetAddr.localAddr.sendMsg('/radtestulmix', 0.25); 0.5.wait },
	check: {
		case
		{ ~radUlCtl.(\freq) != 600 } { "freq " ++ ~radUlCtl.(\freq) }
		{ ~radUlCtl.(\freqRamp) != 0.05 } { "freq moved over " ++ ~radUlCtl.(\freqRamp) }
		{ ~radUlLag.(\freq) != 4 } { "freq's own time changed: " ++ ~radUlLag.(\freq) }
		{ ~radUlCtl.(\mix) != 0.25 } { "mix " ++ ~radUlCtl.(\mix) }
		{ (~radUlCtl.(\mixRamp) ? 0) != 0 } { "mix (off) moved over " ++ ~radUlCtl.(\mixRamp) }
		{ true }
	},
	teardown: { OSCdef.freeAll }
);

// a preset keeps which parameters are on; one saved with none leaves the
// effect's times as they are
~radTest.("preset-keeps-lags",
	setup: ["start 2", "fx t 1 bpf", "fxsave t 1 1 radtest_ul_plain", "fxlag t 1 1 freq 3", "fxset t 1 1 freq 900",
		["fxsave t 1 1 radtest_ul_lag", 1], "fxlag t 1 1 off", "fxlag t 1 1 mix 2"],
	cmds: [["fxload t 1 1 radtest_ul_lag none", 1.5]],
	code: {
		~radUlP1 = [~radUlLag.(\freq), ~radUlLag.(\mix)];
		Radicles.cW.callFunc("fxload t 1 1 radtest_ul_plain none"); 1.5.wait;
		~radUlP2 = [~radUlLag.(\freq), ~radUlLag.(\mix)] },
	check: {
		case
		{ ~radUlP1 != [3, 0] } { "after the preset with a lag: " ++ ~radUlP1.cs }
		{ ~radUlP2 != [3, 0] } { "after the preset without: " ++ ~radUlP2.cs }
		{ true }
	},
	teardown: {
		[\radtest_ul_plain, \radtest_ul_lag].do{|name| File.delete(RadUnit.presetPath(\filter, \bpf, name)) } }
);

// a number names a parameter, never a time control; a modulator on a
// parameter that is on takes its ramp off (it would dull the modulator)
~radTest.("modulator-on-a-parameter-that-is-on",
	setup: ["start 2", "fx t 1 bpf", "fxlag t 1 1 on", "fxset t 1 1 freq 900", "fxset t 1 1 q 5"],
	cmds: ["fxset t 1 1 2 sin", "fxset t 1 1 freq sin"],
	code: {
		~radUlModQ = ~radUlCtl.(\q).isKindOf(NodeProxy);
		Radicles.cW.callFunc("unmodfx t 1 1 2"); 1.wait },
	check: {
		case
		{ ~radUlModQ.not } { "number 2 did not modulate q" }
		{ ~radUlCtl.(\q).isNumber.not } { "q is still modulated: " ++ ~radUlCtl.(\q).cs }
		{ ~radUlCtl.(\freq).isKindOf(NodeProxy).not } { "freq is not modulated: " ++ ~radUlCtl.(\freq).cs }
		{ ~radUlCtl.(\freqRamp) != 0 } { "freq's ramp is still " ++ ~radUlCtl.(\freqRamp) }
		{ ~radUlLag.(\freq) != 0.5 } { "freq's own time changed: " ++ ~radUlLag.(\freq) }
		{ true }
	}
);

// another effect in the slot does not inherit which parameters were on, nor
// the time of the last move; the same effect made again with values keeps
// its times (typed again with none it is a fresh effect: its defaults, off)
~radTest.("another-effect-in-the-slot-starts-off",
	setup: ["start 2", "fx t 1 lpf", "fxlag t 1 1 freq 4", "fxset t 1 1 freq 300"],
	cmds: [["fx t 1 1 lpf [freq, 500]", 2]],
	code: {
		~radUlSame = [~radUlLag.(\freq), ~radUlCtl.(\freqRamp) ? 0];
		Radicles.cW.callFunc("fx t 1 1 bpf"); 2.wait },
	check: {
		case
		{ ~radUlSame != [4, 0] } { "the same effect made again: " ++ ~radUlSame.cs }
		{ ~radUlLag.(\freq) != 0 } { "bpf's freq is on: " ++ ~radUlLag.(\freq) }
		{ (~radUlCtl.(\freqRamp) ? 0) != 0 } { "bpf's freq has a move's time: " ++ ~radUlCtl.(\freqRamp) }
		{ true }
	}
);

// a chain preset brings back which parameters are on, also when nothing
// else differs
~radTest.("csload-brings-lags",
	setup: ["start 2", "fx t 1 bpf", "fxlag t 1 1 freq 3", ["cssave t 1 radtest_ul_chain", 1], "fxlag t 1 1 off"],
	cmds: [["csload t 1 radtest_ul_chain 1", 3]],
	check: {
		case
		{ ~radUlLag.(\freq) != 3 } { "freq's time after the load: " ++ ~radUlLag.(\freq) }
		{ ~radUlLag.(\mix) != 0 } { "mix is on: " ++ ~radUlLag.(\mix) }
		{ true }
	},
	teardown: { File.delete(RadUnit.chainPresetPath('radtest_ul_chain')) }
);

// Blocks, the same way (blklag, blkset, blkgetlag)

~radUlBlk = {|key| Ndef(\block1).nodeMap.at(key) };
~radUlBlkLag = {|key| Assemblage.unitLag(Ndef(\block1), key) };

// off: a block has its time controls at 0, they are not among its
// parameters, and a typed value posts what it always did
~radTest.("block-built-in-and-off",
	setup: ["start 2", "blk 1 sine"],
	cmds: ["blkset 1 freq 300", "blkset 1 1 400", "blkset 1", "blkgetlag 1"],
	check: {
		var keys = Ndef(\block1).controlKeys, unit = RadUnit.read(\block1);
		case
		{ keys.includes(\freqLag).not or: { keys.includes(\freqRamp).not } } { "no time controls: " ++ keys.cs }
		{ ~radUlBlkLag.(\freq) != 0 } { "freq is on: " ++ ~radUlBlkLag.(\freq) }
		{ ~radUlBlk.(\freq) != 400 } { "freq " ++ ~radUlBlk.(\freq) }
		{ unit[\args].keys.any{|key| key.asString.endsWith("Lag") or: { key.asString.endsWith("Ramp") } } } {
			"the document's values: " ++ unit[\args].keys.asArray.sort.cs }
		{ unit[\lags].notNil } { "the document has lags: " ++ unit[\lags].cs }
		{ true }
	},
	noWarnings: true
);

// on, off, exc, a time for what is on, one parameter, a time with a value,
// and blklag N for every block parameter that is on
~radTest.("block-lags-and-moves",
	setup: ["start 2", "blk 1 sine"],
	cmds: ["blklag 1 on", "blkset 1 freq 300", "blklag 1 exc [phase]", "blklag 1 2", "blklag 1 mul 1",
		"blkset 1 freq 500 3", "blkset 1 freq 200", "blklag 3"],
	code: {
		~radUlBlkA = [\freq, \phase, \mul].collect{|k| ~radUlBlkLag.(k) };
		~radUlBlkDoc = RadUnit.read(\block1)[\lags];
		~radUlBlkRamp = ~radUlBlk.(\freqRamp);
		Radicles.cW.callFunc("blklags 1 [1, 2]"); 0.3.wait;
		~radUlBlkB = [\freq, \phase, \mul].collect{|k| ~radUlBlkLag.(k) };
		Radicles.cW.callFunc("blklag 1 off"); 0.3.wait },
	check: {
		case
		{ ~radUlBlkA != [3, 0, 3] } { "after blklag 3: " ++ ~radUlBlkA.cs }
		{ ~radUlBlkDoc != (freq: 3, mul: 3) } { "the document's lags: " ++ ~radUlBlkDoc.cs }
		{ ~radUlBlkRamp != 2 } { "the last move's time: " ++ ~radUlBlkRamp }
		{ ~radUlBlkB != [1, 2, 3] } { "after blklags [1, 2]: " ++ ~radUlBlkB.cs }
		{ [\freq, \phase, \mul].any{|k| ~radUlBlkLag.(k) != 0 } } { "not off" }
		{ Radicles.blkLag != 3 } { "Radicles.blkLag " ++ Radicles.blkLag }
		{ true }
	},
	noWarnings: true,
	teardown: { Radicles.blkLag = 0.5 }
);
