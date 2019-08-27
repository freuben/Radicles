ModMap : Radicles {
	classvar <modNodes, modIndex=0, <lagArr;

	*map {arg ndef, key=\freq, type=\sin, spec=[-1,1], extraArgs, func, mul=1, add=0, min, val, warp, lag;
		var modMap;
		if(spec.isSymbol, {spec = SpecFile.read(\modulation, spec); });
		if((spec.isArray).and(spec[0].isSymbol), {spec = SpecFile.read(spec[0], spec[1]); });
		spec = spec.specFactor(mul, add, min, val, warp);
		modMap = this.getFile(type, spec, extraArgs, func);
		modNodes.do{|item| if( [item[1], item[2]] == [ndef, key], {item[0].clear;
			modNodes.remove(item);
		}); };
		modNodes = modNodes.add([modMap, ndef, key]);
		ndef.xset(key, modMap);
		if(lag.notNil, {
			this.lag(ndef.key.asString.divNumStr[1], key, lag);
		});
		(ndef.cs ++ ".set(" ++ key.cs ++ ", " ++ modMap.cs ++ ");").radpost;
		^modMap;
	}

	*unmap {arg ndef, key, value=0;
		var indexNodes, thisArr, num;
		modNodes.do{|item, index| if( item.indexOfAll([ndef, key]).reject({|item| item == nil}).size == 2,
			{
				ndef.xset(key, value);
				modNodes.removeAt(index);
		});
		};
	}

	*unmapAt {arg index;
		if((modNodes.isNil).or(modNodes.isEmpty).not, {
			this.unmap(modNodes[0][1], modNodes[0][2]);
		}, {
			"no maps left".warn;
		});
	}

	*getFile {arg type=\sin, spec=[-1,1], extraArgs, func;
		var ndefString, compile, ndef, fileData, fileFuncDef, getArgString, getFuncArgs;
		fileData = ControlFile.read(\map, type);
		if(fileData.notNil, {
			modIndex = modIndex + 1;
			ndefString = fileData.cs.replace(".map", spec.mapSpec);
			if(func.notNil, {
				fileFuncDef = fileData.def;
				getArgString = "arg " ++ fileFuncDef.argumentString ++ "; ";
				getFuncArgs = fileFuncDef.makeEnvirFromArgs;
				ndefString = "{" ++ getArgString ++ func.cs ++".(" ++ ndefString
				++ "." ++ getFuncArgs.keys.asArray.asString.squareToRound ++ ")}";
			});
			if(extraArgs.isNil, {
				compile = "Ndef('mod" ++ modIndex.cs ++ "', " ++ ndefString ++ ");";
			}, {
				/*compile = "Ndef('mod" ++ modIndex.cs ++ "').put(0, " ++ ndefString
				++ ", extraArgs: " ++ extraArgs.cs ++ ");";*/
				compile = "Ndef('mod" ++ modIndex.cs ++ "', " ++ ndefString ++ ");" ++
				10.asAscii ++"Ndef('mod" ++ modIndex.cs ++ "').set" ++
				extraArgs.cs.squareToRound ++ ";";
			});
			compile.radpost;
			ndef = compile.interpret;
			ndef.fadeTime = fadeTime;
		}, {
			"Control File not Found".warn;
		});
		^ndef;
	}

	*lag {arg modNum=1, key, value;
		var modNumIndex, string, ndef;
		modNumIndex = modNum-1;
		ndef = modNodes[modNumIndex][0];
		string = ndef.cs ++ ".lag(" ++ key.cs
		++ ", " ++ value.cs ++ ");";
		string.radpost;
		string.interpret;
		lagArr.do{|item| if( [item[0], item[1]] == [ndef, key], {
			lagArr.remove(item);
		}); };
		lagArr = lagArr.add([ndef, key, value]);
	}

	*set {arg modNum=1, key, value;
		var modNumIndex, string;
		modNumIndex = modNum-1;
		string = modNodes[modNumIndex][0].cs ++ ".set(" ++ key.cs
		++ ", " ++ value.cs ++ ");";
		string.radpost;
		string.interpret;
	}

	*xset {arg modNum=1, key, value;
		var modNumIndex, string;
		modNumIndex = modNum-1;
		string = modNodes[modNumIndex][0].cs ++ ".xset(" ++ key.cs
		++ ", " ++ value.cs ++ ");";
		string.radpost;
		string.interpret;
	}

	*ndefs {
		^modNodes.flop[0];
	}

	*list {
		ControlFile.read(\map).postln;
	}

	*read {arg type;
		ControlFile.read(\map, type).cs.postln;
	}

	*getPresets {var result;
		if(ModMap.lagArr.notNil, {
			result = modNodes.collect{|item, index| [item[0].key.cs, item[0].source.cs,
			item[0].controlKeysValues.cs] ++ [item[1].cs, item[2].cs]
		++ [ModMap.lagArr[index].collect({|it| it.cs }) ] };
		}, {
			result = modNodes.collect{|item, index| [item[0].key.cs, item[0].source.cs,
			item[0].controlKeysValues.cs] ++ [item[1].cs, item[2].cs] };
		});
		^result;
	}

	* presetToCode {arg arr, newNdef;
		newNdef ?? {newNdef = arr[3]};
		if(newNdef.isString.not, {newNdef = newNdef.cs});
		^[
			("Ndef(" ++ arr[0] ++ ", " ++ arr[1] ++ ");"),
			("Ndef(" ++ arr[0] ++ ").set(" ++ arr[2].replace("[", "").replace("]", ");");),
			if(arr[5].notNil, {
				(arr[5][0] ++ ".lag(" ++ arr[5][1] ++  ", " ++ arr[5][2] ++ ");");
			}),
			(newNdef ++ ".set(" ++ arr[4] ++ ", " ++ "Ndef(" ++ arr[0] ++ "));";)
		]
	}

}