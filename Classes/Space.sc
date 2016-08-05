Space {var <ndefArr, <system, <chanNum, <panArr, <ndef, <>fadeTime;

	*new {arg ndefArr, system=\pan2, chanNum, panArr, fadeTime=1;
		^super.new.initSpace(ndefArr, system, chanNum, panArr, fadeTime);
	}

	initSpace {arg arrNdef, sysType, numChan, arrPan, timeFade;
		var autoStart;
		if(arrNdef.isNil, {autoStart = false;}, {autoStart = true;});

		ndefArr = arrNdef;
		system = sysType;
		panArr = arrPan;
		fadeTime = timeFade;

		case
		{[\pan2].includes(system)} {
			numChan = 2;
			ndef = Ndef(\space);
			ndef.ar(numChan);
		}
		{[\panB].includes(system)} {
			numChan ?? {numChan = 4};
			ndef = Ndef(\space);
			ndef.ar(numChan);
		}
		{system == \split} {
			numChan ?? {numChan = 8};
			ndef = Ndef(\space);
			ndef.ar(numChan);
		};

		Ndef(\space).fadeTime = fadeTime;

		chanNum = numChan;

		if(autoStart, {this.set(ndefArr, panArr)});
	}

	set {arg arrayOut, arrPan;
<<<<<<< HEAD
		var object, testArr;

		testArr = [
	[\pan2, {arg vol=1, inputs, pan, chan;
		var signal, sigArr;
		pan ?? {pan = Array.panDis(inputs.size);};
		inputs.do({|item, index|
			var sig;
			sig = (\in++index).asSymbol.ar([0]);
			sigArr = sigArr.add(Pan2.ar(sig, pan[index]);)
		});
		signal = (sigArr.sum.flat * vol);}],
	[\panB, {arg vol=1, inputs, pan, chan;
		var a, b, signal, sigArr;
		pan ?? {
			a = Array.interpolation(inputs.size,-0.5,0.5).clump(inputs.size/2);
			a.do({|item| b = b.add(item.reverse) });
			pan = [Array.panDis(inputs.size, inputs.size)*pi, b.flat * pi];
		};
				pan.postln;
		inputs.do({|item, index|
			var  w, x, y, z, sig;
			sig = (\in++index).asSymbol.ar([0]);
			#w, x, y, z = PanB.ar(sig, pan[0][index], pan[1][index]);
			sigArr = sigArr.add(DecodeB2.ar(chan, w, x, y);)
		});
		signal = (sigArr.sum * vol);}],
	[\split, {arg vol=1, inputs, pan, chan;
				var signal, sigArr;
		pan ?? {pan = (0,1..chan)};
				inputs.do({|item, index|
			var sig;
					sig = (\in++index).asSymbol.ar([0]);
					sigArr = sigArr.add(Out.ar(pan[index], sig));
				});
				signal = (sigArr.sum * vol);
			}],
];
=======
>>>>>>> spaceexperiment2

		arrayOut ?? {arrayOut = ndefArr};

		ndefArr = arrayOut;

<<<<<<< HEAD
		object = testArr.flop[1][testArr.flop[0].indexOf(system)];

		object.value(arrayOut, arrPan, chanNum);
/*		{*/
		// Ndef(\space, object).set(\inputs, arrayOut, \pan, arrPan, \chan, chanNum);
		// 0.1.yield;
		// arrayOut.do{|item, index|
		// 	("Ndef('space') <<>.in" ++ index.asString ++ " " ++ item.cs).interpret;
		// };
		// }.fork;

		object.cs.postln;
		// panArr = arrPan;
=======
		case
		{system == \pan2} {
			Ndef(\space, {arg vol=1;
				var signal, sigArr;
				arrPan ?? {arrPan = Array.panDis(arrayOut.size);
					panArr = arrPan;};
				arrayOut.do({|item, index|
					var sig;
					sig = (\in++index).asSymbol.ar([0]);
					sigArr = sigArr.add(Pan2.ar(sig, arrPan[index]);)
				});
				signal = (sigArr.sum.flat * vol);});
		}
		{system == \panB} {
			Ndef(\space, {arg vol=1;
				var a, b, signal, sigArr;
				arrPan ?? {
					a = Array.interpolation(arrayOut.size,-0.5,0.5).clump(arrayOut.size/2);
					a.do({|item| b = b.add(item.reverse) });
					arrPan = [Array.panDis(arrayOut.size, arrayOut.size)*pi, b.flat * pi];
					panArr = arrPan;};
				arrayOut.do({|item, index|
					var  w, x, y, z, sig;
					sig = (\in++index).asSymbol.ar([0]);
					#w, x, y, z = PanB.ar(sig, arrPan[0][index], arrPan[1][index]);
					sigArr = sigArr.add(DecodeB2.ar(chanNum, w, x, y);)
				});
				signal = (sigArr.sum * vol);});
		}
		{system == \split} {
			Ndef(\space, {arg vol=1;
				var signal, sigArr, sig;
				arrPan ?? {arrPan = (0,1..chanNum); panArr = arrPan;};
				arrayOut.do({|item, index|
					sig = (\in++index).asSymbol.ar([0]);
					sigArr = sigArr.add(Out.ar(arrPan[index], sig));
				});
				signal = (sigArr.sum * vol);
			});
		};

		{
		0.1.yield;
			arrayOut.do{|item, index|
				("Ndef('space') <<>.in" ++ index.asString ++ " " ++ item.cs).interpret;
			};
		}.fork;
>>>>>>> spaceexperiment2

	}

	reset {arg ndefArr, system=\pan2, chanNum, panArr, playNdef=true;
		{
			Ndef(\space).clear(fadeTime);
			fadeTime.yield;
			0.1.yield;
			this.initSpace(ndefArr, system, chanNum, panArr, fadeTime);
			if(playNdef, {Ndef(\space).play});
		}.fork;
	}

	clear {
		Ndef(\space).clear
	}

}