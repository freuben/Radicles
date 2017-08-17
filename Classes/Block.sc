Block : MainImprov {classvar <blocks, <ndefs, <blockCount=1;

	*add {arg type=\audio, channels=1, destination;
		var ndefTag;
		if((type == \audio).or(type == \control), {

			ndefTag = ("block" ++ blockCount).asSymbol;
			blockCount = blockCount + 1;

			if(type == \audio, {
				Ndef.ar(ndefTag, channels);
			}, {
				Ndef.kr(ndefTag, channels);
			});

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

	*remove {arg blockNum=1;
		var blockIndex;
		blockIndex = blockNum - 1;
		if((blockNum >= 1).and(blockNum <= blocks.size), {
		ndefs.removeAt(blockIndex);
		blocks.removeAt(blockIndex);
		}, {
			"Block Number not Found".warn;
		});
	}

	*removeArr {arg blockNumArr;
		var newArr;
		blockNumArr.do{|item|
		if((item >= 1).and(item <= blocks.size), {
				newArr = newArr.add(item);
			}, {
				"Block Number not Found".warn;
			});
		};
		ndefs.removeAtAll(newArr-1);
		blocks.removeAtAll(newArr-1);
}

}