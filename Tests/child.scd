// Runs ONE test in a fresh sclang process. Started by run.scd, not by hand:
//   sclang child.scd <case> <test> <check|update> <resultFile>
// Boots its own muted server on port 57150 so it never touches a server
// you have running in the IDE.
(
var args, caseName, testName, mode, resultFile, test, blocks, current, lastPost;
var quietWait, runCmd, finish, headless;

args = thisProcess.argv;
#caseName, testName, mode, resultFile = args;
mode = (mode ? "check").asSymbol;
headless = GUI.current.isNil;

finish = {|status, message|
	var res = (name: testName, caseFile: caseName, status: status, message: message ? "");
	var f = File(resultFile, "w");
	f.write(res.asCompileString); f.close;
	("RADTEST:RESULT " ++ status ++ " " ++ (message ? "")).postln;
	fork { Server.default.quit; 1.wait; 0.exit };
};

Server.default = Server(\radtest, NetAddr("127.0.0.1", 57150));
s = Server.default;
s.options.numWireBufs = 512;
s.volume = -90; // muted from the first boot (applied by Volume when the server starts)

Radicles.new;
(Radicles.mainPath +/+ "Tests/RadTest.scd").load;
~radTestLoadCases.(caseName);
test = ~radTests.detect{|t| t[\name] == testName };
if(test.isNil, { finish.(\error, "test not found: " ++ caseName ++ "/" ++ testName) });

if(test.notNil and: { test[\gui] and: headless }, {
	finish.(\skipped, test[\note] ? "needs a GUI");
	test = nil;
});

if(test.notNil, {
	// Same command set as Radicles.start, without opening the call window.
	Radicles.cW = CallWindow.new(nil, Rect(0, 0, 10, 10), 1, 1);
	Radicles.cW.rootDir = (Radicles.filesPath +/+ "Settings/CallWindow/");
	Radicles.cW.loadStartUp;
	// No call window here: a stand-in for its text, which `save` stores in projects.
	Radicles.cW.instVarPut(\text, (string: ""));
	Radicles.selLibs(["Main"]);
	Radicles.loadAssemblageCmds; Radicles.loadMixCmds; Radicles.loadFxCmds;
	Radicles.loadBlkCmds; Radicles.loadBaseCmds;
	Radicles.synthDefsPn;

	lastPost = Main.elapsedTime;
	Radicles.postCapture = {|str, channel|
		lastPost = Main.elapsedTime;
		if(current.notNil, { current[channel] = current[channel].add(str) });
	};

	// Wait until nothing has been posted for `quiet` seconds and the server
	// is not mid-boot; give up after `timeout`.
	quietWait = {|quiet = 1.0, timeout = 40|
		var t0 = Main.elapsedTime;
		lastPost = t0;
		while({
			((Main.elapsedTime - lastPost) < quiet or: { s.serverBooting })
			and: { (Main.elapsedTime - t0) < timeout }
		}, { 0.1.wait });
		if(s.serverRunning, { s.sync });
	};

	runCmd = {|entry, quiet = 1.0|
		var cmd, wait;
		if(entry.isString, { cmd = entry }, { #cmd, wait = entry });
		// report errors (the driver fails the test) but keep going
		try { Radicles.cW.callFunc(cmd) } {|e| e.reportError };
		if(wait.notNil, { wait.wait; lastPost = Main.elapsedTime }, { quietWait.(quiet) });
	};

	fork {
		var result, snapPath, expected, actual;
		try { test[\teardown].value } {};
		// same random choices every run ({[0, 5, 7].choose} in a command)
		thisThread.randSeed = 1234;
		"RADTEST:SETUP".postln;
		// setup waits longer: building the mixer pauses for the server, and a
		// command typed before it is ready fails (seen with start 2, Oct 2026)
		test[\setup].do{|entry| runCmd.(entry, 3.0) };
		if(s.serverRunning, { s.volume = -90 });
		"RADTEST:BEGIN".postln;
		blocks = [];
		test[\cmds].do{|entry|
			current = (cmd: entry.isString.if({entry}, {entry[0]}), radpost: [], radpostcont: []);
			runCmd.(entry);
			blocks = blocks.add(current);
		};
		if(test[\code].notNil, {
			current = (cmd: "<code>", radpost: [], radpostcont: []);
			try { test[\code].value } {|e| e.reportError };
			quietWait.();
			blocks = blocks.add(current);
		});
		current = nil;
		"RADTEST:END".postln;

		result = try { test[\check].value ? true } {|e| "check raised: " ++ e.errorString };
		"RADTEST:CHECKED".postln;
		try { test[\teardown].value } {};
		actual = ~radTestFormat.(blocks);
		snapPath = ~radTestSnapshotPath.(test);
		expected = ~radTestReadFile.(snapPath);

		case
		{ mode == \update } {
			~radTestWriteFile.(snapPath, actual);
			if(result == true, { finish.(\updated) }, { finish.(\fail, "snapshot written, but check failed: " ++ result) });
		}
		{ result != true } {
			~radTestWriteFile.(~radTestResultsDir.() +/+ caseName +/+ (testName ++ ".actual.txt"), actual);
			finish.(\fail, "check failed: " ++ result);
		}
		{ expected.isNil } {
			~radTestWriteFile.(~radTestResultsDir.() +/+ caseName +/+ (testName ++ ".actual.txt"), actual);
			finish.(\fail, "no approved snapshot yet (run in update mode)");
		}
		{ expected != actual } {
			~radTestWriteFile.(~radTestResultsDir.() +/+ caseName +/+ (testName ++ ".actual.txt"), actual);
			finish.(\fail, "generated code differs from snapshot");
		}
		{ finish.(\pass) };
	};
});
)
