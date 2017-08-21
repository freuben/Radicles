Block : MainImprov {classvar <blocks, <ndefs, <blockCount=1, fadeTime=0.5;

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

			ndefCS1 = (ndefCS1 ++ ndefTag.cs ++ ", " ++ channels.asString ++ ");");
			ndefCS1.postln;
			ndefCS1.interpret;

			ndefCS2 = ("Ndef(" ++ ndefTag.cs ++ ").fadeTime = " ++ fadeTime.asString ++ ";");
			ndefCS2.postln;
			ndefCS2.interpret;

			ndefs = ndefs.add(Ndef(ndefTag));
			blocks = blocks.add( [ndefTag, type, channels, destination] );
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
				ndefs.removeAt(blockIndex);
				blocks.removeAt(blockIndex);
			}, {
				"Block Number not Found".warn;
			});
		}

		*removeArr {arg blockArr;
			var newArr;
			blockArr.do{|item|
				if((item >= 1).and(item <= blocks.size), {
					newArr = newArr.add(item);
				}, {
					"Block Number not Found".warn;
				});
			};
			ndefs.removeAtAll(newArr-1);
			blocks.removeAtAll(newArr-1);
		}

		*play {arg block=1, slot=1, bockName, buffer, isPattern=false, extraArgs, data;
			var blockFunc, blockIndex, slotIndex, newArgs;

			if((block >= 1).and(slot >= 1), {
				blockIndex = block-1;
				slotIndex = slot-1;
				blockFunc = SynthFile.read(\block, bockName);

				if(extraArgs.collect{|item| item.isSymbol}.includes(true), {
					newArgs = extraArgs;
				}, {
					newArgs = [blockFunc.argNames, extraArgs].flop.flat;
				});

				ndefs[blockIndex].put(slotIndex, blockFunc, extraArgs: newArgs);
				(ndefs[blockIndex].asString	++ ".put(" ++ slotIndex.asString ++ ", "
					++ blockFunc.cs ++ ", extraArgs: " ++ newArgs.asString ++ ");").postln;
			}, {
				"Block or slot not found".warn;
			});
		}

		*stop {arg block=1, slot=1, fadeOut;
			var blockIndex, slotIndex;

			if((block >= 1).and(slot >= 1), {
				blockIndex = block-1;
				slotIndex = slot-1;

				fadeOut ?? {fadeOut = ndefs[blockIndex].fadeTime};
				ndefs[blockIndex].fadeTime = fadeOut;
				{
					ndefs[blockIndex].put(slotIndex, nil);
					(ndefs[blockIndex].asString	++ ".put(" ++ slotIndex.asString ++ ", "
						++ "nil);" ).postln;

					fadeOut.wait;

					ndefs[blockIndex].fadeTime = fadeTime;
				}.fork;
			}, {
				"Block or slot not found".warn;
			});

		}

	}