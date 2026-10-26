// Radicles test driver.
//
// From the SuperCollider IDE (Mac):
//   ~radTestMode = \check;   // or \update to (re)write snapshots
//   ~radTestCase = nil;      // or e.g. "step1-regressions"
//   ~radTestOnly = nil;      // or one test name
//   (Radicles.mainPath +/+ "Tests/run.scd").load;
// Each test runs in its own fresh sclang with a muted server, so your own
// session is not touched. A summary is posted at the end.
//
// Headless (cloud):  sclang run.scd [check|update] [case] [test]
(
var args, sclang, conf, tests, results, logDir, timeout;
args = thisProcess.argv;
if(args.size > 0, {
	~radTestMode = args[0].asSymbol;
	~radTestCase = args[1];
	~radTestOnly = args[2];
	~radTestExit = true;
});
~radTestMode = ~radTestMode ? \check;
timeout = ~radTestTimeout ? 150;

Radicles.mainPath ?? { Radicles.new };
(Radicles.mainPath +/+ "Tests/RadTest.scd").load;
tests = ~radTestLoadCases.(~radTestCase);
if(~radTestOnly.notNil, { tests = tests.select{|t| t[\name] == ~radTestOnly.asString } });

// The sclang next to this one (works for /usr/local, /opt/sc313 and the Mac app).
sclang = ~radTestSclang ?? {
	Platform.case(
		\osx, { Platform.resourceDir.dirname +/+ "MacOS/sclang" },
		\linux, { Platform.resourceDir.dirname.dirname +/+ "bin/sclang" }
	)
};
conf = ~radTestLangConf ? "RADTEST_CONF".getenv;
logDir = ~radTestResultsDir.();
File.mkdir(logDir);

("RADTEST: " ++ tests.size ++ " tests, mode " ++ ~radTestMode ++ ", " ++ sclang).postln;

fork {
	results = [];
	tests.do{|t, i|
		var resultFile, logFile, cmd, cond, done = false, pid, res, log, errors, t0;
		resultFile = logDir +/+ t[\caseFile] +/+ (t[\name] ++ ".result");
		logFile = logDir +/+ t[\caseFile] +/+ (t[\name] ++ ".log");
		File.mkdir(logDir +/+ t[\caseFile]);
		File.delete(resultFile);
		cmd = [sclang] ++ (conf.notNil.if({["-l", conf]}, {[]})) ++
			[~radTestDir +/+ "child.scd", t[\caseFile], t[\name], ~radTestMode, resultFile];
		cmd = "exec " ++ cmd.collect{|x| x.asString.shellQuote}.join(" ") ++ " > " ++ logFile.shellQuote ++ " 2>&1";
		cond = Condition.new;
		t0 = Main.elapsedTime;
		pid = cmd.unixCmd({ done = true; cond.unhang }, false);
		while({ done.not and: { (Main.elapsedTime - t0) < timeout } }, { 0.5.wait });
		if(done.not, { ("kill -9 " ++ pid).unixCmd(postOutput: false); 1.wait });
		// never leave a test server behind (it would block the next test's port)
		"pkill -f 'scsynt[h] -u 57150'".unixCmd(postOutput: false); 0.5.wait;
		// wait until it has really gone (shared memory is released on exit)
		10.do{
			if("pgrep -f 'scsynt[h] -u 57150'".unixCmdGetStdOut.size > 0, { 0.5.wait });
		};

		res = ~radTestReadFile.(resultFile);
		res = if(res.notNil, { res.interpret }, {
			(name: t[\name], caseFile: t[\caseFile], status: \fail,
				message: done.if({"child exited without a result (see log)"}, {"timed out"}))
		});

		// "It runs": any SuperCollider error after setup started fails the test.
		log = ~radTestReadFile.(logFile) ? "";
		if(log.find("could not initialize audio").notNil or: { log.find("Server 'radtest' exited with exit code 1").notNil }, {
			res[\status] = \error;
			res[\message] = "the test server did not boot (audio not available: on Linux, is JACK running?)";
		});
		if(log.find("RADTEST:SETUP").notNil, {
			errors = log.copyToEnd(log.find("RADTEST:SETUP")).split($\n)
				.select{|l| l.beginsWith("ERROR:") or: { l.beginsWith("FAILURE IN SERVER") } };
			if(errors.notEmpty and: { res[\status] != \skipped }, {
				res[\status] = \fail;
				res[\message] = res[\message] ++ " | error posted: " ++ errors[0];
			});
		});

		if(t[\noWarnings] and: { log.find("RADTEST:BEGIN").notNil } and: { res[\status] == \pass or: { res[\status] == \updated } }, {
			var section = log.copyToEnd(log.find("RADTEST:BEGIN"));
			var warnings;
			section = section.copyFromStart((section.find("RADTEST:CHECKED") ? section.size) - 1);
			warnings = section.split($\n).select{|l| l.beginsWith("WARNING:") };
			if(warnings.notEmpty, { res[\status] = \fail; res[\message] = "warning posted: " ++ warnings[0] });
		});
		if(t[\known].notNil, {
			case
			{ res[\status] == \fail } {
				res[\status] = \known; res[\message] = "known (" ++ t[\known] ++ "): " ++ res[\message] }
			{ res[\status] == \pass or: { res[\status] == \updated } } {
				res[\message] = res[\message] ++ " NOTE: marked known (" ++ t[\known] ++ ") but passed" };
		});
		results = results.add(res);
		("RADTEST [" ++ (i + 1) ++ "/" ++ tests.size ++ "] " ++ res[\status].asString.toUpper
			++ " " ++ t[\caseFile] ++ "/" ++ t[\name]
			++ (res[\message].size > 0).if({" — " ++ res[\message]}, {""})).postln;
	};

	"".postln;
	"RADTEST SUMMARY".postln;
	[\pass, \updated, \fail, \known, \skipped, \error].do{|st|
		var n = results.count{|r| r[\status] == st };
		if(n > 0, { ("  " ++ st ++ ": " ++ n).postln });
	};
	results.select{|r| r[\status] == \fail }.do{|r|
		("  FAILED " ++ r[\caseFile] ++ "/" ++ r[\name] ++ ": " ++ r[\message]).postln;
	};
	"Details: Tests/results/ (logs, and .actual.txt next to failing snapshots)".postln;
	~radTestResults = results;
	if(~radTestExit == true, {
		0.5.wait;
		(results.any{|r| r[\status] == \fail or: { r[\status] == \error } }).if({ 1.exit }, { 0.exit });
	});
};
)
