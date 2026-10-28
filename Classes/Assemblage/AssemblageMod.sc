// Assemblage: modulation of mixer, effect and send arguments, and removing it.
// Part of the Assemblage class: the class and its variables are in Assemblage.sc.

+ Assemblage {
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
		specArr = this.specsOf(filterTag);
		if(argument.isNumber, {
			argIndex = this.getFilterKeys(filterTag, \args)[argument];
		}, {
			argIndex = argument;
		});
		thisSpec = 	specArr[specArr.flop[0].indexOf(argIndex)];
		this.modFilterTag(filterTag, thisSpec[0], modType, thisSpec[1], extraArgs,
			thisSpec[2], mul, add, min, val, warp, lag);
	}

	modFilterCode {arg type, num, slot, argument, modType=\sin, extraArgs,
		mul=1, add=0, min, val, warp, lag;
		var filterTag;
		filterTag = this.findFilterTag(type, num, slot);
		this.modFilter(filterTag, argument, modType, extraArgs,
			mul, add, min, val, warp, lag);
	}

	modFunc {arg ndefKey, argIn, type, extraArgs, func,
		mul=1, add=0, min, val, warp, lag, thisSpec, modifier=\mod;
		var index, keyValues, spec, newArr, specInd;
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
						func ?? {if(spec[2].notNil, {func = spec[2] }) };
						spec = spec[1];
						/*spec.postln;*/
						if(spec.includes(\db), {
							spec = spec.copyFromStart(1);
							spec = spec.collect({|item| if(item == -inf, {item = -90}, {item = item});});
							if(modifier == \hid, {
								spec = spec ++ [\db];
							});
						});
					}, {
						spec = [-1,1];
					});
				});
			}, {
				spec = [-90,6];
				if(modifier == \hid, {
					spec = spec ++ [\db];
				});
			});
			if(thisSpec.notNil, {spec = thisSpec});
			if(spec.isNil, {spec = [-1,1] });
			{nodeTime.yield;
				this.refreshFunc;
			}.fork(AppClock);
			if(modifier == \mod, {
				^ModMap.map(Ndef(ndefKey), keyValues[0], type, spec, extraArgs,
					func, mul, add, min, val, warp, lag);
			}, {
				^HIDMap.map(Ndef(ndefKey), keyValues[0], type, spec, extraArgs,
					func, mul, add, min, val, warp, lag);
			});

		}, {
			"Argument doesn't match synth".warn;
		});
	}

	modMix {arg trackType, trackNum, modArg, modType, extraArgs,
		mul=1, add=0, min, val, warp, lag, thisSpec, func, modifier=\mod;
		var typeKey, ndefKey;
		if(trackType == \master, {
			trackNum = "";
		});
		typeKey = trackType.asString;
		case
		{(modArg == \vol).or(modArg == \volume)} {
			ndefKey = (typeKey ++ trackNum); modArg = \volume;
			if(modifier == \hid, {
				func = {|val| val.round(0.1)}; lag = 0.01;
			});
		}
		{modArg == \trim} {ndefKey = (\in ++ typeKey.capitalise ++ trackNum);};

		if(ndefKey.isNil, {ndefKey = (\space ++ typeKey.capitalise ++ trackNum);});

		ndefKey = ndefKey.asSymbol;
		{
			this.modFunc(ndefKey, modArg, modType, extraArgs, func,
				mul, add, min, val, warp, lag, thisSpec, modifier);
			server.sync;
			this.refreshFunc;
		}.fork(AppClock);
	}

	modFx {arg filterNum, modArg, modType, extraArgs,
		mul=1, add=0, min, val, warp, lag, thisSpec, func, modifier=\mod;
		var typeKey, ndefKey;
		this.fxWarn(filterNum, {|ndefKey|
			if(modArg.notNil, {
				{
					this.modFunc(ndefKey, modArg, modType, extraArgs, func,
						mul, add, min, val, warp, lag, thisSpec, modifier);
					server.sync;
					this.updateFxWin(ndefKey);
				}.fork(AppClock);
			}, {
				Ndef(ndefKey).controlKeys.postln;
			});
		});
	}

	modFxTrack {arg trackType, trackNum, trackSlot, modArg, modType, extraArgs,
		mul=1, add=0, min, val, warp, lag, thisSpec, func, modifier=\mod;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			if(modArg.notNil, {
				{
					this.modFunc(ndefKey, modArg, modType, extraArgs, func,
						mul, add, min, val, warp, lag, thisSpec, modifier);
					server.sync;
					this.updateFxWin(ndefKey);
				}.fork(AppClock);
			}, {
				Ndef(ndefKey).controlKeys.postln;
			});
		});
	}

	sendSlotBus {arg trackType, trackNum, sendSlot;
		//the bus number used by a track's send slot (slots follow bus order, as in the mixer)
		var busIn;
		if(busArr.isNil, {^nil});
		busIn = this.prepareModSend(trackType, trackNum, sendSlot)[0];
		^if(busIn.notNil, {busIn.asString.divNumStr[1]}, {nil});
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

	modSend {arg trackType, trackNum, sendSlot, modType, extraArgs,
		mul=1, add=0, min, val, warp, lag, thisSpec, func, modifier=\mod;
		var trackKey, indArr, busInArr, slotArr, volArg, thisBusIn, modNdef;
		busInArr = this.prepareModSend(trackType, trackNum, sendSlot);
		if(busInArr.includes(nil).not, {
			{
				thisBusIn = busInArr[0];
				volArg = busInArr[1];
				modNdef = this.modFunc(thisBusIn, volArg, modType, extraArgs, func,
					mul, add, min, val, warp, lag, thisSpec, modifier);
				modSendArr = modSendArr.add([trackType, trackNum, sendSlot, modNdef]);
				server.sync;
				this.refreshFunc;
			}.fork(AppClock);
		}, {
			"Track and slot numers don't match active send".warn;
		});
	}

	//argument number (from 1) for a number or a name such as \pitchRatio
	modArgNum {arg ndefKey, modArg;
		var index;
		if(modArg.isNil or: {modArg.isInteger}, {^modArg});
		index = Ndef(ndefKey).controlKeys.indexOf(modArg.asSymbol);
		if(index.isNil, {
			("argument not found: " ++ modArg).warn;
			^nil;
		});
		^(index + 1);
	}

	unmodFunc {arg ndefKey, modArg;
		var modifier;
		if(modArg.isInteger, {
			modArg = Ndef(ndefKey).controlKeys[modArg-1];
		});
		//a match needs the same entry to hold both this Ndef and this argument
		if(ModMap.modNodes.notNil and: {
			ModMap.modNodes.any({|item| (item[1] == Ndef(ndefKey)) and: {item[2] == modArg} })
		}, {
			modifier = \mod;
		});
		if(HIDMap.hidNodes.notNil and: {
			HIDMap.hidNodes.any({|item| (item[1] == Ndef(ndefKey)) and: {item[2] == modArg} })
		}, {
			modifier = \hid;
		});
		^modifier;
	}

	unmodMix {arg trackType, trackNum, modArg, value;
		var typeKey, ndefKey, modifier;
		if(modArg.notNil, {
			{
				{
					if((ModMap.modNodes.notNil).or(HIDMap.hidNodes.notNil), {
						typeKey = trackType.asString;
						case
						{(modArg == \vol).or(modArg == \volume)} {
							ndefKey = (typeKey ++ trackNum); modArg = \volume }
						/*{modArg == \pan} {ndefKey = (\space ++ typeKey.capitalise ++ trackNum);}*/
						{modArg == \trim} {ndefKey = (\in ++ typeKey.capitalise ++ trackNum);};

						if(ndefKey.isNil, {ndefKey = (\space ++ typeKey.capitalise ++ trackNum);});

						ndefKey = ndefKey.asSymbol;
						modifier = this.unmodFunc(ndefKey, modArg);
						case
						{modifier == \mod} {
							value ?? {
								value = ModMap.modNodes.select({|item|
									(item[1] == Ndef(ndefKey) ).and(item[2] == modArg);
								})[0].last;
							};
							ModMap.unmap(Ndef(ndefKey), modArg, value);
						}
						{modifier == \hid} {
							value ?? {
								value = HIDMap.hidNodes.select({|item|
									(item[1] == Ndef(ndefKey) ).and(item[2] == modArg);
								})[0].last;
							};
							HIDMap.unmap(Ndef(ndefKey), modArg, value);
						};
						server.sync;
						this.refreshFunc;
					}, {
						"no mod in this argument".warn;
					});
				}.fork(AppClock);
				server.sync;
				this.refreshFunc;
			}.fork(AppClock);
		}, {
			"no mod in this argument".warn;
		});
	}

	unmodFx {arg filterNum, modArg, value;
		var modifier;
		this.fxWarn(filterNum, {|ndefKey|
			modArg = this.modArgNum(ndefKey, modArg);
			if(modArg.notNil, {
				{
					if((ModMap.modNodes.notNil).or(HIDMap.hidNodes.notNil), {
						modifier = this.unmodFunc(ndefKey, modArg);
						case
						{modifier == \mod} {
							value ?? {
								value = ModMap.modNodes.select({|item|
									(item[1] == Ndef(ndefKey) ).and(item[2] == Ndef(ndefKey).controlKeys[modArg-1]);
								})[0].last;
							};
							ModMap.unmap(Ndef(ndefKey), modArg-1, value);
						}
						{modifier == \hid} {
							value ?? {
								value = HIDMap.hidNodes.select({|item|
									(item[1] == Ndef(ndefKey) ).and(item[2] == Ndef(ndefKey).controlKeys[modArg-1]);
								})[0].last;
							};
							HIDMap.unmap(Ndef(ndefKey), modArg-1, value);
						};
						server.sync;
						this.updateFxWin(ndefKey);
					}, {
						"no mod in this argument".warn;
					});
				}.fork(AppClock);
			});
		});
	}

	unmodFxTrack {arg trackType, trackNum, trackSlot, modArg, value, post=true;
		var modifier;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			modArg = this.modArgNum(ndefKey, modArg);
			if(modArg.notNil, {
				{
					if((ModMap.modNodes.notNil).or(HIDMap.hidNodes.notNil), {
						modifier = this.unmodFunc(ndefKey, modArg);
						case
						{modifier == \mod} {
							value ?? {
								value = ModMap.modNodes.select({|item|
									(item[1] == Ndef(ndefKey) ).and(item[2] ==
										Ndef(ndefKey).controlKeys[modArg-1]);
								})[0].last;
							};
							ModMap.unmap(Ndef(ndefKey), modArg-1, value);
						}
						{modifier == \hid} {
							value ?? {
								value = HIDMap.hidNodes.select({|item|
									(item[1] == Ndef(ndefKey) ).and(item[2] ==
										Ndef(ndefKey).controlKeys[modArg-1]);
								})[0].last;
							};
							HIDMap.unmap(Ndef(ndefKey), modArg-1, value);
						};
						server.sync;
						this.updateFxWin(ndefKey);
					}, {
						"no mod in this argument".warn;
					});
				}.fork(AppClock);
			});
		}, post: post);
	}

	findFxModNdef {arg trackType, trackNum, trackSlot, trackArg;
		var fxTag, findMods, findModArr, modIndex, modKey, modInfo, result;
		fxTag = this.findFilterTag(trackType, trackNum, trackSlot);
		if(fxTag.notNil, {
			modInfo = ModMap.modNodes;
			if(modInfo.notNil, {
				findMods = ModMap.modNodes.flop[1].indicesOfEqual(Ndef(fxTag));
				if(findMods.notNil, {
					findModArr = ModMap.modNodes.atAll(findMods);
					if(trackArg.isSymbol, {
						modIndex = findModArr.flop[2].indexOf(trackArg);
					}, {
						modKey = 	findModArr.flop[1][0].controlKeys[trackArg-1];
						modIndex = findModArr.flop[2].indexOf(modKey);
					});
					if(modIndex.notNil, {
						result = findModArr.flop[0][modIndex];
					}, {
						"key not found".warn;
					});
				}, {
					"no modulation in this filter".warn;
				});
			}, {
				"no modulation in this filter".warn;
			});
		}, {
			"filter doesn't exist".warn;
		});
		^result;
	}

	setFxMod {arg trackType, trackNum, trackSlot, trackArg, extraArgs;
		var ndefString;
		ndefString = this.findFxModNdef(trackType, trackNum, trackSlot, trackArg);
		if(ndefString.notNil, {
			(ndefString.cs ++ ".setn" ++ extraArgs.cs.replaceAt("(",0)
				.replaceAt(")", extraArgs.cs.size-1)).radcode;
		});
	}

	getFxMod {arg trackType, trackNum, trackSlot, trackArg;
		var ndefString;
		ndefString = this.findFxModNdef(trackType, trackNum, trackSlot, trackArg);
		if(ndefString.notNil, {
			^ndefString.controlKeysValues;
		});
	}

	findMixModNdef {arg trackType, trackNum, modArg;
		var mixTag, findMods, findModArr, modIndex, modKey, modInfo, result, typeKey, ndefKey;
		typeKey = trackType.asString;
		case
		{(modArg == \vol).or(modArg == \volume)} {
			ndefKey = (typeKey ++ trackNum); modArg = \volume }
		/*{modArg == \pan} {ndefKey = (\space ++ typeKey.capitalise ++ trackNum);}*/
		{modArg == \trim} {ndefKey = (\in ++ typeKey.capitalise ++ trackNum);};

		if(ndefKey.isNil, {ndefKey = (\space ++ typeKey.capitalise ++ trackNum);});

		if(ndefKey.notNil, {
			ndefKey = ndefKey.asSymbol;
			modInfo = ModMap.modNodes;
			if(modInfo.notNil, {
				findMods = ModMap.modNodes.flop[1].indicesOfEqual(Ndef(ndefKey));
				if(findMods.notNil, {
					findModArr = ModMap.modNodes.atAll(findMods);
					if(modArg.isSymbol, {
						modIndex = findModArr.flop[2].indexOf(modArg);
					}, {
						modKey = 	findModArr.flop[1][0].controlKeys[modArg-1];
						modIndex = findModArr.flop[2].indexOf(modKey);
					});
					if(modIndex.notNil, {
						result = findModArr.flop[0][modIndex];
					}, {
						"key not found".warn;
					});
				}, {
					"no modulation in this track".warn;
				});
			}, {
				"no modulation in this track".warn;
			});
		}, {
			"track doesn't exist".warn;
		});
		^result;
	}

	setMixMod {arg trackType, trackNum, trackArg, extraArgs;
		var ndefString;
		ndefString = this.findMixModNdef(trackType, trackNum, trackArg);
		if(ndefString.notNil, {
			(ndefString.cs ++ ".setn" ++ extraArgs.cs.replaceAt("(",0)
				.replaceAt(")", extraArgs.cs.size-1)).radcode;
		});
	}

	getMixMod {arg trackType, trackNum, trackArg;
		var ndefString;
		ndefString = this.findMixModNdef(trackType, trackNum, trackArg);
		if(ndefString.notNil, {
			^ndefString.controlKeysValues;
		});
	}

	findSndModNdef {arg trackType, trackNum, trackSlot;
		var arr, arr1, arr2, result;
		arr = modSendArr;
		arr1 = arr.collect({|item| (item[0] ++ "_" ++ item[1] ++ "_" ++ item[2]).asSymbol });
		arr2 = arr.flop[3];
		result = arr2[arr1.indexOfEqual((trackType ++ "_" ++ trackNum ++ "_"
			++ trackSlot).asSymbol)];
		^result;
	}

	setSndMod {arg trackType, trackNum, trackSlot, extraArgs;
		var ndefString;
		ndefString = this.findSndModNdef(trackType, trackNum, trackSlot);
		if(ndefString.notNil, {
			(ndefString.cs ++ ".setn" ++ extraArgs.cs.replaceAt("(",0)
				.replaceAt(")", extraArgs.cs.size-1)).radcode;
		});
	}

	getSndMod {arg trackType, trackNum, trackSlot;
		var ndefString;
		ndefString = this.findSndModNdef(trackType, trackNum, trackSlot);
		if(ndefString.notNil, {
			^ndefString.controlKeysValues;
		});
	}

	unmodSend {arg trackType, trackNum, sendSlot, value;
		var trackKey, indArr, busInArr, slotArr, volArg, thisBusIn, modifier;
		busInArr = this.prepareModSend(trackType, trackNum, sendSlot);
		if(busInArr.includes(nil).not, {
			{
				thisBusIn = busInArr[0];
				volArg = busInArr[1];
				if((ModMap.modNodes.notNil).or(HIDMap.hidNodes.notNil), {
					modifier = this.unmodFunc(thisBusIn, volArg);
					case
					{modifier == \mod} {
						value ?? {
							value = ModMap.modNodes.select({|item|
								(item[1] == Ndef(thisBusIn) ).and(item[2] == volArg);
							})[0].last;
						};
						ModMap.unmap(Ndef(thisBusIn), volArg, value);
					}
					{modifier == \hid} {
						value ?? {
							value = HIDMap.hidNodes.select({|item|
								(item[1] == Ndef(thisBusIn) ).and(item[2] == volArg);
							})[0].last;
						};
						HIDMap.unmap(Ndef(thisBusIn), volArg, value);
					};
					modSendArr.collect{|item| item.copyFromStart(2) }.collect{|it, ind|
						if(it == [trackType, trackNum, sendSlot], {modSendArr.removeAt(ind);});
					};
					server.sync;
					this.refreshFunc;
				});
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
			(item.cs ++ ".clear(" ++ fadeTime.cs ++ ");").radcode;
		};
		if(modSendArr.notNil, {
			modSendArr.flop[3].do{|item, index|
				if(sendNdefs.includes(item), {
					indArr = indArr.add(index) });
			};
			modSendArr.removeAtAll(indArr);
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
							this.unmodFxTrack(keyArr[0], keyArr[1], keyArr[2], item, nil, false);
						};
						server.sync;
						ModMap.clearLooseMods;
					});
				});
			});
		}.fork;
	}
}
