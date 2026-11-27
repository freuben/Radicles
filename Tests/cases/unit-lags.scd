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

// a parameter with real steps (a switch: frz1's freeze, 0 or 1) cannot
// travel: a warning, nothing set
~radTest.("cannot-travel-is-said",
	setup: ["start 2", "fx t 1 frz1"],
	cmds: ["fxlag t 1 1 freeze 2", "fxlag t 1 1 on", "fxset t 1 1 freeze 1 3"],
	check: {
		var keys = Ndef(\filterTrack_1_1).controlKeys;
		case
		{ keys.includes(\freezeLag) } { "freeze has a time control" }
		{ keys.includes(\adjVolLag).not } { "adjVol has no time control: " ++ keys.cs }
		{ ~radUlLag.(\adjVol) != 0.5 } { "adjVol is not on: " ++ ~radUlLag.(\adjVol) }
		{ ~radUlCtl.(\freeze) != 1 } { "freeze " ++ ~radUlCtl.(\freeze) }
		{ true }
	}
);

// A spec with a function (23 Nov 2026): the parameter travels along the
// spec's range and the function is applied on the way. Rounding that leaves
// 30 steps or more is for the display and is left out of the move; fewer are
// real steps, and the value jumps. The function must go steadily one way.
~radTest.("function-spec-which-can-travel",
	check: {
		var path = {|range, func| Assemblage.specPath(range, func) }, pch;
		pch = path.([-12, 12], {|val| val.round(0.4).midiratio });
		case
		{ pch.isNil } { "pch's pitchRatio (60 steps) cannot travel" }
		{ pch[0].size != 61 } { "pch's path has " ++ pch[0].size ++ " points, not one for each step" }
		{ (pch[1][30] - 1).abs > 1e-6 or: { (pch[0][30] - 0.5).abs > 1e-3 } } { "the middle of pch's path: " ++ [pch[0][30], pch[1][30]].cs }
		{ path.([0, 1], {|val| val.round(1) }).notNil } { "a switch can travel" }
		{ path.([1, 16], {|val| val.round(1) }).notNil } { "a bus number (15 steps) can travel" }
		{ path.([0, 6], {|val| val.round(1) }).notNil } { "a choice of 7 can travel" }
		{ path.([10, 100], {|val| val.round(1) }).isNil } { "ratio 10 to 100 by 1 (90 steps) cannot travel" }
		{ path.([2000, 20000, \exp], {|val| val.round(10) }).isNil } { "fMax by 10 cannot travel" }
		{ path.([0.01, 2.01, \exp], {|val| (val-0.01) }).isNil } { "an offset on an exponential range cannot travel" }
		{ path.([1, 0], {|val| val * 2 }).isNil } { "a function that goes down cannot travel" }
		{ path.([-1, 1], {|val| val.squared }).notNil } { "a function that goes down and up can travel" }
		{ path.([0, 1], {|val| if(val > 0.5, { \a }, { \b }) }).notNil } { "a function that gives no number can travel" }
		{ path.([0, 1], {|val| val + (0.004 * sin(val * 300)) }).notNil } { "a function that wobbles on its way up can travel" }
		{ true }
	}
);

// pch's pitch on the running server: half way through a move of an octave
// it is half an octave up (even in value would be 1.5), it arrives, and with
// no time a value between the display's steps is passed exactly as it is
~radTest.("function-spec-moves-along-the-range",
	setup: ["start 2"],
	code: {
		var bus = Bus.control(Server.default, 1), synth, cond = Condition.new;
		synth = ("{arg pitchRatio = 1, pitchRatioRamp = 0; Out.kr(" ++ bus.index ++ ", "
			++ Assemblage.rampCode(\pitchRatio, [-12, 12], {|val| val.round(0.4).midiratio }) ++ ") }").interpret.play;
		Server.default.sync; 0.3.wait;
		Server.default.makeBundle(nil, { synth.set(\pitchRatio, 2, \pitchRatioRamp, 2) });
		1.wait;
		bus.get{|v| ~radUlHalf = v; cond.unhang }; cond.hang;
		1.3.wait; cond = Condition.new;
		bus.get{|v| ~radUlEnd = v; cond.unhang }; cond.hang;
		Server.default.makeBundle(nil, { synth.set(\pitchRatio, 1.2345, \pitchRatioRamp, 0) });
		0.3.wait; cond = Condition.new;
		bus.get{|v| ~radUlJump = v; cond.unhang }; cond.hang;
		//a value set with the time left from the last move: it travels, and
		//ends exactly on the value (not on the nearest point of the path)
		Server.default.makeBundle(nil, { synth.set(\pitchRatio, 0.7071, \pitchRatioRamp, 0.5) });
		1.wait; cond = Condition.new;
		bus.get{|v| ~radUlRest = v; cond.unhang }; cond.hang;
		synth.free; bus.free },
	check: {
		case
		{ (~radUlHalf - 1.414).abs > 0.07 } { "half way: " ++ ~radUlHalf.round(0.001) ++ " (half an octave: 1.414)" }
		{ ~radUlEnd != 2 } { "at the end: " ++ ~radUlEnd }
		{ (~radUlJump - 1.2345).abs > 1e-6 } { "with no time the value is not passed as it is: " ++ ~radUlJump }
		{ (~radUlRest - 0.7071).abs > 1e-6 } { "after a move the value is not exact: " ++ ~radUlRest }
		{ true }
	}
);

// in Radicles: pch's parameters are made with their ramps, switch on, and a
// typed pitch carries its time
~radTest.("function-spec-in-an-effect",
	setup: ["start 2", "fx t 1 pch"],
	cmds: ["fxlag t 1 1 on", "fxset t 1 1 pitchRatio 2", "fxlag t 1 1 off", "fxset t 1 1 pitchRatio 0.5 3", "fxgetlag t 1 1"],
	check: {
		case
		{ ~radUlLag.(\pitchRatio) != 0 } { "pitchRatio's time after off: " ++ ~radUlLag.(\pitchRatio).cs }
		{ ~radUlCtl.(\pitchRatio) != 0.5 or: { ~radUlCtl.(\pitchRatioRamp) != 3 } } {
			"pitchRatio: " ++ [~radUlCtl.(\pitchRatio), ~radUlCtl.(\pitchRatioRamp)].cs }
		{ true }
	},
	noWarnings: true
);

// A function takes 255 arguments at most. A unit whose parameters and time
// controls together would be more (the RAVE library's afterImprov: 157
// parameters, 96 that can travel) has its time controls made by name inside
// the function. It is made without an error, the rest of Radicles finds its
// times as any unit's, and a move is a move.
~radTest.("a-unit-with-too-many-parameters",
	setup: ["start 2"],
	code: {
		var names = 100.collect{|index| ("p" ++ index).asSymbol }, func, cond = Condition.new;
		func = ("{arg " ++ names.collect{|name| name ++ " = 0" }.join(", ") ++ "; p0 + p99 }").interpret;
		~radUlBig = Assemblage.rampUnit(func, names.collect{|name| [name, [0, 1]] });
		Ndef(\radUlBig, ~radUlBig);
		Server.default.sync; 0.3.wait;
		~radUlBigParams = Assemblage.lagParams(Ndef(\radUlBig));
		~radUlBigKeys = Assemblage.paramKeys(Ndef(\radUlBig));
		Ndef(\radUlBig).set(\p0Lag, 2);
		~radUlBigLag = Assemblage.unitLag(Ndef(\radUlBig), \p0);
		Server.default.makeBundle(nil, { Ndef(\radUlBig).set(\p0, 1, \p0Ramp, 2) });
		1.wait;
		Ndef(\radUlBig).bus.get{|v| ~radUlBigHalf = v; cond.unhang }; cond.hang;
		1.5.wait; cond = Condition.new;
		Ndef(\radUlBig).bus.get{|v| ~radUlBigEnd = v; cond.unhang }; cond.hang;
		Ndef(\radUlBig).clear },
	check: {
		case
		{ ~radUlBig.def.argNames.size != 100 } { "the function has " ++ ~radUlBig.def.argNames.size ++ " arguments, not its own 100" }
		{ ~radUlBigParams.size != 100 } { ~radUlBigParams.size.asString ++ " parameters can travel, not 100" }
		{ ~radUlBigKeys.size != 100 } { "its parameters: " ++ ~radUlBigKeys.size ++ ", not 100 (the time controls are among them)" }
		{ ~radUlBigLag != 2 } { "p0's time: " ++ ~radUlBigLag.cs }
		{ (~radUlBigHalf - 0.5).abs > 0.2 } { "half way: " ++ ~radUlBigHalf }
		{ ~radUlBigEnd != 1 } { "at the end: " ++ ~radUlBigEnd }
		{ true }
	},
	noWarnings: true
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

// the same block played again keeps its times; another block on the number
// starts off, after a stop too
~radTest.("another-block-on-the-number-starts-off",
	setup: ["start 2", "blk 1 sine", "blklag 1 on"],
	cmds: [["st 1", 2], ["blk 1 sine", 2]],
	code: {
		~radUlBlkSame = ~radUlBlkLag.(\freq);
		Radicles.cW.callFunc("st 1"); 2.wait;
		Radicles.cW.callFunc("blk 1 blip"); 2.wait },
	check: {
		case
		{ ~radUlBlkSame != 0.5 } { "the same block again: " ++ ~radUlBlkSame }
		{ (~radUlBlkLag.(\freq) ? 0) != 0 } { "blip's freq is on: " ++ ~radUlBlkLag.(\freq) }
		{ true }
	}
);

// a block preset keeps which parameters are on, also when nothing else differs
~radTest.("blkload-brings-lags",
	setup: ["start 2", "blk 1 sine", "blklag 1 freq 3", ["blksave 1 radtest_ul_blk", 1], "blklag 1 off"],
	cmds: [["blkload 1 radtest_ul_blk none", 1.5]],
	check: {
		case
		{ ~radUlBlkLag.(\freq) != 3 } { "freq's time after the load: " ++ ~radUlBlkLag.(\freq) }
		{ ~radUlBlkLag.(\mul) != 0 } { "mul is on: " ++ ~radUlBlkLag.(\mul) }
		{ true }
	},
	teardown: { File.delete(RadUnit.presetPath(\block, \sine, \radtest_ul_blk)) }
);

// Scopes (decision 6). lag N is the whole session: the mixer's time, and
// the effect and block parameters that are on; a parameter that is off stays
// off. It becomes the three times.
~radUlMix = {|tag| Ndef(tag).nodeMap.at(\lagTime) };
~radTest.("lag-n-is-the-whole-session",
	setup: ["start 2 1", ["blk 1 sine", 2], "fx t 1 lpf", "fxlag t 1 1 freq 2", "blklag 1 freq 2"],
	cmds: ["lag", "lag 3", "lag"],
	check: {
		case
		{ [Radicles.mixLag, Radicles.fxLag, Radicles.blkLag] != [3, 3, 3] } {
			"the three times: " ++ [Radicles.mixLag, Radicles.fxLag, Radicles.blkLag].cs }
		{ ~radUlMix.(\track1) != 3 } { "track 1's volume time: " ++ ~radUlMix.(\track1) }
		{ ~radUlMix.(\master) != 3 } { "the master's volume time: " ++ ~radUlMix.(\master) }
		{ ~radUlLag.(\freq) != 3 } { "the effect's freq: " ++ ~radUlLag.(\freq) }
		{ ~radUlLag.(\lag) != 0 } { "the effect's lag is on: " ++ ~radUlLag.(\lag) }
		{ ~radUlBlkLag.(\freq) != 3 } { "the block's freq: " ++ ~radUlBlkLag.(\freq) }
		{ ~radUlBlkLag.(\mul) != 0 } { "the block's mul is on: " ++ ~radUlBlkLag.(\mul) }
		{ true }
	},
	teardown: { Radicles.mixLag = 0.05; Radicles.fxLag = 0.5; Radicles.blkLag = 0.5 },
	noWarnings: true
);

// tlag is one strip: its volume, trim, pan and sends, the parameters that
// are on in its effects and in the block that feeds it. The other track, its
// effect and its block keep their times.
~radTest.("tlag-is-one-strip",
	setup: ["start 2 1", ["blk 1 sine", 2], ["blk 2 sine", 2], "fx t 1 lpf", "fx t 2 lpf",
		"fxlag t 1 1 on", "fxlag t 2 1 on", "fxlag t 1 1 exc [lag]", "blklag 1 on", "blklag 2 on",
		["snd t 1 1 1 -6", 2]],
	cmds: ["tlag t 1 4", "tlag m 2", "tlag 3 1", "tlag t 9 1"],
	check: {
		var send = (RadUnit.readSends(\track1) ? ())[\bus1] ? ();
		case
		{ ~radUlMix.(\track1) != 4 } { "track 1's volume time: " ++ ~radUlMix.(\track1) }
		{ ~radUlMix.(\inTrack1) != 4 } { "track 1's trim time: " ++ ~radUlMix.(\inTrack1) }
		{ Ndef(\spaceTrack1).nodeMap.at(\panLag) != 4 } { "track 1's pan time: " ++ Ndef(\spaceTrack1).nodeMap.at(\panLag) }
		{ send[\lag] != 4 } { "track 1's send time: " ++ send[\lag] }
		{ ~radUlLag.(\freq) != 4 } { "its effect's freq: " ++ ~radUlLag.(\freq) }
		{ ~radUlLag.(\lag) != 0 } { "its effect's lag (excluded) is on: " ++ ~radUlLag.(\lag) }
		{ ~radUlBlkLag.(\freq) != 4 } { "its block's freq: " ++ ~radUlBlkLag.(\freq) }
		{ (~radUlMix.(\track2) ? 0.05) != 0.05 } { "track 2's volume time: " ++ ~radUlMix.(\track2) }
		{ ~radUlLag.(\freq, \filterTrack_2_1) != 0.5 } { "track 2's effect: " ++ ~radUlLag.(\freq, \filterTrack_2_1) }
		{ Assemblage.unitLag(Ndef(\block2), \freq) != 0.5 } { "block 2's freq: " ++ Assemblage.unitLag(Ndef(\block2), \freq) }
		{ ~radUlMix.(\master) != 2 } { "the master's volume time: " ++ ~radUlMix.(\master) }
		{ ~radUlMix.(\bus1) != 1 } { "bus 1's volume time (tlag 3): " ++ ~radUlMix.(\bus1) }
		{ true }
	}
);


// Six units of Main had parameters with no spec, so nothing of theirs could
// travel and a glide load would jump (23 Nov 2026): pch2, pch3, pch4, harm,
// harm2 and cct1's sinadd now have specs. pch3's bus is a number (it
// jumps), and reads 1 to 8 as the other units' inputs do (a number below 1,
// from a preset saved before, is the first input).
~radTest.("units-given-specs-travel",
	setup: ["start 2", "fx t 1 harm2", "fx t 1 2 pch4"],
	cmds: ["fxlag t 1 1 pitchRatio1 2", "fxset t 1 2 transp 2 3"],
	check: {
		var ramps = {|unit| (Assemblage.rampUnit(SynthFile.read(\filter, unit), SpecFile.read(\filter, unit, false)).def.argNames ? [])
			.count{|name| name.asString.endsWith("Ramp") } };
		var counts = [\pch2, \pch3, \pch4, \harm, \harm2, \cct1].collect{|unit| ramps.(unit) };
		var pch3 = SynthFile.read(\filter, \pch3);
		case
		{ counts != [5, 6, 6, 24, 12, 4] } { "parameters that can travel in pch2, pch3, pch4, harm, harm2, cct1: " ++ counts.cs }
		{ pch3.def.prototypeFrame[pch3.def.argNames.indexOf(\bus)] != 1 } { "pch3's bus does not start at 1" }
		{ pch3.def.sourceCode.contains("(bus.round(1) - 1).max(0)").not } { "pch3 does not read input bus - 1 (the first for a number below 1)" }
		{ Assemblage.unitLag(Ndef(\filterTrack_1_1), \pitchRatio1) != 2 } { "harm2's pitchRatio1 time: " ++ Assemblage.unitLag(Ndef(\filterTrack_1_1), \pitchRatio1).cs }
		{ Ndef(\filterTrack_1_2).nodeMap.at(\transpRamp) != 3 } { "pch4's transp, the time of its move: " ++ Ndef(\filterTrack_1_2).nodeMap.at(\transpRamp).cs }
		{ true }
	},
	noWarnings: true
);
