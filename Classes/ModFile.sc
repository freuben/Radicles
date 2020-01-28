ModFile : Radicles {var <filePath, <libArr;

	* new {arg file=\synth, class=\filter, exclude;
		^super.new.initFile(file, class, exclude);
	}

	initFile {arg file, class, exclude;
		if(exclude.isNil, {
			filePath =  this.whichFile(file, class);
		}, {
			if(exclude.indexOfEqual("Main").notNil.not, {
				filePath =  this.whichFile(file, class);
			});
		});
		libArr = this.loadLibs(file, class, exclude);
	}

	whichFile {arg file, class, thisPath;
		var dir, existFiles, fileName, classString, modPath, dash;
		dash = "/";
		Platform.case(\windows,   {dash = "\\"; });
		case
		{file == \synth} { modPath = dash ++ "Files" ++ dash ++ "SynthFiles" ++ dash}
		{file == \spec} { modPath = dash ++ "Files" ++ dash ++ "SpecFiles" ++ dash}
		{file == \control} { modPath = dash ++ "Files" ++ dash ++ "ControlFiles" ++ dash}
		{file == \data} { modPath = dash ++ "Files" ++ dash ++ "DataFiles" ++ dash}
		{file == \description} { modPath = dash ++ "Files" ++ dash ++ "DescriptionFiles" ++ dash}
		{file == \synthdef} { modPath = dash ++ "Files" ++ dash ++ "SynthDefFiles" ++ dash}
		{file == \preset} { modPath = dash ++ "Settings" ++ dash ++ "Presets" ++ dash}
		;
		thisPath ?? {if(file == \preset, {thisPath = filesPath}, {thisPath = mainPath});};
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
		/*path ?? {path = filePath};*/
		path ?? {path = Radicles.getLib("UserLib"); };
		if(path.isNumber, {
			path = ([filePath] ++ libArr)[path];
		});
		arrayFromFile = this.writeArray(path);
		if(arrayFromFile.notNil, {
			keyIndex = arrayFromFile.flop[0].indexOf(key);
			if(this.array.flop[0].includes(key), {
				if(window, {
					Window.warnQuestion(("This key already exists: " ++
						"Are you sure you want to replace it?"), {
						if(keyIndex.notNil, {
							arrayFromFile[keyIndex] = [key, dataArr];
							this.writeFunc(arrayFromFile, path, post);
							func.();
						}, {
							"This fx name already exists in another library".warn;
						});
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

	remove {arg key, window=true, func, path, post=true;
		var arrayFromFile, writeFunc, keyIndex, file;
		/*arrayFromFile = this.array;*/
		/*path ?? {path = filePath};*/
		path ?? {path = Radicles.getLib("UserLib")};
		if(path.isNumber, {
			path = ([filePath] ++ libArr)[path];
		});
		arrayFromFile = this.writeArray(path);

		if(arrayFromFile.notNil, {
			keyIndex = arrayFromFile.flop[0].indexOf(key);
			if(keyIndex.notNil, {
				if(window, {
					Window.warnQuestion(("Are you sure you want to remove this key?"), {
						arrayFromFile.removeAt(keyIndex);
						this.writeFunc(arrayFromFile, path, post);
						func.();
					});
				}, {
					arrayFromFile.removeAt(keyIndex);
					this.writeFunc(arrayFromFile, path, post);
					func.();
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

	loadLibs {arg file, class, exclude;
		var libsFolder, libsFiles, libFolderPath;
		libsFolder = PathName(libPath).folders;
		libsFolder = libsFolder.reject({|item| item.folderName == ".git" });
		if(libsFolder.notEmpty, {
			if(exclude.isNil, {
				libsFiles = libsFolder.collect{|item|
					libFolderPath  = item.pathOnly;
					libFolderPath = libFolderPath.copyFromStart(libFolderPath.size-2);
					this.whichFile(file, class, libFolderPath;);
				};
			}, {
				libsFolder.do{|item|
					if(exclude.indexOfEqual(item.folderName).notNil.not, {
						libFolderPath  = item.pathOnly;
						libFolderPath = libFolderPath.copyFromStart(libFolderPath.size-2);
						libsFiles = libsFiles.add(this.whichFile(file, class, libFolderPath;););
					});
				}
			});
			^libsFiles;
		}, {
			^libsFolder;
		});
	}

}

SynthFile : ModFile {

	*path {arg class=\filter, exclude;
		var synthFile;
		synthFile = this.new(\synth, class, exclude);
		^synthFile.filePath;
	}

	* read {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = this.new(\synth, class, exclude);
		^synthFile.read(key);
	}

	* postAll {arg class=\filter, exclude;
		this.read(class, exclude: exclude).do{|item| (item.cs ++ " -> ").post;
			this.post(class, item, exclude) }
	}

	* post {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = this.new(\synth, class, exclude);
		^synthFile.post(key);
	}

	* write {arg class=\filter, key, dataArr, win=true, path, post=true;
		var synthFile;
		synthFile = this.new(\synth, class);
		^synthFile.write(key, dataArr, win, path: path, post: post);
	}

	* remove {arg class=\filter, key, win=true, exclude, path, post;
		var synthFile;
		synthFile = this.new(\synth, class, exclude);
		^synthFile.remove(key, win, path: path, post: post);
	}

	* string {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = this.new(\synth, class, exclude);
		^synthFile.read(key).cs;
	}

}

SpecFile : ModFile {classvar specArr;

	*path {arg class=\filter, exclude;
		var synthFile;
		synthFile = this.new(\spec, class, exclude);
		^synthFile.filePath;
	}

	* read {arg class=\filter, key, warn=true, exclude;
		var synthFile;
		synthFile = this.new(\spec, class, exclude);
		^synthFile.read(key, warn);
	}

	* post {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = this.new(\spec, class, exclude);
		^synthFile.post(key);
	}

	* postAll {arg class=\filter, exclude;
		this.read(class, exclude: exclude).do{|item| (item.cs ++ " -> ").post;
			this.post(class, item, exclude) }
	}

	* write {arg class=\filter, key, dataArr, win=true, path, post=true;
		var synthFile;
		synthFile = this.new(\spec, class);
		^synthFile.write(key, dataArr, win, path: path, post: post);
	}

	* remove {arg class=\filter, key, win=true, exclude, path, post;
		var synthFile;
		synthFile = this.new(\spec, class, exclude);
		^synthFile.remove(key, win, path: path, post: post);
	}

	*specs {arg class=\filter, key, exclude;
		specArr = this.read(class, key, exclude).collect{ |item| item.funcSpec };
		^super.new(\spec, class, exclude);
	}

	map {arg index, value;
		^specArr[index].(value);
	}

	* string {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = this.new(\spec, class, exclude);
		^synthFile.read(key).cs;
	}

}

ControlFile : ModFile {

	*path {arg class=\map, exclude;
		var synthFile;
		synthFile = this.new(\control, class, exclude);
		^synthFile.filePath;
	}

	* read {arg class=\map, key, exclude;
		var synthFile;
		synthFile = this.new(\control, class, exclude);
		^synthFile.read(key);
	}

	* post {arg class=\map, key, exclude;
		var synthFile;
		synthFile = this.new(\control, class, exclude);
		^synthFile.post(key);
	}

	* postAll {arg class=\map, exclude;
		this.read(class, exclude: exclude).do{|item| (item.cs ++ " -> ").post;
			this.post(class, item, exclude) }
	}

	* write {arg class=\map, key, dataArr, win=true, path, post=true;
		var synthFile;
		synthFile = this.new(\control, class);
		^synthFile.write(key, dataArr, win, path: path, post: post);
	}

	* remove {arg class=\map, key, win=true, exclude, path, post;
		var synthFile;
		synthFile = this.new(\control, class, exclude);
		^synthFile.remove(key, win, path: path, post: post);
	}

	* string {arg class=\map, key, exclude;
		var synthFile;
		synthFile = this.new(\control, class, exclude);
		^synthFile.read(key).cs;
	}

}

DataFile : ModFile {

	*path {arg class=\sampler, exclude;
		var synthFile;
		synthFile = this.new(\data, class, exclude);
		^synthFile.filePath;
	}

	* read {arg class=\sampler, key, exclude;
		var synthFile;
		synthFile = this.new(\data, class, exclude);
		^synthFile.read(key);
	}

	* post {arg class=\sampler, key, exclude;
		var synthFile;
		synthFile = this.new(\data, class);
		^synthFile.post(key);
	}

	* postAll {arg class=\sampler, exclude;
		this.read(class, exclude: exclude).do{|item| (item.cs ++ " -> ").post;
			this.post(class, item, exclude) }
	}

	* write {arg class=\sampler, key, dataArr, win=true, path, post=true;
		var synthFile;
		synthFile = this.new(\data, class);
		^synthFile.write(key, dataArr, win, path: path, post: post);
	}

	* remove {arg class=\sampler, key, win=true, exclude, path, post;
		var synthFile;
		synthFile = this.new(\data, class, exclude);
		^synthFile.remove(key, win, path: path, post: post);
	}

	* string {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = this.new(\data, class, exclude);
		^synthFile.read(key).cs;
	}

}

DescriptionFile : ModFile {

	*path {arg class=\filter, exclude;
		var synthFile;
		synthFile = this.new(\description, class, exclude);
		^synthFile.filePath;
	}

	* read {arg class=\filter, key, warn=true, exclude;
		var synthFile;
		synthFile = this.new(\description, class, exclude);
		^synthFile.read(key, warn);
	}

	* post {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = this.new(\description, class, exclude);
		^synthFile.post(key);
	}

	* postAll {arg class=\filter, exclude;
		this.read(class, exclude: exclude).do{|item| (item.cs ++ " -> ").post;
			this.post(class, item, exclude) }
	}

	* write {arg class=\filter, key, dataArr, win=true, path, post=true;
		var synthFile;
		synthFile = this.new(\description, class);
		^synthFile.write(key, dataArr, win, path: path, post: post);
	}

	* remove {arg class=\filter, key, win=true, exclude, path, post;
		var synthFile;
		synthFile = this.new(\description, class, exclude);
		^synthFile.remove(key, win, path: path, post: post);
	}

	* string {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = this.new(\description, class, exclude);
		^synthFile.read(key).cs;
	}

}

PresetFile : ModFile {

	*path {arg class=\bstore, exclude;
		var presetFile;
		presetFile = this.new(\preset, class, exclude);
		^presetFile.filePath;
	}

	* read {arg class=\bstore, key, exclude;
		var presetFile;
		presetFile = this.new(\preset, class, exclude);
		^presetFile.read(key);
	}

	* post {arg class=\bstore, key, exclude;
		var presetFile;
		presetFile = this.new(\preset, class, exclude);
		^presetFile.post(key);
	}

	* postAll {arg class=\bstore, exclude;
		this.read(class, exclude: exclude).do{|item| (item.cs ++ " -> ").post;
			this.post(class, item, exclude) }
	}

	* readAll {arg class=\bstore, exclude;
		^this.read(class, exclude: exclude).collect{|item|
			[item, this.read(class, item, exclude)] }
	}

	* write {arg class=\bstore, key, dataArr, win=true, path, post=true;
		var presetFile;
		path ?? {path = 0};
		presetFile = this.new(\preset, class);
		^presetFile.write(key, dataArr, win, path: path, post: post);
	}

	* remove {arg class=\bstore, key, win=true, exclude, path, post;
		var presetFile;
		presetFile = this.new(\preset, class, exclude);
		^presetFile.remove(key, win, path: path, post: post);
	}

	* string {arg class=\filter, key, exclude;
		var presetFile;
		presetFile = this.new(\preset, class, exclude);
		^presetFile.read(key).cs;
	}

}

SynthDefFile {

	*path {arg class=\filter, exclude;
		var synthFile;
		synthFile = ModFile.new(\synthdef, class, exclude);
		^synthFile.filePath;
	}

	* read {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = ModFile.new(\synthdef, class, exclude);
		^synthFile.read(key);
	}

	* postAll {arg class=\filter, exclude;
		ModFile.read(class, exclude: exclude).do{|item| (item.cs ++ " -> ").post;
			ModFile.post(class, item, exclude) }
	}

	* post {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = ModFile.new(\synthdef, class, exclude);
		^synthFile.post(key);
	}

	* info {arg class=\filter, key, exclude;
		var string;
		string = [class, key, this.read(class, key, exclude),
			DescriptionFile.read(class, key, exclude: exclude)].cs;
		string = string.replaceAt("(", 0).replaceAt(")", string.size-1);
		^string;
	}

	* write {arg class=\filter, key, dataArr, desc, path, post=true;
		var synthFile;
		synthFile = ModFile.new(\synthdef, class);
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

	* remove {arg class=\filter, key, exclude, path;
		var synthFile;
		Window.warnQuestion(("Are you sure you want to remove this key?"), {
		SynthFile.remove(class, key, false, exclude, path, false);
		SpecFile.remove(class, key, false, exclude, path, false);
		DescriptionFile.remove(class, key, false, exclude, path, false);
			synthFile = ModFile.new(\synthdef, class, exclude);
		 /*path ?? {path = synthFile.filePath};*/
			path ?? {path = Radicles.getLib("UserLib")};
		 if(path.isNumber, {
		 	path = ([synthFile.filePath] ++ synthFile.libArr)[path];
		 });
			 synthFile.remove(key, false, exclude, path);
		});
	}

	* string {arg class=\filter, key, exclude;
		var synthFile;
		synthFile = ModFile.new(\synthdef, class, exclude);
		^synthFile.read(key).cs;
	}

	* list {arg class=\filter, key, exclude;
		var synthFile, list;
		synthFile = ModFile.new(\synthdef, class, exclude);
		list = synthFile.read(key).sort;
		list.dopostln;
		^list;
	}

}