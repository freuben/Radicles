Assemblage : Radicles {var <tracks, <specs, <inputs,
	<trackCount=1, <busCount=1, <space, <masterNdefs, <>masterSynth,
	<trackNames, <>masterInput, <busArr, <filters, <filterBuff , <>mixerWin,
	<setVolSlider, <mixTrackNames, <>systemChanNum, <mixTrackNdefs, <basicFont,
	<sysChans, <sysPan, <setBusIns, <setKnobIns, <setPanKnob, <outputSettings,
	<filtersWindow, <scrollPoint, <winRefresh=false, <fxsNum, <soloStates, <muteStates,
	<recStates, recBStoreArr, <mastOutArr, <screenBounds, <mastOutWin, <oiIns, <oiOuts,
	<recInputArr, <winDirRec, <muteButArr, <recButArr, <soloButArr, <spaceButArr,
	<recordingButton, <recordingValBut, <setOutputMenu, <setInputMenu, <modSendArr,
	<trackDataArr, <trackBufferArr, <setInKnob;

	*new {arg trackNum=1, busNum=0, chanNum=2, spaceType;
		^super.new.initAssemblage(trackNum, busNum, chanNum, spaceType);
	}

	initAssemblage {arg trackNum=1, busNum=0, chanNum=2, spaceType;
		var chanMaster, chanTrack, chanBus, spaceMaster, spaceTrack, spaceBus, inArr;
		server.options.numWireBufs = 128*4;
		server.options.numAudioBusChannels = 128*8;
		server.options.numControlBusChannels = 128*128;
		server.waitForBoot{
			{
				masterSynth = {arg volume=0, lagTime=0, mute=0, off=0;
					(\in * volume.dbamp.lag(lagTime)
						*mute.linlin(0,1,1,0).lag(0.1)
						*off.linlin(0,1,1,0)
				)};
				if(chanNum.isArray.not, {
					chanMaster = chanNum;
					chanTrack = chanNum;
					chanBus = chanNum;
					systemChanNum = chanNum;
				}, {
					chanTrack = chanNum[0];
					chanBus = chanNum[1];
					if(chanNum[2].isNil, {
						chanMaster = 2;
					}, {
						chanMaster = chanNum[2];
					});
					systemChanNum = chanNum[2];
				});
				if(spaceType.isArray.not, {
					spaceMaster = spaceType;
					spaceTrack = spaceType;
					spaceBus = spaceType;
				}, {
					spaceTrack = spaceType[0];
					spaceBus = spaceType[1];
					spaceMaster = spaceType[2];
				});
				this.addTrack(\master, chanMaster.max(2), spaceMaster, masterSynth);
				if(trackNum != 0, {
					this.addTracks(trackNum, \track, chanTrack, spaceTrack);
				});
				if(busNum != 0, {
					this.addTracks(busNum, \bus, chanBus, spaceBus);
				});
				tracks.do{|item|
					this.autoRoute(item);
				};
				server.sync;
				this.inputMaster;
				server.sync;
				this.play;
				this.updateMixInfo;
				oiIns = Ndef(\spaceMaster).numChannels;
				oiOuts = server.options.numOutputBusChannels;
				basicFont = Font("Monaco", 8);
			}.fork
		};
		screenBounds = Window.screenBounds.bounds;
	}

	get {arg trackType = \track;
		^tracks.select{|item|
			item[item.size-2][0].asString.find(trackType.asString).notNil; };
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

	play {
		{
			"Ndef('spaceMaster')[1] = \\filter -> {arg in; (in).clip2};".radpost.interpret;
			server.sync;
			"Ndef('spaceMaster').play;".radpost.interpret;
		}.fork;
	}

	inputMaster {var masterInput;
		masterInput = this.sortTrackNames(trackNames)
		.select({|item| item != \master }).collect({|item|
			Ndef(("space" ++ item.asString.capitalise).asSymbol) });
		this.input(masterInput, \master);
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

	spaceTypeToChanOut {arg spaceType=1;
		var chanNum;
		case
		{spaceType == \pan2} {chanNum = 2}
		{spaceType == \bal2} {chanNum = 2}
		{spaceType == \panAz3} {chanNum = 3}
		{spaceType == \pan4} {chanNum = 4}
		{spaceType == \panAz5} {chanNum = 5}
		{spaceType == \panAz6} {chanNum = 6};
		^chanNum;
	}

	addTrack {arg type=\track, chanNum=1, spaceType, trackSynth;
		var trackInTag, trackTag, spaceTag, ndefCS, ndefCS1, ndefCS2, trackInSynth,
		spaceSynth, trackInSpecs, spaceSpecs, trackSpecs, thisTrackInfo;
		if([\track, \bus, \master].includes(type), {
			spaceType ?? {spaceType = this.findSpaceType(chanNum);};

			trackInSynth = {arg trim=0, lagTime=0;
				(\in * trim.dbamp.lag(lagTime) )};
			trackInSpecs = [ ['trim', [-23, 23] ], ['lagTime', [0, 10] ] ];
			trackSynth ?? {trackSynth = {arg volume=0, lagTime=0, mute=0, solo=1;
				(\in * volume.dbamp.lag(lagTime)
					*mute.linlin(0,1,1,0).lag(0.1)*solo.lag(0.1) )} };
			trackSpecs = [ ['volume', [-inf, 6, \db, 0, -inf, " dB" ] ], ['lagTime', [0, 10] ],
				['mute', [0, 1] ], ['solo', [0,1] ] ];
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

			trackInTag = ("in" ++ trackTag.asString.capitalise).asSymbol;
			ndefCS = "Ndef.ar(" ++ trackInTag.cs ++ ", ";
			ndefCS = (ndefCS ++ chanNum.cs ++ ");");
			ndefCS.radpost.interpret;
			ndefCS = ("Ndef(" ++ trackInTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS.radpost.interpret;
			server.sync;
			ndefCS1 = "Ndef.ar(" ++ trackTag.cs ++ ", ";
			ndefCS1 = (ndefCS1 ++ chanNum.cs ++ ");");
			ndefCS1.radpost.interpret;
			ndefCS2 = ("Ndef(" ++ trackTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS2.radpost.interpret;
			server.sync;
			spaceTag = ("space" ++ trackTag.asString.capitalise).asSymbol;
			ndefCS1 = "Ndef.ar(" ++ spaceTag.cs ++ ", ";
			ndefCS1 = (ndefCS1 ++ this.spaceTypeToChanOut(spaceType).cs ++ ");");
			ndefCS1.radpost.interpret;
			ndefCS2 = ("Ndef(" ++ spaceTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS2.radpost.interpret;
			server.sync;
			thisTrackInfo = [[trackInTag, trackInSynth], [trackTag, trackSynth],
				[spaceTag, spaceSynth] ];
			tracks = tracks.add([[trackInTag, trackInSynth], [trackTag, trackSynth],
				[spaceTag, spaceSynth] ]);
			specs = specs.add([ [trackInTag, trackInSpecs], [trackTag, trackSpecs],
				[spaceTag, spaceSpecs] ]);
			masterNdefs = masterNdefs.add([Ndef(trackInTag), Ndef(trackTag),
				Ndef(spaceTag)]);
			trackNames = trackNames.add(trackTag);
			space = space.add([spaceTag, spaceType]);
			^thisTrackInfo;
		}, {
			"Track type not found".warn;
		});
	}

	addTracks {arg number, type=\track, chanNum, spaceType, trackSynth;
		var thisChan, thisDest;
		if(chanNum.isArray, {
			number.do{|index|
				this.addTrack(type, chanNum[index], spaceType, trackSynth);
			};
		}, {
			number.do{
				this.addTrack(type, chanNum, spaceType, trackSynth);
			};
		});
	}

	addAlltracks {arg arr;
		arr.do{|item|
			this.addTrack(item);
		}
	}

	autoRoute {arg trackInfo, action;
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
			intArr.reverse.do{|item| item.radpost.interpret; server.sync };
			action.();
		}.fork;
	}

	autoAddTrack {arg type=\track, chanNum, spaceType, trackSynth, trackSpecs, action;
		var trackInfo, insertInd;
		{
			if(soloStates.notNil, {
				if(type == \track, {
					insertInd = mixTrackNames.indexOf(\bus1);
				}, {
					insertInd = mixTrackNames.indexOf(\master);
				});
				outputSettings = outputSettings.insert(insertInd, \master);
				soloStates = soloStates.insert(insertInd, 0);
				muteStates = muteStates.insert(insertInd, 0);
				recStates = recStates.insert(insertInd, 0);
			});
			chanNum ?? {chanNum = systemChanNum};
			trackInfo = this.addTrack(type, chanNum, spaceType, trackSynth, trackSpecs);
			server.sync;
			this.autoRoute(trackInfo);
			server.sync;
			this.inputMaster;
			server.sync;
			{this.refreshFunc}.defer;
			server.sync;
			action.();
		}.fork;
	}

	updateTrackCount {arg trackType=\track; var countArr, result;
		if(trackNames.notNil, {
			countArr = trackNames.select({|item|
				item.asString.find(trackType.asString).notNil }).collect{|it|
				it.asString.divNumStr[1] };
			if(countArr.maxItem.notNil, {
				result = (countArr.maxItem) + 1;
			}, {
				result = 1;
			});
		}, {
			result = 1;
		});
		^result;
	}

	removeTrack {arg trackType=\track, trackNum=1, action={};
		var trackString, inTrack, realTrack, spaceTrack, indexTrack, indArr, thisBusNums, indArrBusIn;

		if(trackType != \master, {
			{
				trackString = (trackType ++ trackNum).asString;
				inTrack = ("in" ++ trackString.capitalise).asSymbol;
				realTrack = trackString.asSymbol;
				spaceTrack = ("space" ++ trackString.capitalise).asSymbol;
				indexTrack = mixTrackNames.indexOf(realTrack);

				this.removeTrackFilters(trackType, trackNum, false, true);
				server.sync;

				("Ndef(" ++ inTrack.cs ++ ").clear(" ++ fadeTime ++ ");").radpost.interpret;
				("Ndef(" ++ realTrack.cs ++ ").clear(" ++ fadeTime ++ ");").radpost.interpret;
				("Ndef(" ++ spaceTrack.cs ++ ").clear(" ++ fadeTime ++ ");").radpost.interpret;

				this.clearModTrackNdefs(trackType, trackNum);

				tracks.remove(tracks.detect{|item| item.flat.includes(realTrack); });
				specs.remove(specs.detect{|item| item.flat.includes(inTrack); });
				masterNdefs.remove(masterNdefs.detect{|item|
					item.flat.collect({|item| item.key}).includes(realTrack) };);
				space.remove(space.detect{|item| item.flat.includes(spaceTrack);});
				trackNames.remove(realTrack);
				mixTrackNames.remove(realTrack);
				mixTrackNdefs.remove(mixTrackNdefs.detect{|item| item == Ndef(realTrack)};);
				outputSettings.removeAt(indexTrack);
				soloStates.removeAt(indexTrack);
				muteStates.removeAt(indexTrack);
				recStates.removeAt(indexTrack);
				this.outputMasterFunc;

				if(trackType == \bus, {
					if(busArr.flat.select({|item| item.notNil}).size != 0, {
						indArrBusIn =
						busArr.flop[0].indexOf( (trackType ++  "In" ++ trackNum).asSymbol );
						busArr.flop[1][indArrBusIn].collect{|item| item.key.asString.divNumStr}.do{|it|
							this.removeBus(it[1], trackNum, it[0].asSymbol, true);
							server.sync;
						};
					});
				});

				indArr = [];

				if(busArr.flat.select({|item| item.notNil}).size != 0, {
					busArr.flop[1].select({|item| item.notNil }).do{|item, index|
						if(item.includes(Ndef(realTrack)), {indArr = indArr.add(index)}); };
					thisBusNums = busArr.flop[0].select({|item| item.notNil }).atAll(indArr).collect{|item|
						item.asString.divNumStr[1].interpret };
					thisBusNums.do{|item|
						this.removeBus(trackNum, item, trackType);
						server.sync;
					};
				});

				if(trackType == \track, {
					trackCount = this.updateTrackCount(trackType);
				}, {
					busCount = this.updateTrackCount(trackType);
				});

				//refresh win if open
				if(mixerWin.notNil, {
					if(mixerWin.notClosed, {
						nodeTime.yield;
						{this.refreshMixGUI;}.defer;
					});
				});
				action.();
			}.fork;

		}, {
			"You can\'t remove the master track".warn;
		});
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
			"Track name not found".warn;
		});
	}

	getThisTrack {arg type, num;
		var inTag, trackArr;
		if(type == \master, {inTag = type}, {inTag = (type ++ num).asSymbol});
		trackArr = this.get(type).detect({|item| item.flat.includes(inTag) });
		^trackArr;
	}

	input {arg ndefsIn, type=\track, num=1, respace=true, spaceType;
		var trackArr, ndefCS, connect, inTag, newInIndex;
		if([\track, \bus, \master].includes(type), {
			if(type == \master, {inTag = type}, {inTag = (type ++ num).asSymbol});

			trackArr = this.getThisTrack(type, num);

			if(inputs.notNil, {
				if(inputs.flop[0].includes(inTag), {
					inputs.removeAt(inputs.flop[0].indexOf(inTag));
				});
			});
			inTag = ("in" ++ inTag.asString.capitalise).asSymbol;
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
					ndefCS.radpost.interpret;
				});
			}, {
				if(respace, {
					//this needs more work
					/*if(spaceType.isNil, {spaceType = this.findSpaceType(sysChans.last)});*/
					/*					this.respace(trackArr[0][0], ndefsIn, spaceType);*/

					trackArr =this.getThisTrack(type, num);

					ndefCS = this.ndefPrepare(Ndef(trackArr[0][0]), trackArr[0][1].filterFunc(ndefsIn));
					ndefCS.radpost.interpret;
				}, {
					"Channel number input doesn't match track".warn;
				});
			});
		}, {
			"Wrong track type".warn;
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
				"No input assigned to this track".warn;
			});
		}, {
			"Track doesn't exist".warn;
		});
	}

	getInputs {arg type=\master;
		var arr;
		arr = [];
		inputs.flop[0].do{|item, index|
			if( (item.asString.find(type.asString.capitalise)).notNil , {
				arr = arr.add(inputs.flop[1][index]);
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
			"Destination not found".warn;
		});
	}

	bus {arg trackNum=1, busNum=1, mix=1, trackType=\track, action;
		var numChan, busTag, ndefCS1, ndefCS2, ndefCS3, funcBus, thisBusArr,
		busAdd, argIndex, thisBusLabel;
		{
			thisBusLabel = ("inBus" ++ busNum).asSymbol;
			numChan = Ndef(thisBusLabel).numChannels;
			busTag = ("busIn" ++ busNum).asSymbol;
			if(busArr[busNum-1][0].isNil, {
				busArr[busNum-1][0] = busTag;
				ndefCS1 =	("Ndef.ar(" ++ busTag.cs ++ ", " ++ numChan ++ ");" );
				ndefCS1.radpost.interpret;
				ndefCS1 = ("Ndef(" ++ busTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
				ndefCS1.radpost.interpret;
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
			ndefCS2.radpost.interpret;
			server.sync;
			argIndex = busArr[busNum-1][1].indexOf(Ndef((trackType.asString ++ trackNum).asSymbol));
			ndefCS3 = "Ndef(" ++ busTag.cs ++ ").set('vol" ++ (argIndex+1)
			++ "', " ++ mix ++ ");";
			ndefCS3.radpost.interpret;
			action.();
		}.fork;
	}

	removeBus {arg trackNum= 1, busNum=1, trackType=\track, clearTrack=false;
		var oldLabel, newBusInd, newLabelArr, spaceBusNum, spaceBusLabel,
		spaceInNdef, spaceInSel, spaceInInd, thisBusIndLabel, trackKey;
		oldLabel = ("busIn" ++ busNum).asSymbol;
		spaceBusLabel = ("inBus" ++ busNum).asSymbol;
		spaceInInd = inputs.flop[0].indexOf(spaceBusLabel);
		trackKey = (trackType.asString ++ trackNum).asSymbol;

		if(spaceInInd.notNil, {
			newBusInd = busArr.flop[0].indexOf(oldLabel);
			newLabelArr = busArr.flop[1][newBusInd];
			newLabelArr = newLabelArr.select({|item|
				item.key != trackKey;
			});
			busArr[newBusInd][1] = newLabelArr;
			thisBusIndLabel = inputs.flop[0].indexOf(oldLabel);
			if(newLabelArr.isEmpty, {
				if(clearTrack, {
					("Ndef(" ++ oldLabel.cs ++ ").clear(" ++ fadeTime ++ ");").radpost.interpret;
				}, {
					("Ndef(" ++ oldLabel.cs ++ ").source = nil;").radpost.interpret;
				});
				busArr[newBusInd] = [nil,nil];
				inputs.removeAt(thisBusIndLabel);
				spaceInInd = inputs.flop[0].indexOf(spaceBusLabel);
				if(inputs.flop[1][spaceInInd].isArray.not, {
					if(clearTrack.not, {
						("Ndef(" ++ spaceBusLabel.cs ++ ").source = nil;").radpost.interpret;
						inputs.removeAt(spaceInInd);
					});
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

	filter {arg type=\track, num= 1, slot=1, filter=\pch, extraArgs, buffer, data, action, insert=false;
		var filterTag, ndefArr, ndefCS, arr1, arr2, arr3, arrSize, filterInfo, setArr,
		setTag, filterIndex, startNdefs, filterSpecs, trackTags, convString, routArr1,
		replaceString, cond, bufIndex, bufFunc, ndefNumChan, ndefSpace, routInd;
		//still some work to do with buffer alloc
		{
			filterTag = ("filter" ++ type.asString.capitalise ++ "_" ++ num ++ "_" ++ slot).asSymbol;
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
			ndefArr = this.getThisTrack(type, num);
			ndefSpace = ndefArr.last;
			ndefArr = ndefArr.copyRange(0, ndefArr.size-2);
			ndefNumChan = Ndef(ndefArr[0][0]).numChannels;
			startNdefs = {
				ndefCS = "Ndef.ar(" ++ filterTag.cs ++ ", " ++ ndefNumChan ++ ");";
				ndefCS.radpost.interpret;
				ndefCS = "Ndef(" ++ filterTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";";
				ndefCS.radpost.interpret;
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
			filterSpecs = [filterTag, SpecFile.read(\filter, filter, false)];
			cond = Condition(false);

			if(data.notNil, {
				trackDataArr = trackDataArr.add([filterTag, data]);
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

			//buffers?
			if(buffer.notNil, {
				trackBufferArr = trackBufferArr.add([filterTag, buffer]);
			});

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
			if(type == \master, {
				setTag = type.asSymbol;
			}, {
				setTag = (type ++ num).asSymbol;
			});
			routArr1 = arr1;
			arr1 = arr1 ++ [ndefSpace];
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
			if(insert, {
				"insert is true".postln;
				routInd = routArr1.flop[0].indexOf(filterTag);
				("Ndef(" ++ routArr1[routInd][0].cs ++ ", " ++
					routArr1[routInd][1].filterFunc(Ndef(routArr1[routInd-1][0])).cs
					++ ");").radpost.interpret;
				if(extraArgs.notNil, {
					("Ndef(" ++ routArr1[routInd][0].cs ++ ").set" ++
						extraArgs.cs.replace("[", "(").replace("]", ");")).radpost.interpret;
				}, {
					[routArr1[routInd][1].argNames, routArr1[routInd][1].defaultArgs]
					.flop.do{|item|
						Ndef(filterTag).set(item[0], item[1]);
					};
				});
			}, {
				"insert is false".postln;
				if(extraArgs.notNil, {
					extraArgs.pairsDo{|it1, it2|
						Ndef(filterTag).set(it1, it2);
					};
				});
				cond.test = false;
				this.autoRoute(routArr1.postln, {cond.test = true; cond.signal; });
				cond.wait;
			});
			server.sync;
			bufFunc.();
			server.sync;
			action.();
		}.fork;
	}

	removeBufFromFilter {arg thisSlot, actionBuf;
		var cond, bufArrInd;
		cond = Condition(false);
		{
			server.sync;
			fadeTime.wait;
			if(filterBuff.notNil, {
				if(filterBuff.notEmpty, {
					bufArrInd = filterBuff.flop[0].indexOf(thisSlot);
					if(bufArrInd.notNil, {
						filterBuff.flop[1][bufArrInd].do{|item|
							BStore.remove(item[0], item[1], item[2], {
								cond.test = true; cond.signal;
							});
							cond.wait;
						};
						actionBuf.();
						filterBuff.removeAt(bufArrInd);
					});
				}, {
					actionBuf.();
				});
			}, {
				actionBuf.();
			});
		}.fork;
	}

	removeFilter {arg type=\track, num= 1, slot=1, action, actionBuf;
		var thisTrack, thisSlot, ndefCS, setArr, thisFilterTag, thisFilterIndex,
		keyArr, activeMods, modArr, cond;
		this.globFadeTime;
		thisTrack = this.getThisTrack(type, num);
		if(thisTrack.size > 2, {
			thisFilterTag = this.findFilterTag(type, num, slot);
			thisSlot = this.findFilterTag(type, num, slot);
			thisFilterIndex = thisTrack.flop[0].indexOf(thisSlot);
			if(thisFilterIndex.notNil, {
				{
					this.ndefModClear(thisSlot);
					ndefCS = "Ndef(" ++ thisSlot.cs ++ ").clear(" ++ fadeTime ++ ");";
					ndefCS.radpost.interpret;

					thisTrack.removeAt(thisFilterIndex);
					if(type == \master, {num=""});
					setArr = this.findTrackArr((type ++ num).asSymbol);
					masterNdefs[setArr[0]].removeAt(thisFilterIndex);
					specs[setArr[0]].removeAt(thisFilterIndex);

					this.removeBufFromFilter(thisSlot, actionBuf);
					filters = filters.reject({|item| item[0] == thisSlot });
					trackDataArr = trackDataArr.reject({|item| item[0] == thisSlot });
					trackBufferArr = trackBufferArr.reject({|item| item[0] == thisSlot });
					cond = Condition(false);

					this.autoRoute(thisTrack, {
						cond.test = true; cond.signal;
					});
					cond.wait;
					/*					server.sync;*/
					action.();
				}.fork(AppClock);
			}, {
				"Filter slot not found".warn;
			});
		}, {
			"No filters to remove".warn;
		});
	}

	removeTrackFilters {arg type=\track, num= 1, post=true, clear=false, action, refresh=true;
		var thisTrack, thisSlot, ndefCS, arr1, arr2, setArr, cond, condBuf, bufBool=false;
		thisTrack = this.getThisTrack(type, num);
		if(thisTrack.notNil, {
			thisTrack = thisTrack.copyRange(0, thisTrack.size-2);
			if(thisTrack.size > 2, {
				arr1 = [thisTrack[0], thisTrack.last];
				arr2 = thisTrack.copyRange(1, thisTrack.size-2);
				if(filterBuff.notNil, {
					bufBool = filterBuff.flop[0].detect({|item|
						item.cs.find(type.asString.capitalise ++ "_" ++ num).notNil; }).notNil;
				});
				{
					arr2.do{|item|
						this.ndefModClear(item[0]);
						ndefCS = "Ndef(" ++ item[0].cs ++ ").clear(" ++ fadeTime ++ ");";
						ndefCS.radpost.interpret;
						thisTrack.remove(item);
						if(type == \master, {num=""});
						setArr = this.findTrackArr((type ++ num).asSymbol);
						masterNdefs[setArr[0]].remove(Ndef(item[0]));
						specs[setArr[0]] = specs[setArr[0]].reject({|it| it[0] == item[0] });
						tracks[setArr[0]] = tracks[setArr[0]].reject({|it| it[0] == item[0] });
						filters = filters.reject({|it| it[0] == item[0] });
						trackDataArr = trackDataArr.reject({|it| it[0] == item[0] });
						trackBufferArr = trackBufferArr.reject({|it| it[0] == item[0] });
						server.sync;
					};
					if(clear.not, {
						cond = Condition.new;
						this.autoRoute(arr1, {
							cond.test=true; cond.signal;
						});
					});
					if(bufBool.not, {
						action.();
						if(refresh, {
							{this.refreshFunc;}.defer;
						});
					});
				}.fork;
				{
					condBuf = Condition.new;
					arr2.flop[0].do{|item|
						condBuf.test = false;
						this.removeBufFromFilter(item, {
							condBuf.test = true; condBuf.signal;
						});
					};
					if(bufBool, {
						condBuf.wait;
						action.();
						if(refresh, {
							{this.refreshFunc;}.defer;
						});
					});
				}.fork;
			}, {
				if(post, {
					"No filters to remove".warn;
				});
				action.();
			});
		});
	}

	removeAllFilters {arg action, refresh=true;
		var cond;
		cond = Condition.new;
		{
			mixTrackNames.do({|item|
				var trackArr;
				cond.test = false;
				trackArr = item.asString.divNumStr;
				if(trackArr[1].isNil, {trackArr[1] = 1});
				this.removeTrackFilters(trackArr[0].asSymbol, trackArr[1], false, false, {
					cond.test = true; cond.signal;
				}, false);
				cond.wait;
			});
			if(refresh, {
				{this.refreshFunc;}.defer;
			});
			action.();
		}.fork;
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
			filterTag = this.findFilterTag(filterInfo[0], filterInfo[1], filterInfo[2]);
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
			"Filter info not found".warn;
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

	argToFilterTag {arg type, arg1, arg2;
		var tagString;
		tagString = "filter" ++ type.asString.capitalise ++ "_" ++ arg1 ++ "_" ++ arg2;
		^tagString.asSymbol;
	}

	findFilterTag {arg type, arg1, arg2;
		var tagString, tagIndex;
		tagString = this.argToFilterTag(type, arg1, arg2);
		if(filters.notNil, {
			tagIndex = filters.flop[0].indexOf(tagString);
		});
		if(tagIndex.notNil, {
			^filters.flop[0][tagIndex]
		}, {
			^nil;
		});
	}

	convFilterTag {arg filterTag;
		var keyString, resultArr, varArr;
		keyString = filterTag.asString.replace("filter").toLower;
		varArr =  keyString.asString.split($_);
		resultArr = [varArr[0].asSymbol, varArr[1], varArr[2] ];
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
		("Ndef(" ++ filterNdefKey.cs ++ ").lag(" ++ newNdefArgs.cs.asString
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
		/*this.getInputs(\track).collect{|item| item.numChannels}
		.do{|item, index|
		sysChans[index] = item;
		};*/
		mixTrackNames.do{|item|
			var spatialType;
			spatialType = space.flop[1][space.flop[0].indexOf(("space" ++
				item.asString.capitalise).asSymbol)];
			if([\pan4, \pan6].includes(spatialType).not, {
				sysPan = sysPan.add(0);
			}, {sysPan = sysPan.add(1)
			});
		};

		sysChans[mixTrackNdefs.size-1] = Ndef(("space" ++
			mixTrackNames.last.asString.capitalise).asSymbol).numChannels;

		if(outputSettings.isNil, {
			outputSettings = \master!(mixTrackNames.size-1);
		});
		if(soloStates.isNil, { soloStates = 0!mixTrackNames.size });
		if(muteStates.isNil, { muteStates = 0!mixTrackNames.size });
		if(recStates.isNil, { recStates = 0!(mixTrackNames.size-1) ++ [1] });

	}

	mixGUI {arg updateFreq=10;
		var sends, knobColors, winHeight, winWidth, knobSize, canvas,
		panKnobTextArr, panKnobArr, sliderTextArr, sliderArr, levelTextArr,
		levelArr, vlay, sendsMenuArr, sendsKnobArr, outputMenuArr,
		muteButton, recButton, soloButton, spaceButton, volButtons, buttonsLay1,
		buttonsLay2, oscDefFunc, levelSoloStates, fxSlotArr, trackLabelArr, spaceTextLay,
		popupmenusize, panSpec, mixInputLabels, trackInputSel, inputArray,
		numBuses, thisInputLabel, busInLabels, maxBusIn, knobFunc, busInSettings,
		guiFunc, fltMenuWindow, oldMixerWin, slotsSizeArr, sumWidth, spaceGap,
		sumHeight, inKnob, gapHeight, peakMax, inKnobArr, inputMenuArr;

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

		levelSoloStates = sysChans.collect({|item, index| 1!item }).flat;

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
			{
				nodeTime.yield;
				oldMixerWin.close;
				server.latency.yield;
				server.sync;
				oscDefFunc.();
			}.fork(AppClock);
		});
		mixerWin = ScrollView().name_("Assemblage");
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

		canvas = View();
		canvas.background_(Color.black);

		sysChans.do{|item, index|
			var slider, level, sliderText, levelText, hlay, thisLay, ts, finalLayout, slotsSize,
			panKnob, panKnobText, panKnobText1, panKnobText2, outputMenu, outputLabel,
			sendsMenu, sendsLabel, sendsKnobs, sendsLay, inputMenu, inputLabel,
			inputLabelArr, mixInputLabelArr, fxLabel, fxSlot, trackLabel, trackColor,
			thisInputVal, butUIHeight, butUIWidth, sendsString, soloButtonFunc;
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
			/*.background_(colorMeter).stringColor_(Color.white)*/
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
					["S", Color.white, colorWarning]];
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
				["R", Color.white, colorCritical]];
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
				["M", Color.white, colorTrack]];
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

			if(index == (sysChans.size-1), {
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
				outputMenu.items = ["", "master"];
				if(outputSettings[index] == 'master', {
					outputMenu.value = 1;
				}, {
					outputMenu.value = 0;
				});
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
				sends.do{var smenu, sknob, sendItems;
					smenu = PopUpMenu().maxHeight_(popupmenusize).minHeight_(popupmenusize)
					.maxWidth_(slotsSize-popupmenusize);

					sendItems = mixTrackNames.select({|item|
						item.asString.find("bus").notNil }).collect{|it|
						it.asString.divNumStr[1] };
					sendItems = sendItems ++ (sendItems.maxItem + 1);

					smenu.items = [""] ++ sendItems.collect({|item| "bus" ++ item });

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
			inputLabel = StaticText(canvas).align_(\right).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10)
			.maxWidth_(46).minWidth_(46);
			inputLabel.font = basicFont; inputLabel.string_("Input");
			//trim knob
			inKnob = Knob().minWidth_(popupmenusize).maxWidth_(popupmenusize)
			.maxHeight_(popupmenusize).minHeight_(popupmenusize);
			inKnob.color = knobColors;
			inKnob.centered_(true).action = {|knob| var thisKey, thisSpec;
				thisKey = ("in" ++ mixTrackNames[index].asString.capitalise).asSymbol;
				thisSpec = this.getSpec(thisKey, \trim).asSpec;
				("Ndef(" ++ thisKey.cs ++ ").set('trim', " ++
					thisSpec.map(knob.value) ++ ");").radpostcont.interpret;
			};
			inKnobArr = inKnobArr.add(inKnob);

			inputLabelArr = HLayout(*[ [inputLabel, align: \right], [inKnob, align: \right] ]);
			//input menu
			inputMenu = PopUpMenu().maxHeight_(popupmenusize)
			.minHeight_(popupmenusize).minWidth_(slotsSize).maxWidth_(slotsSize);

			this.inputLablesFunc(index, mixTrackNames, inputMenu);

			inputMenu.background_(Color.black).stringColor_(Color.white)
			.font_(basicFont).action = { arg menu; var arr, trackInInf;
				trackInInf = 	mixTrackNames[index].asString.divNumStr;

				this.labelsToInFunc(trackInInf, menu.item);

			};

			inputMenuArr = inputMenuArr.add(inputMenu);

			setInputMenu = {|indMenu, valMenu|
				inputMenuArr[indMenu].value = valMenu;
			};

			sliderArr = sliderArr.add(slider);
			levelArr = levelArr.add(level);
			ts = [slider] ++ level;
			thisLay = HLayout(*ts);
			//track name label
			case
			{mixTrackNames[index].asString.find("track").notNil} {
				trackColor = colorTrack}
			{mixTrackNames[index].asString.find("bus").notNil} {
				trackColor = colorBus}
			{mixTrackNames[index].asString.find("master").notNil} {
				trackColor = colorMaster};
			trackLabel = StaticText(canvas).align_(\center).background_(trackColor)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			trackLabel.font = basicFont;
			trackLabel.string_(mixTrackNames[index].asString.capitalise);
			trackLabel.minWidth_(slotsSize).maxWidth_(slotsSize);
			trackLabelArr = trackLabelArr.add(trackLabel);

			//input
			[[inputLabelArr, align: \bottom], [inputMenu, align: \bottom]].do{|lay|
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

		//setting up interface
		sliderTextArr.do{|item, index| item.font = basicFont; item.string_(
			mixTrackNdefs[index].getKeysValues.collect({|item|
				if(item[0] == \volume, {item[1]});
		})[0].round(0.1).asString);
		};
		levelTextArr.do{|item, index| item.font = basicFont; item.string_("-inf");
			item.mouseDownAction_({
				peakMax[index] = -inf;
				levelTextArr[index].stringColor = Color.white;
				item.string_("-inf");
			});
		};
		peakMax = -inf!levelTextArr.size;
		//panning
		panKnobTextArr.flat.do{|item| item.font = basicFont;};
		panSpec = \pan.asSpec;
		panKnobArr.do{|item, index|
			var panKey, panKeyValues, panValues;
			panKey = ("space" ++ mixTrackNames[index].asString.capitalise).asSymbol;
			panKeyValues = Ndef(panKey).controlKeysValues;
			case
			{panKeyValues.size == 0} {panValues = 0}
			{panKeyValues.includes(\pan)} {
				panValues = panKeyValues[panKeyValues.indexOf(\pan)+1]}
			{panKeyValues.includes(\panx)} {
				panValues = [panKeyValues[panKeyValues.indexOf(\panx)+1],
					panKeyValues[panKeyValues.indexOf(\pany)+1]
			]};
			if(sysPan[index] == 0, {
				if(panValues.cs.find("mod").notNil, {
					item.background = colorCritical;
					item.enabled = false;
					panValues = 0;
				});
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
			var volSpec, volValue;
			volSpec = this.getSpec(mixTrackNames[index], \volume).asSpec;
			volValue = mixTrackNdefs[index].getKeysValues.collect({|item|
				if(item[0] == \volume, {item[1]});
			})[0];
			if(volValue.cs.find("mod").notNil, {
				item.background = colorCritical;
				item.enabled = false;
				volValue = 0;
				sliderTextArr[index].string_(volValue);
			});
			item.value_(
				volSpec.unmap( volValue );
			).action_({|val|
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

		setInKnob = {|index, value|
			var trimSpec, trimKey;
			trimKey = ("in" ++ mixTrackNames[index].asString.capitalise).asSymbol;
			trimSpec = this.getSpec(trimKey, \trim).asSpec;
			inKnobArr[index].value = trimSpec.unmap(value);
		};

		levelArr.do{|it| it.do{|item|
			item.meterColor = colorMeter;
			item.warningColor = colorWarning;
			item.criticalColor = colorCritical;
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

		gapHeight = 6;
		sumHeight = panKnobArr.collect{|item| item.bounds.height }.maxItem + gapHeight;
		sumHeight = 45 + 9 + sumHeight + (7 * (10+gapHeight)) + (2 * (12+gapHeight)) +
		(180+gapHeight) + (sendsMenuArr[0].size*(14+gapHeight)) +
		(fxSlotArr[0].size*(14+gapHeight));
		sumHeight = sumHeight + 4;

		sumWidth = 9 + slotsSizeArr.sum + spaceGap.sum + (9-spaceGap.last+2);
		if(sumWidth > screenBounds.width, {
			sumWidth = screenBounds.width;
		});
		if(sumHeight > (screenBounds.height-45), {
			sumHeight = screenBounds.height-45;
		});
		mixerWin.maxWidth_(sumWidth).minWidth_(sumWidth);
		mixerWin.maxHeight_(sumHeight).minHeight_(sumHeight);
		mixerWin.front;

		if(scrollPoint.notNil, {
			//check this bug hasn't been fixed in latest SC version, if so, remove fork
			{0.001.yield; mixerWin.visibleOrigin_(scrollPoint);}.fork(AppClock);
		});

		//setting busIns
		if(busInSettings.isNil, {
			busInSettings = (nil!(sendsMenuArr.flat.size-1)).reshapeLike(sendsMenuArr);
		});

		knobFunc = {|it, trackLb, thisKey|
			var thisNdefVal, selArg, thisSpec, selValue;
			if(thisKey.notNil, {
				thisNdefVal = Ndef(thisKey).getKeysValues;
				selArg = ("vol" ++ (busArr.flop[1][busArr.flop[0].indexOf(thisKey)].collect{|keyVal|
					keyVal.key}.indexOf(trackLb) + 1)).asSymbol;
				thisSpec = this.getSpec(thisKey.asString.replace("In", "").asSymbol, \volume).asSpec;
				selValue = thisNdefVal.flop[1][thisNdefVal.flop[0].indexOf(selArg)];
				if(selValue.cs.find("mod").notNil, {
					it.background = colorCritical;
					it.enabled = false;
					selValue = -inf;
				});
				it.value = thisSpec.unmap( selValue );
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
				if(thisTackNameInd.notNil, {
					sendsMenuArr[thisTackNameInd].do{|it, ind|
						var labInArr;
						if(item[1][ind].notNil, {
							it.value = it.items.collect({|it| it.divNumStr[1] })
							.indexOf(item[1][ind].asString.divNumStr[1]);
							busInSettings[thisTackNameInd][ind] = it.item;
						});
					};
					sendsKnobArr[mixTrackNames.indexOf(item[0])].do{|it, ind|
						knobFunc.(it, item[0], item[1][ind]);
					};
				});
			};
		});

		inKnobArr.do{|item, index| var thisKey, trimKeys, thisSpec, thisTrimKey;
			thisKey = ("in" ++ mixTrackNames[index].asString.capitalise).asSymbol;
			trimKeys = Ndef(thisKey).controlKeysValues;
			thisSpec = this.getSpec(thisKey, \trim).asSpec;
			if(trimKeys.notEmpty, {
				thisTrimKey = trimKeys[trimKeys.indexOf(\trim)+1];
				if(thisTrimKey.cs.find("mod").notNil, {
					item.background = colorCritical;
					item.enabled = false;
					thisTrimKey = 0;
				});
				item.value = thisSpec.unmap( thisTrimKey );
			}, {
				item.value = 0.5;
			});
		};

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
							{var trackorbusIndex;
								trackorbusIndex = mixTrackNames[index].asString.divNumStr[1];
								this.setSend(thisTrackLabel, trackorbusIndex, ind+1, thisBusNum);
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
				var trackInfoInt, trackInfoArr, thisFltInfo, tracksFlt, thisFltTags, fltTagArr, thisSlotInfo,
				fltWinFunc1, fltWinFunc2;

				trackInfoArr = mixTrackNames[index].asString.divNumStr;
				if(trackInfoArr[1] == nil, {trackInfoArr[1] = 1});

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
				fltWinFunc1 = {arg menu, labelKey;
					{
						menu.string = labelKey;
						fltMenuWindow.close;
						fltMenuWindow = nil;
						if((ind+1) > (fxsNum-1), {
							server.sync; this.refreshMixGUI;
						});
					}.fork(AppClock);
				};
				fltWinFunc2 = {arg menu, thisListView, irItems, labelKey;
					menu.string = labelKey;
					irItems = PathName(mainPath ++ "SoundFiles/IR/").entries
					.collect({|item| item.fileNameWithoutExtension });
					thisListView.items = [""] ++ irItems;
					thisListView.action = {|sbs|
						this.filter(trackInfoArr[0].asSymbol, trackInfoArr[1], ind+1,
							labelKey, data: [\convrev, sbs.item.asSymbol, 2048], action: {
								{
									fltMenuWindow.close;
									fltMenuWindow = nil;
									if((ind+1) > (fxsNum-1), {
										server.sync; this.refreshMixGUI
									});
								}.fork(AppClock);
						});
					};
				};
				it.mouseDownAction = { arg menu;
					var boundArr, thisBounds, thisArrBounds, thisitemArr,
					thisListView, thisTagFlt, scrollOrg, fltUIArr;
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
						thisitemArr = ([""] ++ SynthFile.read(\filter).sort );
						thisListView = ListView(fltMenuWindow,Rect(0,0,(thisArrBounds[2]),
							(thisArrBounds[3])))
						.items_(thisitemArr)
						.background_(Color.clear)
						.font_(Font("Monaco", 10);)
						.stringColor_(Color.white)
						.hiliteColor_(colorMeter;)
						.action_({ arg sbs;
							var labelKey, irItems;
							labelKey = thisListView.items[sbs.value];
							if(labelKey.asString.find("convrev").isNil, {
								this.filter(trackInfoArr[0].asSymbol, trackInfoArr[1], ind+1,
									labelKey, action: {
										fltWinFunc1.(menu, labelKey);
								});
							}, {
								fltWinFunc2.(menu, thisListView, irItems, labelKey);
							});
						});
					}, {
						fltUIArr = [trackInfoArr[0].asSymbol, trackInfoArr[1], ind+1];
						if(filters.notNil, {
							thisTagFlt = this.findFilterTag(fltUIArr[0],
								fltUIArr[1], fltUIArr[2]);
							if(filters.flop[0].includes(thisTagFlt).not, {
								this.filter(fltUIArr[0], fltUIArr[1], fltUIArr[2],
									menu.string.asSymbol);
							});
						});
						this.filterGUI(fltUIArr[0], fltUIArr[1], fltUIArr[2],
							[menu.bounds.left+mixerWin.bounds.left-scrollPoint.asArray[0],
								(mixerWin.bounds.top-45)+menu.bounds.top+68], menu);
					});
				};
			}
		};

		outputMenuArr.do{|it, ind|
			if(ind != (sysChans.size-1), {
				it.action  = {arg menu;
					outputSettings[ind] = menu.item.asSymbol;
					this.outputMasterFunc;
				};
			});
		};

		setOutputMenu = {arg index, value;
			outputMenuArr[index].value = value;
		};

		Ndef('AssemblageGUI', {
			var in, imp;
			in = sysChans.collect({|item, index|
				if(item == 1, {
					ArrayMax.ar(Ndef(("space" ++ mixTrackNames[index].asString.capitalise)
						.asSymbol).ar)[0];
				}, {
					Ndef(("space" ++ mixTrackNames[index].asString.capitalise).asSymbol).ar;
				});
			});
			in = in.flat;
			imp = Impulse.ar(10);
			SendReply.ar(imp, '/AssemblageGUI',
				[
					RunningSum.ar(in.squared, server.sampleRate / 10),
					Peak.ar(in, Delay1.ar(imp)).lag(0, 3)
				].flop.flat
			);
		});

		oscDefFunc = {
			OSCdef(\AssemblageGUI, {|msg, time, addr, recvPort|
				var dBLow, array, numRMSSampsRecip, numRMSSamps, peakVal, peakAmp, peakArr;
				numRMSSamps = server.sampleRate / updateFreq;
				numRMSSampsRecip = 1 / numRMSSamps;
				dBLow = -80;
				{
					try {
						peakArr = [];
						msg.copyToEnd(3).pairsDo({|val, peak, i|
							var meter, thisPeakVal, value;
							i = i * 0.5;
							meter = 	levelArr.flat[i];
							if(meter.notNil, {
								value = val*levelSoloStates[i];
								meter.value = (value.max(0.0) * numRMSSampsRecip)
								.sqrt.ampdb.linlin(dBLow, 0, 0, 1);
								peakVal = (peak*levelSoloStates[i]).ampdb;
								thisPeakVal = peakVal.linlin(dBLow, 0, 0, 1);
								meter.peakLevel = thisPeakVal;
								peakArr = peakArr.add(peakVal);
							});
						});
						if(peakArr.notNil, {
							peakArr = peakArr.reshapeLike(levelArr);
							peakArr.do{|item, index|
								var peakDb;
								peakDb = item.maxItem;
								if(peakDb < dBLow, {peakDb = -inf });

								if((levelSoloStates.atAll((sysChans.integrate - sysChans[0]))[index] == 0).or(
									muteStates[index] == 1;
								), {
									levelTextArr[index].stringColor = Color.white;
									levelTextArr[index].string = "-inf";
									peakMax[index] = -inf;
								}, {
									if(peakMax[index] < peakDb, {
										peakAmp = peakDb.linlin(dBLow, 0, 0, 1);
										if(peakAmp >= 0.9999, {
											if(levelTextArr[index].notNil, {
												levelTextArr[index].stringColor_(colorCritical);
											});
										}, {
											if(levelTextArr[index].notNil, {
												levelTextArr[index].stringColor_(Color.white);
											});
										});
										levelTextArr[index].string = peakDb.round(0.1).asString;
										peakMax[index] = peakDb;
									});
								});
							};
						});
					} { |error|
						if(error.isKindOf(PrimitiveFailedError).not) { error.throw }
					};
				}.defer;
			}, \AssemblageGUI);
		};

		oscDefFunc.();

		mixerWin.onClose = {
			/*levelTextArr = nil;*/
			/*mixerWin = nil;*/

			OSCdef(\AssemblageGUI).free;
			{
				nodeTime.yield;
				if(mixerWin.visible.isNil, {
					Ndef('AssemblageGUI').clear;
				});
			}.fork(AppClock);

			levelTextArr = nil;

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

	mixer {
		this.refreshMixGUI;
	}

	inputLablesFunc {arg ind, thisTrackNames, thisInputMenu;
		var mixInputLabelArr, mixInputLabels, outputFunc;

		if(thisTrackNames[ind].asString.find("track").notNil, {
			mixInputLabels = (("in" ++
				thisTrackNames[ind].asString.capitalise).asSymbol);
			if(inputs.flop[0].includes(mixInputLabels), {
				mixInputLabels = inputs.flop[1][inputs.flop[0].indexOfEqual(mixInputLabels)];
				if(mixInputLabels.isArray, {
					mixInputLabels = mixInputLabels.collect({|item| item.key });
				}, {
					mixInputLabels = mixInputLabels.key;
				});
				mixInputLabelArr = [mixInputLabels.asString]
				++ Block.ndefs.collect({|item| item.key.asString});
				mixInputLabelArr = mixInputLabelArr.rejectSame.sort;
				if(thisInputMenu.notNil, {
					thisInputMenu.items = mixInputLabelArr;
					thisInputMenu.value = mixInputLabelArr.indexOfEqual(mixInputLabels.asString);
				});
				outputFunc = [mixInputLabelArr, mixInputLabelArr.indexOfEqual(mixInputLabels.asString)];

			});
		}, {
			if(thisInputMenu.notNil, {
				thisInputMenu.items = [thisTrackNames[ind]];
			});
			outputFunc = [thisTrackNames[ind]];
		});
		^outputFunc;
	}

	labelsToInFunc {arg trackInInf, menuItem;
		var arr;
		if(menuItem.includesString("["), {
			arr = menuItem.replace("[", "").replace("]", "").replace(" ", "").split($,);
			arr = arr.collect{|it| Ndef(it.asSymbol) };
			this.input(arr, trackInInf[0].asSymbol, trackInInf[1]);
		}, {
			this.input(Ndef(menuItem.asSymbol), trackInInf[0].asSymbol, trackInInf[1]);
		});
	}

	setTrackIn {arg trackNum=1, inIndex=0;
		var label, mixTrackIndex;
		mixTrackIndex = mixTrackNames.indexOfEqual( (\track ++ trackNum).asSymbol );
		label = this.inputLablesFunc(mixTrackIndex, mixTrackNames);
		if(mixTrackIndex.notNil, {
			this.labelsToInFunc([\track, trackNum], label[0][inIndex]);
			if(mixerWin.notNil, {
				if(mixerWin.notClosed, {
					setInputMenu.(mixTrackIndex, inIndex);
				});
			});
		}, {
			"Track not found".warn;
		});
	}

	getTrackInMenu {arg trackNum=1;
		var mixTrackIndex;
		mixTrackIndex = mixTrackNames.indexOfEqual( (\track ++ trackNum).asSymbol );
		if(mixTrackIndex.notNil, {
			this.inputLablesFunc(mixTrackIndex, mixTrackNames)[0].radpost;
		}, {
			"Track not found".warn;
		});
	}

	getTrackInIndex {arg trackNum=1;
		var mixTrackIndex;
		mixTrackIndex = mixTrackNames.indexOfEqual( (\track ++ trackNum).asSymbol );
		if(mixTrackIndex.notNil, {
			this.inputLablesFunc(mixTrackIndex, mixTrackNames)[1].radpost;
		}, {
			"Track not found".warn;
		});
	}

	getTrackInItem {arg trackNum=1;
		var mixTrackIndex, label;
		mixTrackIndex = mixTrackNames.indexOfEqual( (\track ++ trackNum).asSymbol );
		if(mixTrackIndex.notNil, {
			label = this.inputLablesFunc(mixTrackIndex, mixTrackNames);
			label[0][label[1]].radpost;
		}, {
			"Track not found".warn;
		});
	}

	outputMasterFunc {var thisOutArr;
		thisOutArr = [];
		outputSettings.do{|it, ind|
			if(it == 'master', {
				thisOutArr = thisOutArr.add(
					Ndef(("space" ++ mixTrackNames[ind].asString.capitalise).asSymbol);
				);
			});
		};
		if(thisOutArr.notEmpty, {
			this.input(thisOutArr, \master);
		}, {
			"Ndef('inMaster').source = nil".radpost.interpret;
		});
	}

	setTrackOut {arg trackType=\track, trackNum=1, inIndex=0;
		var mixTrackIndex, label;
		mixTrackIndex = mixTrackNames.indexOfEqual( (trackType ++ trackNum).asSymbol );
		if(inIndex == 0, {
			label = '';
		}, {
			label = 'master';
			inIndex = 1;
		});
		if(mixTrackIndex.notNil, {
			outputSettings[mixTrackIndex] = label;
			this.outputMasterFunc;
			if(mixerWin.notNil, {
				if(mixerWin.notClosed, {
					setOutputMenu.(mixTrackIndex, inIndex);
				});
			});
		}, {
			"Track not found".warn;
		});
	}

	getTrackOutIndex {arg trackType=\track, trackNum=1;
		var mixTrackIndex, outIndex;
		mixTrackIndex = mixTrackNames.indexOfEqual( (trackType ++ trackNum).asSymbol );
		if(mixTrackIndex.notNil, {
			if(outputSettings[mixTrackIndex] == '', {
				outIndex = 0;
			}, {
				outIndex = 1;
			});
			outIndex.postln;
		}, {
			"Track not found".warn;
		});
	}

	getTrackOutItem {arg trackType=\track, trackNum=1;
		var mixTrackIndex, outItem;
		mixTrackIndex = mixTrackNames.indexOfEqual( (trackType ++ trackNum).asSymbol );
		if(mixTrackIndex.notNil, {
			outputSettings[mixTrackIndex].cs.postln;
		}, {
			"Track not found".warn;
		});
	}

	setVolume {arg trackType, trackNum, val, lag, db=true;
		var trackIndex, value;
		if(db.not, {
			value = val.ampdb;
		}, {
			value = val;
		});
		if(trackType == \master, {
			trackIndex = mixTrackNames.indexOf(trackType.asSymbol);
		}, {
			trackIndex = mixTrackNames.indexOf((trackType ++ trackNum).asSymbol);
		});
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
			"Track not found".warn;
		});
	}

	setVolumeLag {arg trackType=\track, trackNum=1, lag=0;
		var ndefCS, symString;
		if(trackType == \master, {
			symString = trackType;
		}, {
			symString = (trackType ++ trackNum);
		});
		ndefCS = ("Ndef(" ++  symString.asSymbol.cs ++
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

	setSend {arg trackType=\track, trackNum=1, slotNum=1, busNum=1, val= -inf, dirMaster=true;
		var trackIndex, funcThis, modArr;
		if(busNum == 0, {
			modArr = ModMap.modNodes;
			if(modArr.notNil, {
				if(modArr.notEmpty, {
					this.unmapSend(trackType, trackNum, slotNum, -inf);
				});
			});
		});
		trackIndex = mixTrackNames.indexOf((trackType.asString ++ trackNum).asSymbol);
		if(trackIndex.notNil, {
			funcThis = {
				if(busNum == 0, {
					this.removeBus(trackNum, slotNum, trackType);
				}, {
					this.bus(trackNum, slotNum, val, trackType);
				});
			}; //track, bus, mix, type
			if(mixerWin.notNil, {
				if(mixerWin.notClosed, {
					{
						setBusIns.(trackIndex,(slotNum-1), busNum);
						if(val != inf, {
							if(busNum != 0, {
								nodeTime.yield;
								this.setSendKnob(trackType, trackNum, slotNum, val);
							});
						});
					}.fork(AppClock);
				}, {
					funcThis.();
				});
			}, {
				funcThis.();
			});
			if(dirMaster, {
				if(outputSettings[trackIndex] == '', {
					this.setTrackOut(trackType, trackNum, 1);
				});
			}, {
				if(outputSettings[trackIndex] == 'master', {
					this.setTrackOut(trackType, trackNum, 0);
				});
			});
		}, {
			"Track not found".warn;
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
					/*"No buses in this track".warn;*/
				});
			}, {
				/*"This bus doesn't exist in this track".warn;*/
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
			thisString = thisArr.select({|item|
				item.find((trackType.asString ++ trackNum)).notNil;
			})[0];
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
			"Wrong track or bus".warn;
		});
	}

	setPanLag {arg trackType=\track, trackNum=1, lag=0, panTag=\pan;
		var tag, ndefCS, symString;
		if(trackType == \master, {
			symString = trackType.asString.capitalise;
		}, {
			symString = trackType.asString.capitalise ++ trackNum;
		});
		tag = ("space" ++ symString).asSymbol;
		ndefCS = "Ndef(" ++ tag.cs ++ ").lag(" ++panTag.cs ++ ", " ++ lag ++ ");";
		ndefCS.radpost.interpret;
	}

	setPan {arg trackType=\track, trackNum=1, val=0, panTag=\pan;
		var tag, ndefCS, trackKey, symString;
		if(trackType == \master, {
			symString = trackType.asString.capitalise;
		}, {
			symString = trackType.asString.capitalise ++ trackNum;
		});
		tag = ("space" ++ symString).asSymbol;
		ndefCS = "Ndef(" ++ tag.cs ++ ").set(" ++panTag.cs ++ ", " ++ val ++ ");";
		ndefCS.radpost.interpret;
		if(mixerWin.notNil, {
			if(mixerWin.notClosed, {
				if(trackType == \master, {
					trackKey = trackType.asSymbol;
				}, {
					trackKey = (trackType ++ trackNum).asSymbol;
				});
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

	setTrim {arg trackType=\track, trackNum=1, val=0;
		var tag, ndefCS, trackKey, symString;
		if(trackType == \master, {
			symString = trackType.asString.capitalise;
		}, {
			symString = trackType.asString.capitalise ++ trackNum;
		});
		tag = ("in" ++ symString).asSymbol;
		ndefCS = "Ndef(" ++ tag.cs ++ ").set('trim', " ++ val ++ ");";
		ndefCS.radpost.interpret;
		if(mixerWin.notNil, {
			if(mixerWin.notClosed, {
				if(trackType == \master, {
					trackKey = trackType.asSymbol;
				}, {
					trackKey = (trackType ++ trackNum).asSymbol;
				});
				setInKnob.(mixTrackNames.indexOfEqual(trackKey), val);
			});
		});
	}

	setTrimLag {arg trackType=\track, trackNum=1, val=0;
		var tag, ndefCS, trackKey, symString;
		if(trackType == \master, {
			symString = trackType.asString.capitalise;
		}, {
			symString = trackType.asString.capitalise ++ trackNum;
		});
		tag = ("in" ++ symString).asSymbol;
		ndefCS = "Ndef(" ++ tag.cs ++ ").lag('trim', " ++ val ++ ");";
		ndefCS.radpost.interpret;
	}

	busMix {arg bus=1, slot=1, mix=0, lag;
		var tag, volTag, ndefCS;
		tag = ("busIn" ++ bus).asSymbol;
		volTag = ("vol" ++ slot).asSymbol;
		if(lag.notNil, {
			ndefCS = "Ndef(" ++ tag.cs ++ ").lag(" ++volTag.cs ++ ", " ++ lag ++ ");";
			ndefCS.radpost.interpret;
		});
		ndefCS = "Ndef(" ++ tag.cs ++ ").set(" ++volTag.cs ++ ", " ++ mix ++ ");";
		ndefCS.radpost.interpret;
	}

	listFx {
		SynthFile.read(\filter).sort.radpost;
	}

	refreshFunc {
		if(mixerWin.notNil, {
			if(mixerWin.visible.notNil, {
				this.refreshMixGUI;
			});
		});
	}

	setFx {arg trackType=\track, num=1, slot=1, filter=\pch, extraArgs, buffer,
		data, remove=false, action, actionBuf;
		var insert, newFilterNdef;
		if(remove, {
			this.removeFilter(trackType, num, slot, {this.refreshFunc; action.();}, {actionBuf.()} );
		}, {
			/*newFilterNdef = this.argToFilterTag(trackType, num, slot);
			this.ndefModClear(newFilterNdef);*/
			if(this.findFilterTag(trackType, num, slot).isNil, {
				insert = false;
			}, {
				insert = true;
			});
			if(extraArgs.notNil, {
				if(extraArgs.select{|item| item.isSymbol}.isEmpty, {
					extraArgs = extraArgs.collect({|item, index| if(index.even, {
						item = SynthFile.read('filter', filter).argNames[item];
					}, {
						item = item;
					});
					});
				});
			});
			this.filter(trackType, num, slot, filter, extraArgs, buffer, data, {
				{this.refreshFunc}.defer;
				action.();
			}, insert);
		});
	}

	setFxs {	arg settingsArr, action;
		var cond, refreshUI, insert, newFilterNdef, thisTracks, arr, thisInfo, dest;
		cond = Condition.new;
		{
			settingsArr.do{|item, index|
				cond.test = false;
				this.filter(item[0], item[1], item[2], item[3], item[4], item[5], item[6], {
					cond.test = true; cond.signal;
				}, true);
				cond.wait;
			};
			thisTracks = settingsArr.collect({|item|
				if(item[0] == \master, {item[0]}, {
					(item[0] ++ item[1]).asSymbol;
				});
			}).rejectSame;
			thisTracks.do{|item|
				arr = this.findTrackArr(item);
				thisInfo = tracks[arr[0]][arr[1]];
				dest = tracks[arr[0]][arr[1]-1][0];
				("Ndef(" ++ item.cs ++ ", " ++ thisInfo[1].filterFunc(Ndef(dest ) ).cs
					++ ");").radpost.interpret;
				server.sync;
			};
			{this.refreshFunc;}.defer;
			action.();
		}.fork;
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
			noUIFunc = {muteStates[trackInd] = value;
				("Ndef(" ++ mixTrackNames[trackInd].cs ++ ").set('mute', "
					++ value ++ ");").radpost.interpret;
			};
			if(mixerWin.notNil, {
				if(mixerWin.visible.notNil, {
					this.muteButArr[trackInd].valueAction = value;
				}, {
					noUIFunc.();
				});
			}, {
				if(muteStates.isNil, {
					muteStates = 0!mixTrackNames.size;
				});
				noUIFunc.();
			});
		}, {
			"Track not found".warn;
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
				if(recStates.isNil, {
					recStates = 0!mixTrackNames.size;
				});
				noUIFunc.();
			});
		}, {
			"Track not found".warn;
		});
	}

	setSolo {arg trackType=\track, num=1, value=1;
		var noUIFunc, trackInd;
		if(trackType != 'master', {
			trackInd = this.getMixTrackIndex(trackType, num);
			if(trackInd.notNil, {
				noUIFunc = {soloStates[trackInd] = value;
					this.masterSoloFunc2
				};
				if(mixerWin.notNil, {
					if(mixerWin.visible.notNil, {
						this.soloButArr[trackInd].valueAction = value;
					}, {
						noUIFunc.();
					});
				}, {
					if(soloStates.isNil, {
						soloStates = 0!mixTrackNames.size;
					});
					noUIFunc.();
				});
			}, {
				"Track not found".warn;
			});
		}, {
			"Master track can\'t be soloed".warn;
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
				/*if(soloStates.isNil, {
				soloStates = 0!mixTrackNames.size;
				});
				*/
				noUIFunc.();
			});
		}, {
			"Track not found".warn;
		});
	}

	filterWinGUI {arg filterTag=\filterTrack_1_1, filterKey=\pch, filterPairs,
		fltWinLeft=0, fltWinDown=0, mixButton;
		var winName, filtersWin, fltCanvas, panKnobTextArr, fltVlay, fltWinWidth, fltWinHeight,
		stringLengh, argArr, specArr, defaultArgArr, specBool, removeButton, fltWinTop, winBool;

		winName = filterTag.asString;
		/*		winName = filterTag.asString.split($_);
		winName[0] = winName[0].asString.replace("filter", "Filter ");
		if(winName.size == 3, {
		winName = [winName[0] ++ winName[1], winName[2]]
		});
		winName = (winName[0] ++ ": " ++ winName[1]) ++ " | " ++ filterKey.asString;*/
		winName = winName ++ " | " ++ filterKey.asString;
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
					if(defaultArgArr[index].isNumber, {
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
						panKnob.value = 0.5;
						panKnob.background = colorCritical; //mod color
						panKnob.enabled = false;
						panKnobText.string = defaultArgArr[index].key.asString.copyRange(0, 7);
					});
				}, {
					panKnob = TextField().minWidth_(180).maxWidth_(180)
					.font_(basicFont)
					.background_(colorTextField)
					.maxHeight_(15).minHeight_(15);
					defaultVal = defaultArgArr[index];
					if(defaultVal.notNil, {
						if(defaultVal.cs.find("Ndef").isNil, {
							panKnob.string = defaultVal.cs;
							panKnobText.string = defaultVal.cs.copyRange(0, 7);
							panKnob.action = {arg field;
								panKnobText.string = field.value;
								("Ndef(" ++ filterTag.cs ++ ").set(" ++ item.cs ++ ", "
									++ field.value ++ ");").radpostcont.interpret;
							};
						}, {
							panKnob.background = colorCritical; //mod color
							panKnob.enabled = false;
							panKnob.string = defaultVal.cs;
							panKnobText.string = defaultVal.key.asString.copyRange(0, 7);
						});
					});

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
			.states_([["", colorCritical, Color.black]])
			.string_("R E M O V E   F I L T E R")
			.font_(basicFont)
			.action = { arg menu;
				var filterInfoArr, fxsNum2;
				filterInfoArr = this.convFilterTag(filterTag);
				this.removeFilter(filterInfoArr[0], filterInfoArr[1].asInt, filterInfoArr[2].asInt, {
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
				"Filter not found".warn;
			});
		}, {
			"No active filters".warn;
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
			"No filters active".warn;
		});
	}

	masterSoloFunc {arg buttstates;
		var inArr, newCS, startCS, endCS, finalCS, cs;
		cs = Ndef(\inMaster).source.cs;
		startCS = cs.find("([");
		endCS = cs.find("].sum");
		inArr = inputs.flop[1][inputs.flop[0].indexOf(\inMaster);];
		newCS = inArr.collect {|item, index|  "Ndef.ar(" ++ item.key.cs ++
			", " ++ item.numChannels ++ ") * "	++ buttstates[index] };
		finalCS = cs.replace(cs.copyRange(startCS+1,endCS), newCS);
		("Ndef('inMaster').source = " ++ finalCS).radpost.interpret;
	}

	masterSoloFunc2 {var newStates;
		if(outputSettings.isNil, {
			outputSettings = \master!(mixTrackNames.size-1);
		});
		soloStates.do({|item, index|
			if(outputSettings[index] == \master, {
				newStates = newStates.add(item);
			});
		});
		if(soloStates.includes(1), {
			if(newStates.includes(1), {
				this.masterSoloFunc((newStates));
				if(Ndef('master').getKeysValues.collect{|item| item == [\off, 1] }.includes(true), {
					{
						Ndef(\master).fadeTime.yield;
						"Ndef('master').set('off', 0)".radpost.interpret;
					}.fork;
				}, {
					Ndef('master').set('off', 0);
				});
			}, {
				"Ndef('master').set('off', 1)".radpost.interpret;
			});
		}, {
			this.masterSoloFunc((1!newStates.size););
			if(Ndef('master').getKeysValues.collect{|item| item == [\off, 1] }.includes(true), {
				"Ndef('master').set('off', 0)".radpost;
				Ndef('master').set('off', 0);
			});
		});
	}

	prepareRecording {arg headerFormat = "wav", sampleFormat = "int16";
		var recPath, timestamp, recTracks;
		recPath = Radicles.mainPath ++ "SoundFiles/Record/";
		timestamp = Date.localtime;
		if(recStates.isNil, { recStates = 0!(mixTrackNames.size-1) ++ [1] });
		recStates.do({|item, index|
			if(item == 1, {
				recTracks = recTracks.add(("space" ++
					mixTrackNames[index].asString.capitalise).asSymbol);
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
				recTracks = recTracks.add(("space" ++
					mixTrackNames[index].asString.capitalise).asSymbol);
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
		("Ndef('masterOut', {var out; \nout = Ndef.ar('spaceMaster', " ++
			Ndef(\spaceMaster).numChannels ++ ");\n\t" ++	arr3.asString ++ ";\n\t});")
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
			.states_([["", Color.white, Color.black], ["", Color.white, colorMaster] ]);
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
					funcSource = Ndef(\spaceMaster).source;
					"Ndef('spaceMaster').clear;".radpost.interpret;
					Ndef(\spaceMaster).fadeTime.yield;
					server.sync;
					("Ndef('spaceMaster', " ++ funcSource.cs ++ ");").radpost.interpret;
					"Ndef('spaceMaster')[1] = \\filter -> {arg in; (in).clip2};".radpost.interpret;
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
		.hiliteColor_(colorMeter;)
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

	globFadeTime {
		masterNdefs.flat.do{|item| item.fadeTime = fadeTime};
	}

	fadeTime {arg secs;
		{
			Radicles.fadeTime = secs;
			server.sync;
			this.globFadeTime;
		}.fork;
	}

	garbage {arg wait, extraArr;
		var keys, envir;
		keys = masterNdefs.flat.collect({|item| item.key });
		keys = keys ++ ['masterOut', 'AssemblageGUI'] ++ extraArr;
		envir = Ndef.all[server.asSymbol];
		{
			wait.yield;
			keys.do{|item|
				if(envir[item].source.isNil, {
					envir.removeAt(item);
				});
			}; //garbage collection
		}.fork;
	}

	modFunc {arg ndefKey, argIn, type, extraArgs, func,
		mul=1, add=0, min, val, warp, lag, thisSpec;
		var filterType, index, keyValues, spec, newArr, specInd;
		if(argIn.isNumber, {
			index = argIn-1;
		}, {
			index = Ndef(ndefKey).controlKeys.indexOf(argIn)
		});
		if(index.notNil, {
			keyValues = Ndef(ndefKey).getKeysValues[index];
			if(ndefKey.cs.find("busIn").isNil, {
				spec = [];
				specs.do{|item| spec = (item ++ spec) };
				specInd =	spec.flop[1][spec.flop[0].indexOf(ndefKey);];
				if(specInd.isNil, {
					spec = [-1,1];
				}, {
					if(specInd.notEmpty, {
						spec =specInd.detect({|item| item[0] == keyValues[0] });
						spec = spec[1];
						if(spec.includes(\db), {
							spec = spec.copyFromStart(1);
							spec = spec.collect({|item| if(item == -inf, {item = -90}, {item = item});});
						});
					}, {
						spec = [-1,1];
					});
				});
			}, {
				spec = [-90, 6];
			});
			if(thisSpec.notNil, {spec = thisSpec});
			if(spec.isNil, {spec = [-1,1] });
			^ModMap.map(Ndef(ndefKey), keyValues[0], type, spec, extraArgs,
				func, mul, add, min, val, warp, lag);
		}, {
			"Argument doesn't match synth".warn;
		});
	}

	modMix {arg trackType, trackNum, modArg, modType, extraArgs,
		func, mul=1, add=0, min, val, warp, lag, thisSpec;
		var typeKey, ndefKey;
		typeKey = trackType.asString;
		case
		{(modArg == \vol).or(modArg == \volume)} {
			ndefKey = (typeKey ++ trackNum); modArg = \volume }
		{modArg == \pan} {ndefKey = (\space ++ typeKey.capitalise ++ trackNum);}
		{modArg == \trim} {ndefKey = (\in ++ typeKey.capitalise ++ trackNum);};
		ndefKey = ndefKey.asSymbol;
		{
			this.modFunc(ndefKey, modArg, modType, extraArgs, func,
				mul, add, min, val, warp, lag, thisSpec);
			server.sync;
			this.refreshFunc;
		}.fork(AppClock);
	}

	updateFxWin {arg ndefKey;
		var convTag, getWin;
		if(filtersWindow.notNil, {
			if(filtersWindow.notEmpty, {
				getWin = filtersWindow.detect({|item, index|
					item.name.find(ndefKey.asString).notNil });
				if(getWin.notNil, {
					getWin.close;
				});
				server.sync;
				convTag = this.convFilterTag(ndefKey);
				this.filterGUI(convTag[0], convTag[1], convTag[2]);
			});
		});
	}

	fxWarn {arg filterNum, action;
		var ndefKey, filterType, filterInfo;
		if(filters.notNil, {
			filterInfo = filters[filterNum-1];
			if(filterInfo.notNil, {
				ndefKey = filterInfo[0];
				filterType = filterInfo[1];
				if(ndefKey.notNil, {
					action.(ndefKey, filterType);
				}, {
					"Filter not found".warn;
				});
			}, {
				"Incorrect filter number".warn;
			});
		}, {
			"No filters are active".warn;
		});
	}

	fxTrackWarn {arg trackType, trackNum, trackSlot, action, post=true;
		var ndefKey;
		ndefKey = this.argToFilterTag(trackType, trackNum, trackSlot);
		if(filters.notNil, {
			if(filters.flop[0].includes(ndefKey), {
				action.(ndefKey);
			}, {
				if(post, {
					"No filter matches specified track and slot numbers".warn;
				});
			});
		}, {
			if(post, {
				"No filters are active".warn;
			});
		});
	}

	prepArrArg {arg fxArg, ndefKey, type=\set, value;
		var newArr1, newArr2, thisArg;
		if(fxArg.isArray, {
			if(fxArg.rank == 2, {fxArg = fxArg.flat});
			if(fxArg.select({|item| item.isSymbol }).isEmpty, {
				newArr1 = fxArg.reshape((fxArg.size/2).asInt,2);
				newArr2 = newArr1.flop[0].collect{|item| Ndef(ndefKey).controlKeys[item-1] };
				thisArg = [newArr2, newArr1.flop[1]].flop.flat;
				("Ndef(" ++ ndefKey.cs ++ ")." ++ type.asString ++ "(" ++
					thisArg.cs.replace("[", "").replace("]", "") ++
					");").radpostcont.interpret;
			}, {
				("Ndef(" ++ ndefKey.cs ++ ")." ++ type.asString ++ "(" ++
					fxArg.cs.replace("[", "").replace("]", "") ++
					");").radpostcont.interpret;
			});
		}, {
			("Ndef(" ++ ndefKey.cs ++ ")." ++ type.asString ++ "(" ++
				fxArg.cs ++ ", " ++ value.cs ++ ");").radpostcont.interpret;
		});
	}

	setFxArg {arg filterNum, fxArg, value;
		var thisArg, index;
		this.fxWarn(filterNum, {|ndefKey|
			if(fxArg.notNil, {
				{
					if(fxArg.isNumber, {
						index = fxArg-1;
						thisArg = Ndef(ndefKey).controlKeys[index];
						if(thisArg.notNil, {
							("Ndef(" ++ ndefKey.cs ++ ").set(" ++ thisArg.cs ++ ", " ++
								value.cs ++ ");").radpostcont.interpret;
						}, {
							"Fx argument doesn't exist".warn;
						});
					}, {
						("Ndef(" ++ ndefKey.cs ++ ").set(" ++ fxArg.cs ++ ", " ++
							value.cs ++ ");").radpostcont.interpret;
						this.prepArrArg(fxArg, ndefKey, \set, value);
					});
					server.sync;
					this.updateFxWin(ndefKey);
				}.fork(AppClock);
			}, {
				Ndef(ndefKey).controlKeys.postln;
			});
		});
	}

	setFxArgTrack {arg trackType, trackNum, trackSlot, fxArg, value, update=true;
		var ndefKey, thisArg, index, newArr1, newArr2;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			if(fxArg.notNil, {
				{
					if(fxArg.isNumber, {
						index = fxArg-1;
						thisArg = Ndef(ndefKey).controlKeys[index];
						if(thisArg.notNil, {
							("Ndef(" ++ ndefKey.cs ++ ").set(" ++ thisArg.cs ++ ", " ++
								value.cs ++ ");").radpostcont.interpret;
						}, {
							"Fx argument doesn't exist".warn;
						});
					}, {
						this.prepArrArg(fxArg, ndefKey, \set, value);
					});
					server.sync;
					if(update, {
						this.updateFxWin(ndefKey);
					});
				}.fork(AppClock);
			}, {
				Ndef(ndefKey).controlKeys.postln;
			});
		});
	}

	lagFxArg {arg filterNum, fxArg, value;
		var thisArg, index;
		this.fxWarn(filterNum, {|ndefKey|
			if(fxArg.notNil, {
				{
					if(fxArg.isNumber, {
						index = fxArg-1;
						thisArg = Ndef(ndefKey).controlKeys[index];
						if(thisArg.notNil, {
							("Ndef(" ++ ndefKey.cs ++ ").lag(" ++ thisArg.cs ++ ", " ++
								value.cs ++ ");").radpost.interpret;
						}, {
							"Fx argument doesn't exist".warn;
						});
					}, {
						this.prepArrArg(fxArg, ndefKey, \lag, value);
					});
					server.sync;
					this.updateFxWin(ndefKey);
				}.fork(AppClock);
			}, {
				Ndef(ndefKey).controlKeys.postln;
			});
		});
	}

	lagFxArgTrack {arg trackType, trackNum, trackSlot, fxArg, value;
		var ndefKey, thisArg, index;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			if(fxArg.notNil, {
				{
					if(fxArg.isNumber, {
						index = fxArg-1;
						thisArg = Ndef(ndefKey).controlKeys[index];
						if(thisArg.notNil, {
							("Ndef(" ++ ndefKey.cs ++ ").lag(" ++ thisArg.cs ++ ", " ++
								value.cs ++ ");").radpost.interpret;
						}, {
							"Fx argument doesn't exist".warn;
						});
					}, {
						this.prepArrArg(fxArg, ndefKey, \lag, value);
					});
					server.sync;
					this.updateFxWin(ndefKey);
				}.fork(AppClock);
			}, {
				Ndef(ndefKey).controlKeys.postln;
			});
		});
	}

	setFxArgs {arg arr;
		arr.do{|item|
			this.setFxArg(item[0], item[1]);
		};
	}

	setFxArgTracks {arg arr;
		arr.do{|item|
			this.setFxArgTrack(item[0], item[1], item[2], item[3]);
		};
	}

	lagFxArgs {arg arr;
		arr.do{|item|
			this.lagFxArg(item[0], item[1]);
		};
	}

	lagFxArgTracks {arg arr;
		arr.do{|item|
			this.lagFxArgTrack(item[0], item[1], item[2], item[3]);
		};
	}

	modFx {arg filterNum, modArg, modType, extraArgs,
		func, mul=1, add=0, min, val, warp, lag, thisSpec;
		var typeKey, ndefKey;
		this.fxWarn(filterNum, {|ndefKey|
			if(modArg.notNil, {
				{
					this.modFunc(ndefKey, modArg, modType, extraArgs, func,
						mul, add, min, val, warp, lag, thisSpec);
					server.sync;
					this.updateFxWin(ndefKey);
				}.fork(AppClock);
			}, {
				Ndef(ndefKey).controlKeys.postln;
			});
		});
	}

	modFxTrack {arg trackType, trackNum, trackSlot, modArg, modType, extraArgs, func,
		mul=1, add=0, min, val, warp, lag, thisSpec;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			if(modArg.notNil, {
				{
					this.modFunc(ndefKey, modArg, modType, extraArgs, func,
						mul=1, add=0, min, val, warp, lag, thisSpec);
					server.sync;
					this.updateFxWin(ndefKey);
				}.fork(AppClock);
			}, {
				Ndef(ndefKey).controlKeys.postln;
			});
		});
	}

	prepareModSend {arg trackType, trackNum, sendSlot;
		var trackKey, indArr, busInArr, slotArr, volArg, thisBusIn;
		trackKey = (trackType ++ trackNum).asSymbol;
		busArr.flop[1].do{|item, index|
			if(item.notNil, {
				if(item.includes(Ndef(trackKey)), {
					indArr = indArr.add(index);
				});
			});
		};
		if(indArr.notNil, {
			busInArr = indArr.collect({|item| busArr.flop[0][item] });
			thisBusIn = busInArr[sendSlot-1];
			if(thisBusIn.notNil, {
				slotArr = busArr.flop[1][busArr.flop[0].indexOf(thisBusIn)];
				volArg = (\vol ++ (slotArr.indexOf(Ndef(trackKey) ) + 1)).asSymbol;
			});
		});
		^[thisBusIn, volArg];
	}

	modSend  {arg trackType, trackNum, sendSlot, modType, extraArgs,
		func, mul=1, add=0, min, val, warp, lag, thisSpec;
		var trackKey, indArr, busInArr, slotArr, volArg, thisBusIn, modNdef;
		busInArr = this.prepareModSend(trackType, trackNum, sendSlot);
		if(busInArr.includes(nil).not, {
			{
				thisBusIn = busInArr[0];
				volArg = busInArr[1];
				modNdef = this.modFunc(thisBusIn, volArg, modType, extraArgs, func,
					mul, add, min, val, warp, lag, thisSpec);
				modSendArr = modSendArr.add([trackType, trackNum, sendSlot, modNdef]);
				server.sync;
				this.refreshFunc;
			}.fork(AppClock);
		}, {
			"Track and slot numers don't match active send".warn;
		});
	}

	unmapMix {arg trackType, trackNum, modArg, value=0;
		var typeKey, ndefKey;
		{
			typeKey = trackType.asString;
			case
			{(modArg == \vol).or(modArg == \volume)} {
				ndefKey = (typeKey ++ trackNum); modArg = \volume }
			{modArg == \pan} {ndefKey = (\space ++ typeKey.capitalise ++ trackNum);}
			{modArg == \trim} {ndefKey = (\in ++ typeKey.capitalise ++ trackNum);};
			ndefKey = ndefKey.asSymbol;
			ModMap.unmap(Ndef(ndefKey), modArg, value);
			server.sync;
			this.refreshFunc;
		}.fork(AppClock);
	}

	unmapFx {arg filterNum, modArg, value=0;
		this.fxWarn(filterNum, {|ndefKey|
			if(modArg.notNil, {
				{
					ModMap.unmap(Ndef(ndefKey), modArg-1, value);
					server.sync;
					this.updateFxWin(ndefKey);
				}.fork(AppClock);
			});
		});
	}

	unmapFxTrack {arg trackType, trackNum, trackSlot, modArg, value=0, post=true;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			if(modArg.notNil, {
				{
					ModMap.unmap(Ndef(ndefKey), modArg-1, value);
					server.sync;
					this.updateFxWin(ndefKey);
				}.fork(AppClock);
			});
		}, post: post);
	}

	unmapSend {arg trackType, trackNum, sendSlot, value=0;
		var trackKey, indArr, busInArr, slotArr, volArg, thisBusIn;
		busInArr = this.prepareModSend(trackType, trackNum, sendSlot);
		if(busInArr.includes(nil).not, {
			{
				thisBusIn = busInArr[0];
				volArg = busInArr[1];
				ModMap.unmap(Ndef(thisBusIn), volArg, value);
				modSendArr.collect{|item| item.copyFromStart(2) }.collect{|it, ind|
					if(it == [trackType, trackNum, sendSlot], {modSendArr.removeAt(ind);});
				};
				server.sync;
				this.refreshFunc;
			}.fork(AppClock);
		}, {
			"Track and slot numers don't match active send".warn;
		});
	}

	prepModTrackNdefs {arg trackType=\track, trackNum=1;
		var thisInfoArr, thisIndexArr, thisModNodes, result;
		thisModNodes = ModMap.modNodes;
		if(thisModNodes.notNil, {
			thisInfoArr = thisModNodes.flop[1].collect{|item|
				var thisString;
				thisString = item.key.asString;
				if(thisString.find("filter").isNil, {
					thisString.divNumStr;
				}, {
					thisString.split($_);
				});
			};
			thisInfoArr.do{|item, index|
				if((item[0].toLower.find(trackType.asString).notNil).and(
					item[1].interpret == trackNum), {
					thisIndexArr = thisIndexArr.add(index);
				});
			};
			result = thisModNodes.flop[0].atAll(thisIndexArr);
		});
		^result;
	}

	prepModSendNdefs {arg trackType=\track, trackNum=1;
		var newArr, result;
		newArr = modSendArr.select({|item| (item[0] == trackType)
			.and(item[1] == trackNum) });
		if(newArr.notNil, {
			result = newArr.flop[3];
		});
		^result;
	}

	clearModTrackNdefs {arg trackType=\track, trackNum=1;
		var ndefArr, sendNdefs, indArr;
		sendNdefs = this.prepModSendNdefs(trackType, trackNum);
		ndefArr = (this.prepModTrackNdefs(trackType, trackNum) ++
			sendNdefs);
		ndefArr.do{|item|
			(item.cs ++ ".clear(" ++ fadeTime.cs ++ ");").radpost.interpret;
		};
		if(modSendArr.notNil, {
			modSendArr.flop[3].do{|item, index|
				if(sendNdefs.includes(item), {
					indArr = indArr.add(index) });
			};
			modSendArr.removeAtAll(indArr);
		});
	}

	rawFxPreset {arg ndefKey, filter=true;
		var mods, keyValues, arr, arr2, arr3, rawWrite, thisKey;
		Ndef(ndefKey).getKeysValues.do{|item|
			var modArr;
			if(item[1].cs.find("Ndef").notNil, {
				modArr = ModMap.modInfoArr.detect({|it| it[0] == item[1].key });
				keyValues = keyValues.add(	[
					ModMap.modNodes.detect({|it| it[0] == item[1] }).last,
					modArr;
				]);
				mods = mods.add(modArr[0])
			}, {keyValues = keyValues.add(item)});
		};
		if(mods.notNil, {
			arr = ModMap.modNodes.flop[0].collect{|item|
				[item.key,
					item.getKeysValues.collect{|it1|
						if(it1[1].cs.find("Ndef").notNil, {
							[ModMap.modNodes.detect({|it2| it2[0] == it1[1] }).last,
								ModMap.modInfoArr.detect({|it2| it2[0] == it1[1].key })];
						}, {it1});
				}];
			};
			mods.do{|item|
				var thisArr;
				thisArr = arr[arr.flop[0].indexOf(item)];
				if(thisArr[1].flat[1].asString.find("mod").notNil, {
					mods = mods.add(thisArr[1].flat[1]);
				});
			};
			arr.flop[0].do{|item, index|
				if(mods.includes(item), {arr2 = arr2.add(index) });
			};
			arr3 = arr.atAll(arr2);
		});
		keyValues = [ndefKey, keyValues];
		rawWrite = ([keyValues] ++ arr3);
		if(filter, {
			filters.flop[0].do{|item, index|
				if(item == ndefKey, {thisKey = filters.flop[1][index] });
			};
			^[thisKey, rawWrite];
		}, {
			^[ndefKey, rawWrite];
		});

	}

	prepareWriteFxPreset {arg filterKey;
		var presetArr, extraArgs, hasMod, newArr, dataArr, bufData, dataData, trackArrInd;
		presetArr = this.rawFxPreset(filterKey);
		presetArr[1].do{|item, index|
			item[1].do{|it|
				if(it[1][0].cs.find("mod").notNil, {
					hasMod = hasMod.add([item[0], it]);
				}, {
					extraArgs = extraArgs.add([item[0], it]); //extra args
				});
		} };
		if(trackBufferArr.notNil, {
			if(trackBufferArr.notEmpty, {
				trackArrInd = trackBufferArr.flop[0].indexOf(filterKey);
				if(trackArrInd.notNil, {
					bufData = trackBufferArr.flop[1][trackArrInd];
				});
			});
		});
		if(trackDataArr.notNil, {
			if(trackDataArr.notEmpty, {
				trackArrInd = trackDataArr.flop[0].indexOf(filterKey);
				if(trackArrInd.notNil, {
					dataData = trackDataArr.flop[1][trackArrInd];
				});
			});
		});
		presetArr[1].flop[0].do{|item|
			newArr = newArr.add( [item, extraArgs.flop[1].atAll(
				extraArgs.flop[0].indicesOfEqual(item)).flat, bufData, dataData] );
		};
		hasMod.do{|item|
			item[1][1][3] = newArr.flop[1][newArr.flop[0].indexOf(item[1][1][0])];
		};
		dataArr = [presetArr[0], newArr, hasMod];
		^dataArr;
	}

	prepWriteFxPreset {arg filterKey, presetName;
		var dataArr;
		dataArr = this.prepareWriteFxPreset(filterKey);
		if(presetName.notNil, {
			PresetFile.write(\filter, presetName, dataArr);
		});
	}

	writeFxPreset {arg filterNum, presetName;
		this.fxWarn(filterNum, {|ndefKey|
			this.prepWriteFxPreset(ndefKey, presetName);
		});
	}

	writeFxTrackPreset {arg trackType, trackNum, trackSlot, presetName;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			this.prepWriteFxPreset(ndefKey, presetName);
		});
	}

	writeTrackPreset {arg trackType=\track, trackNum=1, presetName;
		var dataArr, labelStr, filterKeyArr;
		labelStr = ("filter" ++ trackType.asString.capitalise ++ "_" ++ trackNum);
		filterKeyArr = filters.flop[0].select{|item| item.cs.find(labelStr).notNil };
		filterKeyArr.do{|item|
			dataArr = dataArr.add(this.prepareWriteFxPreset(item));
		};
		if(presetName.notNil, {
			PresetFile.write(\track, presetName, [trackType, dataArr]);
		});
	}

	writeTracksPreset {arg presetName;
		var filterKeyArr, mixDataArr, dataArr2;
		filterKeyArr = filters.flop[0];
		mixDataArr = this.mixControlKeyValues;
		mixTrackNames.do{|item|
			var trackInfo, labelStr, dataArr;
			trackInfo = item.asString.divNumStr;
			if(trackInfo[1].isNil, {trackInfo[1] = 1});
			labelStr = ("filter" ++ trackInfo[0].asString.capitalise ++ "_" ++ trackInfo[1]);
			filterKeyArr = filters.flop[0].select{|item| item.cs.find(labelStr).notNil };
			filterKeyArr.do{|it|
				dataArr = dataArr.add([item, this.prepareWriteFxPreset(it)]);
			};
			if(dataArr.notNil, {
				dataArr2 = dataArr2.add(dataArr);
			});
		};
		if(presetName.notNil, {
			PresetFile.write(\tracks, presetName, [mixDataArr, dataArr2]);
		});
	}

	listFxPresets {arg filterType=\pch;
		if(filterType.isNil, {
			PresetFile.post(\filter);
		}, {
			PresetFile.readAll(\filter).select{|item| item[1][0] == filterType }.flop[0].cs.postln;
		});
	}

	listTrackPresets {arg trackType=\track;
		if(trackType.isNil, {
			PresetFile.post(\track);
		}, {
			PresetFile.readAll(\track).select{|item| item[1][0] == trackType }.flop[0].cs.postln;
		});
	}

	modRawPreset {arg newNdef, hasMod, action;
		var firstNdef;
		firstNdef = Ndef(newNdef);
		if(hasMod.notNil, {
			hasMod.do{|item|
				server.sync;
				firstNdef = ModMap.map(firstNdef, item[1][0], item[1][1][1], item[1][1][2],
					item[1][1][3], item[1][1][4], item[1][1][5], item[1][1][6], item[1][1][7],
					item[1][1][8], item[1][1][9], item[1][1][10]);
				server.sync;
			};
			action.();
		});
	}

	loadRawFilterPreset {arg newFilterNdef, dataArr, filterType, action;
		var newArr, hasMod, filterArgs, firstNdef, tagArr, cond;
		if(dataArr.notNil, {
			if(dataArr[0] == filterType, {
				newArr = dataArr[1];
				hasMod = dataArr[2];
				filterArgs = newArr[0];
				newFilterNdef ?? {newFilterNdef = filterArgs[0]};
				if([filterArgs[2], filterArgs[3]].detect({|item| item.notNil }).isNil, {
					("Ndef(" ++ newFilterNdef.cs ++ ").set" ++
						filterArgs[1].cs.replace("[", "(").replace("]", ")")++ ";").radpost.interpret;
					server.sync;
				}, {
					tagArr = this.convFilterTag(newFilterNdef);
					cond = Condition(false);
					this.removeFilter(tagArr[0], tagArr[1], tagArr[2], actionBuf: {
						cond.test = true; cond.signal;
					});
					cond.wait;
					server.sync;
					cond.test = false;
					[tagArr[0], tagArr[1], tagArr[2], filterType, filterArgs[1], filterArgs[3]].postln;
					this.setFx(tagArr[0], tagArr[1], tagArr[2], filterType, filterArgs[1], filterArgs[2],
						filterArgs[3], action: {
							cond.test = true; cond.signal;
					});
					cond.wait;
				});
				this.modRawPreset(newFilterNdef, hasMod, {
					action.();
				});
			}, {
				"Wrong filter type for preset".warn;
			});
		});
	}

	loadRawFxPreset {arg newFilterNdef, presetName, filterType=\pch;
		var dataArr;
		this.ndefModClear(newFilterNdef);
		server.sync;
		dataArr = PresetFile.read(\filter, presetName);
		this.loadRawFilterPreset(newFilterNdef, dataArr, filterType);
	}

	loadFxPreset {arg filterNum, presetName;
		this.fxWarn(filterNum, {|ndefKey, filterType|
			{
				this.loadRawFxPreset(ndefKey, presetName, filterType);
				server.sync;
				this.updateFxWin(ndefKey);
			}.fork(AppClock);
		});
	}

	loadFxTrackPreset {arg trackType, trackNum, trackSlot, presetName;
		var filterType;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			{
				if(filters.notNil, {
					filterType = filters.flop[1][filters.flop[0].indexOf(ndefKey)];
				});
				this.loadRawFxPreset(ndefKey, presetName, filterType);
				server.sync;
				this.updateFxWin(ndefKey);
			}.fork(AppClock);
		});
	}

	loadFxPresets {arg arr;
		arr.do{|item|
			this.loadFxPreset(item[0], item[1]);
		};
	}

	loadFxTrackPresets {arg arr;
		arr.do{|item|
			this.loadFxTrackPreset(item[0], item[1], item[2], item[3]);
		}
	}

	prepLoadTrackPreset {arg trackType, trackNum, dataArr;
		var fxSettings, modSettings, newFilterNdef, bufData, dataData;
		dataArr.do{|item, index|
			var slot, filterType, refresh, extraArgs, modArgs;
			if(index == (dataArr.size-1), {
				refresh = true;
			}, {
				refresh = false
			});
			slot = this.convFilterTag(item[1][0][0]).last;
			filterType = item[0];
			extraArgs = item[1][0][1];
			bufData = item[1][0][2];
			dataData = item[1][0][3];
			modArgs = item[2];
			fxSettings = fxSettings.add([trackType, trackNum, slot, filterType, extraArgs, bufData, dataData]);
			newFilterNdef = this.argToFilterTag(trackType, trackNum, slot);
			if(modArgs.notNil, {
				modSettings = modSettings.add([newFilterNdef, modArgs]);
			});
			this.ndefModClear(newFilterNdef);
		};
		^[fxSettings, modSettings];
	}

	loadTrackPreset {arg trackType, trackNum, presetName;
		var dataArr, cond, modSettings, fxSettings, arrSettings;
		{
			cond = Condition(false);
			this.removeTrackFilters(trackType, trackNum, false, action: {
				cond.test = true; cond.signal;
			}, refresh: false);
			cond.wait;
			dataArr = PresetFile.read(\track, presetName);
			arrSettings = this.prepLoadTrackPreset(trackType, trackNum, dataArr[1]);
			fxSettings = arrSettings[0];
			modSettings = arrSettings[1];
			this.setFxs(fxSettings, {
				cond.test = false;
				modSettings.do{|item|
					server.sync;
					this.modRawPreset(item[0], item[1], {cond.test = true; cond.signal;});
					cond.wait;
				}
			});
		}.fork
	}

	loadTracksPreset {arg presetName;
		var dataArr, cond, modSettings, mixSettings, fxSettings,
		arrSettings, prepSettings, trackInfoArr, extraArgs;
		{
			cond = Condition(false);
			this.removeAllFilters(action: {
				cond.test = true; cond.signal;
			}, refresh: false);
			cond.wait;
			dataArr = PresetFile.read(\tracks, presetName);
			mixSettings = dataArr[0];
			prepSettings = dataArr[1];
			prepSettings.do{|item|
				var infoArr;
				infoArr = item.flop[0][0].asString.divNumStr;
				if(infoArr[1].isNil, {infoArr[1] = 1});
				arrSettings = this.prepLoadTrackPreset(infoArr[0].asSymbol, infoArr[1], item.flop[1]);
				fxSettings = fxSettings ++ arrSettings[0];
				if(arrSettings[1].notNil, {
					modSettings = modSettings ++ arrSettings[1];
				});
			};
			extraArgs = this.prepMixSettings(mixSettings);
			extraArgs.do({|item| ("Ndef(" ++ item[0].cs ++ ").set" ++
				item[1].cs.replace("[", "(").replace("]", ");") ).radpost.interpret;
			server.sync;
			});
			mixSettings[1].flop[1].do{|item|
				cond.test = false;
				this.modRawPreset(item[0][0], item, {cond.test = true; cond.signal;});
				cond.wait;
			};
			this.setFxs(fxSettings, {
				cond.test = false;
				modSettings.do{|item|
					server.sync;
					this.modRawPreset(item[0], item[1], {cond.test = true; cond.signal;});
					cond.wait;
				}
			});
		}.fork;
	}

	fxLags {arg filterNum, lag=nil;
		this.fxWarn(filterNum, {|ndefKey, filterType|
			this.filterLags(ndefKey, lag);
		});
	}

	fxTrackLags {arg trackType, trackNum, trackSlot, lag=nil;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			this.filterLags(ndefKey, lag);
		});
	}

	fxsLags {arg arr;
		arr.do{|item|
			this.fxLags(item[0], item[1]);
		}
	}

	fxTracksLags {arg arr;
		arr.do{|item|
			this.fxTrackLags(item[0], item[1], item[2], item[3]);
		};
	}

	fxsTrackLags {arg trackType, trackNum, lag=nil;
		var arr;
		if(filters.notNil, {
			if(filters.notEmpty, {
				filters.flop[0].do({|item, index|
					if(item.cs.find((trackType.asString.capitalise ++ "_" ++ trackNum)).notNil, {
						arr = 	arr.add((index+1));
					});
				});
				if(arr.notNil, {
					this.fxsLags([arr, lag!arr.size].flop);
				});
			});
		});
	}

	ndefModClear {arg ndefKey;
		var modArr, keyArr, activeMods;
		{
			modArr = ModMap.modNodes;
			if(modArr.notNil, {
				if(modArr.notEmpty, {
					keyArr = this.convFilterTag(ndefKey);
					activeMods = ModMap.modNodes.select({|item| item[1].key == ndefKey });
					if(activeMods.notNil, {
						activeMods.flop[2].do{|item|
							this.unmapFxTrack(keyArr[0], keyArr[1], keyArr[2], item, nil, false);
						};
						server.sync;
						ModMap.clearLooseMods;
					});
				});
			});
		}.fork;
	}

	mixControlKeyValues {
		var dataArr, inPreset, trackPreset, spacePreset, hasMod, extraArgs,
		addFunc, newArr, hasMod2, hasModFunc;
		addFunc = {arg presetArr;
			presetArr[1].do{|item, index|
				item[1].do{|it|
					if(it[1][0].cs.find("mod").notNil, {
						hasMod = hasMod.add([item[0], it]);
					}, {
						extraArgs = extraArgs.add([item[0], it]); //extra args
					});
				}
			};
		};
		hasModFunc = {
			if(hasMod.notNil, {
				if(hasMod.notEmpty, {
					hasMod2 = hasMod2.add([hasMod[0][0], hasMod]);
					hasMod = [];
				});
			});
		};
		mixTrackNames.do({|item|
			var in, space;
			in = ("in" ++ item.asString.capitalise).asSymbol;
			space =("space" ++ item.asString.capitalise).asSymbol;
			inPreset = this.rawFxPreset(in, false);
			trackPreset = this.rawFxPreset(item, false);
			spacePreset = this.rawFxPreset(space, false);
			addFunc.(inPreset);
			hasModFunc.();
			addFunc.(trackPreset);
			hasModFunc.();
			addFunc.(spacePreset);
			hasModFunc.();

		});
		dataArr = [extraArgs, hasMod2];
		^dataArr;
	}

	prepMixSettings {arg arr;
		var arr2, arr3, arr4;
		arr2 = arr[0].flop[0];
		arr2.rejectSame;
		arr3 = arr2.collect{|item|
			arr[0].flop[0].indicesOfEqual(item);
		};
		arr4 = arr3.collect{|item| arr[0].flop[1][item].flat }
		^[arr2 ,arr4].flop;
	}

	resetVars {
		tracks=nil; specs=nil; inputs=nil; space=nil; masterNdefs=nil; masterSynth=nil;
		trackNames=nil; masterInput=nil; busArr=nil; filters=nil; filterBuff=nil; mixerWin=nil;
		setVolSlider=nil; mixTrackNames=nil; systemChanNum=nil; mixTrackNdefs=nil;
		basicFont=nil; sysChans=nil; sysPan=nil; setBusIns=nil; setKnobIns=nil; setPanKnob=nil;
		outputSettings=nil; filtersWindow=nil; scrollPoint=nil; fxsNum=nil; soloStates=nil;
		muteStates=nil; recStates=nil; recBStoreArr=nil; mastOutArr=nil; screenBounds=nil;
		mastOutWin=nil; oiIns=nil; oiOuts=nil; recInputArr=nil; winDirRec=nil; muteButArr=nil;
		recButArr=nil; soloButArr=nil; spaceButArr=nil; recordingButton=nil; recordingValBut=nil;
		setOutputMenu=nil; setInputMenu=nil; modSendArr=nil; trackDataArr=nil; trackBufferArr=nil;
		trackCount=1; busCount=1; winRefresh=false;
	}

	clearNdefs {var modArr, keys, modNdefs, cond, busNdefs;
		{
			if(mixerWin.notNil, {
				{mixerWin.close}.defer;
			});
			cond = Condition(false);
			if(ModMap.modNodes.notNil, {
				modArr = masterNdefs.flat.collect{|item|
					ModMap.modNodes.flop[1].indicesOfEqual(item);
				};
				ModMap.modNodes.atAll(modArr.select({|item| item.notNil }).flat;).do{|item|
					ModMap.unmap(item[0], item[2], nil);
					modNdefs = modNdefs.add(item[0]);
					server.sync;
				};
				ModMap.clearLooseMods({|item| cond.test = true; cond.signal;
					(modNdefs = modNdefs ++ item) });
				cond.wait;
			});
			keys = masterNdefs.flat.collect({|item| item.key });
			keys = keys ++ ['masterOut', 'AssemblageGUI'];
			if(busArr.notNil, {
				busArr.flop[0].do{|item|
					if(item.notNil, {
						busNdefs = busNdefs.add(item);
					});
				};
				keys = keys ++ busNdefs;
			});
			modNdefs = modNdefs.collect({|item| item.key });
			keys.do({|item| ("Ndef(" ++ item.cs ++ ").clear(" ++ fadeTime ++ ");").radpost.interpret;
				server.sync;
			});
			server.sync;
			nodeTime.yield;
			this.garbage(fadeTime, (modNdefs ++ busNdefs).postln);
		}.fork;
	}

	nomixer {
		if(mixerWin.notNil, {
			{mixerWin.close}.defer;
		});
	}

}