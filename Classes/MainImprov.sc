MainImprov {classvar <>mainPath, <>nodeTime=0.08, <server, <>postWin=nil, <>postWhere=\ide, <>fadeTime=0.5;

*new {
		^super.new.initMainImprov;
	}

	initMainImprov {var clock;
		mainPath = ("~/Library/Application Support/SuperCollider/" ++
			"Extensions/ModImprov/").standardizePath;
		server = Server.default;
		clock = "Ndef('metronome').proxyspace.makeTempoClock(1.0)";
		clock.postln;
		clock.interpret;
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

}