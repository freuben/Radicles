// Examples from the teaching handout "Blocks, Modulation & Machine Listening"
// (Feb 2020), in handout order.

~radTest.("list-blocks", setup: ["start 2"], cmds: ["blks"]);

~radTest.("block-sine", setup: ["start 2"], cmds: ["blk 1 sine"]);

~radTest.("block-sinekey-set",
	setup: ["start 2"],
	cmds: ["blk 1 sinekey", "blkset 1", "blkset 1 key 5", "blkset 1 1 0", "st 1"]
);

~radTest.("block-start-with-params",
	setup: ["start 2"],
	cmds: ["blk 1 sinekey [key, 18, oct, 0.5]"]
);

~radTest.("block-set-several-and-random",
	setup: ["start 2", "blk 1 sinekey"],
	cmds: ["blksetn 1 [key, 5, oct, 1]", "blkset 1 key {[0, 5, 7].choose}",
		"blksetn 1 [key, {[0, 5, 7].choose}, oct, {rrand(0.25,4)}]"],
	note: "random choices repeat exactly: each test seeds the random generator"
);

~radTest.("block-crossfade",
	setup: ["start 2", "blk 1 sinekey"],
	cmds: ["blkxset 1 key 7", "blkxsetn 1 [key, 5, oct, 2]"]
);

~radTest.("block-lags",
	setup: ["start 2", "blk 1 sinekey"],
	cmds: ["blklag 1 oct 4", "blkset 1 oct 0.5", "blkset 1 oct 2", "blklagn 1 [oct, 3, mul, 4]", "blksetn 1 [oct, 2, mul, 0.8]"]
);

// Writing your own blocks (to UserLib), then removing them again without a dialog.
~radTestRemoveUserBlock = {|key|
	var lib = Radicles.getLib("UserLib");
	SynthFile.remove(\block, key, false, nil, lib, false);
	SpecFile.remove(\block, key, false, nil, lib, false);
	DescriptionFile.remove(\block, key, false, nil, lib, false);
	ModFile.new(\synthdef, \block).remove(key, false, nil, lib, false);
};

~radTest.("own-block",
	setup: ["start 2"],
	code: {
		SynthDefFile.write('block', 'radtest_myblock', {arg freq = 200 -> [20, 20000, \exp];
			SinOsc.ar(freq);
		}, "radtest block");
		1.wait;
		Radicles.cW.callFunc("imp UserLib");
		1.wait;
		Radicles.cW.callFunc("blk 1 radtest_myblock");
		3.wait;
	},
	teardown: { ~radTestRemoveUserBlock.('radtest_myblock') }
);

~radTest.("own-envelope-block",
	setup: ["start 2"],
	code: {
		SynthDefFile.write('block', 'radtest_myblock2', {arg freq = 200 -> [20, 20000, \exp],
			trig = 1 -> [0, 1, {|val| val.round(1)}], attack = 0.01 -> [0.0001, 0.5],
			decay = 0.3 -> [0.001, 0.4], sustain = 0.5 -> [0.1, 1], release = 1 -> [0.01, 3];
			SinOsc.ar(freq) * Env.adsr(attack, decay, sustain, release).kr(gate: trig);
		}, "radtest envelope block");
		1.wait;
		Radicles.cW.callFunc("imp UserLib");
		1.wait;
		Radicles.cW.callFunc("blk 1 radtest_myblock2");
		3.wait;
		Radicles.cW.callFunc("blkset 1 trig 0");
		1.wait;
		Radicles.cW.callFunc("blkset 1 trig 1");
		1.wait;
	},
	teardown: { ~radTestRemoveUserBlock.('radtest_myblock2') }
);

// Modulation
~radTest.("list-mods", setup: ["start 2"], cmds: ["mods"]);

~radTest.("modulate-block",
	setup: ["start 2", "blk 1 sine"],
	cmds: ["blkset 1", "blkset 1 1 lfn1", "blkset 1 1 sin_(freq, 2)", "modblkset 1",
		"modblkset 1 freq [freq, 5]", "unmodblk 1 freq"],
	check: { ModMap.modNodes.isNil or: { ModMap.modNodes.any{|m| m[1] == Ndef('block1') }.not }
		or: { "block 1 still modulated" } }
);

~radTest.("modulate-effect",
	known: "unmodfx 1 1 (mixer position, argument) is commented out in Radicles.sc; current form is unmodfx 1 1 1",
	setup: ["start 2", "lp 1 marilyn1"],
	cmds: ["fx 1 pch", "fxset 1 1", "fxset 1 1 1 sin_(freq, 1)", "fxset 1 1 2 lfn2_(freq, 1)",
		"modfxset 1 1", "modfxset 1 1 [freq, 5]", "unmodfx 1 1", "unmodfx 1 2"],
	check: { ModMap.modNodes.isNil or: { ModMap.modNodes.any{|m| m[1] == Ndef('filterTrack_1_1') }.not }
		or: { "effect still modulated" } }
);

~radTest.("modulate-effect-current",
	setup: ["start 2", "lp 1 marilyn1"],
	cmds: ["fx 1 pch", "fxset 1 1 1 sin_(freq, 1)", "fxset 1 1 2 lfn2_(freq, 1)",
		"unmodfx 1 1 1", "unmodfx 1 1 2"],
	check: { ModMap.modNodes.isNil or: { ModMap.modNodes.any{|m| m[1] == Ndef('filterTrack_1_1') }.not }
		or: { "effect still modulated" } }
);

// MIDI and HIDs (no controller needed for mapping and presets)
~radTest.("midi-start-and-post", setup: ["start 2"], cmds: ["midi", "midicc", "midiccst"], cloud: false,
	note: "needs MIDI, not available in the headless build");

~radTest.("map-vol-to-midi",
	setup: ["start 8"],
	cmds: ["vol 1 midicc_(19)", "vol 2 midicc_(23)", "vol 3 midicc_(27)", "vol 4 midicc_(31)",
		"vol 5 midicc_(49)", "vol 6 midicc_(53)", "vol 7 midicc_(57)", "vol 8 midicc_(61)",
		"hidsave radtest_volmidimix"],
	teardown: { PresetFile.remove(\dstore, 'radtest_volmidimix', false, path: 0, post: false) }
);

// Machine listening: the onset and pitch examples, then every listening
// modulator named in the handout, each on the frequency of a sinenv block.
~radTest.("onset-and-pitch",
	setup: ["start 2", "blk 1 sinenv"],
	cmds: ["blkset 1 trig ons", "modblkset 1 trig [thresh, 2]", "blkset 1 freq pch",
		"modblkset 1 trig [thresh, 0.2]", "blkset 1 trig ons_(in, 2)", "blkset 1 freq pch_(in, 2)",
		"blkset 1 trig onst", "blkset 1 trig onst_(track, 3)"]
);

[
	\ons, \ons2, \ons3, \ons4, \pch, \hpch, \pch2, \hpch2, \loud, \loudt, \onst, \ons2t, \ons3t, \ons4t,
	\pcht, \hpcht, \pch2t, \hpch2t, \rms, \amp, \rmst, \ampt, \diss, \disst, \cent, \centt, \flat, \flatt,
	\pcile, \pcilet, \entr, \entrt, \mel1, \mel1t, \mel13, \mel13t, \pwr, \pwrt, \flux, \fluxt,
	\slope, \slopet, \peak, \peakt, \crest, \crestt, \key, \keyt
].do{|name|
	var tartini = [\pch2, \hpch2, \pch2t, \hpch2t].includes(name);
	~radTest.("listening-mod-" ++ name, setup: ["start 2", "blk 1 sinenv"], cmds: ["blkset 1 freq " ++ name],
		cloud: tartini.not, note: tartini.if({"uses Tartini (sc3-plugins PitchDetection), not in the cloud build"}),
		known: (name == \mel13t).if({"mel2t-mel13t: Ndef(\\trackN).ar is a 1-element array, so MFCC.kr(chain)[n] is nil (fix: .flat[n]); mel1t returns all 13 coefficients"}));
};
