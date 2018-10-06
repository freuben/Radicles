ModFile : MainImprov {var <filePath;

	* new {arg file=\synth, class=\filter;
		^super.new.initSynthFile(file, class);
	}

	initSynthFile {arg file, class;
		var dir, existFiles, fileName, classString, modPath;

		case
		{file == \synth} { modPath = "Files/SynthFiles/"}
		{file == \spec} { modPath = "Files/SpecFiles/"}
		{file == \control} { modPath = "Files/ControlFiles/"}
		{file == \data} { modPath = "Files/DataFiles/"}
		{file == \description} { modPath = "Files/DescriptionFiles/"}
		{file == \synthdef} { modPath = "Files/SynthDefFiles/"}
		{file == \preset} { modPath = "Settings/Presets/"}

		;

		dir = (mainPath ++ modPath);
		PathName(dir).files.do{|item|
			existFiles = existFiles.add(item.fileNameWithoutDoubleExtension.asSymbol);
		};

		classString = class.asString.firstToUpper;

		if(existFiles.includes(classString.asSymbol), {
			fileName = (class.asString.firstToUpper ++ ".scd");
			filePath = (dir ++ fileName);
		}, {
			"Class not found".warn;
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
				if(index.notNil, {
					result = arr[index][1];
				}, {
					"Key not found".warn;
				});
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
		file.write(arr.cs.lineFormat);
		file.close;
		"Updated file".postln;
	}

	write {arg key, dataArr, window=true, func;
		var arrayFromFile, writeFunc, keyIndex;
		arrayFromFile = this.array;
		if(arrayFromFile.notNil, {
			keyIndex = arrayFromFile.flop[0].indexOf(key);
			if(keyIndex.notNil, {
				if(window, {
					Window.warnQuestion(("This key already exists: " ++
						"Are you sure you want to replace it?"), {
						arrayFromFile[keyIndex] = [key, dataArr];
						this.writeFunc(arrayFromFile);
						func.();
					});
				}, {
					arrayFromFile[keyIndex] = [key, dataArr];
					this.writeFunc(arrayFromFile);
				});
			}, {
				arrayFromFile = arrayFromFile.add([key, dataArr]);
				this.writeFunc(arrayFromFile);
				func.();
			});
		}, {^arrayFromFile});
	}

	remove {arg key, window=true, func;
		var arrayFromFile, writeFunc, keyIndex, file;
		arrayFromFile = this.array;

		if(arrayFromFile.notNil, {
			keyIndex = arrayFromFile.flop[0].indexOf(key);
			if(keyIndex.notNil, {
				if(window, {
					Window.warnQuestion(("Are you sure you want to remove this key?"), {
						arrayFromFile.removeAt(keyIndex);
						this.writeFunc(arrayFromFile);
						func.();
					});
				}, {
					arrayFromFile.removeAt(keyIndex);
					this.writeFunc(arrayFromFile);
				});
			}, {
				"Key not found".warn;
			});
		}, {^arrayFromFile});
	}

	*all {var file, arr;
		file = this.path;
		PathName.new(file.dirname).files.do{|item|
			arr = arr.add(item.fileNameWithoutExtension.toLower.asSymbol)};
		^arr
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

	* write {arg class=\filter, key, dataArr, win=true;
		var synthFile;
		synthFile = this.new(\synth, class);
		^synthFile.write(key, dataArr, win);
	}

	* remove {arg class=\filter, key, win=true;
		var synthFile;
		synthFile = this.new(\synth, class);
		^synthFile.remove(key, win);
	}

	* string {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\synth, class);
		^synthFile.read(key).cs;
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

	* write {arg class=\filter, key, dataArr, win=true;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.write(key, dataArr, win);
	}

	* remove {arg class=\filter, key, win=true;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.remove(key, win);
	}

	*specs {arg class=\filter, key;
		specArr = this.read(class, key).collect{ |item| item.funcSpec };
		^super.new(\spec, class);
	}

	map {arg index, value;
		^specArr[index].(value);
	}

	* string {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.read(key).cs;
	}

}

ControlFile : ModFile {

	*path {arg class=\map;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.filePath;
	}

	* read {arg class=\map, key;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.read(key);
	}

	* post {arg class=\map, key;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.post(key);
	}

	* postAll {arg class=\map;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* write {arg class=\map, key, dataArr, win=true;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.write(key, dataArr, win);
	}

	* remove {arg class=\map, key, win=true;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.remove(key, win);
	}

	* string {arg class=\map, key;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.read(key).cs;
	}

}

DataFile : ModFile {

	*path {arg class=\sampler;
		var synthFile;
		synthFile = this.new(\data, class);
		^synthFile.filePath;
	}

	* read {arg class=\sampler, key;
		var synthFile;
		synthFile = this.new(\data, class);
		^synthFile.read(key);
	}

	* post {arg class=\sampler, key;
		var synthFile;
		synthFile = this.new(\data, class);
		^synthFile.post(key);
	}

	* postAll {arg class=\sampler;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* write {arg class=\sampler, key, dataArr, win=true;
		var synthFile;
		synthFile = this.new(\data, class, win);
		^synthFile.write(key, dataArr);
	}

	* remove {arg class=\sampler, key, win=true;
		var synthFile;
		synthFile = this.new(\data, class);
		^synthFile.remove(key, win);
	}

	* string {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\data, class);
		^synthFile.read(key).cs;
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

	* write {arg class=\filter, key, dataArr, win=true;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.write(key, dataArr, win);
	}

	* remove {arg class=\filter, key, win=true;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.remove(key, win);
	}

	* string {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.read(key).cs;
	}

}

PresetFile : ModFile {

	*path {arg class=\bstore;
		var synthFile;
		synthFile = this.new(\preset, class);
		^synthFile.filePath;
	}

	* read {arg class=\bstore, key;
		var synthFile;
		synthFile = this.new(\preset, class);
		^synthFile.read(key);
	}

	* post {arg class=\bstore, key;
		var synthFile;
		synthFile = this.new(\preset, class);
		^synthFile.post(key);
	}

	* postAll {arg class=\bstore;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* write {arg class=\bstore, key, dataArr, win=true;
		var synthFile;
		synthFile = this.new(\preset, class);
		^synthFile.write(key, dataArr, win);
	}

	* remove {arg class=\bstore, key, win=true;
		var synthFile;
		synthFile = this.new(\preset, class);
		^synthFile.remove(key, win);
	}

	* string {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\preset, class);
		^synthFile.read(key).cs;
	}

}

SynthDefFile : ModFile {

	*path {arg class=\filter;
		var synthFile;
		synthFile = this.new(\synthdef, class);
		^synthFile.filePath;
	}

	* read {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\synthdef, class);
		^synthFile.read(key);
	}

	* postAll {arg class=\filter;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* post {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\synthdef, class);
		^synthFile.post(key);
	}

	* write {arg class=\filter, key, dataArr, desc;
		var synthFile;
		synthFile = this.new(\synthdef, class);
		^synthFile.write(key, dataArr, true, {
			SynthFile.write(class, key, dataArr.specFunc, false);
			SpecFile.write(class, key, dataArr.specArr, false);
			if(desc.notNil, {
				DescriptionFile.write(class, key, desc, false);
			});
		});
	}

	* remove {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\synthdef, class);
		^synthFile.remove(key, true, {
			SynthFile.remove(class, key, false);
			SpecFile.remove(class, key, false);
			DescriptionFile.remove(class, key, false);
		});
	}

	* string {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\synthdef, class);
		^synthFile.read(key).cs;
	}

}