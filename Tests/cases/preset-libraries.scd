// Libraries for chain and track presets (step 4): csload and tload load the
// libraries their effects, blocks and wavetables come from,
// keeping those already selected (as imp [Main, RadTestLib] would).
// Each test makes a small library, RadTestLib, with one effect and one block,
// and removes it.

~radTestLibPath = { Radicles.libPath +/+ "RadTestLib" };
~radMakeTestLib = {
	var files = ~radTestLibPath.() +/+ "Files";
	("rm -rf " ++ ~radTestLibPath.().shellQuote ++ "; cp -R " ++
		(Radicles.libPath +/+ "EmptyLib").shellQuote ++ " " ++ ~radTestLibPath.().shellQuote).systemCmd;
	File.use(files +/+ "SynthFiles/Filter.scd", "w", {|f| f.write("[ [ 'radtestgain', {arg gain = 1; \\in * gain} ] ]") });
	File.use(files +/+ "SynthDefFiles/Filter.scd", "w", {|f| f.write("[ [ 'radtestgain', {arg gain = 1->[0, 2]; \\in * gain} ] ]") });
	File.use(files +/+ "SpecFiles/Filter.scd", "w", {|f| f.write("[ [ 'radtestgain', [ [ 'gain', [ 0, 2 ] ] ] ] ]") });
	File.use(files +/+ "SynthFiles/Block.scd", "w", {|f| f.write("[ [ 'radtestblk', {arg freq = 220, mul = 0.1; SinOsc.ar(freq, 0, mul)} ] ]") });
	File.use(files +/+ "SynthDefFiles/Block.scd", "w", {|f| f.write("[ [ 'radtestblk', {arg freq = 220->[20, 2000], mul = 0.1->[0, 1]; SinOsc.ar(freq, 0, mul)} ] ]") });
	File.use(files +/+ "SpecFiles/Block.scd", "w", {|f| f.write("[ [ 'radtestblk', [ [ 'freq', [ 20, 2000 ] ], [ 'mul', [ 0, 1 ] ] ] ] ]") });
	Radicles.selLibs(["Main", "RadTestLib"]);
};
~radLibClean = {
	Radicles.selLibs(["Main"]);
	("rm -rf " ++ ~radTestLibPath.().shellQuote).systemCmd;
	['radtest_libchain', 'radtest_libchain2'].do{|name| File.delete(RadUnit.chainPresetPath(name)) };
	['radtest_libtrack', 'radtest_libtrack2', 'radtest_libtrack3'].do{|name| File.delete(RadUnit.trackPresetPath(name)) };
};
~radCmd = {|cmd, wait = 1| Radicles.cW.callFunc(cmd); wait.wait };
~radFxDefs = {|name| (RadUnit.refresh(RadDoc.current.copy).at([name, \fx]) ? ()).collect(_[\def]) };
~radSelected = { RadUnit.selectedLibraries.asSet };

// which libraries a chain needs: Main for pch, RadTestLib for radtestgain
~radTest.("libraries-for-chain",
	setup: ["start 2"],
	code: { ~radMakeTestLib.() },
	check: {
		var got = RadUnit.chainLibraries((fx: (1: (def: \radtestgain), 2: (def: \pch)))).asSet,
		none = RadUnit.chainLibraries((fx: ()));
		case
		{ got != Set["Main", "RadTestLib"] } { "libraries: " ++ got.cs }
		{ none.notEmpty } { "an empty chain needs " ++ none.cs }
		{ true }
	},
	teardown: ~radLibClean
);

// csload of a chain with an effect from a library that isn't loaded loads it
~radTest.("csload-loads-library",
	setup: ["start 2"],
	code: {
		~radMakeTestLib.();
		~radCmd.("fx t 1 radtestgain", 2);
		~radCmd.("fx t 1 2 pch", 2);
		~radCmd.("cssave t 1 radtest_libchain");
		Radicles.selLibs(["Main"]); //as after imp Main
		~radCmd.("csload t 2 radtest_libchain", 6);
	},
	check: {
		case
		{ ~radSelected.().includes("RadTestLib").not } { "selected: " ++ ~radSelected.().cs }
		{ ~radFxDefs.(\track2) != (1: \radtestgain, 2: \pch) } { "track2 fx: " ++ ~radFxDefs.(\track2).cs }
		{ true }
	},
	teardown: ~radLibClean
);

// libraries already selected stay selected; nothing is posted when nothing is missing
~radTest.("csload-keeps-libraries",
	setup: ["start 2"],
	code: {
		~radMakeTestLib.();
		~radCmd.("fx t 1 radtestgain", 2);
		~radCmd.("cssave t 1 radtest_libchain2");
		Radicles.selLibs(["Main", "FedLib"]);
		~radCmd.("csload t 2 radtest_libchain2", 4);
		~radAfterFirst = ~radSelected.();
		~radCmd.("csload t 2 radtest_libchain2", 2);
	},
	check: {
		case
		{ ~radAfterFirst != Set["Main", "FedLib", "RadTestLib"] } { "selected: " ++ ~radAfterFirst.cs }
		{ ~radSelected.() != ~radAfterFirst } { "changed on the second load: " ++ ~radSelected.().cs }
		{ true }
	},
	teardown: ~radLibClean
);

// tload loads the libraries of the chain and of the source
~radTest.("tload-loads-library",
	setup: ["start 2"],
	code: {
		~radMakeTestLib.();
		~radCmd.("blk 1 radtestblk", 2);
		~radCmd.("fx t 1 radtestgain", 2);
		~radCmd.("tsave t 1 radtest_libtrack");
		Radicles.selLibs(["Main"]);
		~radCmd.("tload t 2 radtest_libtrack", 8);
	},
	check: {
		case
		{ ~radSelected.().includes("RadTestLib").not } { "selected: " ++ ~radSelected.().cs }
		{ (RadUnit.readBlock(\block2) ? ())[\def] != \radtestblk } { "block 2: " ++ RadUnit.readBlock(\block2).cs }
		{ ~radFxDefs.(\track2) != (1: \radtestgain) } { "track2 fx: " ++ ~radFxDefs.(\track2).cs }
		{ true }
	},
	teardown: ~radLibClean
);

// with nosrc and nofx, the libraries of the parts left out aren't loaded
~radTest.("tload-nosrc-nofx-libraries",
	setup: ["start 2"],
	code: {
		~radMakeTestLib.();
		~radCmd.("blk 1 radtestblk", 2);
		~radCmd.("fx t 1 radtestgain", 2);
		~radCmd.("tsave t 1 radtest_libtrack2");
		Radicles.selLibs(["Main"]);
		~radCmd.("tload t 2 radtest_libtrack2 nosrc nofx", 4);
	},
	check: {
		~radSelected.().includes("RadTestLib").not or: { "loaded: " ++ ~radSelected.().cs }
	},
	teardown: ~radLibClean
);

// with a typed time too
~radTest.("timed-loads-load-library",
	setup: ["start 3"],
	code: {
		~radMakeTestLib.();
		~radCmd.("blk 1 radtestblk", 2);
		~radCmd.("fx t 1 radtestgain", 2);
		~radCmd.("tsave t 1 radtest_libtrack3");
		~radCmd.("cssave t 1 radtest_libchain");
		Radicles.selLibs(["Main"]);
		~radCmd.("tload t 2 radtest_libtrack3 1", 4);
		~radTmorph = ~radSelected.();
		Radicles.selLibs(["Main"]);
		~radCmd.("csload t 3 radtest_libchain 1", 4);
	},
	check: {
		case
		{ ~radTmorph.includes("RadTestLib").not } { "tload: " ++ ~radTmorph.cs }
		{ ~radSelected.().includes("RadTestLib").not } { "csload: " ++ ~radSelected.().cs }
		{ (RadUnit.readBlock(\block2) ? ())[\def] != \radtestblk } { "block 2: " ++ RadUnit.readBlock(\block2).cs }
		{ ~radFxDefs.(\track3) != (1: \radtestgain) } { "track3 fx: " ++ ~radFxDefs.(\track3).cs }
		{ true }
	},
	teardown: ~radLibClean
);
