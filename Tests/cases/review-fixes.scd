// Fixes from the 18 Nov 2026 review.

// the document's strip at an address, values refreshed from the running system
~radStripNow = {|name| RadUnit.refresh(RadDoc.current.copy).at(name) ? () };
// the strips among names whose value key has no modulator, as a String, or true
~radModsOn = {|names, key|
	var missing = names.reject{|name|
		(~radStripNow.(name)[\mods] !? {|mods| mods[key] }).notNil };
	missing.isEmpty or: { "no " ++ key ++ " modulator on " ++ missing.cs }
};

// a range of mixer positions modulates every strip in it, not only the last
// one (vol, pan, trim with a dash range and a modulator). Typed in the call
// window this always worked: there the command runs on the GUI thread, where
// defer runs at once. It went wrong when the command ran on another thread
// (a controller or a scheduled command): the deferred calls ran after the
// loop and all saw the last track. So these run the command from SystemClock.
~radOffGuiThread = {|cmd, wait = 3|
	fork({ Radicles.cW.callFunc(cmd) }, SystemClock);
	wait.wait;
};

~radTest.("range-mod-vol",
	setup: ["start 3"],
	code: { ~radOffGuiThread.("vol 1-3 sin") },
	check: { ~radModsOn.([\track1, \track2, \track3], \volume) }
);

~radTest.("range-mod-pan",
	setup: ["start 3"],
	code: { ~radOffGuiThread.("pan 1-3 sin") },
	check: { ~radModsOn.([\track1, \track2, \track3], \pan) }
);

~radTest.("range-mod-trim",
	setup: ["start 3"],
	code: { ~radOffGuiThread.("trim 1-3 sin") },
	check: { ~radModsOn.([\track1, \track2, \track3], \trim) }
);

// the typed form, as a guard
~radTest.("range-mod-vol-typed",
	setup: ["start 3"],
	cmds: [["vol 1-3 sin", 3]],
	check: { ~radModsOn.([\track1, \track2, \track3], \volume) }
);
