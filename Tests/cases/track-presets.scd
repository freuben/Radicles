// Track presets (Document Model, step 4, PR 2): tsave / tload save and load a
// whole channel strip (its chain, trim, volume, mute, pan, lags, mixer
// modulators, sends by bus, and its source) as one readable file in
// Settings/Presets/units/track, onto any strip, changing only what differs.
// Its output to the master comes back as saved (17 Nov 2026). Never solo or
// record arm.

~radTrackClean = {|names|
	names.do{|name| File.delete(RadUnit.trackPresetPath(name)) };
};
// a strip as the document has it, values refreshed
~radStripNow = {|name| RadUnit.refresh(RadDoc.current.copy).at(name) ? () };
// what a track preset carries of a strip (no structure, no session settings)
~radTrackPart = {|name|
	var strip = ~radStripNow.(name);
	strip.select{|v, k| [\trim, \volume, \mute, \pan, \lags, \sends].includes(k) }
};
~radChainPlainT = {|name|
	(~radStripNow.(name)[\fx] ? ()).collect{|unit| unit.reject{|v, k| [\position, \morphTime].includes(k) } }
};
~radBlockArgs = {|n| (RadUnit.readBlock(("block" ++ n).asSymbol) ? ()) };

~radTest.("track-save-file",
	setup: ["start 2 1", "blk 1 sine", "blkset 1 freq 330", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5",
		"vol t 1 -6", "pan t 1 -0.3", "trim t 1 3", "vollag t 1 2", "snd t 1 1 1 -9", "solo t 1"],
	cmds: ["tsave t 1 radtest_track"],
	check: {
		var data = RadDoc.readData(File.readAllString(RadUnit.trackPresetPath('radtest_track'))),
		track = data !? { data[\track] };
		case
		{ track.isNil } { "no file" }
		{ data[\type] != \track } { "type: " ++ data[\type] }
		{ [track[\from], track[\volume], track[\trim], track[\pan]] != [\track, -6, 3, (pan: -0.3)] } {
			"values: " ++ track.cs }
		{ track[\lags] != (volume: 2) } { "lags: " ++ track[\lags].cs }
		{ track[\sends] != (bus1: (level: -9)) } { "sends: " ++ track[\sends].cs }
		{ track[\chain][\fx][1][\def] != \pch or: { track[\chain][\fx][1][\args][\pitchRatio] != 0.5 } } {
			"chain: " ++ track[\chain].cs }
		{ track[\source].size != 1 or: { track[\source][0][\def] != \sine } or: {
			track[\source][0][\args][\freq] != 330 } } { "source: " ++ track[\source].cs }
		{ track[\output] != \master } { "output: " ++ track[\output].cs }
		{ [\solo, \input, \kind].any{|k| track.includesKey(k) } } { "session settings saved: " ++ track.keys.cs }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_track']) }
);

// onto another track: the same values, sends, chain, and its block plays the source
~radTest.("track-load-other-track",
	setup: ["start 2 1", "blk 1 sine", "blkset 1 freq 330", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5",
		"vol t 1 -6", "pan t 1 -0.3", "trim t 1 3", "vollag t 1 2", "snd t 1 1 1 -9",
		"tsave t 1 radtest_track2"],
	cmds: [["tload t 2 radtest_track2", 10]],
	check: {
		var one = ~radTrackPart.(\track1), two = ~radTrackPart.(\track2), diff = RadDoc.diff(one, two, 1e-9);
		case
		{ diff.notEmpty } { "values differ: " ++ diff.collect(_[\address]).cs ++ " " ++ two.cs }
		{ RadDoc.diff(~radChainPlainT.(\track1), ~radChainPlainT.(\track2), 1e-9).notEmpty } {
			"chain: " ++ ~radChainPlainT.(\track2).cs }
		{ ~radBlockArgs.(2)[\def] != \sine or: { ~radBlockArgs.(2)[\args][\freq] != 330 } } {
			"block 2: " ++ ~radBlockArgs.(2).cs }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_track2']) }
);

// the source plays its own definition, even where the block played another
~radTest.("track-load-other-definition",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 330", "tsave t 1 radtest_track3", "blk 2 noise"],
	cmds: [["tload t 2 radtest_track3", 8]],
	check: {
		var two = ~radBlockArgs.(2);
		(two[\def] == \sine and: { two[\args][\freq] == 330 }) or: { "block 2: " ++ two.cs }
	},
	teardown: { ~radTrackClean.(['radtest_track3']) }
);

// nosrc keeps the track's source; nofx keeps its effects
~radTest.("track-load-nosrc-nofx",
	setup: ["start 3", "blk 1 sine", "fx t 1 pch", "vol t 1 -6", "tsave t 1 radtest_track4",
		"blk 2 noise", "blk 3 noise", "fx t 3 dl1"],
	cmds: [["tload t 2 radtest_track4 nosrc", 8], ["tload t 3 radtest_track4 nofx", 8]],
	check: {
		case
		{ ~radBlockArgs.(2)[\def] != \noise } { "block 2: " ++ ~radBlockArgs.(2).cs }
		{ ~radChainPlainT.(\track2).collect(_[\def]) != (1: \pch) } { "track 2 fx: " ++ ~radChainPlainT.(\track2).cs }
		{ ~radBlockArgs.(3)[\def] != \sine } { "block 3: " ++ ~radBlockArgs.(3).cs }
		{ ~radChainPlainT.(\track3).collect(_[\def]) != (1: \dl1) } { "track 3 fx: " ++ ~radChainPlainT.(\track3).cs }
		{ [~radStripNow.(\track2)[\volume], ~radStripNow.(\track3)[\volume]] != [-6, -6] } { "volumes" }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_track4']) }
);

// default: a clean strip; the source keeps playing
~radTest.("track-load-default",
	setup: ["start 2 1", "blk 1 sine", "fx t 1 pch", "vol t 1 -6", "pan t 1 0.5", "trim t 1 2",
		"vollag t 1 3", "snd t 1 1 1 -9", "mute t 1"],
	cmds: [["tload t 1 default", 8]],
	check: {
		var one = ~radStripNow.(\track1);
		case
		{ [one[\trim], one[\volume], one[\mute], one[\pan]] != [0, 0, 0, (pan: 0)] } { "values: " ++ one.cs }
		{ one[\lags].notNil or: { one[\sends].notNil } or: { one[\mods].notNil } } { "left: " ++ one.cs }
		{ (one[\fx] ? ()).notEmpty } { "fx: " ++ one[\fx].cs }
		{ ~radBlockArgs.(1)[\def] != \sine } { "block 1: " ++ ~radBlockArgs.(1).cs }
		{ true }
	}
);

// a preset saved with no source stops the track's source (16 Nov 2026)
~radTest.("track-load-no-source-stops",
	setup: ["start 2", "vol t 2 -3", "tsave t 2 radtest_track5", "blk 1 sine"],
	cmds: [["tload t 1 radtest_track5", 8]],
	check: {
		(RadUnit.readBlock(\block1).isNil and: { ~radStripNow.(\track1)[\volume] == -3 }) or: {
			"block 1: " ++ RadUnit.readBlock(\block1).cs }
	},
	teardown: { ~radTrackClean.(['radtest_track5']) }
);

// sends the preset doesn't have are removed; the output to the master comes
// back on, as saved (it stayed off before 17 Nov 2026)
~radTest.("track-load-removes-sends",
	setup: ["start 2 2", "tsave t 1 radtest_track6", "snd t 2 1 1 -6", "snd t 2 2 2 -9", "outmenu t 2 0"],
	cmds: [["tload t 2 radtest_track6", 8]],
	check: {
		var two = ~radStripNow.(\track2);
		(two[\sends].isNil and: { two[\output] == \master }) or: { "track2: " ++ two.cs }
	},
	teardown: { ~radTrackClean.(['radtest_track6']) }
);

// a send's lag and modulator come back
~radTest.("track-load-send-lag-and-mod",
	setup: ["start 2 1", "snd t 1 1 1 -6", "sndset t 1 1 -10 2", "vol t 1 sin", "tsave t 1 radtest_track7"],
	cmds: [["tload t 2 radtest_track7", 8]],
	check: {
		var two = ~radStripNow.(\track2);
		case
		{ two[\sends].isNil or: { two[\sends][\bus1][\lag] != 2 } or: { two[\sends][\bus1][\level] != -10 } } {
			"sends: " ++ two[\sends].cs }
		{ two[\mods].isNil or: { two[\mods][\volume][\type] != \sin } } { "mods: " ++ two[\mods].cs }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_track7']) }
);

// a track preset onto a bus and onto the master: what they can't have is left out
~radTest.("track-load-bus-and-master",
	setup: ["start 2 2", "blk 1 sine", "fx t 1 pch", "vol t 1 -6", "snd t 1 1 2 -9", "tsave t 1 radtest_track8"],
	cmds: [["tload b 1 radtest_track8", 8], ["tload m radtest_track8", 8]],
	check: {
		var bus = ~radStripNow.(\bus1), master = ~radStripNow.(\master);
		case
		{ bus[\volume] != -6 or: { bus[\sends] != (bus2: (level: -9)) } } { "bus1: " ++ bus.cs }
		{ (bus[\fx] ? ()).collect(_[\def]) != (1: \pch) } { "bus1 fx: " ++ bus[\fx].cs }
		{ master[\volume] != -6 or: { (master[\fx] ? ()).collect(_[\def]) != (1: \pch) } } { "master: " ++ master.cs }
		{ ~radBlockArgs.(2)[\def].notNil } { "block 2 started: " ++ ~radBlockArgs.(2).cs }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_track8']) }
);

// a send to a bus the session doesn't have, and a pan argument the panner
// doesn't have, are left out and named
~radTest.("track-load-left-out",
	setup: ["start 2 1"],
	code: {
		~radLeft = RadUnit.applyTrack(\track, 1,
			(volume: -4, pan: (pan: 0.2, width: 3), sends: (bus1: (level: -6), bus5: (level: -3)), chain: (fx: ())));
	},
	check: {
		var one = ~radStripNow.(\track1);
		case
		{ ~radLeft.size != 2 } { "left: " ++ ~radLeft.cs }
		{ ~radLeft.any{|text| text.contains("bus5") }.not } { "bus5 not named: " ++ ~radLeft.cs }
		{ ~radLeft.any{|text| text.contains("width") }.not } { "width not named: " ++ ~radLeft.cs }
		{ [one[\volume], one[\pan], one[\sends]] != [-4, (pan: 0.2), (bus1: (level: -6))] } { "track1: " ++ one.cs }
		{ true }
	}
);

// a track fed by an Ndef of your own keeps it; one nothing feeds gets a block
~radTest.("track-load-no-block",
	setup: ["start 3", "blk 1 sine", "tsave t 1 radtest_track9"],
	code: {
		var az = Radicles.aZ;
		Ndef(\radtestOwn, { SinOsc.ar(220, 0, 0.1) });
		az.input(Ndef(\radtestOwn), \track, 2);
		//nothing feeds track 3
		az.inputs.removeAt(az.inputs.detectIndex{|item| item[0] == \inTrack3 });
		1.wait;
		~radLeft2 = RadUnit.applyTrack(\track, 2, RadUnit.readTrackPreset('radtest_track9'));
		~radLeft3 = RadUnit.applyTrack(\track, 3, RadUnit.readTrackPreset('radtest_track9'));
	},
	check: {
		var three = ~radStripNow.(\track3)[\input];
		case
		{ ~radStripNow.(\track2)[\input] != [\radtestOwn] } { "track 2 input: " ++ ~radStripNow.(\track2)[\input].cs }
		{ ~radLeft2.any{|text| text.contains("radtestOwn") }.not } { "not named: " ++ ~radLeft2.cs }
		{ three.isNil or: { three.size != 1 } } { "track 3 input: " ++ three.cs }
		{ (RadUnit.readBlock(three[0]) ? ())[\def] != \sine } { "track 3 source: " ++ RadUnit.readBlock(three[0]).cs }
		{ ~radLeft3.notEmpty } { "track 3 left: " ++ ~radLeft3.cs }
		{ true }
	},
	//no Ndef(\radtestOwn).clear here: its node may be gone already (a server
	//error on the Mac), and each test runs in its own sclang
	teardown: { ~radTrackClean.(['radtest_track9']) },
	docCheck: false
);

// tload lists presets, and refuses a name it doesn't have
~radTest.("track-list-and-missing",
	setup: ["start 2", "tsave t 1 radtest_track10"],
	cmds: ["tload", "tload t 1 radtest_nothing"],
	check: {
		RadUnit.trackPresetNames.includes('radtest_track10') or: { "names: " ++ RadUnit.trackPresetNames.cs }
	},
	teardown: { ~radTrackClean.(['radtest_track10']) }
);

// a whole strip round trip: save, change everything, load back
~radTest.("track-round-trip",
	setup: ["start 2 1", "lp 1 marilyn1", "fx t 1 pch", "fx t 1 2 dl1", "fxset t 1 1 pitchDispertion sin",
		"fxbypass t 1 2 1", "vol t 1 -6", "pan t 1 0.3", "trim t 1 -2", "trimlag t 1 0.5",
		"snd t 1 1 1 -9", "sndset t 1 1 sin"],
	code: {
		var before;
		Radicles.cW.callFunc("tsave t 1 radtest_track11"); 2.wait;
		~radBefore = [~radTrackPart.(\track1), ~radChainPlainT.(\track1), ~radBlockArgs.(1)];
		["blk 1 sine", "fx t 1 lpf", "fx t 1 2 hpf", "vol t 1 0", "pan t 1 -1", "trim t 1 0", "trimlag t 1 0",
			"snd t 1 1 0", "mute t 1"].do{|cmd| Radicles.cW.callFunc(cmd); 1.5.wait };
		Radicles.cW.callFunc("tload t 1 radtest_track11"); 12.wait;
		~radAfter = [~radTrackPart.(\track1), ~radChainPlainT.(\track1), ~radBlockArgs.(1)];
	},
	check: {
		var diff = RadDoc.diff(~radBefore, ~radAfter, 1e-9);
		diff.isEmpty or: { "differences: " ++ diff.collect(_[\address]).cs }
	},
	teardown: { ~radTrackClean.(['radtest_track11']) }
);

// ---- the output to the master, as saved (17 Nov 2026) ----

// a strip that only sends (sndry) keeps its output off on another track
~radTest.("track-load-output-off",
	setup: ["start 2 1", "sndry t 1 1 1 -6", "tsave t 1 radtest_dry"],
	cmds: [["tload t 2 radtest_dry", 8]],
	check: {
		var two = ~radStripNow.(\track2);
		(two[\output] == \none and: { two[\sends] == (bus1: (level: -6)) }) or: { "track2: " ++ two.cs }
	},
	teardown: { ~radTrackClean.(['radtest_dry']) }
);

// on a bus too
~radTest.("track-load-output-bus",
	setup: ["start 1 2", "outmenu b 1 0", "tsave b 1 radtest_busdry"],
	cmds: [["tload b 2 radtest_busdry", 8]],
	check: { (~radStripNow.(\bus2)[\output] == \none) or: { "bus2: " ++ ~radStripNow.(\bus2).cs } },
	teardown: { ~radTrackClean.(['radtest_busdry']) }
);

// a preset saved before then has no output: the strip's is left as it is
~radTest.("track-load-output-old-preset",
	setup: ["start 2 1", "outmenu t 2 0"],
	code: {
		RadUnit.writeTrackPreset((from: \track, trim: 0, volume: -3, mute: 0, chain: (fx: ()), sends: ()), 'radtest_old');
		Radicles.cW.callFunc("tload t 2 radtest_old"); 8.wait;
	},
	check: {
		var two = ~radStripNow.(\track2);
		(two[\output] == \none and: { two[\volume] == -3 }) or: { "track2: " ++ two.cs }
	},
	teardown: { ~radTrackClean.(['radtest_old']) }
);

// tload default: a clean strip goes to the master
~radTest.("track-load-default-output",
	setup: ["start 2 1", "sndry t 1 1 1 -6"],
	cmds: [["tload t 1 default", 8]],
	check: {
		var one = ~radStripNow.(\track1);
		(one[\output] == \master and: { one[\sends].isNil }) or: { "track1: " ++ one.cs }
	}
);
