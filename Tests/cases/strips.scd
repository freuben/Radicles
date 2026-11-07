// Strip values in the document (Document Model, step 3, PR 1): each strip of
// the mixer has, at its own address, its trim, volume, mute, pan, lags,
// modulators, solo and output, read from the running system, beside its
// effects. Every test in the suite also runs the consistency check after each
// command (Tests/child.scd).

// the document's strip at an address, values refreshed from the running system
~radStripAt = {|name| RadUnit.refresh(RadDoc.current.copy).at(name) };
// a strip without its effects, for comparing
~radStripValues = {|name| (~radStripAt.(name) ? ()).reject{|v, k| k == \fx } };

~radTest.("strip-values",
	setup: ["start 2 1"],
	cmds: ["vol t 1 -6", "pan t 1 -0.3", "trim t 2 3", "mute t 2"],
	check: {
		var one = ~radStripValues.(\track1), two = ~radStripValues.(\track2);
		case
		{ one != (kind: \strip, channels: 1, space: \pan2, trim: 0, volume: -6, mute: 0,
			pan: (pan: -0.3), solo: 0, output: \master) } { "track1: " ++ one.cs }
		{ [two[\trim], two[\mute], two[\volume]] != [3, 1, 0] } { "track2: " ++ two.cs }
		{ true }
	}
);

// a bus like a track; the master has no solo and no output
~radTest.("strip-bus-and-master",
	setup: ["start 2 1"],
	cmds: ["vol b 1 -3", "vol m -2"],
	check: {
		var bus = ~radStripValues.(\bus1), master = ~radStripValues.(\master);
		case
		{ bus[\volume] != -3 or: { bus[\output] != \master } } { "bus1: " ++ bus.cs }
		{ master[\volume] != -2 or: { master.includesKey(\solo) } or: { master.includesKey(\output) } } {
			"master: " ++ master.cs }
		{ true }
	}
);

// every strip of the mixer is in the document, with its effects beside it
~radTest.("strip-all-strips",
	setup: ["start 2 1"],
	cmds: ["fx t 1 pch"],
	check: {
		var strips = RadDoc.current.at(\strips);
		case
		{ strips.keys != Set[\track1, \track2, \bus1, \master] } { "strips: " ++ strips.keys.cs }
		{ strips.every{|unit| unit[\kind] == \strip }.not } { "kinds: " ++ strips.collect(_[\kind]).cs }
		{ RadDoc.current.at("track1.fx.1.def") != \pch } { "track1: " ++ strips[\track1].cs }
		{ true }
	}
);

~radTest.("strip-solo-and-output",
	setup: ["start 3"],
	cmds: ["solo t 2", "outmenu t 3 0"],
	check: {
		var got = [\track1, \track2, \track3].collect{|name|
			[~radStripValues.(name)[\solo], ~radStripValues.(name)[\output]] };
		(got == [[0, \master], [1, \master], [0, \none]]) or: { "solo, output: " ++ got.cs }
	}
);

// vollag sets the volume's lagTime; trimlag and panlag set the Ndef's own lag
~radTest.("strip-lags",
	setup: ["start 2"],
	cmds: ["vollag t 1 2", "trimlag t 2 0.5", "panlag t 2 1.5"],
	check: {
		var one = ~radStripValues.(\track1)[\lags], two = ~radStripValues.(\track2)[\lags];
		(one == (volume: 2) and: { two == (trim: 0.5, pan: (pan: 1.5)) }) or: { "lags: " ++ [one, two].cs }
	}
);

// a modulated volume or pan: the modulator by what it is
~radTest.("strip-modulators",
	setup: ["start 2"],
	cmds: ["vol t 1 sin", "pan t 2 sin"],
	check: {
		var one = ~radStripValues.(\track1)[\mods], two = ~radStripValues.(\track2)[\mods];
		case
		{ one.isNil or: { one.keys != Set[\volume] } or: { one[\volume][\type] != \sin } } {
			"track1 mods: " ++ one.cs }
		{ two.isNil or: { two.keys != Set[\pan] } or: { two[\pan][\pan][\type] != \sin } } {
			"track2 mods: " ++ two.cs }
		{ true }
	}
);

// a track added later is in the document, and a list kept by position
// (outputSettings) stays with its strip with no window open
~radTest.("strip-added",
	setup: ["start 2 1", "outmenu b 1 0"],
	cmds: [["t add 1", 4]],
	check: {
		var got = [\track1, \track2, \track3, \bus1].collect{|name|
			~radStripValues.(name) !? {|unit| unit[\output] } };
		(got == [\master, \master, \master, \none]) or: { "outputs: " ++ got.cs }
	}
);

// adding a track while another is soloed keeps the solo (it failed before)
~radTest.("strip-added-with-solo",
	setup: ["start 2 1", "solo t 2"],
	cmds: [["t add 1", 4]],
	check: {
		var got = [\track1, \track2, \track3, \bus1].collect{|name|
			~radStripValues.(name) !? {|unit| unit[\solo] } };
		(got == [0, 1, 0, 0]) or: { "solo: " ++ got.cs }
	}
);

// removing a middle track: its strip goes, its neighbours keep their own values
~radTest.("strip-removed-middle",
	setup: ["start 3", "solo t 3", "mute t 3", "vol t 3 -9"],
	cmds: [["t rmvn [2]", 4]],
	check: {
		var three = ~radStripValues.(\track3);
		case
		{ RadDoc.current.at(\track2).notNil } { "track2: " ++ RadDoc.current.at(\track2).cs }
		{ [three[\solo], three[\mute], three[\volume]] != [1, 1, -9] } { "track3: " ++ three.cs }
		{ true }
	}
);

// values are read when needed, so faders notify nothing; strips added or
// removed do
~radTest.("strip-notifications",
	setup: ["start 2"],
	code: {
		~radHeard = [];
		~radListener = {|doc, what, path| ~radHeard = ~radHeard.add([what, RadDoc.addressString(path)]) };
		RadDoc.current.addDependant(~radListener);
		Radicles.cW.callFunc("vol t 1 -6"); 1.wait;
		Radicles.cW.callFunc("pan t 1 0.5"); 1.wait;
		Radicles.cW.callFunc("t add 1"); 4.wait;
		Radicles.cW.callFunc("t rmvn 1"); 4.wait;
		RadDoc.current.removeDependant(~radListener);
	},
	check: {
		(~radHeard == [[\put, "track3"], [\removeAt, "track3"]]) or: { "heard: " ++ ~radHeard.cs }
	}
);

// a document with strips writes as code and reads back the same
~radTest.("strip-document-file",
	setup: ["start 2 1", "vol t 1 -6", "pan t 2 0.4", "vol t 2 sin", "solo t 1", "fx t 1 pch"],
	check: {
		var doc = RadUnit.refresh(RadDoc.current.copy), path = "/tmp/radtest_doc/strips.scd", back;
		doc.write(path);
		back = RadDoc.read(path);
		File.delete(path);
		if(back.isNil, { "not read" }, {
			RadDoc.diff(doc, back).isEmpty or: { "differences: " ++ RadDoc.diff(doc, back).collect(_[\address]).cs }
		})
	}
);
