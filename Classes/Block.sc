Block : MainImprov {classvar <blocks, <ndefs, <liveBlocks, <blockCount=1, cueCount=1, allocCount=1,
	<recbuffers, <recNdefs, <recBlocks, <recBlockCount=1, <recBufInfo, timeInfo;

	*add {arg type=\audio, channels=1, destination;
		var ndefTag, ndefCS1, ndefCS2;
		if((type == \audio).or(type == \control), {
			ndefTag = ("block" ++ blockCount).asSymbol;
			blockCount = blockCount + 1;
			if(type == \audio, {
				ndefCS1 = "Ndef.ar(";
			}, {
				ndefCS1 = "Ndef.kr(";
			});
			ndefCS1 = (ndefCS1 ++ ndefTag.cs ++ ", " ++ channels.cs ++ ");");
			ndefCS1.postln;
			ndefCS1.interpret;
			ndefCS2 = ("Ndef(" ++ ndefTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
			ndefCS2.postln;
			ndefCS2.interpret;
			ndefs = ndefs.add(Ndef(ndefTag));
			blocks = blocks.add( [ndefTag, type, channels, destination] );
			liveBlocks = liveBlocks.add(nil);
		}, {
			"Block Ndef rate not found".warn;
		});
	}

	*addNum {arg number, type, channels, destinations;
		var thisType, thisChan, thisDest;
		number.do{|index|
			if(type.isArray, {thisType = type[index]}, {thisType = type});
			if(type.isArray, {thisChan = channels[index]}, {thisChan = channels});
			if(type.isArray, {thisDest = destinations[index]}, {thisDest = destinations});
			this.add(thisType, thisChan, thisDest);
		};
	}

	*addAll {arg arr;
		arr.do{|item|
			this.add(item[0], item[1], item[2]);
		}
	}

	*remove {arg block=1;
		var blockIndex;
		blockIndex = block - 1;
		if((block >= 1).and(block <= blocks.size), {
			/*ndefs[blockIndex].free;*/
			ndefs[blockIndex].clear;
			ndefs.removeAt(blockIndex);
			blocks.removeAt(blockIndex);
			liveBlocks.removeAt(blockIndex);
		}, {
			"Block Number not Found".warn;
		});
	}

	*removeArr {arg blockArr;
		var newArr;
		blockArr.do{|item|
			if((item >= 1).and(item <= blocks.size), {
				newArr = newArr.add(item-1);
				/*ndefs[item-1].free;*/
				ndefs[item-1].clear;
			}, {
				"Block Number not Found".warn;
			});
		};
		ndefs.removeAtAll(newArr);
		blocks.removeAtAll(newArr);
		liveBlocks.removeAll(newArr);
	}

	*removeAll {
		ndefs.do{|item| item.clear };
		ndefs = [];
		blocks = [];
		liveBlocks = [];
	}

	*clear {
		Ndef.clear;
		ndefs = [];
		blocks = [];
		liveBlocks = [];
	}

	*setBufferID {arg buffer, blockFuncString;
		var storeType, bufferID;
		case
		{buffer.isNumber} {
			storeType = \alloc;
			buffer = [(\alloc++allocCount).asSymbol, buffer];
			bufferID = [storeType, buffer[0], [buffer[1]] ];
			allocCount = allocCount + 1;
		}
		{buffer.isSymbol} {
			case
			{(blockFuncString.find("PlayBuf.ar(")).notNil} {
				storeType = \play;
				BStore.playFormat = \audio;
				bufferID = [storeType, \audio, buffer].flat;
			}
			{blockFuncString.find("PV_PlayBuf").notNil} {
				storeType = \play;
				BStore.playFormat = \scpv;
				bufferID = [storeType, \scpv, buffer];
			}
			{blockFuncString.find("DiskIn.ar(").notNil} {
				storeType = \cue;
				buffer = [(\cue++cueCount).asSymbol, buffer].flat;
				bufferID = [storeType, buffer].flat;
				cueCount = cueCount + 1;
			};
		};
		^[storeType, buffer, bufferID];
	}

	*play {arg block=1, blockName, buffer, extraArgs, data;
		var blockFunc, blockIndex, newArgs, ndefCS, blockFuncCS, blockFuncString;
		var storeType, dataString, cond, bufferArr, bufferID, bufInfo;
		if(block >= 1, {
			blockIndex = block-1;

			if(ndefs[blockIndex].notNil, {

				{
					blockFunc = SynthFile.read(\block, blockName);
					blockFuncCS = blockFunc;
					blockFuncString = blockFunc.cs;

					if(blockFuncString.findAll("{").size == 2, {
						if(buffer.isArray.not, {
							bufferArr = this.setBufferID(buffer, blockFuncString);
							storeType = bufferArr[0];
							bufInfo = bufferArr[1];
							bufferID = bufferArr[2];

							"includes buffer".postln;
							cond = Condition(false);
							cond.test = false;
							if(bufInfo.notNil.or(bufInfo == \nobuf), {
								BStore.add(storeType, bufInfo.postln, {arg buf;
									blockFunc = blockFunc.(buf);
									cond.test = true;
									cond.signal;
									//fill buffer with wavetable function
									if(data.notNil, {
										DataFile.read(\wavetables, data).cs.postln;
										DataFile.read(\wavetables, data).(buf);
									});

								});
								cond.wait;
							}, {
								"Buffer not provided".warn;
							});

						}, {

							if([\alloc, \play, \cue].includes(buffer[0]), {
								blockFunc = blockFunc.(BStore.buffByID(buffer));
								"this buffer is an existing buffer with ID".postln;
							}, {

								buffer.do{|item|
									bufferArr = bufferArr.add(this.setBufferID(item, blockFuncString));
								};

								bufferArr.postln;
								storeType = bufferArr.flop[0];
								bufInfo = bufferArr.flop[1];
								bufferID = bufferArr.flop[2];

								"includes buffer array".postln;
								cond = Condition(false);
								cond.test = false;

								BStore.addAll(bufferID, {arg buf;

									blockFunc = blockFunc.(buf);
									cond.test = true;
									cond.signal;
									//fill buffer with wavetable function
									if(data.notNil, {
										if(data.isArray, {
											"this data is an array".postln;
										data.do{|item, index|
											DataFile.read(\wavetables, item).cs.postln;
											DataFile.read(\wavetables, item).(buf[index]);
										};
										}, {
												"this data is a symbol".postln;
											DataFile.read(\wavetables, data).cs.postln;
											buf.do{|item, index|
												[item, buf.size+1, index].postln;
											DataFile.read(\wavetables, data).(item, buf.size+1, index);
											};
										});
									});

								});
								cond.wait;

								"this buffer array that need to be allocated".postln;
							});
						});
					}, {
						"no buffer".postln;
					});

					if(liveBlocks[blockIndex].notNil, {
						if((liveBlocks[blockIndex][2] == bufferID).not, {
							"free buffer from play".postln;
							this.buffree(blockIndex, ndefs[blockIndex].fadeTime*2);
						});
					});

					ndefCS = (ndefs[blockIndex].cs.replace(")")	++ ", "
						++ blockFuncCS.cs ++ ");");

					ndefCS.postln;

					if(extraArgs.notNil, {
						if(extraArgs.collect{|item| item.isSymbol}.includes(true), {
							newArgs = extraArgs;
						}, {
							newArgs = [blockFunc.argNames, extraArgs].flop.flat;
						});
						this.xset(block, newArgs, true);
					});

										ndefs[blockIndex].put(0, blockFunc, extraArgs: newArgs);

					liveBlocks[blockIndex] = [blocks[blockIndex][0], blockName, bufferID, data];

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

			fadeOut ?? {fadeOut = ndefs[blockIndex].fadeTime};

			ndefCS = (ndefs[blockIndex].cs	 ++ ".source = "
				++ "nil;" );
			ndefCS.interpret;
			ndefCS.postln;

			this.buffree(blockIndex, fadeOut, {
				liveBlocks[blockIndex] = nil;
				ndefs[blockIndex].fadeTime = fadeTime;
			});

			ndefs[blockIndex].fadeTime = fadeOut;

		}, {
			"Block not found".warn;
		});

	}

	*buffree {arg blockIndex=0, fadeOut, func;
		var thisBuffer, thisBlock;
		thisBlock = liveBlocks;
		thisBuffer = thisBlock[blockIndex][2].postln;
		{
			fadeOut.wait;
			if(thisBuffer.notNil, {
				if((thisBlock.flop[2].indicesOfEqual(thisBuffer).size > 1).not, {
					"remove buffer".postln;
					this.nodeTime.wait;
					BStore.removeID(thisBuffer);
				});
			});
			func.();
		}.fork;
	}

	*set {arg block, argArr, post=false;
		var blockIndex;
		if((block >= 1), {
			blockIndex = block-1;
			if(post == true, {
				(ndefs[blockIndex].cs	++ ".set(" ++
					argArr.cs.replace("[", "").replace("]", "") ++  ");").postln;
			});
			argArr.keysValuesDo{|key, val|
				ndefs[blockIndex].set(key, val);
			};
		}, {
			"Block not found".warn;
		});
	}

	*xset {arg block, argArr, post=false;
		var blockIndex;
		if((block >= 1), {
			blockIndex = block-1;
			if(post == true, {
				(ndefs[blockIndex].cs	++ ".xset(" ++
					argArr.cs.replace("[", "").replace("]", "") ++  ");").postln;
			});
			argArr.keysValuesDo{|key, val|
				ndefs[blockIndex].xset(key, val);
			};
		}, {
			"Block not found".warn;
		});
	}

	*lag {arg block, argArr;
		var blockIndex;
		if((block >= 1), {
			blockIndex = block-1;
			argArr.keysValuesDo{|key, val|
				ndefs[blockIndex].lag(key, val);
			};
		}, {
			"Block not found".warn;
		});
	}

	*playNdefs {
		Server.default.waitForBoot{
			ndefs.do{|item| item.play};
		};
	}

	//record
	*addRecNdefs {arg channels=1;
		var ndefTag, ndefCS1;
		ndefTag = ("recblock" ++ recBlockCount).asSymbol;
		recBlockCount = recBlockCount + 1;
		ndefCS1 = ("Ndef.ar(" ++ ndefTag.cs ++ ", " ++ channels.cs ++ ");");
		ndefCS1.postln;
		ndefCS1.interpret;
		recNdefs = recNdefs.add(Ndef(ndefTag));
		recBlocks = recBlocks.add( [ndefTag, channels] );
	}

	*getRecBStoreIDs {arg number=1, seconds=1, channels=1, format=\audio, frameSize=2048, hopSize=0.5;
		var buffer, numFrames, resultArr;
		number.do{
			if(format==\audio, {numFrames=seconds*Server.default.sampleRate;
				recBufInfo = recBufInfo.add([seconds, channels, format]);
			}, {
				numFrames=seconds.calcPVRecSize(frameSize, hopSize);
				recBufInfo = recBufInfo.add([seconds, channels, format, frameSize, hopSize]);
			});
			buffer = [(\alloc++allocCount).asSymbol, [numFrames, channels]];
			recbuffers = recbuffers.add([\alloc, buffer[0], buffer[1]]);
			resultArr = resultArr.add([\alloc, buffer[0], buffer[1]]);
			allocCount = allocCount + 1;
		};
		^resultArr;
	}

	*addRecNum {arg number=1, seconds=1, channels=1, format=\audio, frameSize=2048, hopSize=0.5, func;
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
		BStore.addAll(bufferArr.postln, {|bufArr|
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
					blockFunc = blockFunc.replace("'input'", inString);
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
					(recNdefs[blockIndex].cs.replace(")")	 ++ ", " ++ blockFuncString ++ ");").postln;
					recNdefs[blockIndex].source = blockFunc;
					if(argString.notNil,  {
						argString.do{|item|
							setRec = (recNdefs[blockIndex].cs ++ item);
							setRec.interpret;
							setRec.postln;
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
		(recNdefs[timeInfo[0]-1].cs.replace(")", "") ++ ", 0)").postln.interpret;
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

}