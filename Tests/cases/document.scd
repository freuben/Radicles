// The document (Document Model, step 1, PR 1): RadDoc and RadLiteral.
// No server or mixer needed: these test the data structure, its files and
// the safe reader.

// an effect unit as in the step 1 design note, with values that are easy to get wrong
~radDocUnit = {
	(kind: \filter, def: \pch,
		args: (pitchRatio: 0.5, pitchDispertion: 0, timeDispersion: 0.0001, winSize: 0.02,
			sum: 0.1 + 0.2, tiny: 1e-05, low: -inf, big: 123456789.0),
		mods: (pitchDispertion: (type: \sin, spec: [0, 1], mul: 1, add: 0, args: (freq: 5, phase: 0))),
		buffer: (name: \ir, path: "/Sounds/IR \"hall\" \\ 2.wav", numChannels: 2),
		data: [\convrev, 2048, (code: "{[0, 5, 7].choose}")],
		morphShape: \xfade, bypass: false, note: "two\nlines\tand a tab", char: $x,
		'odd key': 'a symbol with spaces', empty: (), none: [])
};

~radDocSample = {
	var doc = RadDoc.new;
	doc.put("track1.fx.2", ~radDocUnit.());
	doc.put("track10.fx.1", (kind: \filter, def: \lpf, args: (freq: 800)));
	doc.put("track2.fx.1", (kind: \filter, def: \dl1, args: ()));
	doc.put("bus1.fx.1", (kind: \filter, def: \pch, args: (pitchRatio: 2)));
	doc.put("master.fx.1", (kind: \filter, def: \lpf, args: (freq: 12000)));
	doc
};

// get and set by address, written three ways; strip names imply strips
~radTest.("doc-put-at",
	check: {
		var doc = RadDoc.new, problems = [];
		doc.put("track1.fx.2.args.pitchRatio", 0.5);
		doc.put('bus1.fx.1.def', \lpf);
		doc.put([\master, \fx, 1, \args, \freq], 440);
		if(doc.at("track1.fx.2.args.pitchRatio") != 0.5, { problems = problems.add("track1 pitchRatio") });
		if(doc.at([\track1, \fx, 2, \args, \pitchRatio]) != 0.5, { problems = problems.add("array address") });
		if(doc.at("bus1.fx.1.def") != \lpf, { problems = problems.add("bus1 def") });
		if(doc.at("master.fx.1.args.freq") != 440, { problems = problems.add("master freq") });
		if(doc.data[\strips][\track1][\fx][2][\args][\pitchRatio] != 0.5, { problems = problems.add("strips implied") });
		if(doc.at("track1.fx.2").isKindOf(Event).not, { problems = problems.add("track1.fx.2 is not an Event") });
		if(doc.at("track9.fx.1").notNil, { problems = problems.add("missing address not nil") });
		if(doc.at("track1.fx.2.args.pitchRatio.more").notNil, { problems = problems.add("address through a value not nil") });
		if(doc.at(\format) != 1, { problems = problems.add("format") });
		if(RadDoc.path("track1.fx.2.args.pitchRatio") != [\strips, \track1, \fx, 2, \args, \pitchRatio], {
			problems = problems.add("path " ++ RadDoc.path("track1.fx.2.args.pitchRatio").cs) });
		if(RadDoc.addressString([\strips, \bus1, \fx, 1]) != "bus1.fx.1", { problems = problems.add("addressString") });
		if(try { doc.put("track1.fx.2.args.pitchRatio.more", 1); true } { false }, {
			problems = problems.add("put through a value did not refuse") });
		problems.isEmpty or: { problems.join(", ") }
	}
);

~radTest.("doc-remove",
	check: {
		var doc = ~radDocSample.(), old, problems = [];
		old = doc.removeAt("track1.fx.2");
		if(old[\def] != \pch, { problems = problems.add("removeAt did not return the old value") });
		if(doc.at("track1.fx.2").notNil, { problems = problems.add("still there") });
		if(doc.at("track1.fx").isKindOf(Event).not, { problems = problems.add("parent removed too") });
		if(doc.removeAt("track1.fx.7").notNil, { problems = problems.add("missing address not nil") });
		problems.isEmpty or: { problems.join(", ") }
	}
);

// change times: the address and everything above it; siblings untouched
~radTest.("doc-change-times",
	check: {
		var doc = RadDoc.new, clock = 0, problems = [];
		doc.clockFunc = { clock = clock + 1 };
		doc.put("track1.fx.1.args.freq", 100);   // time 1
		doc.put("track2.fx.1.args.freq", 200);   // time 2
		doc.put("track1.fx.1.args.rq", 0.5);     // time 3
		if(doc.changedAt("track1.fx.1.args.freq") != 1, { problems = problems.add("freq " ++ doc.changedAt("track1.fx.1.args.freq")) });
		if(doc.changedAt("track1.fx.1") != 3, { problems = problems.add("track1.fx.1 " ++ doc.changedAt("track1.fx.1")) });
		if(doc.changedAt("track2") != 2, { problems = problems.add("track2 " ++ doc.changedAt("track2")) });
		if(doc.changedAt([]) != 3, { problems = problems.add("whole document " ++ doc.changedAt([])) });
		if(doc.changedAt("track3").notNil, { problems = problems.add("unchanged address not nil") });
		doc.removeAt("track2.fx.1");             // time 4
		if(doc.changedAt("track2.fx.1") != 4, { problems = problems.add("remove time") });
		if(doc.data.values.any{|v| v == 1 } and: { doc.data.keys.includes(\times) }, { problems = problems.add("times inside data") });
		problems.isEmpty or: { problems.join(", ") }
	}
);

// notifications through SuperCollider's dependants
~radTest.("doc-notifications",
	check: {
		var doc = RadDoc.new, heard = [], listener;
		listener = {|who, what, path, value| heard = heard.add([what, path, value]) };
		doc.addDependant(listener);
		doc.put("track1.fx.1.args.freq", 100);
		doc.removeAt("track1.fx.1");
		doc.removeDependant(listener);
		doc.put("track1.vol", -3);
		(heard == [[\put, [\strips, \track1, \fx, 1, \args, \freq], 100],
			[\removeAt, [\strips, \track1, \fx, 1], nil]]) or: { "heard " ++ heard.cs }
	}
);

// a copy is deep: changing it leaves the original alone
~radTest.("doc-copy-deep",
	check: {
		var doc = ~radDocSample.(), copy = doc.copy, problems = [];
		copy.put("track1.fx.2.args.pitchRatio", 3);
		copy.at("track1.fx.2.mods.pitchDispertion.spec").put(0, 99);
		if(doc.at("track1.fx.2.args.pitchRatio") != 0.5, { problems = problems.add("args shared") });
		if(doc.at("track1.fx.2.mods.pitchDispertion.spec") != [0, 1], { problems = problems.add("nested array shared") });
		if(copy.changedAt("track2.fx.1") != doc.changedAt("track2.fx.1"), { problems = problems.add("times not copied") });
		if(RadDoc.diff(doc, copy).size != 2, { problems = problems.add("diff " ++ RadDoc.diff(doc, copy).size) });
		problems.isEmpty or: { problems.join(", ") }
	}
);

// written as code and read back by RadLiteral: exactly the same data
~radTest.("doc-literal-roundtrip",
	check: {
		var doc = ~radDocSample.(), text = doc.asLiteral, back, diff;
		back = RadLiteral.read(text);
		diff = RadDoc.diff(doc.data, back);
		(diff.isEmpty and: { back == doc.data }
			and: { back[\strips][\track1][\fx][2][\args][\sum] == (0.1 + 0.2) }
			and: { back[\strips][\track1][\fx][2][\note] == "two\nlines\tand a tab" })
		or: { "differences: " ++ diff.collect{|d| d[\address] ++ " " ++ d[\old].cs ++ " -> " ++ d[\new].cs }.join("; ") }
	}
);

// the layout a person reads: one level per line, short Events on one line,
// strips in mixer order, unit fields in a fixed order
~radTest.("doc-literal-layout",
	check: {
		var doc = RadDoc.new, expected;
		doc.put("track2.fx.1", (args: (freq: 800, rq: 0.5), def: \lpf, kind: \filter, bypass: false));
		doc.put("master.fx.1", (kind: \filter, def: 'band pass', args: ()));
		doc.put("track10.fx.1", (kind: \filter, def: \dl1, args: (time: 0.25)));
		doc.put("bus1.fx.3", (kind: \filter, def: \pch, args: (pitchRatio: 2),
			mods: (pitchRatio: (type: \sin, spec: [0.5, 2], args: (freq: 0.1)))));
		expected = "(
  format: 1,
  strips: (
    track2: (
      fx: (
        1: (kind: \\filter, def: \\lpf, args: (freq: 800, rq: 0.5), bypass: false)
      )
    ),
    track10: (
      fx: (
        1: (kind: \\filter, def: \\dl1, args: (time: 0.25))
      )
    ),
    bus1: (
      fx: (
        3: (
          kind: \\filter,
          def: \\pch,
          args: (pitchRatio: 2),
          mods: (
            pitchRatio: (type: \\sin, spec: [0.5, 2], args: (freq: 0.1))
          )
        )
      )
    ),
    master: (
      fx: (
        1: (kind: \\filter, def: 'band pass', args: ())
      )
    )
  )
)";
		(doc.asLiteral == expected) or: { "got:\n" ++ doc.asLiteral }
	}
);

// write a file with a header, read it back
~radTest.("doc-write-read",
	check: {
		var doc = ~radDocSample.(), path = "/tmp/radtest_doc/doc-write-read.scd", back, text, problems = [];
		File.delete(path);
		doc.write(path);
		text = File.readAllString(path);
		back = RadDoc.read(path);
		if(text.beginsWith("// Radicles document · format 1 · saved ").not, { problems = problems.add("header: " ++ text.keep(50)) });
		if(back.isNil, { problems = problems.add("not read") }, {
			if(RadDoc.diff(doc, back).notEmpty, { problems = problems.add("differences after reading") });
			if(back.isKindOf(RadDoc).not, { problems = problems.add("not a RadDoc") });
		});
		if(RadDoc.read("/tmp/radtest_doc/no-such-file.scd").notNil, { problems = problems.add("missing file not nil") });
		problems.isEmpty or: { problems.join(", ") }
	},
	teardown: { File.delete("/tmp/radtest_doc/doc-write-read.scd") }
);

// the safe reader: anything that is not plain data is refused, and nothing in it runs
~radTest.("doc-safe-reader-refuses",
	check: {
		var bad = [
			"\"touch /tmp/radtest_doc/pwned\".unixCmd",
			"(a: \"touch /tmp/radtest_doc/pwned\".unixCmd)",
			"(a: 1); \"touch /tmp/radtest_doc/pwned\".unixCmd",
			"(a: { \"touch /tmp/radtest_doc/pwned\".unixCmd })",
			"(a: ~radPwned = 1)",
			"(a: ~radPwned)",
			"(a: thisProcess)",
			"(a: currentEnvironment)",
			"(a: Server)",
			"(a: 1 + 2)",
			"(a: [1] ++ [2])",
			"(a: 1.neg)",
			"(a: \\b -> 1)",
			"(a: 1) (b: 2)",
			"(a: \"unclosed)",
			"(a: 1,, b: 2)",
			"[1, 2]",       // not an Event: refused by RadDoc, though RadLiteral reads it
			""
		], read = [];
		~radPwned = nil;
		File.delete("/tmp/radtest_doc/pwned");
		bad.do{|text| if(RadDoc.fromString(text, "test").notNil, { read = read.add(text) }) };
		case
		{ read.notEmpty } { "read: " ++ read.cs }
		{ File.exists("/tmp/radtest_doc/pwned") } { "a file ran a command" }
		{ ~radPwned.notNil } { "a file set an environment variable" }
		{ true }
	}
);

// a refusal says where: the line and what was found
~radTest.("doc-safe-reader-message",
	check: {
		var message = try { RadLiteral.read("(\n  a: 1,\n  b: Server\n)"); "read" } {|err| err.errorString };
		(message.find("line 3").notNil and: { message.find("Server").notNil }) or: { message }
	}
);

// the data forms RadLiteral reads beyond what it writes (hand-edited files)
~radTest.("doc-literal-forms",
	check: {
		var text = "// comment\n/* block /* nested */ */\n(a: 1, 'b c': #[1, -2.5e3, +3], 4: \\sym, -1: inf, d: nan,)\n;";
		var back = try { RadLiteral.read(text) } {|err| err.errorString };
		(back.isKindOf(Event) and: { back[\a] == 1 } and: { back['b c'] == [1, -2500.0, 3] }
			and: { back[4] == \sym } and: { back[-1] == inf } and: { back[\d].isNaN })
		or: { "read: " ++ back.cs }
	}
);

// format numbers: older data is converted one format at a time; newer data is
// read with a warning; data without a format is refused
~radTest.("doc-format-convert",
	check: {
		var saved = RadDoc.converters.copy, out, problems = [];
		RadDoc.converters[1] = {|d| d[\strips] = d.removeAt(\tracks); d };
		RadDoc.converters[2] = {|d| d[\three] = true; d };
		out = RadDoc.convert((format: 1, tracks: (track1: ())), 3);
		RadDoc.converters = saved;
		if(out != (format: 3, strips: (track1: ()), three: true), { problems = problems.add("converted: " ++ out.cs) });
		if(try { RadDoc.convert((format: 1), 2); true } { false }, { problems = problems.add("missing converter not refused") });
		if(RadDoc.fromString("(strips: ())", "test").notNil, { problems = problems.add("no format read") });
		if(RadDoc.fromString("(format: 99, strips: ())", "test").isNil, { problems = problems.add("newer format not read") });
		problems.isEmpty or: { problems.join(", ") }
	}
);

// diff: added, removed and changed, each with its address; tolerance for numbers
~radTest.("doc-diff",
	check: {
		var a = ~radDocSample.(), b = a.copy, diff, expected;
		b.put("track1.fx.2.args.pitchRatio", 0.75);
		b.removeAt("bus1.fx.1");
		b.put("track3.fx.1", (kind: \filter, def: \lpf));
		b.put("master.fx.1.args.freq", 12000.0000001);
		diff = RadDoc.diff(a, b).collect{|d| [d[\change], d[\address], d[\old], d[\new]] };
		expected = [
			[\changed, "track1.fx.2.args.pitchRatio", 0.5, 0.75],
			[\added, "track3", nil, (fx: (1: (kind: \filter, def: \lpf)))],
			[\removed, "bus1.fx.1", (kind: \filter, def: \pch, args: (pitchRatio: 2)), nil],
			[\changed, "master.fx.1.args.freq", 12000, 12000.0000001]
		];
		case
		{ diff != expected } { "diff: " ++ diff.cs }
		{ RadDoc.diff(a, b, 0.001).size != 3 } { "tolerance: " ++ RadDoc.diff(a, b, 0.001).size }
		{ RadDoc.diff(a, a.copy).notEmpty } { "a copy differs" }
		{ RadDoc.diff((a: \x), (a: "x")).size != 1 } { "Symbol and String counted equal" }
		{ RadDoc.diff((a: 1), (a: 1.0)).notEmpty } { "1 and 1.0 counted different" }
		{ true }
	}
);

// what RadLiteral cannot write is refused with the class named, never written half
~radTest.("doc-literal-refuses-objects",
	check: {
		var message = try { RadLiteral.write((a: (b: { 1 }))); "written" } {|err| err.errorString };
		(message.find("Function").notNil) or: { message }
	}
);
