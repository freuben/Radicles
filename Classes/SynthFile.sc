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

		classString = class.asString.firstToUpper;

		classString.postln;

		if(existFiles.includes(classString.asSymbol), {
			fileName = (class.asString.firstToUpper ++ ".scd");
			filePath = (dir ++ fileName);
			^filePath;
		}, {
			"Class not found".warn;
			^filePath;
		});
	}

	*array {arg class;
		^this.new(class).loadPath;
	}

	*keys {arg class;
		var arr;
		arr = this.array(class);
		if(arr.notNil, {
			^arr.flop[0];
		}, {^arr});
	}

	*read {arg class, key;
		var arr, index, sys, result;
		if(key.isNil, {
			result = this.keys(class);
		}, {
			arr = this.array(class);
			if(arr.notNil, {
				index = arr.flop[0].indexOf(key);
				result = arr[index][1];
			}, {result = nil});
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
		var arrayFromFile, writeFunc, keyIndex, file;
		arrayFromFile = this.array(class);
		writeFunc = {arg arr;
			var path;
			path = this.new(class);
			file = File(path, "w+");
			file.write(arr.cs);
			file.close;
			"Written into file".postln;
		};
		if(arrayFromFile.notNil, {
			keyIndex = arrayFromFile.flop[0].indexOf(key);
			if(keyIndex.notNil, {
				Window.warnQuestion(("This key already exists: " ++
					"Are you sure you want to replace it?"), {
					arrayFromFile[keyIndex] = [key, dataArr];
					writeFunc.value(arrayFromFile);
				});
			}, {
				arrayFromFile = arrayFromFile.add([key, dataArr]);
				writeFunc.value(arrayFromFile);
			});
		}, {^arrayFromFile});
	}

}