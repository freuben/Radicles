// Presets and libraries: a library's effects can have presets in the library
// (RadiclesLibs/<lib>/Settings/Presets/units/...), read with the performer's
// own; fxsave <effect> <preset> <library> copies one of your presets there.
// Each test makes a small library, RadTestLib, with one effect, and removes it.

~radTestLibPath = { Radicles.libPath +/+ "RadTestLib" };
// a copy of EmptyLib (Radicles expects every library folder to be there),
// with one effect
~radMakeTestLib = {
	var dir = ~radTestLibPath.() +/+ "Files" +/+ "SynthFiles";
	("rm -rf " ++ ~radTestLibPath.().shellQuote ++ "; cp -R " ++
		(Radicles.libPath +/+ "EmptyLib").shellQuote ++ " " ++ ~radTestLibPath.().shellQuote).systemCmd;
	File.use(dir +/+ "Filter.scd", "w", {|f|
		f.write("[ [ 'radtestgain', {arg gain = 1; \\in * gain} ] ]") });
	Radicles.selLibs(["Main", "RadTestLib"]); //loaded, like imp RadTestLib
};
~radLibClean = {
	Radicles.selLibs(["Main"]);
	("rm -rf " ++ ~radTestLibPath.().shellQuote).systemCmd;
	(RadUnit.presetRoot +/+ "filter/*/radtest_*.scd").pathMatch.do{|path| File.delete(path) };
};
~radLibPreset = {|name| ~radTestLibPath.() +/+ "Settings/Presets/units/filter/radtestgain" +/+ name ++ ".scd" };
// a preset file in the library, written as fxsave writes them
~radWriteLibPreset = {|name, gain|
	var path = ~radLibPreset.(name);
	File.mkdir(path.dirname);
	RadDoc((format: 1, type: \unit, unit: (kind: \filter, def: \radtestgain, args: (gain: gain))))
	.write(path, "unit preset · filter radtestgain");
};
~radGain = { Ndef(\filterTrack_1_1).get(\gain) };

~radTest.("library-of-effect",
	setup: ["start 2"],
	code: { ~radMakeTestLib.() },
	check: {
		case
		{ RadUnit.libraryOf(\filter, \radtestgain) != "RadTestLib" } { "radtestgain: " ++ RadUnit.libraryOf(\filter, \radtestgain).cs }
		{ RadUnit.libraryOf(\filter, \pch) != "Main" } { "pch: " ++ RadUnit.libraryOf(\filter, \pch).cs }
		{ RadUnit.libraryOf(\filter, \nosucheffect).notNil } { "an unknown effect has a library" }
		{ true }
	},
	teardown: ~radLibClean
);

// fxsave <effect> <preset> <library> copies your preset into the library
~radTest.("library-publish",
	setup: ["start 2"],
	code: {
		~radMakeTestLib.();
		Radicles.cW.callFunc("fx t 1 radtestgain"); 2.wait;
		Radicles.cW.callFunc("fxset t 1 1 gain 0.5"); 0.5.wait;
		Radicles.cW.callFunc("fxsave t 1 1 radtest_share"); 0.5.wait;
		Radicles.cW.callFunc("fxsave radtestgain radtest_share RadTestLib"); 0.5.wait;
	},
	check: {
		var own = RadUnit.presetPath(\filter, \radtestgain, \radtest_share), lib = ~radLibPreset.("radtest_share");
		case
		{ File.exists(lib).not } { "not in the library" }
		{ File.readAllString(lib) != File.readAllString(own) } { "the library's copy differs" }
		{ true }
	},
	teardown: ~radLibClean
);

// refused, with a reason: another library's effect, no such preset of your
// own, no such library, and replacing without a window to ask in
~radTest.("library-publish-refused",
	setup: ["start 2", "fx t 1 pch", "fxsave t 1 1 radtest_pch"],
	code: {
		~radMakeTestLib.();
		Radicles.cW.callFunc("fxsave pch radtest_pch RadTestLib"); 0.3.wait;
		Radicles.cW.callFunc("fxsave radtestgain radtest_none RadTestLib"); 0.3.wait;
		Radicles.cW.callFunc("fxsave radtestgain radtest_none NoSuchLib"); 0.3.wait;
		Radicles.cW.callFunc("fx t 2 radtestgain"); 2.wait;
		Radicles.cW.callFunc("fxset t 2 1 gain 0.5"); 0.3.wait;
		Radicles.cW.callFunc("fxsave t 2 1 radtest_twice"); 0.3.wait;
		Radicles.cW.callFunc("fxsave radtestgain radtest_twice RadTestLib"); 0.3.wait;
		File.delete(RadUnit.presetPath(\filter, \radtestgain, \radtest_twice));
		Radicles.cW.callFunc("fxset t 2 1 gain 0.75"); 0.3.wait;
		Radicles.cW.callFunc("fxsave t 2 1 radtest_twice"); 0.3.wait;
		Radicles.cW.callFunc("fxsave radtestgain radtest_twice RadTestLib"); 0.3.wait;
	},
	check: {
		case
		{ (~radTestLibPath.() +/+ "Settings/Presets/units/filter/pch").pathMatch.notEmpty } { "pch preset went into RadTestLib" }
		{ File.exists(~radLibPreset.("radtest_none")) } { "a preset that does not exist was copied" }
		{ (RadDoc.readData(File.readAllString(~radLibPreset.("radtest_twice")), "lib") !? {|d| d[\unit][\args][\gain] }) != 0.5 } {
			"the library's preset was replaced" }
		{ true }
	},
	teardown: ~radLibClean
);

// a library's preset loads like your own, and is listed
~radTest.("library-preset-loads",
	setup: ["start 2"],
	code: {
		~radMakeTestLib.();
		~radWriteLibPreset.("radtest_lib", 0.25);
		Radicles.cW.callFunc("fx t 1 radtestgain"); 2.wait;
		Radicles.cW.callFunc("fxload t 1 1 radtest_lib"); 2.wait;
	},
	check: {
		case
		{ ~radGain.() != 0.25 } { "gain " ++ ~radGain.() }
		{ RadUnit.presetSource(\filter, \radtestgain, \radtest_lib) != "RadTestLib" } { "source: " ++ RadUnit.presetSource(\filter, \radtestgain, \radtest_lib).cs }
		{ RadUnit.presetNames(\filter, \radtestgain).includes(\radtest_lib).not } { "not in presetNames" }
		{ Radicles.fxPresetList.any{|p| p == [\radtestgain, \radtest_lib] }.not } { "not listed by fxload" }
		{ true }
	},
	teardown: ~radLibClean
);

// the same name in both: your own wins
~radTest.("library-preset-own-wins",
	setup: ["start 2"],
	code: {
		~radMakeTestLib.();
		~radWriteLibPreset.("radtest_both", 0.75);
		Radicles.cW.callFunc("fx t 1 radtestgain"); 2.wait;
		Radicles.cW.callFunc("fxset t 1 1 gain 0.5"); 0.5.wait;
		Radicles.cW.callFunc("fxsave t 1 1 radtest_both"); 0.5.wait;
		Radicles.cW.callFunc("fxset t 1 1 gain 1"); 0.5.wait;
		Radicles.cW.callFunc("fxload t 1 1 radtest_both"); 2.wait;
	},
	check: {
		(~radGain.() == 0.5 and: { RadUnit.presetSource(\filter, \radtestgain, \radtest_both) == \own })
		or: { "gain " ++ ~radGain.() }
	},
	teardown: ~radLibClean
);

// the PRESET menu marks a library's preset with the library (Mac)
~radTest.("library-preset-menu",
	setup: ["start 2"],
	code: {
		var win, menu;
		~radMakeTestLib.();
		~radWriteLibPreset.("radtest_lib", 0.25);
		Radicles.cW.callFunc("fx t 1 radtestgain"); 2.wait;
		Radicles.aZ.filterGUI(\track, 1, 1); 0.5.wait;
		win = Radicles.aZ.filtersWindow.last;
		menu = win.canvas.children.detect{|v| v.isKindOf(PopUpMenu) };
		~radMenuItems = menu.items;
		menu.valueAction = menu.items.indexOfEqual("radtest_lib · RadTestLib") ? 0;
		2.wait;
	},
	check: {
		case
		{ ~radMenuItems.includesEqual("radtest_lib · RadTestLib").not } { "menu: " ++ ~radMenuItems.cs }
		{ ~radGain.() != 0.25 } { "not loaded: gain " ++ ~radGain.() }
		{ true }
	},
	teardown: {
		(Radicles.aZ !? {|a| a.filtersWindow } ? []).copy.do{|w| w.close };
		~radLibClean.();
	},
	gui: true
);
