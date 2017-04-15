ModFile : MainImprov {var <filePath;

	* new {arg file=\synth, class=\filter;
		^super.new.initSynthFile(file, class);
	}

	initSynthFile {arg file, class;
		var dir, existFiles, fileName, classString, modPath;

		case
		{file == \synth} { modPath = "SynthFiles/"}
		{file == \spec} { modPath = "SpecFiles/"}
		{file == \control} { modPath = "ControlFiles/"}
		{file == \description} { modPath = "DescriptionFiles/"}
		;

		dir = (mainPath ++ modPath);
		PathName(dir).files.do{|item|
			existFiles = existFiles.add(item.fileNameWithoutDoubleExtension.asSymbol);
		};

		classString = class.asString.firstToUpper;

		/*classString.postln;*/

		if(existFiles.includes(classString.asSymbol), {
			fileName = (class.asString.firstToUpper ++ ".scd");
			filePath = (dir ++ fileName);
			/*^filePath;*/
		}, {
			"Class not found".warn;
			/*^filePath;*/
		});
	}

	array {
		^filePath.loadPath;
	}

	keys {var arr;
		arr = this.array;
		if(arr.notNil, {
			^arr.flop[0];
		}, {^arr});
	}

	read {arg key;
		var arr, index, sys, result;
		if(key.isNil, {
			result = this.keys;
		}, {
			arr = this.array;
			if(arr.notNil, {
				index = arr.flop[0].indexOf(key);
				result = arr[index][1];
			}, {result = nil});
		});
		^result;
	}

	post {arg key;
		this.read(key).cs.postln;
	}

	writeFunc  {arg arr;
		var path, file;
		path = filePath;
		file = File(path, "w+");
		file.write(arr.cs);
		file.close;
		"Updated file".postln;
	}

	write {arg key, dataArr;
		var arrayFromFile, writeFunc, keyIndex;
		arrayFromFile = this.array;
		if(arrayFromFile.notNil, {
			keyIndex = arrayFromFile.flop[0].indexOf(key);
			if(keyIndex.notNil, {
				Window.warnQuestion(("This key already exists: " ++
					"Are you sure you want to replace it?"), {
					arrayFromFile[keyIndex] = [key, dataArr];
					this.writeFunc(arrayFromFile);
				});
			}, {
				arrayFromFile = arrayFromFile.add([key, dataArr]);
				this.writeFunc(arrayFromFile);
			});
		}, {^arrayFromFile});
	}

	remove {arg key;
		var arrayFromFile, writeFunc, keyIndex, file;
		arrayFromFile = this.array;

		if(arrayFromFile.notNil, {
			keyIndex = arrayFromFile.flop[0].indexOf(key);
			if(keyIndex.notNil, {
				Window.warnQuestion(("This key already exists: " ++
					"Are you sure you want to remove it?"), {
					arrayFromFile.removeAt(keyIndex);
					this.writeFunc(arrayFromFile);
				});
			}, {
				"Key not found".warn;
			});
		}, {^arrayFromFile});
	}

}

SynthFile : ModFile {

	*path {arg class=\filter;
		var synthFile;
		synthFile = this.new(\synth, class);
		^synthFile.filePath;
	}

	* read {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\synth, class);
		^synthFile.read(key);
	}

	* postAll {arg class=\filter;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* post {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\synth, class);
		^synthFile.post(key);
	}

	* write {arg class=\filter, key, dataArr;
		var synthFile;
		synthFile = this.new(\synth, class);
		^synthFile.write(key, dataArr);
	}

	* remove {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\synth, class);
		^synthFile.remove(key);
	}

}

SpecFile : ModFile {classvar specArr;

	*path {arg class=\filter;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.filePath;
	}

	* read {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.read(key);
	}

	* post {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.post(key);
	}

	* postAll {arg class=\filter;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* write {arg class=\filter, key, dataArr;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.write(key, dataArr);
	}

	* remove {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.remove(key);
	}

	*specs {arg class=\filter, key;
		specArr = this.read(class, key).collect{ |item| item.funcSpec };
		^super.new(\spec, class);
	}

	map {arg index, value;
	^specArr[index].(value);
	}

}

ControlFile : ModFile {

	*path {arg class=\filter;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.filePath;
	}

	* read {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.read(key);
	}

	* post {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.post(key);
	}

	* postAll {arg class=\filter;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* write {arg class=\filter, key, dataArr;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.write(key, dataArr);
	}

	* remove {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.remove(key);
	}

}

DescriptionFile : ModFile {

	*path {arg class=\filter;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.filePath;
	}

	* read {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.read(key);
	}

	* post {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.post(key);
	}

	* postAll {arg class=\filter;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* write {arg class=\filter, key, dataArr;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.write(key, dataArr);
	}

	* remove {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.remove(key);
	}

}