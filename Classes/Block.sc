Block : MainImprov {classvar <blocks, <ndefs, <liveBlocks, <blockCount=1, fadeTime=0.5, <>playFormat=\audio;

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
		var blockFunc, blockIndex, newArgs, ndefCS, cond, blockFuncCS;
		if(block >= 1, {
			blockIndex = block-1;

			if(ndefs[blockIndex].notNil, {
				{
					blockFunc = SynthFile.read(\block, blockName);
					blockFuncCS = blockFunc;
					if(blockFunc.cs.findAll("{").size == 2, {
						"includes buffer".postln;
						cond = Condition(false);
						cond.test = false;
						if(buffer.notNil.or(buffer == \nobuf), {
							BStore.add(\play, buffer, {arg buf;
								blockFunc = blockFunc.(buf);

								cond.test = true;
								cond.signal;
							});
							cond.wait;
						}, {
							"Buffer not provided".warn;
						});
					}, {
						"no buffer".postln;
					});

					if(extraArgs.notNil, {
						if(extraArgs.collect{|item| item.isSymbol}.includes(true), {
							newArgs = extraArgs;
						}, {
							newArgs = [blockFunc.argNames, extraArgs].flop.flat;
						});
					});

					ndefCS = (ndefs[blockIndex].cs	++ ".source = "
						++ blockFuncCS.cs ++ ");");

					ndefs[blockIndex].source = blockFunc;

					ndefCS.postln;
					this.set(block, newArgs, true);

					liveBlocks[blockIndex] = [blocks[blockIndex][0], blockName, buffer, isPattern, data];

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
			ndefs[blockIndex].fadeTime = fadeOut;
			{
				ndefCS = (ndefs[blockIndex].cs	++ ".source = "
					++ "nil;" );
				ndefCS.interpret;
				ndefCS.postln;
				fadeOut.wait;
				ndefs[blockIndex].fadeTime = fadeTime;
			}.fork;
		}, {
			"Block not found".warn;
		});

	}

	*set {arg block, argArr, post=false;
		var blockIndex;
if((block >= 1), {
			blockIndex = block-1;
			if(post == true, {
			(ndefs[blockIndex].cs	++ ".set(" ++ argArr.cs.replace("[", "").replace("]", "") ++  ");").postln;
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
		ndefs.do{|item| item.play};
	}

}