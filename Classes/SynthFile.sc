SynthFile : MainImprov {var filePath;

	* new {arg class=\filter;
		^super.new.initSynthFile(class);
	}

	initSynthFile {arg class;
		var dir, existFiles, fileName, classString;

		dir = (mainPath ++ "SynthFiles/");
		PathName(dir).files.do{|item|
			existFiles = existFiles.add(item.fileNameWithoutDoubleExtension.asSymbol);
		};

		classString = class.asString.capitalize;

		if(existFiles.includes(classString.asSymbol), {
		fileName = (class.asString.capitalize ++ ".scd");
		filePath = (dir ++ fileName);
		^filePath;
		}, {
			"Class not found".warn;
		});
	}

	*array {arg class;
		^this.new(class).loadPath;
	}

	*keys {arg class;
		var arr;
		arr = this.array(class);
		^arr.flop[0];
	}

	*read {arg class, key;
		var arr, index, sys, result;
		if(key.isNil, {
			result = this.keys(class);
		}, {
			arr = this.array(class);
		index = arr.flop[0].indexOf(key);
		result = arr[index][1];
		});
		^result;
	}

	*post {arg class, key;
		this.read(class, key).cs.postln;
	}

	*dopost {arg class, key;
		this.read(class, key).do{|item| item.cs.postln};
	}

	*write {arg class, key, dataArr;

	}

}