// Block position (Document Model, step 2, PR 3; option A, decided 3 Nov
// 2026): a sound-file block writes where it plays (seconds of its file) to a
// control bus; Block.position reads it, blkget 1 position posts it.
// marilyn1 is 49.04 s, drums1 124.7 s (44.1 kHz files on a 48 kHz server).

// block N's position, read from the server (inside a Routine)
~radPos = {|block = 1|
	var cond = Condition(false), value;
	Block.position(block, {|v| value = v; cond.test = true; cond.signal });
	cond.wait;
	value
};
~radNear = {|value, low, high| value.notNil and: { value >= low } and: { value <= high } };

// a loop reports its position, and it moves with time
~radTest.("position-reported",
	setup: ["start 2", ["lp 1 marilyn1", 3]],
	code: { ~radP1 = ~radPos.(); 1.wait; ~radP2 = ~radPos.() },
	check: {
		case
		{ ~radP1.isNil } { "no position" }
		{ ~radNear.(~radP1, 0.5, 6).not } { "position " ++ ~radP1 }
		{ ~radNear.(~radP2 - ~radP1, 0.8, 1.2).not } { "moved " ++ (~radP2 - ~radP1) ++ " in 1 s" }
		{ true }
	}
);

// it starts at startPos
~radTest.("position-startpos",
	setup: ["start 2", ["lp 1 marilyn1 [startPos, 0.5]", 3]],
	code: { ~radP1 = ~radPos.() },
	check: { ~radNear.(~radP1, 24.5, 30) or: { "position " ++ ~radP1 } }
);

// twice the rate, twice as fast
~radTest.("position-rate",
	setup: ["start 2", ["lp 1 marilyn1", 3], ["blkset 1 rate 2", 1]],
	code: { ~radP1 = ~radPos.(); 1.wait; ~radP2 = ~radPos.() },
	check: { ~radNear.(~radP2 - ~radP1, 1.7, 2.3) or: { "moved " ++ (~radP2 - ~radP1) ++ " in 1 s at rate 2" } }
);

// a loop wraps at the end of the file
~radTest.("position-loop-wraps",
	setup: ["start 2", ["lp 1 marilyn1 [startPos, 0.98]", 4]],
	code: { ~radP1 = ~radPos.() },
	check: { ~radNear.(~radP1, 0, 8) or: { "position " ++ ~radP1 ++ " (should have wrapped)" } }
);

// play (no loop) stops at the end
~radTest.("position-play-stops",
	setup: ["start 2", ["blk 1 play marilyn1", 1], ["blkset 1 startPos 0.99", 0.2], ["blkset 1 trigger 0", 0.1], ["blkset 1 trigger 1", 3]],
	code: { ~radP1 = ~radPos.() },
	check: { ~radNear.(~radP1, 48.9, 49.1) or: { "position " ++ ~radP1 ++ " (should stop at 49.04)" } }
);

// a retrigger goes back to startPos
~radTest.("position-retrigger",
	setup: ["start 2", ["lp 1 marilyn1", 4]],
	code: {
		Radicles.cW.callFunc("blkset 1 trigger 0"); 0.1.wait;
		Radicles.cW.callFunc("blkset 1 trigger 1"); 0.5.wait;
		~radP1 = ~radPos.();
	},
	check: { ~radNear.(~radP1, 0, 1.5) or: { "position " ++ ~radP1 ++ " (should be back near 0)" } }
);

// after a crossfade to another file, the new copy owns the bus (startPos
// 0.5 stays in the block's node map, so marilyn1 also starts half way)
~radTest.("position-crossfade",
	setup: ["start 2", ["lp 1 drums1 [startPos, 0.5]", 3]],
	cmds: [["lp 1 marilyn1", 4]],
	code: { ~radP1 = ~radPos.() },
	check: { ~radNear.(~radP1, 24.5, 32) or: { "position " ++ ~radP1 ++ " (marilyn1 from half way, not drums1 at 62 s)" } }
);

// every Main block that plays a file through time reports
~radTest.("position-main-blocks",
	setup: ["start 2"],
	code: {
		var capture = Radicles.postCapture;
		//the posted code is not what this test is about
		Radicles.postCapture = nil;
		~radReports = [\play, \loop, \playbuf, \looptr, \play2, \loop2].collect{|def|
			Radicles.cW.callFunc("blk 1 " ++ def ++ " marilyn1"); 2.5.wait;
			[def, ~radPos.()]
		};
		Radicles.postCapture = capture;
	},
	check: {
		var missing = ~radReports.select{|pair| pair[1].isNil }.collect(_[0]);
		missing.isEmpty or: { "no position: " ++ missing.cs }
	}
);

// a block that plays no file reports nothing
~radTest.("position-none",
	setup: ["start 2", "blk 1 sine"],
	cmds: ["blkget 1 position"],
	code: { ~radP1 = ~radPos.() },
	check: { ~radP1.isNil or: { "position " ++ ~radP1 } }
);

// the startPos that starts a block's file at a time
~radTest.("position-startpos-of",
	setup: ["start 2", ["lp 1 marilyn1", 3]],
	check: {
		var v = Block.startPosOf(1, 24.52);
		~radNear.(v, 0.499, 0.501) or: { "startPosOf: " ++ v }
	}
);

// after the server quits or boots, the old bus numbers belong to nobody (or
// to an Ndef): the block forgets its bus and makes a new one when it plays
~radTest.("position-bus-after-reboot",
	setup: ["start 2", ["lp 1 marilyn1", 3]],
	code: {
		var capture = Radicles.postCapture;
		~radBefore = Block.positionBus(1);
		ServerQuit.run(Server.default);
		~radForgotten = Block.positionBus(1).isNil;
		//the posted code holds buffer numbers that depend on the run: not captured
		Radicles.postCapture = nil;
		Radicles.cW.callFunc("lp 1 drums1"); 3.wait;
		Radicles.postCapture = capture;
		~radAfter = Block.positionBus(1);
		~radP1 = ~radPos.();
	},
	check: {
		case
		{ ~radBefore.isNil } { "no bus before" }
		{ ~radForgotten.not } { "the bus was kept after the server quit" }
		{ ~radAfter.isNil or: { ~radAfter === ~radBefore } } { "no new bus: " ++ ~radAfter }
		{ ~radP1.isNil } { "no position after" }
		{ true }
	}
);

// RadPosition reports exactly where PlayBuf plays: a file whose samples are
// their own position (a ramp), at 44.1 kHz on the server's rate, played with a
// modulated rate; PlayBuf's output and RadPosition agree (Mac check of 5 Nov
// found the position lagging: RadPosition scaled the rate and PlayBuf doesn't)
~radTest.("position-matches-playbuf",
	setup: ["start 2"],
	code: {
		var path = Platform.defaultTempDir +/+ "radtest_ramp.wav", sf, buf, bus, synth, cond = Condition(false);
		sf = SoundFile.new.headerFormat_("WAV").sampleFormat_("float").numChannels_(1).sampleRate_(44100);
		sf.openWrite(path); sf.writeData(FloatArray.fill(88200, {|i| i / 88200 })); sf.close;
		buf = Buffer.read(Server.default, path); Server.default.sync;
		bus = Bus.control(Server.default, 2);
		synth = {
			var rate = SinOsc.kr(0.7).range(0.3, 2.5);
			RadPosition.kr(bus.index, buf, rate, 1, 0, 1);
			Out.kr(bus.index + 1, A2K.kr(PlayBuf.ar(1, buf, rate, 1, 0, 1)) * 2);
			Silent.ar
		}.play;
		~radPairs = [];
		4.do{
			0.6.wait;
			cond.test = false;
			bus.getn(2, {|v| ~radPairs = ~radPairs.add(v); cond.test = true; cond.signal });
			cond.wait;
		};
		synth.free; buf.free; bus.free; File.delete(path);
	},
	check: {
		var off = ~radPairs.reject{|p| (p[0] - p[1]).abs < 0.005 };
		case
		{ ~radPairs.size != 4 } { "read " ++ ~radPairs.size ++ " times" }
		{ off.notEmpty } { "RadPosition vs PlayBuf: " ++ off.collect{|p| p.round(0.001) }.cs }
		{ true }
	}
);
