/*getting there. still troubleshoot buffer arrays when the array has files that are already playing. also look at simultaneaus evaluation of blocks - particularly as they have the same buffer file*/

Block : Radicles {classvar <blocks, <ndefs, <liveBlocks, <blockCount=1,
	<recbuffers, <recNdefs, <recBlocks, <recBlockCount=1, <recBufInfo, timeInfo,
	<pattCount=1, <timecond;

	*add {arg channels=1;
		var ndefTag, ndefCS1, ndefCS2;
		ndefTag = ("block" ++ blockCount).asSymbol;
		blockCount = blockCount + 1;
		ndefCS1 = "Ndef.ar(";
		ndefCS1 = (ndefCS1 ++ ndefTag.cs ++ ", " ++ channels.cs ++ ");");
		ndefCS1.radpost.interpret;
		ndefCS2 = ("Ndef(" ++ ndefTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
		ndefCS2.radpost.interpret;
		ndefs = ndefs.add(Ndef(ndefTag));
		blocks = blocks.add( [ndefTag, channels] );
		liveBlocks = liveBlocks.add(nil);
		timecond = timecond.add(Condition(false));
	}

	*addNum {arg number, channels=1;
		var thisChan;
		number.do{|index|
			if(channels.isArray, {thisChan = channels[index]}, {thisChan = channels});
			this.add(thisChan);
		};
	}

	*addAll {arg arr;
		arr.do{|item|
			this.add(item);
		}
	}

	*remove {arg block=1;
		var blockIndex, string;
		blockIndex = block - 1;
		if((block >= 1).and(block <= blocks.size), {
			string = (ndefs[blockIndex].cs ++ ".clear;");
			string.radpost.interpret;
			ndefs.removeAt(blockIndex);
			blocks.removeAt(blockIndex);
			liveBlocks.removeAt(blockIndex);
			timecond.removeAt(blockIndex);
		}, {
			"Block Number not Found".warn;
		});
	}

	*removeArr {arg blockArr;
		var newArr, string;
		blockArr.do{|item|
			if((item >= 1).and(item <= blocks.size), {
				newArr = newArr.add(item-1);
				string = (ndefs[item-1].cs ++ ".clear;");
				string.radpost.interpret;
			}, {
				"Block Number not Found".warn;
			});
		};
		ndefs.removeAtAll(newArr);
		blocks.removeAtAll(newArr);
		liveBlocks.removeAll(newArr);
		timecond.removeAll(newArr);
	}

	*removeAll {var string;
		ndefs.do{|item|
			string = (item.cs ++ ".clear;");
			string.radpost.interpret;
		};
		ndefs = [];
		blocks = [];
		liveBlocks = [];
		timecond = [];
		blockCount = 1;
	}

	*clear {
		"Ndef.clear;".radpost.interpret;
		ndefs = [];
		blocks = [];
		liveBlocks = [];
		timecond = [];
		blockCount = 1;
	}

	*initCond {arg block=1;
		timecond[block-1] = Condition(false);
		timecond[block-1].test = false;
	}

	*syncStart {arg block=1;
		timecond[block-1].test = true;
		timecond[block-1].signal;
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

	*play {arg block=1, blockName, buffer, extraArgs, data, sync=false, xfade=true, action;
		var blockFunc, blockIndex, newArgs, ndefCS, blockFuncString,
		storeType, dataString, cond, bufferArr, bufferID, bufInfo, bstoreSize,
		pattArr, extraPattCount, bufIndex, bufString, bufIDs;
		if(block >= 1, {
			blockIndex = block-1;
			if(ndefs[blockIndex].notNil, {
				{
					this.initCond(block);
					if((blockName == 'pattern').not, {
						blockFunc = SynthFile.read(\block, blockName);
						blockFuncString = blockFunc.cs;
						if(blockFuncString.includesString("\\buffer")
							.or(blockFuncString.includesString("'buffer'")), {
								if(buffer.isArray.not, {
									bufferArr = BStore.setBufferID(buffer, blockFuncString);
									storeType = bufferArr[0];
									bufInfo = bufferArr[1];
									bufferID = bufferArr[2];
									if(bufInfo.notNil.or(bufInfo == \nobuf), {
										server.sync;
										BStore.add(storeType, bufInfo, {arg buf;
											bufIndex = BufferSystem.bufferArray.indexOf(buf);
											bufString = BufferSystem.globVarArray[bufIndex];
											blockFunc = blockFuncString.replace("\\buffer",
												bufString).replace("'buffer'", bufString).interpret;
											if(data.notNil, {
												this.readWavetable(data, buf);
											});
										});
										server.sync;
										/*cond.wait;*/
									}, {
										"Buffer not provided".warn;
									});
								}, {
									if([\alloc, \play, \cue].includes(buffer[0]), {
										bufIDs = BStore.buffByID(buffer);
										bufIndex = BufferSystem.bufferArray.indexOf(bufIDs);
										bufString = BufferSystem.globVarArray[bufIndex];
										blockFunc = blockFuncString.replace("\\buffer",
											bufString).replace("'buffer'", bufString).interpret;
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
											bufferID.do{|item|
												bufIDs = BStore.buffByID(item);
												bufIndex = BufferSystem.bufferArray.indexOf(bufIDs);
												bufString = bufString.add(BufferSystem.globVarArray[bufIndex]);
											};
											blockFunc = blockFunc.cs.replace(("\\buffer"),
												bufString).replace(("'buffer'"), bufString).interpret;
											cond.test = true;
											cond.signal;
											//fill buffer with wavetable function
											if(data.notNil, {
												if(data.isArray, {
													/*	"this data is an array".postln;*/
													data.do{|item, index|
														(DataFile.read(\wavetables, item).cs ++ ".(" ++
															bufString[index].cs ++	");").radpost;
														DataFile.read(\wavetables, item).(buf[index]);
													};
												}, {
													/*"this data is a symbol".postln;*/
													buf.do{|item, index|
														(DataFile.read(\wavetables, data).cs ++ ".(" ++
															bufString[index] ++ "," ++ (buf.size+1) ++ ", " ++
															index ++ ");").radpost;
														DataFile.read(\wavetables, data).(item, buf.size+1, index);
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
							blockFunc = this.pattData(DataFile.read(\pattern, extraArgs), data)
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
										this.pattData(DataFile.read(\pattern, item), data)
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
						timecond[block-1].wait;
					});
					if(liveBlocks[blockIndex].notNil, {
						if((liveBlocks[blockIndex][2] == bufferID).not, {
							/*"free buffer from play".postln;*/
							this.buffree(blockIndex, ndefs[blockIndex].fadeTime*2);
						});
					});
					ndefCS = (ndefs[blockIndex].cs.replace(")")	++ ", "
						++ blockFunc.cs ++ ");");
					ndefCS.radpost;
					if((blockName == 'pattern').not, {
						if(extraArgs.notNil, {
							if(extraArgs.collect{|item| item.isSymbol}.includes(true), {
								newArgs = extraArgs;
							}, {
								newArgs = [blockFunc.argNames, extraArgs].flop.flat;
							});
							if(xfade, {
								this.xset(block, newArgs);
							}, {
							this.set(block, newArgs);
							});
						});
					}, {
						/*"no pattern before".postln;*/
						if(liveBlocks[blockIndex].notNil, {
							if((liveBlocks[blockIndex][1] != \pattern), {
								ndefs[blockIndex].put(0, nil);
								fadeTime.wait;
								ndefs[blockIndex].resetNodeMap;
							});
						});
					});
					server.sync;
					ndefs[blockIndex].put(0, blockFunc, extraArgs: newArgs);
					liveBlocks[blockIndex] = [blocks[blockIndex][0], blockName, bufferID, data];
					action.();
				}.fork;
			}, {
				"This block does not exist".warn;
			});
		}, {
			"Block not found".warn;
		});
	}

	*stop {arg block=1, fadeOut;
		var blockIndex, slotIndex, ndefCS;
		if(block >= 1, {
			blockIndex = block-1;
			if(fadeOut.notNil, {
				ndefCS = (ndefs[blockIndex].cs ++ ".fadeTime = " ++
					fadeOut ++ ";");
				ndefCS.radpost.interpret;
			});
			fadeOut ?? {fadeOut = ndefs[blockIndex].fadeTime};
			ndefCS = (ndefs[blockIndex].cs	 ++ ".source = "
				++ "nil;" );
			ndefCS.radpost.interpret;
			if(liveBlocks[blockIndex].notNil, {
				this.buffree(blockIndex, fadeOut, {
					liveBlocks[blockIndex] = nil;
					ndefs[blockIndex].fadeTime = fadeTime;
				});
			});
			ndefs[blockIndex].fadeTime = fadeOut;
		}, {
			"Block not found".warn;
		});

	}

	*playAll {arg blockArr, blockName, buffer, extraArgs, data, sync=false, xfade=false, action;
		var blockNameArr, bufferArr, extraArgsArr, dataArr, syncArr, xfadeArr, cond;
		cond = Condition(false);
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
				/*[item, blockNameArr[index], bufferArr[index], extraArgsArr[index],
					dataArr[index], syncArr[index], xfade[index] ].postln;*/
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

	*buffree {arg blockIndex=0, fadeOut, func;
		var thisBuffer, thisBlock;
		thisBlock = liveBlocks;
		thisBuffer = thisBlock[blockIndex][2];
		{
			fadeOut.wait;
			if(thisBuffer.notNil, {
				if((thisBlock.flop[2].indicesOfEqual(thisBuffer).size > 1).not, {
					/*"remove buffer".postln;*/
					server.sync;
					/*this.nodeTime.wait;*/
					if(thisBuffer.rank <= 1, {
						BStore.removeID(thisBuffer);
					}, {
						thisBuffer.do{|item| BStore.removeID(item);};
					});
				});
			});
			func.();
		}.fork;
	}

	*set {arg block, argArr, post=true;
		var blockIndex;
		if((block >= 1), {
			blockIndex = block-1;
			if(post == true, {
				(ndefs[blockIndex].cs	++ ".set(" ++
					argArr.cs.replace("[", "").replace("]", "") ++  ");").radpost.interpret;
			});
		}, {
			"Block not found".warn;
		});
	}

	*xset {arg block, argArr, post=true;
		var blockIndex;
		if((block >= 1), {
			blockIndex = block-1;
			if(post == true, {
				(ndefs[blockIndex].cs	++ ".xset(" ++
					argArr.cs.replace("[", "").replace("]", "") ++  ");").radpost.interpret;
			});
		}, {
			"Block not found".warn;
		});
	}

	*lag {arg block, argArr;
		var blockIndex, string;
		if((block >= 1), {
			blockIndex = block-1;
			argArr.keysValuesDo{|key, val|
				string = ndefs[blockIndex].cs ++ ".lag(" ++ key.cs
				++ ", " ++ val.cs ++ ");";
				string.radpost.interpret;
			};
		}, {
			"Block not found".warn;
		});
	}

	*playNdefs {
		Server.default.waitForBoot{
			ndefs.do{|item| (item.cs ++ ".play;").radpost.interpret };
		};
	}

	//record
	*addRecNdefs {arg channels=1;
		var ndefTag, ndefCS1;
		ndefTag = ("recblock" ++ recBlockCount).asSymbol;
		recBlockCount = recBlockCount + 1;
		ndefCS1 = ("Ndef.ar(" ++ ndefTag.cs ++ ", " ++ channels.cs ++ ");");
		ndefCS1.radpost.interpret;
		recNdefs = recNdefs.add(Ndef(ndefTag));
		recBlocks = recBlocks.add( [ndefTag, channels] );
	}

	*getRecBStoreIDs {arg number=1, seconds=1, channels=1, format=\audio, frameSize=2048,
		hopSize=0.5;
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

	*addRecNum {arg number=1, seconds=1, channels=1, format=\audio, frameSize=2048,
		hopSize=0.5, func;
		var bufferArr;
		number.do{
			this.addRecNdefs(channels);
		};
		bufferArr = this.getRecBStoreIDs(number, seconds, channels, format, frameSize, hopSize);
		BStore.addAll(bufferArr, {|bufArr|
			"Record Buffers Allocated".postln;
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
			"Record Buffers Allocated".postln;
			func.(bufArr);
		});
	}

	*removeRec {arg recbuf=1;
		var recindex, fadeTime;
		recindex = recbuf - 1;
		if((recbuf >= 1).and(recbuf <= recBlocks.size), {
			{
				fadeTime = recNdefs[recindex].fadeTime.postln;
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

	*rec {arg recblock=1, input=1, loop=0, recLevel, preLevel;
		var blockIndex, blockFunc, blockFuncCS, blockFuncString, argString;
		var setRec, recBufData, inString, recType;
		if(recblock >= 1, {
			blockIndex = recblock-1;
			if((input.isNumber).or(input.isArray), {
				inString = "SoundIn.ar(" ++ (input-1).cs ++ ")";
			}, {
				inString = input.cs;
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
					blockFunc = SynthFile.string(\block, recType);
					blockFunc = blockFunc.replace("\\in", inString);
					blockFunc = blockFunc.interpret;
					blockFuncCS = blockFunc;
					blockFuncString = blockFunc.cs;
					if(recBufData[2] == \audio, {
						blockFunc = blockFunc.(BStore.buffByID(recbuffers[blockIndex]), recBufData[1]);
					}, {
						blockFunc = blockFunc.(BStore.buffByID(recbuffers[blockIndex]), recBufData[1],
							recBufData[3], recBufData[4]);
					});
					if(recLevel.notNil, {
						argString = argString.add(".set('recLevel', " ++ recLevel.cs ++ ");");
					});
					if(preLevel.notNil, {
						argString = argString.add(".set('preLevel', " ++ preLevel.cs ++ ");");
					});
					(recNdefs[blockIndex].cs.replace(")")	 ++ ", " ++ blockFuncString ++ ");").radpost;
					recNdefs[blockIndex].source = blockFunc;
					if(argString.notNil,  {
						argString.do{|item|
							setRec = (recNdefs[blockIndex].cs ++ item);
							setRec.radpost.interpret;
						};
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
		this.rec(recblock, input, loop, recLevel, preLevel);
	}

	*loopTimer {arg block=1;
		var elapsedTime, blockindex;
		(recNdefs[timeInfo[0]-1].cs.replace(")", "") ++ ", 0)").radpost.interpret;
		if(recBufInfo[timeInfo[0]-1][2] == \audio, {
			elapsedTime = (Main.elapsedTime - timeInfo[1]);
			blockindex = block-1;
			this.play(block, \looptr, Block.recBuf(timeInfo[0]), extraArgs: [\triggerRate, 1/elapsedTime]);
		}, {
			"loopTime only works with audio format".warn;
		});
	}

	*recNow {arg seconds=1, channels=1, format=\audio, input=1, loop=0, recLevel=1,
		preLevel=0, frameSize=2048, hopSize=0.5, timer=false;
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
			string.radpost.interpret;
		};
	}

	*readWavetable {arg data, buf;
		var bufIndex, bufString;
		bufIndex = BufferSystem.bufferArray.indexOf(buf);
		bufString = BufferSystem.globVarArray[bufIndex];
		(DataFile.read(\wavetables, data).cs ++ ".(" ++
			bufString ++	");").radpost.interpret;
	}

	*blockPattern {arg block, extraArgs, data;
		var extraPattCount, blockFunc, pattArr;
		if(extraArgs.isArray.not, {
			blockFunc = this.pattData(DataFile.read(\pattern, extraArgs), data)
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
						this.pattData(DataFile.read(\pattern, item), data)
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

}