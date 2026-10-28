// Issue #51: commands that arrive while a block is still loading (buffer and
// server sync), before its function goes into the Ndef. Sending two commands
// back to back with no wait hits that window every time.

~radBufIDs = { (BStore.bstoreIDs ? []).collect(_.cs) };

// st while loading cancels the start: nothing goes into the Ndef and the
// buffer it was loading is freed
~radTest.("stop-while-loading",
	setup: ["start 2"],
	code: {
		Radicles.cW.callFunc("pl 1 drums1");
		Radicles.cW.callFunc("st 1");
		3.wait;
	},
	check: {
		(Ndef('block1').source.isNil and: { Block.liveBlocks[0].isNil } and: { ~radBufIDs.().isEmpty })
		or: { "source: " ++ Ndef('block1').source.cs ++ ", live: " ++ Block.liveBlocks[0].cs
			++ ", buffers: " ++ ~radBufIDs.() }
	}
);

// a second pl while the first is still loading wins cleanly: only the second
// goes into the Ndef, and the first one's buffer is freed
~radTest.("second-play-wins-while-loading",
	setup: ["start 2"],
	code: {
		Radicles.cW.callFunc("pl 1 drums1");
		Radicles.cW.callFunc("pl 1 marilyn1");
		4.wait;
	},
	check: {
		var bufs = ~radBufIDs.();
		(Block.liveBlocks[0].notNil and: { bufs.size == 1 } and: { bufs[0].contains("marilyn1") })
		or: { "live: " ++ Block.liveBlocks[0].cs ++ ", buffers: " ++ bufs }
	},
	teardown: { Ndef('block1').source = nil }
);

// after loading, nothing changes: pl then st (waiting in between) plays, then
// stops and frees the buffer as before
~radTest.("stop-after-loading",
	setup: ["start 2"],
	cmds: [["pl 1 drums1", 3], ["st 1", 3]],
	check: {
		(Block.liveBlocks[0].isNil and: { ~radBufIDs.().isEmpty })
		or: { "live: " ++ Block.liveBlocks[0].cs ++ ", buffers: " ++ ~radBufIDs.() }
	}
);
