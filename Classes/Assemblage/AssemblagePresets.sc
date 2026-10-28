// Assemblage: writing and loading effect, track and mixer presets.
// Part of the Assemblage class: the class and its variables are in Assemblage.sc.

+ Assemblage {
	rawFxPreset {arg ndefKey, filter=true;
		var mods, keyValues, arr, arr2, arr3, rawWrite, thisKey;
		Ndef(ndefKey).getKeysValues.do{|item|
			var modArr;
			if(item[1].cs.find("Ndef").notNil, {
				modArr = ModMap.modInfoArr.detect({|it| it[0] == item[1].key });
				ModMap.modNodes.detect({|it| it[0] == item[1] })[2];
				keyValues = keyValues.add(	[
					ModMap.modNodes.detect({|it| it[0] == item[1] })[2],
					modArr;
				]);
				mods = mods.add(modArr[0])
		}, {keyValues = keyValues.add(item)});};
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
				if(it[1].isArray, {
					if(it[1][0].cs.find("mod").notNil, {
						hasMod = hasMod.add([item[0], it]);
					}, {
						extraArgs = extraArgs.add([item[0], it]); //extra args
					});
				}, {
					extraArgs = extraArgs.add([item[0], it]);
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
		var dataArr, filterKeyArr;
		filterKeyArr = (filters ? []).collect({|item| item[0] }).select{|item|
			this.filterTagOnStrip(item, trackType, trackNum) };
		filterKeyArr.do{|item|
			dataArr = dataArr.add(this.prepareWriteFxPreset(item));
		};
		if(presetName.notNil, {
			PresetFile.write(\track, presetName, [trackType, dataArr]);
		});
	}

	writeTracksPreset {arg presetName;
		var dataArr;
		//same data as projects use; prepWritePreset skips removed (nil) buses
		dataArr = this.prepWritePreset;
		if(presetName.notNil, {
			PresetFile.write(\tracks, presetName, dataArr);
		});
	}

	listFxPresets {arg filterType=\pch;
		if(filterType.isNil, {
			PresetFile.post(\filter);
		}, {
			PresetFile.readAll(\filter).select{|item| item[1][0] == filterType }.flop[0].cs.radpostwarn;
		});
	}

	listTrackPresets {arg trackType=\track;
		if(trackType.isNil, {
			PresetFile.read(\track).dopostln;
		}, {
			PresetFile.readAll(\track).select{|item| item[1][0] == trackType }.flop[0].cs.radpostwarn;
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
						filterArgs[1].cs.replace("[", "(").replace("]", ")")++ ";").radcode;
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
		var dataArr, cond, modSettings, mixSettings, fxSettings, arrSettings, prepSettings,
		trackInfoArr, extraArgs, modMixSettings, busSettings, outSettings, ioSettings;
		{
			cond = Condition(false);
			this.removeAllFilters(action: {
				cond.test = true; cond.signal;
			}, refresh: false);
			cond.wait;
			cond.test = false;
			this.removeAllSends(action: {
				cond.test = true; cond.signal;
			}, refresh: false);
			cond.wait;
			dataArr = PresetFile.read(\tracks, presetName);
			mixSettings = dataArr[0];
			prepSettings = dataArr[1];
			busSettings = dataArr[2];
			outSettings = dataArr[3];
			ioSettings = dataArr[4];
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
			extraArgs.do({|item, index| ("Ndef(" ++ item[0].cs ++ ").set" ++
				item[1].cs.replace("[", "(").replace("]", ");") ).radcode;
			server.sync;
			//solo is not an Ndef value (it stays 1); it is restored from soloStates below
			if(item[1].includes(\mute), {
				muteStates[mixTrackNames.indexOf(item[0])] =
				item[1][(item[1].indexOfEqual(\mute)+1)];
			});
			});
			modMixSettings =mixSettings[1];
			if(modMixSettings.notNil, {
				modMixSettings.flop[1].do{|item|
					cond.test = false;
					this.modRawPreset(item[0][0], item, {cond.test = true; cond.signal;});
					cond.wait;
				};
			});
			if(outSettings.notNil, {
				this.setOutputSettings(outSettings);
				nodeTime.yield;
			});
			if(busSettings.notNil, {
				cond.test = false;
				this.setBusForPreset(busSettings, false, {cond.test = true; cond.signal;});
				cond.wait;
			});
			if(ioSettings.notNil, {
				recStates = ioSettings[0];
				recInputArr = ioSettings[1];
				mastOutArr = ioSettings[2];
				this.restoreSoloStates(ioSettings[3]);
				this.mapOutFunc;
				nodeTime.yield;
			});
			if(fxSettings.notNil, {
				this.setFxs(fxSettings, {
					cond.test = false;
					modSettings.do{|item|
						server.sync;
						this.modRawPreset(item[0], item[1], {cond.test = true; cond.signal;});
						cond.wait;
					};
					nodeTime.yield;
				});
			}, {
				nodeTime.yield;
				this.refreshFunc; //only rebuild the mixer if it is open
			});
		}.fork(AppClock);
	}

	loadPreset {arg presetName;
		var dataArr, cond, modSettings, mixSettings, fxSettings, arrSettings, prepSettings,
		trackInfoArr, extraArgs, modMixSettings, busSettings, outSettings, ioSettings;
		{
			cond = Condition(false);
			this.removeAllFilters(action: {
				cond.test = true; cond.signal;
			}, refresh: false);
			cond.wait;
			"filters removed".postln;
			cond.test = false;
			this.removeAllSends(action: {
				cond.test = true; cond.signal;
			}, refresh: false);
			cond.wait;
			"sends removed".postln;

			dataArr = PresetFile.read(\tracks, presetName);
			mixSettings = dataArr[0];
			prepSettings = dataArr[1];
			busSettings = dataArr[2];
			outSettings = dataArr[3];
			ioSettings = dataArr[4];

			"data imported".postln;

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

			"prepSettings".postln;

			extraArgs = this.prepMixSettings(mixSettings);
			extraArgs.do({|item, index| ("Ndef(" ++ item[0].cs ++ ").set" ++
				item[1].cs.replace("[", "(").replace("]", ");") ).radcode;
			server.sync;
			//solo is not an Ndef value (it stays 1); it is restored from soloStates below
			if(item[1].includes(\mute), {
				muteStates[mixTrackNames.indexOf(item[0])] =
				item[1][(item[1].indexOfEqual(\mute)+1)];
			});
			});

			"mixSettings loaded".postln;

			modMixSettings =mixSettings[1];
			if(modMixSettings.notNil, {
				modMixSettings.flop[1].do{|item|
					cond.test = false;
					this.modRawPreset(item[0][0], item, {cond.test = true; cond.signal;});
					cond.wait;
				};
				"modMixSettings loaded".postln;
			});

			if(outSettings.notNil, {
				this.setOutputSettings(outSettings);
				nodeTime.yield;
			});
			"outSettings loaded".postln;
			if(busSettings.notNil, {
				cond.test = false;
				this.setBusForPreset(busSettings, false, {cond.test = true; cond.signal;});
				cond.wait;
			});
			"busSettings loaded".postln;
			//still some work on this...
			if(ioSettings.notNil, {
				recStates = ioSettings[0];
				recInputArr = ioSettings[1];
				mastOutArr = ioSettings[2];
				this.restoreSoloStates(ioSettings[3]);
				this.mapOutFunc;
				nodeTime.yield;
				/*this.mapOuts(ioSettings[2]);*/
			});
			"ioSettings loaded".postln;

			if(fxSettings.notNil, {
				this.setFxs(fxSettings, {
					cond.test = false;
					modSettings.do{|item|
						server.sync;
						this.modRawPreset(item[0], item[1], {cond.test = true; cond.signal;});
						cond.wait;
					};
					nodeTime.yield;
					"fxSettings loaded".postln;
				});
			}, {
				nodeTime.yield;
				"refresh GUI".postln;
				this.refreshFunc; //only rebuild the mixer if it is open
			});
		}.fork(AppClock);
	}

	mixControlKeyValues {
		var dataArr, inPreset, trackPreset, spacePreset, hasMod, extraArgs,
		addFunc, newArr, hasMod2, hasModFunc;
		addFunc = {arg presetArr;
			presetArr[1].do{|item, index|
				item[1].do{|it|
					if(it[1].isNumber.not, {
						/*it[1][0].postln;*/
						if(it[1][0].cs.find("mod").notNil, {
							hasMod = hasMod.add([item[0], it]);
						}, {
							extraArgs = extraArgs.add([item[0], it]); //extra args
						});
					}, {
						extraArgs = extraArgs.add([item[0], it]);
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
			/*[in, item, space].cs.postln;*/
			inPreset = this.rawFxPreset(in, false);
			/*inPreset.cs.postln;*/
			trackPreset = this.rawFxPreset(item, false);
			/*trackPreset.cs.postln;*/
			spacePreset = this.rawFxPreset(space, false);
			/*spacePreset.cs.postln;*/
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
		arr2 = arr2.rejectSame; //one entry per Ndef, so each .set runs once on load
		arr3 = arr2.collect{|item|
			arr[0].flop[0].indicesOfEqual(item);
		};
		arr4 = arr3.collect{|item| arr[0].flop[1][item].flat }
		^[arr2 ,arr4].flop;
	}

	removeAllSends {arg refresh=false, action;
		{
			this.getBusInLabels.postln;
			this.getBusInLabels.do{|item|
				var trackInform;
				trackInform = item[0].asString.divNumStr;
				item[1].do{|it|
					var thisBusNum;
					thisBusNum = it.asString.divNumStr[1];
					"should post send remove".postln;
					this.removeBus(trackInform[1], thisBusNum, trackInform[0]);
					server.sync;
				};
			};
			if(refresh, { {this.refreshMixGUI}.defer });
			action.();
		}.fork
	}

	setBusForPreset {arg arr, refresh=false, action;
		var busSettings, thisBusArr;
		{
			thisBusArr = arr[0];
			busSettings = this.getBusInArr(thisBusArr);
			/*"busSettings ".post; busSettings.postln;*/
			busSettings.do{|item|
				var trackSetting;
				trackSetting = item[0].asString.divNumStr;
				item[1].do{|it, ind|
					var busNum;
					busNum = it.asString.divNumStr[1];
					this.setSend(trackSetting[0].asSymbol, trackSetting[1], ind+1, busNum, presetMode: true);
				};
			};
			nodeTime.yield;
			thisBusArr.flop[0].do{|item, index|
				if(item.notNil, {
					("Ndef(" ++ item.cs ++ ").set" ++
						arr[1][index].cs.replace("[", "(").replace("]", ")") ++ ";" ).radcode;
				});
			};
			server.sync;
			if(refresh, { {this.refreshMixGUI}.defer });
			action.();
		}.fork
	}

	saveFileInfo {arg presetName;
		var resultArr, trk, bs, ms, mastArr, arr1, arr2, arr3;
		trk = mixTrackNdefs.select{|item| item.key.asString.contains("track") };
		bs = mixTrackNdefs.select{|item| item.key.asString.contains("bus") };
		ms = mixTrackNdefs.select{|item| item.key.asString.contains("master") };
		mastArr = [trk, bs, ms];
		arr1 = mastArr.copyFromStart(1).collect{|item| item.size};
		arr2 = mastArr.collect{|item| item.collect{|it| it.numChannels } };
		arr2 = arr2.collect{|item| if(item.size == 1, {item[0]}, {item});};
		arr3 = [arr1, arr2];
		resultArr = [arr3] ++ [this.prepWritePreset];
		resultArr.postln;
		PresetFile.write(\assemblage, presetName, resultArr);
	}

	prepWritePreset {
		var filterKeyArr, mixDataArr, dataArr2, busSettings, ioSettings;
		mixDataArr = this.mixControlKeyValues;
		if(filters.notNil, {
			filterKeyArr = filters.flop[0];
			mixTrackNames.do{|item|
				var trackInfo, dataArr;
				trackInfo = item.asString.divNumStr;
				if(trackInfo[1].isNil, {trackInfo[1] = 1});
				filterKeyArr = filters.flop[0].select{|item|
					this.filterTagOnStrip(item, trackInfo[0], trackInfo[1]) };
				filterKeyArr.do{|it|
					dataArr = dataArr.add([item, this.prepareWriteFxPreset(it)]);
				};
				if(dataArr.notNil, {
					dataArr2 = dataArr2.add(dataArr);
				});
			};
		});
		if(this.getBusInLabels.notNil, {
			busSettings = [busArr, busArr.flop[0].collect({|item|
				if(item.notNil, {Ndef(item).controlKeysValues}); });];
		});
		ioSettings = [recStates, recInputArr, mastOutArr, soloStates.copy]; //4th item: solo states
		^[mixDataArr, dataArr2, busSettings, outputSettings, ioSettings];
		/*PresetFile.write(\tracks, presetName, [mixDataArr, dataArr2, busSettings, outputSettings, ioSettings]);*/
	}
}
