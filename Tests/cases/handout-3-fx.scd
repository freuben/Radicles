// Examples from the teaching handout "Using and creating FXs in Radicles" (Jan 2020).

~radTest.("list-commands", setup: ["start 2"], cmds: ["ls"]);

~radTest.("save-load-commands",
	setup: ["start 2"],
	cmds: ["ssave radtest_cmds", "sload radtest_cmds", "sconcat radtest_cmds"],
	teardown: { PresetFile.remove(\session, 'radtest_cmds', false, path: 0, post: false) },
	gui: true
);

// Every effect named in the handout, one after another in track 1, slot 1.
// Checks that each definition still builds and runs.
[
	\cbg, \chr1, \chr2, \dl1, \dl2, \gvrb1, \gvrb2, \gvrb3, \gvrb4, \gvrb5, \convrev1, \fvrb,
	\ctp, \harm, \harm2, \pch, \pch2, \pch3, \pch4, \pch5, \bpf, \eq8, \hpf, \lpf,
	\cmp, \lmt, \amp, \gain, \cut1, \cut2, \cut3, \gate, \ds1, \ds2, \ds3, \ds4, \ds5, \sclip, \rm1, \rm2, \rm3,
	\frz1, \frz2, \mga, \sdf, \srt, \srv, \ssq, \cvi1, \cvi2, \cvt1, \cvt2, \mmi, \mmt
].do{|name|
	var known = (
		convrev1: "convrev1 needs an impulse response, which only the mixer GUI can pass (principle 9)",
		ctp: "ctp indexes a mono signal with [0] (fails on 3.13 and 3.14)"
	)[name];
	~radTest.("main-fx-" ++ name, setup: ["start 2"], cmds: ["fx 1 " ++ name], known: known,
		check: {
			(Radicles.aZ.filters.notNil and: { Radicles.aZ.filters.flop[1].includes(name) })
				or: { "effect not inserted: " ++ Radicles.aZ.filters.cs }
		});
};

// fvrb2 is a 2-channel reverb: it needs a stereo track.
~radTest.("main-fx-fvrb2-stereo", setup: ["start 2", "t add 2"], cmds: ["fx t 3 fvrb2"],
	check: {
		(Radicles.aZ.filters.notNil and: { Radicles.aZ.filters.flop[1].includes('fvrb2') })
			or: { "effect not inserted: " ++ Radicles.aZ.filters.cs }
	});

~radTest.("libraries",
	setup: ["start 2"],
	cmds: ["libs", "imp SpectraLib", "fxs", "imp [MarkLib, SpectraLib]", "imp all", "imp SpectralLib", "imp [Main, MarkLib]"],
	noWarnings: true,
	known: "handout typo: the library is SpectralLib, not SpectraLib"
);
~radTest.("libraries-current",
	setup: ["start 2"],
	cmds: ["libs", "imp SpectralLib", "fxs", "imp [MarkLib, SpectralLib]", "imp all", "imp [Main, MarkLib]"],
	noWarnings: true
);

~radTest.("show-generated-code", setup: ["start 2"], cmds: ["doc"], gui: true);

// Making your own effect: write it to UserLib, import, use, remove.
~radTest.("own-effect",
	setup: ["start 2"],
	code: {
		SynthDefFile.write('filter', 'radtest_myfx', {arg volume = 0 -> [-20, 20];
			\in * volume.dbamp;
		}, "radtest effect");
		1.wait;
		Radicles.cW.callFunc("imp UserLib");
		1.wait;
		Radicles.cW.callFunc("fx 1 radtest_myfx");
		3.wait;
	},
	check: {
		(Radicles.aZ.filters.notNil and: { Radicles.aZ.filters.flop[1].includes('radtest_myfx') })
			or: { "own effect not inserted: " ++ Radicles.aZ.filters.cs }
	},
	// SynthDefFile.remove asks for confirmation in a window, so remove the four
	// entries directly (same steps, no dialog).
	teardown: {
		var lib = Radicles.getLib("UserLib");
		SynthFile.remove(\filter, 'radtest_myfx', false, nil, lib, false);
		SpecFile.remove(\filter, 'radtest_myfx', false, nil, lib, false);
		DescriptionFile.remove(\filter, 'radtest_myfx', false, nil, lib, false);
		ModFile.new(\synthdef, \filter).remove('radtest_myfx', false, nil, lib, false);
	}
);
