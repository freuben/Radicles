// A block preset loaded over time (Document Model, step 2, PR 4; "Time in
// Radicles", 19 Nov 2026): blkload moves a block to one of its presets over
// time, crossfading by default (decided 3 Nov 2026), and a sound file
// carries on from where it is. (These tests were written for blkmorph, which
// blkload replaced; see also load-transition.scd.)

~radMorphClean = {
	(RadUnit.presetRoot +/+ "block/*/radtest_*.scd").pathMatch.do{|path| File.delete(path) };
};
// run func with the posted code not captured (code that holds where a file
// is, which differs from run to run)
~radQuiet = {|func| var capture = Radicles.postCapture; Radicles.postCapture = nil; func.value; Radicles.postCapture = capture };
~radPos = {|block = 1|
	var cond = Condition(false), value;
	Block.position(block, {|v| value = v; cond.test = true; cond.signal });
	cond.wait;
	value
};

// a crossfade: the fade time is the typed time while it runs, then goes back
~radTest.("morph-xfade",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 500", "blksave 1 radtest_hi", "blkset 1 freq 330"],
	cmds: [["blkload 1 radtest_hi 1", 0.5]],
	code: { ~radFadeDuring = Ndef(\block1).fadeTime; 1.5.wait },
	check: {
		case
		{ ~radFadeDuring != 1 } { "fade time during: " ++ ~radFadeDuring }
		{ Ndef(\block1).get(\freq) != 500 } { "freq " ++ Ndef(\block1).get(\freq) }
		{ Ndef(\block1).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\block1).fadeTime }
		{ true }
	},
	teardown: ~radMorphClean
);

// glide: a lag while it runs, then the lag goes back
~radTest.("morph-glide",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 500", "blksave 1 radtest_hi", "blkset 1 freq 330"],
	cmds: [["blkload 1 radtest_hi 1 glide", 3]],
	check: {
		var rates = Ndef(\block1).nodeMap.ratesFor([\freq]);
		case
		{ Ndef(\block1).get(\freq) != 500 } { "freq " ++ Ndef(\block1).get(\freq) }
		{ rates.notNil and: { rates.any(_.notNil) } } { "lag left: " ++ rates.cs }
		{ true }
	},
	teardown: ~radMorphClean
);

// none: at once
~radTest.("morph-none",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 500", "blksave 1 radtest_hi", "blkset 1 freq 330"],
	cmds: [["blkload 1 radtest_hi 4 none", 0.3]],
	check: { (Ndef(\block1).get(\freq) == 500 and: { Ndef(\block1).fadeTime == 0.5 }) or: {
		"freq " ++ Ndef(\block1).get(\freq) ++ ", fade " ++ Ndef(\block1).fadeTime } },
	teardown: ~radMorphClean
);

// default: the definition's own values
~radTest.("morph-default",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 500"],
	cmds: [["blkload 1 default 0.5", 1.5]],
	check: { (Ndef(\block1).get(\freq) == 440) or: { "freq " ++ Ndef(\block1).get(\freq) } }
);

// the block's own fade time when the command gives none: nothing is set or
// put back
~radTest.("morph-own-fade-time",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 500", "blksave 1 radtest_hi", "blkset 1 freq 330", "fade blk 1 1.5"],
	cmds: [["blkload 1 radtest_hi", 0.5]],
	code: { ~radFadeDuring = Ndef(\block1).fadeTime; 1.5.wait },
	check: {
		(~radFadeDuring == 1.5 and: { Ndef(\block1).fadeTime == 1.5 } and: { Ndef(\block1).get(\freq) == 500 })
		or: { "fade time during: " ++ ~radFadeDuring ++ ", after: " ++ Ndef(\block1).fadeTime ++ ", freq " ++ Ndef(\block1).get(\freq) }
	},
	teardown: ~radMorphClean
);

// a load interrupted by another: the times go back to what they were first
~radTest.("morph-interrupted",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 500", "blksave 1 radtest_hi", "blkset 1 freq 200",
		"blksave 1 radtest_lo", "blkset 1 freq 330"],
	cmds: [["blkload 1 radtest_hi 3", 0.5], ["blkload 1 radtest_lo 1", 2]],
	check: {
		(Ndef(\block1).get(\freq) == 200 and: { Ndef(\block1).fadeTime == 0.5 }) or: {
			"freq " ++ Ndef(\block1).get(\freq) ++ ", fade " ++ Ndef(\block1).fadeTime }
	},
	teardown: ~radMorphClean
);

// a modulator in the preset arrives with the load
~radTest.("morph-modulator",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq sin", "modblkset 1 freq [freq, 3]", "blksave 1 radtest_mod",
		"unmodblk 1 freq", "blkset 1 freq 330"],
	cmds: [["blkload 1 radtest_mod 0.5", 1.5]],
	check: {
		var mod = RadUnit.readBlock(\block1) !? {|u| u[\mods] } !? {|m| m[\freq] };
		(mod.notNil and: { mod[\type] == \sin } and: { mod[\args][\freq] == 3 }) or: { "modulator: " ++ mod.cs }
	},
	teardown: ~radMorphClean
);

// already there: nothing happens
~radTest.("morph-already-there",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 500", "blksave 1 radtest_hi"],
	cmds: [["blkload 1 radtest_hi 1", 0.3]],
	check: { (Ndef(\block1).fadeTime == 0.5) or: { "fade time " ++ Ndef(\block1).fadeTime } },
	teardown: ~radMorphClean
);

// a sound file carries on from where it is through a crossfade, and startPos
// goes back to the preset's afterwards
~radTest.("morph-sound-file-keeps-place",
	setup: ["start 2", ["lp 1 marilyn1", 3], "blkset 1 rate 1.5", "blksave 1 radtest_fast", "blkset 1 rate 1"],
	code: {
		~radQuiet.({
			~radBefore = ~radPos.();
			Radicles.cW.callFunc("blkload 1 radtest_fast 1");
			3.wait;
			~radAfter = ~radPos.();
			~radStart = Ndef(\block1).get(\startPos);
		});
	},
	check: {
		var moved = ~radAfter - ~radBefore;
		case
		{ ~radBefore.isNil or: { ~radAfter.isNil } } { "no position" }
		//about 3 s at rate 1 to 1.5 (with the reads): 3 to 5.5, not back near 0
		{ moved < 2.5 or: { moved > 6 } } { "moved " ++ moved.round(0.01) ++ " s (" ++ ~radBefore.round(0.01) ++ " -> " ++ ~radAfter.round(0.01) ++ ")" }
		{ ~radStart != 0 } { "startPos left at " ++ ~radStart }
		{ Ndef(\block1).get(\rate) != 1.5 } { "rate " ++ Ndef(\block1).get(\rate) }
		{ true }
	},
	teardown: ~radMorphClean
);

// ...and through a glide (the lag rebuilds the block, twice)
~radTest.("morph-sound-file-glide-keeps-place",
	setup: ["start 2", ["lp 1 marilyn1", 3], "blkset 1 rate 1.5", "blksave 1 radtest_fast", "blkset 1 rate 1"],
	code: {
		~radQuiet.({
			~radBefore = ~radPos.();
			Radicles.cW.callFunc("blkload 1 radtest_fast 1 glide");
			4.wait;
			~radAfter = ~radPos.();
			~radStart = Ndef(\block1).get(\startPos);
		});
	},
	check: {
		var moved = ~radAfter - ~radBefore;
		case
		{ ~radBefore.isNil or: { ~radAfter.isNil } } { "no position" }
		{ moved < 3.5 or: { moved > 7.5 } } { "moved " ++ moved.round(0.01) ++ " s (" ++ ~radBefore.round(0.01) ++ " -> " ++ ~radAfter.round(0.01) ++ ")" }
		{ ~radStart != 0 } { "startPos left at " ++ ~radStart }
		{ true }
	},
	teardown: ~radMorphClean
);

// how far the file jumps when the crossfade starts the new copy (the same
// rate before and after; time by the server's own clock): within 30 ms
// (RadUnit.placeLead; 2.5 ms in the cloud on 5 Nov)
~radTest.("morph-place-continuous",
	setup: ["start 2", ["lp 1 marilyn1", 3], "blkset 1 mul 0.5", "blksave 1 radtest_q", "blkset 1 mul 1"],
	code: {
		~radQuiet.({
			var clockBus = Bus.control(Server.default, 1), clock, read, c1, c2, p1, p2;
			clock = { Out.kr(clockBus, Sweep.kr(0, 1)) }.play; 0.2.wait;
			read = { var cond = Condition(false), v; clockBus.get({|x| v = x; cond.test = true; cond.signal }); cond.wait; v };
			p1 = ~radPos.(); c1 = read.();
			Radicles.cW.callFunc("blkload 1 radtest_q 1");
			3.wait;
			p2 = ~radPos.(); c2 = read.();
			clock.free; clockBus.free;
			~radJump = (p2 - p1) - (c2 - c1);
		});
	},
	check: { (~radJump.notNil and: { ~radJump.abs < 0.03 }) or: { "the file jumped " ++ (~radJump !? {|j| j.round(0.001) }) ++ " s" } },
	teardown: ~radMorphClean
);

// a preset with another sound file: the block plays the preset's file, a
// crossfade over the typed time (blkmorph refused this), and the block's
// fade time comes back once the fade has ended
~radTest.("morph-other-file-crossfades",
	setup: ["start 2", ["lp 1 drums1", 3], "blksave 1 radtest_drums", ["lp 1 marilyn1", 3]],
	//the wait lets the file load and the block start again before the code
	//reads the fade time; the fade time comes back about 5.3 s after that
	cmds: [["blkload 1 radtest_drums 5", 3]],
	code: { ~radFadeDuring = Ndef(\block1).fadeTime; 5.wait },
	check: {
		var unit = RadUnit.readBlock(\block1);
		case
		{ ~radFadeDuring != 5 } { "fade time during: " ++ ~radFadeDuring }
		{ unit[\buffer][\play] != \drums1 } { "block: " ++ unit.cs }
		{ Ndef(\block1).fadeTime != 0.5 } { "fade time after: " ++ Ndef(\block1).fadeTime }
		{ true }
	},
	teardown: ~radMorphClean
);

// refused: a preset of another definition
~radTest.("morph-other-def-refused",
	setup: ["start 2", "blk 1 sine", "blkset 1 freq 500", "blksave 1 radtest_hi", "blk 2 noise"],
	cmds: [["blkload 2 radtest_hi 1", 0.5]],
	check: { (RadUnit.readBlock(\block2)[\def] == \noise and: { Ndef(\block2).fadeTime == 0.5 }) or: { "block2 changed" } },
	teardown: ~radMorphClean
);
