// Cue blocks (streamed from disk) at the file's own speed (6 Nov 2026).
// A cue buffer holds a few frames at the server's rate, so BufRateScale
// cannot tell the file's rate; the block's code gets the file's sample rate
// from its header, and VDiskIn plays at rate * fileRate / SampleRate.ir.
// marilyn1 is a 44.1 kHz file; the cloud's server runs at 48 kHz.

~radTest.("cue-file-rate-in-code",
	setup: ["start 2"],
	cmds: [["blk 1 cue marilyn1", 2]],
	check: {
		var code = Ndef(\block1).source.cs;
		(code.includesString("VDiskIn") and: { code.includesString("44100 / SampleRate.ir") }) or: { "code: " ++ code }
	}
);

// rate is the cue block's own argument now
~radTest.("cue-rate-arg",
	setup: ["start 2", ["blk 1 cue marilyn1", 2]],
	cmds: [["blkset 1 rate 0.5", 0.5]],
	check: { (Ndef(\block1).get(\rate) == 0.5) or: { "rate " ++ Ndef(\block1).get(\rate) } }
);

// the block is in the document with its file
~radTest.("cue-in-document",
	setup: ["start 2"],
	cmds: [["blk 1 cue marilyn1", 2]],
	check: {
		var unit = RadUnit.readBlock(\block1);
		(unit.notNil and: { unit[\def] == \cue } and: { unit[\buffer][\cue] == \marilyn1 }) or: { "unit: " ++ unit.cs }
	}
);

// heard speed: a 440 Hz tone in a 44.1 kHz and in a 48 kHz file, streamed by
// a cue block, measured on the server (rising zero crossings per second of
// the server's clock). Before the fix the 44.1 kHz file sounded at 479 Hz on
// a 48 kHz server.
~radCueDir = { Radicles.soundFilePath +/+ "Play" +/+ "radtest_cue" };
~radCueMake = {
	File.mkdir(~radCueDir.());
	[44100, 48000].do{|sr|
		var sf = SoundFile.new.headerFormat_("WAV").sampleFormat_("float").numChannels_(1).sampleRate_(sr);
		sf.openWrite(~radCueDir.() +/+ "radtest_tone" ++ (sr div: 1000) ++ ".wav");
		sf.writeData(FloatArray.fill(sr * 20, {|i| sin(2pi * 440 * i / sr) * 0.1 }));
		sf.close;
	};
};
~radCueClean = {
	(~radCueDir.() +/+ "*").pathMatch.do{|p| File.delete(p) };
	if(File.exists(~radCueDir.()), { ("rmdir " ++ ~radCueDir.().shellQuote).systemCmd });
	BStore.playFolder = 0;
};
~radCueFreq = {|file|
	var bus = Bus.control(Server.default, 2), synth, read, a, b;
	Radicles.cW.callFunc("blk 1 cue " ++ file); 3.wait;
	synth = { var sig = Ndef.ar(\block1, 1), rise = (sig > 0) * (Delay1.ar(sig) <= 0);
		Out.kr(bus, [A2K.kr(PulseCount.ar(rise)), Sweep.kr(0, 1)]) }.play(addAction: \addToTail);
	0.3.wait;
	read = { var cond = Condition(false), v; bus.getn(2, {|x| v = x; cond.test = true; cond.signal }); cond.wait; v };
	a = read.(); 1.5.wait; b = read.();
	synth.free; bus.free;
	[file, (b[0] - a[0]) / (b[1] - a[1])]
};

~radTest.("cue-own-speed-44k-and-48k-files",
	setup: ["start 2"],
	code: {
		var capture = Radicles.postCapture;
		~radCueMake.();
		BStore.playFolder = "radtest_cue";
		Radicles.postCapture = nil;
		~radCueResults = [\radtest_tone44, \radtest_tone48].collect{|file| ~radCueFreq.(file) };
		Radicles.postCapture = capture;
	},
	check: {
		var off = ~radCueResults.reject{|r| (r[1] - 440).abs < 5 };
		off.isEmpty or: { "server at " ++ Server.default.sampleRate ++ " Hz: " ++
			~radCueResults.collect{|r| [r[0], r[1].round(0.1)] }.cs }
	},
	teardown: ~radCueClean
);
