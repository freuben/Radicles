Assemblage : Radicles {var <tracks, <specs, <inputs, <livetracks,
	<trackCount=1, <busCount=1, <space, <masterNdefs, <>masterSynth,
	<trackNames, <>masterInput, <busArr, <filters, <filterBuff , <>mixerWin,
	<setVolSlider, <mixTrackNames, <>systemChanNum, <mixTrackNdefs, <basicFont,
	<sysChans, <sysPan, <setBusIns, <setKnobIns, <setPanKnob, <outputSettings,
	<filtersWindow, <scrollPoint, <winRefresh=false, <fxsNum, <soloStates, <muteStates,
	<recStates, recBStoreArr, <mastOutArr, <screenBounds, <mastOutWin, <oiIns, <oiOuts,
	<recInputArr, <winDirRec, <muteButArr, <recButArr, <soloButArr, <spaceButArr,
	<recordingButton, <recordingValBut;

	*new {arg trackNum=1, busNum=0, chanNum=2, spaceType;
		^super.new.initAssemblage(trackNum, busNum, chanNum, spaceType);
	}

	initAssemblage {arg trackNum=1, busNum=0, chanNum=2, spaceType;
		var chanMaster, chanTrack, chanBus, spaceMaster, spaceTrack, spaceBus, inArr;
		server.options.numWireBufs = 128*4;
		server.options.numAudioBusChannels = 1024;
		server.options.numControlBusChannels = 16384;
		server.waitForBoot{
			{
				masterSynth = {arg volume=0, lagTime=0, mute=0, off=0;
					(\in * volume.dbamp.lag(lagTime)
						*mute.range(1,0).lag(0.1)
						*off.range(1,0)
				).softclip};
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
				/*inArr = masterNdefs.flop[1];*/
				/*masterInput = inArr.copyRange(1, inArr.size-1);*/

				masterInput = this.sortTrackNames(trackNames)
				.select({|item| item != \master }).collect({|item| Ndef(item) });

				this.input(masterInput, \master);
				server.sync;
				this.play;
				this.updateMixInfo;
				oiIns = Ndef(\master).numChannels;
				oiOuts = server.options.numOutputBusChannels;
				basicFont = Font("Monaco", 8);
			}.fork
		};
		screenBounds = Window.screenBounds.bounds;
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
		{chanNum == 3} {spaceType = \panAz3}
		{chanNum == 4} {spaceType = \pan4}
		{chanNum == 5} {spaceType = \panAz5}
		{chanNum == 6} {spaceType = \panAz6};
		^spaceType;
	}

	addTrack {arg type=\track, chanNum=1, spaceType, trackSynth, trackSpecs;
		var trackTag,spaceTag, ndefCS1, ndefCS2, spaceSynth, spaceSpecs, thisTrackInfo;
		if([\track, \bus, \master].includes(type), {
			spaceType ?? {spaceType = this.findSpaceType(chanNum);};
			trackSynth ?? {trackSynth = {arg volume=0, lagTime=0, mute=0, solo=1;
				(\in * volume.dbamp.lag(lagTime)
					*mute.range(1,0).lag(0.1)*solo.lag(0.1) )}; };
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
		{
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
			intArr.reverse.do{|item| item.radpost; item.interpret; server.sync }; }.fork;
	}

	autoAddTrack {arg type=\track, chanNum, spaceType, trackSynth, trackSpecs, action={};
		var trackInfo, masterInput, insertInd;
		{

			if(soloStates.notNil, {
				if(type == \track, {
					insertInd = mixTrackNames.indexOf(\bus1);
				}, {
					insertInd = mixTrackNames.indexOf(\master);
				});
				outputSettings.insert(insertInd, \master);
				soloStates.insert(insertInd, 0);
				muteStates.insert(insertInd, 0);
				recStates.insert(insertInd, 0);
			});

			chanNum ?? {chanNum = systemChanNum};
			trackInfo = this.addTrack(type, chanNum, spaceType, trackSynth, trackSpecs);

			server.sync;
			this.autoRoute(trackInfo);
			server.sync;

			masterInput = this.sortTrackNames(trackNames)
			.select({|item| item != \master }).collect({|item| Ndef(item) });

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
				if(type == \master, {
					ndefCS = this.ndefPrepare(Ndef(trackArr[0][0]), trackArr[0][1].filterFunc(ndefsIn));
					ndefCS.interpret;
					if(soloStates.notNil, {
						this.masterSoloFunc2;
					}, {
						ndefCS.radpost;
					});
				}, {
					ndefCS = this.ndefPrepare(Ndef(trackArr[0][0]), trackArr[0][1].filterFunc(ndefsIn));
					ndefCS.radpost;
					ndefCS.interpret;
				});
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

	filter {arg type=\track, num= 1, slot=1, filter=\pch, extraArgs, buffer, data, action;
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
				bufFunc = {
				};
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
				server.sync;
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
			server.sync;
			action.();
		}.fork;
	}

	removeFilter {arg type=\track, num= 1, slot=1, action;
		var thisTrack, thisSlot, ndefCS, setArr, bufArrInd, thisFilterTag, thisFilterIndex;
		this.globFadeTime;
		thisTrack = this.get(type)[num-1];
		if(thisTrack.size > 2, {

			thisFilterTag = this.findFilterTag(type, num, slot);
			thisSlot = this.findFilterTag(type, num, slot);
			thisFilterIndex = thisTrack.flop[0].indexOf(thisSlot);

			/*			if(slot < (thisTrack.size-1), {
			thisSlot = thisTrack[slot];*/

			if(thisFilterIndex.notNil, {

				ndefCS = "Ndef(" ++ thisSlot.cs ++ ").clear(" ++ fadeTime ++ ");";
				ndefCS.radpost;
				ndefCS.interpret;
				thisTrack.removeAt(thisFilterIndex);
				if(type == \master, {num=""});
				setArr = this.findTrackArr((type ++ num).asSymbol);
				masterNdefs[setArr[0]].removeAt(thisFilterIndex);
				specs[setArr[0]].removeAt(thisFilterIndex);
				{
					server.sync;
					fadeTime.wait;
					if(filterBuff.notNil, {
						if(filterBuff.notEmpty, {
							bufArrInd = filterBuff.flop[0].indexOf(thisSlot);
							filterBuff.flop[1][bufArrInd].do{|item|
								server.sync;
								BStore.remove(item[0], item[1], item[2]);
							};
						});
					});
				}.fork;
				filters = filters.reject({|item| item[0] == thisSlot });
				{
					this.autoRoute(thisTrack);
					server.sync;
					action.();
				}.fork(AppClock);
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
			resultArr = [varArr[0].asSymbol, varArr[1], varArr[2] ];
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
		if(filterTag.notNil, {
			^this.getFilterKeys(filterTag, \pairs);
		}, {^nil});
	}

	filterLags {arg filterNdefKey, lag=nil;
		var ndefArgs, argValArr, newNdefArgs;
		ndefArgs = Ndef(filterNdefKey).controlKeys;
		if(lag	.isArray, {
			argValArr = lag
		}, {
			argValArr = lag!ndefArgs.size;
		});
		newNdefArgs = ([ndefArgs.copyRange(0,argValArr.size-1)] ++ [argValArr]).flop.flat;
		("Ndef('filterBus_1_1').lag(" ++ newNdefArgs.cs.asString
			.replace("[", "").replace("]", "") ++ ");").radpost.interpret;
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
			spatialType = space.flop[1][space.flop[0].indexOf(("space" ++
				item.asString.capitalise).asSymbol)];
			if([\bal2, \pan2].includes(spatialType), {
				sysPan = sysPan.add(0);
			}, {sysPan = sysPan.add(1)
			});
		};
	}

	mixGUI {arg updateFreq=10;
		var sends, knobColors, winHeight, winWidth, knobSize, canvas,
		panKnobTextArr, panKnobArr, sliderTextArr, sliderArr, levelTextArr,
		levelArr, vlay, sendsMenuArr, sendsKnobArr, inputMenuArr, outputMenuArr,
		muteButton, recButton, soloButton, spaceButton, volButtons, buttonsLay1,
		buttonsLay2, oscDefFunc, levelSoloStates, fxSlotArr, trackLabelArr, spaceTextLay,
		popupmenusize, panSpec, mixInputLabels, trackInputSel, inputArray,
		numBuses, thisInputLabel, busInLabels, maxBusIn, knobFunc, busInSettings,
		guiFunc, fltMenuWindow, oldMixerWin, slotsSizeArr, sumWidth, spaceGap;

		Ndef.all[server.asSymbol].clean; //garbage collection
		this.updateMixInfo; //update info

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
		if(filters.notNil, {
			if(filters.notEmpty, {
				fxsNum = filters.flop[0].collect({|item|
					item.asString.split($_).last.asInt }).maxItem.max(1) + 1;
			});
		});

		if(soloStates.isNil, { soloStates = 0!mixTrackNames.size });
		levelSoloStates = sysChans.collect({|item, index| 1!item }).flat;

		if(muteStates.isNil, { muteStates = 0!mixTrackNames.size });

		if(recStates.isNil, { recStates = 0!(mixTrackNames.size-1) ++ [1] });

		muteButArr = [];
		soloButArr = [];
		recButArr = [];
		spaceButArr = [];

		numBuses = trackNames.select{|item| item.asString.find("bus").notNil }.size+1;

		knobColors = [ Color(0.91764705882353, 0.91764705882353, 0.91764705882353),
			Color.white, Color.black, Color() ];

		winHeight = 504 + ((sends-2)*15) + ((fxsNum-2)*15);
		winWidth = (42*(sysChans.sum));
		if(sysPan.includes(1), {knobSize = 40;}, {knobSize = 30; });
		if(winRefresh, {oldMixerWin=mixerWin; winRefresh = false;
			{0.1.yield; oldMixerWin.close; 0.1.yield; oscDefFunc.()}.fork(AppClock);
		});
		mixerWin = ScrollView(bounds: (Rect(0, 0, winWidth,winHeight))).name_("Assemblage");
		mixerWin.hasVerticalScroller = false;
		mixerWin.mouseDownAction = {
			if(fltMenuWindow.notNil, {
				if(fltMenuWindow.visible, {
					fltMenuWindow.close;
					fltMenuWindow = nil;
				});
			});
			if(mastOutWin.notNil, {
				if(mastOutWin.visible, {
					mastOutWin.close;
					mastOutWin = nil;
				});
			});
			if(winDirRec.notNil, {
				if(winDirRec.visible, {
					winDirRec.close;
					winDirRec = nil;
				});
			});
		};
		mixerWin.onMove = {
			if(fltMenuWindow.notNil, {
				if(fltMenuWindow.visible, {
					fltMenuWindow.close;
					fltMenuWindow = nil;
				});
			});
		};
		if(mixerWin.bounds != Rect(0, 0, winWidth,winHeight), {
			mixerWin.bounds = Rect(0, 0, winWidth,winHeight);
		});
		/*mixerWin.fixedHeight = winHeight;*/
		canvas = View();
		canvas.background_(Color.black);

		sysChans.do{|item, index|
			var slider, level, sliderText, levelText, hlay, thisLay, ts, finalLayout, slotsSize,
			panKnob, panKnobText, panKnobText1, panKnobText2, outputMenu, outputLabel,
			sendsMenu, sendsLabel, sendsKnobs, sendsLay, inputMenu, inputLabel, fxLabel, fxSlot,
			trackLabel, trackColor, thisInputVal, butUIHeight, butUIWidth, sendsString, soloButtonFunc;
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
			/*.background_(Color.new255(78, 109, 38)).stringColor_(Color.white)*/
			.background_(Color.black).stringColor_(Color.white)
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

			butUIHeight = 12;
			butUIWidth = 25;

			if(index == (mixTrackNames.size-1), {
				soloButton = StaticText(canvas).minWidth_(butUIWidth).maxWidth_(butUIWidth)
				.maxHeight_(butUIHeight).minHeight_(butUIHeight)
				.background_(Color.black);
				soloButton.string= "     ";
			}, {

				soloButtonFunc = {var mastChans;
					if(soloStates.includes(1), {
						levelSoloStates = sysChans.collect({|item, index|
							if(soloStates[index].isNil, {
								1!item;
							}, {
								soloStates[index]!item;
							});
						}).flat;
					}, {
						levelSoloStates = sysChans.collect({|item, index| 1!item }).flat;
					});

					mastChans = Ndef(\master).numChannels;
					levelSoloStates = levelSoloStates.copyRange(0,
						(levelSoloStates.size-1) - mastChans)
					++ (1!mastChans);
				};

				soloButton = Button().minWidth_(butUIWidth).maxWidth_(butUIWidth)
				.maxHeight_(butUIHeight).minHeight_(butUIHeight)
				.focusColor_(Color.red(alpha:0.2))
				.background_(Color.black);
				soloButton.states = [["S", Color.white, Color.black],
					["S", Color.white, Color.new255(232, 90, 13)]];
				soloButton.font = basicFont;
				soloButton.action = {|butt|
					soloStates[index] = butt.value;
					soloButtonFunc.();
					this.masterSoloFunc2;
				};
				soloButton.value = soloStates[index];
				soloButtonFunc.();
			});

			recButton = Button().minWidth_(butUIWidth).maxWidth_(butUIWidth)
			.maxHeight_(butUIHeight).minHeight_(butUIHeight)
			.focusColor_(Color.red(alpha:0.2))
			.background_(Color.black);
			recButton.states = [["R", Color.white, Color.black],
				["R", Color.white, Color.new255(211, 14, 14)]];
			recButton.font = basicFont;
			recButton.action = {|butt|
				var thisVal;
				thisVal = butt.value;
				recStates[index] = thisVal;
			};
			recButton.value = recStates[index];

			muteButton = Button().minWidth_(butUIWidth).maxWidth_(butUIWidth)
			.maxHeight_(butUIHeight).minHeight_(butUIHeight)
			.background_(Color.black);
			muteButton.states = [["M", Color.white, Color.black],
				["M", Color.white, Color.new255(58, 162, 175)]];
			muteButton.font = basicFont;
			muteButton.action = {|butt|
				var thisVal;
				thisVal = butt.value;
				("Ndef(" ++ mixTrackNames[index].cs ++ ").set('mute', " ++ thisVal
					++ ");").radpost.interpret;
				muteStates[index] = thisVal;
			};
			muteButton.value = muteStates[index];

			spaceButton = Button().minWidth_(butUIWidth).maxWidth_(butUIWidth)
			.maxHeight_(butUIHeight).minHeight_(butUIHeight)
			.focusColor_(Color.red(alpha:0.2))
			.background_(Color.black);
			spaceButton.states = [["◯", Color.white, Color.black],
				["◯", Color.white, Color.black]];
			spaceButton.font = basicFont;
			spaceButton.action = {|butt|
				"space is the place".postln;
			};

			muteButArr = muteButArr.add(muteButton);
			spaceButArr = spaceButArr.add(spaceButton);
			soloButArr = soloButArr.add(soloButton);
			recButArr = recButArr.add(recButton);

			buttonsLay1 = HLayout(*[muteButton, spaceButton]);
			buttonsLay2 = HLayout(*[soloButton, recButton]);

			popupmenusize = 14;

			if((1..3).includes(item), {
				slotsSize = 68;
			}, {
				 slotsSize = 68+(16*(item-3));
			});

			slotsSizeArr = slotsSizeArr.add(slotsSize);

			//output label
			outputLabel = StaticText(canvas).align_(\center).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			outputLabel.font = basicFont; outputLabel.string_("Output");

			if( index == (sysChans.size-1), {
				//master output button
				outputMenu = Button().maxHeight_(popupmenusize).minHeight_(popupmenusize)
				.minWidth_(slotsSize).maxWidth_(slotsSize);
				outputMenu.canFocus = false;
				outputMenu.states_([["", Color.white, Color.black]])
				.font_(basicFont);
				outputMenu.string = "Outputs";
				outputMenu.action = {|it|
					var boundArr, scrollPoint, scrollOrg;
					boundArr = it.bounds.asArray;
					scrollPoint = mixerWin.visibleOrigin;
					scrollOrg = scrollPoint.asArray;
					this.mastOutGUI(boundArr, scrollOrg);
				};
			}, {
				//output menu
				outputMenu = PopUpMenu().maxHeight_(popupmenusize)
				.minHeight_(popupmenusize).minWidth_(slotsSize).maxWidth_(slotsSize);
				outputMenu.items = ["", "master"] ++numBuses.collect{|item| "bus" ++ (item+1)};
				outputMenu.background_(Color.black).stringColor_(Color.white)
				.font_(basicFont);

			});
			outputMenuArr = outputMenuArr.add(outputMenu);

			//sends label
			if(index == (sysChans.size-1), {
				sendsString = "Recording";
			}, {
				sendsString = "Sends";
			});
			sendsLabel = StaticText(canvas).align_(\center).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			sendsLabel.font = basicFont; sendsLabel.string_(sendsString);

			//sends menu
			if( index != (sysChans.size-1), {
				sends.do{var smenu, sknob;
					smenu = PopUpMenu().maxHeight_(popupmenusize).minHeight_(popupmenusize)
					.maxWidth_(slotsSize-popupmenusize);
					smenu.items = [""] ++numBuses.collect{|item| "bus" ++ (item+1)};
					smenu.background_(Color.black).stringColor_(Color.white)
					.font_(basicFont);
					sknob = Knob().minWidth_(popupmenusize).maxWidth_(popupmenusize)
					.maxHeight_(popupmenusize).minHeight_(popupmenusize);
					sknob.color = knobColors;

					sendsMenu = sendsMenu.add(smenu);
					sendsKnobs = sendsKnobs.add(sknob);
					sendsLay = sendsLay.add(HLayout(smenu, sknob));
				};
				sendsMenuArr = sendsMenuArr.add(sendsMenu);
				sendsKnobArr = sendsKnobArr.add(sendsKnobs);
			}, {
				sends.do{|sendInd|
					var smenu;
					smenu = Button().maxHeight_(popupmenusize).minHeight_(popupmenusize)
					.minWidth_(slotsSize).maxWidth_(slotsSize);
					smenu.canFocus = false;
					if(sendInd == 0, {
						smenu.states_([["prepare rec", Color.red, Color.black],
							["record", Color.white, Color.red],
							["stop record", Color.red, Color.white]]);
						recordingButton = smenu;
						smenu.action = {|butt|
							var value;
							value = butt.value;
							case
							{value == 1} {
								this.prepareRecording;
							}
							{value == 2} {
								this.startRecording;
							}
							{value == 0} {
								this.stopRecording;
							};
							recordingValBut = (value+1)%3;
						};
						if(recordingValBut.notNil, {
							recordingButton.value = (recordingValBut-1)%3;
						});
					}, {
						smenu.states_([["dir in rec", Color.white, Color.black]]);
						smenu.action = {|it|
							var boundArr, scrollPoint, scrollOrg;
							boundArr = it.bounds.asArray;
							scrollPoint = mixerWin.visibleOrigin;
							scrollOrg = scrollPoint.asArray;
							this.dirInRec(boundArr, scrollOrg);
						};
					});
					smenu.font_(basicFont);
					if(sendInd < 2, {
						sendsLay = sendsLay.add(smenu);
					}, {
						sendsLay = sendsLay.add(nil);
					});
				};
			});
			//audio fxs
			fxLabel = StaticText(canvas).align_(\center).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			fxLabel.font = basicFont; fxLabel.string_("Audio FX");
			//fx buttons
			fxsNum.do{var fxbutton;
				fxbutton = Button().maxHeight_(popupmenusize).minHeight_(popupmenusize)
				.minWidth_(slotsSize).maxWidth_(slotsSize);
				fxbutton.canFocus = false;
				fxbutton.states_([["", Color.white, Color.black]])
				.font_(basicFont);
				fxSlot = fxSlot.add(fxbutton);
			};
			fxSlotArr = fxSlotArr.add(fxSlot);

			//input label
			inputLabel = StaticText(canvas).align_(\center).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			inputLabel.font = basicFont; inputLabel.string_("Input");
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
			.font_(basicFont).action = { arg menu;
				[menu.value, menu.item].postln;
			};

			inputMenuArr = inputMenuArr.add(inputMenu);

			sliderArr = sliderArr.add(slider);
			levelArr = levelArr.add(level);
			ts = [slider] ++ level;
			thisLay = HLayout(*ts);
			//track name label
			case
			{mixTrackNames[index].asString.find("track").notNil} {
				trackColor = Color.new255(58, 162, 175)}
			{mixTrackNames[index].asString.find("bus").notNil} {
				trackColor = Color.new255(132, 124, 10)}
			{mixTrackNames[index].asString.find("master").notNil} {
				trackColor = Color.new255(102, 57, 130)};
			trackLabel = StaticText(canvas).align_(\center).background_(trackColor)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			trackLabel.font = basicFont;
			trackLabel.string_(mixTrackNames[index].asString.capitalise);
			trackLabel.minWidth_(slotsSize).maxWidth_(slotsSize);
			trackLabelArr = trackLabelArr.add(trackLabel);

			trackLabel.mouseDownAction = {|it| it.bounds.postln };

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
			//button UI solo,mute,rec,space
			[[buttonsLay1, align: \center], [buttonsLay2, align: \center]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			//track names
			finalLayout = finalLayout.add([trackLabel, align: \below]);
			vlay = vlay.add(VLayout(*finalLayout) );
		};

		//setting interface
		sliderTextArr.do{|item, index| item.font = basicFont; item.string_(
			mixTrackNdefs[index].getKeysValues.collect({|item|
				if(item[0] == \volume, {item[1]});
		})[0].round(0.1).asString);
		};
		levelTextArr.do{|item| item.font = basicFont; item.string_("-inf");  };
		//panning
		panKnobTextArr.flat.do{|item| item.font = basicFont;};
		panSpec = \pan.asSpec;
		panKnobArr.do{|item, index|
			var panKey, panKeyValues, panValues;
			panKey = ("space" ++ mixTrackNames[index].asString.capitalise).asSymbol;
			panKeyValues = Ndef(panKey).controlKeysValues;
			/*case
			{panKeyValues.size == 0} {panValues = 0}
			{panKeyValues.size == 4} {panValues = panKeyValues[1]}
			{panKeyValues.size == 6} {panValues = [panKeyValues[1], panKeyValues[3]]};*/
			case
			{panKeyValues.size == 0} {panValues = 0}
			{panKeyValues.includes(\pan)} {
				panValues = panKeyValues[panKeyValues.indexOf(\pan)+1]}
			{panKeyValues.includes(\panx)} {
				panValues = [panKeyValues[panKeyValues.indexOf(\panx)+1],
					panKeyValues[panKeyValues.indexOf(\pany)+1]
			]};
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
					("Ndef(" ++ panKey.cs ++ ").set('panx', " ++
						newPanVal1 ++ ");").radpostcont.interpret;
					("Ndef(" ++ panKey.cs ++ ").set('pany', " ++
						newPanVal2 ++ ");").radpostcont.interpret;
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
		spaceGap = sysChans.collect({|item|
			if((1..3).includes(item), {
				item =7;
		}, {
				item = 6;
			});
		});
		spaceGap.postln;
		sumWidth = 9 + slotsSizeArr.sum + spaceGap.sum + (9-spaceGap.last+2);
		slotsSizeArr.postln;
		sumWidth.postln;
		mixerWin.maxWidth_(sumWidth).minWidth_(sumWidth);
		mixerWin.front;
		if(scrollPoint.notNil, {
			//check this bug hasn't been fixed in latest SC version, if so, remove .defer
			{0.001.yield; mixerWin.visibleOrigin_(scrollPoint);}.fork(AppClock);
		});

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
					var thisBusItem, thisTrackLabel, thisBusNum, busInBool1, busInBool2;

					scrollPoint = mixerWin.visibleOrigin;

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
						busInBool1 = busInSettings[index].select({|item, index| index != ind})
						.includesEqual(it.item);
						busInBool2 =  busInSettings[index].collect({|item|
							if(item.notNil, {item.asSymbol}, {item});
						}).includes(outputSettings[index]);
						if((busInBool1).or(busInBool2), {
							"This bus is already assigned to this track".warn;
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
				var trackInfoInt, trackInfoArr, thisFltInfo, tracksFlt, thisFltTags, fltTagArr, thisSlotInfo;

				trackInfoArr = mixTrackNames[index].asString.divNumStr;

				if(filters.notNil, {
					if(filters.notEmpty, {
						thisFltTags = filters.flop[0].collect({|item| this.convFilterTag(item) });
						fltTagArr = thisFltTags.collect({|item|
							[item[0], item[1].asInt, item[2].asInt] });
						thisFltInfo = thisFltTags.collect({|item| [item[0], item[1].asInt] });
						tracksFlt = ([thisFltInfo.flop[0], thisFltInfo.flop[1]].flop);
						trackInfoInt = [trackInfoArr[0].asSymbol, trackInfoArr[1]];
						if(trackInfoInt[1].isNil, {trackInfoInt[1] = 1 });
						if(tracksFlt.collect({ |item|
							(item == trackInfoInt) }).includes(true), {
							thisSlotInfo = [trackInfoInt, ind+1].flat;
							if(fltTagArr.collect({|item| item == thisSlotInfo}).includes(true), {
								it.string = filters[fltTagArr.indexOfEqual(thisSlotInfo)][1];
							});
						});
					});
				});

				it.mouseDownAction = { arg menu;
					var boundArr, thisBounds, thisArrBounds, thisitemArr,
					thisListView, thisTagFlt, scrollOrg;

					boundArr = it.bounds.asArray;
					scrollPoint = mixerWin.visibleOrigin;
					scrollOrg = scrollPoint.asArray;
					thisBounds = 	Rect(boundArr[0]+mixerWin.bounds.left - scrollOrg[0],
						(screenBounds.height-boundArr[1]-285)-(mixerWin.bounds.top-45),
						140, 240);
					thisArrBounds = thisBounds.asArray;
					if(menu.string == "", {
						fltMenuWindow = Window.new("", thisBounds, border: false).front;
						fltMenuWindow.background_(Color.black);
						thisitemArr = ([""] ++ SynthFile.read(\filter) );
						thisListView = ListView(fltMenuWindow,Rect(0,0,(thisArrBounds[2]),
							(thisArrBounds[3])))
						.items_(thisitemArr)
						.background_(Color.clear)
						.font_(Font("Monaco", 10);)
						.stringColor_(Color.white)
						.hiliteColor_(Color.new255(78, 109, 38);)
						.action_({ arg sbs;
							var labelKey, irItems;
							{
								labelKey = thisListView.items[sbs.value];
								if(['convrev1', 'convrev2'].includes(labelKey).not, {
									this.filter(trackInfoArr[0].asSymbol, trackInfoArr[1], ind+1,
										labelKey);
									menu.string = labelKey;
									fltMenuWindow.close;
									fltMenuWindow = nil;
									if((ind+1) > (fxsNum-1), {
										server.sync; this.refreshMixGUI;
									});
								}, {
									menu.string = labelKey;
									irItems = PathName(mainPath ++ "SoundFiles/IR/").entries
									.collect({|item| item.fileNameWithoutExtension });
									thisListView.items = [""] ++ irItems;
									thisListView.action = {|sbs|
										this.filter(trackInfoArr[0].asSymbol, trackInfoArr[1], ind+1,
											labelKey, data: [\convrev, sbs.item.asSymbol, 2048]);

										fltMenuWindow.close;
										fltMenuWindow = nil;
									};

								});

							}.fork(AppClock);
						});
					}, {
						if(trackInfoArr[1] == nil, {trackInfoArr[1] = 1});
						if(filters.notNil, {
							thisTagFlt = this.findFilterTag(trackInfoArr[0].asSymbol,
								trackInfoArr[1], ind+1);
							if(filters.flop[0].includes(thisTagFlt).not, {
								this.filter(trackInfoArr[0].asSymbol, trackInfoArr[1],
									ind+1, menu.string.asSymbol);
							});
						});
						this.filterGUI(trackInfoArr[0].asSymbol, trackInfoArr[1], ind+1,
							[menu.bounds.left+mixerWin.bounds.left-scrollPoint.asArray[0],
								(mixerWin.bounds.top-45)+menu.bounds.top+68], menu);
					});
				};
			}
		};

		//setting outputs

		if(outputSettings.isNil, {
			outputSettings = \master!(outputMenuArr.size);
		});
		outputMenuArr.do{|it, ind|
			if(ind != (sysChans.size-1), {
				it.value = it.items.indexOfEqual(outputSettings[ind].asString);
				it.action = { arg menu;
					var arrayz, trackz, oldTrack, thisInput, thisNewArr, oldDest, oldInput,
					spaceInd, busInSpace, busInBool;

					busInBool =  busInSettings[ind].collect({|item|
						if(item.notNil, {item.asSymbol}, {item});
					}).includes(menu.item.asSymbol);

					if(busInBool, {
						it.value = it.items.collect({|item| if(item != "", {item.asSymbol}, {item});
						}).indexOf(outputSettings[ind]);
						"This bus is already assigned to this track".warn;
					}, {
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
										this.input(thisNewArr[0],
											oldDest[0].asSymbol, oldDest[1]);
									}, {
										this.input(thisNewArr,
											oldDest[0].asSymbol, oldDest[1]);
									});
								}, {
									("Ndef(" ++ ("space" ++
										oldDest[0].capitalise).asSymbol.cs ++
									").source = nil").radpost.interpret;
								});
							}, {
								("Ndef(" ++ oldInput.cs ++
									").source = nil;").radpost.interpret;
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
							spaceInd = inputs.flop[0].indexOf(("space" ++
								menu.item.capitalise).asSymbol);
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

					});
				};

			});
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

		oscDefFunc = {
			OSCdef(\AssembladgeGUI, {|msg, time, addr, recvPort|
				var dBLow, array, numRMSSampsRecip, numRMSSamps, peakVal, peakArr;
				numRMSSamps = server.sampleRate / updateFreq;
				numRMSSampsRecip = 1 / numRMSSamps;
				dBLow = -80;
				{
					peakArr = [];
					msg.copyToEnd(3).pairsDo({|val, peak, i|
						var meter, thisPeakVal, value;
						i = i * 0.5;
						meter = 	levelArr.flat[i];
						value = val*levelSoloStates[i];
						meter.value = (value.max(0.0) * numRMSSampsRecip)
						.sqrt.ampdb.linlin(dBLow, 0, 0, 1);
						peakVal = (peak*levelSoloStates[i]).ampdb;
						thisPeakVal = peakVal.linlin(dBLow, 0, 0, 1);
						meter.peakLevel = thisPeakVal;
						peakArr = peakArr.add(peakVal);
					});
					peakArr = peakArr.reshapeLike(levelArr);
					if(levelTextArr.notNil, {
						peakArr.do{|item, index|
							var peakDb;
							peakDb = item.maxItem;
							if(peakDb < dBLow, {peakDb = -inf });
							if(levelTextArr[index].notNil, {
								levelTextArr[index].string = peakDb.round(0.1).asString;
							});
							/*case
							{peakDb <= 0.9 } {
							if(levelTextArr[index].notNil, {
							levelTextArr[index].background_(Color.new255(78, 109, 38));
							});
							}
							{(peakDb > 0.9).and(peakDb < 1) } {
							if(levelTextArr[index].notNil, {
							levelTextArr[index].background_(Color.new255(232, 90, 13));
							});
							}
							{peakDb == 1} {
							if(levelTextArr[index].notNil, {
							levelTextArr[index].background_(Color.new255(211, 14, 14));
							});
							};*/
						};
					});
				}.defer;
			}, \AssembladgeGUI);
		};

		oscDefFunc.();

		mixerWin.onClose = {
			levelTextArr = nil;
			/*mixerWin = nil;*/

			OSCdef(\AssembladgeGUI).free;
			Ndef("AssembladgeGUI").clear;

			filtersWindow.do{|item| item.close; };
			if(fltMenuWindow.notNil, {
				if(fltMenuWindow.visible, {
					fltMenuWindow.close;
					fltMenuWindow = nil;
				});
			});
		};

	}

	refreshMixGUI {
		if(mixerWin.notNil, {
			mixerWin.children.do { |child| child.remove };
			winRefresh = true;
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

	setFx {arg trackType=\track, num=1, slot=1, filter=\pch, extraArgs, buffer, data, remove=false;
		var refreshFunc;
		refreshFunc = {
			if(mixerWin.notNil, {
				if(mixerWin.visible, {
					"refresh".postln;
					this.refreshMixGUI;
				});
			});
		};
		if(remove, {
			this.removeFilter(trackType, num, slot, {refreshFunc.()} );
		}, {
			this.filter(trackType, num, slot, filter, extraArgs, buffer, data, {
				{refreshFunc.()}.defer;
			});
		});
	}

	getMixTrackIndex {arg trackType=\track, num=1;
		var trackInd;
		if(trackType == \master, {
			trackInd = mixTrackNames.indexOf((trackType).asSymbol);
		}, {
			trackInd = mixTrackNames.indexOf((trackType ++ num).asSymbol);
		});
		^trackInd;
	}

	setMute {arg trackType=\track, num=1, value=1;
		var noUIFunc, trackInd;
		trackInd = this.getMixTrackIndex(trackType, num);
		if(trackInd.notNil, {
			noUIFunc = {muteStates[trackInd] = value;};
			if(mixerWin.notNil, {
				if(mixerWin.visible.notNil, {
					this.muteButArr[trackInd].valueAction = value;
				}, {
					noUIFunc.();
				});
			}, {
				muteStates = 0!mixTrackNames.size;
				noUIFunc.();
			});
		}, {
			"track not found".warn;
		});
	}

	setRec {arg trackType=\track, num=1, value=1;
		var noUIFunc, trackInd;
		trackInd = this.getMixTrackIndex(trackType, num);
		if(trackInd.notNil, {
			noUIFunc = {recStates[trackInd] = value;};
			if(mixerWin.notNil, {
				if(mixerWin.visible.notNil, {
					this.recButArr[trackInd].valueAction = value;
				}, {
					noUIFunc.();
				});
			}, {
				recStates = 0!mixTrackNames.size;
				noUIFunc.();
			});
		}, {
			"track not found".warn;
		});
	}

	setSolo {arg trackType=\track, num=1, value=1;
		var noUIFunc, trackInd;
		if(trackType != 'master', {
			trackInd = this.getMixTrackIndex(trackType, num);
			if(trackInd.notNil, {
				noUIFunc = {soloStates[trackInd] = value;};
				if(mixerWin.notNil, {
					if(mixerWin.visible.notNil, {
						this.soloButArr[trackInd].valueAction = value;
					}, {
						noUIFunc.();
					});
				}, {
					soloStates = 0!mixTrackNames.size;
					noUIFunc.();
				});
			}, {
				"track not found".warn;
			});
		}, {
			"master track can\'t be soloed".warn;
		});
	}

	setSpace {arg trackType=\track, num=1, value=1;
		var noUIFunc, trackInd;
		trackInd = this.getMixTrackIndex(trackType, num);
		if(trackInd.notNil, {
			noUIFunc = {"this space thing";};
			if(mixerWin.notNil, {
				if(mixerWin.visible.notNil, {
					this.spaceButArr[trackInd].valueAction = value;
				}, {
					noUIFunc.();
				});
			}, {
				/*recStates = 0!mixTrackNames.size;*/
				noUIFunc.();
			});
		}, {
			"track not found".warn;
		});
	}

	filterWinGUI {arg filterTag=\filterTrack_1_1, filterKey=\pch, filterPairs,
		fltWinLeft=0, fltWinDown=0, mixButton;
		var winName, filtersWin, fltCanvas, panKnobTextArr, fltVlay, fltWinWidth, fltWinHeight,
		stringLengh, argArr, specArr, defaultArgArr, specBool, removeButton, fltWinTop, winBool;

		winName = filterTag.asString.split($_);
		winName[0] = winName[0].asString.replace("filter", "Filter ");
		if(winName.size == 3, {
			winName = [winName[0] ++ winName[1], winName[2]]
		});
		winName = (winName[0] ++ ": " ++ winName[1]) ++ " | " ++ filterKey.asString;
		winBool = true;
		if(filtersWindow.notNil, {
			if(filtersWindow.collect({|item| item.name ==  winName}).includes(true), {
				winBool = false;
			});
		});
		if(winBool, {
			argArr = filterPairs.flop[0];
			defaultArgArr = filterPairs.flop[1];
			specArr = SpecFile.read(\filter, filterKey, false);
			stringLengh = argArr.collect({|item| item.asString.size }).maxItem*4.8;
			filtersWin = ScrollView()
			.name_(winName);
			filtersWin.hasHorizontalScroller = false;
			fltWinWidth = (250) + stringLengh + 7;
			fltWinTop = screenBounds.height-filtersWin.bounds.top-fltWinDown;
			fltWinHeight = ( ((argArr.size+1) * (15 + 7)) + 13 + 6 ).min(fltWinTop);
			filtersWin.fixedHeight = fltWinHeight;
			filtersWin.fixedWidth = fltWinWidth;
			filtersWindow = filtersWindow.add(filtersWin);
			filtersWin.onClose = {
				filtersWindow = filtersWindow.reject({|item| item.name == filtersWin.name });
			};
			/*filtersWin.alwaysOnTop = true;*/
			fltCanvas = View();
			fltCanvas.background_(Color.black);
			argArr.do{|item, index|
				var finalLayout, panKnob, panKnobText, spaceTextLay, specOptions,
				panKnobArr, labelText, labelString, defaultVal, thisSpec, thisFunc,
				thisResult, labelTextArr;
				if(specArr.notNil, {
					specBool = specArr[index].notNil;
				}, {
					specBool = false;
				});
				panKnobText = StaticText(fltCanvas).align_(\center)
				.background_(Color.black)
				.stringColor_(Color.white)
				.font_(basicFont)
				.minWidth_(40).maxWidth_(40).maxHeight_(10).minHeight_(10);
				if(specBool, {
					thisSpec = specArr[index][1].asSpec;
					thisFunc = specArr[index][2];
					panKnob = Slider().minWidth_(180).maxWidth_(180)
					.maxHeight_(15).minHeight_(15);
					panKnob.orientation = \horizontal;
					panKnob.action = {
						if(thisFunc.notNil, {
							thisResult = thisFunc.(thisSpec.map(panKnob.value));
						}, {
							thisResult = thisSpec.map(panKnob.value);
						});
						panKnobText.string = thisResult.asString.copyRange(0, 7);
						("Ndef(" ++ filterTag.cs ++ ").set(" ++ item.cs ++ ", "
							++ thisResult ++ ");").radpostcont.interpret;
					};
					thisResult = Radicles.specUnmap(defaultArgArr[index], thisSpec, thisFunc);
					panKnob.value = thisResult;
					panKnobText.string = defaultArgArr[index].asString.copyRange(0, 7);
				}, {
					panKnob = TextField().minWidth_(180).maxWidth_(180)
					.font_(basicFont)
					.background_(Color.new255(246, 246, 246))
					.maxHeight_(15).minHeight_(15);
					defaultVal = defaultArgArr[index];
					if(defaultVal.notNil, {
						panKnob.string = defaultArgArr[index].cs;
						panKnobText.string = defaultArgArr[index].cs.copyRange(0, 7);
					});
					panKnob.action = {arg field;
						panKnobText.string = field.value;
						("Ndef(" ++ filterTag.cs ++ ").set(" ++ item.cs ++ ", "
							++ field.value ++ ");").radpostcont.interpret;
					};
				});
				labelString = item.asString;
				labelText = StaticText(fltCanvas).align_(\center)
				.background_(Color.black)
				.stringColor_(Color.white)
				.font_(basicFont)
				.string_(labelString)
				.maxWidth_(stringLengh)
				.minWidth_(stringLengh)
				.minHeight_(10);
				panKnobTextArr = panKnobTextArr.add(panKnobText);
				labelTextArr = labelTextArr.add(labelText);
				panKnobArr = panKnobArr.add(panKnob);
				[[labelText, align: \center], [panKnob, align: \center],
					[panKnobText, align: \center]].do{|lay|
					finalLayout = finalLayout.add(lay);
				};
				fltVlay = fltVlay.add(HLayout(*finalLayout) );
			};
			removeButton = Button().maxHeight_(15).minHeight_(15)
			.states_([["", Color.new255(211, 14, 14), Color.black]])
			.string_("R E M O V E   F I L T E R")
			.font_(basicFont).action = { arg menu;
				var filterInfoArr, fxsNum2;
				filterInfoArr = this.convFilterTag(filterTag);
				this.removeFilter(filterInfoArr[0], filterInfoArr[1].asInt, filterInfoArr[2].asInt);
				filtersWin.close;
				if(mixButton.notNil, {
					mixButton.string = "";
				});
				if(filters.notNil, {
					if(filters.notEmpty, {
						fxsNum2 = filters.flop[0].collect({|item|
							item.asString.split($_).last.asInt }).maxItem.max(1) + 1;

						{
							if(fxsNum2 < fxsNum, {
								server.sync; this.refreshMixGUI;
							});
						}.fork(AppClock);

					});
				});

			};
			removeButton.canFocus = false;
			fltVlay = [removeButton] ++ fltVlay;
			fltCanvas.layout = VLayout(*fltVlay);
			filtersWin.canvas = fltCanvas;
			filtersWin.bounds = Rect(fltWinLeft, fltWinDown, fltWinWidth, fltWinHeight);
			filtersWin.front;
		}, {
			filtersWin = filtersWindow.collect({|item|
				item.name });
			filtersWindow[filtersWin.indexOfEqual(winName)].front;
		});
	}

	filterGUIIndex {arg index=0, topLeftArr, mixButton;
		var filterTag, filterKey, filterPairs, filterInfo, top, left, thisFilter, defaultPos;
		if(filters.notNil, {
			thisFilter = filters[index];
			if(thisFilter.notNil, {
				filterKey = thisFilter[1];
				filterTag = thisFilter[0];
				filterInfo = this.convFilterTag(filterTag);
				filterPairs = this.getFilterPairs(filterInfo[0], filterInfo[1], filterInfo[2]);
				if(topLeftArr.isNil, {
					defaultPos = {top = 75; left = 75;};
					if(filtersWindow.isNil, {
						defaultPos.();
					}, {
						if(filtersWindow.isEmpty, {
							defaultPos.();
						}, {
							top = filtersWindow.last.bounds.top + 10;
							left = filtersWindow.last.bounds.top + 10;
						});
					});
				}, {
					left = topLeftArr[0];
					top = topLeftArr[1];
				});
				this.filterWinGUI(filterTag, filterKey, filterPairs, left, top, mixButton);
			}, {
				"filter not found".warn;
			});
		}, {
			"no active filters".warn;
		});
	}

	filterGUI {arg type=\track, num= 1, slot=1, topLeftArr, mixButton;
		var filterTag, filterIndex;
		if(filters.notNil, {
			filterTag = this.findFilterTag(type, num, slot);
			filterIndex = filters.flop[0].indexOfEqual(filterTag);
			if(filterIndex.notNil, {
				this.filterGUIIndex(filterIndex, topLeftArr, mixButton);
			});
		}, {
			"no filters active".warn;
		});
	}

	masterSoloFunc {arg buttstates;
		var inArr, newCS, startCS, endCS, finalCS, cs;
		cs = Ndef(\spaceMaster).source.cs;
		startCS = cs.find("= [");
		endCS = cs.find("].sum");
		inArr = inputs.flop[1][inputs.flop[0].indexOf(\spaceMaster);];
		newCS = inArr.collect {|item, index|  "Ndef.ar(" ++ item.key.cs ++
			", " ++ item.numChannels ++ ") * "	++ buttstates[index] };
		finalCS = cs.replace(cs.copyRange(startCS+2,endCS), newCS);
		("Ndef('spaceMaster').source = " ++ finalCS).radpost.interpret;
	}

	masterSoloFunc2 {var newStates;
		soloStates.do({|item, index|
			if(outputSettings[index] == \master, {
				newStates = newStates.add(item);
			});
		});
		if(soloStates.includes(1), {
			if(newStates.includes(1), {
				this.masterSoloFunc((newStates));
				Ndef('master').set('off', 0);
			}, {
				Ndef('master').set('off', 1);
			});
		}, {
			Ndef('master').set('off', 0);
			this.masterSoloFunc((1!newStates.size););
		});
	}

	prepareRecording {arg headerFormat = "wav", sampleFormat = "int16";
		var recPath, timestamp, recTracks;
		recPath = Radicles.mainPath ++ "SoundFiles/Record/";
		timestamp = Date.localtime;
		if(recStates.isNil, { recStates = 0!(mixTrackNames.size-1) ++ [1] });
		recStates.do({|item, index|
			if(item == 1, {
				recTracks = recTracks.add(mixTrackNames[index]);
			});
		});
		recBStoreArr = [];
		if(recInputArr.isNil, {
			recInputArr = [ "Inputs 1-2" ];
		});
		recTracks = recTracks ++ recInputArr;
		recTracks.do{|item|
			var recNumChans;
			if(item.asString.find("Inputs").isNil, {
				recNumChans = Ndef(item).numChannels;
			}, {
				recNumChans = 2;
			});
			recBStoreArr = 	recBStoreArr.add([\alloc, ("alloc" ++ BStore.allocCount).asSymbol,
				[server.sampleRate.nextPowerOfTwo, recNumChans]];);
			BStore.allocCount = BStore.allocCount + 1;
		};
		"//prepare recording".radpost;
		BStore.addAll(recBStoreArr, {|buf|
			recBStoreArr.do{|it, in|
				(BStore.buffStrByID(it) ++ ".write(" ++
					(recPath ++ timestamp ++ " " ++ in ++ " " ++ recTracks[in].asString
						++ ".wav").cs ++ ", " ++ headerFormat.cs ++ ", " ++ sampleFormat.cs
					++ ", 0, 0, true);").radpost.interpret;
			};
		});
	}

	startRecording {var recTracks;
		recStates.do({|item, index|
			if(item == 1, {
				recTracks = recTracks.add(mixTrackNames[index]);
			});
		});
		if(recInputArr.isNil, {
			recInputArr = [ "Inputs 1-2" ];
		});
		recTracks = recTracks ++ recInputArr;
		recTracks.do{|item, index|
			var recBus, busArr;
			if(item.asString.find("Inputs").isNil, {
				recBus = ("Ndef.ar(" ++ item.cs ++ ",  " ++ Ndef(item).numChannels ++ ")");
			}, {
				busArr = item.split($ )[1].split($-).asString.interpret;
				recBus = ("SoundIn.ar(" ++ (busArr-1) ++ ")");
			});
			("//recording: " ++ recTracks[index]).radpost;
			("Ndef(" ++ ("record_" ++ index).asSymbol.cs ++ ", {\n\tDiskOut.ar("
				++ BStore.buffStrByID(recBStoreArr[index]) ++ ", " ++ recBus ++ " ) }); ")
			.radpost.interpret;
		};
	}

	stopRecording {
		{
			"//stop recording".radpost;
			recBStoreArr.do{|item, index|
				("Ndef(" ++ ("record_" ++ index).asSymbol.cs ++ ").free;").radpost.interpret;
				server.sync;
				(BStore.buffStrByID(item) ++ ".close;").radpost.interpret;
				BStore.removeID(item);
			};
		}.fork;
	}

	mastOutSynth	{
		var arr2, arr3, selArr;
		if(mastOutArr.flat.includes(1), {
			arr2 = mastOutArr.flop.collect({|item| item.collect({|it, in|
				if(it != 0, {it = "out[" ++ in ++ "]" }, {it =  it}); }); });
			arr2 = arr2.collect{|item| item.select({|it| it != 0 }) };
			arr2.do{|item, index| if(item.isEmpty.not, {selArr = selArr.add(index)}) };
			arr2 = arr2.copyRange(0, selArr.last);
			arr3 = arr2.collect{|item|
				if(item.size == 1, {
					item = item[0].asString;
				}, {
					if(item.isEmpty, {
						item = "DC.ar(0)";
					}, {
						item =  (item.asString ++ ".sum";)
					});
			}); };
		}, {
			arr3 = "DC.ar(0)"!Ndef(\master).numChannels ;
		});
		("Ndef('masterOut', {var out; \nout = Ndef.ar('master', " ++
			Ndef(\master).numChannels ++ ");\n\t" ++	arr3.asString ++ ";\n\t});")
		.radpost.interpret;
	}

	mastOutGUI {arg boundArr, scrollOrg;
		var butt, oi1, oi2, selButt, oiArr, funcSource, thisBounds;
		thisBounds = 	Rect(boundArr[0]+mixerWin.bounds.left - scrollOrg[0],
			(screenBounds.height-boundArr[1]-((oiOuts * 25+10)+45))-(mixerWin.bounds.top-45),
			(oiIns*25+10), (oiOuts * 25+10));
		mastOutWin = Window("", thisBounds, border:false);
		mastOutWin.background_(Color.black);
		butt = { {Button(mastOutWin).maxWidth_(15).maxHeight_(15)
			.states_([["", Color.white, Color.black], ["", Color.white, Color.new255(102, 57, 130)] ]);
		} ! oiOuts } ! oiIns ;
		butt.flat.do{|item| item.canFocus = false };
		oiOuts.do{|ind|
			oi1 = oi1.add( StaticText(mastOutWin).string_(ind+1) );
		};
		oi1.do{|item| item.font = basicFont;
			item.stringColor_(Color.white);
			item.maxWidth_(15).maxHeight_(15).align_(\center);
		};
		oi2 = [oi1] ++ butt;
		oi2 = oi2.collect({|item, ind| [StaticText(mastOutWin)] ++ item });
		oi2.do{|item, index|
			item[0].font = basicFont;
			if(index == 0, {
				item[0].string_("O/S");
			}, {
				item[0].string_(index);
				item[0].align_(\center);
			});
			item[0].maxWidth_(15).maxHeight_(15).minWidth_(15).minHeight_(15);
			item[0].stringColor_(Color.white);
		};
		selButt = Button(mastOutWin).maxHeight_(15).font_(basicFont)
		.states_([["S E L E C T", Color.white, Color.black]]);
		selButt.action = {
			mastOutArr = butt.collect({|item|  item.collect({|it| it.value }) });
			this.mapOutFunc;
			mastOutWin.close;
		};
		selButt.canFocus = false;
		if(mastOutArr.isNil, {
			mastOutArr = {0!oiOuts} ! oiIns;
			oiIns.collect{|item| item }.do{|it, ind| mastOutArr[ind][it] = 1 };
		});
		butt.flat.do{|item, index| item.value = mastOutArr.flat[index] };
		mastOutWin.layout = VLayout(*[HLayout(*oi2.collect { |x| VLayout(* x) }), selButt]);
		mastOutWin.front;
	}

	mapOutFunc {var mastOutArr2, funcSource;
		{mastOutArr2 = {0!oiOuts} ! oiIns;
			oiIns.collect{|item| item }.do{|it, ind| mastOutArr2[ind][it] = 1 };
			if(Ndef(\masterOut).source.isNil, {
				if(mastOutArr != mastOutArr2, {
					funcSource = Ndef(\master).source;
					"Ndef('master').clear;".radpost.interpret;
					Ndef(\master).fadeTime.yield;
					server.sync;
					("Ndef('master', " ++ funcSource.cs ++ ");").radpost.interpret;
					"Ndef('masterOut').play;".radpost.interpret;
					"Ndef('masterOut').reshaping = 'elastic';".radpost.interpret;
					this.mastOutSynth;
				});
			}, {
				this.mastOutSynth;
			});
		}.fork(AppClock);
	}

	mapOuts {arg outArr = [1,2];
		var thisOutArr, outs;
		outs = outArr - 1;
		thisOutArr = {0!oiOuts} ! oiIns;
		thisOutArr.do{|item, index| item[outs[index] ] = 1 };
		mastOutArr = thisOutArr;
		this.mapOutFunc;
	}

	dirInRec {arg boundArr, scrollOrg;
		var states, view, butt, thisBounds;
		thisBounds = 	Rect(boundArr[0]+mixerWin.bounds.left - scrollOrg[0],
			(screenBounds.height-boundArr[1]-285)-(mixerWin.bounds.top-45),
			140, 240);
		(1..server.options.numInputBusChannels).pairsDo{|it1, it2|
			states = states.add(("Inputs " ++ it1 ++ "-" ++ it2))};
		if(recInputArr.isNil, {
			recInputArr = states.atAll([0]);
		});
		winDirRec = Window("", thisBounds, border: false);
		winDirRec.background_(Color.black);
		view = ListView(winDirRec,Rect(10,10,120,70))
		.items_(states)
		.background_(Color.clear)
		.font_(Font("Monaco", 10);)
		.stringColor_(Color.white)
		.hiliteColor_(Color.new255(78, 109, 38);)
		.selectionMode_(\multi);
		view.selection = recInputArr.collect({|item|
			states.indicesOfEqual(item) }).flat;
		butt = Button().maxHeight_(15).minHeight_(15)
		.states_([["", Color.white, Color.black]])
		.string_("S E L E C T")
		.font_(basicFont)
		.action = {
			recInputArr = states.atAll(view.selection);
			winDirRec.close;
		};
		winDirRec.layout = VLayout(view, butt);
		winDirRec.front;
	}

	setDirInRec {arg stereoInIndices = [0];
		var inputs;
		(1..server.options.numInputBusChannels).pairsDo{|it1, it2|
			inputs = inputs.add(("Inputs " ++ it1 ++ "-" ++ it2))};
		recInputArr = inputs.atAll(stereoInIndices);
		^recInputArr;
	}

	setRecording {
		var noUIFunc;
		if(recordingValBut.isNil, {recordingValBut = 1});
			case
			{recordingValBut == 1} {this.prepareRecording}
			{recordingValBut == 2} {this.startRecording}
			{recordingValBut == 0} {this.stopRecording};
		if(mixerWin.notNil, {
			if(mixerWin.visible.notNil, {
				recordingButton.value = recordingValBut;
			});
		});
		recordingValBut = (recordingValBut+1)%3;
	}

	/*	fadeTime {arg newFadeTime=3;
	masterNdefs.flat.do{|item| item.fadeTime = newFadeTime};
	}*/

	globFadeTime {
		masterNdefs.flat.do{|item| item.fadeTime = fadeTime};
	}
}