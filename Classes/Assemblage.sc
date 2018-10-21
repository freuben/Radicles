Assemblage : MainImprov {var <tracks, <inputs, <outputs, <livetracks, <trackCount=1, <busCount=1, <ndefs, <space, <>masterSynth, <trackNames;

	*new {arg trackNum=1, busNum=0, chanNum=2, spaceType=\bal2;
		^super.new.initAssemblage(trackNum, busNum, chanNum, spaceType);
	}

	initAssemblage {arg trackNum=1, busNum=0, chanNum=2, spaceType=\bal2;
		var ndefCS1, ndefCS2, masterTag, spaceTag, spaceSynth, main;
		var chanMaster, chanTrack, chanBus, spaceMaster, spaceTrack, spaceBus;
		server.options.numAudioBusChannels = 1024;
		server.options.numControlBusChannels = 16384;
		server.waitForBoot{
			{
				masterSynth = {arg volume=0; (\in * volume.dbamp ).softclip};
				if(chanNum.isArray.not, {
					chanMaster = chanNum;
					chanTrack = chanNum;
					chanBus = chanNum;
				}, {
					chanMaster = chanNum[0];
					chanTrack = chanNum[1];
					chanBus = chanNum[2];
				});
				if(spaceType.isArray.not, {
					spaceMaster = spaceType;
					spaceTrack = spaceType;
					spaceBus = spaceType;
				}, {
					spaceMaster = spaceType[0];
					spaceTrack = spaceType[1];
					spaceBus = spaceType[2];
				});

				this.addTrack(\master, chanMaster, spaceMaster, masterSynth);
				this.addTracks(trackNum, \track, chanTrack, spaceTrack);
				this.addTracks(busNum, \bus, chanBus, spaceBus);

				tracks.do{|item|
					this.autoRoute(item);
				};
				server.sync;
				this.play;
			}.fork
		}
	}

	get {arg trackType = \track;
		^tracks.select{|item|
			item.last[0].asString.find(trackType.asString).notNil; };
	}

	getMaster {
		^this.get(\master)[0];
	}

	getBuses {
		^this.get(\bus);
	}

	getTracks {
		^this.get(\track);
	}

	play {var ndefCS;
		ndefCS = "Ndef('master').play;";
		ndefCS.radpost;
		ndefCS.interpret;
	}

	autoRoute {arg trackInfo;
		var newArr, ndefArr, ndefCS, synthArr, intArr, thisSynthFunc;
		newArr = trackInfo.reverse;
		ndefArr = newArr.flop[0];
		synthArr = newArr.flop[1];
		(newArr.size-1).do{|index|
			var extraArgs, synthFunc, dest;
			extraArgs = Ndef(ndefArr[index]).getKeysValues;
			dest = Ndef(ndefArr[index+1]);
					if(synthArr[index].isFunction, {
			if(synthArr[index].isArray, {
				thisSynthFunc = synthArr[index];
				synthFunc = thisSynthFunc[0].filterFunc([dest] ++
					thisSynthFunc.copyRange(1, thisSynthFunc.size-1); );
			}, {
				synthFunc = synthArr[index].filterFunc(dest);
			});
			if(extraArgs.isEmpty, {
				ndefCS = ("Ndef(" ++ ndefArr[index].cs ++ ", " ++ synthFunc.cs ++ ");");
			}, {
				ndefCS = ("Ndef(" ++ ndefArr[index].cs ++ ").put(0, " ++ synthFunc.cs ++
					", extraArgs: " ++ extraArgs.cs ++ ");");
			});

			}, {
				synthArr[index].postln;
			});
			intArr = intArr.add(ndefCS);
		};
		{intArr.reverse.do{|item| item.radpost; item.interpret; server.sync }; }.fork;
	}

	input {arg ndefs, type=\track, num=1;
		var trackArr, ndefCS;
		trackArr = this.get(type)[num-1];

		ndefCS = ("Ndef(" ++ trackArr[0][0].cs ++ ", "
			++ trackArr[0][1].filterFunc(ndefs).cs ++ ");");

		ndefCS.radpost;
		ndefCS.interpret;
	}

	addTrack {arg type=\track, chanNum, spaceType, trackSynth;
		var trackTag,spaceTag, ndefCS1, ndefCS2, spaceSynth;

		if([\track, \bus, \master].includes(type), {

			chanNum ?? {chanNum = space[0][0]};
			spaceType ?? {spaceType = space[0][1]};

			trackSynth ?? {trackSynth = {arg volume=0; (\in * volume.dbamp )}; };
			spaceSynth = SynthFile.read(\space, spaceType);

			case
			{type == \track} {
				trackTag = (type.asString ++ trackCount).asSymbol;
				trackCount = trackCount + 1;
			}
			{type == \bus} {
				trackTag = (type.asString ++ busCount).asSymbol;
				busCount = busCount + 1;
			}
			{type == \master} {
				trackTag = (type.asString).asSymbol;
			};

			ndefCS1 = "Ndef.ar(" ++ trackTag.cs ++ ", ";
			ndefCS1 = (ndefCS1 ++ chanNum.cs ++ ");");
			ndefCS1.radpost;
			ndefCS1.interpret;
			ndefCS2 = ("Ndef(" ++ trackTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS2.radpost;
			ndefCS2.interpret;
			server.sync;
			spaceTag = ("space" ++ trackTag.asString.capitalise).asSymbol;
			ndefCS1 = "Ndef.ar(" ++ spaceTag.cs ++ ", ";
			ndefCS1 = (ndefCS1 ++ chanNum.cs ++ ");");
			ndefCS1.radpost;
			ndefCS1.interpret;
			ndefCS2 = ("Ndef(" ++ spaceTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS2.radpost;
			ndefCS2.interpret;
			server.sync;
			tracks = tracks.add([ [spaceTag, spaceSynth], [trackTag, trackSynth] ]);
			trackNames = trackNames.add(trackTag);
			space = space.add([chanNum, spaceType ]);

		}, {
			"track type not found".warn;
		});
	}

	addTracks {arg number, type=\track, chanNum, spaceType, trackSynth;
		var thisChan, thisDest;
		number.do{
			this.addTrack(type, chanNum, spaceType, trackSynth);
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