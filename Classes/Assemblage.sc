Assemblage : MainImprov {var <tracks, <specs, <inputs, <outputs, <livetracks,
	<trackCount=1, <busCount=1, <space, <ndefs, <>masterSynth, <trackNames,
	<>masterInput, <busArr, <busInArr, <filters;

	*new {arg trackNum=1, busNum=0, chanNum=2, spaceType;
		^super.new.initAssemblage(trackNum, busNum, chanNum, spaceType);
	}

	initAssemblage {arg trackNum=1, busNum=0, chanNum=2, spaceType;
		var chanMaster, chanTrack, chanBus, spaceMaster, spaceTrack, spaceBus, inArr;
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
				busArr = nil!2!busNum;
				tracks.do{|item|
					this.autoRoute(item);
				};
				server.sync;
				inArr = ndefs.flop[1];
				masterInput = inArr.copyRange(1, inArr.size-1);
				this.input(masterInput, \master);
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

	findSpaceType {arg chanNum=1;
		var spaceType;
		case
		{chanNum == 1} {spaceType = \pan2}
		{chanNum == 2} {spaceType = \bal2}
		{chanNum == 4} {spaceType = \pan4};
		^spaceType;
	}

	addTrack {arg type=\track, chanNum=1, spaceType, trackSynth;
		var trackTag,spaceTag, ndefCS1, ndefCS2, spaceSynth, spaceSpecs, trackSpecs;
		if([\track, \bus, \master].includes(type), {
			spaceType ?? {spaceType = this.findSpaceType(chanNum);};
			trackSynth ?? {trackSynth = {arg volume=0; (\in * volume.dbamp )}; };
			trackSpecs ?? trackSpecs = [ ['volume', [-inf, 6, \db, 0, -inf, " dB" ] ] ];
			spaceSynth = SynthFile.read(\space, spaceType);
			spaceSpecs = SpecFile.read(\space, spaceType);
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
			specs = specs.add([ [spaceTag, spaceSpecs], [trackTag, trackSpecs] ]);
			ndefs = ndefs.add([Ndef(spaceTag), Ndef(trackTag)]);
			trackNames = trackNames.add(trackTag);
			space = space.add([spaceTag, chanNum, spaceType ]);
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

	autoRoute {arg trackInfo;
		var newArr, ndefArr, ndefCS, synthArr, intArr, thisSynthFunc;
		newArr = trackInfo.reverse;
		ndefArr = newArr.flop[0];
		synthArr = newArr.flop[1];
		(newArr.size-1).do{|index|
			var synthFunc, dest;
			dest = Ndef(ndefArr[index+1]);
			if(synthArr[index].isFunction, {
				if(synthArr[index].isArray, {
					thisSynthFunc = synthArr[index];
					synthFunc = thisSynthFunc[0].filterFunc([dest] ++
						thisSynthFunc.copyRange(1, thisSynthFunc.size-1); );
				}, {
					synthFunc = synthArr[index].filterFunc(dest);
				});
				ndefCS = this.ndefPrepare(Ndef(ndefArr[index]), synthFunc);
			}, {
				synthArr[index].radpost;
			});
			intArr = intArr.add(ndefCS);
		};
		{intArr.reverse.do{|item| item.radpost; item.interpret; server.sync }; }.fork;
	}

	ndefPrepare {arg ndef, func;
		var extraArgs, key, result;
		extraArgs = ndef.getKeysValues;
		key = ndef.key;
		if(extraArgs.isEmpty, {
			result = ("Ndef(" ++ key.cs ++ ", " ++ func.cs ++ ");");
		}, {
			result = ("Ndef(" ++ key.cs ++ ").put(0, " ++ func.cs ++
				", extraArgs: " ++ extraArgs.flat.cs ++ ");");
		});
		^result;
	}

	findTrackArr {arg key=\master;
		var arr;
		tracks.do{|it, in|
			it.do{|item, index| if(item[0] == key, {arr = [in, index]}) }
		};
		^arr;
	}

	collectTrackArr {arg find=\track;
		var arr;
		tracks.do{|it, in|
			it.do{|item, index| if(item[0].asString.find(find.asString).notNil, {arr = arr.add(item)}); }
		};
		^arr;
	}

	replaceFunc {arg name, function;
		var array;
		array = this.findTrackArr(name);
		tracks[array[0]][array[1]][1] = function;
	}

	respace {arg trackName=\spaceMaster, trackInput, spaceType;
		var trackTag,spaceTag, ndefCS1, ndefCS2, spaceSynth;
		var array, func, chanNum;
		array = this.findTrackArr(trackName);
		if(array.notNil, {
			if(trackInput.isArray, {
				chanNum = trackInput.collect{|item| item.numChannels}.maxItem;
			}, {
				chanNum = trackInput.numChannels;
			});
			spaceType ?? {spaceType = this.findSpaceType(chanNum);};
			spaceSynth = SynthFile.read(\space, spaceType);
			func = spaceSynth.filterFunc(trackInput);
			this.replaceFunc(trackName, func);
			space.do{|item| if(item[0] == trackName, {item[1] = chanNum; item[2] = spaceType }) };
		}, {
			"track name not found".warn;
		});
	}

	input {arg ndefsIn, type=\track, num=1, respace=true, spaceType;
		var trackArr, ndefCS, connect, inTag;
		trackArr = this.get(type)[num-1];
		if(type == \master, {inTag = type}, {inTag = (type ++ num).asSymbol});
		inputs = inputs.add([inTag, ndefsIn]);
		inputs = ([ inputs[0] ] ++
			inputs.copyRange(1, inputs.size-1).sort { arg a, b; a[0] <= b[0] };);
		if(ndefsIn.numChannels.isNil, {ndefsIn.mold(1) });
		if(ndefsIn.isArray, {
			connect =
			ndefsIn.collect({|item|
				item.numChannels != Ndef(trackArr[0][0]).numChannels;
			}).includes(true).not;
		}, {
			if(ndefsIn.numChannels != Ndef(trackArr[0][0]).numChannels, {
				connect = false;
			}, {
				connect = true;
			});
		});
		if(connect, {
			ndefCS = this.ndefPrepare(Ndef(trackArr[0][0]), trackArr[0][1].filterFunc(ndefsIn));
			ndefCS.radpost;
			ndefCS.interpret;
		}, {
			if(respace, {
				this.respace(trackArr[0][0], ndefsIn, spaceType);
				trackArr = this.get(type)[num-1];
				ndefCS = this.ndefPrepare(Ndef(trackArr[0][0]), trackArr[0][1].filterFunc(ndefsIn));
				ndefCS.radpost;
				ndefCS.interpret;
			}, {
				"channel number input doesn't match track".warn;
			});
		});
	}

	getTrackInput {arg type=\master, num=1;
		var inTag, index;
		if(type == \master, {inTag = type}, {inTag = (type ++ num).asSymbol});
		if(trackNames.includes(inTag), {
			index = inputs.flop[0].indexOfEqual(inTag);
			if(index.notNil, {
				^inputs.flop[1][index];
			}, {
				"no input assigned to this track".warn;
			});
		}, {
			"track doesn't exist".warn;
		});
	}

	getInputs {arg type=\master;
		var arr;
		arr = [];
		inputs.flop[0].do{|item, index|
			if( (item.asString.find(type.asString)).notNil , {
				arr = arr.add(inputs[index]);
			});
		};
		^arr;
	}

	getAllInputs {arg master=true;
		var arr, types;
		arr=[];
		types = [\bus, \track];
		if(master, {types = [\master] ++ types });
		types.do{|item|
			arr = this.getInputs(item) ++arr;
		};
		^arr;
	}

	setInputs {arg ndefIns, type, num;
		var typeArr, numArr;
		{
			if(type.isArray, {
				typeArr = type;
			}, {
				typeArr = type!ndefIns.size;
			});
			if(num.isArray, {
				numArr = num;
			}, {
				numArr = (num..ndefIns.size);
			});
			ndefIns.do{|item, index|
				this.input(item, typeArr[index], numArr[index]);
				server.sync;
			}
		}.fork;
	}

	getTrackOutput {arg type=\master, num=1;
		var inTag;
		if(type == \master, {inTag = type}, {inTag = (type ++ num).asSymbol});
		^Ndef(inTag);
	}

	bus {arg trackNum=1, busNum=1, mix=1, dirIn=true;
		var numChan, busTag, ndefCS1, ndefCS2, ndefCS3, funcBus, thisBusArr,
		busAdd, argIndex;
		{
			if(dirIn.not, {
				masterInput = masterInput.copyRange(1, masterInput.size-1);
				this.input(masterInput, \master);
				server.sync;
			});
			numChan = Ndef(this.getBuses.flop[0][busNum-1][0]).numChannels;
			busTag = ("bus" ++ busNum ++ "In").asSymbol;
			if(busArr[busNum-1][0].isNil, {
				busArr[busNum-1][0] = busTag;
				ndefCS1 =	("Ndef.ar(" ++ busTag.cs ++ ", " ++ numChan ++ ");" );
				ndefCS1.radpost;
				ndefCS1.interpret;
				ndefCS1 = ("Ndef(" ++ busTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
				ndefCS1.radpost;
				ndefCS1.interpret;
				server.sync;
				this.input(Ndef(busTag), \bus, busNum);
			});
			busAdd = {busArr[busNum-1][1] = busArr[busNum-1][1].add(
				this.getTrackOutput(\track, trackNum));};
			thisBusArr = busArr[busNum-1][1];
			if(thisBusArr.notNil, {
				if(thisBusArr.includes(this.getTrackOutput(\track, trackNum)).not, {
					busAdd.()
				});
			}, {
				busAdd.();
			});
			funcBus = busArr[busNum-1][1].busFunc;
			ndefCS2 = this.ndefPrepare(Ndef(busTag), funcBus);
			server.sync;
			ndefCS2.radpost;
			ndefCS2.interpret;
			server.sync;
			argIndex = busArr[busNum-1][1].indexOf(Ndef(("track" ++ trackNum).asSymbol));
			ndefCS3 = "Ndef(" ++ busTag.cs ++ ").set('vol" ++ (argIndex+1)
			++ "', " ++ mix ++ ");";
			ndefCS3.radpost;
			ndefCS3.interpret;
		}.fork;
	}

	busMix {arg bus=1, slot=1, mix=0, lag;
		var tag, volTag, ndefCS;
		tag = ("bus" ++ bus ++ "In").asSymbol;
		volTag = ("vol" ++ slot).asSymbol;
		if(lag.notNil, {
			ndefCS = "Ndef(" ++ tag.cs ++ ").lag(" ++volTag.cs ++ ", " ++ lag ++ ");";
			ndefCS.radpost;
			ndefCS.interpret;
		});
		ndefCS = "Ndef(" ++ tag.cs ++ ").set(" ++volTag.cs ++ ", " ++ mix ++ ");";
		ndefCS.radpost;
		ndefCS.interpret;
	}

	busLag {arg bus=1, slot=1, lag=0;
		var tag, volTag, ndefCS;
		tag = ("bus" ++ bus ++ "In").asSymbol;
		volTag = ("vol" ++ slot).asSymbol;
		ndefCS = "Ndef(" ++ tag.cs ++ ").lag(" ++volTag.cs ++ ", " ++ lag ++ ");";
		ndefCS.radpost;
		ndefCS.interpret;
	}

	filter {arg type=\track, num= 1, slot=1, filter=\pch, extraArgs;
		var filterTag, ndefArr, ndefCS, arr1, arr2, arr3, arrSize, filterInfo, setArr,
		setTag, filterIndex, startNdefs, filterSpecs;
		if(type == \master, {
			filterTag = ("filter" ++ type.asString.capitalise ++ "_" ++ slot).asSymbol;
		}, {
			filterTag = ("filter" ++ type.asString.capitalise ++ num ++ "_" ++ slot).asSymbol;
		});
		ndefArr = this.get(type)[num-1];
		startNdefs = {
			ndefCS = "Ndef.ar(" ++ filterTag.cs ++ ", " ++ ndefArr[0].numChannels ++ ");";
			ndefCS.radpost;
			ndefCS.interpret;
			ndefCS = "Ndef(" ++ filterTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";";
			ndefCS.radpost;
			ndefCS.interpret;
		};

		if(filters.isNil, {
			startNdefs.();
		}, {
			if(filters.flop[0].includes(filterTag).not, {
				startNdefs.();
			});
		});

		filterInfo = [filterTag, SynthFile.read(\filter, filter);];
		//still some work to do with automatic control specs
		filterSpecs = [filterTag, SpecFile.read(\filter, \pch)];
		//
		if(ndefArr.size > 2, {
			arr1 = [ndefArr[0], ndefArr.last];
			arr2 = ndefArr.copyRange(1, ndefArr.size-2);
			arr3 = arr2.flop[0];

			if(arr3.includes(filterTag), {
				arr2[arr3.indexOf(filterTag)] = filterInfo;
				filterIndex = filters.flop[0].indexOf(filterTag);
				filters[filterIndex] = [filterTag, filter];
			}, {

				arr2 = arr2.add(filterInfo);
				arr2.sort({ arg a, b; a[0] <= b[0] });

				filters = filters.add([filterTag, filter]);
				filters.sort({ arg a, b; a[0] <= b[0] });
			});

			arr1.insert(1, arr2);
			arrSize = (arr1.flat.size/2);
			arr1 = arr1.reshape(arrSize.asInteger, 2)

		}, {

			filters = filters.add([filterTag, filter]);
			filters.sort({ arg a, b; a[0] <= b[0] });

			arr1 = ndefArr;
			arr2 = filterInfo;
			arr1.insert(1, arr2);
		});

		if(extraArgs.notNil, {
			extraArgs.pairsDo{|it1, it2|
				Ndef(filterTag).set(it1, it2);
			};
		});
		if(type == \master, {
			setTag = type.asSymbol;
		}, {
			setTag = (type ++ num).asSymbol;
		});
		setArr = this.findTrackArr(setTag);
		tracks[setArr[0]] = arr1;
		ndefs[setArr[0]] = arr1.flop[0].collect({|item| Ndef(item)});
		this.autoRoute(arr1);
	}

	removeFilter {arg type=\track, num= 1, slot=1;
		var thisTrack, thisSlot, ndefCS, setArr;
		thisTrack = this.get(type)[num-1];
		if(thisTrack.size > 2, {
			if(slot < (thisTrack.size-1), {
				thisSlot = thisTrack[slot];
				ndefCS = "Ndef(" ++ thisSlot[0].cs ++ ").clear(" ++ fadeTime ++ ");";
				ndefCS.radpost;
				ndefCS.interpret;
				thisTrack.removeAt(slot);
				setArr = this.findTrackArr((type ++ 1).asSymbol);
				ndefs[setArr[0]].removeAt(slot);
				filters = filters.reject({|item| item[0] == thisSlot[0] });
				this.autoRoute(thisTrack);
			}, {
				"Filter slot not found".warn;
			});
		}, {
			"No filters to remove".warn;
		});
	}

	removeTrackFilters {arg type=\track, num= 1, post=true;
		var thisTrack, thisSlot, ndefCS, arr1, arr2, setArr;
		thisTrack = this.get(type)[num-1];
		if(thisTrack.size > 2, {
			arr1 = [thisTrack[0], thisTrack.last];
			arr2 = thisTrack.copyRange(1, thisTrack.size-2);
			arr2.do{|item|
				ndefCS = "Ndef(" ++ item[0].cs ++ ").clear(" ++ fadeTime ++ ");";
				ndefCS.radpost;
				ndefCS.interpret;
				thisTrack.remove(item);
				if(type == \master, {num=""});
				setArr = this.findTrackArr((type ++ num).asSymbol);
				ndefs[setArr[0]].remove(Ndef(item[0]));
				filters = filters.reject({|it| it[0] == item[0] });
			};
			this.autoRoute(arr1);
		}, {
			if(post, {
				"No filters to remove".warn;
			});
		});
	}

	removeAllFilters {arg type=\all, post=true;
		var thisTrack, num, maxSize;
		if(type == \all, {
			if(filters.isEmpty, {
				"No filters to remove".warn;
			}, {
			[\track, \bus, \master].do{|item| this.removeAllFilters(item, false) };
			});
		}, {
		thisTrack = this.get(type);
			maxSize = thisTrack.collect{|item| item.size }.maxItem;
			if(maxSize > 2, {
		thisTrack.do{|item|
			if(type == \master, {
				num = 1;
			}, {
			num = item.last[0].asString.last.asString.interpret;
			});
				this.removeTrackFilters(type, num, false);
		};
			}, {
				if(post, {
				"No filters to remove".warn;
			});
			});
		});
	}

	remove {arg track=1;
		var trackIndex;
		trackIndex = track - 1;
		if((track >= 1).and(track <= tracks.size), {
			/*ndefs[trackIndex].free;*/
			ndefs[trackIndex].clear;
			ndefs.removeAt(trackIndex);
			tracks.removeAt(trackIndex);
			specs.removeAt(trackIndex);
			livetracks.removeAt(trackIndex);
		}, {
			"track number not found".warn;
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
		specs.removeAtAll(newArr);
		livetracks.removeAll(newArr);
	}

	removeAll {
		ndefs.do{|item| item.clear };
		ndefs = [];
		tracks = [];
		specs = [];
		livetracks = [];
		trackCount = 1;
	}

	clear {
		Ndef.clear;
		ndefs = [];
		tracks = [];
		specs = [];
		livetracks = [];
		trackCount = 1;
	}

	playNdefs {
		Server.default.waitForBoot{
			outputs.do{|item| item.play};
		};
	}

}