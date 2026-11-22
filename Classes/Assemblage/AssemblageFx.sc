// Assemblage: effects (filters): adding, removing and setting them, filter tags, effect arguments and lags, convolution reverb.
// Part of the Assemblage class: the class and its variables are in Assemblage.sc.

+ Assemblage {
	filter {arg type=\track, num= 1, slot=1, filter=\pch, extraArgs, buffer, data, action, insert=false;
		var filterTag, ndefArr, ndefCS, arr1, arr2, arr3, arrSize, filterInfo, setArr,
		setTag, filterIndex, startNdefs, filterSpecs, trackTags, convString, routArr1,
		replaceString, cond, bufIndex, bufFunc, ndefNumChan, ndefSpace, routInd, wasBypassed, replacing;
		//still some work to do with buffer alloc
		{
			filterTag = ("filter" ++ type.asString.capitalise ++ "_" ++ num ++ "_" ++ slot).asSymbol;
			if(filterBuff.notNil, {
				bufIndex = filterBuff.flop[0].indicesOfEqual(filterTag);
				if(bufIndex.notNil, {
					bufFunc = {
						//the old effect fades out over the slot's fade time; the
						//fade starts one server latency after it is asked for
						((Ndef(filterTag).fadeTime ? fadeTime) + (server.latency ? 0.2) + 0.1).yield;
						bufIndex.reverseDo{|index| //from the end, so removeAt keeps earlier indices valid
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
			//another effect replacing a bypassed one: the slot is no longer
			//bypassed, and the stage after it reads it again (below)
			replacing = (filters ? []).detect{|item| item[0] == filterTag } !? {|item| item[1] != filter } ? false;
			wasBypassed = replacing and: { this.isBypassed(filterTag) };
			if(wasBypassed, { this.prClearBypass(filterTag) });
			//a load still running on the effect it replaces stops
			if(replacing, { this.prClearMorph(filterTag) });
			ndefArr = this.getThisTrack(type, num);
			ndefSpace = ndefArr.last;
			ndefArr = ndefArr.copyRange(0, ndefArr.size-2);
			ndefNumChan = Ndef(ndefArr[0][0]).numChannels;
			startNdefs = {
				ndefCS = "Ndef.ar(" ++ filterTag.cs ++ ", " ++ ndefNumChan ++ ");";
				ndefCS.radcode;
				ndefCS = "Ndef(" ++ filterTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";";
				ndefCS.radcode;
				server.sync;
			};
			if(filters.isNil, {
				startNdefs.();
			}, {
				if(filters.flop[0].includes(filterTag).not, {
					startNdefs.();
				});
			});
			filterSpecs = [filterTag, SpecFile.read(\filter, filter, false)];
			//each parameter that can travel gets its ramp, built in and off
			filterInfo = [filterTag, Assemblage.rampUnit(SynthFile.read(\filter, filter), filterSpecs[1])];
			cond = Condition(false);

			//one entry per slot: replacing an effect replaces its data
			trackDataArr !? { trackDataArr = trackDataArr.reject({|item| item[0] == filterTag }) };
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
					arr2.sort({arg a, b; this.filterTagBefore(a[0], b[0]) });
					filters = filters.add([filterTag, filter]);
					filters.sort({arg a, b; this.filterTagBefore(a[0], b[0]) });
				});
				arr1.insert(1, arr2);
				arrSize = (arr1.flat.size/2);
				arr1 = arr1.reshape(arrSize.asInteger, 2)
			}, {
				filters = filters.add([filterTag, filter]);
				filters.sort({arg a, b; this.filterTagBefore(a[0], b[0]) });
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
			//aqui talvez
			/*specs[setArr[0]].postln;*/
			specs[setArr[0]] = arr1.flop[0].collect{|item|
				if(trackTags.includes(item), {
					specs[setArr[0]][trackTags.indexOf(item)];
				}, {
					filterSpecs;
				});
			};
			/*specs[setArr[0]].postln;*/
			if(insert, {
				/*"insert is true".postln;*/
				routInd = routArr1.flop[0].indexOf(filterTag);
				("Ndef(" ++ routArr1[routInd][0].cs ++ ", " ++
					routArr1[routInd][1].filterFunc(Ndef(routArr1.copyRange(0, routInd-1)
						.reverse.detect{|item| this.isBypassed(item[0]).not }[0])).cs
					++ ");").radcode;
				if(extraArgs.notNil, {
					("Ndef(" ++ routArr1[routInd][0].cs ++ ").set" ++
						extraArgs.cs.replace("[", "(").replace("]", ");")).radcode;
				}, {
					[routArr1[routInd][1].argNames, routArr1[routInd][1].defaultArgs]
					.flop.do{|item|
						Ndef(filterTag).set(item[0], item[1]);
					};
				});
			}, {
				/*"insert is false".postln;*/
				if(extraArgs.notNil, {
					extraArgs.pairsDo{|it1, it2|
						Ndef(filterTag).set(it1, it2);
					};
				});
				cond.test = false;
				this.autoRoute(routArr1, {cond.test = true; cond.signal; });
				cond.wait;
			});
			server.sync;
			if(wasBypassed, {
				this.prNextActive(arr1, filterTag) !? {|next| this.prRouteStage(arr1, next[0]) };
			});
			//a bypassed slot whose effect was given new settings stays paused
			if(this.isBypassed(filterTag), { ("Ndef(" ++ filterTag.cs ++ ").pause;").radcode });
			bufFunc.();
			server.sync;
			RadUnit.sync(filterTag, this); //document (step 1)
			action.();
		}.fork;
	}

	// fade: the time the removed effect fades out over (its own fade time;
	// the session's when not given)
	removeBufFromFilter {arg thisSlot, actionBuf, fade;
		var cond, bufArrInd;
		cond = Condition(false);
		{
			server.sync;
			(fade ? fadeTime).wait;
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
		keyArr, activeMods, modArr, cond, slotFade;
		//(it used to put every strip's fade time back to the session's here,
		//which wiped the fade times single things were given)
		thisTrack = this.getThisTrack(type, num);
		if(thisTrack.size > 2, {
			thisFilterTag = this.findFilterTag(type, num, slot);
			thisSlot = this.findFilterTag(type, num, slot);
			thisFilterIndex = thisTrack.flop[0].indexOf(thisSlot);
			if(thisFilterIndex.notNil, {
				{
					this.ndefModClear(thisSlot);
					this.prClearBypass(thisSlot);
					this.prClearMorph(thisSlot);
					slotFade = Ndef(thisSlot).fadeTime;
					ndefCS = "Ndef(" ++ thisSlot.cs ++ ").clear(" ++ slotFade ++ ");";
					ndefCS.radcode;
					Radicles.forgetHeldFades([thisSlot]); //nothing to put back on an effect that is gone

					thisTrack.removeAt(thisFilterIndex);
					if(type == \master, {num=""});
					setArr = this.findTrackArr((type ++ num).asSymbol);
					masterNdefs[setArr[0]].removeAt(thisFilterIndex);
					//aqui talvez
					specs[setArr[0]].removeAt(thisFilterIndex);

					this.removeBufFromFilter(thisSlot, actionBuf, slotFade);
					filters = filters.reject({|item| item[0] == thisSlot });
					trackDataArr = trackDataArr.reject({|item| item[0] == thisSlot });
					trackBufferArr = trackBufferArr.reject({|item| item[0] == thisSlot });
					cond = Condition(false);

					this.autoRoute(thisTrack, {
						cond.test = true; cond.signal;
					});
					cond.wait;
					/*					server.sync;*/
					RadUnit.sync(thisSlot, this); //document (step 1)
					action.();
				}.fork(AppClock);
			}, {
				"Filter slot not found".warn;
				action.(); //a caller waiting for the removal carries on
			});
		}, {
			"No filters to remove".warn;
			action.();
		});
	}

	removeTrackFilters {arg type=\track, num= 1, post=true, clear=false, action, refresh=true;
		var thisTrack, ndefCS, arr1, arr2, setArr, cond, bufSlots;
		thisTrack = this.getThisTrack(type, num);
		if(thisTrack.notNil, {
			thisTrack = thisTrack.copyRange(0, thisTrack.size-2);
			if(thisTrack.size > 2, {
				arr1 = [thisTrack[0], thisTrack.last];
				arr2 = thisTrack.copyRange(1, thisTrack.size-2);
				//effect slots on this strip that hold buffers (exact tags, not substrings)
				bufSlots = arr2.flop[0].select({|tag|
					filterBuff.notNil and: { filterBuff.flop[0].includes(tag) } });
				setArr = this.findTrackArr(if(type == \master, {type}, {(type ++ num).asSymbol}));
				//one routine, so action only runs once routing and buffer cleanup are done
				{
					arr2.do{|item|
						this.ndefModClear(item[0]);
						this.prClearBypass(item[0]);
						this.prClearMorph(item[0]);
						ndefCS = "Ndef(" ++ item[0].cs ++ ").clear(" ++ Ndef(item[0]).fadeTime ++ ");";
						ndefCS.radcode;
						masterNdefs[setArr[0]].remove(Ndef(item[0]));
						specs[setArr[0]] = specs[setArr[0]].reject({|it| it[0] == item[0] });
						tracks[setArr[0]] = tracks[setArr[0]].reject({|it| it[0] == item[0] });
						filters = filters.reject({|it| it[0] == item[0] });
						trackDataArr = trackDataArr.reject({|it| it[0] == item[0] });
						trackBufferArr = trackBufferArr.reject({|it| it[0] == item[0] });
						server.sync;
					};
					if(clear.not, {
						cond = Condition(false);
						this.autoRoute(arr1, {cond.test = true; cond.signal;});
						cond.wait;
					});
					//removeBufFromFilter only calls back for slots that hold buffers
					bufSlots.do{|tag|
						cond = Condition(false);
						this.removeBufFromFilter(tag, {cond.test = true; cond.signal;});
						cond.wait;
					};
					RadUnit.syncAll(this); //document (step 1)
					action.();
					if(refresh, {
						{this.refreshFunc;}.defer;
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
		case
		{post == \code} { ndefCS.radcode }
		{post == \spec} { ndefCS.radlog; [arg1, arg2].cs.post; }
		{ ndefCS.radlog };  // post: nil keeps it out of the document, not the log
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
		specArr = this.specsOf(filterTag);
		if(arg1.isNumber, {
			thisSpec =	specArr[arg1];
		}, {
			thisSpec = 	specArr[specArr.flop[0].indexOf(arg1)];
		});
		modSpec = thisSpec[1].specFactor(mul, add, min, val, warp);
		thisVal = thisSpec[2].(modSpec.asSpec.map(arg2));
		this.setFilterTag(filterTag, thisSpec[0], thisVal, post);
	}

	filterTagOnStrip {arg tag, type, num;
		//exact match of an effect tag (e.g. filterTrack_10_1) to a strip, so track 1 never matches track 10
		var parts;
		parts = tag.asString.split($_);
		if(parts.size < 3, {^false});
		if(parts[0].toLower != ("filter" ++ type.asString.toLower), {^false});
		^(type.asSymbol == \master) or: { parts[1].asInteger == num.asInteger };
	}

	specsOf {arg key;
		//the spec list stored for one Ndef key (exact match)
		specs.do{|strip| strip.do{|item| if(item[0] == key, {^item[1]}) } };
		^nil;
	}

	filterTagBefore {arg tagA, tagB;
		//sort effect tags by strip type, then track and slot as numbers (so slot 2 comes before slot 10)
		var a, b;
		a = tagA.asString.split($_);
		b = tagB.asString.split($_);
		if(a[0] != b[0], {^a[0].compare(b[0]) <= 0});
		if(a[1].asInteger != b[1].asInteger, {^a[1].asInteger < b[1].asInteger});
		^a[2].asInteger <= b[2].asInteger;
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

	// an effect's own parameters (not its time controls): their names, a flat
	// list of names and values, or pairs; \lags: each parameter that can
	// travel with its time for typed values (0: off)
	getFilterKeys {arg filterTag, type=\args;
		var result, keys;
		Ndef(filterTag).cleanNodeMap;
		keys = Assemblage.paramKeys(Ndef(filterTag));
		case
		{type == \args} { result = keys; }
		{type == \vals} { result = Ndef(filterTag).getKeysValues.select{|pair| keys.includes(pair[0]) }.flat;}
		{type == \pairs} { result = Ndef(filterTag).getKeysValues.select{|pair| keys.includes(pair[0]) };}
		{type == \lags} {
			result = Assemblage.lagParams(Ndef(filterTag)).collect{|key|
				[key, Assemblage.unitLag(Ndef(filterTag), key)] }.flat;
		};
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

	// the time for typed values of an effect's parameters: one number for
	// every parameter that can travel, or a list by position (the effect's
	// parameters in their order; nil or a missing place leaves one as it is)
	filterLags {arg filterNdefKey, lag=nil;
		var keys = Assemblage.paramKeys(Ndef(filterNdefKey)), pairs;
		if(lag.isArray, {
			pairs = lag.collect{|time, index| if(time.isNumber and: { keys[index].notNil }, { [keys[index], time] }) }
			.reject(_.isNil);
		}, {
			pairs = Assemblage.lagParams(Ndef(filterNdefKey)).collect{|key| [key, lag] };
		});
		Assemblage.unitLagSet(filterNdefKey, pairs.flat);
	}

	// posts the code that gives parameters of an effect (or a block) their
	// time for typed values: pairs is a flat list of names and times. A
	// parameter that cannot travel is named in a warning and left out.
	// Nothing is rebuilt: the times are controls.
	*unitLagSet {arg ndefKey, pairs;
		var can = Assemblage.lagParams(Ndef(ndefKey)), args = [], cannot = [];
		pairs.pairsDo{|key, time|
			if(can.includes(key) and: { time.isNumber }, {
				args = args ++ [(key ++ "Lag").asSymbol, time.max(0)];
			}, {
				cannot = cannot.add(key);
			});
		};
		if(cannot.notEmpty, {
			("cannot travel (no spec, a stepped spec, or a spec with a function): " ++ cannot.join(", ")).warn;
		});
		if(args.notEmpty, {
			("Ndef(" ++ ndefKey.cs ++ ").set(" ++ args.collect(_.cs).join(", ") ++ ");").radcode;
		});
	}

	// names for a list of parameters given by name or by number (1 is the
	// unit's first parameter)
	*unitParamNames {arg ndefKey, list;
		var keys = Assemblage.paramKeys(Ndef(ndefKey));
		^list.asArray.collect{|item| if(item.isNumber, { keys[item.asInteger - 1] }, { item.asSymbol }) }.reject(_.isNil)
	}

	// on: every parameter that can travel takes the effects' lag time; off:
	// none travels; a number: the parameters that are on take it; exc: the
	// listed ones off; inc: only the listed ones on
	*unitLagMode {arg ndefKey, mode, list, globalTime;
		var ndef = Ndef(ndefKey), can = Assemblage.lagParams(ndef), names, on;
		globalTime = globalTime ? Radicles.fxLag;
		on = can.select{|key| Assemblage.unitLag(ndef, key) > 0 };
		case
		{ mode == \on } {
			if(can.isEmpty, { "none of its parameters can travel".warn }, {
				Assemblage.unitLagSet(ndefKey, can.collect{|key| [key, globalTime] }.flat) }) }
		{ mode == \off } {
			Assemblage.unitLagSet(ndefKey, on.collect{|key| [key, 0] }.flat) }
		{ mode.isNumber } {
			if(on.isEmpty, { "none of its parameters is on (on turns them on)".warn }, {
				Assemblage.unitLagSet(ndefKey, on.collect{|key| [key, mode] }.flat) }) }
		{ mode == \exc } {
			names = Assemblage.unitParamNames(ndefKey, list);
			Assemblage.unitLagSet(ndefKey, names.collect{|key| [key, 0] }.flat) }
		{ mode == \inc } {
			names = Assemblage.unitParamNames(ndefKey, list);
			Assemblage.unitLagSet(ndefKey, (names.collect{|key|
				[key, if((Assemblage.unitLag(ndef, key) ? 0) > 0, { Assemblage.unitLag(ndef, key) }, { globalTime })] }
			++ can.reject{|key| names.includes(key) }.select{|key| on.includes(key) }.collect{|key| [key, 0] }).flat) }
		{ ("fxlag: on, off, exc or inc, not " ++ mode).warn };
	}

	fxTrackLagMode {arg trackType, trackNum, trackSlot, mode, list;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			Assemblage.unitLagMode(ndefKey, mode, list);
		});
	}

	fxLagMode {arg filterNum, mode, list;
		this.fxWarn(filterNum, {|ndefKey|
			Assemblage.unitLagMode(ndefKey, mode, list);
		});
	}

	// fxlag N: the effects' lag time, for every parameter of every effect
	// that is on, and for what is switched on later
	setFxLag {arg time;
		time = time.max(0);
		("Radicles.fxLag = " ++ time ++ ";").radcode;
		(filters ? []).do{|item|
			var ndef = Ndef(item[0]), on;
			on = Assemblage.lagParams(ndef).select{|key| Assemblage.unitLag(ndef, key) > 0 };
			if(on.notEmpty, { Assemblage.unitLagSet(item[0], on.collect{|key| [key, time] }.flat) });
		};
	}

	// the code that sets parameters of an effect to values (a flat list of
	// names and values): a parameter that is on travels over its own time, or
	// over the one given (for this move only)
	*unitSetCode {arg ndefKey, pairs, time, bracketStyle=false;
		var ndef = Ndef(ndefKey), ramps = [], body;
		pairs.pairsDo{|key, value|
			var rampKey = (key ++ "Ramp").asSymbol, move;
			if(Assemblage.lagParams(ndef).includes(key), {
				move = time ? Assemblage.unitLag(ndef, key);
				if((ndef.nodeMap.at(rampKey) ? 0) != move and: { ndef.nodeMap.at(key) != value }, {
					ramps = ramps ++ [rampKey, move];
				});
			});
		};
		body = if(bracketStyle, {
			(pairs ++ ramps).cs.replace("[", "").replace("]", "")
		}, {
			(pairs ++ ramps).collect(_.cs).join(", ")
		});
		^"Ndef(" ++ ndefKey.cs ++ ").set(" ++ body ++ ");"
	}

	convRevBuf {arg filterTag, impulse=\ortf_s1r1, fftsize=2048, inVar,
		action={|val| val.radpost}, chanIn, globBuf;
		var path, buffArr, file, numChan, irbuffer, irArr, bufsize, numtag,
		string, filterBuffArr, wcond, dash;
		{
			wcond = Condition.new(false);
			inVar ?? {inVar = "input"};
			dash = "/";
			Platform.case(
				\windows,   {dash = "\\"; }
			);
			path = soundFilePath ++ dash++ "IR" ++ dash ++ impulse ++ ".wav";
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

	listFx {
		SynthFile.read(\filter, exclude: excludeLibs).sort.radpost;
	}

	setFx {arg trackType=\track, num=1, slot=1, filter=\pch, extraArgs, buffer,
		data, remove=false, action, actionBuf;
		var insert, newFilterNdef, extraArgsBool;
		if(SynthDefFile.read(\filter, exclude: excludeLibs).includes(filter), {
			if(remove, {
				this.removeFilter(trackType, num, slot, {this.refreshFunc; action.();}, {actionBuf.()} );
			}, {
				if(this.findFilterTag(trackType, num, slot).isNil, {
					insert = false;
				}, {
					insert = true;
				});
				if(extraArgs.notNil, {
					extraArgsBool = extraArgs.select({|item, index| index.even.and(item == 0) }).isEmpty;
					if(extraArgsBool, {
						if(extraArgs.select{|item| item.isSymbol}.isEmpty, {
							extraArgs = extraArgs.collect({|item, index| if(index.even, {
								item = SynthFile.read('filter', filter, exclude: excludeLibs).argNames[item-1];
							}, {
								item = item;
							});
							});
						});
					}, {
						"wrong arg number, should be numbers starting with 1".warn;
					});
				}, {
					extraArgsBool = true;
				});
				if(extraArgsBool, {
					this.filter(trackType, num, slot, filter, extraArgs, buffer, data, {
						{this.refreshFunc}.defer;
						action.();
					}, insert);
				});
			});
		}, {
			"filter doesn't exist or is not loaded".warn;
		});
	}

	setFxs {	arg settingsArr, action, refresh=true;
		var cond, refreshUI, insert, newFilterNdef, thisTracks, arr, thisInfo, dest;
		cond = Condition.new;
		{
			settingsArr.do{|item, index|
				cond.test = false;
				this.filter(item[0], item[1], item[2], item[3], item[4], item[5], item[6], {
					server.sync;
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
					++ ");").radcode;
				server.sync;
			};
			if(refresh, {
				{this.refreshFunc;}.defer;
			});
			action.();
		}.fork;
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

	// fxArg: a list of pairs, number and value or name and value (1 is the
	// effect's first parameter), or one name with value. type \set sets the
	// values (time: for this move only); \lag gives the parameters their
	// time for typed values.
	prepArrArg {arg fxArg, ndefKey, type=\set, value, time;
		var newArr1, newArr2, thisArg, keys = Assemblage.paramKeys(Ndef(ndefKey));
		if(fxArg.isArray, {
			if(fxArg.rank == 2, {fxArg = fxArg.flat});
			if(fxArg.select({|item| item.isSymbol }).isEmpty, {
				newArr1 = fxArg.reshape((fxArg.size/2).asInteger,2);
				newArr2 = newArr1.flop[0].collect{|item| keys[item-1] };
				thisArg = [newArr2, newArr1.flop[1]].flop.flat;
			}, {
				thisArg = fxArg;
			});
		}, {
			thisArg = [fxArg, value];
		});
		if(type == \lag, {
			Assemblage.unitLagSet(ndefKey, thisArg);
		}, {
			Assemblage.unitSetCode(ndefKey, thisArg, time, fxArg.isArray).radcode(\radpostcont);
		});
	}

	setFxArg {arg filterNum, fxArg, value, time;
		var thisArg, index;
		this.fxWarn(filterNum, {|ndefKey|
			if(fxArg.notNil, {
				{
					if(fxArg.isNumber, {
						index = fxArg-1;
						thisArg = Assemblage.paramKeys(Ndef(ndefKey))[index];
						if(thisArg.notNil, {
							Assemblage.unitSetCode(ndefKey, [thisArg, value], time).radcode(\radpostcont);
						}, {
							"Fx argument doesn't exist".warn;
						});
					}, {
						if(fxArg.isSymbol, {
							Assemblage.unitSetCode(ndefKey, [fxArg, value], time).radcode(\radpostcont);
						}, {
							this.prepArrArg(fxArg, ndefKey, \set, value, time);
						});
					});
					server.sync;
					this.updateFxWin(ndefKey);
				}.fork(AppClock);
			}, {
				Assemblage.paramKeys(Ndef(ndefKey)).postln;
			});
		});
	}

	setFxArgTrack {arg trackType, trackNum, trackSlot, fxArg, value, update=true, time;
		var ndefKey, thisArg, index, newArr1, newArr2;
		this.fxTrackWarn(trackType, trackNum, trackSlot, {|ndefKey|
			if(fxArg.notNil, {
				{
					if(fxArg.isNumber, {
						index = fxArg-1;
						thisArg = Assemblage.paramKeys(Ndef(ndefKey))[index];
						if(thisArg.notNil, {
							Assemblage.unitSetCode(ndefKey, [thisArg, value], time).radcode(\radpostcont);
						}, {
							"Fx argument doesn't exist".warn;
						});
					}, {
						if(fxArg.isSymbol, {
							Assemblage.unitSetCode(ndefKey, [fxArg, value], time).radcode(\radpostcont);
						}, {
							if(fxArg.select({|item, index| index.even.and(item == 0) }).isEmpty, {
								this.prepArrArg(fxArg, ndefKey, \set, value, time);
							}, {
								"wrong arg number, should be numbers starting with 1".warn;
							});
						});
					});
					server.sync;
					if(update, {
						this.updateFxWin(ndefKey);
					});
				}.fork(AppClock);
			}, {
				Assemblage.paramKeys(Ndef(ndefKey)).postln;
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
						thisArg = Assemblage.paramKeys(Ndef(ndefKey))[index];
						if(thisArg.notNil, {
							Assemblage.unitLagSet(ndefKey, [thisArg, value]);
						}, {
							"Fx argument doesn't exist".warn;
						});
					}, {
						this.prepArrArg(fxArg, ndefKey, \lag, value);
					});
				}.fork(AppClock);
			}, {
				this.getFilterKeys(ndefKey, \lags).postln;
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
						thisArg = Assemblage.paramKeys(Ndef(ndefKey))[index];
						if(thisArg.notNil, {
							Assemblage.unitLagSet(ndefKey, [thisArg, value]);
						}, {
							"Fx argument doesn't exist".warn;
						});
					}, {
						this.prepArrArg(fxArg, ndefKey, \lag, value);
					});
				}.fork(AppClock);
			}, {
				this.getFilterKeys(ndefKey, \lags).postln;
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
					if(this.filterTagOnStrip(item, trackType, trackNum), {
						arr = 	arr.add((index+1));
					});
				});
				if(arr.notNil, {
					this.fxsLags([arr, lag!arr.size].flop);
				});
			});
		});
	}
}
