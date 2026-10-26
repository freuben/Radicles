// RadTest: helpers shared by the test driver (run.scd) and the per-test child (child.scd).
// Loaded with: (Radicles.mainPath +/+ "Tests/RadTest.scd").load
// Defines environment variables only; nothing here is compiled into the class library.

~radTestDir = PathName(thisProcess.nowExecutingPath).pathOnly;
~radTests = ~radTests ? [];

// Register a test. Used in Tests/cases/*.scd:
//   ~radTest.("vol-track", setup: ["start 2"], cmds: ["vol t 1 -10"],
//       check: { Ndef('track1').get('volume') == -10 });
// setup: commands run first, not snapshotted.
// cmds: commands whose posted code is snapshotted. An entry may be
//   "cmd" or ["cmd", seconds] to wait a fixed time instead of the quiet-wait.
// check: function returning true, or a String explaining the failure.
// gui: true = needs windows; skipped where there is no GUI (cloud).
// cloud: false = needs something the cloud build lacks (MIDI, a plugin that
//   needs FFTW such as Tartini); skipped where there is no GUI, like gui.
// code: optional function run after cmds (for actions with no command,
//   e.g. moving a mixer knob from code); its posts are snapshotted too.
//   It runs inside a Routine, so it may .wait.
// teardown: function that removes anything the test saved (presets, files);
//   runs before setup (in case an earlier run crashed) and after the check.
// noWarnings: true = fail if Radicles posts a WARNING during the test.
// known: a String naming a reported problem (e.g. "issue #39"). The test still
//   runs, but a failure is listed as KNOWN instead of FAIL, and a pass is
//   flagged so the marker can be removed.
~radTest = {|name, setup, cmds, check, gui = false, code, teardown, noWarnings = false, known, note, cloud = true|
	~radTests = ~radTests.add((
		name: name.asString,
		caseFile: ~radTestCurrentCase,
		setup: setup ? [],
		cmds: cmds ? [],
		check: check,
		gui: gui or: { cloud.not },
		code: code,
		teardown: teardown,
		noWarnings: noWarnings,
		known: known,
		note: note
	));
};

// Snapshot text for a list of captured blocks:
// [ (cmd: "vol t 1 -10", radpost: [..], radpostcont: [..]), ... ]
~radTestFormat = {|blocks|
	var str = "";
	blocks.do{|b|
		str = str ++ ">> " ++ b[\cmd] ++ "\n";
		[\radpost, \radpostcont].do{|ch|
			str = str ++ "== " ++ ch ++ " ==\n";
			if(b[ch].isEmpty, {
				str = str ++ "(none)\n";
			}, {
				b[ch].do{|s| str = str ++ ~radTestNormalise.(s) ++ "\n"};
			});
		};
		str = str ++ "\n";
	};
	str;
};

// Remove details that change between runs or machines without meaning anything.
~radTestNormalise = {|s|
	s = s.asString;
	// tabs and trailing spaces
	s = s.replace("\t", "    ");
	s = s.split($\n).collect{|line| line.stripWhiteSpace.isEmpty.if({""}, {
		var end = line.size - 1;
		while({ end >= 0 and: { line[end].isSpace } }, { end = end - 1 });
		line.copyRange(0, end)
	})}.join("\n");
	// Numbers measured from timing (a timed recording's length, for example) vary
	// from run to run in the 3rd decimal, so numbers with 6 or more decimals are
	// rounded to 2 decimals. Typed values (0.5, -10) are never that long.
	s = {|str|
		var out = "", pos = 0;
		str.findRegexp("-?[0-9]+\\.[0-9]{6,}").do{|m|
			out = out ++ str.copyRange(pos, m[0] - 1) ++ m[1].asFloat.round(0.01).asString;
			pos = m[0] + m[1].size;
		};
		out ++ str.copyToEnd(pos)
	}.(s);
	// A timed recording's loop rate is measured from the clock, so it can differ
	// in any decimal between runs: compare it as "measured", not by value.
	s = {|str|
		var out = "", pos = 0, key = "'triggerRate', ";
		str.findRegexp("'triggerRate', -?[0-9.e-]+").do{|m|
			out = out ++ str.copyRange(pos, m[0] - 1) ++ key ++ "<measured>";
			pos = m[0] + m[1].size;
		};
		out ++ str.copyToEnd(pos)
	}.(s);
	// absolute paths to the Radicles folders
	[Radicles.mainPath, Radicles.libPath, Radicles.filesPath].do{|p|
		if(p.notNil, { s = s.replace(p.asString, "<" ++ p.asString.basename ++ ">") });
	};
	s;
};

~radTestSnapshotPath = {|t|
	~radTestDir +/+ "snapshots" +/+ t[\caseFile] +/+ (t[\name] ++ ".txt");
};

~radTestResultsDir = { ~radTestDir +/+ "results" };

~radTestReadFile = {|path|
	var f, str;
	if(File.exists(path), {
		f = File(path, "r"); str = f.readAllString; f.close; str;
	}, { nil });
};

~radTestWriteFile = {|path, str|
	var f;
	File.mkdir(PathName(path).pathOnly);
	f = File(path, "w"); f.write(str); f.close;
};

// Load every case file (registers tests). caseFilter: nil or a case file name.
~radTestLoadCases = {|caseFilter|
	~radTests = [];
	(~radTestDir +/+ "cases/*.scd").pathMatch.sort.do{|path|
		var case = PathName(path).fileNameWithoutExtension;
		if(caseFilter.isNil or: { caseFilter.asString == case }, {
			~radTestCurrentCase = case;
			path.load;
		});
	};
	~radTests;
};
