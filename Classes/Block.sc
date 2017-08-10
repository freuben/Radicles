Block : MainImprov {classvar <blockIDs, <blocks, <blockCount=0;

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

			blocks = blocks.add(Ndef(ndefTag));
			blockIDs = blockIDs.add( [ndefTag, type, channels, destination] );

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

}