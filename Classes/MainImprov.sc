Radicles {classvar <>mainPath, <>nodeTime=0.08, <server, <>postWin=nil, <>postWhere=\ide, <>fadeTime=0.5, <>schedFunc, <>schedDiv=1, <bpm, <postDoc;

	*new {
		^super.new.initRadicles;
	}

	initRadicles {arg doc=false;
		mainPath = ("~/Library/Application Support/SuperCollider/" ++
			"Extensions/ModImprov/").standardizePath;
		server = Server.default;
	}

	*document {
		postDoc = Document.new("Radicles: " ++ Date.getDate.asString);
	}

	*startbr {
		if(postDoc.notNil, {
		"(".radpost;
		});
	}

	*endbr {
		if(postDoc.notNil, {
		")".radpost;
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