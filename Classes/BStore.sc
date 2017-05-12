BStore : Store {classvar <playPath, <samplePath, <>playFolder=0, <>playFormat=\audio,
	<>sampleFormat=\audio, condition, <bufferArray, <bufAlloc;

	*add {arg type, settings, function;
		var bstore, format, path, currentArr, boolean, bufInfo;

		case
		{type == \play} {
			format = playFormat;
			path = this.getPlayPath(format, settings);
		}
		{type == \sample} {
			format = sampleFormat;
			path = this.getSamplePath(format, settings);
		};

		condition = Condition.new;
		currentArr = this.indexArr;

		if(currentArr.isNil, {
			boolean = true;
		}, {
			boolean = currentArr.flop[1].includesEqual([\bstore, type, format, settings]).not;
		});

		if(boolean, {
			case
			{type == \play} {
				bstore = PlayStore.read(path, function);
			}
			{type == \sample} {
				/*bstore = SampleStore.read(settings, format) */
			};

			this.new(\bstore, type, format, settings);

			stores = stores.add(bstore);

		}, {
			if(function.notNil, {

				bufferArray.do{|item|
					if(path == item.path, {
						bufInfo = [item.numChannels, item.bufnum, item.numFrames, item.sampleRate];
					});
				};

				function.value(bufInfo[0], bufInfo[1], bufInfo[2], bufInfo[3]);
			}, {
				"BStore already exists".warn;
			});
		});

	}

	*bstores {var resultArr, storeArr;
		storeArr = this.indexArr;
		storeArr.do{|item, index|
			if((item[1][0] == \bstore), {
				resultArr = resultArr.add(item);
			});
		}
		^resultArr;
	}

	*indexArr {var arr, result, bstoreArr;
		arr = this.bstores;
		if(arr.notNil, {
			bstoreArr = this.bstores.flop[1];
			result = ([Array.series(bstoreArr.size)] ++ [bstoreArr]).flop;
		});
		^result;
	}

	*info {var arr;
		arr = this.indexArr;
		if(arr.notNil, {
			arr.postin(\ide, \doln);
		}, {
			"No active bstores".warn;
		});
	}

	*storeIndeces {var arr, resultArr;
		arr = this.bstores;
		if(arr.notNil, {
			resultArr = arr.flop[0];
		});
		^resultArr;
	}

	*removeAt {arg index;
		var arr;
		arr = this.storeIndeces;
		if(arr.notNil, {
			if((index > (arr.size-1)).or(index.isNegative), {
				"Index out of bounds".warn;
			}, {
				super.removeAt(arr[index]);
			});
		}, {
			"No active bstores".warn;
		});
	}

	*getPlayPath {arg format=\audio, fileName=\test;
		var folderPath, mainClass, fileIndex, selectedPath;
		mainClass = this.new;
		playPath = mainClass.mainPath ++ "SoundFiles/Play/";

		if([\audio, \scpv].includes(format), {
			if(format == \audio, {
				folderPath = (playPath ++ playFolder.asString);
			}, {
				folderPath = (playPath ++ "scpv/" ++ playFolder.asString);
			});

			folderPath.fileNameWithoutExtension.do{|item, index|
				if(item.asSymbol == fileName, {
					fileIndex = index;
				});
			};

			if(fileIndex.notNil, {
				selectedPath = folderPath.folderContents[fileIndex]
			}, {
				"Incorrect fileName, soundfile does not exist".warn;
			});
		}, {
			"not a recognized audio format".warn;
		});

		^selectedPath.asString;
	}

	*contents {
		^bufferArray;
	}

}

PlayStore : BStore {

	*read {arg pathName, function;
		var main, s, buffer;
		main = this.new;
		s = main.server;
		s.makeBundle(nil, {
			{
				bufAlloc = true;
				buffer = Buffer.read(s, pathName).postln;
				bufferArray = bufferArray.add(buffer);
				s.sync(condition);
				bufAlloc = false;
				function.value(buffer.numChannels, buffer.bufnum, buffer.numFrames, buffer.sampleRate);
			}.fork;
		});

		^pathName;
	}

	*remove {
		"remove play store".postln;
	}

}

SampleStore : BStore {

	*read {arg settings;

		settings.postln;
	}

	*remove {
		"remove sample store".postln;
	}

}