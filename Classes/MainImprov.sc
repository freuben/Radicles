MainImprov {classvar <>mainPath, <>nodeTime=0.08, <server, <>postWin=nil, <>postWhere=\ide, <>fadeTime=0.5, <>schedFunc, <>schedDiv=1, <bpm, <postDoc, <>posttime, <>postdev=0.01;

	*new {arg doc=true;
		^super.new.initMainImprov(doc);
	}

	initMainImprov {arg doc=true;
		mainPath = ("~/Library/Application Support/SuperCollider/" ++
			"Extensions/ModImprov/").standardizePath;
		server = Server.default;
		if(doc, {
			postDoc = Document.new("Radicles: " ++ Date.getDate.asString);
		});
	}

	*document {
		postDoc = Document.new("Radicles: " ++ Date.getDate.asString);
	}

	*rpost {arg string;
		var type;
		if(posttime.isNil, {
			type = \post;
		}, {
			type = \fork;
		});
		if(postDoc.notNil, {
			string.radpost(type, postDoc);
		}, {
			string.radpost(type);
		});
	}

	*rpostln {arg string;
			var type;
			if(posttime.isNil, {
			type = \ln;
		}, {
			type = \forkln;
		});
		if(postDoc.notNil, {
			string.radpost(\ln, postDoc);
		}, {
			string.radpost(\ln);
		});
	}

		*rpostbr {arg string;
		var type;
			if(posttime.isNil, {
			type = \br;
		}, {
			type = \forkbr;
		});
		if(postDoc.notNil, {
			string.radpost(\br, postDoc);
		}, {
			string.radpost(\br);
		});
	}

	*startbr {
		if(postDoc.notNil, {
		this.rpostln("(");
		});
	}

	*endbr {
		if(postDoc.notNil, {
		this.rpostln(")");
		postDoc.selectRange(postDoc.text.size-1, 0);
		});
	}

	*clock {var clock, tclock;
		clock = "Ndef('metronome').proxyspace.makeTempoClock(1.0)";
		clock.postln;
		clock.interpret;
		tclock = Ndef('metronome').proxyspace.clock;
		tclock.schedAbs(tclock.beats.ceil, { arg beat, sec; schedFunc.(beat, sec); schedDiv });
	}

	*tempo {arg newBPM;
		var metroCS;
		bpm = newBPM;
		if(bpm.notNil, {
			metroCS = "Ndef('metronome').clock.tempo = " ++ (bpm/60).cs;
			metroCS.postln;
			metroCS.interpret;
		}, {
			("BPM = " ++ ((Ndef('metronome').clock.tempo*60).round(0.01)).cs).postln;
		});
	}

	*schedAction {arg func;
		schedFunc = { func.(); func=nil};
	}

	*schedCount {arg func, min=1, max=100, loop=false;
		var count;
		count = min;
		schedFunc = { |a,b| if(a.isNumber, {
			if(loop, { func.(count.wrap(min,max)); count = count + 1}, {
				if(count <= max, {
					func.(count); count = count + 1
				});
			});
		}); };
	}

}