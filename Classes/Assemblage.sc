Assemblage : MainImprov {var <tracks, <inputs, <outputs, <livetracks, <trackCount=1, <ndefs, <master, <space, <>masterSynth;

	*new {arg trackNum=1, busNum=0, chanNum=2, spaceType=\pan2;
		^super.new.initAssemblage(trackNum, busNum, chanNum, spaceType);
	}

	initAssemblage {arg trackNum=1, busNum=0, chanNum=2, spaceType=\pan2;
		var ndefCS1, ndefCS2, masterTag, spaceTag, spaceSynth;
		Server.default.waitForBoot{
			masterSynth = {arg volume=0; (\in * volume.dbamp ).softclip};
			spaceSynth = SynthFile.read(\space, spaceType);
			masterTag = \master;
			ndefCS1 = "Ndef.ar(" ++ masterTag.cs ++ ", ";
			ndefCS1 = (ndefCS1 ++ chanNum.cs ++ ");");
			ndefCS1.radpost;
			ndefCS1.interpret;
			ndefCS2 = ("Ndef(" ++ masterTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS2.radpost;
			ndefCS2.interpret;
			spaceTag = \spaceMaster;
			ndefCS1 = "Ndef.ar(" ++ spaceTag.cs ++ ", ";
			ndefCS1 = (ndefCS1 ++ chanNum.cs ++ ");");
			ndefCS1.radpost;
			ndefCS1.interpret;
			ndefCS2 = ("Ndef(" ++ spaceTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS2.radpost;
			ndefCS2.interpret;
			master = [ [spaceTag, spaceSynth], [masterTag, masterSynth] ];
			space = [ [chanNum, spaceType ] ];
			this.autoRoute(master);
			this.play;
		}
	}

	play {var ndefCS;
		ndefCS = "Ndef('master').play;";
		ndefCS.radpost;
		ndefCS.interpret;
	}

	autoRoute {arg trackInfo;
		var newArr, ndefArr, ndefCS, synthArr, intArr;
		newArr = trackInfo.reverse;
		ndefArr = newArr.flop[0];
		synthArr = newArr.flop[1];
		(newArr.size-1).do{|index|
			var extraArgs, synthFunc, dest;
			extraArgs = Ndef(ndefArr[index]).getKeysValues;
			dest = Ndef(ndefArr[index+1]);
			synthFunc = synthArr[index].filterFunc(dest);
			if(extraArgs.isEmpty, {
				ndefCS = ("Ndef(" ++ ndefArr[index].cs ++ ", " ++ synthFunc.cs ++ ");");
			}, {
				ndefCS = ("Ndef(" ++ ndefArr[index].cs ++ ").put(0, " ++ synthFunc.cs ++
					", extraArgs: " ++ extraArgs.cs ++ ");");
			});
			intArr = intArr.add(ndefCS);
		};
		intArr.reverse.do{|item| item.radpost; item.interpret };
	}

	addtrack {arg input, channels, spaceType=\pan2;
		var trackTag, ndefCS1, ndefCS2;
		channels ?? {channels = input.numChannels};
		trackTag = ("track" ++ trackCount).asSymbol;
		trackCount = trackCount + 1;
		/*			ndefCS1 = "Ndef.ar(";
		ndefCS1 = (ndefCS1 ++ ndefTag.cs ++ ", " ++ channels.cs ++ ");");
		ndefCS1.radpost;
		ndefCS1.interpret;
		ndefCS2 = ("Ndef(" ++ ndefTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
		ndefCS2.radpost;
		ndefCS2.interpret;*/
		/*			ndefs = ndefs.add(Ndef(ndefTag));*/
		inputs = inputs.add(input);
		outputs = outputs.add(input);
		//tag, input, output, channels, spaceType:
		tracks = tracks.add( [trackTag, input, input, channels, spaceType] );
		livetracks = livetracks.add(nil);
	}

	addtracks {arg number, inputs, channels=1, spaceTypes;
		var thisChan, thisDest;
		number.do{|index|
			if(channels.isArray, {thisChan = channels[index]}, {thisChan = channels});
			if(spaceTypes.isArray, {thisDest = spaceTypes[index]}, {thisDest = spaceTypes});
			this.add(inputs, thisChan, thisDest);
		};
	}

	addAlltracks {arg arr;
		arr.do{|item|
			this.addtrack(item);
		}
	}

	remove {arg track=1;
		var trackIndex;
		trackIndex = track - 1;
		if((track >= 1).and(track <= tracks.size), {
			/*ndefs[trackIndex].free;*/
			ndefs[trackIndex].clear;
			ndefs.removeAt(trackIndex);
			tracks.removeAt(trackIndex);
			livetracks.removeAt(trackIndex);
		}, {
			"track Number not Found".warn;
		});
	}

	removeArr {arg trackArr;
		var newArr;
		trackArr.do{|item|
			if((item >= 1).and(item <= tracks.size), {
				newArr = newArr.add(item-1);
				/*ndefs[item-1].free;*/
				ndefs[item-1].clear;
			}, {
				"track Number not Found".warn;
			});
		};
		ndefs.removeAtAll(newArr);
		tracks.removeAtAll(newArr);
		livetracks.removeAll(newArr);
	}

	removeAll {
		ndefs.do{|item| item.clear };
		ndefs = [];
		tracks = [];
		livetracks = [];
		trackCount = 1;
	}

	clear {
		Ndef.clear;
		ndefs = [];
		tracks = [];
		livetracks = [];
		trackCount = 1;
	}

	playNdefs {
		Server.default.waitForBoot{
			outputs.do{|item| item.play};
		};
	}

}