ModFile : Radicles {var <filePath, <libArr;

	* new {arg file=\synth, class=\filter;
		^super.new.initFile(file, class);
	}

	initFile {arg file, class;
		filePath =  this.whichFile(file, class);
		libArr = this.loadLibs(file, class);
	}

	whichFile {arg file, class, thisPath;
		var dir, existFiles, fileName, classString, modPath;

		case
		{file == \synth} { modPath = "Files/SynthFiles/"}
		{file == \synth} { modPath = "Files/SynthFiles/"}
		{file == \spec} { modPath = "Files/SpecFiles/"}
		{file == \control} { modPath = "Files/ControlFiles/"}
		{file == \data} { modPath = "Files/DataFiles/"}
		{file == \description} { modPath = "Files/DescriptionFiles/"}
		{file == \synthdef} { modPath = "Files/SynthDefFiles/"}
		{file == \preset} { modPath = "Settings/Presets/"}
		;
		thisPath ?? {thisPath = mainPath};
		dir = (thisPath ++ modPath);

		PathName(dir).files.do{|item|
			existFiles = existFiles.add(item.fileNameWithoutDoubleExtension.asSymbol);
		};

		classString = class.asString.firstToUpper;

		if(existFiles.includes(classString.asSymbol), {
			fileName = (class.asString.firstToUpper ++ ".scd");
			^(dir ++ fileName);
		}, {
			"Class not found".warn;
		});
	}

	writeArray {arg path;
		^path.loadPath;
	}

	array {var resultArr, mainLib;
		mainLib = filePath.loadPath;
		resultArr = mainLib;
		if(libArr.notNil, {
			if(libArr.notEmpty, {
				libArr.do{|item|
					resultArr = resultArr ++ item.loadPath;
				};
				resultArr = resultArr.atAll(resultArr.flop[0].rejectSame.collect{|item|
					resultArr.flop[0].indicesOfEqual(item)[0]}
				);
			});
		});
		^resultArr;
	}

	keys {var arr;
		arr = this.array;
		if(arr.notNil, {
			^arr.flop[0];
		}, {^arr});
	}

	read {arg key, warn=true;
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
					if(warn, {
						"Key not found".warn;
					});
				});
			}, {result = nil});
		});
		^result;
	}

	post {arg key;
		this.read(key).cs.postln;
	}

	writeFunc  {arg arr, path, post=true;
		var file;
		file = File(path, "w+");
		file.write(arr.cs);
		file.close;
		if(post, {
		"Updated file".postln;
		});
	}

	write {arg key, dataArr, window=true, func, path, post=true;
		var arrayFromFile, writeFunc, keyIndex;
		path ?? {path = filePath};
		if(path.isNumber, {
			path = libArr[path];
		});
		arrayFromFile = this.writeArray(path);
		if(arrayFromFile.notNil, {
			keyIndex = arrayFromFile.flop[0].indexOf(key);
			if(keyIndex.notNil, {
				if(window, {
					Window.warnQuestion(("This key already exists: " ++
						"Are you sure you want to replace it?"), {
						arrayFromFile[keyIndex] = [key, dataArr];
						this.writeFunc(arrayFromFile, path, post);
						func.();
					});
				}, {
					arrayFromFile[keyIndex] = [key, dataArr];
					this.writeFunc(arrayFromFile, path, post);
				});
			}, {
				arrayFromFile = arrayFromFile.add([key, dataArr]);
				this.writeFunc(arrayFromFile, path, post);
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

	loadLibs {arg file, class;
		var libsFolder, libsFiles;
		libsFolder = PathName(libPath).folders;
		if(libsFolder.notEmpty, {
			libsFiles = libsFolder.collect{|item|
				this.whichFile(file, class, item.pathOnly;);
			};
			^libsFiles;
		});
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

	* write {arg class=\filter, key, dataArr, win=true, path, post=true;
		var synthFile;
		synthFile = this.new(\synth, class);
		^synthFile.write(key, dataArr, win, path: path, post: post);
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

	* read {arg class=\filter, key, warn=true;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.read(key, warn);
	}

	* post {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.post(key);
	}

	* postAll {arg class=\filter;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* write {arg class=\filter, key, dataArr, win=true, path, post=true;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.write(key, dataArr, win, path: path, post: post);
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

	* write {arg class=\map, key, dataArr, win=true, path, post=true;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.write(key, dataArr, win, path: path, post: post);
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

	* write {arg class=\sampler, key, dataArr, win=true, path, post=true;
		var synthFile;
		synthFile = this.new(\data, class);
		^synthFile.write(key, dataArr, win, path: path, post: post);
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

	* read {arg class=\filter, key, warn=true;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.read(key, warn);
	}

	* post {arg class=\filter, key;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.post(key);
	}

	* postAll {arg class=\filter;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* write {arg class=\filter, key, dataArr, win=true, path, post=true;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.write(key, dataArr, win, path: path, post: post);
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
		var presetFile;
		presetFile = this.new(\preset, class);
		^presetFile.filePath;
	}

	* read {arg class=\bstore, key;
		var presetFile;
		presetFile = this.new(\preset, class);
		^presetFile.read(key);
	}

	* post {arg class=\bstore, key;
		var presetFile;
		presetFile = this.new(\preset, class);
		^presetFile.post(key);
	}

	* postAll {arg class=\bstore;
		this.read(class).do{|item| (item.cs ++ " -> ").post; this.post(class, item) }
	}

	* readAll {arg class=\bstore;
		^this.read(class).collect{|item| [item, this.read(class, item)] }
	}

	* write {arg class=\bstore, key, dataArr, win=true, path, post=true;
		var presetFile;
		presetFile = this.new(\preset, class);
		^presetFile.write(key, dataArr, win, path: path, post: post);
	}

	* remove {arg class=\bstore, key, win=true;
		var presetFile;
		presetFile = this.new(\preset, class);
		^presetFile.remove(key, win);
	}

	* string {arg class=\filter, key;
		var presetFile;
		presetFile = this.new(\preset, class);
		^presetFile.read(key).cs;
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

	* info {arg class=\filter, key;
		var string;
		string = [class, key, this.read(class, key), DescriptionFile.read(class, key)].cs;
		string = string.replaceAt("(", 0).replaceAt(")", string.size-1);
		^string;
	}

	* write {arg class=\filter, key, dataArr, desc, path, post=true;
		var synthFile;
		synthFile = this.new(\synthdef, class);
		if(dataArr.specArr.notNil, {
			^synthFile.write(key, dataArr, true, {
				SynthFile.write(class, key, dataArr.specFunc, false, path, false);
				SpecFile.write(class, key, dataArr.specArr, false, path, false);
				if(desc.notNil, {
					DescriptionFile.write(class, key, desc, false, path, false);
				});
			}, path: path, post: post);
		}, {
			"You need to provide at least one argument and spec".warn;
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