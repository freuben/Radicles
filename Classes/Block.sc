Block : MainImprov {classvar <blocks, <ndefs, <liveBlocks, <blockCount=1, fadeTime=0.5, cueCount=1, allocCount=1, <recbuffers, <recNdefs, <recBlocks, <recBlockCount=1;

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

	*play {arg block=1, blockName, buffer, isPattern=false, extraArgs, data;
		var blockFunc, blockIndex, newArgs, ndefCS, cond, blockFuncCS, bufferID, bufTag;
		var storeType, blockFuncString, fadeWait=0;
		if(block >= 1, {
			blockIndex = block-1;

			if(ndefs[blockIndex].notNil, {

				{
					blockFunc = SynthFile.read(\block, blockName);
					blockFuncCS = blockFunc;
					blockFuncString = blockFunc.cs;
					bufTag = buffer;

					case
					{buffer.isNumber} {
						storeType = \alloc;
						"alloc".postln;
						buffer = [(\alloc++allocCount).asSymbol, buffer].postln;
						bufferID = [storeType, buffer ].flat;
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
							buffer = [(\cue++cueCount).asSymbol, buffer];
							bufferID = [storeType, buffer].flat;
							cueCount = cueCount + 1;
						};
					};

					if(liveBlocks[blockIndex].notNil, {
						fadeWait = ndefs[blockIndex].fadeTime * 2;

						if((liveBlocks[blockIndex][2] == bufferID).not, {
							"free buffer from play".postln;
							this.buffree(blockIndex, ndefs[blockIndex].fadeTime*2);
						});
					});

					if(blockFuncString.findAll("{").size == 2, {

						if(bufTag.isArray.not, {
							"includes buffer".postln;
							cond = Condition(false);
							cond.test = false;
							if(buffer.notNil.or(buffer == \nobuf), {
								BStore.add(storeType, buffer, {arg buf;
									blockFunc = blockFunc.(buf);
									cond.test = true;
									cond.signal;
								});
								cond.wait;
							}, {
								"Buffer not provided".warn;
							});
						}, {
							//already allocated buffer
							/*bufferID = bufTag;
							bufTag.postln;*/
							blockFunc = blockFunc.(BStore.buffByTag(bufTag).postln);
						});

					}, {
						"no buffer".postln;
					});

					ndefCS = (ndefs[blockIndex].cs.replace(")")	++ ", "
						++ blockFuncCS.cs ++ ");");

					ndefs[blockIndex].source = blockFunc;

					ndefCS.postln;

					if(extraArgs.notNil, {
						if(extraArgs.collect{|item| item.isSymbol}.includes(true), {
							newArgs = extraArgs;
						}, {
							newArgs = [blockFunc.argNames, extraArgs].flop.flat;
						});
						fadeWait.wait;
						this.set(block, newArgs, true);
					});

					liveBlocks[blockIndex] = [blocks[blockIndex][0], blockName, bufferID, isPattern, data];

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
		thisBuffer = thisBlock[blockIndex][2];
		{
			fadeOut.wait;
			if(thisBuffer.notNil, {
				if((thisBlock.flop[2].indicesOfEqual(thisBuffer).size > 1).not, {
					"remove buffer".postln;
					this.new.nodeTime.wait;
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

	*xset {arg block, argArr;
		var blockIndex;
		if((block >= 1), {
			blockIndex = block-1;
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
			if(format==\audio, {numFrames=seconds*Server.default.sampleRate}, {
				numFrames = seconds.calcPVRecSize(frameSize, hopSize);
			});
			buffer = [(\alloc++allocCount).asSymbol, [numFrames, channels]];
			recbuffers = recbuffers.add([\alloc, buffer].flat);
			resultArr = resultArr.add([\alloc, buffer[0], buffer[1]]);
			allocCount = allocCount + 1;
		};
		^resultArr;
	}

	*addRec {arg seconds=1, channels=1, format=\audio, frameSize=2048, hopSize=0.5, func;
		var buffer;
		this.addRecNdefs(channels);
		buffer = this.getRecBStoreIDs(1, seconds, channels, format, frameSize, hopSize).flat;
		BStore.add(buffer[0], buffer.copyRange(1, buffer.size-1), {
			"Record Buffer Allocated".postln;
			func.();
		});
	}

	*addRecNum {arg number=1, seconds=1, channels=1, format=\audio,
		frameSize=2048, hopSize=0.5, func;
		var bufferArr;
		number.do{
			this.addRecNdefs(channels);
		};
		bufferArr = this.getRecBStoreIDs(number, seconds, channels, format, frameSize, hopSize);
		BStore.addAll(bufferArr, {
			"Record Buffers Allocated".postln;
			func.();
		});
	}

	*addRecArr {arg argArr, func;
		var bufferArr, seconds, channels, format, frameSize, hopSize;
		argArr.do{|item|

			if(item[0].notNil, {seconds =  item[0]}, {seconds= 1});
			if(item[1].notNil, {channels =  item[1]}, {channels= 1});
			if(item[2].notNil, {format =  item[2]}, {format= \audio});
			if(item[3].notNil, {frameSize =  item[3]}, {frameSize = 2048});
			if(item[4].notNil, {frameSize =  item[4]}, {hopSize = 0.5});

			this.addRecNdefs(channels);
			bufferArr = bufferArr.add(this.getRecBStoreIDs(1, seconds, channels, format,
				frameSize,hopSize).flat);
		};
		BStore.addAll(bufferArr, {
			"Record Buffers Allocated".postln;
			func.();
		});
	}

	*removeRec { }

	*removeAllRec { }

	*addRecBuf {arg seconds=1, chanNum=1;
		var buffer;
		buffer = [(\alloc++allocCount).asSymbol, seconds*44100, chanNum];
		recbuffers = recbuffers.add([\alloc, buffer].flat);
		allocCount = allocCount + 1;

		BStore.add(\alloc, buffer, {arg buf;
			"Buffer Allocated".postln;
		});
	}

	*addAllRecBufs {

	}

	*recBuf {arg bufnum=1;
		if(recbuffers.notNil, {
			^recbuffers[bufnum-1];
		}, {
			"No record buffers".postln;
		});
	}

}