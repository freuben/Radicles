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
			//vol typed during a whole-track fade-out stops that load
			RadUnit.volumeSetDuringLoad(trackType, trackNum);
			//over lag seconds when one is given (for this move only), else
			//over the strip's own time (vollag)
			(mixTrackNdefs[trackIndex].cs ++ ".set(" ++ this.volumeArgs(mixTrackNdefs[trackIndex],
				value, lag ?? { this.stripControl(mixTrackNdefs[trackIndex], \lagTime) }) ++ ");").radcode(\radpostcont);
			this.mixWinBool({
				setVolSlider.(trackIndex, value); //slider always shows dB
			});
		}, {
			"Track not found".warn;
		});
	}

	// the time a strip's own value moves over when none is given, and the
	// time of the short smooth move a new strip starts with
	*shortLag { ^0.05 }

	// a control of a strip's Ndef as it stands (its default when never set)
	// (the two times, lagTime and ramp, both start at the short default)
	stripControl {arg ndef, key;
		^ndef.nodeMap.at(key) ? Assemblage.shortLag
	}

	// the arguments that move a strip's volume to value over time seconds:
	// the time is sent only when the strip's last move had another, so the
	// usual line stays set('volume', x). Typed values pass the strip's own
	// time or the one typed; the window's fader and mapped controls pass
	// Radicles.ctlLag. Not with a value the strip already has: a new time
	// without a new value makes the ramp overshoot (see addTrack).
	volumeArgs {arg ndef, value, time;
		^this.rampArgs(ndef, \volume, value, time)
	}

	// the same for any control that moves by a ramp: a strip's trim (rampKey
	// 'ramp' on its input) and a send's level (volN with rampN on the bus's
	// input)
	rampArgs {arg ndef, key, value, time, rampKey=\ramp;
		^key.cs ++ ", " ++ value ++ if(this.stripControl(ndef, rampKey) != time and: { ndef.nodeMap.at(key) != value },
			{ ", " ++ rampKey.cs ++ ", " ++ time }, { "" })
	}

	// the control that holds the ramp's time for a mixer control, or nil when
	// the control does not move by a ramp: 'ramp' for a strip's volume and
	// trim, rampN for a send's volN
	*rampKey {arg ndef, key;
		var name = key.asString, rampKey;
		rampKey = case
		{ key == \volume or: { key == \trim } } { \ramp }
		{ name.size > 3 and: { name.beginsWith("vol") } and: { name.drop(3).every(_.isDecDigit) } } {
			("ramp" ++ name.drop(3)).asSymbol }
		//a panner's control x: xRamp
		{ ndef.key.asString.beginsWith("space") } { (name ++ "Ramp").asSymbol }
		//an effect's or a block's parameter that can travel
		{ Assemblage.lagParams(ndef).includes(key) } { (name ++ "Ramp").asSymbol };
		^if(rampKey.notNil and: { ndef.controlKeys.includes(rampKey) }, { rampKey }, { nil })
	}

	// the control that holds the time for typed values of a mixer control
	// (vollag, trimlag, sndlag, panlag), or nil
	*lagKey {arg ndef, key;
		var name = key.asString, lagKey;
		lagKey = case
		{ key == \volume or: { key == \trim } } { \lagTime }
		{ name.size > 3 and: { name.beginsWith("vol") } and: { name.drop(3).every(_.isDecDigit) } } {
			("lag" ++ name.drop(3)).asSymbol }
		{ ndef.key.asString.beginsWith("space") } { (name ++ "Lag").asSymbol };
		^if(lagKey.notNil and: { ndef.controlKeys.includes(lagKey) }, { lagKey }, { nil })
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

	// mixlag: the time for typed values of every control of every strip, and
	// of every send
	setMixLag {arg lag=0;
		mixTrackNames.do{|name|
			var arr = name.asString.divNumStr, type, num;
			type = arr[0].asSymbol;
			num = arr[1] ? 1;
			this.setVolumeLag(type, num, lag);
			this.setTrimLag(type, num, lag);
			this.setPanLag(type, num, lag);
		};
		(busArr ? []).do{|item|
			var lags;
			if(item.notNil and: { item[0].notNil }, {
				lags = (item[1] ? []).asArray.collect{|member, index|
					("lag" ++ (index + 1)).asSymbol.cs ++ ", " ++ lag };
				if(lags.notEmpty, {
					("Ndef(" ++ item[0].cs ++ ").set(" ++ lags.join(", ") ++ ");").radcode(\radpostcont);
				});
			});
		};
	}

	sendsFunc {|trackLb, thisKey|
		var thisSpec, selArg, lagArg, thisNdefVal, thisFunc, thisSpecVal, getBusInNum;
		if(thisKey.notNil, {
			thisNdefVal = Ndef(thisKey).getKeysValues;
			getBusInNum = (busArr.flop[1][busArr.flop[0].indexOf(thisKey)].collect{|keyVal|
				keyVal.key}.indexOf(trackLb) + 1);
			selArg = ("vol" ++ getBusInNum).asSymbol;
			lagArg = ("lag" ++ getBusInNum).asSymbol;
			//over time seconds when one is given (for this move only), else
			//over the send's own time (sndlag)
			thisFunc = {|vol, time|
				("Ndef(" ++ thisKey.cs ++ ").set(" ++ this.rampArgs(Ndef(thisKey), selArg, vol,
					time ?? { this.stripControl(Ndef(thisKey), lagArg) }, ("ramp" ++ getBusInNum).asSymbol)
					++ ");").radcode(\radpostcont);
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
									this.setSendKnob(trackType, trackNum, slotNum, val, busNum: busNum);
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

	//busNum: the bus the slot sends to, when the caller knows it; otherwise it
	//is looked up from the slot (it used to take the slot number for the bus
	//number, so snd t 3 1 3 -12 with the mixer open left the level at -inf)
	setSendKnob {arg trackType=\track, trackNum=1, slotNum=1, val=0, lagTime, db=true, busNum;
		var trackIndex, value, thisResult, thisKey, thisBusKey;
		busNum = busNum ?? { this.sendSlotBus(trackType, trackNum, slotNum) } ? slotNum;
		if(db.not, {
			value = val.ampdb;
		}, {
			value = val;
		});
		thisKey = (trackType.asString ++ trackNum).asSymbol;
		trackIndex = mixTrackNames.indexOf(thisKey);
		if(trackIndex.notNil, {
			thisBusKey = (\busIn ++ busNum).asSymbol;
			if(busArr.flop[0].select({|item| item.notNil}).includes(thisBusKey), {
				if(busArr.flop[1][busArr.flop[0].indexOf(thisBusKey)].includes(
					Ndef((trackType ++ trackNum).asSymbol);
				), {
					thisResult = this.sendsFunc((trackType.asString ++ trackNum).asSymbol,
						("busIn" ++ busNum).asSymbol);
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

	// a send's own time for typed values (sndlag): lagN on the bus's input
	setSendVolLag {arg trackType=\track, trackNum=1, busNum=1, lag=0;
		var thisBusKey = ("busIn" ++ busNum).asSymbol, index;
		index = (busArr ? []).detect{|item| item[0] == thisBusKey } !? {|item| item[1] ? [] }
		!? {|members| members.indexOf(Ndef((trackType.asString ++ trackNum).asSymbol)) };
		if(index.notNil, {
			("Ndef(" ++ thisBusKey.cs ++ ").set(" ++ ("lag" ++ (index + 1)).asSymbol.cs ++ ", " ++ lag ++ ");")
			.radcode(\radpostcont);
		}, {
			"Wrong track or bus".warn;
		});
	}

	// the same by send slot, as sndset names a send
	setSendLag {arg trackType=\track, trackNum=1, slotNum=1, lag=0;
		var busNum = this.sendSlotBus(trackType, trackNum, slotNum);
		if(busNum.notNil, {
			this.setSendVolLag(trackType, trackNum, busNum, lag);
		}, {
			"No send in this slot".warn;
		});
	}

	spaceTag {arg trackType=\track, trackNum=1;
		^("space" ++ if(trackType.asSymbol == \master, { "Master" },
			{ trackType.asString.capitalise ++ trackNum })).asSymbol
	}

	// the panner's own controls, in their places (not its time controls)
	panKeys {arg trackType=\track, trackNum=1;
		var keys = Ndef(this.spaceTag(trackType, trackNum)).getKeysValues.collect(_[0]);
		^keys.reject{|key| Assemblage.timeControl(keys, key) }
	}

	// panlag: the time for typed values of every control of the panner, or
	// of the one numbered panNum
	setPanLag {arg trackType=\track, trackNum=1, lag=0, panNum;
		var tag = this.spaceTag(trackType, trackNum), keys, lagKeys;
		keys = this.panKeys(trackType, trackNum);
		if(panNum.notNil, { keys = [keys[panNum-1]].reject(_.isNil) });
		lagKeys = keys.collect{|key| Assemblage.lagKey(Ndef(tag), key) }.reject(_.isNil);
		if(lagKeys.isEmpty, {
			//a panner with no timed control (split) is passed over, unless
			//one control was asked for
			if(panNum.notNil, { "No pan control number % to give a time to".format(panNum).warn });
		}, {
			("Ndef(" ++ tag.cs ++ ").set(" ++ lagKeys.collect{|key| key.cs ++ ", " ++ lag }.join(", ") ++ ");").radcode;
		});
	}

	// a typed pan value: a steady move over lag seconds when one is given
	// (for this move only), else over the control's own time (panlag)
	setPan {arg trackType=\track, trackNum=1, val=0, panNum=1, lag;
		var tag = this.spaceTag(trackType, trackNum), ndefCS, trackKey, key, rampKey, spec;
		key = this.panKeys(trackType, trackNum)[panNum-1];
		if(key.isNil, { ^"No pan control number %".format(panNum).warn });
		//a value outside the control's range is not set (pan t 1 2 0.5 used
		//to mean control number 2: it now means the value 2 over 0.5 s)
		spec = this.getSpec(tag, key) !? {|it| it.asSpec };
		if(spec.notNil and: { val.isNumber } and: {
			val < min(spec.clipLo, spec.clipHi) or: { val > max(spec.clipLo, spec.clipHi) } }, {
			^"% is outside %'s range (% to %)".format(val, key, spec.clipLo, spec.clipHi).warn });
		rampKey = Assemblage.rampKey(Ndef(tag), key);
		ndefCS = "Ndef(" ++ tag.cs ++ ").set(" ++ if(rampKey.notNil, {
			this.rampArgs(Ndef(tag), key, val,
				lag ?? { this.stripControl(Ndef(tag), Assemblage.lagKey(Ndef(tag), key)) }, rampKey)
		}, { key.cs ++ ", " ++ val }) ++ ");";
		ndefCS.radcode;
		this.mixWinBool({
			if(trackType.asSymbol == \master, {
				trackKey = \master;
			}, {
				trackKey = (trackType ++ trackNum).asSymbol;
			});
			setPanKnob.(mixTrackNames.indexOfEqual(trackKey), val, panNum-1);
		});
	}

	// a pan control by its name (panx, width, elevation, ...)
	setPanKey {arg trackType=\track, trackNum=1, name, val=0, lag;
		var keys = this.panKeys(trackType, trackNum), index;
		index = keys.indexOf(name);
		if(index.isNil, {
			("% has no pan control called %".format(
				if(trackType.asSymbol == \master, { "master" }, { trackType.asString ++ trackNum }),
				name)).warn;
		}, {
			this.setPan(trackType, trackNum, val, index + 1, lag);
		});
	}

	setTrim {arg trackType=\track, trackNum=1, val=0, lag;
		var tag, ndefCS, trackKey, symString;
		if(trackType == \master, {
			symString = trackType.asString.capitalise;
		}, {
			symString = trackType.asString.capitalise ++ trackNum;
		});
		tag = ("in" ++ symString).asSymbol;
		//over lag seconds when one is given (for this move only), else over
		//the strip's own time (trimlag)
		ndefCS = "Ndef(" ++ tag.cs ++ ").set(" ++ this.rampArgs(Ndef(tag), \trim, val,
			lag ?? { this.stripControl(Ndef(tag), \lagTime) }) ++ ");";
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
		ndefCS = "Ndef(" ++ tag.cs ++ ").set('lagTime', " ++ val ++ ");";
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
