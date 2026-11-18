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
// one (vol, pan, trim with a dash range and a modulator)
~radTest.("range-mod-vol",
	setup: ["start 3"],
	cmds: [["vol 1-3 sin", 3]],
	check: { ~radModsOn.([\track1, \track2, \track3], \volume) }
);

~radTest.("range-mod-pan",
	setup: ["start 3"],
	cmds: [["pan 1-3 sin", 3]],
	check: { ~radModsOn.([\track1, \track2, \track3], \pan) }
);

~radTest.("range-mod-trim",
	setup: ["start 3"],
	cmds: [["trim 1-3 sin", 3]],
	check: { ~radModsOn.([\track1, \track2, \track3], \trim) }
);
