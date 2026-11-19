// Block.play called from SC code takes names as Strings or Symbols (6 Nov
// 2026). Before, a String block name found no definition and nothing played
// (the action still ran), and a String buffer name was read as an array of
// buffers, one per letter (an error in BStore.addAll).

~radPlayForm = {|blockName, buffer|
	var cond = Condition(false);
	Block.play(1, blockName, buffer, action: { cond.test = true; cond.signal });
	cond.wait;
	0.3.wait;
};

~radTest.("play-string-names",
	setup: ["start 2"],
	code: { ~radPlayForm.("play", "marilyn1") },
	check: {
		var live = Block.liveBlocks[0];
		case
		{ live.isNil } { "nothing playing" }
		{ live[1] != \play } { "block " ++ live[1].cs }
		{ live[2] != [\play, \audio, \marilyn1] } { "buffer " ++ live[2].cs }
		{ Ndef(\block1).source.isNil } { "no source" }
		{ true }
	}
);

~radTest.("play-string-block-no-buffer",
	setup: ["start 2"],
	code: { ~radPlayForm.("sine") },
	check: {
		var live = Block.liveBlocks[0];
		(live.notNil and: { live[1] == \sine } and: { Ndef(\block1).get(\freq) == 440 }) or: { "live " ++ live.cs }
	}
);

~radTest.("play-symbol-names",
	setup: ["start 2"],
	code: { ~radPlayForm.(\play, \marilyn1) },
	check: {
		var live = Block.liveBlocks[0];
		(live.notNil and: { live[1] == \play } and: { live[2] == [\play, \audio, \marilyn1] }) or: { "live " ++ live.cs }
	}
);

// the same file again after another block (the order the timing script used)
~radTest.("play-string-again",
	setup: ["start 2"],
	code: { ~radPlayForm.("play", "marilyn1"); ~radPlayForm.("sine"); ~radPlayForm.("play", "marilyn1") },
	check: {
		var live = Block.liveBlocks[0];
		(live.notNil and: { live[1] == \play } and: { live[2] == [\play, \audio, \marilyn1] }) or: { "live " ++ live.cs }
	}
);
