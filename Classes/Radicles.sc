Radicles {classvar <>mainPath, <>nodeTime=0.08, <server, <>postWin=nil,
	<>postWhere=\ide, <>fadeTime=0.5, <>schedFunc, <>schedDiv=1,
	<bpm, <postDoc, <>lineSize=68, <>logCodeTime=false, <>reducePostControl=false,
	<>ignorePost=false, <>ignorePostcont=false, <>colorCritical, <>colorMeter, <>colorWarning,
	<>colorTrack, <>colorBus, <>colorMaster, <>colorTextField, <>callWin;

	*new {
		colorCritical = Color.new255(211, 14, 14);
		colorMeter = Color.new255(78, 109, 38);
		colorWarning = Color.new255(232, 90, 13);
		colorTrack = Color.new255(58, 162, 175);
		colorBus = Color.new255(132, 124, 10);
		colorMaster = Color.new255(102, 57, 130);
		colorTextField = Color.new255(246, 246, 246);
		^super.new.initRadicles;
	}

	initRadicles {arg doc=false;
		mainPath = (Platform.userExtensionDir ++ "/Radicles/");
		server = Server.default;
	}

	*document {
		postDoc = Document.new("Radicles: " ++ Date.getDate.asString);
	}

	*clock {var clock, tclock;
		clock = "Ndef('metronome').proxyspace.makeTempoClock(1.0)";
		clock.radpost;
		clock.interpret;
		tclock = Ndef('metronome').proxyspace.clock;
		tclock.schedAbs(tclock.beats.ceil, { arg beat, sec; schedFunc.(beat, sec); schedDiv });
	}

	*tempo {arg newBPM;
		var metroCS;
		bpm = newBPM;
		if(bpm.notNil, {
			metroCS = "Ndef('metronome').clock.tempo = " ++ (bpm/60).cs;
			metroCS.radpost;
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

	*specUnmap {arg unmapVal, thisSpec, thisFunc;
		var specOptions, thisResult;
		if(thisFunc.notNil, {
			specOptions= (0..127).linlin(0,127,0,1.0).collect({|item|
				thisFunc.(thisSpec.map(item.round(0.01))) });
			thisResult = specOptions.indexIn(unmapVal).linlin(0,127,0,1);
		}, {
			thisResult = thisSpec.unmap(unmapVal);
		});
		^thisResult;
	}

	*callWindow {arg name;
		name ?? {name = "Call Window";
		callWin = CallWindow.window(name, Window.win4TopRight, postBool: true);
	};
	}

}