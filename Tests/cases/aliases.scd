// User aliases, from the Radicles help file (Radicles.schelp, "->" aliases).
// The alias code is built by editing printed arrays, which SuperCollider 3.14
// prints without inner spaces, so these tests also guard the 3.14 upgrade.
~radTest.("alias-with-argument",
	setup: ["start 2"],
	cmds: ["(fx $n1 pch -> pch $n1)", "pch 1"],
	check: { (Radicles.aZ.filters == [['filterTrack_1_1', 'pch']]) or: { "filters: " ++ Radicles.aZ.filters.cs } }
);
~radTest.("alias-plain",
	setup: ["start 2"],
	cmds: ["(intro = vol t 1 -10)", "intro"],
	check: { (Ndef('track1').get('volume') == -10) or: { "volume is " ++ Ndef('track1').get('volume') } }
);
