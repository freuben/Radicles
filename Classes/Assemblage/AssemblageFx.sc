// Assemblage: effects (filters): adding, removing and setting them, filter tags, effect arguments and lags, convolution reverb.
// Part of the Assemblage class: the class and its variables are in Assemblage.sc.

+ Assemblage {
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
					routArr1[routInd][1].filterFunc(Ndef(routArr1[routInd-1][0])).cs
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
					ndefCS.radcode;

					thisTrack.removeAt(thisFilterIndex);
					if(type == \master, {num=""});
					setArr = this.findTrackArr((type ++ num).asSymbol);
					masterNdefs[setArr[0]].removeAt(thisFilterIndex);
					//aqui talvez
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
						ndefCS = "Ndef(" ++ item[0].cs ++ ").clear(" ++ fadeTime ++ ");";
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

	getFilterKeys {arg filterTag, type=\args;
		var result, getKey, controlKeys, ratesFor;
		Ndef(filterTag).cleanNodeMap;
		case
		{type == \args} { result = Ndef(filterTag).controlKeys; }
		{type == \vals} { result = Ndef(filterTag).controlKeysValues;}
		{type == \pairs} { result = Ndef(filterTag).getKeysValues;}
		{type == \lags} {
			getKey = filterTag;
			controlKeys = Ndef(getKey).controlKeys;
			ratesFor = Ndef(getKey).nodeMap.ratesFor(
				controlKeys);
			if(ratesFor.notNil, {
				ratesFor.do{|item, index|
					if(item.notNil, {
						result = result.add([controlKeys[index], item]);
					});
				};
				result = result.flat;
			});
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
			.replace("[", "").replace("]", "") ++ ");").radcode;
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

	prepArrArg {arg fxArg, ndefKey, type=\set, value;
		var newArr1, newArr2, thisArg;
		if(fxArg.isArray, {
			if(fxArg.rank == 2, {fxArg = fxArg.flat});
			if(fxArg.select({|item| item.isSymbol }).isEmpty, {
				newArr1 = fxArg.reshape((fxArg.size/2).asInteger,2);
				newArr2 = newArr1.flop[0].collect{|item| Ndef(ndefKey).controlKeys[item-1] };
				thisArg = [newArr2, newArr1.flop[1]].flop.flat;
				("Ndef(" ++ ndefKey.cs ++ ")." ++ type.asString ++ "(" ++
					thisArg.cs.replace("[", "").replace("]", "") ++
					");").radcode(\radpostcont);
			}, {
				("Ndef(" ++ ndefKey.cs ++ ")." ++ type.asString ++ "(" ++
					fxArg.cs.replace("[", "").replace("]", "") ++
					");").radcode(\radpostcont);
			});
		}, {
			("Ndef(" ++ ndefKey.cs ++ ")." ++ type.asString ++ "(" ++
				fxArg.cs ++ ", " ++ value.cs ++ ");").radcode(\radpostcont);
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
								value.cs ++ ");").radcode(\radpostcont);
						}, {
							"Fx argument doesn't exist".warn;
						});
					}, {
						if(fxArg.isSymbol, {
							("Ndef(" ++ ndefKey.cs ++ ").set(" ++ fxArg.cs ++ ", " ++
								value.cs ++ ");").radcode(\radpostcont);
						}, {
							this.prepArrArg(fxArg, ndefKey, \set, value);
						});
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
								value.cs ++ ");").radcode(\radpostcont);
						}, {
							"Fx argument doesn't exist".warn;
						});
					}, {
						if(fxArg.isSymbol, {
							("Ndef(" ++ ndefKey.cs ++ ").set(" ++ fxArg.cs ++ ", " ++
								value.cs ++ ");").radcode(\radpostcont);
						}, {
							if(fxArg.select({|item, index| index.even.and(item == 0) }).isEmpty, {
								this.prepArrArg(fxArg, ndefKey, \set, value);
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
								value.cs ++ ");").radcode;
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
								value.cs ++ ");").radcode;
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
