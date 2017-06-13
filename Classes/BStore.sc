BStore : Store {classvar <playPath, <samplerPath, <>playFolder=0, <>playFormat=\audio;
	classvar <>samplerFormat=\audio, <bufAlloc;

	*addRaw {arg type, format, settings, function;
		var path, boolean, typeStore;

		case
		{type == \play} {
			path = this.getPlayPath(format);
		}
		{type == \sampler} {
			path = this.getSamplerPath(format, settings);
		}
		{type == \alloc} {
			path = settings;
			/*path = settings.copyRange(1,2);*/
		};

		case
		{type == \play} {
			typeStore = this.addPlay(settings, path, {|buf|
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
			typeStore = this.addSampler(settings, path, {|buf|
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
			typeStore = this.addAlloc(path, function: {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, path);
					if(boolean, {
						stores = stores.add(buf);
					});
				});
				function.(buf);
			});
		};
	}

	*add {arg type, settings, function;
		var format, path, newSettings;

		case
		{type == \play} {
			format = playFormat;
			newSettings = settings;
		}
		{type == \sampler} {
			format = samplerFormat;
			newSettings = settings;
		}
		{type == \alloc} {
			format = settings[0];
			newSettings = settings.copyRange(1,2);
		};

		this.addRaw(type, format, newSettings, function);
	}

	*addAll {arg array, function;
		var arr, cond;
		cond = Condition(false);
		{
			array.do{|item|
				cond.test = false;
				BStore.addRaw(item[0], item[1], item[2], {|buf|
					arr = arr.add(buf);
					cond.test = true;
					cond.signal;
				});
				cond.wait;
			};
			function.(arr);
		}.fork;
	}

	*remove {arg type, format, settings;
		var bstoreIDs, bstoreIndex, bstores, thisBStore, freeBufArr;
		bstoreIDs = this.bstoreIDs;
		bstores = this.bstores;
		if(type == \alloc, {
			bstoreIndex = bstoreIDs.flop[1].indexOfEqual(format);
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
				thisBStore.do{|item|
					if((bstores.flat.indicesOfEqual( item).size > 1).not, {
						freeBufArr = freeBufArr.add(BufferSystem.bufferArray.indexOf(item));
					});
				};
				BufferSystem.freeAtAll(freeBufArr.sort);
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

	*removeByArg {arg argument, index;
		var indices, count=0;
		indices = this.bstoreIDs.flop[index].indicesOfEqual(argument);
		if(indices.notNil, {
			indices.do{|item|
				this.removeByIndex(item-count);
				count = count+1;
			}
		}, {
			"BStore not found".warn;
		});
	}

	*removeByType {arg type;
		this.removeByArg(type, 0);
	}

	*removeByFormat {arg format;
		this.removeByArg(format, 1);
	}

	*removeBySetting {arg setting;
		this.removeByArg(setting, 2);
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

	*addPlay {arg settings, path, function;
		^BufferSystem.add(settings, path, function);
	}

	*addSampler {arg settings, path, function;
		var samplerArr;
		if(DataFile.read(\sampler).includes(settings), {
			samplerArr = DataFile.read(\sampler, settings);
			^BufferSystem.addAll(samplerArr, path, function);
		}, {
			"Sampler not found".warn;
		});
	}

	*addAlloc {arg settings, function;
		^BufferSystem.add(settings[0], settings[1], function);
	}

}