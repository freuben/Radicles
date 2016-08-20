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

		ndefs = [];

		{
			ndefArr.do{|item, index|
				key = ((\space++index).asSymbol);
				Ndef(key).ar(numChannels);
				fadeTime ?? {fadeTime =Ndef(key).fadeTime};
				Ndef(key).fadeTime = fadeTime;
				ndefs = ndefs.add(Ndef(key));
				if(arrPan.rank == 1, {
					Ndef(key).put(0, object, extraArgs: [\pan, arrPan[index], \chanNum, numChannels]);
				}, {
					Ndef(key).put(0, object, extraArgs: [\pan1, arrPan[index][0],
						\pan2, arrPan[index][1], \chanNum, numChannels]);
				});
				nodeTime.yield;
				Ndef(key) <<>.in item;
				nodeTime.yield;
				if(arrPan.rank == 1, {
					Ndef(key).xset(\pan, arrPan[index], \chanNum, numChannels);
				}, {
					Ndef(key).xset(\pan1, arrPan[index][0], \pan2, arrPan[index][1],
						\chanNum, numChannels);
				});
			};
			if(play, {this.play});
		}.fork;
	}

	redefine {arg ndefArr, system=\pan2, panArr, chanNum, play=true;
		var fadeTime;
		//not working for the moment
		fadeTime = ndefs[0].fadeTime;
		{
			ndefs.do{|item| item.clear(fadeTime)};
			fadeTime.yield;
			nodeTime.yield;
			this.initSpace(ndefArr, system, panArr, chanNum, play, fadeTime);
		}.fork;
	}

	clear {
		ndefs.do{|item| item.clear};
	}

	fadeTime {arg index, time;
		if(index == \all, {
			ndefs.do{|item| item.fadeTime = time};
		}, {
			ndefs[index].fadeTime = time
		});
	}

	set {| ... args|
		if(args[0].isInteger.not, {
			ndefs.do{|item| item.set(*args)};
		}, {
			ndefs[args[0]].set(*args.copyRange(1, args.size));
		});
	}

	xset {| ... args|
		if(args[0].isInteger.not, {
			ndefs.do{|item| item.xset(*args)};
		}, {
			ndefs[args[0]].xset(*args.copyRange(1, args.size));
		});
	}

	setn {arg key, array;
		array.do{|item, index|
			this.set(index, key, *item);
		}
	}

	xsetn {arg key, array;
		array.do{|item, index|
			this.xset(index, key, *item);
		}
	}

}