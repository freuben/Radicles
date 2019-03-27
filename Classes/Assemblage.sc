Assemblage : Radicles {var <tracks, <specs, <inputs, <livetracks,
	<trackCount=1, <busCount=1, <space, <masterNdefs, <>masterSynth,
	<trackNames, <>masterInput, <busArr, <filters, <filterBuff , <>mixerWin,
	<setVolSlider, <mixTrackNames, <>systemChanNum, <mixTrackNdefs,
	<sysChans, <sysPan, <setBusIns, <setKnobIns, <setPanKnob, <outputSettings;

	*new {arg trackNum=1, busNum=0, chanNum=2, spaceType;
		^super.new.initAssemblage(trackNum, busNum, chanNum, spaceType);
	}

	initAssemblage {arg trackNum=1, busNum=0, chanNum=2, spaceType;
		var chanMaster, chanTrack, chanBus, spaceMaster, spaceTrack, spaceBus, inArr;
		/*		server.options.numAudioBusChannels = 1024;
		server.options.numControlBusChannels = 16384;*/
		server.waitForBoot{
			{
				masterSynth = {arg volume=0, lagTime=0; (\in * volume.dbamp.lag(lagTime); ).softclip};
				if(chanNum.isArray.not, {
					chanMaster = chanNum;
					chanTrack = chanNum;
					chanBus = chanNum;
				}, {
					chanTrack = chanNum[0];
					chanBus = chanNum[1];
					chanMaster = chanNum[2];
				});
				systemChanNum = chanNum;
				if(spaceType.isArray.not, {
					spaceMaster = spaceType;
					spaceTrack = spaceType;
					spaceBus = spaceType;
				}, {
					spaceTrack = spaceType[0];
					spaceBus = spaceType[1];
					spaceMaster = spaceType[2];
				});
				this.addTrack(\master, chanMaster, spaceMaster, masterSynth);
				this.addTracks(trackNum, \track, chanTrack, spaceTrack);
				this.addTracks(busNum, \bus, chanBus, spaceBus);
				tracks.do{|item|
					this.autoRoute(item);
				};
				server.sync;
				inArr = masterNdefs.flop[1];
				masterInput = inArr.copyRange(1, inArr.size-1);
				this.input(masterInput, \master);
				server.sync;
				this.play;
				this.updateMixInfo;
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
		ndefCS.radpost.interpret;
	}

	findSpaceType {arg chanNum=1;
		var spaceType;
		case
		{chanNum == 1} {spaceType = \pan2}
		{chanNum == 2} {spaceType = \bal2}
		{chanNum == 4} {spaceType = \pan4};
		^spaceType;
	}

	addTrack {arg type=\track, chanNum=1, spaceType, trackSynth, trackSpecs;
		var trackTag,spaceTag, ndefCS1, ndefCS2, spaceSynth, spaceSpecs, thisTrackInfo;
		if([\track, \bus, \master].includes(type), {
			spaceType ?? {spaceType = this.findSpaceType(chanNum);};
			trackSynth ?? {trackSynth = {arg volume=0, lagTime=0; (\in * volume.dbamp.lag(lagTime); )}; };
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
				busArr = busArr.add(nil!2);
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
			thisTrackInfo = [ [spaceTag, spaceSynth], [trackTag, trackSynth] ];
			tracks = tracks.add([ [spaceTag, spaceSynth], [trackTag, trackSynth] ]);
			specs = specs.add([ [spaceTag, spaceSpecs], [trackTag, trackSpecs] ]);
			masterNdefs = masterNdefs.add([Ndef(spaceTag), Ndef(trackTag)]);
			trackNames = trackNames.add(trackTag);
			space = space.add([spaceTag, spaceType]);
			^thisTrackInfo;
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
			this.addTrack(item);
		}
	}

	autoRoute {arg trackInfo, replace=false;
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

	autoAddTrack {arg type=\track, chanNum, spaceType, trackSynth, trackSpecs, action={};
		var trackInfo, inArr, masterInput;
		{
			chanNum ?? {chanNum = systemChanNum};
			trackInfo = this.addTrack(type, chanNum, spaceType, trackSynth, trackSpecs);
			server.sync;
			this.autoRoute(trackInfo);
			server.sync;
			inArr = masterNdefs.flop[1];
			masterInput = inArr.copyRange(1, inArr.size-1);
			this.input(masterInput, \master);
			server.sync;
			if(mixerWin.notNil, {
				if(mixerWin.notClosed, {
					{this.refreshMixGUI;}.defer;
				});
			});
			server.sync;
			action.();
		}.fork;
	}

	ndefPrepare {arg ndef, func;
		var extraArgs, key, result;
		extraArgs = ndef.controlKeysValues;
		key = ndef.key;
		if(extraArgs.isEmpty, {
			result = [key, func].presetToNdefCS;
		}, {
			result = [key, func, extraArgs].presetToNdefCS;
		});
		^result;
	}

	routePairs {var newArr, newMasNdefs;
		inputs.do{|item, index|
			var ndefOut;
			ndefOut = Ndef(item[0]);
			if(item[1].isArray, {item[1].do{|it| newArr = newArr.add([it, ndefOut])   };
			}, {
				newArr = newArr.add([item[1], ndefOut]);
			});
		};
		newMasNdefs = masterNdefs.collect{|it| it.collectAdjacentPairs };
		newMasNdefs = newMasNdefs.reshape((newMasNdefs.flat.size/2).asInteger, 2);
		^(newMasNdefs ++ newArr);
	}

	routePairsSort {
		var keyArr, newKeyArr = [[],[],[],[]];
		keyArr = this.routePairs.collect({|ndef| [ndef[0].key, ndef[1].key] });
		keyArr.do{|item, index|
			var bol;
			bol = true;
			case
			{
				(item[0].asString.find("track").notNil).or(item[0].asString.find("Track").notNil)
			} {newKeyArr[1] = newKeyArr[1].add(keyArr[index]); bol = false}
			{
				(item[0].asString.find("bus").notNil).or(item[0].asString.find("Bus").notNil)
			} {newKeyArr[2] = newKeyArr[2].add(keyArr[index]); bol = false}
			{
				(item[0].asString.find("Master").notNil)
			} {newKeyArr[3] = newKeyArr[3].add(keyArr[index]); bol = false};
			if(bol, {
				newKeyArr[0] = newKeyArr[0].add(keyArr[index]);
			});
		};
		newKeyArr = newKeyArr.collect{|item| item.sort({ arg a, b; a[0] <= b[0] }; ) };
		newKeyArr = newKeyArr.reshapeLike(keyArr);
		^newKeyArr;
	}

	routingMap {var arr, labelArr, labelArr2, globArr, globArr2, globArr3, indices,
		thisArr, stringArr, selectString, thisIndex, thisString, indArr;
		arr = this.routePairsSort;
		labelArr = Array.fill(
			arr.collect{|item| arr.flop[0].indicesOfEqual(item[1]).size;}.maxIndex, nil);
		arr.do{|item|
			labelArr = Array.fill(
				arr.collect{|item| arr.flop[0].indicesOfEqual(item[1]).size;}.maxIndex, nil);
			item.do{|lab|
				labelArr[0] = lab;
				globArr = globArr.add(labelArr);
				labelArr = Array.fill(
					arr.collect{|item| arr.flop[0].indicesOfEqual(item[1]).size;}.maxIndex, nil);
			};
			indices = arr.flop[0].indicesOfEqual(item[1]);
			thisArr = indices.collect{|it| arr.flop[1][it] };
			if(thisArr.notNil, {
				labelArr.do{|item, index|
					labelArr[index] = thisArr[index];
				};
				while ({ labelArr.collect({|item| item == nil }).includes(false) }, {
					globArr = globArr.add(labelArr);
					labelArr2 = labelArr.collect{|lab|
						arr.flop[0].indicesOfEqual(lab);
					};
					labelArr = labelArr2.collect{|lab|
						if(lab.notNil, {
							lab.collect{|lab2|
								arr[lab2][1];
							};
						});
					}.flat;
				});
			});
			globArr2 = globArr2.add(globArr);
			globArr = [];
		};
		globArr2.do{|globArr|
			var newArr, newArr2, newArr3, newArrMod, switch, newArr4;
			newArr = globArr.flop.select{|item| item.includes(\master)};
			newArr2 = newArr.flop;
			newArr2.do{|item, index|
				var thisNils, replace;
				if(item.includes(nil), {
					thisNils = item.indicesOfEqual(nil);
					replace = item.select({|it| it.notNil });
					thisNils.do{|ind| newArr2[index][ind] = replace[0] };
				});
			};
			newArr3 = newArr2.flop;
			newArrMod = [];
			newArr3.do{|item, index|
				switch = true;
				item.do{|it| if(it == \master, {switch = false});
					if(switch, {newArrMod = newArrMod.add(it);
					}, {
						newArrMod = newArrMod.add(\master);
					});
				};
			};
			newArr4 = newArrMod.reshapeLike(newArr3);
			newArr4.do{|item| item.rejectSame };
			newArr4.do{|item|
				globArr3 = globArr3.add(item);
			};
		};
		stringArr = globArr3.collect{|item|
			item.asString.replace("[", "").replace("]", "").replace(", ","->").replace(" ", "");
		};
		selectString = [];
		(stringArr.size-1).do{|num|
			thisIndex = num;
			thisString = stringArr[thisIndex];
			indArr =	stringArr.collect{|item, index| if(item.find(thisString).notNil, {index}); };
			indArr =	indArr.select({|item| item.notNil }).size;
			if(indArr == 1, { selectString = selectString.add(stringArr[thisIndex]) });
		};
		^selectString;
	}

	routingSelMap {arg filter1="", filter2="", filter3="", filter4="";
		var selectString2, selectString3;
		this.routingMap.do{|item| selectString2 = selectString2.add(
			item.replace(">", "").split($-).select({|it|
				(it.find(filter1).isNil).and(it.find(filter2).isNil)
				.and(it.find(filter3).isNil).and(it.find(filter4).isNil) })
		) };
		selectString3 = selectString2.collect{|item|
			item.asString.replace("[", "").replace("]", "").replace(", ","->").replace(" ", "");
		};
		^selectString3.rejectSame;
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

	collectSpecArr {arg find=\track;
		var arr;
		specs.do{|it, in|
			it.do{|item, index| if(item[0].asString.find(find.asString).notNil, {arr = arr.add(item)}); }
		};
		^arr;
	}

	getSpec {arg trackName = \master, argument = \volume;
		specs.do{|item|
			item.do{|it| if(it[0] == trackName, {
				it[1].do{|thisIt|
					if(thisIt[0] == argument, { ^thisIt[1]; }); };
			}); };
		};
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
			//test this
			space[space.flop[0].indexOf(trackName)] = [trackName, spaceType];

			/*space.do{|item| if(item[0] == trackName, {item[1] = chanNum; item[2] = spaceType }) };*/
		}, {
			"track name not found".warn;
		});
	}

	input {arg ndefsIn, type=\track, num=1, respace=true, spaceType;
		var trackArr, ndefCS, connect, inTag, newInIndex;
		if([\track, \bus, \master].includes(type), {
			trackArr = this.get(type)[num-1];
			if(type == \master, {inTag = type}, {inTag = (type ++ num).asSymbol});
			if(inputs.notNil, {
				if(inputs.flop[0].includes(inTag), {
					inputs.removeAt(inputs.flop[0].indexOf(inTag));
				});
			});
			inTag = ("space" ++ inTag.asString.capitalise).asSymbol;
			if(inputs.notNil, {
				if(inputs.flop[0].includes(inTag), {
					newInIndex = inputs.flop[0].indexOf(inTag);
					inputs[newInIndex.asInteger] = [inTag, ndefsIn];
				}, {
					inputs = inputs.add([inTag, ndefsIn]);
				});
			}, {
				inputs = inputs.add([inTag, ndefsIn]);
			});
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
		}, {
			"wrong track type".warn;
		});
	}

	getTrackInput {arg type=\master, num=1;
		var inTag, index, trackTag;
		if(type == \master, {inTag = ("space" ++ type.asString.capitalise).asSymbol;
			trackTag = type;
		}, {inTag = (("space" ++ type.asString.capitalise) ++ num).asSymbol;
			trackTag = (type ++ num).asSymbol;
		});
		if(trackNames.includes(trackTag), {
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

	getTrackDestination {arg type=\master, num=1;
		var key, ind;
		ind = [];
		key = (type ++ num).asSymbol;
		inputs.flop[1].do{|item, index|
			if(item.isArray, {
				if(item.includes(Ndef(key) ), {
					ind = ind.add(index)});
			}, {
				if(item == Ndef(key), { ind = ind.add(index) });
		});  };
		if(ind.notNil, {
			if(ind.size == 1, {
				^Ndef(inputs.flop[0][ind[0]]);
			}, {
				^inputs.flop[0].atAll(ind);
			});
		}, {
			"destination not found".warn;
		});
	}

	bus {arg trackNum=1, busNum=1, mix=1, trackType=\track, action;
		var numChan, busTag, ndefCS1, ndefCS2, ndefCS3, funcBus, thisBusArr,
		busAdd, argIndex;
		{
			numChan = Ndef(this.getBuses.flop[0][busNum-1][0]).numChannels;
			busTag = ("busIn" ++ busNum).asSymbol;
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
				this.getTrackOutput(trackType, trackNum));};
			thisBusArr = busArr[busNum-1][1];
			if(thisBusArr.notNil, {
				if(thisBusArr.includes(this.getTrackOutput(trackType, trackNum)).not, {
					busAdd.()
				});
			}, {
				busAdd.();
			});
			if(inputs.notNil, {
				if(inputs.flop[0].includes(busTag), {
					inputs.removeAt(inputs.flop[0].indexOf(busTag));
				});
			});
			if(busArr[busNum-1][1].size == 1, {
				inputs = inputs.add( [busTag, busArr[busNum-1][1][0]]);
			}, {
				inputs = inputs.add( [busTag, busArr[busNum-1][1]]);
			});
			funcBus = busArr[busNum-1][1].busFunc;
			ndefCS2 = this.ndefPrepare(Ndef(busTag), funcBus);
			server.sync;
			ndefCS2.radpost;
			ndefCS2.interpret;
			server.sync;
			argIndex = busArr[busNum-1][1].indexOf(Ndef((trackType.asString ++ trackNum).asSymbol));
			ndefCS3 = "Ndef(" ++ busTag.cs ++ ").set('vol" ++ (argIndex+1)
			++ "', " ++ mix ++ ");";
			ndefCS3.radpost;
			ndefCS3.interpret;
			action.();
		}.fork;
	}

	removeBus {arg trackNum= 1, busNum=1, trackType=\track;
		var oldLabel, newBusInd, newLabelArr, spaceBusNum, spaceBusLabel,
		spaceInNdef, spaceInSel, spaceInInd, thisBusIndLabel;
		oldLabel = ("busIn" ++ busNum).asSymbol;
		spaceBusLabel = ("spaceBus" ++ busNum).asSymbol;
		spaceInInd = inputs.flop[0].indexOf(spaceBusLabel);
		if(spaceInInd.notNil, {
			newBusInd = busArr.flop[0].indexOf(oldLabel);
			newLabelArr = busArr.flop[1][newBusInd];
			newLabelArr = newLabelArr.select({|item|
				item.key != (trackType.asString ++ trackNum).asSymbol;
			});
			busArr[newBusInd][1] = newLabelArr;
			thisBusIndLabel = inputs.flop[0].indexOf(oldLabel);
			if(newLabelArr.isEmpty, {
				("Ndef(" ++ oldLabel.cs ++ ").source = nil;").radpost.interpret;
				busArr[newBusInd] = [nil,nil];
				inputs.removeAt(thisBusIndLabel);
				spaceInInd = inputs.flop[0].indexOf(spaceBusLabel);
				if(inputs.flop[1][spaceInInd].isArray.not, {
					("Ndef(" ++ spaceBusLabel.cs ++ ").source = nil;").radpost.interpret;
					inputs.removeAt(spaceInInd);
				});
			}, {
				if(newLabelArr.size == 1, {
					inputs[thisBusIndLabel][1] = newLabelArr[0];
				}, {
					inputs[thisBusIndLabel][1] = newLabelArr;
				});
				("Ndef(" ++ oldLabel.cs ++ ", " ++ newLabelArr.busFunc.cs ++ ");").radpost.interpret;
			});
			spaceInNdef = inputs.flop[1][spaceInInd];
			if(spaceInNdef.isArray, {
				spaceInSel = spaceInNdef.select{|item| item.key !=  oldLabel};
				if(spaceInSel.size == 1, {
					this.input(spaceInSel[0], \bus, busNum);
				}, {
					this.input(spaceInSel, \bus, busNum);
				});
			});
		}, {
			"Bus not found".warn;
		});
	}

	filter {arg type=\track, num= 1, slot=1, filter=\pch, extraArgs, buffer, data;
		var filterTag, ndefArr, ndefCS, arr1, arr2, arr3, arrSize, filterInfo, setArr,
		setTag, filterIndex, startNdefs, filterSpecs, trackTags, convString,
		replaceString, cond, bufIndex, bufFunc, ndefNumChan;

		//still some work to do with buffer alloc
		{
			if(type == \master, {
				filterTag = ("filter" ++ type.asString.capitalise ++ "_" ++ slot).asSymbol;
				num = 1;
			}, {
				filterTag = ("filter" ++ type.asString.capitalise ++ "_" ++ num ++ "_" ++ slot).asSymbol;
			});

			if(filterBuff.notNil, {
				bufIndex = filterBuff.flop[0].indicesOfEqual(filterTag);

				if(bufIndex.notNil, {
					bufFunc = {
						(nodeTime+fadeTime).yield;
						bufIndex.do{|index|
							filterBuff.flop[1][index].do{|item|
								BStore.removeID(item);
								server.sync;
							};
							filterBuff.removeAt(index);
						};
					};
				});
			}, {
				bufFunc = {};
			});

			ndefArr = this.get(type)[num-1];
			ndefNumChan = Ndef(ndefArr[0][0]).numChannels;
			startNdefs = {
				ndefCS = "Ndef.ar(" ++ filterTag.cs ++ ", " ++ ndefNumChan ++ ");";
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
			filterSpecs = [filterTag, SpecFile.read(\filter, \pch)];
			cond = Condition(false);
			cond.test = false;
			if(data.notNil, {
				if(data[0] == \convrev, {
					convString = filterInfo[1].cs;
					this.convRevBuf(filterTag, data[1], data[2], data[3], {|string|
						replaceString = string;
						if(convString.find("\\convrev").notNil, {
							convString = convString.replace("\\convrev", string);
							filterInfo[1] = convString.interpret;
						});
						cond.test = true;
						cond.signal;
					}, ndefNumChan);
				});
			}, {
				cond.test = true;
				cond.signal;
			});

			cond.wait;

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
					arr2.sort({arg a, b; a[0] <= b[0] });

					filters = filters.add([filterTag, filter]);
					filters.sort({arg a, b; a[0] <= b[0] });
				});

				arr1.insert(1, arr2);
				arrSize = (arr1.flat.size/2);
				arr1 = arr1.reshape(arrSize.asInteger, 2)

			}, {

				filters = filters.add([filterTag, filter]);
				filters.sort({arg a, b; a[0] <= b[0] });

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
			masterNdefs[setArr[0]] = arr1.flop[0].collect({|item| Ndef(item)});

			trackTags = specs[setArr[0]].flop[0];
			specs[setArr[0]] = arr1.flop[0].collect{|item|
				if(trackTags.includes(item), {
					specs[setArr[0]][trackTags.indexOf(item)];
				}, {
					filterSpecs;
				});
			};

			this.autoRoute(arr1, true);
			server.sync;
			bufFunc.();
		}.fork;
	}

	removeFilter {arg type=\track, num= 1, slot=1;
		var thisTrack, thisSlot, ndefCS, setArr, bufArrInd;
		thisTrack = this.get(type)[num-1];
		if(thisTrack.size > 2, {
			if(slot < (thisTrack.size-1), {
				thisSlot = thisTrack[slot];
				ndefCS = "Ndef(" ++ thisSlot[0].cs ++ ").clear(" ++ fadeTime ++ ");";
				ndefCS.radpost;
				ndefCS.interpret;
				thisTrack.removeAt(slot);
				if(type == \master, {num=""});
				setArr = this.findTrackArr((type ++ num).asSymbol);
				masterNdefs[setArr[0]].removeAt(slot);
				specs[setArr[0]].removeAt(slot);
				{
					fadeTime.wait;
					bufArrInd = filterBuff.flop[0].indexOf(thisSlot[0]);
					filterBuff.flop[1][bufArrInd].do{|item|
						server.sync;
						BStore.remove(item[0], item[1], item[2]);
					};
				}.fork;
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
				masterNdefs[setArr[0]].remove(Ndef(item[0]));
				specs[setArr[0]] = specs[setArr[0]].reject({|it| it[0] == item[0] });
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

	//this is not working - work on removing tracks (and GUI update)
	remove {arg track=1;
		var trackIndex;
		trackIndex = track - 1;
		if((track >= 1).and(track <= tracks.size), {
			masterNdefs[trackIndex].clear;
			masterNdefs.removeAt(trackIndex);

			tracks.removeAt(trackIndex);
			specs.removeAt(trackIndex);
			trackNames.removeAt(trackIndex);
			space.removeAt(trackIndex);
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
				masterNdefs[item-1].clear;
			}, {
				"track Number not Found".warn;
			});
		};
		masterNdefs.removeAtAll(newArr);
		tracks.removeAtAll(newArr);
		specs.removeAtAll(newArr);
		trackNames.removeAt(newArr);
		space.removeAt(newArr);
		livetracks.removeAll(newArr);
	}

	removeAll {
		masterNdefs.do{|item| item.clear };
		masterNdefs = [];

		tracks = [];
		specs = [];
		trackNames = [];
		space = [];
		livetracks = [];
		trackCount = 1;
	}

	clear {
		Ndef.clear;
		masterNdefs = [];
		tracks = [];
		specs = [];
		trackNames = [];
		space = [];
		livetracks = [];
		trackCount = 1;
	}

	setFilterTag {arg filterTag, arg1, arg2, post=\code;
		var ndefCS;
		ndefCS = "Ndef(" ++ filterTag.cs ++ ").set(" ++ arg1.cs ++ ", " ++ arg2.cs ++ ")";
		ndefCS.interpret;
		if(post.notNil, {
			case
			{post == \code} {ndefCS.radpost}
			{post == \spec} {[arg1, arg2].cs.post;};
		});
	}

	filterInfoToTag {arg filterInfo;
		var filterTag;
		case
		{filterInfo.isNumber} {
			filterTag = this.getFilterTag(filterInfo);
		}
		{filterInfo.isSymbol} {
			filterTag = filterInfo;}
		{filterInfo.isArray} {
			if(filterInfo.size > 2, {
				filterTag = this.findFilterTag(filterInfo[0], filterInfo[1], filterInfo[2]);
			}, {
				filterTag = this.findFilterTag(filterInfo[0], filterInfo[1]);
			});
		};
		^filterTag;
	}

	setFilter {arg filterInfo, arg1, arg2, post=\code;
		var filterTag, key;
		filterTag = this.filterInfoToTag(filterInfo);
		if(filterTag.notNil, {
			if(arg1.isNumber, {
				key = this.getFilterKeys(filterTag)[arg1];
			}, {
				key = arg1;
			});
			this.setFilterTag(filterTag, key, arg2, post);
		}, {
			"filter info not found".warn;
		});
	}

	setFilterSpec {arg filterInfo, arg1, arg2, mul=1, add=0, min, val, warp, post=\code;
		var filterTag, specArr, thisSpec, modSpec, thisVal;
		filterTag = this.filterInfoToTag(filterInfo);
		specArr = this.collectSpecArr(filterTag)[0][1];
		if(arg1.isNumber, {
			thisSpec =	specArr[arg1];
		}, {
			thisSpec = 	specArr[specArr.flop[0].indexOf(arg1)];
		});
		thisSpec.postln;
		modSpec = thisSpec[1].specFactor(mul, add, min, val, warp);
		thisVal = thisSpec[2].(modSpec.asSpec.map(arg2));
		this.setFilterTag(filterTag, thisSpec[0], thisVal, post);
	}

	findFilterTag {arg type, arg1, arg2;
		var tagString, tagIndex;
		if(type == \master, {
			tagString = "filter" ++ type.asString.capitalise ++ "_" ++ arg1;
		}, {
			tagString = "filter" ++ type.asString.capitalise ++ "_" ++ arg1 ++ "_" ++ arg2;
		});
		tagString = tagString.asSymbol;
		tagIndex = filters.flop[0].indexOf(tagString);
		if(tagIndex.notNil, {
			^filters.flop[0][tagIndex]
		}, {
			"filter slot is not active".warn;
			^nil;
		});
	}

	convFilterTag {arg filterTag;
		var keyString, resultArr, varArr;
		keyString = filterTag.asString.replace("filter").toLower;
		varArr =  keyString.asString.split($_);
		if(varArr.size == 2, {
			resultArr = [varArr[0].asSymbol, 1, varArr[1] ];
		}, {
			resultArr = [varArr[0].asSymbol, varArr[1], varArr[1] ];
		});
		^resultArr;
	}

	getFilterTag {arg filterIndex=0;
		^filters[filterIndex][0];
	}

	getFilterKeys {arg filterTag, type=\args;
		var result;
		Ndef(filterTag).cleanNodeMap;
		case
		{type == \args} { result = Ndef(filterTag).controlKeys; }
		{type == \vals} { result = Ndef(filterTag).controlKeysValues;}
		{type == \pairs} { result = Ndef(filterTag).getKeysValues;};
		^result;
	}

	getFilterArgs {arg type, num, slot;
		var filterTag;
		filterTag = this.findFilterTag(type, num, slot);
		^this.getFilterKeys(filterTag, \args);
	}

	getFilterVals {arg type, num, slot;
		var filterTag;
		filterTag = this.findFilterTag(type, num, slot);
		^this.getFilterKeys(filterTag, \vals);
	}

	getFilterPairs {arg type, num, slot;
		var filterTag;
		filterTag = this.findFilterTag(type, num, slot);
		^this.getFilterKeys(filterTag, \pairs);
	}

	modFilterTag {arg filterTag, argument, modType=\sin, spec=[-1,1], extraArgs,
		func, mul=1, add=0, min, val, warp, lag;
		var key, argArr, modMap;
		if(argument.isNumber, {
			key = this.getFilterKeys(filterTag, \args)[argument];
		}, {
			key = argument;
		});
		modMap = ModMap.map(Ndef(filterTag), key, modType, spec, extraArgs,
			func, mul, add, min, val, warp, lag);
	}

	modFilterIndex {arg filterIndex, argument, modType=\sin, spec=[-1,1], extraArgs,
		func, mul=1, add=0, min, val, warp, lag;
		var filterTag;
		filterTag = this.getFilterTag(filterIndex);
		this.modFilterTag(filterTag, argument, modType, spec, extraArgs, func,
			mul, add, min, val, warp, lag);
	}

	modFilterIndexCode {arg type, num, slot, argument, modType=\sin, spec=[-1,1],
		extraArgs, func, mul=1, add=0, min, val, warp, lag;
		var filterTag;
		filterTag = this.findFilterTag(type, num, slot);
		this.modFilterTag(filterTag, argument, modType, spec, extraArgs, func,
			mul, add, min, val, warp, lag);
	}

	modFilter {arg filterInfo, argument, modType=\sin, extraArgs,
		mul=1, add=0, min, val, warp, lag;
		var filterTag, specArr, thisSpec, argIndex;
		if(filterInfo.isNumber, {
			filterTag = this.getFilterTag(filterInfo);
		}, {
			filterTag = filterInfo;
		});
		specArr = this.collectSpecArr(filterTag)[0][1];
		if(argument.isNumber, {
			argIndex = this.getFilterKeys(filterTag, \args)[argument];
		}, {
			argIndex = argument;
		});
		thisSpec = 	specArr[specArr.flop[0].indexOf(argIndex)];
		/*thisSpec.postln;*/
		this.modFilterTag(filterTag, thisSpec[0], modType, thisSpec[1], extraArgs,
			thisSpec[2], mul=1, add=0, min, val, warp, lag);
	}

	modFilterCode {arg type, num, slot, argument, modType=\sin, extraArgs,
		mul=1, add=0, min, val, warp, lag;
		var filterTag;
		filterTag = this.findFilterTag(type, num, slot);
		this.modFilter(filterTag, argument, modType, extraArgs,
			mul, add, min, val, warp, lag);
	}

	/*	savePresetFilter {arg filterTag;

	}*/

	convRevBuf {arg filterTag, impulse=\ortf_s1r1, fftsize=2048, inVar,
		action={|val| val.radpost}, chanIn, globBuf;
		var path, buffArr, file, numChan, irbuffer, irArr, bufsize, numtag,
		string, filterBuffArr, wcond;
		{
			wcond = Condition.new(false);
			inVar ?? {inVar = "input"};
			path = mainPath ++ "SoundFiles/IR/" ++ impulse ++ ".wav";
			file = SoundFile.new;
			file.openRead(path);
			numChan = file.numChannels;
			file.close;
			numChan.do{|index|
				wcond.test = false;
				BStore.add(\ir, [impulse, [index]], {|buf|
					irbuffer = buf;
					irArr = irArr.add(irbuffer);
					bufsize= PartConv.calcBufSize(fftsize, irbuffer);
					numtag = (\alloc++BStore.allocCount).asSymbol;
					filterBuffArr = filterBuffArr.add([\alloc, numtag, [bufsize] ]);
					BStore.addRaw(\alloc, numtag, [bufsize], {|buf|
						buf.preparePartConv(irbuffer, fftsize);
						buffArr = buffArr.add(buf);
						BStore.allocCount = BStore.allocCount + 1;
						wcond.test = true;
						wcond.signal;
					});
				});
				wcond.wait;
			};
			server.sync;
			irArr.do{|item|
				BStore.removeByIndex(BStore.bstores.indexOf(item));
				server.sync; };
			filterBuff = filterBuff.add([filterTag, filterBuffArr]);
			string = "[";
			buffArr.do{|item, index|
				globBuf = BufferSystem.getGlobVar(item);
				if(chanIn == 1, {
					string = string ++ ("PartConv.ar(" ++ inVar ++ ", " ++ fftsize ++ ", "
						++ globBuf ++ "),");
				}, {
					string = string ++ ("PartConv.ar(" ++ inVar ++ "[" ++ index ++ "], "
						++ fftsize ++ ", " ++ globBuf ++ "),");
				});
			};
			string = string.copyRange(0, string.size-2);
			string = string ++ "]/" ++ numChan ;
			action.(string.cs.interpret);
		}.fork;
	}

	sortTrackNames {arg trackNameArr;
		var newSortArr = [[],[],[],[]];
		trackNameArr.do{|item, index|
			var bol;
			bol = true;
			case
			{
				(item.asString.find("track").notNil).or(item.asString.find("Track").notNil)
			} {newSortArr[1] = newSortArr[1].add(trackNameArr[index]); bol = false}
			{
				(item.asString.find("bus").notNil).or(item.asString.find("Bus").notNil)
			} {newSortArr[2] = newSortArr[2].add(trackNameArr[index]); bol = false}
			{
				(item.asString.find("master").notNil).or(item.asString.find("Master").notNil)
			} {newSortArr[3] = newSortArr[3].add(trackNameArr[index]); bol = false};
			if(bol, {
				newSortArr[0] = newSortArr[0].add(trackNameArr[index]);
			});
		};
		^newSortArr.flat;
	}

	getBusInLabels {var busLabelArr, sameTracks, result;
		busArr.do{|item|
			if(item[1].notNil, {
				if(item[1].isArray, {
					item[1].do{|it|
						busLabelArr = busLabelArr.add([it.key, item[0]]);
					};
				}, {
					busLabelArr = busLabelArr.add([item[1].key, item[0]]);
				});
			});
		};
		if(busLabelArr.notNil, {
			sameTracks = busLabelArr.flop[0].rejectSame;
			result = sameTracks.collect{|item| [item, busLabelArr.flop[1].atAll(
				busLabelArr.flop[0].indicesOfEqual(item)) ] };
		}, {result = nil});
		^result;
	}

	updateMixInfo {
		mixTrackNames = this.sortTrackNames(trackNames);
		mixTrackNdefs = mixTrackNames.collect({|item| Ndef(item.asSymbol) });
		sysChans = mixTrackNdefs.collect({|item| item.numChannels});
		mixTrackNames.do{|item|
			var spatialType;
			spatialType = space.flop[1][space.flop[0].indexOf(("space" ++ item.asString.capitalise).asSymbol)];
			if([\bal2, \pan2].includes(spatialType), {sysPan = sysPan.add(0); }, {sysPan = sysPan.add(1) });
		};
	}

	mixGUI {arg updateFreq=10;
		var sends, fxsNum, knobColors, winHeight,
		winWidth, knobSize, canvas, panKnobTextArr, panKnobArr, sliderTextArr, sliderArr, levelTextArr,
		levelArr, vlay, sendsMenuArr, sendsKnobArr, inputMenuArr, outputMenuArr, fxSlotArr, trackLabelArr,
		spaceTextLay, popupmenusize, slotsSize, panSpec, mixInputLabels,
		trackInputSel, inputArray, numBuses, thisInputLabel, busInLabels, maxBusIn,
		knobFunc, busInSettings;

		this.updateMixInfo;
		//getting input label data
		inputArray = 	inputs.flop[0].collect{|item| item.asString.replace("space").toLower.asSymbol; };
		mixTrackNames.do{|item|
			if(inputArray.includes(item), {
				thisInputLabel = inputs.flop[1][inputArray.indexOf(item)];
				if(thisInputLabel.isArray, {thisInputLabel = thisInputLabel.collect{|item| item.key};
				}, {
					thisInputLabel = thisInputLabel.key;});
				if(item != \master, {
					trackInputSel = trackInputSel.add([mixTrackNames.indexOf(item),
						thisInputLabel ]); });
			});
		};

		busInLabels = this.getBusInLabels;
		if(busInLabels.isNil, {
			sends = 2;
		}, {
			maxBusIn = busInLabels.flop[1].collect{|item| item.size}.maxItem;
			if(maxBusIn >= 2, {
				sends = maxBusIn + 1;
			}, {
				sends = 2;
			});
		});

		fxsNum = 2;

		numBuses = trackNames.select{|item| item.asString.find("bus").notNil }.size+1;

		knobColors = [ Color(0.91764705882353, 0.91764705882353, 0.91764705882353),
			Color.white, Color.black, Color() ];

		winHeight = 478 + ((sends-2)*15);
		winWidth = (43*(sysChans.sum));
		if(sysPan.includes(1), {knobSize = 40;}, {knobSize = 30; });
		if(mixerWin.isNil, {
			mixerWin = ScrollView(bounds: (Rect(0, 0, winWidth,winHeight))).name_("Assemblage");
			mixerWin.hasVerticalScroller = false;
		});
		if(mixerWin.bounds != Rect(0, 0, winWidth,winHeight), {
			mixerWin.bounds = Rect(0, 0, winWidth,winHeight);
		});
		/*mixerWin.fixedHeight = winHeight;*/
		canvas = View();
		canvas.background_(Color.black);

		sysChans.do{|item, index|
			var slider, level, sliderText, levelText, hlay, thisLay, ts, finalLayout,
			panKnob, panKnobText, panKnobText1, panKnobText2, outputMenu, outputLabel,
			sendsMenu, sendsLabel, sendsKnobs, sendsLay, inputMenu, inputLabel, fxLabel, fxSlot,
			trackLabel, trackColor, thisInputVal;
			//volume slider
			sliderText = StaticText(canvas).align_(\center)
			.background_(Color.black).stringColor_(Color.white)
			.minWidth_(24).maxWidth_(24).maxHeight_(10).minHeight_(10);
			sliderTextArr = sliderTextArr.add(sliderText);
			slider = Slider().minWidth_(20).maxWidth_(20).maxHeight_(180).minHeight_(180)
			.focusColor_(Color.red(alpha:0.2))
			.background_(Color.black);

			//levelIndicator
			levelText = StaticText(canvas).align_(\center)
			.background_(Color.new255(78, 109, 38)).stringColor_(Color.white)
			.minWidth_(24).maxHeight_(10).minHeight_(10);
			levelTextArr = levelTextArr.add(levelText);
			item.do({
				level = level.add(LevelIndicator()
					.drawsPeak_(true)
					/*	.style_(\led)*/
					.warning_(0.9)
					.critical_(0.9999)
					.minWidth_(10)
					.maxWidth_(10)
				);
			});
			hlay = HLayout(sliderText, levelText);

			//panning knob(s)/slider2D(s)
			if(sysPan[index] == 0, {
				panKnob = Knob().minWidth_(knobSize).maxWidth_(knobSize)
				.maxHeight_(knobSize).minHeight_(knobSize).centered_(true);
				panKnob.color = knobColors;
				panKnobText = StaticText(canvas).align_(\center).background_(Color.black)
				.stringColor_(Color.white).minWidth_(24).maxWidth_(24).maxHeight_(10).minHeight_(10);
				panKnobText = [panKnobText];
			}, {
				panKnob = Slider2D().minWidth_(knobSize).maxWidth_(knobSize)
				.maxHeight_(knobSize).minHeight_(knobSize);
				panKnob.x = 0.5;
				panKnob.y = 0.5;

				panKnobText1 = StaticText(canvas).align_(\center).background_(Color.black)
				.stringColor_(Color.white).minWidth_(24).maxWidth_(24).maxHeight_(10).minHeight_(10);

				panKnobText2 = StaticText(canvas).align_(\center).background_(Color.black)
				.stringColor_(Color.white).minWidth_(24).maxWidth_(24).maxHeight_(10).minHeight_(10);

				panKnobText = [panKnobText1, panKnobText2];
			});

			panKnobTextArr = panKnobTextArr.add(panKnobText);
			spaceTextLay = HLayout(*panKnobText);
			panKnobArr = panKnobArr.add(panKnob);

			popupmenusize = 14;

			slotsSize = 68;

			//output label
			if(index != (sysChans.size-1), {
				outputLabel = StaticText(canvas).align_(\center).background_(Color.black)
				.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
				outputLabel.font = Font("Monaco", 8); outputLabel.string_("Output");
				//output menu
				outputMenu = PopUpMenu().maxHeight_(popupmenusize)
				.minHeight_(popupmenusize).minWidth_(slotsSize).maxWidth_(slotsSize);
				outputMenu.items = ["", "master"] ++numBuses.collect{|item| "bus" ++ (item+1)};
				outputMenu.background_(Color.black).stringColor_(Color.white)
				.font_(Font("Monaco", 8));
				outputMenuArr = outputMenuArr.add(outputMenu);

				//sends
				sendsLabel = StaticText(canvas).align_(\center).background_(Color.black)
				.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
				sendsLabel.font = Font("Monaco", 8); sendsLabel.string_("Sends");
				//sends menu
				sends.do{var smenu, sknob;
					smenu = PopUpMenu().maxHeight_(popupmenusize).minHeight_(popupmenusize)
					.maxWidth_(slotsSize);
					smenu.items = [""] ++numBuses.collect{|item| "bus" ++ (item+1)};
					smenu.background_(Color.black).stringColor_(Color.white)
					.font_(Font("Monaco", 8));
					sknob = Knob().minWidth_(popupmenusize).maxWidth_(popupmenusize)
					.maxHeight_(popupmenusize).minHeight_(popupmenusize);
					sknob.color = knobColors;

					sendsMenu = sendsMenu.add(smenu);
					sendsKnobs = sendsKnobs.add(sknob);
					sendsLay = sendsLay.add(HLayout(smenu, sknob));
				};
				sendsMenuArr = sendsMenuArr.add(sendsMenu);
				sendsKnobArr = sendsKnobArr.add(sendsKnobs);
			});
			//audio fxs
			fxLabel = StaticText(canvas).align_(\center).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			fxLabel.font = Font("Monaco", 8); fxLabel.string_("Audio FX");
			//fx buttons
			fxsNum.do{var fxbutton;
				fxbutton = Button().maxHeight_(popupmenusize).minHeight_(popupmenusize)
				.minWidth_(slotsSize).maxWidth_(slotsSize);
				fxbutton.canFocus = false;
				fxbutton.states_([["", Color.white, Color.black]])
				.font_(Font("Monaco", 8));
				/*.mouseDownAction = { arg menu;
					var boundArr, thisBounds, thisArrBounds, thisWindow, thisitemArr,
					thisListView, screenBounds;
					screenBounds = Window.screenBounds.bounds.asArray.last;
					boundArr = fxbutton.bounds.asArray;
					thisBounds = 	Rect(boundArr[0], (screenBounds-boundArr[1]-285), 140, 240);
					thisArrBounds = thisBounds.asArray;
					if(menu.string == "", {
					thisWindow = Window.new("", thisBounds, border: false).front;
					thisWindow.background_(Color.black);
					thisitemArr = ([""] ++ SynthFile.read(\filter) );
					thisListView = ListView(thisWindow,Rect(0,0,(thisArrBounds[2]),(thisArrBounds[3])))
					.items_(thisitemArr)
					.background_(Color.clear)
					.font_(Font("Monaco", 10);)
					.stringColor_(Color.white)
					.hiliteColor_(Color.new255(78, 109, 38);)
					.action_({ arg sbs;
						/*~thisFilter = thisListView.items[sbs.value];*/
						thisListView.items[sbs.value].postln;
						menu.string = thisListView.items[sbs.value];
						thisWindow.close;
					});
					}, {
						"selected filter: ".post;
						menu.string.postln;
					});
				};*/
				fxSlot = fxSlot.add(fxbutton);
			};
			fxSlotArr = fxSlotArr.add(fxSlot);

			//input label
			inputLabel = StaticText(canvas).align_(\center).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			inputLabel.font = Font("Monaco", 8); inputLabel.string_("Input");
			//input menu
			inputMenu = PopUpMenu().maxHeight_(popupmenusize)
			.minHeight_(popupmenusize).minWidth_(slotsSize).maxWidth_(slotsSize);

			if(mixTrackNames[index].asString.find("track").notNil, {
				inputs.flop[1].flat.do({|item| if((item.key.asString.find("busIn").isNil)
					.and(item.key.asString.find("track").isNil), {
						mixInputLabels =	mixInputLabels.add(item.key);
				});
				});
				inputMenu.items = [""] ++ this.sortTrackNames(mixInputLabels.rejectSame)
				++ numBuses.collect{|item| "bus" ++ (item+1)};
				//input names
				if(trackInputSel.notNil, {
					if(trackInputSel.flop[0].includes(index), {
						thisInputLabel = trackInputSel.flop[1][index];
						thisInputVal = inputMenu.items;
						if(thisInputLabel.isArray, {
							inputMenu.items = thisInputVal.insert(1, thisInputLabel.asString);
							inputMenu.value = 1;
						}, {
							inputMenu.value = thisInputVal.indexOf(trackInputSel.flop[1][index]);
						});
					});
				});
			}, {
				inputMenu.items = [mixTrackNames[index].asString]
			});

			inputMenu.background_(Color.black).stringColor_(Color.white)
			.font_(Font("Monaco", 8)).action = { arg menu;
				[menu.value, menu.item].postln;
			};

			inputMenuArr = inputMenuArr.add(inputMenu);

			sliderArr = sliderArr.add(slider);
			levelArr = levelArr.add(level);
			ts = [slider] ++ level;
			thisLay = HLayout(*ts);

			//track name label
			case
			{mixTrackNames[index].asString.find("track").notNil} {trackColor = Color.new255(58, 162, 175)}
			{mixTrackNames[index].asString.find("bus").notNil} {trackColor = Color.new255(132, 124, 10)}
			{mixTrackNames[index].asString.find("master").notNil} {trackColor = Color.new255(102, 57, 130)};
			trackLabel = StaticText(canvas).align_(\center).background_(trackColor)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			trackLabel.font = Font("Monaco", 8);
			trackLabel.string_(mixTrackNames[index].asString.capitalise);
			trackLabelArr = trackLabelArr.add(trackLabel);

			//input
			[[inputLabel, align: \bottom], [inputMenu, align: \bottom]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			//audio fx
			finalLayout = finalLayout.add([fxLabel, align: \center]);
			fxSlot.do{|thisSlot|
				finalLayout = finalLayout.add([thisSlot, align: \center]);
			};
			//sends
			finalLayout = finalLayout.add([sendsLabel, align: \bottom]);
			sendsLay.do{|smenu|
				finalLayout = finalLayout.add([smenu, align: \bottom]);
			};
			//output
			[[outputLabel, align: \bottom], [outputMenu, align: \bottom]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			//spatialisation interface
			[[spaceTextLay, align: \bottom], [panKnob, align: \bottom]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			//slider and levelIndicators
			[[hlay, align: \center], [thisLay, align: \center]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			//track names
			finalLayout = finalLayout.add([trackLabel, align: \below]);
			vlay = vlay.add(VLayout(*finalLayout) );
		};

		//setting interface
		sliderTextArr.do{|item, index| item.font = Font("Monaco", 8); item.string_(
			mixTrackNdefs[index].getKeysValues.collect({|item|
				if(item[0] == \volume, {item[1]});
		})[0].round(0.1).asString);
		};
		levelTextArr.do{|item| item.font = Font("Monaco", 8); item.string_("-inf");  };
		//panning
		panKnobTextArr.flat.do{|item| item.font = Font("Monaco", 8);};
		panSpec = \pan.asSpec;
		panKnobArr.do{|item, index|
			var panKey, panKeyValues, panValues;
			panKey = ("space" ++ mixTrackNames[index].asString.capitalise).asSymbol;
			panKeyValues = Ndef(panKey).controlKeysValues;
			case
			{panKeyValues.size == 0} {panValues = 0}
			{panKeyValues.size == 2} {panValues = panKeyValues[1]}
			{panKeyValues.size == 4} {panValues = [panKeyValues[1], panKeyValues[3]]};
			if(sysPan[index] == 0, {
				item.value = panSpec.unmap(panValues);
				panKnobTextArr[index][0].string_(panValues);
				item.action = {|val|
					var newPanVal;
					newPanVal = panSpec.map(val.value).round(0.01);
					panKnobTextArr[index][0].string_(newPanVal.asString);
					("Ndef(" ++ panKey.cs ++ ").set('pan', " ++ newPanVal ++ ");").radpostcont.interpret;
				};
			}, {
				item.setXY(panSpec.unmap(panValues[0]), panSpec.unmap(panValues[1]));
				panKnobTextArr[index][0].string_(panValues[0]);
				panKnobTextArr[index][1].string_(panValues[1]);
				item.action_({|sl|
					var newPanVal1, newPanVal2;
					newPanVal1 = panSpec.map(sl.x).round(0.01);
					newPanVal2 = panSpec.map(sl.y).round(0.01);
					panKnobTextArr[index][0].string_(newPanVal1);
					panKnobTextArr[index][1].string_(newPanVal2);
					("Ndef(" ++ panKey.cs ++ ").set('panx', " ++ newPanVal1 ++ ");").radpostcont.interpret;
					("Ndef(" ++ panKey.cs ++ ").set('pany', " ++ newPanVal2 ++ ");").radpostcont.interpret;
				});
			});
		};

		sliderArr.do{|item, index|
			var volSpec;
			volSpec = this.getSpec(mixTrackNames[index], \volume).asSpec;
			item.value_(volSpec.unmap(
				mixTrackNdefs[index].getKeysValues.collect({|item|
					if(item[0] == \volume, {item[1]});
				})[0];
			) ).action_({|val|
				sliderTextArr[index].string_(volSpec.map(val.value).round(0.1).asString);
				(mixTrackNdefs[index].cs ++ ".set('volume', " ++
					volSpec.map(val.value) ++ ");").radpostcont.interpret;
		});  };

		setVolSlider = {|index, value|
			var volSpec;
			volSpec = this.getSpec(mixTrackNames[index], \volume).asSpec;
			sliderArr[index].value = volSpec.unmap(value);
			sliderTextArr[index].string_(value.round(0.1).asString);
		};

		setPanKnob = {|index, value, indXY=0|
			if(sysPan[index] == 0, {
				panKnobArr[index].value = panSpec.unmap(value);
				panKnobTextArr[index][0].string_(value);
			}, {
				if(indXY == 0, {
					panKnobArr[index].x_(panSpec.unmap(value));
				}, {
					panKnobArr[index].y_(panSpec.unmap(value));
				});
				panKnobTextArr[index][indXY].string_(value);
			});
		};

		levelArr.do{|it| it.do{|item|
			item.meterColor = Color.new255(78, 109, 38);
			item.warningColor = Color.new255(232, 90, 13);
			item.criticalColor = Color.new255(211, 14, 14);
			item.drawsPeak = true;
		}
		};

		canvas.layout = HLayout(*vlay);
		mixerWin.canvas = canvas;
		mixerWin.front;

		//setting busIns
		if(busInSettings.isNil, {
			busInSettings = (nil!(sendsMenuArr.flat.size-1)).reshapeLike(sendsMenuArr);
		});

		knobFunc = {|it, trackLb, thisKey|
			var thisNdefVal, selArg, thisSpec;
			if(thisKey.notNil, {
				thisNdefVal = Ndef(thisKey).getKeysValues;
				selArg = ("vol" ++ (busArr.flop[1][busArr.flop[0].indexOf(thisKey)].collect{|keyVal|
					keyVal.key}.indexOf(trackLb) + 1)).asSymbol;
				thisSpec = this.getSpec(thisKey.asString.replace("In", "").asSymbol, \volume).asSpec;
				it.value = thisSpec.unmap(thisNdefVal.flop[1][thisNdefVal.flop[0].indexOf(selArg)]);
				it.action = {|val|
					("Ndef(" ++ thisKey.cs ++ ").set(" ++ selArg.cs ++
						", " ++ thisSpec.map(val.value) ++ ");").radpostcont.interpret;
				};
			});
		};

		if(busInLabels.notNil, {
			busInLabels.do{|item|
				var thisTackNameInd;
				thisTackNameInd = mixTrackNames.indexOf(item[0]);
				sendsMenuArr[thisTackNameInd].do{|it, ind|
					var labInArr;
					if(item[1][ind].notNil, {
						it.value = item[1][ind].asString.divNumStr[1];
						busInSettings[thisTackNameInd][ind] = it.item;
					});
				};
				sendsKnobArr[mixTrackNames.indexOf(item[0])].do{|it, ind|
					knobFunc.(it, item[0], item[1][ind]);
				};
			};
		});

		setBusIns = {arg trackInd=0, slotInd=0, busInNum=1;
			var thisMenu, thisString;
			thisMenu = sendsMenuArr[trackInd][slotInd];
			if(busInNum == 0, {
				thisString = "";
			}, {
				thisString = ("bus" ++ busInNum).asString;
			});
			thisMenu.valueAction = thisMenu.items.indexOfEqual(thisString);
		};

		setKnobIns = {arg trackInd=0, slotInd=0, value=0;
			var thisMenu, thisString, thisSpec;
			thisMenu = sendsKnobArr[trackInd][slotInd];
			thisSpec = this.getSpec(mixTrackNames[trackInd], \volume).asSpec;
			thisMenu.value = thisSpec.unmap(value);
		};

		sendsMenuArr.do{|item, index|
			item.do{|it, ind|
				it.action = {arg menu;
					var thisBusItem, thisTrackLabel, thisBusNum;

					thisBusNum = menu.item.divNumStr[1];
					thisTrackLabel = mixTrackNames[index].asString.divNumStr[0].asSymbol;

					if(([''] ++ mixTrackNames).includesEqual(it.item.asSymbol).not, {
						this.autoAddTrack(\bus, systemChanNum, action: {
							{
								this.setSend(thisTrackLabel, index+1, ind+1, thisBusNum);
							}.defer;
						});
					}, {
						thisBusItem = busInSettings[index][ind];
						thisTrackLabel = mixTrackNames[index].asString.divNumStr;
						busInSettings[index][ind] = menu.item;
						if(busInSettings[index].select({|item, index| index != ind})
							.includesEqual(it.item), {
							"This send insert is already assigned to this track".warn;
							sendsMenuArr[index][ind].value = 0;
							sendsKnobArr[index][ind].value = 0;
							sendsKnobArr[index][ind].action = {};
						}, {
							if(((thisBusItem.isNil).or(thisBusItem == "")).not, {
								if(busInSettings[index].includesEqual(thisBusItem).not, {
								//remove
								thisBusNum = thisBusItem.divNumStr[1];
								this.removeBus(thisTrackLabel[1], thisBusNum,
									thisTrackLabel[0].asSymbol);
									});
							});
							if(menu.item != "", {
								thisBusNum = menu.item.divNumStr[1];
								this.bus(thisTrackLabel[1], thisBusNum, -inf,
									thisTrackLabel[0].asSymbol, {
										{knobFunc.(sendsKnobArr[index][ind],
											mixTrackNames[index],
											("busIn" ++ thisBusNum).asSymbol);
										if(ind == (sends-1), { this.refreshMixGUI; });
										}.defer;
								});
							}, {
								sendsKnobArr[index][ind].value = 0;
								sendsKnobArr[index][ind].action = {};
								if(ind == (sends-2), { this.refreshMixGUI; });
							});
						});
					});
				};
			};
		};
//fx UI settings
		fxSlotArr.do{|item, index|
			item.do{|it, ind|
				it.mouseDownAction = { arg menu;
					var boundArr, thisBounds, thisArrBounds, thisWindow, thisitemArr,
					thisListView, screenBounds;
					screenBounds = Window.screenBounds.bounds.asArray.last;
					boundArr = it.bounds.asArray;
					thisBounds = 	Rect(boundArr[0], (screenBounds-boundArr[1]-285), 140, 240);
					thisArrBounds = thisBounds.asArray;
					if(menu.string == "", {
					thisWindow = Window.new("", thisBounds, border: false).front;
					thisWindow.background_(Color.black);
					thisitemArr = ([""] ++ SynthFile.read(\filter) );
					thisListView = ListView(thisWindow,Rect(0,0,(thisArrBounds[2]),(thisArrBounds[3])))
					.items_(thisitemArr)
					.background_(Color.clear)
					.font_(Font("Monaco", 10);)
					.stringColor_(Color.white)
					.hiliteColor_(Color.new255(78, 109, 38);)
					.action_({ arg sbs;
							var trackInfoArr;
							trackInfoArr = mixTrackNames[index].asString.divNumStr;
							this.filter(trackInfoArr[0].asSymbol, trackInfoArr[1], ind+1, thisListView.items[sbs.value]);
						menu.string = thisListView.items[sbs.value];
						thisWindow.close;
					});
					}, {
						"selected filter: ".post;
						menu.string.postln;
					});
				};
			}
		};

		//setting outputs
		if(outputSettings.isNil, {
			outputSettings = \master!outputMenuArr.size;
		});
		outputMenuArr.do{|it, ind|
			it.value = it.items.indexOfEqual(outputSettings[ind].asString);
			it.action = { arg menu;
				var arrayz, trackz, oldTrack, thisInput, thisNewArr, oldDest, oldInput, spaceInd, busInSpace;
				oldTrack = outputSettings[ind].asSymbol;
				oldDest = mixTrackNdefs[ind];
				//old destination off
				if(oldTrack != "".asSymbol, {
					inputs.flop[0].do{|item, index|
						if(item.asString.find(oldTrack.asString.capitalise).notNil, {
							oldInput = item;
							thisInput =  inputs.flop[1][index]});
					};
					thisNewArr = [];
					if(thisInput.isArray, {
						thisInput.do{|item|
							if(mixTrackNdefs[ind] != item, {
								thisNewArr = thisNewArr.add(item);
							});
						};
						oldDest = oldTrack.asString.divNumStr;
						if(oldDest[1].isNil, {oldDest[1] = 1;});
						if(thisNewArr.notEmpty, {
							if(thisNewArr.size == 1, {
								this.input(thisNewArr[0], oldDest[0].asSymbol, oldDest[1]);
							}, {
								this.input(thisNewArr, oldDest[0].asSymbol, oldDest[1]);
							});
						}, {
							("Ndef(" ++ ("space" ++ oldDest[0].capitalise).asSymbol.cs ++
								").source = nil").radpost.interpret;
						});
					}, {
						("Ndef(" ++ oldInput.cs ++ ").source = nil;").radpost.interpret;
					});
				});
				//new destination
				outputSettings[ind] = menu.item.asSymbol;
				if(outputSettings[ind] != "".asSymbol, {
					arrayz = [];
					outputSettings.do{|item, index|
						if(item == outputSettings[ind], {
							arrayz = arrayz.add(mixTrackNdefs[index])
					}); };
					trackz = menu.item.divNumStr;
					spaceInd = inputs.flop[0].indexOf(("space" ++ menu.item.capitalise).asSymbol);
					if(spaceInd.notNil, {
						busInSpace = inputs.flop[1][spaceInd];
						if(busInSpace.asString.find("busIn").notNil, {
							arrayz = (arrayz ++ busInSpace).flat;
						});
					});
					if(trackz[1].isNil, {trackz[1] = 1;});
					if(arrayz.size == 1, {
						this.input(arrayz[0], trackz[0].asSymbol, trackz[1]);
					}, {
						this.input(arrayz, trackz[0].asSymbol, trackz[1]);
					});
				});
			};
		};

		Ndef("AssembladgeGUI", {
			var in, imp;
			in = mixTrackNdefs.collect({|item| item.ar }).flat;
			imp = Impulse.ar(updateFreq);
			SendReply.ar(imp, "/AssembladgeGUI",
				[
					RunningSum.ar(in.squared, server.sampleRate / updateFreq),
					Peak.ar(in, Delay1.ar(imp)).lag(0, 3)
				].flop.flat
			);
		});

		OSCdef(\AssembladgeGUI, {|msg, time, addr, recvPort|
			var dBLow, array, numRMSSampsRecip, numRMSSamps, peakVal, peakArr;
			numRMSSamps = server.sampleRate / updateFreq;
			numRMSSampsRecip = 1 / numRMSSamps;
			dBLow = -80;
			{
				peakArr = [];
				msg.copyToEnd(3).pairsDo({|val, peak, i|
					var meter, thisPeakVal;
					i = i * 0.5;
					meter = 	levelArr.flat[i];
					meter.value = (val.max(0.0) * numRMSSampsRecip).sqrt.ampdb.linlin(dBLow, 0, 0, 1);
					peakVal = peak.ampdb;
					thisPeakVal = peakVal.linlin(dBLow, 0, 0, 1);
					meter.peakLevel = thisPeakVal;
					peakArr = peakArr.add(thisPeakVal);
				});
				peakArr = peakArr.reshapeLike(levelArr);
				peakArr.do{|item, index|
					var peakDb;
					peakDb = item.maxItem;
					levelTextArr[index].string = peakDb.ampdb.round(0.1).asString;
					case
					{peakDb <= 0.9 } {levelTextArr[index].background_(Color.new255(78, 109, 38));}
					{(peakDb > 0.9).and(peakDb < 1) } {
						levelTextArr[index].background_(Color.new255(232, 90, 13));}
					{peakDb == 1} {levelTextArr[index].background_(Color.new255(211, 14, 14));};
				};
			}.defer;
		}, \AssembladgeGUI);

		mixerWin.onClose = {
			Ndef("AssembladgeGUI").clear;
			OSCdef(\AssembladgeGUI).free;
			mixerWin = nil;
			/*if(outputSettings.includes("".asSymbol).not, {
			Ndef.all[server.asSymbol].clean; //garbage collection
			});*/
		};

	}

	refreshMixGUI {
		if(mixerWin.notNil, {
			mixerWin.children.do { |child| child.remove };
		});
		this.mixGUI;
	}

	setVolume {arg trackType, trackNum, val, lag, db=true;
		var trackIndex, value;
		if(db.not, {
			value = val.ampdb;
		}, {
			value = val;
		});
		trackIndex = mixTrackNames.indexOf((trackType ++ trackNum).asSymbol);
		if(trackIndex.notNil, {
			if(lag.isNil, {
				(mixTrackNdefs[trackIndex].cs ++ ".set('volume', " ++
					value ++ ");").radpostcont.interpret;
			}, {
				(mixTrackNdefs[trackIndex].cs ++ ".set('volume', " ++
					value ++ ", 'lagTime', " ++ lag ++ ");").radpostcont.interpret;
			});
			if(mixerWin.notNil, {
				if(mixerWin.notClosed, {
					setVolSlider.(trackIndex, val);
				});
			});
		}, {
			"track not found".warn;
		});
	}

	setVolumeLag {arg trackType=\track, trackNum=1, lag=0;
		var ndefCS;
		ndefCS = ("Ndef(" ++ (trackType ++ trackNum).asSymbol.cs ++
			").set('lagTime', " ++ lag ++ ");");
		ndefCS.radpostcont.interpret;
	}

	sendsFunc {|trackLb, thisKey|
		var thisSpec, selArg, lagArg, thisNdefVal, thisFunc, thisSpecVal, getBusInNum;
		if(thisKey.notNil, {
			thisNdefVal = Ndef(thisKey).getKeysValues;
			getBusInNum = (busArr.flop[1][busArr.flop[0].indexOf(thisKey)].collect{|keyVal|
				keyVal.key}.indexOf(trackLb) + 1);
			selArg = ("vol" ++ getBusInNum).asSymbol;
			lagArg = ("lag" ++ getBusInNum).asSymbol;
			thisFunc = {|vol, lag|
				("Ndef(" ++ thisKey.cs ++ ").set(" ++ selArg.cs ++
					", " ++ vol ++ ", " ++ lagArg.cs ++ ", " ++ lag ++ ");").radpostcont.interpret;
			};
		});
		^thisFunc;
	}

	setSend {arg trackType=\track, trackNum=1, slotNum=1, busNum=1;
		var trackIndex, funcThis;
		funcThis = {this.bus(trackNum, slotNum, -inf, trackType)}; //track, bus, mix, type
		if(mixerWin.notNil, {
			if(mixerWin.notClosed, {
				trackIndex = mixTrackNames.indexOf((trackType.asString ++ trackNum).asSymbol);
				setBusIns.(trackIndex,(slotNum-1), busNum);
			}, {
				funcThis.();
			});
		}, {
			funcThis.();
		});
	}

	setSendKnob {arg trackType=\track, trackNum=1, slotNum=1, val=0, lagTime=0, db=true;
		var trackIndex, value, thisResult, thisKey, thisBusKey;
		if(db.not, {
			value = val.ampdb;
		}, {
			value = val;
		});
		thisKey = (trackType.asString ++ trackNum).asSymbol;
		trackIndex = mixTrackNames.indexOf(thisKey);
		if(trackIndex.notNil, {
			thisBusKey = (\busIn ++ slotNum).asSymbol;
			if(busArr.flop[0].select({|item| item.notNil}).includes(thisBusKey), {
				if(busArr.flop[1][busArr.flop[0].indexOf(thisBusKey)].includes(
					Ndef((trackType ++ trackNum).asSymbol);
				), {
					thisResult = this.sendsFunc((trackType.asString ++ trackNum).asSymbol,
						("busIn" ++ slotNum).asSymbol);
					if(thisResult.notNil, {
						thisResult.(value, lagTime);
					});
					if(mixerWin.notNil, {
						if(mixerWin.notClosed, {
							setKnobIns.(trackIndex, (slotNum-1), val);
						});
					});
				}, {
					"No buses in this track".warn;
				});
			}, {
				"This bus doesn't exist in this track".warn;
			});
		});
	}

	setSendVolLag {arg trackType=\track, trackNum=1, busNum=1, lag=0;
		var ndefCS, synthString, thisArr, thisString, thisKey, thisBusKey;
		thisBusKey = ("busIn" ++ busNum).asSymbol;
		synthString = Ndef(thisBusKey).source.cs;
		if(synthString.find("[").notNil, {
			thisArr = synthString.copyRange(synthString.find("[")+1, synthString.find("]")-1;);
			thisArr = thisArr.replace("),", ")%");
			thisArr = thisArr.split($%);
			thisString = thisArr.select({|item| item.find((trackType.asString ++ trackNum)).notNil })[0];
		}, {
			if(synthString.find((trackType.asString ++ trackNum)).notNil, {
				thisString = 	synthString;
			});
		});
		if(thisString.notNil, {
			thisString = thisString.replace("; ", "").replace("}", "");
			thisKey = thisString.copyRange(thisString.find(".lag(")+5, thisString.size-2).asSymbol.cs;
			ndefCS = ("Ndef(" ++ thisBusKey.cs ++
				").set(" ++ thisKey ++ ", " ++ lag ++ ");");
			ndefCS.radpostcont.interpret;
		}, {
			"wrong track or bus".warn;
		});
	}

	setPanLag {arg trackType=\track, trackNum=1, lag=0, panTag=\pan;
		var tag, ndefCS;
		tag = ("space" ++ trackType.asString.capitalise ++ trackNum).asSymbol;
		ndefCS = "Ndef(" ++ tag.cs ++ ").lag(" ++panTag.cs ++ ", " ++ lag ++ ");";
		ndefCS.radpost;
		ndefCS.interpret;
	}

	setPan {arg trackType=\track, trackNum=1, val=0, panTag=\pan;
		var tag, ndefCS, trackKey;
		tag = ("space" ++ trackType.asString.capitalise ++ trackNum).asSymbol;
		ndefCS = "Ndef(" ++ tag.cs ++ ").set(" ++panTag.cs ++ ", " ++ val ++ ");";
		ndefCS.radpost;
		ndefCS.interpret;
		if(mixerWin.notNil, {
			if(mixerWin.notClosed, {
				trackKey = (trackType ++ trackNum).asSymbol;
				if(panTag == \pan, {
					setPanKnob.(mixTrackNames.indexOfEqual(trackKey), val);
				}, {
					if(panTag == \panx, {
						setPanKnob.(mixTrackNames.indexOfEqual(trackKey), val, 0);
					}, {
						setPanKnob.(mixTrackNames.indexOfEqual(trackKey), val, 1);
					});
				});
			});
		});
	}

	busMix {arg bus=1, slot=1, mix=0, lag;
		var tag, volTag, ndefCS;
		tag = ("busIn" ++ bus).asSymbol;
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

}
