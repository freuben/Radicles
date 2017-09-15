ModMap : MainImprov {
	classvar <modNodes, modIndex=0;

	*map {arg ndef, key=\freq, type=\sin, spec=[-1,1], extraArgs, post=\ide;
		var modMap;
		modMap = this.getFile(type, spec, extraArgs, post);
		modNodes.do{|item| if( [item[1], item[2]] == [ndef, key], {item[0].clear;
			modNodes.remove(item);
		}); };
		modNodes = modNodes.add([modMap, ndef, key, spec]);
		ndef.set(key, modMap);
		(ndef.cs ++ ".xset(" ++ key.cs ++ ", " ++ modMap.cs ++ ");").postin(post, \ln);
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

}