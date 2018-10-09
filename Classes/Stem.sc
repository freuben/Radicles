Stem : MainImprov {classvar <stems, <ndefs, <livestems, <stemCount=1;

	*add {arg type=\audio, channels=1;
		var ndefTag, ndefCS1, ndefCS2;
		if((type == \audio).or(type == \control), {
			ndefTag = ("stem" ++ stemCount).asSymbol;
			stemCount = stemCount + 1;
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
			stems = stems.add( [ndefTag, type, channels] );
			livestems = livestems.add(nil);
		}, {
			"stem Ndef rate not found".warn;
		});
	}

	*addNum {arg number, type, channels;
		var thisType, thisChan;
		number.do{|index|
			if(type.isArray, {thisType = type[index]}, {thisType = type});
			if(type.isArray, {thisChan = channels[index]}, {thisChan = channels});
			this.add(thisType, thisChan);
		};
	}

	*addAll {arg arr;
		arr.do{|item|
			this.add(item[0], item[1], item[2]);
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
		stemCount=1;
	}

	*clear {
		Ndef.clear;
		ndefs = [];
		stems = [];
		livestems = [];
		stemCount=1;
	}

	*playNdefs {
		Server.default.waitForBoot{
			ndefs.do{|item| item.play};
		};
	}

}