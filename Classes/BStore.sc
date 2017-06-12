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
			path = this.getSamplerPath(format, settings);
		}
		{type == \alloc} {
			format = settings[0];
			newSettings = settings.copyRange(1,2);
		};

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
			typeStore = SamplerStore.add(settings, path, {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, settings);
					if(boolean, {
						stores = stores.add(buf);
					});
				});
				function.(buf);
			});
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

	*remove {arg type, format, settings;
		var bstoreIDs, bstoreIndex, bstores, thisBStore, freeBufArr;
		bstoreIDs = this.bstoreIDs;
		bstores = this.bstores;
		if(type == \alloc, {
			bstoreIndex = bstoreIDs.flop[1].indexOfEqual(format);
			/*bstoreIndex = bstoreIDs.indexOfEqual([type, format, settings]);*/
		}, {
			bstoreIndex = bstoreIDs.indexOfEqual([type, format, settings]);
		});
		if(bstoreIndex.notNil, {
			thisBStore = bstores[bstoreIndex];

			case
			{type == \play} {
				BufferSystem.freeAt(BufferSystem.bufferArray.indexOf(thisBStore));
				this.removeAt(bstoreIndex);

			}
			{type == \sampler} {
				thisBStore.do{|item| item.postln;
					if(bstores.flat.indicesOfEqual( item).size > 1, {
						"dont' free".postln;
					}, {
						freeBufArr = freeBufArr.add(BufferSystem.bufferArray.indexOf(item));
						"free".postln;
					});
				};
				BufferSystem.freeAtAll(freeBufArr);
				this.removeAt(bstoreIndex);
			}
			{type == \alloc} {
				BufferSystem.freeAt(BufferSystem.bufferArray.indexOf(thisBStore));
				this.removeAt(bstoreIndex);
			};

		}, {
			"BStore not found".warn;
		});
	}

	*removeByIndex {arg index;
		var ids;
		ids = this.bstoreIDs[index];
		if(ids.notNil, {
			this.remove(ids[0], ids[1], ids[2]);
		}, {
			"BStore not found".warn;
		});
	}

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

	*add {arg settings, path, function;
		var samplerArr;
		if(DataFile.read(\sampler).includes(settings), {
			samplerArr = DataFile.read(\sampler, settings);
			^BufferSystem.addAll(samplerArr, path, function);
		}, {
			"Sampler not found".warn;
		});
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