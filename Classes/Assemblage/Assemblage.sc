Assemblage : Radicles {var <tracks, <specs, <inputs,
	<trackCount=1, <busCount=1, <space, <masterNdefs, <>masterSynth,
	<trackNames, <>masterInput, <busArr, <filters, <filterBuff , <>mixerWin,
	<setVolSlider, <mixTrackNames, <>systemChanNum, <mixTrackNdefs, <basicFont,
	<sysChans, <sysPan, <setBusIns, <setKnobIns, <setPanKnob, <outputSettings,
	<filtersWindow, <scrollPoint, <winRefresh=false, <fxsNum, <soloStates, <muteStates,
	<>recStates, recBStoreArr, <>mastOutArr, <screenBounds, <mastOutWin, <oiIns, <oiOuts,
	<>recInputArr, <winDirRec, <muteButArr, <recButArr, <soloButArr, <spaceButArr,
	<recordingButton, <recordingValBut, <setOutputMenu, <setInputMenu, <modSendArr,
	<trackDataArr, <trackBufferArr, <setInKnob, <>updateFreq=10,
	<sourceWindows, srcWinTracks, srcButtonArr;

	*new {arg trackNum=1, busNum=0, chanNum=1, spaceType, memSize, action;
		^super.new.initAssemblage(trackNum, busNum, chanNum, spaceType, memSize, action);
	}

	initAssemblage {arg trackNum=1, busNum=0, chanNum=2, spaceType, memSize, action;
		var chanMaster, chanTrack, chanBus, spaceMaster, spaceTrack, spaceBus, inArr;
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
				/*oiOuts = Ndef(\spaceMaster).numChannels;*/
				oiOuts = server.options.numOutputBusChannels;
				basicFont = Font("Monaco", 8);
				server.sync;
				action.(trackNum, chanNum);
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
			"Ndef('spaceMaster')[1] = \\filter -> {arg in; (in).clip2};".radcode;
			server.sync;
			"Ndef('spaceMaster').play;".radcode;
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
		spaceType = chanNum.spaceType;
		^spaceType;
	}

	spaceTypeToChanOut {arg spaceType=1;
		var chanNum;
		if(spaceType == \dir, {
			chanNum = systemChanNum;
		}, {
			chanNum = spaceType.spaceToChans;
		});
		^chanNum;
	}

	addTrack {arg type=\track, chanNum=1, spaceType, trackSynth;
		var trackInTag, trackTag, spaceTag, ndefCS, ndefCS1, ndefCS2, trackInSynth,
		spaceSynth, trackInSpecs, spaceSpecs, trackSpecs, thisTrackInfo;
		if([\track, \bus, \master].includes(type), {
			spaceType ?? {spaceType = this.findSpaceType(chanNum);};
			if(chanNum > 2, {
			if(type == \master, {spaceType = \dir;});
			});
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
			ndefCS.radcode;
			ndefCS = ("Ndef(" ++ trackInTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS.radcode;
			ndefCS = ("Ndef(" ++ trackInTag.cs ++ ").set('trim', 0);");
			ndefCS.radcode;
			server.sync;
			ndefCS1 = "Ndef.ar(" ++ trackTag.cs ++ ", ";
			ndefCS1 = (ndefCS1 ++ chanNum.cs ++ ");");
			ndefCS1.radcode;
			ndefCS2 = ("Ndef(" ++ trackTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS2.radcode;
			server.sync;
			spaceTag = ("space" ++ trackTag.asString.capitalise).asSymbol;
			ndefCS1 = "Ndef.ar(" ++ spaceTag.cs ++ ", ";
			ndefCS1 = (ndefCS1 ++ this.spaceTypeToChanOut(spaceType).cs ++ ");");
			ndefCS1.radcode;
			ndefCS2 = ("Ndef(" ++ spaceTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS2.radcode;
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
			intArr.reverse.do{|item| item.radcode; server.sync };
			action.();
		}.fork;
	}

	autoAddTrack {arg type=\track, chanNum, spaceType, trackSynth, trackSpecs, action;
		var trackInfo, insertInd;
		{
			if(soloStates.notNil, {
				if(type == \track, {
					//new tracks go before the first bus, or before the master if there are no buses
					insertInd = mixTrackNames.detectIndex({|item|
						item.asString.beginsWith("bus") or: { item == \master } });
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
			action.(trackInfo);
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

				("Ndef(" ++ inTrack.cs ++ ").clear(" ++ fadeTime ++ ");").radcode;
				("Ndef(" ++ realTrack.cs ++ ").clear(" ++ fadeTime ++ ");").radcode;
				("Ndef(" ++ spaceTrack.cs ++ ").clear(" ++ fadeTime ++ ");").radcode;

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
				this.mixWinBool({
					nodeTime.yield;
					{this.refreshMixGUI;}.defer;
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
		^nil; //not found (previously returned the Assemblage itself)
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
					if(soloStates.notNil, {
						// replaced at once by the solo-adjusted version, which is posted
						ndefCS.radlog;
						this.masterSoloFunc2;
					}, {
						ndefCS.radcode;
					});
				}, {
					ndefCS = this.ndefPrepare(Ndef(trackArr[0][0]), trackArr[0][1].filterFunc(ndefsIn));
					ndefCS.radcode;
				});
			}, {
				if(respace, {
					//this needs more work
					/*if(spaceType.isNil, {spaceType = this.findSpaceType(sysChans.last)});*/
					/*					this.respace(trackArr[0][0], ndefsIn, spaceType);*/

					trackArr =this.getThisTrack(type, num);

					ndefCS = this.ndefPrepare(Ndef(trackArr[0][0]), trackArr[0][1].filterFunc(ndefsIn));
					ndefCS.radcode;
				}, {
					"Channel number input doesn't match track".warn;
				});
			});
			if(type == \track, { this.prSourceChanged(num, ndefsIn) });
		}, {
			"Wrong track type".warn;
		});
	}

	getTrackInput {arg type=\master, num=1;
		var inTag, index, trackTag;
		//inputs are stored under 'in' keys, e.g. inTrack1, inMaster
		if(type == \master, {inTag = ("in" ++ type.asString.capitalise).asSymbol;
			trackTag = type;
		}, {inTag = (("in" ++ type.asString.capitalise) ++ num).asSymbol;
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
		^nil;
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
				numArr = (num..(num + ndefIns.size - 1)); //one track number per input, starting at num
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
		if(ind.notEmpty, {
			if(ind.size == 1, {
				^Ndef(inputs.flop[0][ind[0]]);
			}, {
				^inputs.flop[0].atAll(ind);
			});
		}, {
			"Destination not found".warn;
			^nil;
		});
	}

	bus {arg trackNum=1, busNum=1, mix=1, trackType=\track, action, setVal=true;
		var numChan, busTag, ndefCS1, ndefCS2, ndefCS3, funcBus, thisBusArr,
		busAdd, argIndex, thisBusLabel;
		{
			thisBusLabel = ("inBus" ++ busNum).asSymbol;
			/*"thisBusLabel ".post; thisBusLabel.postln;*/
			numChan = Ndef(thisBusLabel).numChannels;
			busTag = ("busIn" ++ busNum).asSymbol;
			/*"busArr ".post; busArr.postln;
			"busArr[busNum-1][0] ".post; busArr[busNum-1][0].postln;*/
			if(busArr[busNum-1][0].isNil, {
				busArr[busNum-1][0] = busTag;
				ndefCS1 =	("Ndef.ar(" ++ busTag.cs ++ ", " ++ numChan ++ ");" );
				ndefCS1.radcode;
				ndefCS1 = ("Ndef(" ++ busTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
				ndefCS1.radcode;
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
			ndefCS2.radcode;
			server.sync;
			if(setVal, {
				/*"setVal ".post; setVal.postln;*/
				argIndex = busArr[busNum-1][1].indexOf(Ndef((trackType.asString ++ trackNum).asSymbol));
				ndefCS3 = "Ndef(" ++ busTag.cs ++ ").set('vol" ++ (argIndex+1)
				++ "', " ++ mix ++ ");";
				ndefCS3.radcode;
			});
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
					("Ndef(" ++ oldLabel.cs ++ ").clear(" ++ fadeTime ++ ");").radcode;
				}, {
					("Ndef(" ++ oldLabel.cs ++ ").source = nil;").radcode;
				});
				busArr[newBusInd] = nil!2;
				inputs.removeAt(thisBusIndLabel);
				spaceInInd = inputs.flop[0].indexOf(spaceBusLabel);
				if(inputs.flop[1][spaceInInd].isArray.not, {
					if(clearTrack.not, {
						("Ndef(" ++ spaceBusLabel.cs ++ ").source = nil;").radcode;
						inputs.removeAt(spaceInInd);
					});
				});
			}, {
				if(newLabelArr.size == 1, {
					inputs[thisBusIndLabel][1] = newLabelArr[0];
				}, {
					inputs[thisBusIndLabel][1] = newLabelArr;
				});
				("Ndef(" ++ oldLabel.cs ++ ", " ++ newLabelArr.busFunc.cs ++ ");").radcode;
			});
			//look the bus input up again: entries may have been removed above
			spaceInInd = inputs.flop[0].indexOf(spaceBusLabel);
			if(spaceInInd.notNil, {spaceInNdef = inputs.flop[1][spaceInInd]});
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

	getBusInArr {arg thisArr;
		var busLabelArr, sameTracks, result;
		thisArr.do{|item|
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

	getBusInLabels {var busLabelArr, sameTracks, result;
		^this.getBusInArr(busArr);
	}

	busMix {arg bus=1, slot=1, mix=0, lag;
		var tag, volTag, ndefCS;
		tag = ("busIn" ++ bus).asSymbol;
		volTag = ("vol" ++ slot).asSymbol;
		if(lag.notNil, {
			ndefCS = "Ndef(" ++ tag.cs ++ ").lag(" ++volTag.cs ++ ", " ++ lag ++ ");";
			ndefCS.radcode;
		});
		ndefCS = "Ndef(" ++ tag.cs ++ ").set(" ++volTag.cs ++ ", " ++ mix ++ ");";
		ndefCS.radcode;
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

	saveVars {
		^[inputs, space, masterNdefs, masterSynth, trackNames, masterInput, busArr,
			filters, filterBuff, mixerWin, setVolSlider, mixTrackNames, systemChanNum, mixTrackNdefs,
			basicFont, sysChans, sysPan, setBusIns, setKnobIns, setPanKnob, 	outputSettings, filtersWindow,
			scrollPoint, fxsNum, soloStates, muteStates, recStates, recBStoreArr, mastOutArr, screenBounds,
			mastOutWin, oiIns, oiOuts, recInputArr, winDirRec, muteButArr, recButArr, soloButArr, spaceButArr,
			recordingButton, recordingValBut, setOutputMenu, setInputMenu, modSendArr, trackDataArr,
			trackBufferArr, trackCount, busCount, winRefresh];
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

	clearNdefs {arg action;
		var modArr, keys, modNdefs, cond, busNdefs;
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
			//what about unmodding HIDs?
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
			keys.do({|item| ("Ndef(" ++ item.cs ++ ").clear(" ++ fadeTime ++ ");").radcode;
				server.sync;
			});
			server.sync;
			nodeTime.yield;
			this.garbage(fadeTime, (modNdefs ++ busNdefs).postln);
			action.();
		}.fork;
	}

	kill {arg action;
		{
			{this.nomixer}.defer;
			nodeTime.yield;
			this.clearNdefs({
				this.resetVars;
				nodeTime.yield;
				action.();
			});
		}.fork;
	}
}
