BStore : Store {classvar <playPath, <samplePath, <>playFolder=0, <>playFormat=\audio;
	classvar <>sampleFormat=\audio, <bufAlloc;

	*add {arg type, settings, function;
		var format, path, boolean;

		//get paths for Bufferst that require on
		case
		{type == \play} {
			format = playFormat;
			path = this.getPlayPath(format, settings);
		}
		{type == \sample} {
			format = sampleFormat;
			path = this.getSamplePath(format, settings);
		};

		case
		{type == \play} {
			PlayStore.add(settings, path, {|buf|
				if(boolean, {
					stores = stores.add(buf);
				});
				function.(buf);
			});
		}
		{type == \sample} {
			SampleStore.read(settings, format)
		}
		{type == \alloc} {
			"alloc function".postln;
		};

		boolean = this.store(\bstore, type, format, settings);
	}

	/*	*bstores {var resultArr, storeArr;
	storeArr = this.indexArr;
	storeArr.do{|item, index|
	if((item[1][0] == \bstore), {
	resultArr = resultArr.add(item);
	});
	}
	^resultArr;
	}*/

	/*	*indexArr {var arr, result, bstoreArr;
	arr = this.bstores;
	if(arr.notNil, {
	bstoreArr = this.bstores.flop[1];
	result = ([Array.series(bstoreArr.size)] ++ [bstoreArr]).flop;
	});
	^result;
	}*/

	/*	*info {var arr;
	arr = this.indexArr;
	if(arr.notNil, {
	arr.postin(\ide, \doln);
	}, {
	"No active bstores".warn;
	});
	}*/

	/*	*storeIndeces {var arr, resultArr;
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
	}*/

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

		}, {
			"not a recognized audio format".warn;
		});

		^folderPath.asString;
	}
}

PlayStore : BStore {

	*add {arg settings, path, function;
		BufferSystem.add(settings, path, function);
	}

	*remove {
		"remove play store".postln;
	}

}

SampleStore : BStore {

	*add {arg settings;

		settings.postln;
	}

	*remove {
		"remove sample store".postln;
	}

}