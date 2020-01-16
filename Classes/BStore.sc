BStore : Store {classvar <playPath, <samplerPath, <>playFolder=0, <>playFormat=\audio;
	classvar <>samplerFormat=\audio, <>diskStart=0, <>diskBufSize=1, <>cueCount=1, <>allocCount=1;

	*addRaw {arg type, format, settings, function;
		var path, boolean, typeStore, existFormat, bstoreIDs;
		this.updateFree;
		bstoreIDs = this.bstoreIDs;
		case
		{type == \play} {
			path = this.getPlayPath(format);
		}
		{type == \sampler} {
			path = this.getSamplerPath(format, settings);
		}
		{type == \alloc} {
			path = settings;
		}
		{type == \cue} {
			path = this.getPlayPath(\audio);
		}
		{type == \ir} {
			path = this.getIRPath;
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
			if(bstoreIDs.notNil, {
			existFormat = bstoreIDs.flop[1].indexOf(format);
			});
			if(existFormat.notNil, {
				typeStore = this.buffByID(bstoreIDs[existFormat]);
				("//Buffer already been used as: ~buffer" ++
					typeStore.bufnum).radpost;
			}, {
			typeStore = this.addAlloc(path, function: {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, path);
					if(boolean, {
						stores = stores.add(buf);
					});
				});
				function.(buf);
			});
			});
		}
		{type == \cue} {
			if(bstoreIDs.notNil, {
			existFormat = bstoreIDs.flop[1].indexOf(format);
			});
			if(existFormat.notNil, {
				typeStore = this.buffByID(bstoreIDs[existFormat]);
				("//Buffer already been used as: ~buffer" ++
					typeStore.bufnum).radpost;
			}, {
			typeStore = this.addCue(settings, path, function: {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, settings);
					if(boolean, {
						stores = stores.add(buf);
					});
				});
				function.(buf);
			});
				});
		}
		{type == \ir} {
			typeStore = this.addIR(settings, path, {|buf|
				if(typeStore.notNil, {
					boolean = this.store(\bstore, type, format, settings);
					if(boolean, {
						stores = stores.add(buf);
					});
				});
				function.(buf);
			});
		}
		;
	}

	*add {arg type, settings, function;
		var format, newSettings;

		case
		{(type == \play)} {
			format = playFormat;
			newSettings = settings;
		}
		{type == \sampler} {
			format = samplerFormat;
			newSettings = settings;
		}
		{type == \alloc} {
			format = settings[0];
			newSettings = settings.copyRange(1,3);
		}
		{type == \cue} {
			format = settings[0];
			newSettings = settings[1];
		}
		{type == \ir} {
			format = \audio;
			newSettings = settings;
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

	*remove {arg type, format, settings, action;
		var bstoreIDs, bstoreIndex, bstores, thisBStore, freeBufArr, cond;
		{
		cond = Condition(false);
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
			{ [\play, \alloc, \cue, \ir].includes(type)} {
				BufferSystem.freeAt(BufferSystem.bufferArray.indexOf(thisBStore), {
						cond.test = true; cond.signal;
					});
					this.removeAt(bstoreIndex);
					cond.wait;
					action.();
			}
			{type == \sampler} {
				thisBStore.do{|item|
					if((bstores.flat.indicesOfEqual( item).size > 1).not, {
						freeBufArr = freeBufArr.add(BufferSystem.bufferArray.indexOf(item));
					});
				};
				BufferSystem.freeAtAll(freeBufArr.sort, {
						cond.test = true; cond.signal;
					});
					this.removeAt(bstoreIndex);
					cond.wait;
					action.();
			}
		});
	}.fork;
	}

	*removeID {arg ids;
		var currentIDs;
		currentIDs = this.bstoreIDs;
		if(currentIDs.notNil, {
		if(currentIDs.indexOfEqual(ids).notNil, {
		this.remove(ids[0], ids[1], ids[2]);
		});
		});
	}

	*removeAll {
		this.removeBStores;
	}

	*removeByIndex {arg index;
		var ids;
		ids = this.bstoreIDs[index];
		if(ids.notNil, {
			this.remove(ids[0], ids[1], ids[2]);
		});
	}

	*removeIndices {arg indices;
		var count=0;
		indices.do{|item|
				this.removeByIndex(item-count);
				count = count+1;
			}
	}

	*removeByArg {arg argument, index;
		var indices, count=0;
		indices = this.bstoreIDs.flop[index].indicesOfEqual(argument);
		if(indices.notNil, {
			this.removeIndices(indices);
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

	*updateFree {
		this.bstores.do{|buf, ind| if(buf.bufnum.isNil, {this.removeByIndex(ind); });};
	}

	*getDirPath {arg format=\audio, directory, subDir;
		var folderPath, fileIndex, selectedPath, dash;
		dash = "/";
		/*Platform.case(
			\windows,   {dash = "\\"; "Windows".postln }
		);*/
		this.new;
		playPath = this.soundFilePath ++ dash ++ directory;

		if([\audio, \scpv].includes(format), {
			if(format == \audio, {
				folderPath = (playPath ++ subDir.asString);
			}, {
				folderPath = (playPath ++ "scpv" ++ dash ++ subDir.asString);
			});

		}, {
			"not a recognized audio format".warn;
		});

		^folderPath.asString;
	}

	*getPlayPath {arg format=\audio, fileName=\test;
		var dash;
		dash = "/";
		/*Platform.case(
			\windows,   {dash = "\\"; "Windows".postln }
		);*/
		^this.getDirPath(format, ("Play" ++ dash), playFolder);
	}

	*getSamplerPath {arg format=\audio, samplerName=\str;
		var dash;
		dash = "/";
		/*Platform.case(
			\windows,   {dash = "\\"; "Windows".postln }
		);*/
		^this.getDirPath(format, ("Sampler" ++ dash), "");
	}

		*getIRPath {arg format=\audio, samplerName=\str;
		var dash, irpath;
		dash = "/";
		/*Platform.case(
			\windows,   {dash = "\\"; "Windows".postln }
		);*/
		this.new;
		irpath = (this.soundFilePath ++ dash ++ "IR" ++ dash);
		^irpath;
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
		^BufferSystem.add(settings[0], settings[1], function, settings[2]);
	}

	*addCue {arg settings, path, function;
		^BufferSystem.add([settings, 'cue', [diskStart,diskBufSize]], path, function);
	}

		*addIR {arg settings, path, function;
		^BufferSystem.add(settings, path, function);
	}

	*buffByArg {arg argument, index;
		var indices, buffs, result, count=0;
		indices = this.bstoreIDs.flop[index].indicesOfEqual(argument);
		if(indices.notNil, {
			buffs = this.bstores.atAll(indices);
			if(buffs.size == 1, {
				result = buffs[0];
			}, {
				result = buffs;
			});
			^result;
		});
	}

	*buffByType {arg type;
		^this.buffByArg(type, 0);
	}

	*buffByFormat {arg format;
		^this.buffByArg(format, 1);
	}

	*buffBySetting {arg setting;
		^this.buffByArg(setting, 2);
	}

	*buffByID {arg bstoreID;
		^this.bstores[this.bstoreIDs.indexOfEqual(bstoreID)];
	}

	*bstoreTags {
		var newArr;
		this.bstoreIDs.do{|item| newArr = newArr.add(item.flat)};
		^newArr;
	}

	*buffByTag {arg bstoreID;
		^this.bstores[this.bstoreTags.indexOfEqual(bstoreID)];
	}

		*setBufferID {arg buffer, blockFuncString;
		var storeType, bufferID;
		case
		{buffer.isNumber} {
			storeType = \alloc;
			buffer = [(\alloc++allocCount).asSymbol, buffer];
			bufferID = [storeType, buffer[0], [buffer[1]] ];
			allocCount = allocCount + 1;
		}
		{buffer.isSymbol} {
			case
			{(blockFuncString.find("PlayBuf.ar(")).notNil} {
				storeType = \play;
				BStore.playFormat = \audio;
				bufferID = [storeType, \audio, buffer].flat;
			}
			{blockFuncString.find("PV_PlayBuf").notNil} {
				storeType = \play;
				BStore.playFormat = \scpv;
				bufferID = [storeType, \scpv, buffer];
			}
			{blockFuncString.find("DiskIn.ar(").notNil} {
				storeType = \cue;
				buffer = [(\cue++cueCount).asSymbol, buffer].flat;
				bufferID = [storeType, buffer].flat;
				cueCount = cueCount + 1;
			};
		};
		^[storeType, buffer, bufferID];
	}

	*buffStrByID {arg bstoreID;
		var buffer, bufIndex, bufString;
		buffer = this.buffByID(bstoreID);
		^BufferSystem.getGlobVar(buffer);
	}

		*buffIDstoStrArr {arg bstoreIDArr;
		^bstoreIDArr.collect({|item| this.buffStrByID(item) });
	}

}