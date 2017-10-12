MainImprov {classvar <>mainPath, <>nodeTime=0.08, <server, <>postWin=nil, <>postWhere=\ide, <>fadeTime=0.5, <>schedFunc, <>schedDiv=1;

	*new {
		^super.new.initMainImprov;
	}

	initMainImprov {var clock, tclock;
		mainPath = ("~/Library/Application Support/SuperCollider/" ++
			"Extensions/ModImprov/").standardizePath;
		server = Server.default;
		clock = "Ndef('metronome').proxyspace.makeTempoClock(1.0)";
		clock.postln;
		clock.interpret;
		tclock = Ndef('metronome').proxyspace.clock;
		tclock.schedAbs(tclock.beats.ceil, { arg beat, sec; schedFunc.(beat, sec); schedDiv });
	}

	*tempo {arg bpm;
		var metroCS;
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