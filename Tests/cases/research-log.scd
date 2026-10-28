// Step 3, PR 4: the research code log (RadCodeLog). Off by default; when on,
// it appends every piece of code, with its time and source, to a file in
// RadiclesFiles/Logs. Each test removes the log file it wrote.

~radLogRead = {|p| var f = File(p, "r"), s = f.readAllString; f.close; s };

~radTest.("code-log-off-by-default",
	check: { RadCodeLog.isOn.not and: { Radicles.codeLog.not } }
);

// Typed, GUI and direct calls are recorded with their source and channel;
// code run in the IDE is recorded as ide; the IDE hook is restored when off.
~radTest.("code-log-records-sources",
	setup: ["start 2"],
	code: {
		var hookBefore = thisProcess.interpreter.codeDump;
		Radicles.codeLog = true;
		~radLogPath = RadCodeLog.path;
		Radicles.cW.callFunc("vol t 1 -10");
		Radicles.withSource(\gui, { Radicles.aZ.setVolume(\track, 1, -5) });
		Radicles.aZ.setVolume(\track, 1, -6);
		// what the IDE does after evaluating a line
		thisProcess.interpreter.codeDump.value("Ndef(\\mine).play; // hand-written");
		Radicles.codeLog = false;
		~radLogHookRestored = thisProcess.interpreter.codeDump === hookBefore;
	},
	check: {
		var log = ~radLogRead.(~radLogPath);
		var want = [
			"// Radicles code log",
			" typed radpost\nNdef('track1').set('volume', -10);",
			" gui radpostcont\nNdef('track1').set('volume', -5);",
			" code radpostcont\nNdef('track1').set('volume', -6);",
			" ide ide\nNdef(\\mine).play; // hand-written",
			"// stopped: "
		];
		var missing = want.reject{|w| log.find(w).notNil };
		(missing.isEmpty and: { ~radLogHookRestored } and: { RadCodeLog.isOn.not }
			and: { ~radLogPath.dirname.basename == "Logs" })
		or: { "missing from log: " ++ missing ++ ", hook restored: " ++ ~radLogHookRestored }
	},
	teardown: { ~radLogPath !? { File.delete(~radLogPath) } }
);

// The call window command turns it on and off.
~radTest.("code-log-command",
	setup: ["start 2"],
	cmds: ["codelog on", "vol t 2 -3", "codelog off"],
	code: { ~radLogPath = PathName((Radicles.filesPath +/+ "Logs")).files.last.fullPath },
	check: {
		var log = ~radLogRead.(~radLogPath);
		(log.find(" typed radpost\nNdef('track2').set('volume', -3);").notNil and: { RadCodeLog.isOn.not })
		or: { "log was: " ++ log }
	},
	teardown: { ~radLogPath !? { File.delete(~radLogPath) } }
);
