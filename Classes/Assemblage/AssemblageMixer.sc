// Assemblage: mixer controls: volume, pan, trim, sends, mute, solo, record-arm, space, track inputs and outputs.
// Part of the Assemblage class: the class and its variables are in Assemblage.sc.

+ Assemblage {
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
			/*if([\pan4, \pan6].includes(spatialType).not, {
				sysPan = sysPan.add(0);
			}, {sysPan = sysPan.add(1)
			});*/
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

	setTrackIn {arg trackNum=1, inIndex=0;
		var label, mixTrackIndex;
		mixTrackIndex = mixTrackNames.indexOfEqual( (\track ++ trackNum).asSymbol );
		label = this.inputLablesFunc(mixTrackIndex, mixTrackNames);
		if(mixTrackIndex.notNil, {
			this.labelsToInFunc([\track, trackNum], label[0][inIndex]);
			this.mixWinBool({
				setInputMenu.(mixTrackIndex, inIndex);
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
			"Ndef('inMaster').source = nil".radcode;
		});
	}

	setTrackOut {arg trackType=\track, trackNum=1, inIndex=0, bool=true;
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
			if(bool, {
				this.outputMasterFunc;
			});
			this.mixWinBool({
				setOutputMenu.(mixTrackIndex, inIndex);
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
					value ++ ");").radcode(\radpostcont);
			}, {
				(mixTrackNdefs[trackIndex].cs ++ ".set('volume', " ++
					value ++ ", 'lagTime', " ++ lag ++ ");").radcode(\radpostcont);
			});
			this.mixWinBool({
				setVolSlider.(trackIndex, value); //slider always shows dB
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
		ndefCS.radcode(\radpostcont);
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
					", " ++ vol ++ ", " ++ lagArg.cs ++ ", " ++ lag ++ ");").radcode(\radpostcont);
			};
		});
		^thisFunc;
	}

	setSend {arg trackType=\track, trackNum=1, slotNum=1, busNum=1, val= -inf,
		dirMaster=true, presetMode=false;
		var trackIndex, funcThis, setThisVal;
		if(busNum == 0, {
			//clearing a send: remove its modulation first, if this send is modulated
			if(modSendArr.notNil and: {
				modSendArr.any({|item| item.copyFromStart(2) == [trackType, trackNum, slotNum] })
			}, {
				this.unmodSend(trackType, trackNum, slotNum, -inf);
			});
		});
		//a send to a bus that does not exist (e.g. snd t 1 1 -6, where -6 is read
		//as the bus number) used to throw an error and leave an Ndef 'inBus-6'
		if(busNum != 0 and: { mixTrackNames.includes(("bus" ++ busNum).asSymbol).not }, {
			("Bus " ++ busNum ++ " not found").warn;
			^this;
		});
		trackIndex = mixTrackNames.indexOf((trackType.asString ++ trackNum).asSymbol);
		if(trackIndex.notNil, {
			funcThis = {arg setVal;
				var slotBus;
				if(busNum == 0, {
					//clearing: remove the bus this send slot uses
					slotBus = this.sendSlotBus(trackType, trackNum, slotNum);
					if(slotBus.notNil, {
						this.removeBus(trackNum, slotBus, trackType);
					}, {
						"No send in this slot".warn;
					});
				}, {
					/*"setVal ".post; setVal.postln;
					"	this.bus ".post; [trackNum, busNum, val, trackType].postln;*/
					this.bus(trackNum, busNum, val, trackType, setVal: setVal);
				});
			}; //track, bus, mix, type
			if(presetMode.not, {setThisVal = true}, {setThisVal = false});
			if(mixerWin.notNil, {
				if(mixerWin.notClosed, {
					if(presetMode.not, {
						{
							setBusIns.(trackIndex,(slotNum-1), busNum);
							if(val != inf, {
								if(busNum != 0, {
									nodeTime.yield;
									this.setSendKnob(trackType, trackNum, slotNum, val);
								});
							});
						}.fork(AppClock);
					}, {funcThis.(setThisVal);});
				}, {
					funcThis.(setThisVal);
				});
			}, {
				funcThis.(setThisVal);
			});
			if(presetMode.not, {
				if(dirMaster, {
					if(outputSettings[trackIndex] == '', {
						this.setTrackOut(trackType, trackNum, 1);
					});
				}, {
					if(outputSettings[trackIndex] == 'master', {
						this.setTrackOut(trackType, trackNum, 0);
					});
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
					this.mixWinBool({
						setKnobIns.(trackIndex, (slotNum-1), val);
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
				item.find((trackType.asString ++ trackNum).asSymbol.cs).notNil;
			})[0];
		}, {
			if(synthString.find((trackType.asString ++ trackNum).asSymbol.cs).notNil, {
				thisString = 	synthString;
			});
		});
		if(thisString.notNil, {
			thisString = thisString.replace("; ", "").replace("}", "");
			thisKey = thisString.copyRange(thisString.find(".lag(")+5, thisString.size-2).asSymbol.cs;
			ndefCS = ("Ndef(" ++ thisBusKey.cs ++
				").set(" ++ thisKey ++ ", " ++ lag ++ ");");
			ndefCS.radcode(\radpostcont);
		}, {
			"Wrong track or bus".warn;
		});
	}

	setPanLag {arg trackType=\track, trackNum=1, lag=0, panNum=1;
		var tag, ndefCS, symString, getKeys;
		if(trackType == \master, {
			symString = trackType.asString.capitalise;
		}, {
			symString = trackType.asString.capitalise ++ trackNum;
		});
		tag = ("space" ++ symString).asSymbol;
		getKeys = Ndef(tag).getKeysValues;
		ndefCS = "Ndef(" ++ tag.cs ++ ").lag(" ++getKeys[panNum-1][0].cs ++ ", " ++ lag ++ ");";
		ndefCS.radcode;
	}

	setPan {arg trackType=\track, trackNum=1, val=0, panNum=1;
		var tag, ndefCS, trackKey, symString, getKeys;
		if(trackType == \master, {
			symString = trackType.asString.capitalise;
		}, {
			symString = trackType.asString.capitalise ++ trackNum;
		});
		tag = ("space" ++ symString).asSymbol;
		getKeys = Ndef(tag).getKeysValues;
		ndefCS = "Ndef(" ++ tag.cs ++ ").set(" ++getKeys[panNum-1][0].cs ++ ", " ++ val ++ ");";
		ndefCS.radcode;
		this.mixWinBool({
			if(trackType == \master, {
				trackKey = trackType.asSymbol;
			}, {
				trackKey = (trackType ++ trackNum).asSymbol;
			});
			setPanKnob.(mixTrackNames.indexOfEqual(trackKey), val, panNum-1);
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
		ndefCS.radcode;
		this.mixWinBool({
			if(trackType == \master, {
				trackKey = trackType.asSymbol;
			}, {
				trackKey = (trackType ++ trackNum).asSymbol;
			});
			setInKnob.(mixTrackNames.indexOfEqual(trackKey), val);
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
		ndefCS.radcode;
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
					++ value ++ ");").radcode;
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

	masterSoloFunc {arg buttstates;
		var inArr, newCS, startCS, endCS, finalCS, cs;
		cs = Ndef(\inMaster).source.cs;
		startCS = cs.find("([");
		endCS = cs.find("].sum");
		inArr = inputs.flop[1][inputs.flop[0].indexOf(\inMaster);];
		newCS = inArr.collect {|item, index|  "Ndef.ar(" ++ item.key.cs ++
			", " ++ item.numChannels ++ ") * "	++ buttstates[index] };
		newCS = newCS.cs.replace("\"", "");
		finalCS = cs.replace(cs.copyRange(startCS+1,endCS), newCS);
		("Ndef('inMaster').source = " ++ finalCS).radcode;
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
						"Ndef('master').set('off', 0)".radcode;
					}.fork;
				}, {
					Ndef('master').set('off', 0);
				});
			}, {
				"Ndef('master').set('off', 1)".radcode;
			});
		}, {
			this.masterSoloFunc((1!newStates.size););
			if(Ndef('master').getKeysValues.collect{|item| item == [\off, 1] }.includes(true), {
				"Ndef('master').set('off', 0)".radcode;
			});
		});
	}

	restoreSoloStates {arg states;
		//solo states saved in presets (absent in presets saved before Oct 2026)
		if(states.notNil and: { states.size == mixTrackNames.size }, {
			soloStates = states.copy;
			this.masterSoloFunc2;
		});
	}

	setOutputSettings {arg arr;
		var refreshNdefs;
		arr.do{|item, index|
			var trackArr;
			if(index == (arr.size-1), {refreshNdefs = true }, {refreshNdefs = false });
			trackArr = mixTrackNames[index].asString.divNumStr;
			if(item == 'master', {
				this.setTrackOut(trackArr[0].asSymbol, trackArr[1], 1, refreshNdefs);
			}, {
				this.setTrackOut(trackArr[0].asSymbol, trackArr[1], 0, refreshNdefs);
			});
		};
	}
}
