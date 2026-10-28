// radpost / radpostcont review (28 Oct): list posting, line wrapping, OSC on
// both channels and a GUI window for the continuous channel. GUI windows are
// stood in for by an Event that records what it is given.

~radFakeWindow = { (lines: [], addLine: {|self, x| self[\lines] = self[\lines].add(x.asString) }) };

// radpost(\doln) posts each item of a list on its own line
~radTest.("radpost-doln-list",
	code: {
		~radWin = ~radFakeWindow.();
		Radicles.postWindow = ~radWin;
		['drums', 'voice'].radpost(\doln);
		Radicles.postWindow = nil;
	},
	check: { (~radWin[\lines] == ["drums", "voice"]) or: { "lines: " ++ ~radWin[\lines].cs } }
);

// line wrapping never breaks inside a string or a symbol
~radTest.("lineformat-keeps-strings",
	check: {
		var path = "\"/a/b/Application Support/Record/27_10 0 spaceMaster.wav\"";
		var code = "~buffer0.write(" ++ path ++ ", \"wav\"); Ndef('a').set('b', 1, 'c', 2, 'd', 3);";
		var wrapped = code.lineFormat(20);
		var lines = wrapped.split($\n);
		(wrapped.replace("\n", "") == code)
		and: { lines.any{|l| l.find(path).notNil } }
		and: { lines.size > 1 }
		or: { "wrapped: " ++ wrapped.cs }
	}
);

// OSC: radpost on /code, radpostcont on /codecont; oscCodeCont = false sends /code only
~radTest.("osc-both-channels",
	code: {
		~radOsc = [];
		OSCdef(\radtestCode, {|msg| ~radOsc = ~radOsc.add([msg[0], msg[1].asString]) }, '/code');
		OSCdef(\radtestCodeCont, {|msg| ~radOsc = ~radOsc.add([msg[0], msg[1].asString]) }, '/codecont');
		Radicles.oscForCode("127.0.0.1", NetAddr.langPort);
		"Ndef('a').set('x', 1);".radpost;
		"Ndef('a').set('x', 2);".radpostcont;
		Radicles.oscCodeCont = false;
		"Ndef('a').set('x', 3);".radpostcont;
		Radicles.oscCodeCont = true;
		0.3.wait;
		Radicles.oscCode = nil;
	},
	check: {
		(~radOsc == [['/code', "Ndef('a').set('x', 1);"], ['/codecont', "Ndef('a').set('x', 2);"]])
		or: { "received: " ++ ~radOsc.cs }
	},
	teardown: { OSCdef(\radtestCode).free; OSCdef(\radtestCodeCont).free; Radicles.oscCode = nil }
);

// radpostcont can also show in a GUI window (Radicles.postWindowCont)
~radTest.("radpostcont-gui-window",
	code: {
		~radWin = ~radFakeWindow.();
		Radicles.postWindowCont = ~radWin;
		"Ndef('a').set('x', 4);".radpostcont;
		Radicles.postWindowCont = nil;
	},
	check: { (~radWin[\lines] == ["Ndef('a').set('x', 4);"]) or: { "lines: " ++ ~radWin[\lines].cs } }
);

// .radshow posts on the source's channel without running the code
~radTest.("radshow-by-source",
	code: {
		~radShown = nil;
		"~radShown = 1; // typed".radshow;
		Radicles.withSource(\gui, { "~radShown = 2; // slider".radshow });
	},
	check: { ~radShown.isNil or: { "radshow ran the code: " ++ ~radShown } }
);

// .radlog runs the code and logs it, without displaying it
~radTest.("radlog-runs-and-logs",
	code: {
		var path;
		~radLogged = nil;
		Radicles.codeLog = true;
		path = RadCodeLog.path;
		"~radLogged = 5;".radlog;
		Radicles.codeLog = false;
		~radLogText = File(path, "r").readAllString;
		File.delete(path);
	},
	check: {
		((~radLogged == 5) and: { ~radLogText.find(" code nopost\n~radLogged = 5;").notNil })
		or: { "ran: " ++ ~radLogged ++ ", log: " ++ ~radLogText }
	}
);

// a modulation set from a slider posts on radpostcont; typed, on radpost
~radTest.("modmap-set-by-source",
	setup: ["start 2", "fx 1 pch", "fxset 1 1 1 sin_(freq, 1)"],
	code: {
		ModMap.set(1, \freq, 2);
		Radicles.withSource(\gui, { ModMap.set(1, \freq, 3) });
	},
	check: { Ndef('mod1').get(\freq) == 3 }
);
