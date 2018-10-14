Assemblage : MainImprov {classvar <stems, <inputs, <outputs, <livestems, <stemCount=1, <ndefs;

	*addStem {arg input, channels, spaceType=\pan2;
		var stemTag, ndefCS1, ndefCS2;
		channels ?? {channels = input.numChannels};
		stemTag = ("stem" ++ stemCount).asSymbol;
		stemCount = stemCount + 1;
		/*			ndefCS1 = "Ndef.ar(";
		ndefCS1 = (ndefCS1 ++ ndefTag.cs ++ ", " ++ channels.cs ++ ");");
		ndefCS1.postln;
		ndefCS1.interpret;
		ndefCS2 = ("Ndef(" ++ ndefTag.cs ++ ").fadeTime = " ++ fadeTime.cs ++ ";");
		ndefCS2.postln;
		ndefCS2.interpret;*/
		/*			ndefs = ndefs.add(Ndef(ndefTag));*/
		inputs = inputs.add(input);
		outputs = outputs.add(input);
		//tag, input, output, channels, spaceType:
		stems = stems.add( [stemTag, input, input, channels, spaceType] );
		livestems = livestems.add(nil);
	}

	*addStems {arg number, inputs, channels=1, spaceTypes;
		var thisChan, thisDest;
		number.do{|index|
			if(channels.isArray, {thisChan = channels[index]}, {thisChan = channels});
			if(spaceTypes.isArray, {thisDest = spaceTypes[index]}, {thisDest = spaceTypes});
			this.add(inputs, thisChan, thisDest);
		};
	}

	*addAllStems {arg arr;
		arr.do{|item|
			this.addStem(item);
		}
	}

	*remove {arg stem=1;
		var stemIndex;
		stemIndex = stem - 1;
		if((stem >= 1).and(stem <= stems.size), {
			/*ndefs[stemIndex].free;*/
			ndefs[stemIndex].clear;
			ndefs.removeAt(stemIndex);
			stems.removeAt(stemIndex);
			livestems.removeAt(stemIndex);
		}, {
			"stem Number not Found".warn;
		});
	}

	*removeArr {arg stemArr;
		var newArr;
		stemArr.do{|item|
			if((item >= 1).and(item <= stems.size), {
				newArr = newArr.add(item-1);
				/*ndefs[item-1].free;*/
				ndefs[item-1].clear;
			}, {
				"stem Number not Found".warn;
			});
		};
		ndefs.removeAtAll(newArr);
		stems.removeAtAll(newArr);
		livestems.removeAll(newArr);
	}

	*removeAll {
		ndefs.do{|item| item.clear };
		ndefs = [];
		stems = [];
		livestems = [];
		stemCount = 1;
	}

	*clear {
		Ndef.clear;
		ndefs = [];
		stems = [];
		livestems = [];
		stemCount = 1;
	}

	*playNdefs {
		Server.default.waitForBoot{
			outputs.do{|item| item.play};
		};
	}

}