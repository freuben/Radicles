// A track preset loaded over time (Document Model, step 4, PR 3; "Time in
// Radicles", 19 Nov 2026): tload moves a whole strip to a track preset over
// one time: the chain as csload, the source as blkload (or a crossfade to
// another block), trim, volume, pan and sends glide, mute switches off at the
// start or on at the end, the output to the master on at the start or off at
// the end. Without a time, the strip's own fade time is used. (These tests
// were written for tmorph, which tload replaced; see also
// load-transition.scd.)

~radTrackClean = {|names|
	names.do{|name| File.delete(RadUnit.trackPresetPath(name)) };
};
~radStripNow = {|name| RadUnit.refresh(RadDoc.current.copy).at(name) ? () };
~radCmd = {|cmd, wait = 0.5| Radicles.cW.callFunc(cmd); wait.wait };

// with glide the values glide over the time: the volume as one steady move
// (its 'ramp' is the load's time; the strip's own time, vollag, is not
// touched), and the trim and sends the same (without the word they
// crossfade: load-transition.scd)
~radTest.("tmorph-values-glide",
	setup: ["start 2 1", "vol t 2 -6", "pan t 2 0.5", "trim t 2 3", "snd t 2 1 1 -9",
		"tsave t 2 radtest_tm1", "vol t 1 -20"],
	code: {
		~radCmd.("tload t 1 radtest_tm1 3 glide", 1);
		~radMid = [Ndef(\track1).get(\ramp), Ndef(\inTrack1).get(\ramp), ~radStripNow.(\track1)[\sends], Ndef(\track1).get(\lagTime),
			Ndef(\inTrack1).get(\lagTime), Ndef(\busIn1).get(\ramp2)];
		4.wait;
		~radEnd = ~radStripNow.(\track1);
	},
	check: {
		case
		{ ~radMid[0] != 3 or: { ~radMid[1] != 3 } or: { ~radMid[3] != 0.05 } or: { ~radMid[4] != 0.05 } } { "times during the morph: " ++ ~radMid.cs }
		{ ~radMid[2].isNil or: { ~radMid[2][\bus1][\lag].notNil } or: { ~radMid[5] != 3 } } { "send during the morph: " ++ ~radMid.cs }
		{ [~radEnd[\volume], ~radEnd[\pan], ~radEnd[\trim]] != [-6, (pan: 0.5), 3] } { "values: " ++ ~radEnd.cs }
		{ ~radEnd[\lags].notNil } { "lags after: " ++ ~radEnd[\lags].cs }
		{ ~radEnd[\sends] != (bus1: (level: -9)) } { "sends after: " ++ ~radEnd[\sends].cs }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_tm1']) }
);

// the source: the same block moves as blkload does; another block crossfades in
~radTest.("tmorph-source",
	setup: ["start 3", "blk 1 sine", "blkset 1 freq 330", "tsave t 1 radtest_tm2", "blk 2 sine", "blk 3 noise"],
	code: {
		~radCmd.("tload t 2 radtest_tm2 2", 0.2);
		~radCmd.("tload t 3 radtest_tm2 2", 1);
		~radMidFade = Ndef(\block3).fadeTime;
		3.wait;
	},
	check: {
		var two = RadUnit.readBlock(\block2) ? (), three = RadUnit.readBlock(\block3) ? ();
		case
		{ two[\def] != \sine or: { two[\args][\freq] != 330 } } { "block 2: " ++ two.cs }
		{ three[\def] != \sine or: { three[\args][\freq] != 330 } } { "block 3: " ++ three.cs }
		{ ~radMidFade != 2 } { "block 3 fade time during the morph: " ++ ~radMidFade }
		{ Ndef(\block3).fadeTime != 0.5 } { "block 3 fade time after: " ++ Ndef(\block3).fadeTime }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_tm2']) }
);

// the chain moves as csload does
~radTest.("tmorph-chain",
	setup: ["start 2", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "tsave t 1 radtest_tm3", "fx t 2 pch", "fx t 2 2 dl1"],
	code: { ~radCmd.("tload t 2 radtest_tm3 1", 3) },
	check: {
		var fx = ~radStripNow.(\track2)[\fx] ? ();
		(fx.collect(_[\def]) == (1: \pch) and: { fx[1][\args][\pitchRatio] == 0.5 }) or: { "track2 fx: " ++ fx.cs }
	},
	teardown: { ~radTrackClean.(['radtest_tm3']) }
);

// a send the preset doesn't have glides out, then goes; the output stays
~radTest.("tmorph-send-removed",
	setup: ["start 2 1", "tsave t 2 radtest_tm4", "snd t 1 1 1 -6"],
	code: {
		~radCmd.("tload t 1 radtest_tm4 2", 1);
		~radMidSends = ~radStripNow.(\track1)[\sends];
		2.wait;
	},
	check: {
		var one = ~radStripNow.(\track1);
		case
		{ ~radMidSends.isNil } { "the send went at once" }
		{ one[\sends].notNil } { "sends after: " ++ one[\sends].cs }
		{ one[\output] != \master } { "output: " ++ one[\output] }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_tm4']) }
);

// mute: on at the end; off at the start
~radTest.("tmorph-mute",
	setup: ["start 2", "mute t 2", "tsave t 2 radtest_tm5", "unmute t 2", "tsave t 2 radtest_tm6", "mute t 2"],
	code: {
		~radCmd.("tload t 1 radtest_tm5 2", 0.5);
		~radCmd.("tload t 2 radtest_tm6 2", 0.5);
		~radMidMutes = [~radStripNow.(\track1)[\mute], ~radStripNow.(\track2)[\mute]];
		2.5.wait;
	},
	check: {
		var ends = [~radStripNow.(\track1)[\mute], ~radStripNow.(\track2)[\mute]];
		case
		{ ~radMidMutes != [0, 0] } { "during: " ++ ~radMidMutes.cs }
		{ ends != [1, 0] } { "after: " ++ ends.cs }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_tm5', 'radtest_tm6']) }
);

// none: at once, as tload
~radTest.("tmorph-none",
	setup: ["start 2", "vol t 2 -6", "tsave t 2 radtest_tm7"],
	code: { ~radCmd.("tload t 1 radtest_tm7 5 none", 1) },
	check: {
		var one = ~radStripNow.(\track1);
		(one[\volume] == -6 and: { one[\lags].isNil }) or: { "track1: " ++ one.cs }
	},
	teardown: { ~radTrackClean.(['radtest_tm7']) }
);

// a tload (at once) during another stops it where it is and puts the lags back
~radTest.("tmorph-interrupted",
	setup: ["start 2 1", "vol t 2 -6", "snd t 2 1 1 -9", "tsave t 2 radtest_tm8"],
	code: {
		~radCmd.("tload t 1 radtest_tm8 6", 1);
		~radCmd.("tload t 1 default none", 3);
	},
	check: {
		var one = ~radStripNow.(\track1);
		case
		{ one[\volume] != 0 or: { one[\sends].notNil } } { "track1: " ++ one.cs }
		{ one[\lags].notNil or: { Ndef(\track1).get(\lagTime) != 0.05 } } { "lags: " ++ one[\lags].cs }
		{ RadUnit.prTrackMorphs[\track1].notNil } { "the morph is still running" }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_tm8']) }
);

// the strip's own fade time is the load's time when the command gives none
~radTest.("tmorph-strip-fade-time",
	setup: ["start 2", "vol t 2 -6", "tsave t 2 radtest_tm9", "fade t 1 3"],
	code: {
		~radCmd.("tload t 1 radtest_tm9", 1);
		~radMidLag = [Ndef(\track1).get(\lagTime) ? 0, Ndef(\track1).fadeTime];
		4.wait;
	},
	check: {
		var one = ~radStripNow.(\track1);
		case
		{ ~radMidLag != [0.05, 3] } { "volume lag and fade time during the load: " ++ ~radMidLag }
		{ one[\volume] != -6 or: { one[\lags].notNil } } { "track1 after: " ++ one.cs }
		{ Ndef(\track1).fadeTime != 3 } { "track1's fade time after: " ++ Ndef(\track1).fadeTime }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_tm9']) }
);

// nosrc and nofx with a time; an unknown word is refused
~radTest.("tmorph-options",
	setup: ["start 2", "blk 1 sine", "fx t 1 pch", "vol t 1 -6", "tsave t 1 radtest_tm10", "blk 2 noise", "fx t 2 dl1"],
	//the first waits out its load, so the lag going back is in its own snapshot
	cmds: [["tload t 2 radtest_tm10 1 nosrc nofx", 2.5], "tload t 2 radtest_tm10 1 sideways"],
	check: {
		var two = ~radStripNow.(\track2);
		case
		{ (RadUnit.readBlock(\block2) ? ())[\def] != \noise } { "block 2: " ++ RadUnit.readBlock(\block2).cs }
		{ (two[\fx] ? ()).collect(_[\def]) != (1: \dl1) } { "fx: " ++ two[\fx].cs }
		{ two[\volume] != -6 } { "volume: " ++ two[\volume] }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_tm10']) }
);

// the output to the master, as saved (17 Nov 2026): off only at the end
~radTest.("tmorph-output-off-at-end",
	setup: ["start 2 1", "sndry t 2 1 1 -6", "tsave t 2 radtest_tmdry"],
	code: {
		~radCmd.("tload t 1 radtest_tmdry 2", 1);
		~radMidOut = ~radStripNow.(\track1)[\output];
		3.wait;
	},
	check: {
		var out = ~radStripNow.(\track1)[\output];
		case
		{ ~radMidOut != \master } { "output during the morph: " ++ ~radMidOut.cs }
		{ out != \none } { "output after: " ++ out.cs }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_tmdry']) }
);

// ... and on at the start
~radTest.("tmorph-output-on-at-start",
	setup: ["start 2 1", "tsave t 2 radtest_tmwet", "outmenu t 1 0"],
	code: {
		~radCmd.("tload t 1 radtest_tmwet 2", 1);
		~radMidOut = ~radStripNow.(\track1)[\output];
		3.wait;
	},
	check: {
		var out = ~radStripNow.(\track1)[\output];
		case
		{ ~radMidOut != \master } { "output during the morph: " ++ ~radMidOut.cs }
		{ out != \master } { "output after: " ++ out.cs }
		{ true }
	},
	teardown: { ~radTrackClean.(['radtest_tmwet']) }
);

// tload default over a time: the track fades out whole over it (its
// volume; the block keeps its own fade time), then the source stops
// (18 and 19 Nov 2026)
~radTest.("tmorph-default-source",
	setup: ["start 2", "blk 1 sine", "vol t 1 -6"],
	code: {
		~radCmd.("tload t 1 default 2", 1);
		~radMidFade = [Ndef(\block1).fadeTime, Ndef(\track1).fadeTime, Ndef(\track1).get(\volume)];
		4.wait;
	},
	check: {
		var one = ~radStripNow.(\track1);
		case
		{ ~radMidFade != [0.5, 2, -inf] } { "block 1's and track 1's fade time and the volume during the load: " ++ ~radMidFade }
		{ RadUnit.readBlock(\block1).notNil } { "block 1 still plays: " ++ RadUnit.readBlock(\block1).cs }
		{ one[\volume] != 0 } { "track1: " ++ one.cs }
		{ true }
	}
);
