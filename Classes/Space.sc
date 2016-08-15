Space : MainImprov {var <ndef, <>objectFile, <numChannels, <inputArr, <arrPan, thisIndex, object;

	*new {arg ndefArr, system=\pan2, panArr, chanNum, fadeTime;
		^super.new.initSpace(ndefArr, system, panArr, chanNum, fadeTime);
	}

	initSpace {arg ndefArr, system, panArr, chanNum, fadeTime;
		var autoStart;

		panArr ?? {panArr = Array.panDis(ndefArr.size)};
		chanNum ?? {chanNum = 2};

		objectFile = (mainPath ++ "SynthFiles/Space.scd").loadPath;

		numChannels = chanNum;
		thisIndex = objectFile.flop[0].indexOf(system);
		object = objectFile.flop[1][thisIndex];

		inputArr = ndefArr;
		arrPan = panArr;

		if(inputArr.isNil, {autoStart = false;}, {autoStart = true;});

		ndef = Ndef(\space);
		ndef.ar(numChannels.postln);

		fadeTime ?? {fadeTime = Ndef(\space).fadeTime};
		Ndef(\space).fadeTime = fadeTime;

		if(autoStart, {this.set(inputArr, arrPan)});

	}

	set {arg ndefArr, panArr;

		ndefArr ?? {ndefArr = inputArr};
		inputArr = ndefArr;
		arrPan = panArr;

		{
			Ndef(\space, object.value(inputArr.postln, panArr.postln, numChannels.postln));
			nodeTime.yield;
			inputArr.do{|item, index|
				("Ndef('space') <<>.in" ++ index.asString ++ " " ++ item.cs).interpret;
			nodeTime.yield;
			};
		}.fork;

	}

	reset {arg ndefArr, system=\pan2, panArr, chanNum, playNdef=true;
		var fadeTime;
		fadeTime = Ndef(\space).fadeTime;
		{
			Ndef(\space).clear(fadeTime);
			fadeTime.yield;
			nodeTime.yield;
			this.initSpace(ndefArr, system, panArr, chanNum, fadeTime);
			if(playNdef, {Ndef(\space).play});
		}.fork;
	}

	clear {
		Ndef(\space).clear
	}

}