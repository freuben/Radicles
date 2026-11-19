// Sound files at their own speed on any server rate (5 Nov 2026, the
// BufRateScale fix). Mac only (the cloud's audio runs at 48 kHz only).
// Run it once per server rate, with the rate in RADTEST_SR, e.g. in the IDE:
//   "RADTEST_SR".setenv(44100); ~radTestMode = \check; ~radTestCase = "sample-rates"; ...
//   "RADTEST_SR".setenv(48000); ...
// Each test plays a 44.1 kHz and a 48 kHz file (made here, in
// SoundFiles/Play/radtest_rates, removed after) through a loop block and
// measures, against the server's own clock, how fast it moves through it.

"RADTEST_SR".getenv !? {|sr| s.options.sampleRate = sr.asInteger };

~radRatesDir = { Radicles.soundFilePath +/+ "Play" +/+ "radtest_rates" };
// a 20 s 440 Hz tone at a file sample rate (long enough not to loop while measured)
~radRatesMake = {
	File.mkdir(~radRatesDir.());
	[44100, 48000].do{|sr|
		var sf = SoundFile.new.headerFormat_("WAV").sampleFormat_("float").numChannels_(1).sampleRate_(sr);
		sf.openWrite(~radRatesDir.() +/+ "radtest_tone" ++ (sr div: 1000) ++ ".wav");
		sf.writeData(FloatArray.fill(sr * 20, {|i| sin(2pi * 440 * i / sr) * 0.1 }));
		sf.close;
	};
};
~radRatesClean = {
	(~radRatesDir.() +/+ "*").pathMatch.do{|p| File.delete(p) };
	if(File.exists(~radRatesDir.()), { ("rmdir " ++ ~radRatesDir.().shellQuote).systemCmd });
	BStore.playFolder = 0;
};
// block N's position, and the server's clock, read one after the other
~radRatesRead = {|bus|
	var cond = Condition(false), pos, clock;
	Block.position(1, {|v| pos = v; cond.test = true; cond.signal }); cond.wait;
	cond.test = false;
	bus.get({|v| clock = v; cond.test = true; cond.signal }); cond.wait;
	[pos, clock]
};
// how fast block 1 moves through its file, per second of the server's clock
~radRatesSpeed = {|file|
	var clockBus = Bus.control(Server.default, 1), clock, a, b, speed;
	Radicles.cW.callFunc("lp 1 " ++ file); 3.wait;
	clock = { Out.kr(clockBus, Sweep.kr(0, 1)) }.play; 0.2.wait;
	a = ~radRatesRead.(clockBus); 1.5.wait; b = ~radRatesRead.(clockBus);
	clock.free; clockBus.free;
	speed = (a[0].notNil and: { b[0].notNil }).if({ (b[0] - a[0]) / (b[1] - a[1]) });
	[file, speed]
};

~radTest.("own-speed-44k-and-48k-files",
	setup: ["start 2"],
	code: {
		var capture = Radicles.postCapture;
		~radRatesMake.();
		BStore.playFolder = "radtest_rates";
		//the posted code is the usual loop block's, not what this test is about
		Radicles.postCapture = nil;
		~radRatesResults = [\radtest_tone44, \radtest_tone48].collect{|file| ~radRatesSpeed.(file) };
		Radicles.postCapture = capture;
		~radRatesServer = Server.default.sampleRate;
	},
	check: {
		var off = ~radRatesResults.reject{|r| r[1].notNil and: { (r[1] - 1).abs < 0.01 } };
		off.isEmpty or: { "server at " ++ ~radRatesServer ++ " Hz: " ++
			~radRatesResults.collect{|r| [r[0], r[1] !? {|v| v.round(0.001) }] }.cs }
	},
	teardown: ~radRatesClean,
	cloud: false
);
