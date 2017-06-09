BStore : Store {classvar <playPath, <samplerPath, <>playFolder=0, <>playFormat=\audio;
	classvar <>samplerFormat=\audio, <bufAlloc;

	*add {arg type, settings, function;
		var format, path, boolean, typeStore, newSettings;

		//get paths for Bufferst that require on
		case
		{type == \play} {
			format = playFormat;
			path = this.getPlayPath(format);
		}
		{type == \sampler} {
			format = samplerFormat;
			path = this.getsamplerPath(format, settings);
		}
		{type == \alloc} {
			format = settings[0];
			newSettings = settings.copyRange(1,2);
		};

/*		boolean = this.store(\bstore, type, format, settings);*/

		case
		{type == \play} {
			typeStore = PlayStore.add(settings, path, {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, settings);
				if(boolean, {
					stores = stores.add(buf);
					});
				});
				function.(buf);
			});
		}
		{type == \sampler} {
			SamplerStore.add(settings, format)
		}
		{type == \alloc} {
			typeStore = AllocStore.add(newSettings, function: {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, newSettings);
				if(boolean, {
					stores = stores.add(buf);
					});
				});
				function.(buf);
			});
		};
	}

/*	*removeAt {arg index;
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

		*getDirPath {arg format=\audio, directory, subDir;
		var folderPath, mainClass, fileIndex, selectedPath;
		mainClass = this.new;
		playPath = mainClass.mainPath ++ directory;

		if([\audio, \scpv].includes(format), {
			if(format == \audio, {
				folderPath = (playPath ++ subDir.asString);
			}, {
				folderPath = (playPath ++ "scpv/" ++ subDir.asString);
			});

		}, {
			"not a recognized audio format".warn;
		});

		^folderPath.asString;
	}

		*getPlayPath {arg format=\audio, fileName=\test;
		^this.getDirPath(format, "SoundFiles/Play/", playFolder);
	}

	*getSamplerPath {arg format=\audio, samplerName=\str;
		^this.getDirPath(format, "SoundFiles/Sampler/", "");
	}

}

PlayStore : BStore {

	*add {arg settings, path, function;
		^BufferSystem.add(settings, path, function);
	}

	*remove {
		"remove play store".postln;
	}

}

SamplerStore : BStore {

	*add {arg settings;
		DataFile.read(\sampler, \mbx);
		settings.postln;
	}

	*remove {
		"remove sampler store".postln;
	}
}

AllocStore : BStore {

	*add {arg settings, function;
		^BufferSystem.add(settings[0], settings[1], function);
	}

	*remove {
		"remove alloc store".postln;
	}

}