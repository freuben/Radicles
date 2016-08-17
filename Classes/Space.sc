Space : MainImprov {var <ndefs, <>objectFile, <numChannels, <inputArr, <arrPan, thisIndex, object;

	*new {arg ndefArr, system=\pan2, panArr, chanNum=2, play=true, fadeTime;
		^super.new.initSpace(ndefArr, system, panArr, chanNum, play, fadeTime);
	}

	initSpace {arg ndefArr, system, panArr, chanNum, play, fadeTime;

		panArr ?? {panArr = Array.panDis(ndefArr.size)};

		objectFile = (mainPath ++ "SynthFiles/Space.scd").loadPath;

		numChannels = chanNum;
		thisIndex = objectFile.flop[0].indexOf(system);
		object = objectFile.flop[1][thisIndex];

		inputArr = ndefArr;
		arrPan = panArr;

		if(inputArr.isNil, {
			"No ndef array specified".warn;
		}, {

			this.define(inputArr, arrPan, play, fadeTime);
		});
	}

	play {
		ndefs.do{|item| item.play};
	}

	define {arg ndefArr, panArr, play=true, fadeTime;
		var key;

		ndefArr ?? {ndefArr = inputArr};
		inputArr = ndefArr;
		arrPan = panArr;

		{
			ndefArr.do{|item, index|
				key = ((\space++index).asSymbol);
				Ndef(key).ar(numChannels);
				fadeTime ?? {fadeTime =Ndef(key).fadeTime};
				Ndef(key).fadeTime = fadeTime;
				ndefs = ndefs.add(Ndef(key));
				Ndef(key, object);
				nodeTime.yield;
				Ndef(key) <<>.in item;
				nodeTime.yield;
				Ndef(key).xset(\pan, arrPan[index], \chanNum, numChannels);
			};
			if(play, {this.play});
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