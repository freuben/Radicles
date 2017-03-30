ModMap : MainImprov {
	var <modNodes, <>fadeTime, modIndex;

	*new {arg fadeTime=3;
		^super.new.initModMap(fadeTime);
	}

	initModMap {arg time;
		fadeTime = time;
		modNodes = [];
		modIndex = 0;
	}

	fromFile {arg type=\sin, spec=[-1,1], extraArgs, post=\ide;
		var ndefString, compile, ndef;
		modIndex = modIndex + 1;
		ndefString = ControlFile.read(\map, type).cs.replace(".map", spec.mapSpec);
		if(extraArgs.isNil, {
			compile = "Ndef('mod" ++ modIndex.cs ++ "', " ++ ndefString ++ ");";
		}, {
			compile = "Ndef('mod" ++ modIndex.cs ++ "').put(0, " ++ ndefString
			++ ", extraArgs: " ++ extraArgs.cs ++ ");";
		});
		compile.postin(post, \ln);
		ndef = compile.interpret;
		ndef.fadeTime = fadeTime;
		^ndef;
	}

	map {arg ndef, key=\freq, type=\sin, spec=[-1,1], extraArgs, lagTime, post=\ide;
		var modMap;
		ndef.lag(key, lagTime);
		modMap = this.fromFile(type, spec, extraArgs, post);
		modNodes = modNodes.add([modMap, ndef, key, spec]);
		{
			fadeTime.yield;
			ndef.map(key, modMap);
		}.fork;
		^modMap;
	}

	unmap {arg ndef;
		var indexNodes, thisArr;
		modNodes.flop[0].do{|item, index| if(item.key == ndef.key, {indexNodes = index}) };
		thisArr = modNodes[indexNodes];
		thisArr[1].set(thisArr[2], thisArr[3].funcSpec.value(0.5) );
		thisArr[0].free;
		modNodes.removeAt(indexNodes);
	}

}