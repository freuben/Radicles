// Session audit (Oct 2026): does `save` + `load` bring a session back?
// Each scenario is a pair of tests, run in separate sclang processes like a
// real save, quit, restart and load:
//   <name>-save : builds a session, saves it with `save`, writes a fingerprint
//   <name>-load : starts fresh, loads it with `load ... run`, compares
// The fingerprint covers mixer layout, every Ndef's control values, effects,
// blocks, buffers, HID mappings, tempo and the call window text. A failing
// load test lists what came back different.
// Run the whole case, in order: Tests/ci/run-headless.sh check session-audit

~radAuditDir = "/tmp/radaudit";
File.mkdir(~radAuditDir);

// A description of the running session, as key -> string.
~radFp = {
	var fp = (), az = Radicles.aZ, ndefs;
	var str = {|x| x.cs.replace(" ", "") };
	if(az.notNil, {
		fp[\tracks] = str.(az.mixTrackNames);
		fp[\channels] = str.(az.mixTrackNdefs.collect{|n| [n.key, n.numChannels] });
		fp[\filters] = str.(az.filters);
		fp[\muteStates] = str.(az.muteStates);
		fp[\soloStates] = str.(try { az.soloStates });
		fp[\recStates] = str.(try { az.recStates });
	}, { fp[\assemblage] = "nil" });
	ndefs = Ndef.dictFor(Server.default).envir;
	// every Ndef that exists, whether it has a source and its control values
	ndefs.keys.asArray.sort.do{|k|
		var n = ndefs[k];
		fp[("ndef_" ++ k).asSymbol] = str.([
			n.source.notNil,
			n.controlKeysValues.collect{|v| if(v.isNumber, { v.round(0.0001) }, { v }) }
		]);
	};
	fp[\liveBlocks] = str.(Block.liveBlocks);
	fp[\bstoreIDs] = str.(try { BStore.bstoreIDs });
	fp[\hidInfo] = str.(try { HIDMap.hidInfoArr.collect{|i| i.keep(5) } });
	fp[\hidCmds] = str.(try { HIDMap.hidCmds });
	fp[\bpm] = str.(try { Radicles.bpm });
	fp[\libs] = str.(try { Radicles.excludeLibs });
	fp[\callWindow] = str.(try { Radicles.cW.text.string });
	fp
};

~radFpSave = {|name|
	var f = File(~radAuditDir +/+ name ++ ".fp", "w");
	f.write(~radFp.().asCompileString); f.close;
	true
};

// Compare with the saved fingerprint: true, or a list of differences.
~radFpCompare = {|name|
	var path = ~radAuditDir +/+ name ++ ".fp", saved, now, diffs = [];
	if(File.exists(path).not, {
		"no fingerprint saved for " ++ name ++ ": run the -save test first"
	}, {
		saved = File.readAllString(path).interpret;
		now = ~radFp.();
		(saved.keys ++ now.keys).asArray.asSet.asArray.sort.do{|k|
			if(saved[k] != now[k], {
				diffs = diffs.add(k ++ "\n      saved:  " ++ (saved[k] ? "(missing)").keep(220)
					++ "\n      loaded: " ++ (now[k] ? "(missing)").keep(220));
			});
		};
		(diffs.isEmpty) or: { diffs.size.asString ++ " differences:\n    " ++ diffs.join("\n    ") }
	})
};

~radAuditPair = {|name, build, loadCmds|
	var preset = ("radaudit_" ++ name);
	// teardown runs before setup (removes the previous run's preset, so `save`
	// does not ask to overwrite) and after the check (keeps it for -load)
	~radTest.(name ++ "-save",
		setup: build ++ [["save " ++ preset, 2]],
		check: { ~radAuditSaved = true; ~radFpSave.(name) },
		teardown: { if(~radAuditSaved != true, {
			try { PresetFile.remove(\radicles, preset.asSymbol, false, path: 0, post: false) }
		}) });
	~radTest.(name ++ "-load",
		setup: loadCmds ? [["load " ++ preset ++ " run", 15]],
		check: { ~radFpCompare.(name) });
};

// 1. Mixer: layout, volumes, pan, trim, mute, solo, a send, bus and master levels
~radAuditPair.("mixer", ["start 3 1", "t add 2", "vol t 1 -10", "pan t 2 0.5", "trim [1,3] 5",
	"mute t 3", "solo t 1", "snd t 1 1 -6", "vol b 1 -3", "vol m -2"]);

// 2. Effects on a track (two slots), a bus and the master, with a setting
~radAuditPair.("fx", ["start 2 1", "fx t 1 pch", "fxset t 1 1 pitchRatio 0.5", "fx t 1 2 dl1",
	"fx b 1 pch", "fx m pch"]);

// 3. Modulation of an effect argument and of a mixer volume
~radAuditPair.("mod", ["start 2 1", "fx t 1 pch", "fxset t 1 1 pitchRatio sin",
	"modfxset 1 1 [freq, 5]", "vol t 2 sin"]);

// 4. Blocks with settings, one playing a sound file
~radAuditPair.("blocks", ["start 3 1", "blk 1 sinekey", "blksetn 1 [key, 5, oct, 2]",
	["pl 2 drums1", 3], "blk 3 sine", "blkset 3 freq 300"]);

// 5. Modulation of a block argument
~radAuditPair.("blockmod", ["start 2", "blk 1 sine", "blkset 1 freq sin",
	"modblkset 1 freq [freq, 5]"]);

// 6. Buffers: a looped sound file and a wavetable block
~radAuditPair.("buffers", ["start 2", ["lp 1 drums1", 3], ["blk 2 osc1 sine1harm6", 3]]);

// 7. Tempo
~radAuditPair.("tempo", ["start 2", "bpm 80"]);

// 8. Loading into a session that is already running (different layout and effects)
~radAuditPair.("over", ["start 2 1", "fx t 1 pch", "vol t 2 -12"],
	["start 3", "fx t 3 dl1", "vol t 1 -20", ["load radaudit_over run", 15]]);

// 9. Sends (on their own, see the mixer scenario)
~radAuditPair.("sends", ["start 2 1", "snd t 1 1 -6", "sndry t 2 1 -3"]);

// 10. A track added after start (stereo)
~radAuditPair.("addtrack", ["start 2", "t add 2", "vol t 3 -5"]);

// 11. Solo on its own
~radAuditPair.("solo", ["start 2", "solo t 1"]);

// 12. Selected libraries
~radAuditPair.("libs", ["start 2", "imp [Main, MarkLib]"]);

