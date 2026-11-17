// Quick checks that the harness itself works.
~radTest.("vol-track",
	setup: ["start 2"],
	cmds: ["vol t 1 -10"],
	check: { (Ndef('track1').get('volume') == -10) or: { "volume is " ++ Ndef('track1').get('volume') } }
);
~radTest.("fx-pch",
	setup: ["start 2"],
	cmds: ["fx 1 pch"],
	check: { (Radicles.aZ.filters == [['filterTrack_1_1', 'pch']]) or: { "filters: " ++ Radicles.aZ.filters.cs } }
);
// Radicles.mainPath is the folder sclang compiled, so a checkout in another
// folder (a worktree, with RADTEST_CONF) tests its own cases (17 Nov 2026)
~radTest.("main-path",
	cmds: [],
	check: {
		var compiled = LanguageConfig.includePaths.collect(_.withoutTrailingSlash);
		compiled.any{|p| p == Radicles.mainPath.withoutTrailingSlash }
		or: { "mainPath " ++ Radicles.mainPath ++ " is not a compiled folder: " ++ compiled.cs }
	}
);
