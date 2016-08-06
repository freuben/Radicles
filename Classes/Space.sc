Space : MainImprov {var <ndef, <>objectFile, <numChannels, <inputArr, <arrPan, thisIndex, object;

	*new {arg ndefArr, system=\pan2, panArr, chanNum, fadeTime;
		^super.new.initSpace(ndefArr, system, panArr, chanNum, fadeTime);
	}

	initSpace {arg ndefArr, system, panArr, chanNum, fadeTime;
		var autoStart;

		objectFile = [
			[\pan2, {arg numChan; numChan ?? {numChan = 2}},
				{arg inputArr, panArr, chanNum, vol=1;
					panArr ?? {panArr = Array.panDis(inputArr.size);};
					{var signal, sigArr;
						inputArr.do({|item, index|
							var sig;
							sig = (\in++index).asSymbol.ar([0]);
							sigArr = sigArr.add(Pan2.ar(sig, panArr[index]);)
						});
						signal = (sigArr.sum.flat * vol);};
				};
			],
			[\panB, {arg numChan; numChan ?? {numChan = 4}},
				{arg inputArr, panArr, chanNum, vol=1;
					var a, b;
					panArr ?? {
						a = Array.interpolation(inputArr.size,-0.5,0.5).clump(inputArr.size/2);
						a.do({|item| b = b.add(item.reverse) });
						panArr = [Array.panDis(inputArr.size, inputArr.size)*pi, b.flat * pi];
					};
					{var signal, sigArr;
						inputArr.do({|item, index|
							var  w, x, y, z, sig;
							sig = (\in++index).asSymbol.ar([0]);
							#w, x, y, z = PanB.ar(sig, panArr[0][index], panArr[1][index]);
							sigArr = sigArr.add(DecodeB2.ar(chanNum, w, x, y);)
						});
						signal = (sigArr.sum * vol);};
				};
			],
			[\split, {arg numChan; numChan ?? {numChan = 8}},
				{arg inputArr, panArr, chanNum, vol=1;
					panArr ?? {panArr = (0,1..(chanNum-1)); };
					{arg vol=1;
						var signal, sigArr, sig;
						inputArr.do({|item, index|
							sig = (\in++index).asSymbol.ar([0]);
							sigArr = sigArr.add(Out.ar(panArr[index], sig));
						});
						signal = (sigArr.sum * vol);
					};
				}
			],
		];

		thisIndex = objectFile.flop[0].indexOf(system);
		numChannels = objectFile.flop[1][thisIndex].value(chanNum);
		object = objectFile.flop[2][thisIndex];

		inputArr = ndefArr;
		arrPan = panArr;

		if(inputArr.isNil, {autoStart = false;}, {autoStart = true;});

		ndef = Ndef(\space);
		ndef.ar(numChannels);

		fadeTime ?? {fadeTime = Ndef(\space).fadeTime};
		Ndef(\space).fadeTime = fadeTime;

		if(autoStart, {this.set(inputArr, arrPan)});

	}

	set {arg ndefArr, panArr;

		ndefArr ?? {ndefArr = inputArr};

		inputArr = ndefArr;

		{
			Ndef(\space, object.value(inputArr, panArr, numChannels));
			0.1.yield;
			inputArr.do{|item, index|
				("Ndef('space') <<>.in" ++ index.asString ++ " " ++ item.cs).interpret;
				0.1.yield;
			};
		}.fork;

	}

	reset {arg ndefArr, system=\pan2, panArr, chanNum, playNdef=true;
		var fadeTime;
		fadeTime = Ndef(\space).fadeTime;
		{
			Ndef(\space).clear(fadeTime);
			fadeTime.yield;
			0.1.yield;
			this.initSpace(ndefArr, system, panArr, chanNum, fadeTime);
			if(playNdef, {Ndef(\space).play});
		}.fork;
	}

	clear {
		Ndef(\space).clear
	}

}