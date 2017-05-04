ModMap : MainImprov {
	var <modNodes, <>fadeTime, modIndex;

	*new {arg fadeTime=0.1;
		^super.new.initModMap(fadeTime);
	}

	initModMap {arg time;
		fadeTime = time;
		modNodes = [];
		modIndex = 0;
	}

	fromFile {arg type=\sin, spec=[-1,1], extraArgs, post=\ide;
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

	map {arg ndef, key=\freq, type=\sin, spec=[-1,1], extraArgs, lagTime, post=\ide;
		var modMap;
		lagTime ?? {lagTime = fadeTime;};
		ndef.lag(key, lagTime);
		modMap = this.fromFile(type, spec, extraArgs, post);
		modNodes = modNodes.add([modMap, ndef, key, spec]);
		{
			fadeTime.yield;
			ndef.map(key, modMap);
			(ndef.cs ++ ".map(" ++ key.cs ++ ", " ++ modMap.cs ++ ");").postin(post, \ln);
		}.fork;
		^modMap;
	}

	unmap {arg ndef;
		var indexNodes, thisArr;
		modNodes.flop[0].do{|item, index| if(item.key == ndef.key, {indexNodes = index}) };
		thisArr = modNodes[indexNodes];
		thisArr[1].set(thisArr[2], thisArr[3].funcSpec.(0.5) );
		thisArr[0].free;
		modNodes.removeAt(indexNodes);
	}

	ndefs {
		^this.modNodes.flop[0];
	}

}