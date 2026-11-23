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
	<sourceWindows, srcWinTracks, srcButtonArr, srcMenuWindow, <bypassed, morphs;

	*new {arg trackNum=1, busNum=0, chanNum=1, spaceType, memSize, action;
		^super.new.initAssemblage(trackNum, busNum, chanNum, spaceType, memSize, action);
	}

	initAssemblage {arg trackNum=1, busNum=0, chanNum=2, spaceType, memSize, action;
		var chanMaster, chanTrack, chanBus, spaceMaster, spaceTrack, spaceBus, inArr;
		RadUnit.syncAll(this); //document (step 1): a new mixer starts with no effects
		server.waitForBoot{
			{
				//the volume moves as a steady fader move (see addTrack)
				masterSynth = {arg volume=0, lagTime=0.05, mute=0, off=0, ramp=0.05;
					(\in * (VarLag.kr((volume.dbamp / 6.dbamp).sqrt, ramp.max(ControlDur.ir), warp: \lin).squared * 6.dbamp)
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
				RadUnit.syncStrips(this); //document (step 3): the strips
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
			//the trim moves evenly in dB over 'ramp' seconds; 'lagTime' is the
			//strip's own time for typed values (trimlag). See the volume below.
			trackInSynth = {arg trim=0, lagTime=0.05, ramp=0.05;
				(\in * VarLag.kr(trim, ramp.max(ControlDur.ir), warp: \lin).dbamp )};
			trackInSpecs = [ ['trim', [-23, 23] ], ['lagTime', [0, 10] ] ];
			//the volume moves as a steady fader move (mixer ramps, Nov 2026): the
			//dB value becomes a fader position (the volume spec's scale: amplitude
			//is the position squared, the bottom is silence), the position moves
			//evenly over 'ramp' seconds, and is squared again. 'ramp' is the time
			//of the move under way, sent with the value when it differs
			//(Assemblage:volumeArgs); 'lagTime' is the strip's own time for typed
			//values (vollag). The linear VarLag, not its default form or an
			//EnvGen: those miss a value that arrives one control block after
			//another and flatten a modulator. The time is never below one
			//control block (VarLag divides by it), and it must not change on a
			//running synth without a new value: VarLag then overshoots.
			trackSynth ?? {trackSynth = {arg volume=0, lagTime=0.05, mute=0, solo=1, ramp=0.05;
				(\in * (VarLag.kr((volume.dbamp / 6.dbamp).sqrt, ramp.max(ControlDur.ir), warp: \lin).squared * 6.dbamp)
					*mute.linlin(0,1,1,0).lag(0.1)*solo.lag(0.1) )} };
			trackSpecs = [ ['volume', [-inf, 6, \db, 0, -inf, " dB" ] ], ['lagTime', [0, 10] ],
				['mute', [0, 1] ], ['solo', [0,1] ] ];
			spaceSpecs = SpecFile.read(\space, spaceType);
			spaceSynth = Assemblage.rampSpace(SynthFile.read(\space, spaceType), spaceSpecs);

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
			//made after mixlag N: the strip's times for typed values start at N
			if(Radicles.mixLag != Assemblage.shortLag, {
				("Ndef(" ++ trackInTag.cs ++ ").set('lagTime', " ++ Radicles.mixLag ++ ");").radcode;
				("Ndef(" ++ trackTag.cs ++ ").set('lagTime', " ++ Radicles.mixLag ++ ");").radcode;
				if(spaceSynth.isKindOf(Function), {
					var names = spaceSynth.def.argNames ? [], lagKeys;
					lagKeys = names.select{|name| name.asString.endsWith("Lag") and: {
						Assemblage.timeControl(names, name) } };
					if(lagKeys.notEmpty, {
						("Ndef(" ++ spaceTag.cs ++ ").set(" ++ lagKeys.collect{|key|
							key.cs ++ ", " ++ Radicles.mixLag }.join(", ") ++ ");").radcode;
					});
				});
			});
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
			//a bypassed effect is left out of the chain (AssemblageBypass)
			newArr = this.activeStages(trackInfo).reverse;
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
			//mixTrackNames as the window would make it, so lists kept by position
			//(soloStates, outputSettings) match it with no window open too
			this.updateMixInfo;
			server.sync;
			this.autoRoute(trackInfo);
			server.sync;
			//to the master by each strip's output (inputMaster would connect every
			//strip, also one set to no output, and solo then failed on the extra input)
			this.outputMasterFunc;
			server.sync;
			RadUnit.syncStrips(this); //document (step 3): the new strip
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
		var trackString, inTrack, realTrack, spaceTrack, indexTrack, indArr, thisBusNums, indArrBusIn, cond;

		//a track or bus that isn't there: say so and do nothing (it posted
		//three clear lines for Ndefs that didn't exist, then failed)
		if(trackType != \master and: {
			(mixTrackNames ? []).includes((trackType ++ trackNum).asSymbol).not }, {
			(trackType.asString.capitalise ++ " " ++ trackNum ++ " not found").warn;
			action.();
			^this
		});

		if(trackType != \master, {
			{
				trackString = (trackType ++ trackNum).asString;
				inTrack = ("in" ++ trackString.capitalise).asSymbol;
				realTrack = trackString.asSymbol;
				spaceTrack = ("space" ++ trackString.capitalise).asSymbol;

				//morphs running on the strip stop first
				RadUnit.prStopStripMorphs(trackType, trackNum, this);
				//wait until its effects are really gone (it only synced before)
				cond = Condition(false);
				this.removeTrackFilters(trackType, trackNum, false, true,
					{ cond.test = true; cond.signal }, false);
				cond.wait;
				server.sync;
				//a track's source goes with it (18 Nov 2026)
				if(trackType == \track, { this.prRemoveTrackSource(trackNum) });
				//where the strip sits now, looked up after the waits above
				indexTrack = mixTrackNames.indexOf(realTrack);

				("Ndef(" ++ inTrack.cs ++ ").clear(" ++ Ndef(inTrack).fadeTime ++ ");").radcode;
				("Ndef(" ++ realTrack.cs ++ ").clear(" ++ Ndef(realTrack).fadeTime ++ ");").radcode;
				("Ndef(" ++ spaceTrack.cs ++ ").clear(" ++ Ndef(spaceTrack).fadeTime ++ ");").radcode;

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
						//nil when nothing sends to this bus (other buses may have sends)
						if(indArrBusIn.notNil, {
							busArr.flop[1][indArrBusIn].collect{|item| item.key.asString.divNumStr}.do{|it|
								this.removeBus(it[1], trackNum, it[0].asSymbol, true);
								server.sync;
							};
						});
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
				RadUnit.syncStrips(this); //document (step 3): the strip is gone
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

	// A panner from the library with a ramp on each of its controls (mixer
	// ramps, Nov 2026): every control moves evenly over its own xRamp seconds,
	// and xLag is its time for typed values (panlag), as 'ramp' and 'lagTime'
	// are for a strip's volume. The panner's own function is kept whole and
	// called with the ramped values; its controls keep their places, the
	// times come after them. Not a control whose spec rounds its value (the
	// 'split' panner's output number).
	*rampSpace {arg func, specs;
		var names, defaults, keys, args, values;
		if(func.isKindOf(Function).not, { ^func });
		names = func.def.argNames ? [];
		defaults = func.def.prototypeFrame ? [];
		//already wrapped
		if(names.any{|name| this.timeControl(names, name) }, { ^func });
		keys = names.select{|name|
			(specs ? []).detect{|spec| spec[0] == name }.notNil and: {
				(specs ? []).detect{|spec| spec[0] == name }.any(_.isKindOf(Function)).not } };
		if(keys.isEmpty, { ^func });
		args = names.collect{|name, index| name ++ " = " ++ defaults[index].cs }
		++ keys.collect{|key| key ++ "Lag = " ++ this.shortLag ++ ", " ++ key ++ "Ramp = " ++ this.shortLag };
		values = names.collect{|name| if(keys.includes(name), {
			"VarLag.kr(" ++ name ++ ", " ++ name ++ "Ramp.max(ControlDur.ir), warp: \\lin)" }, { name.asString }) };
		^("{arg " ++ args.join(", ") ++ ";\n\t" ++ func.cs ++ ".value(" ++ values.join(", ") ++ ") }").interpret
	}

	// An effect's or a block's function with a ramp on each parameter that can
	// travel: one whose spec is a plain range (with or without a scale). Not a
	// parameter with no spec, a spec with a step or with a function, or a
	// default that is not a number. The ramps are built in and off: each
	// parameter x gets xLag (its time for typed values; 0 is off) and xRamp
	// (the time of the move under way), both 0, and at 0 the value passes
	// through as it is. The library function is kept whole and called with the
	// ramped values; its controls keep their places, the times come after
	// them. A move follows the spec's scale: even in value for a linear one,
	// even in pitch for an exponential one, the fader's law for a dB one.
	// (docs/design/lags-blocks-effects.md)
	*rampUnit {arg func, specs;
		var names, defaults, keys, args, values;
		if(func.isKindOf(Function).not, { ^func });
		names = func.def.argNames ? [];
		defaults = func.def.prototypeFrame ? [];
		//already wrapped
		if(names.any{|name| this.timeControl(names, name) }, { ^func });
		keys = names.select{|name, index|
			this.rampableSpec((specs ? []).detect{|spec| spec.isArray and: { spec[0] == name } })
			and: { defaults[index].isNumber }
			and: { names.includes((name ++ "Lag").asSymbol).not }
			and: { names.includes((name ++ "Ramp").asSymbol).not } };
		if(keys.isEmpty, { ^func });
		args = names.collect{|name, index| name ++ " = " ++ defaults[index].cs }
		++ keys.collect{|key| key ++ "Lag = 0, " ++ key ++ "Ramp = 0" };
		values = names.collect{|name| if(keys.includes(name), {
			this.rampCode(name, (specs.detect{|spec| spec.isArray and: { spec[0] == name } })[1]) }, { name.asString }) };
		^("{arg " ++ args.join(", ") ++ ";\n\t" ++ func.cs ++ ".value(" ++ values.join(",\n\t\t") ++ ") }").interpret
		?? { "the ramps could not be added to this function; it is used as it is".warn; func }
	}

	// true for a library spec ([name, [min, max, scale, step ...], function])
	// whose parameter can travel
	*rampableSpec {arg spec;
		var range;
		if(spec.isNil, { ^false });
		if(spec.any(_.isKindOf(Function)), { ^false });
		range = spec[1];
		if(range.isArray.not or: { range.size < 2 }, { ^false });
		if(range[0].isNumber.not or: { range[1].isNumber.not }, { ^false });
		if(range[3].isNumber and: { range[3] != 0 }, { ^false });
		^true
	}

	// the code of one ramped parameter, on its spec's scale
	*rampCode {arg name, range;
		var time = name ++ "Ramp.max(ControlDur.ir)", warp = range[2], lo, hi;
		warp = if(warp.isKindOf(Symbol) or: { warp.isString }, { warp.asSymbol }, { \lin });
		^case
		//even in pitch: the ramp runs on the logarithm
		{ warp == \exp and: { range[0] > 0 } and: { range[1] > 0 } } {
			"Select.kr(" ++ name ++ "Ramp > 0, [" ++ name ++ ", VarLag.kr(" ++ name
			++ ".max(1e-9).log, " ++ time ++ ", warp: \\lin).exp])" }
		//the fader's law, as the mixer's volume
		{ warp == \db and: { range[1] != inf } and: { range[0] < range[1] } } {
			lo = range[0].dbamp; hi = range[1].dbamp;
			"Select.kr(" ++ name ++ "Ramp > 0, [" ++ name ++ ", (VarLag.kr(((" ++ name ++ ".dbamp - " ++ lo ++ ") / "
			++ (hi - lo) ++ ").max(0).sqrt, " ++ time ++ ", warp: \\lin).squared * " ++ (hi - lo) ++ " + " ++ lo ++ ").ampdb])" }
		//(the same way out as the others: with no time the value is passed
		//as it is, in the same control period; a ramp arrives one late, which
		//a trigger set with it would miss)
		{ "Select.kr(" ++ name ++ "Ramp > 0, [" ++ name ++ ", VarLag.kr(" ++ name ++ ", " ++ time ++ ", warp: \\lin)])" }
	}

	// a unit's own parameters, in their places (not its time controls)
	*paramKeys {arg ndef;
		var keys = ndef.controlKeys;
		^keys.reject{|key| this.timeControl(keys, key) }
	}

	// the parameters of a unit that can travel (they have a time control)
	*lagParams {arg ndef;
		var keys;
		//(a panner's time controls are the mixer's: panlag)
		if(ndef.key.asString.beginsWith("space"), { ^[] });
		keys = ndef.controlKeys;
		//(only what the unit's function has: the node map can still hold the
		//controls of an earlier unit in this place)
		if(ndef.source.isKindOf(Function), {
			keys = keys.select{|key| (ndef.source.def.argNames ? []).includes(key) } });
		^keys.select{|key| keys.includes((key ++ "Lag").asSymbol) and: { keys.includes((key ++ "Ramp").asSymbol) } }
	}

	// a unit parameter's time for typed values (0: off), nil when it cannot travel
	*unitLag {arg ndef, key;
		if(this.lagParams(ndef).includes(key).not, { ^nil });
		^ndef.nodeMap.at((key ++ "Lag").asSymbol) ? 0
	}

	// the arguments that move a unit's parameter to value over time seconds;
	// the time is sent only when the last move had another and the value is
	// new (a new time on a running ramp makes it overshoot)
	*unitRampArgs {arg ndef, key, value, time;
		var rampKey = (key ++ "Ramp").asSymbol;
		if(this.lagParams(ndef).includes(key).not, { ^key.cs ++ ", " ++ value.cs });
		time = time ? this.unitLag(ndef, key);
		^key.cs ++ ", " ++ value.cs ++ if((ndef.nodeMap.at(rampKey) ? 0) != time and: { ndef.nodeMap.at(key) != value },
			{ ", " ++ rampKey.cs ++ ", " ++ time }, { "" })
	}

	// the time the window's sliders and mapped controls move a unit's
	// parameter over: the native time when the parameter is on, none when off
	*unitNativeTime {arg ndef, key;
		^if((this.unitLag(ndef, key) ? 0) > 0, { Radicles.ctlLag }, { 0 })
	}

	// true for a panner's time controls (xLag, xRamp, where x is another of
	// its controls): they are not values of the panner
	*timeControl {arg keys, key;
		var name = key.asString;
		^["Lag", "Ramp"].any{|end| name.endsWith(end) and: {
			keys.includes(name.drop(end.size.neg).asSymbol) } }
	}

	ndefPrepare {arg ndef, func;
		var extraArgs, key, result;
		extraArgs = ndef.controlKeysValues;
		//(not a unit's time controls that are off: nothing to put back)
		extraArgs = extraArgs.clump(2).reject{|pair| pair[1] == 0 and: {
			Assemblage.timeControl(ndef.controlKeys, pair[0]) } }.flatten(1);
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
			spaceSynth = Assemblage.rampSpace(SynthFile.read(\space, spaceType), SpecFile.read(\space, spaceType));
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

	// A track that is removed takes its source with it: its input is
	// forgotten, and a block that fed it is removed too, unless that block
	// also feeds another track (Federico, 18 Nov 2026). An input that is not a
	// block (an Ndef of your own) is only unplugged.
	prRemoveTrackSource {arg trackNum;
		var tag = ("inTrack" ++ trackNum).asSymbol, entry, sources;
		entry = (inputs ? []).detect{|item| item[0] == tag };
		if(entry.notNil, {
			sources = entry[1].asArray;
			inputs.remove(entry);
			sources.do{|ndef|
				var number;
				if(ndef.isKindOf(NodeProxy) and: { RadUnit.isBlockTag(ndef.key) }, {
					number = ndef.key.asString.divNumStr[1].asInteger;
					if(RadUnit.prBlockShared(number, trackNum, this).not, { Block.remove(number) });
				});
			};
		});
	}

	// Unplug an Ndef (a block that is being removed) from every track it
	// feeds: a track fed only by it is left with no input, and its input menu
	// and source slot show empty; a track fed by several keeps the others.
	removeInput {arg ndef;
		(inputs ? []).copy.do{|item|
			var tag = item[0], fed = item[1], num, rest;
			if(tag.asString.beginsWith("inTrack") and: { fed.asArray.includes(ndef) }, {
				num = tag.asString.divNumStr[1].asInteger;
				rest = fed.asArray.reject{|other| other == ndef };
				if(rest.isEmpty, {
					inputs.remove(item);
					("Ndef(" ++ tag.cs ++ ").source = nil;").radcode;
					this.prSourceChanged(num, nil);
				}, {
					this.input(if(rest.size == 1, { rest[0] }, { rest }), \track, num);
				});
			});
		};
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
		busAdd, argIndex, thisBusLabel, added = false;
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
			busAdd = {added = true; busArr[busNum-1][1] = busArr[busNum-1][1].add(
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
			//a send made after mixlag N: its time for typed values starts at N
			//(not when a preset or session makes it: its saved time follows)
			if(added and: { setVal } and: { Radicles.mixLag != Assemblage.shortLag }, {
				argIndex = busArr[busNum-1][1].indexOf(Ndef((trackType.asString ++ trackNum).asSymbol));
				if(argIndex.notNil, {
					("Ndef(" ++ busTag.cs ++ ").set(" ++ ("lag" ++ (argIndex+1)).asSymbol.cs ++ ", "
						++ Radicles.mixLag ++ ");").radcode;
				});
			});
			if(setVal, {
				/*"setVal ".post; setVal.postln;*/
				argIndex = busArr[busNum-1][1].indexOf(Ndef((trackType.asString ++ trackNum).asSymbol));
				//a typed move: over the send's own time (sndlag), not the last move's
				ndefCS3 = "Ndef(" ++ busTag.cs ++ ").set(" ++ this.rampArgs(Ndef(busTag),
					("vol" ++ (argIndex+1)).asSymbol, mix,
					this.stripControl(Ndef(busTag), ("lag" ++ (argIndex+1)).asSymbol),
					("ramp" ++ (argIndex+1)).asSymbol) ++ ");";
				ndefCS3.radcode;
			});
			action.();
		}.fork;
	}

	removeBus {arg trackNum= 1, busNum=1, trackType=\track, clearTrack=false;
		var oldLabel, newBusInd, newLabelArr, spaceBusNum, spaceBusLabel,
		spaceInNdef, spaceInSel, spaceInInd, thisBusIndLabel, trackKey, oldLabelArr;
		oldLabel = ("busIn" ++ busNum).asSymbol;
		spaceBusLabel = ("inBus" ++ busNum).asSymbol;
		spaceInInd = inputs.flop[0].indexOf(spaceBusLabel);
		trackKey = (trackType.asString ++ trackNum).asSymbol;

		if(spaceInInd.notNil, {
			newBusInd = busArr.flop[0].indexOf(oldLabel);
			newLabelArr = busArr.flop[1][newBusInd];
			oldLabelArr = newLabelArr;
			newLabelArr = newLabelArr.select({|item|
				item.key != trackKey;
			});
			busArr[newBusInd][1] = newLabelArr;
			thisBusIndLabel = inputs.flop[0].indexOf(oldLabel);
			if(newLabelArr.isEmpty, {
				if(clearTrack, {
					("Ndef(" ++ oldLabel.cs ++ ").clear(" ++ Ndef(oldLabel).fadeTime ++ ");").radcode;
				}, {
					("Ndef(" ++ oldLabel.cs ++ ").source = nil;").radcode;
					//the last sender's level and times go with it: the next
					//sender to this bus starts from the defaults
					[\vol1, \lag1, \ramp1].select{|key| Ndef(oldLabel).nodeMap[key].notNil } !? {|keys|
						if(keys.notEmpty, {
							("Ndef(" ++ oldLabel.cs ++ ").nodeMap.set(" ++
								keys.collect{|key| key.cs ++ ", nil" }.join(", ") ++ ");").radcode;
						});
					};
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
				//the senders after the one leaving move up a place: so do their values
				//(not when the bus itself is going: its input is cleared with
				//its last sender, and rebuilding it just before that asked the
				//server for a synth whose SynthDef the clear had already freed)
				if(clearTrack.not, {
					this.prRenumberSends(Ndef(oldLabel), oldLabelArr, newLabelArr);
					("Ndef(" ++ oldLabel.cs ++ ", " ++ newLabelArr.busFunc.cs ++ ");").radcode;
				});
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

	// A bus input's controls are named by the sender's place: vol1, lag1,
	// ramp1, vol2, lag2, ramp2, ... When a sender leaves, the senders after it move up a
	// place, so their levels, lags and modulators have to move with them before
	// the input is rebuilt. They did not: track 2's send took the level track
	// 1's send had (18 Nov 2026). The values are moved in the Ndef's node map
	// (not sent to the synth that is fading out), and the places left over at
	// the end are cleared (set to nil), so a sender added later starts from
	// the defaults.
	prRenumberSends {arg ndef, oldArr, newArr;
		var map = ndef.nodeMap, sets = [], moved = IdentityDictionary.new;
		var keysAt = {|index| ["vol", "lag", "ramp"].collect{|key| (key ++ (index + 1)).asSymbol } };
		newArr.do{|sender, newIndex|
			var oldIndex = oldArr.indexOf(sender);
			if(oldIndex.notNil and: { oldIndex != newIndex }, {
				[keysAt.(oldIndex), keysAt.(newIndex)].flop.do{|pair|
					moved[pair[0]] = pair[1];
					//nil clears a value the new place held and the old one did not
					if(map[pair[0]].notNil or: { map[pair[1]].notNil }, {
						sets = sets ++ [pair[1], map[pair[0]]] });
				};
			});
		};
		(oldArr.size - newArr.size).do{|count|
			keysAt.(newArr.size + count).do{|key|
				if(map[key].notNil, { sets = sets ++ [key, nil] }) };
		};
		if(sets.notEmpty, {
			(ndef.cs ++ ".nodeMap.set(" ++ sets.collect(_.cs).join(", ") ++ ");").radcode;
		});
		//modulators on the moved places follow them
		(ModMap.modNodes ? []).do{|item|
			if(item[1] == ndef and: { moved[item[2]].notNil }, { item[2] = moved[item[2]] });
		};
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
		trackCount=1; busCount=1; winRefresh=false; bypassed=nil; morphs=nil;
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
				RadUnit.syncAll(this); //document (step 1): no effects left
				nodeTime.yield;
				action.();
			});
		}.fork;
	}
}
