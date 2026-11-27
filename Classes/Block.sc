Block : Radicles {classvar stopFades, <lastDefs, <blocks, <ndefs, <liveBlocks, <blockCount=1,
	<>recbuffers, <recNdefs, <recBlocks, <recBlockCount=1, <recBufInfo, timeInfo,
	<pattCount=1, <timecond, startTickets, <>wavetableSize=2048, liveBuffers, positionBuses, making;

	*add {arg channels=1, action;
		var ndefTag, ndefCS1, ndefCS2;
		{
		ndefTag = ("block" ++ blockCount).asSymbol;
		blockCount = blockCount + 1;
		ndefCS1 = "Ndef.ar(";
		ndefCS1 = (ndefCS1 ++ ndefTag.cs ++ ", " ++ channels.cs ++ ");");
		ndefCS1.radcode;
		server.sync;
		ndefCS2 = ("Ndef(" ++ ndefTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
		ndefCS2.radcode;
			server.sync;
		ndefs = ndefs.add(Ndef(ndefTag));
		blocks = blocks.add( [ndefTag, channels] );
		liveBlocks = liveBlocks.add(nil);
		timecond = timecond.add(Condition(false));
			action.(Ndef(ndefTag));
		}.fork;
	}

	*addNum {arg number, channels=1, action;
		var thisChan, cond, arr;
		{
		cond = Condition(false);
		number.do{|index|
				cond.test = false;
			if(channels.isArray, {thisChan = channels[index]}, {thisChan = channels});
				this.add(thisChan, {|val| cond.test = true; cond.signal; arr = arr.add(val) });
			cond.wait;
		};
			action.(arr);
		}.fork;
	}

	*addAll {arg arr, action;
		var cond, ndefArr;
		{
		cond = Condition(false);
		arr.do{|item|
				cond.test = false;
			this.add(item, {|val| cond.test = true; cond.signal; ndefArr = ndefArr.add(val) });
			cond.wait;
		};
			action.(ndefArr);
	}.fork;
	}

	// A block keeps its number for the whole session: the block typed as N is
	// the one named blockN, wherever it sits in the lists once another block
	// has been removed (decided 18 Nov 2026; play, stop and remove counted
	// positions before, while blkset, the presets and the posted code went by
	// name). These answer nil when there is no such block.
	*indexOfBlock {arg block;
		var tag;
		if(block.isNumber.not or: { ndefs.isNil }, { ^nil });
		tag = ("block" ++ block.asInteger).asSymbol;
		^ndefs.detectIndex{|ndef| ndef.key == tag };
	}

	*ndefOf {arg block;
		^this.indexOfBlock(block) !? {|index| ndefs[index] };
	}

	// the number of the block at a place in the lists (block3 -> 3)
	*numberAt {arg index;
		^ndefs[index] !? {|ndef| ndef.key.asString.divNumStr[1].asInteger };
	}

	// Make the block with this number (blockN) when it isn't there: after it
	// was removed, or a number that never existed. It goes into the lists in
	// number order. It is connected to the track with its number when that
	// track exists and nothing feeds it (as start sets blocks up); otherwise
	// it is left unconnected, and a note says so. Call inside a Routine.
	*make {arg block;
		var tag, ndef, channels = 1, index, trackKey, connected = false;
		block = block.asInteger;
		tag = ("block" ++ block).asSymbol;
		trackKey = ("track" ++ block).asSymbol;
		if(aZ.notNil and: { (aZ.trackNames ? []).includes(trackKey) }, {
			channels = Ndef(("inTrack" ++ block).asSymbol).numChannels ? 1;
		});
		("Ndef.ar(" ++ tag.cs ++ ", " ++ channels.cs ++ ");").radcode;
		server.sync;
		("Ndef(" ++ tag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";").radcode;
		server.sync;
		ndef = Ndef(tag);
		index = (ndefs ? []).count{|item| item.key.asString.divNumStr[1].asInteger < block };
		ndefs = (ndefs ? []).insert(index, ndef);
		blocks = (blocks ? []).insert(index, [tag, channels]);
		liveBlocks = (liveBlocks ? []).insert(index, nil);
		timecond = (timecond ? []).insert(index, Condition(false));
		blockCount = max(blockCount, block + 1);
		if(aZ.notNil and: { (aZ.trackNames ? []).includes(trackKey) }
			and: { RadUnit.readInput(trackKey, aZ).isNil }, {
			aZ.input(ndef, \track, block);
			server.sync;
			connected = true;
			{ aZ.refreshFunc }.defer;
		});
		if(connected.not, {
			("// " ++ tag ++ " is not connected to a track (blk " ++ block ++ " <> 1 connects it to track 1)").radcode;
		});
		^ndef
	}

	// The block of a track: block N feeds track N. Makes it when it isn't
	// there (a new track's block takes the track's number, also after other
	// tracks and blocks were removed), and connects it when nothing feeds the
	// track.
	*forTrack {arg num, action;
		{
			var ndef = this.ndefOf(num);
			if(ndef.isNil, { ndef = this.make(num) }, {
				if(aZ.notNil and: { RadUnit.readInput(("track" ++ num).asSymbol, aZ).isNil }, {
					aZ.input(ndef, \track, num);
					server.sync;
				});
			});
			action.(ndef);
		}.fork(AppClock);
	}

	*remove {arg block=1;
		var blockIndex, string;
		blockIndex = this.indexOfBlock(block);
		if(blockIndex.notNil, {
			//unplug it from the tracks it feeds first, so no track is left
			//pointing at a block that is gone
			if(aZ.notNil, {
				aZ.removeInput(ndefs[blockIndex]);
				aZ.closeSourceWin(ndefs[blockIndex].key);
			});
			this.newStartTicket(ndefs[blockIndex].key); //cancel a start still loading
			RadUnit.prStopBlockMorph(ndefs[blockIndex].key, false); //and a preset load still running
			//it fades out over its own fade time (a plain clear cut it: a
			//click), also when a load had set another for a while
			string = (ndefs[blockIndex].cs ++ ".clear(" ++ Radicles.ownFade(ndefs[blockIndex].key) ++ ");");
			string.radcode;
			Radicles.forgetHeldFades([ndefs[blockIndex].key]); //nothing to put back on a block that is gone
			ndefs.removeAt(blockIndex);
			blocks.removeAt(blockIndex);
			liveBlocks.removeAt(blockIndex);
			timecond.removeAt(blockIndex);
			RadUnit.syncBlocks; //document (step 2)
			if(aZ.notNil, { aZ.refreshSourceSlots; { aZ.refreshFunc }.defer });
		}, {
			"Block Number not Found".warn;
		});
	}

	*removeArr {arg blockArr;
		var newArr, string;
		blockArr.do{|item|
			var index = this.indexOfBlock(item);
			if(index.notNil, {
				newArr = newArr.add(index);
				string = (ndefs[index].cs ++ ".clear;");
				string.radcode;
			}, {
				"Block Number not Found".warn;
			});
		};
		ndefs.removeAtAll(newArr);
		blocks.removeAtAll(newArr);
		liveBlocks.removeAll(newArr);
		timecond.removeAll(newArr);
		RadUnit.syncBlocks; //document (step 2)
	}

	*removeAll {var string;
		ndefs.do{|item|
			string = (item.cs ++ ".clear;");
			string.radcode;
		};
		ndefs = [];
		blocks = [];
		liveBlocks = [];
		timecond = [];
		blockCount = 1;
		liveBuffers = nil;
		this.prFreePositionBuses;
		RadUnit.syncBlocks; //document (step 2)
	}

	*clear {
		"Ndef.clear;".radcode;
		ndefs = [];
		blocks = [];
		liveBlocks = [];
		timecond = [];
		blockCount = 1;
		liveBuffers = nil;
		this.prFreePositionBuses;
		RadUnit.syncBlocks; //document (step 2)
	}

	*initCond {arg block=1;
		this.indexOfBlock(block) !? {|index|
			timecond[index] = Condition(false);
			timecond[index].test = false;
		};
	}

	*syncStart {arg block=1;
		this.indexOfBlock(block) !? {|index|
			timecond[index].test = true;
			timecond[index].signal;
		};
	}

	*allSync {
		/*{*/
		blocks.flop[0].do{|item|
			var blockNum;
			blockNum = item.asString.replace("block").asInteger;
			this.syncStart(blockNum);
			/*server.sync;*/
		};
		/*}.fork;*/
	}

	// Issue #51: a block start waits for its buffer and the server before its
	// function goes into the Ndef (the loading window). Each start takes a
	// ticket; st, or a newer start on the same block, takes the next one, so an
	// older start still loading knows it has been cancelled.
	*newStartTicket {arg key;
		startTickets = startTickets ?? { IdentityDictionary.new };
		startTickets[key] = (startTickets[key] ? 0) + 1;
		^startTickets[key];
	}

	*isCurrentStart {arg key, ticket;
		^startTickets.notNil and: { startTickets[key] == ticket };
	}

	*play {arg block=1, blockName, buffer, extraArgs, data, sync=false, xfade=true, action;
		var blockFunc, blockIndex, ndefCS, blockFuncString,
		storeType, dataString, cond, bufferArr, bufferID, bufInfo, bstoreSize, bufStrings, bufLoaded, bufWait,
		pattArr, extraPattCount, bufIndex, bufString, bufIDs, extraArgsBool, thisExcludeLibs,
		startKey, ticket, cancelStart;
		// names as symbols, as the call window passes them: a String block
		// name found no definition (nothing played), and a String buffer was
		// taken for an array of buffers (one per letter)
		if(blockName.isString, { blockName = blockName.asSymbol });
		if(buffer.isString, { buffer = buffer.asSymbol });
		if(block >= 1, {
			blockIndex = this.indexOfBlock(block);
			if(blockIndex.notNil, {
				startKey = ndefs[blockIndex].key;
				ticket = this.newStartTicket(startKey);
				// a start cancelled while loading frees what it loaded (unless a
				// live block uses it), leaves the Ndef alone and says so
				cancelStart = {
					var ids, registered, inUse;
					("// " ++ startKey ++ ": " ++ blockName ++ " " ++ (buffer ? "")
						++ " cancelled while loading").radcode;
					if(bufferID.notNil, {
						ids = if(bufferID.rank <= 1, { [bufferID] }, { bufferID });
						// the buffer is registered a moment after the server sync:
						// wait for it (up to 5 s), then free it unless a live block uses it
						registered = { ids.every{|id| (BStore.bstoreIDs ? []).includesEqual(id) } };
						50.do{ if(registered.value.not, { 0.1.wait }) };
						inUse = liveBlocks.select(_.notNil).collect{|item| item[2] };
						if(inUse.includesEqual(bufferID).not, {
							ids.do{|id| BStore.removeID(id) };
						});
					});
					action.();
				};
				{
					this.initCond(block);
					if((blockName == 'pattern').not, {
						thisExcludeLibs = excludeLibs;
						thisExcludeLibs = thisExcludeLibs.select({|item| item != "Main"});
						blockFunc = SynthFile.read(\block, blockName, thisExcludeLibs);
						blockFuncString = this.prPositionString(startKey, blockFunc.cs);
						if(blockFuncString.includesString("\\buffer")
							.or(blockFuncString.includesString("'buffer'")), {
								if(buffer.isArray.not, {
									bufferArr = BStore.setBufferID(buffer, blockFuncString);
									storeType = bufferArr[0];
									bufInfo = bufferArr[1];
									bufferID = bufferArr[2];
									if(bufInfo.notNil.or(bufInfo == \nobuf), {
										server.sync;
										/*buffer*/
										bufLoaded = false;
										BStore.add(storeType, bufInfo, {arg buf;
											bufLoaded = true;
											if(buf.notNil, {
											bufString = BufferSystem.getGlobVar(buf);
											/*bufIndex = BufferSystem.bufferArray.indexOf(buf);
											bufString = BufferSystem.globVarArray[bufIndex];*/
											/*blockFunc.postln;*/
											blockFunc = this.prFileRateString(blockFuncString, buf).replace("\\buffer",
												bufString).replace("'buffer'", bufString).interpret;
											}, {
												blockFunc = nil;
											});
											if(data.notNil, {
												this.readWavetable(data, buf);
											});
										});
										server.sync;
										//wait for the buffer (at most 5 s), so the block's code is
										//posted with the buffer's name instead of \buffer
										bufWait = Main.elapsedTime;
										while({ bufLoaded.not and: { (Main.elapsedTime - bufWait) < 5 } }, {
											0.01.wait;
										});
									}, {
										"Buffer not provided".warn;
									});
								}, {
									if([\alloc, \play, \cue].includes(buffer[0]), {
										/*bufIDs = BStore.buffByID(buffer);
										bufIndex = BufferSystem.bufferArray.indexOf(bufIDs);
										bufString = BufferSystem.globVarArray[bufIndex];*/
										bufString = BStore.buffStrByID(buffer);
										blockFunc = this.prFileRateString(blockFuncString, BStore.buffByID(buffer))
										.replace("\\buffer", bufString).replace("'buffer'", bufString).interpret;
										/*"this buffer is an existing buffer with ID".postln;*/
									}, {
										buffer.do{|item|
											bufferArr = bufferArr.add(
												BStore.setBufferID(item, blockFuncString)
											);
										};
										storeType = bufferArr.flop[0];
										bufInfo = bufferArr.flop[1];
										bufferID = bufferArr.flop[2];
										/*"includes buffer array".postln;*/
										cond = Condition(false);
										cond.test = false;
										//for multiple wavetables with consecutive buffer allocation:
										if(data.notNil, {
											/*"if this is a wavetable then alloc consecutive buffers".postln;*/
											if(BStore.bstores.notNil, {
												bstoreSize = BStore.bstores.collect({|item| item.bufnum}).maxItem+1;
											}, {
												bstoreSize = 0;
											});
											bufferID = bufferID.collect({|item, index| item = [item[0], item[1], item[2]
												++ [1, bstoreSize+index] ] });
										});
										server.sync;
										BStore.addAll(bufferID, {arg buf;
											//all the buffers, as an array (vosc reads buffer[0]
											//and buffer.size); before Oct 2026 only the last one
											bufStrings = bufferID.collect({|item| BStore.buffStrByID(item) });
											bufString = "[" ++ bufStrings.join(", ") ++ "]";
											blockFunc = blockFunc.cs.replace(("\\buffer"),
												bufString).replace(("'buffer'"), bufString).interpret;
											cond.test = true;
											cond.signal;
											//fill buffer with wavetable function
											if(data.notNil, {
												if(data.isArray, {
													/*	"this data is an array".postln;*/
													data.do{|item, index|
														(DataFile.read(\wavetables, item, excludeLibs).cs ++ ".(" ++
															bufStrings[index] ++	");").radshow;
														DataFile.read(\wavetables, item, excludeLibs).(buf[index]);
													};
												}, {
													/*"this data is a symbol".postln;*/
													buf.do{|item, index|
														(DataFile.read(\wavetables, data, excludeLibs).cs ++ ".(" ++
															bufStrings[index] ++ "," ++ (buf.size+1) ++ ", " ++
															index ++ ");").radshow;
														DataFile.read(\wavetables, data, excludeLibs).(item, buf.size+1, index);
													};
												});
											});
										});
										cond.wait;
										server.sync;
										/*nodeTime.wait;*/
										/*"this buffer array that need to be allocated".postln;*/
									});
								});
							}, {
								/*"no buffer".postln;*/
						});
					}, {
						/*"this is a pattern hurray".postln;*/
						/*blockFunc = this.blockPattern(block, extraArgs, data);*/
						if(extraArgs.isArray.not, {
							blockFunc = this.pattData(DataFile.read(\pattern, extraArgs, excludeLibs), data)
							.toPattern(pattCount);
						}, {
							if(extraArgs.collect({|item| item.isArray}).includes(true), {
								blockFunc = this.pattData(extraArgs, data)
								.toPattern(pattCount);
								/*	"this is a pattern defined".postln;*/
							}, {
								extraPattCount = 1;
								blockFunc = extraArgs.do{|item|
									pattArr = pattArr.add(
										this.pattData(DataFile.read(\pattern, item, excludeLibs), data)
										.toPattern(pattCount.cs ++ "_" ++ extraPattCount));
									extraPattCount = extraPattCount + 1;
								};
								blockFunc = Pdef(("'patt" ++ block ++ "'").interpret, Ppar(pattArr, 1));
								/*	"this is a ppar".postln;*/
							});
						});
						pattCount = pattCount + 1;
						/*blockFuncCS = blockFunc;*/
					});
					if(sync, {
						timecond[blockIndex].wait;
					});
					if(this.isCurrentStart(startKey, ticket).not, { cancelStart.value }, {
						if(liveBlocks[blockIndex].notNil, {
							if((liveBlocks[blockIndex][2] == bufferID).not, {
								/*"free buffer from play".postln;*/
									if(blockFunc.notNil, {
								this.buffree(blockIndex, ndefs[blockIndex].fadeTime*2);
								});
							});
						});
						//each parameter that can travel gets its ramp, built in and off
						//(docs/design/lags-blocks-effects.md); another block in this
						//number does not inherit which parameters were on
						if(blockFunc.isKindOf(Function), {
							blockFunc = Assemblage.rampUnit(blockFunc, SpecFile.read(\block, blockName, false));
						});
						if(blockFunc.notNil, {
						ndefCS = (ndefs[blockIndex].cs.replace(")")	++ ", "
							++ blockFunc.cs ++ ");");
						ndefCS.radshow;
						});
						if((blockName == 'pattern').not, {
							if(extraArgs.notNil, {
								if(extraArgs.collect{|item| item.isSymbol}.includes(true), {
								}, {
									extraArgsBool = extraArgs.select({|item, index|
										index.even.and(item == 0) }).isEmpty;
									if(extraArgsBool, {
										extraArgs = extraArgs.collect({|item, index| if(index.even, {
											item = blockFunc.argNames[item-1];
										}, {
											item = item;
										});
										});
									}, {
										"wrong arg number, should be numbers starting with 1".warn;
									});
								});
								if(xfade, {
									this.xset(block, extraArgs);
								}, {
									this.set(block, extraArgs);
								});
							});
						}, {
							/*"no pattern before".postln;*/
							if(liveBlocks[blockIndex].notNil, {
								if((liveBlocks[blockIndex][1] != \pattern), {
									//replace out with cs string using presetToNdef
									ndefs[blockIndex].put(0, nil);
									fadeTime.wait;
									ndefs[blockIndex].resetNodeMap;
								});
							});
						});
						server.sync;
						if(this.isCurrentStart(startKey, ticket), {
							//replace out with cs string using presetToNdef
							if(blockFunc.notNil, {
								//(the block last played on this number is remembered: after
								//a stop nothing else says what the node map's times were for)
								if(blockFunc.isKindOf(Function), {
									var names = blockFunc.def.argNames ? [], last = (lastDefs ? ())[startKey];
									if(last.notNil, {
										Assemblage.unitTimesReset(startKey, last[0] != blockName, last[1]) });
									lastDefs = (lastDefs ?? { IdentityDictionary.new }).put(startKey, [blockName,
										names.select{|name| names.includes((name ++ "Lag").asSymbol)
											and: { names.includes((name ++ "Ramp").asSymbol) } }]);
								});
								ndefs[blockIndex].put(0, blockFunc, extraArgs: extraArgs);
								liveBlocks[blockIndex] = [blocks[blockIndex][0], blockName, bufferID, data];
								//document (step 2): the buffer by what it is, then the unit
								this.prSetBufferForm(startKey, blockName, bufferID ?? {
									if(buffer.isArray and: { [\alloc, \play, \cue].includes(buffer[0]) }, { buffer }) });
								RadUnit.sync(startKey);
								if(aZ.notNil, { aZ.updateSourceWin(ndefs[blockIndex].key); aZ.refreshSourceSlots });
							});
							action.();
						}, { cancelStart.value });
					});
				}.fork;
			}, {
				//a block that isn't there (removed, or never made) is made, then
				//played; a newer start for the same number while it is being
				//made replaces this one
				making = making ?? { IdentityDictionary.new };
				if(making[block].isNil, {
					making[block] = [blockName, buffer, extraArgs, data, sync, xfade, action];
					{
						var args;
						this.make(block);
						args = making.removeAt(block);
						this.play(block, *args);
					}.fork(AppClock);
				}, {
					making[block] = [blockName, buffer, extraArgs, data, sync, xfade, action];
				});
			});
		}, {
			"Block not found".warn;
		});
	}

	*stop {arg block=1, fadeOut;
		var blockIndex, slotIndex, ndefCS, ownFade, ndef, token;
		blockIndex = this.indexOfBlock(block);
		if(blockIndex.notNil, {
			ndef = ndefs[blockIndex];
			//a preset load still running on the block ends here, putting
			//the block's own fade time and lags back
			RadUnit.prStopBlockMorph(ndef.key);
			//a time typed is for this stop only: the block's own fade time
			//comes back when the fade is over (it used to keep the typed one).
			//A stop typed during another keeps the first one's own time.
			stopFades = stopFades ?? { IdentityDictionary.new };
			ownFade = stopFades[ndef.key] !? {|pending| pending[\own] } ?? { ndef.fadeTime };
			if(fadeOut.notNil, {
				ndefCS = (ndefs[blockIndex].cs ++ ".fadeTime = " ++
					fadeOut ++ ";");
				ndefCS.radcode;
			});
			fadeOut ?? {fadeOut = ndefs[blockIndex].fadeTime};
			// cancel a start still loading on this block (issue #51)
			this.newStartTicket(ndefs[blockIndex].key);
			ndefCS = (ndefs[blockIndex].cs	 ++ ".source = "
				++ "nil;" );
			ndefCS.radcode;
			RadUnit.sync(ndefs[blockIndex].key); //document (step 2)
			if(aZ.notNil, { aZ.closeSourceWin(ndefs[blockIndex].key); aZ.refreshSourceSlots });
			if(liveBlocks[blockIndex].notNil, {
				this.buffree(blockIndex, fadeOut, {|entry|
					//by name, and only if it still is what was stopped: the
					//block may have moved in the lists or been started again
					this.indexOfBlock(block) !? {|index|
						if(ndefs[index] === ndef and: { liveBlocks[index] === entry }, { liveBlocks[index] = nil }) };
				});
			});
			if(fadeOut != ownFade, {
				token = Object.new;
				stopFades[ndef.key] = (own: ownFade, token: token);
				{
					(fadeOut + (server.latency ? 0.2) + 0.1).wait;
					//the newest stop puts the own time back, unless the block
					//was given a fade time (fade N, fade blk N) or removed meanwhile
					if(stopFades !? {|fades| fades[ndef.key] } !? {|pending| pending[\token] === token } ? false, {
						stopFades.removeAt(ndef.key);
						if(ndefs.includes(ndef), { ndef.fadeTime = ownFade });
					});
				}.fork;
			});
		}, {
			"Block not found".warn;
		});

	}

	*playAll {arg blockArr, blockName, buffer, extraArgs, data, sync=false, xfade=false, action;
		var blockNameArr, bufferArr, extraArgsArr, dataArr, syncArr, xfadeArr, cond;
		cond = Condition(false);
		if(blockName.isString, { blockName = blockName.asSymbol });
		if(buffer.isString, { buffer = buffer.asSymbol });
		if(blockName.isArray, {blockNameArr = blockName}, {blockNameArr =
			blockName!blockArr.size});
		if(buffer.isArray, {bufferArr = buffer}, {bufferArr = buffer!blockArr.size});
		if(extraArgs.rank != 1, {extraArgsArr = extraArgs}, {extraArgsArr =
			extraArgs!blockArr.size});
		if(data.isArray, {
			if(data.rank != 1, {dataArr = data}, {dataArr = data!blockArr.size});
		}, {dataArr = data!blockArr.size});
		if(sync.isArray, {syncArr = sync}, {syncArr = sync!blockArr.size});
		if(xfade.isArray, {xfadeArr = xfade}, {xfadeArr = xfade!blockArr.size});
		{
			blockArr.do{|item, index|
				cond.test = false;
				this.play(item, blockNameArr[index], bufferArr[index], extraArgsArr[index],
					dataArr[index], syncArr[index], xfadeArr[index], {
						cond.test = true;
						cond.signal;
				});
				if(syncArr[index].not, {
					cond.wait;
				});
			};
			action.();
		}.fork;
	}

	*stopAll {arg fadeOut;
		fadeOut ?? {fadeOut = this.fadeTime};
		if(liveBlocks.notNil, {
			liveBlocks.do{|item|
				var index;
				if(item.notNil, {
					index = item[0].cs.replace("block", "").interpret.asInteger;
					this.stop(index, fadeOut);
				});
			};
		});
	}

	// a fade time given on purpose (fade N, fade blk N T) stays: a running
	// blk stop N T no longer puts the old one back
	*forgetStopFade {arg key;
		if(key.isNil, { stopFades = nil }, { stopFades !? {|fades| fades.removeAt(key) } });
	}

	*buffree {arg blockIndex=0, fadeOut, func;
		var thisBuffer, entry;
		//what the block was playing when it was stopped: if it has been
		//started again by the time the fade is over, that is another entry
		entry = liveBlocks[blockIndex];
		thisBuffer = entry[2];
		{
			var others;
			//the fade starts one server latency after it is asked for, so it
			//ends that much later: freeing the buffer at fadeOut cut its tail
			((fadeOut ? 0) + (server.latency ? 0.2) + 0.1).wait;
			if(thisBuffer.notNil, {
				//free it unless something else plays it: another block, or
				//this block started again with the same file
				others = (liveBlocks ? []).select{|item|
					item.notNil and: { item !== entry } and: { item[2] == thisBuffer } };
				if(others.isEmpty, {
					server.sync;
					if(thisBuffer.rank <= 1, {
						BStore.removeID(thisBuffer);
					}, {
						thisBuffer.do{|item| BStore.removeID(item);};
					});
				});
			});
			func.(entry);
		}.fork;
	}

	// time: for this move only (a parameter that is on travels over its own)
	*set {arg block, argArr, post=true, time;
		var blockIndex;
		blockIndex = this.indexOfBlock(block);
		if(blockIndex.notNil, {
			if(post == true, {
				Assemblage.unitSetCode(ndefs[blockIndex].key, argArr, time, true).radcode;
			});
		}, {
			"Block not found".warn;
		});
	}

	*xset {arg block, argArr, post=true;
		var blockIndex;
		blockIndex = this.indexOfBlock(block);
		if(blockIndex.notNil, {
			if(post == true, {
				(ndefs[blockIndex].cs	++ ".xset(" ++
					argArr.cs.replace("[", "").replace("]", "") ++  ");").radcode;
			});
		}, {
			"Block not found".warn;
		});
	}

	// the time for typed values of a block's parameters (names and times):
	// nothing is rebuilt, the times are controls
	*lag {arg block, argArr;
		var blockIndex;
		blockIndex = this.indexOfBlock(block);
		if(blockIndex.notNil, {
			//(a parameter by name or by number)
			argArr = argArr.asArray.clump(2).collect{|pair|
				[Assemblage.unitParamNames(ndefs[blockIndex].key, [pair[0]])[0], pair[1]] }
			.reject{|pair| pair[0].isNil }.flatten(1);
			Assemblage.unitLagSet(ndefs[blockIndex].key, argArr);
		}, {
			"Block not found".warn;
		});
	}

	// on, off, a time for the parameters that are on, exc, inc
	// (Assemblage.unitLagMode), with the blocks' lag time
	*lagMode {arg block, mode, list;
		var blockIndex = this.indexOfBlock(block);
		if(blockIndex.notNil, {
			Assemblage.unitLagMode(ndefs[blockIndex].key, mode, list, Radicles.blkLag);
		}, {
			"Block not found".warn;
		});
	}

	// times by position (the block's parameters in their order)
	*lags {arg block, times;
		var blockIndex = this.indexOfBlock(block), keys, pairs;
		if(blockIndex.notNil, {
			keys = Assemblage.paramKeys(ndefs[blockIndex]);
			pairs = times.asArray.collect{|time, index| if(time.isNumber and: { keys[index].notNil }, { [keys[index], time] }) }
			.reject(_.isNil);
			Assemblage.unitLagSet(ndefs[blockIndex].key, pairs.flat);
		}, {
			"Block not found".warn;
		});
	}

	// blklag N: the blocks' lag time, for every block parameter that is on,
	// and for what is switched on later
	*setLag {arg time;
		time = time.max(0);
		("Radicles.blkLag = " ++ time ++ ";").radcode;
		RadUnit.blockTags.do{|tag|
			var on = Assemblage.lagParams(Ndef(tag)).select{|key| Assemblage.unitLag(Ndef(tag), key) > 0 };
			if(on.notEmpty, { Assemblage.unitLagSet(tag, on.collect{|key| [key, time] }.flat) });
		};
	}

	*playNdefs {
		Server.default.waitForBoot{
			ndefs.do{|item| (item.cs ++ ".play;").radcode };
		};
	}

	//record
	*addRecNdefs {arg channels=1;
		var ndefTag, ndefCS1;
		ndefTag = ("recblock" ++ recBlockCount).asSymbol;
		recBlockCount = recBlockCount + 1;
		ndefCS1 = ("Ndef.ar(" ++ ndefTag.cs ++ ", " ++ channels.cs ++ ");");
		ndefCS1.radcode;
		recNdefs = recNdefs.add(Ndef(ndefTag));
		recBlocks = recBlocks.add( [ndefTag, channels] );
	}

	*getRecBStoreIDs {arg number=1, seconds=1, channels=1, format=\audio,
		frameSize=2048, hopSize=0.5;
		var buffer, numFrames, resultArr;
		number.do{
			if(format==\audio, {numFrames=seconds*Server.default.sampleRate;
				recBufInfo = recBufInfo.add([seconds, channels, format]);
			}, {
				numFrames=seconds.calcPVRecSize(frameSize, hopSize);
				recBufInfo = recBufInfo.add([seconds, channels, format, frameSize, hopSize]);
			});
			buffer = [(\alloc++BStore.allocCount).asSymbol, [numFrames, channels]];
			recbuffers = recbuffers.add([\alloc, buffer[0], buffer[1]]);
			resultArr = resultArr.add([\alloc, buffer[0], buffer[1]]);
			BStore.allocCount = BStore.allocCount + 1;
		};
		^resultArr;
	}

	*addRecNum {arg number=1, seconds=1, channels=1, format=\audio,
		frameSize=2048, hopSize=0.5, func;
		var bufferArr;
		number.do{
			this.addRecNdefs(channels);
		};
		bufferArr = this.getRecBStoreIDs(number, seconds, channels, format, frameSize, hopSize);
		BStore.addAll(bufferArr, {|bufArr|
			/*"//Record Buffers Allocated".radpost;*/
			func.(bufArr);
		});
	}

	*addRec {arg seconds=1, channels=1, format=\audio, frameSize=2048, hopSize=0.5, func;
		this.addRecNum(1, seconds, channels, format, frameSize, hopSize, func);
	}

	*addRecArr {arg argArr, func;
		var bufferArr, seconds, channels, format, frameSize, hopSize;
		argArr.do{|item|
			if(item[0].notNil, {seconds =  item[0]}, {seconds= 1});
			if(item[1].notNil, {channels =  item[1]}, {channels= 1});
			if(item[2].notNil, {format =  item[2]}, {format= \audio});
			if(item[3].notNil, {frameSize =  item[3]}, {frameSize = 2048});
			if(item[4].notNil, {hopSize =  item[4]}, {hopSize = 0.5});
			this.addRecNdefs(channels);
			bufferArr = bufferArr.add(this.getRecBStoreIDs(1, seconds, channels, format,
				frameSize,hopSize).unbubble);
		};
		BStore.addAll(bufferArr, {|bufArr|
			/*"//Record Buffers Allocated".postln;*/
			func.(bufArr);
		});
	}

	*removeRec {arg recbuf=1;
		var recindex, fadeTime;
		recindex = recbuf - 1;
		if((recbuf >= 1).and(recbuf <= recBlocks.size), {
			{
				fadeTime = recNdefs[recindex].fadeTime;
				recNdefs[recindex].clear;
				recNdefs.removeAt(recindex);
				recBlocks.removeAt(recindex);
				fadeTime.yield;
				BStore.removeID(recbuffers[recindex]);
				recbuffers.removeAt(recindex);
				recBufInfo.removeAt(recindex);
			}.fork;
		}, {
			"Record Buffer not Found".warn;
		});
	}

	*removeAllRec {
		var bstoreIndeces, fadeTime;
		{
			fadeTime = recNdefs[0].fadeTime;
			recNdefs.do{|item| item.clear };
			recNdefs = [];
			recBlocks = [];
			fadeTime.yield;
			recbuffers.do{|item|
				bstoreIndeces = bstoreIndeces.add(BStore.bstoreIDs.indexOfEqual(item));
			};
			bstoreIndeces.postln;
			BStore.removeIndices(bstoreIndeces);
			recbuffers = [];
			recBufInfo = [];
			recBlockCount = 1;
		}.fork;
	}

	*recBuf {arg bufnum=1;
		if(recbuffers.notNil, {
			^recbuffers[bufnum-1];
		}, {
			"No record buffers".postln;
		});
	}

	*rec {arg recblock=1, input=1, loop=0, recLevel, preLevel, xfade=false;
		var blockIndex, blockFunc, blockFuncCS, blockFuncString, bufString;
		var setRec, recBufData, inString, recType, bufIndex, bufIDs, setArg;
		if(recblock >= 1, {
			blockIndex = recblock-1;
			if((input.isNumber).or(input.isArray), {
				inString = "SoundIn.ar(" ++ (input-1).cs ++ ")";
			}, {
				inString = input.cs; //for ndefs
			});
			if(recNdefs.notNil, {
				if(recNdefs[blockIndex].notNil, {
					recBufData = recBufInfo[blockIndex];
					if(recBufData[2] == \audio, {
						if(loop == 0, {
							recType = \rec;
						}, {
							recType = \recloop;
						});
					}, {
						recType = \recpv;
					});
					blockFunc = SynthFile.read(\block, recType);
					blockFuncString = blockFunc.cs;
					bufString = BStore.buffStrByID(recbuffers[blockIndex]);
					blockFuncString = blockFuncString.replace("\\buffer",
						bufString).replace("'buffer'", bufString);
					blockFuncString = blockFuncString.replace("\\in", inString);
					blockFuncString = blockFuncString.replace("\\duration", recBufInfo[blockIndex][0]);
					blockFunc = blockFuncString.interpret;
					(recNdefs[blockIndex].cs.replace(")")	 ++ ", " ++
						blockFuncString ++ ");").radshow;
					//replace out with cs string using presetToNdef
					recNdefs[blockIndex].put(0, blockFunc);
					if(xfade, {
						setArg = ".xset";
					}, {
						setArg = ".set";
					});
					if(recLevel.notNil, {
						(recNdefs[blockIndex].cs ++ setArg ++ "(\\recLevel, " ++
							recLevel ++ ");").radcode;
					});
					if(preLevel.notNil, {
						(recNdefs[blockIndex].cs ++ setArg ++ "(\\preLevel, " ++
							preLevel ++ ");").radcode;
					});
				}, {
					"This recblock does not exist".warn;
				});
			}, {
				"No Recblocks active".warn;
			});
		}, {
			"Recblock not found".warn;
		});
	}

	*getRecBuf {arg recblock=1;
		^BStore.buffByID(Block.recBuf(recblock));
	}

	*recTimer {arg recblock=1, input=1, loop=0, recLevel, preLevel;
		timeInfo = [recblock, Main.elapsedTime];
		this.rec(recblock, input, loop, recLevel, preLevel, xfade: true);
	}

	*loopTimer {arg block=1;
		var elapsedTime, blockindex;
		(recNdefs[timeInfo[0]-1].cs.replace(")", "") ++ ", 0)").radcode;
		if(recBufInfo[timeInfo[0]-1][2] == \audio, {
			elapsedTime = (Main.elapsedTime - timeInfo[1]);
			blockindex = block-1;
			this.play(block, \looptr, Block.recBuf(timeInfo[0]),
				extraArgs: [\triggerRate, 1/elapsedTime]);
		}, {
			"loopTime only works with audio format".warn;
		});
	}

	*recNow {arg seconds=1, channels=1, format=\audio, input=1, loop=0,
		recLevel=1, preLevel=0, frameSize=2048, hopSize=0.5, timer=false;
		var recblock;
		this.addRec(seconds, channels, format, frameSize, hopSize, {|item|
			recblock = item[1].asString.last.asString.asInteger;
			if(timer, {
				this.recTimer(recblock, input, loop, recLevel, preLevel);
			}, {
				this.rec(recblock, input, loop, recLevel, preLevel);
			});
			("recording into recBlock: " ++ recblock).postln;
		});
	}

	*setFadeTime {arg newFadeTime;
		var string;
		newFadeTime ?? {newFadeTime = fadeTime};
		fadeTime = newFadeTime;
		ndefs.do{|item| string = (item.cs ++ ".fadeTime = " ++ fadeTime.cs ++ ";");
			string.radcode;
		};
	}

	*readWavetable {arg data, buf;
		var bufIndex, bufString;
		bufString = BufferSystem.getGlobVar(buf);
		/*bufIndex = BufferSystem.bufferArray.indexOf(buf);
		bufString = BufferSystem.globVarArray[bufIndex];*/
		(DataFile.read(\wavetables, data, excludeLibs).cs ++ ".(" ++
			bufString ++	");").radcode;
	}

	*blockPattern {arg block, extraArgs, data;
		var extraPattCount, blockFunc, pattArr;
		if(extraArgs.isArray.not, {
			blockFunc = this.pattData(DataFile.read(\pattern, extraArgs, excludeLibs), data)
			.toPattern(pattCount);
		}, {
			if(extraArgs.collect({|item| item.isArray}).includes(true), {
				blockFunc = this.pattData(extraArgs, data)
				.toPattern(pattCount);
				/*"this is a pattern defined".postln;*/
			}, {
				extraPattCount = 1;
				blockFunc = extraArgs.do{|item|
					pattArr = pattArr.add(
						this.pattData(DataFile.read(\pattern, item, excludeLibs), data)
						.toPattern(pattCount.cs ++ "_" ++ extraPattCount));
					extraPattCount = extraPattCount + 1;
				};
				blockFunc = Pdef(("'patt" ++ block ++ "'").interpret, Ppar(pattArr, 1));
				/*"this is a ppar".postln;*/
			});
		});
		pattCount = pattCount + 1;
		^blockFunc;
	}

	*pattData {arg pattern, data;
		var result, indexArr, inst, newData, patt;
		patt = pattern;
		if(data.notNil, {
			data.do({|item, index| if(item.isArray, {newData = newData.add(item);
			}, {
				if(index.even, {
					newData = newData.add([item, data[index+1]]);
				});
			});
			});
			newData.do{|item, index|
				if(item[0] == \instrument, {
					patt = patt.reject({|item| item[0] == \instrument});
				});
			};
			if(indexArr.notNil, {
				newData.removeAtAll(indexArr.flat);
			});
		});
		result = patt ++ newData;
		^result;
	}

	//what a block does with its buffer, read from its code: nil (no buffer),
	//\play (PlayBuf), \scpv (PV_PlayBuf), \cue (DiskIn), \vosc (VOsc),
	//\wavetable (Osc, COsc, Shaper) or \other
	*bufferKind {arg blockName;
		var func, string;
		func = SynthFile.read(\block, blockName.asSymbol, excludeLibs);
		if(func.isNil, {^nil});
		string = func.cs;
		if(string.includesString("\\buffer").not and: { string.includesString("'buffer'").not }, {^nil});
		^case
		{string.includesString("PV_PlayBuf")} {\scpv}
		{string.includesString("PlayBuf.ar(")} {\play}
		{string.includesString("DiskIn.ar(")} {\cue}
		{string.includesString("VOsc")} {\vosc}
		{["Osc.ar(\\buffer", "Osc.ar('buffer'", "Shaper.ar(\\buffer", "Shaper.ar('buffer'"]
			.any{|code| string.replace(" ", "").includesString(code) }} {\wavetable}
		{\other};
	}

	//the buffer (and wavetable data) for a block from what was typed after its
	//name: @N (a recording buffer), a wavetable name (a new wavetableSize
	//buffer filled with it), a loaded buffer's tag (alloc3, cue1), or a sound file
	*bufferArg {arg blockName, tag;
		var string, kind, id, tables;
		string = tag.asString;
		if(string[0] == $@, { ^[this.recBuf(string.divNumStr[1]), nil] });
		kind = this.bufferKind(blockName);
		if([\wavetable, \vosc].includes(kind), {
			tables = DataFile.read(\wavetables, exclude: excludeLibs) ? [];
			if(tables.includes(tag.asSymbol), { ^[wavetableSize, tag.asSymbol] });
		});
		id = (BStore.bstoreIDs ? []).detect{|item|
			[\alloc, \cue].includes(item[0]) and: { item[1] == tag.asSymbol } };
		if(id.notNil, { ^[id, nil] });
		^[tag, nil];
	}

	// a definition that streams a file from disk (cue) writes \fileRate where
	// it needs the file's sample rate, which only the file's header knows
	// (a cue buffer holds a few frames at the server's rate, so BufRateScale
	// is 1): it becomes the number, so the file plays at its own speed (6 Nov
	// 2026). Without a file it is the server's rate (speed 1).
	*prFileRateString {arg string, buf;
		var path, file, rate;
		if(string.includesString("\\fileRate").not and: { string.includesString("'fileRate'").not }, { ^string });
		path = buf !? {|b| b.path };
		if(path.notNil and: { File.exists(path.standardizePath) }, {
			file = SoundFile.openRead(path.standardizePath);
			file !? { rate = file.sampleRate; file.close };
		});
		rate = rate !? {|r| r.asInteger.asString } ?? { "SampleRate.ir" };
		^string.replace("\\fileRate", rate).replace("'fileRate'", rate);
	}

	// ---- the document (Document Model, step 2) ----

	// a block's buffer by what it is, never by its number (numbers change from
	// one run to the next): what Block.play needs to load it again.
	//   [\play, \audio, \drums1]   -> (play: 'drums1', folder: 0)
	//   [\cue, \cue1, \marilyn1]   -> (cue: 'marilyn1', folder: 0)
	//   [\alloc, \alloc2, [2048]]  -> (alloc: 2048)  (with channels: n if more than 1)
	//   a recording buffer         -> (rec: 1)       (@1)
	// folder: the Play folder (BStore.playFolder) the file was read from.
	// Several buffers (vosc) give an Array of these.
	*bufferForm {arg id, folder;
		var index, size;
		folder = folder ? BStore.playFolder;
		if(id.isArray.not or: { id.isEmpty }, { ^nil });
		if(id[0].isArray, { ^id.collect{|item| this.bufferForm(item, folder) } });
		^case
		{ id[0] == \play } { (play: id[2], folder: folder) }
		{ id[0] == \cue } { (cue: id[2], folder: folder) }
		{ id[0] == \alloc } {
			index = (recbuffers ? []).indexOfEqual(id);
			if(index.notNil, { (rec: index + 1) }, {
				size = id[2].asArray;
				if(size.size > 1 and: { size[1] != 1 }, {
					(alloc: size[0], channels: size[1]) }, { (alloc: size[0]) })
			})
		}
		{ nil };
	}

	// ---- position (step 2, PR 3; see RadPosition) ----

	// a block definition's code with \position replaced by the block's
	// control bus (made, and posted, the first time the block needs one)
	*prPositionString {arg key, string;
		var name;
		if(string.includesString("\\position").not and: { string.includesString("'position'").not }, { ^string });
		positionBuses = positionBuses ?? { IdentityDictionary.new };
		name = "~position" ++ key.asString.divNumStr[1];
		if(positionBuses[key].isNil or: { positionBuses[key].index.isNil }, {
			(name ++ " = Bus.control(s, 1);").radcode;
			positionBuses[key] = name.interpret;
		});
		^string.replace("\\position", name).replace("'position'", name);
	}

	//a server that quits or boots starts its bus numbers again: forget the
	//position buses, so a block never writes to a bus an Ndef owns now
	*initClass {
		Class.initClassTree(ServerQuit);
		Class.initClassTree(ServerBoot);
		ServerQuit.add(this);
		ServerBoot.add(this);
	}

	*doOnServerQuit {arg server; if(server === Server.default, { positionBuses = nil }) }

	*doOnServerBoot {arg server; if(server === Server.default, { positionBuses = nil }) }

	*prFreePositionBuses {
		positionBuses !? {|buses| buses.do{|bus| try { bus.free } } };
		positionBuses = nil;
	}

	// the control bus a block writes its position to, or nil
	*positionBus {arg block=1;
		^positionBuses !? {|buses| buses[("block" ++ block).asSymbol] }
	}

	// does block N play a definition that reports its position?
	*reportsPosition {arg block=1;
		var source = this.ndefOf(block) !? {|ndef| ndef.source };
		^source.isKindOf(Function) and: { source.def.sourceCode.asString.contains("RadPosition") }
		and: { this.positionBus(block).notNil }
	}

	// the buffer block N plays (the first, for several), or nil
	*bufferOf {arg block=1;
		var id = liveBlocks !? { this.indexOfBlock(block) } !? {|index| liveBlocks[index] } !? {|entry| entry[2] };
		if(id.isNil, { ^nil });
		if(id[0].isArray, { id = id[0] });
		^BStore.buffByID(id)
	}

	// where block N plays, in seconds of its file: action is called with the
	// position, or nil when the block reports none (asks the server)
	*position {arg block=1, action;
		if(this.reportsPosition(block).not, { action.(nil); ^nil });
		this.positionBus(block).get({|value| action.(value) });
	}

	// the startPos (0-1) that starts block N's file at seconds, or nil
	*startPosOf {arg block=1, seconds;
		var buf = this.bufferOf(block);
		if(buf.isNil or: { buf.duration.isNil } or: { buf.duration <= 0 }, { ^nil });
		^(seconds / buf.duration).clip(0, 1)
	}

	// remember the buffer of a block that just started, by what it is (the
	// Play folder can change later); blocks without a buffer have none
	*prSetBufferForm {arg key, blockName, id;
		liveBuffers = liveBuffers ?? { IdentityDictionary.new };
		liveBuffers[key] = if(blockName == \pattern, { nil }, { this.bufferForm(id) });
	}

	// what Block.play takes to load a buffer form again: a file's name, a
	// size to allocate, or a recording buffer (several for an Array)
	*bufferArgOf {arg form;
		if(form.isNil, { ^nil });
		if(form.isKindOf(Dictionary).not, { ^form.asArray.collect{|item| this.bufferArgOf(item) } });
		^case
		{ form[\play].notNil } { form[\play] }
		{ form[\cue].notNil } { form[\cue] }
		{ form[\rec].notNil } { this.recBuf(form[\rec]) }
		{ form[\alloc].notNil } { form[\alloc] }
		{ nil };
	}

	// the buffer form of a playing block (the unit's buffer), or nil
	*bufferFormOf {arg key;
		^liveBuffers !? {|b| b[key.asSymbol] } !? {|form| form.deepCopy }
	}

	//modulation
	*getSpec {arg blkNum, argIn;
		var ndefKey, spec, index, liveBlks;
		ndefKey = ("block" ++ blkNum).asSymbol;
		if(argIn.isNumber, {
			index = argIn-1;
		}, {
			index = Assemblage.paramKeys(Ndef(ndefKey)).indexOf(argIn)
		});
		if(index.notNil, {
			liveBlks = Block.liveBlocks;
			spec = SpecFile.read(\block,
				liveBlks.flop[1][liveBlks.flop[0].indexOf(ndefKey);]
			)[index][1];
			if(spec.isNil, {spec = [-1,1] });
			^spec;
		}, {
			"Argument doesn't match synth".warn;
		});
	}

	*modFunc {arg ndefKey, argIn, type, extraArgs, func,
		mul=1, add=0, min, val, warp, lag, thisSpec, modifier=\mod;
		var filterType, index, keyValues, spec, liveBlks;
		if(argIn.isNumber, {
			index = argIn-1;
		}, {
			index = Assemblage.paramKeys(Ndef(ndefKey)).indexOf(argIn)
		});
		if(index.notNil, {
			keyValues = Ndef(ndefKey).getKeysValues.select{|pair|
				Assemblage.paramKeys(Ndef(ndefKey)).includes(pair[0]) }[index];
			liveBlks = Block.liveBlocks;
			spec = SpecFile.read(\block,
				liveBlks.flop[1][liveBlks.flop[0].indexOf(ndefKey);]
			)[index][1];
			if(spec.isNil, {spec = [-1,1] });

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

	*modBlk {arg blkNum, modArg, modType, extraArgs,
		func, mul=1, add=0, min, val, warp, lag, thisSpec, modifier=\mod;
		var typeKey, ndefKey;
		ndefKey = ("block" ++ blkNum).asSymbol;
		if(modArg.notNil, {
			this.modFunc(ndefKey, modArg, modType, extraArgs, func,
				mul, add, min, val, warp, lag, thisSpec, modifier);
			if(aZ.notNil, { {server.sync; aZ.updateSourceWin(ndefKey)}.fork(AppClock) });
		}, {
			Assemblage.paramKeys(Ndef(ndefKey)).postln;
		});
	}

	*	unmodBlk {arg blkNum, modArg, value;
		var ndefKey, modifier;
		ndefKey = ("block" ++ blkNum).asSymbol;
		modArg = this.modArgNum(ndefKey, modArg);
		if(modArg.notNil, {
				{
					if((ModMap.modNodes.notNil).or(HIDMap.hidNodes.notNil), {
						modifier = this.unmodFunc(ndefKey, modArg);
						case
						{modifier == \mod} {
							value ?? {
								value = ModMap.modNodes.select({|item|
									(item[1] == Ndef(ndefKey) ).and(item[2] == Assemblage.paramKeys(Ndef(ndefKey))[modArg-1]);
								})[0].last;
							};
							ModMap.unmap(Ndef(ndefKey), modArg-1, value);
						}
						{modifier == \hid} {
							value ?? {
								value = HIDMap.hidNodes.select({|item|
									(item[1] == Ndef(ndefKey) ).and(item[2] == Assemblage.paramKeys(Ndef(ndefKey))[modArg-1]);
								})[0].last;
							};
							HIDMap.unmap(Ndef(ndefKey), modArg-1, value);
						};
						server.sync;
						if(aZ.notNil, { aZ.updateSourceWin(ndefKey) });
					}, {
						"no mod in this argument".warn;
					});
				}.fork(AppClock);
			});
	}

	//argument number (from 1) for a number or a name such as \freq
	*modArgNum {arg ndefKey, modArg;
		var index;
		if(modArg.isNil or: {modArg.isInteger}, {^modArg});
		index = Assemblage.paramKeys(Ndef(ndefKey)).indexOf(modArg.asSymbol);
		if(index.isNil, {
			("argument not found: " ++ modArg).warn;
			^nil;
		});
		^(index + 1);
	}

	*unmodFunc {arg ndefKey, modArg;
		var modifier;
		if(modArg.isInteger, {
			modArg = Assemblage.paramKeys(Ndef(ndefKey))[modArg-1];
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

	*findBlkModNdef {arg blkNum, trackArg;
		var blkTag, findMods, findModArr, modIndex, modKey, modInfo, result;
		blkTag = ("block" ++ blkNum).asSymbol;
		if(blkTag.notNil, {
			modInfo = ModMap.modNodes;
			if(modInfo.notNil, {
				findMods = ModMap.modNodes.flop[1].indicesOfEqual(Ndef(blkTag));
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
					"no modulation in this block".warn;
				});
			}, {
				"no modulation in this block".warn;
			});
		}, {
			"block doesn't exist".warn;
		});
		^result;
	}

	*setBlkMod {arg blkNum, trackArg, extraArgs;
		var ndefString;
		ndefString = this.findBlkModNdef(blkNum, trackArg);
		if(ndefString.notNil, {
			(ndefString.cs ++ ".setn" ++ extraArgs.cs.replaceAt("(",0)
				.replaceAt(")", extraArgs.cs.size-1)).radcode;
		});
	}

	*getBlkMod {arg blkNum, trackArg;
		var ndefString;
		ndefString = this.findBlkModNdef(blkNum, trackArg);
		if(ndefString.notNil, {
			ndefString.controlKeysValues.radpost;
		});
	}

}