ModMap : MainImprov {
	classvar <modNodes, modIndex=0;

	*map {arg ndef, key=\freq, type=\sin, spec=[-1,1], extraArgs, mul=1, add=0, min, val, warp, post=\ide;
		var modMap;
		if(spec.isSymbol, {spec = SpecFile.read(\modulation, spec); });
		if((spec.isArray).and(spec[0].isSymbol), {spec = SpecFile.read(spec[0], spec[1]); });

		spec = spec.specFactor(mul, add, min, val, warp);
		modMap = this.getFile(type, spec, extraArgs, post);
		modNodes.do{|item| if( [item[1], item[2]] == [ndef, key], {item[0].clear;
			modNodes.remove(item);
		}); };
		modNodes = modNodes.add([modMap, ndef, key, spec]);
		ndef.xset(key, modMap);
		(ndef.cs ++ ".set(" ++ key.cs ++ ", " ++ modMap.cs ++ ");").postin(post, \ln);
		^modMap;
	}

	*unmap {arg ndef, key, value;
		var indexNodes, thisArr, num;
		modNodes.do{|item, index| if( item.indexOfAll([ndef, key]).reject({|item| item == nil}).size == 2,
			{
				if(value.isNil, {
					value = item.last.funcSpec.(0.5);
				});
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

	*getFile {arg type=\sin, spec=[-1,1], extraArgs, post=\ide;
		var ndefString, compile, ndef, fileData;
		fileData = ControlFile.read(\map, type);
		if(fileData.notNil, {
			modIndex = modIndex + 1;
			ndefString = fileData.cs.replace(".map", spec.mapSpec);
			if(extraArgs.isNil, {
				compile = "Ndef('mod" ++ modIndex.cs ++ "', " ++ ndefString ++ ");";
			}, {
				compile = "Ndef('mod" ++ modIndex.cs ++ "').put(0, " ++ ndefString
				++ ", extraArgs: " ++ extraArgs.cs ++ ");";
			});
			compile.postin(post, \ln);
			ndef = compile.interpret;
			ndef.fadeTime = fadeTime;
		}, {
			"Control File not Found".warn;
		});
		^ndef;
	}

	*lag {arg modNum=1, key, value;
		var modNumIndex;
		modNumIndex = modNum-1;
		modNodes[modNumIndex][0].lag(key, value);
	}

	*set {arg modNum=1, key, value;
		var modNumIndex;
		modNumIndex = modNum-1;
		modNodes[modNumIndex][0].set(key, value);
	}

	*xset {arg modNum=1, key, value;
		var modNumIndex;
		modNumIndex = modNum-1;
		modNodes[modNumIndex][0].xset(key, value);
	}

	*ndefs {
		^modNodes.flop[0];
	}

}