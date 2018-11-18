Space : Radicles {
	var <ndefs, <>objectFile, <numChannels, <inputArr, <arrPan, thisIndex, <>object;

	*new {arg ndefArr, system=\pan2, panArr, chanNum=2, play=true, fadeTime;
		^super.new.initSpace(ndefArr, system, panArr, chanNum, play, fadeTime);
	}

	initSpace {arg ndefArr, system, panArr, chanNum, play, fadeTime;

		panArr ?? {panArr = Array.panDis(ndefArr.size)};

		objectFile = (mainPath ++ "Files/SynthFiles/Space.scd").loadPath;

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
		var key, csobject, newcs, newfunc;

		ndefArr ?? {ndefArr = inputArr};
		inputArr = ndefArr;
		arrPan = panArr;

		ndefs = [];

		{
			ndefArr.do{|item, index|
				key = ((\space++(index+1)).asSymbol);
				Ndef.ar(key, numChannels);
				fadeTime ?? {fadeTime =Ndef(key).fadeTime};
				Ndef(key).fadeTime = fadeTime;
				Ndef(key).reshaping = \elastic;
				ndefs = ndefs.add(Ndef(key));

				csobject = object.cs;
				newcs = csobject.replace("\\in", item.asString ++ ".ar(1)" )
				.replace("\\num", numChannels.asString);
				newfunc = newcs.interpret;

				if(arrPan.rank == 1, {
					Ndef(key).put(0, newfunc, extraArgs: [\pan, arrPan[index]]);
				}, {
					Ndef(key).put(0, newfunc, extraArgs: [\pan1, arrPan[index][0],
						\pan2, arrPan[index][1]]);
				});
			};
			if(play, {this.play});
		}.fork;
	}

	redefine {arg ndefArr, system=\pan2, panArr, chanNum=2, play=true;
		var fadeTime;
		fadeTime = ndefs[0].fadeTime;
		{
			ndefs.do{|item| item.clear(fadeTime)};
			fadeTime.yield;
/*			nodeTime.yield;*/
			this.initSpace(ndefArr, system, panArr.postln, chanNum, play, fadeTime);
		}.fork;
	}

	clear {
		ndefs.do{|item| item.clear};
	}

	fadeTime {arg index, time;
		if(index.notNil, {
		if(index == \all, {
			ndefs.do{|item| item.fadeTime = time};
		}, {
			ndefs[index].fadeTime = time
		});
		}, {
			ndefs.do{|item| item.fadeTime.postln};
		});
	}

	set {| ... args|
		if(args[0].isInteger.not, {
			ndefs.do{|item, index| item.set(args[0], args[1][index])};
		}, {
			ndefs[args[0]].set(*args.copyRange(1, args.size));
		});
	}

	xset {| ... args|
		if(args[0].isInteger.not, {
			ndefs.do{|item, index| item.xset(args[0], args[1][index])};
		}, {
			ndefs[args[0]].xset(*args.copyRange(1, args.size));
		});
	}

	setn {| ... args|
		args.do{|item, index|
			if(index.even, {
				[item, args[index+1]].postln;
				this.set(item, args[index+1]);
			});
		}
	}

	xsetn {| ... args|
		args.do{|item, index|
			if(index.even, {
				[item, args[index+1]].postln;
				this.xset(item, args[index+1]);
			});
		}
	}

}