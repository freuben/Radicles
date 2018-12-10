BufferSystem : Radicles {classvar <bufferArray, <globVarArray,
	<tags, countTag=0, <bufAlloc, <>defaultPath,
	<>postWin, countCue=0,  bufcount=0;

	*bufferCount {
		if(bufcount < server.options.numBuffers, {
			bufcount = bufcount + 1;
		}, {
			bufcount = 0;
		});
	}

	*read {arg pathName, function, channels;
		var buffer, globVar;
		this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					globVar = "~buffer" ++ bufcount;
					globVarArray = globVarArray.add(globVar);
					if(channels.isNil, {
						(globVar ++ " = " ++ "Buffer.read(s, " ++ pathName.cs ++
							", bufnum: " ++ bufcount ++ ");").radpost.interpret;
					}, {
						(globVar ++ " = " ++ "Buffer.readChannel(s, " ++ pathName.cs ++
							", bufnum: " ++ bufcount ++ ", channels: " ++ channels ++ ");")
						.radpost.interpret;
					});
					buffer = globVar.interpret;
					bufferArray = bufferArray.add(buffer);
					this.bufferCount;
					if(channels.isNil, {
						tags = tags.add(this.pathToTag(pathName));
					}, {
						tags = tags.add((this.pathToTag(pathName) ++
							channels).asSymbol);
					});
					server.sync;
					bufAlloc = false;
					function.(buffer);
				}.fork;
			});
		}, {"Server not running".warn});
	}

	*readAll {arg pathArr, function, channelsArr;
		var buffer, returnArray, globVar;
		this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					pathArr.do{|item, index|
						globVar = "~buffer" ++ bufcount;
						globVarArray = globVarArray.add(globVar);
						if(channelsArr.isNil, {
							(globVar ++ " = " ++ "Buffer.read(s, " ++ item.cs ++
								", bufnum: " ++ bufcount ++ ");").radpost.interpret;
						}, {
							if(channelsArr[index].isNil, {
								(globVar ++ " = " ++ "Buffer.read(s, " ++ item.cs ++
									", bufnum: " ++ bufcount ++ ");").radpost.interpret;
							}, {
								(globVar ++ " = " ++ "Buffer.readChannel(s, " ++ item.cs ++
									", bufnum: " ++ bufcount ++ ", channels: " ++
									channelsArr[index] ++ ");").radpost.interpret;
							});
						});
						buffer = globVar.interpret;
						bufferArray = bufferArray.add(buffer);
						this.bufferCount;
						if(channelsArr.isNil, {
							tags = tags.add(this.pathToTag(item));
						}, {
							if(channelsArr[index].isNil, {
								tags = tags.add(this.pathToTag(item));
							}, {
								tags = tags.add((this.pathToTag(item) ++  channelsArr[index])
									.asSymbol);
							});
						});
						returnArray = returnArray.add(buffer);
						server.sync;
					};
					function.(returnArray);
					bufAlloc = false;
				}.fork;
			});
		}, {"Server not running".warn});
	}

	*readDir {arg path, function, channelsArr;
		var myPath, newArr;
		if(path.notNil, {
			myPath = PathName.new(path);
			myPath.files.do{|item| newArr = newArr.add(item.fullPath)};
			this.readAll(newArr, function, channelsArr);
		}, {
			"Not path specified".warn;
		});
	}

	*bufferInfo {var array, tag, count=0;
		bufferArray.do{|item|
			if(item.path.notNil, {
				tag = this.pathToTag(item.path);
			}, {
				tag = ("alloc" ++ count).asSymbol;
				count = count + 1;
			});
			array = array.add([tag, item.numChannels, item.bufnum,
				item.numFrames, item.sampleRate, item.path]);
		};
		^array;
	}

	*bufferPaths {var arr;
		bufferArray.do{|item|
			if(item.path.notNil, {
				arr = arr.add([item.path, item]);
			});
		};
		^arr;
	}

	*getPath {arg fileName=\test, pathDir;
		var folderPath, fileIndex, selectedPath;
		var myPath, newArray, newerArr;
		myPath = PathName.new(pathDir);
		myPath.files.do{|item| newArray =
			newArray.add(item.fileNameWithoutExtension)};
		newArray.do{|item, index|
			if(item.asSymbol == fileName, {
				fileIndex = index;
			});
		};

		if(fileIndex.notNil, {
			myPath = PathName.new(pathDir);
			myPath.files.do{|item| newerArr = newerArr.add(item.fullPath)};
			selectedPath = newerArr[fileIndex]
		}, {
			"Incorrect fileName, soundfile does not exist".warn;
		});

		^selectedPath;
	}

	*alloc {arg numFrames, numChannels, function;
		var buffer, globVar;
		this.new;
		numFrames ?? {numFrames = 44100};
		numChannels ?? {numChannels = 1};
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					globVar = "~buffer" ++ bufcount;
					globVarArray = globVarArray.add(globVar);
					(globVar ++ " = " ++ "Buffer.alloc(s, " ++ numFrames ++ ", " ++
						numChannels ++ ", bufnum: " ++ bufcount ++ ");").radpost.interpret;
					buffer = globVar.interpret;
					bufferArray = bufferArray.add(buffer);
					this.bufferCount;
					tags = tags.add( ("alloc" ++ countTag).asSymbol );
					countTag = countTag + 1;
					server.sync;
					bufAlloc = false;
					function.(buffer);
				}.fork;
			});
		}, {
			"Server is not running".warn;
		});
	}

	*allocAll {arg argArr, function;
		var buffer, returnArr, globVar;
		this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					argArr.do{|item|
						item[0] ?? {item[0] = 44100};
						item[1] ?? {item[1] = 1};
						globVar = "~buffer" ++ bufcount;
						globVarArray = globVarArray.add(globVar);
						(globVar ++ " = " ++ "Buffer.alloc(s, " ++ item[0] ++ ", " ++
							item[1] ++ ", bufnum: " ++ bufcount ++ ");").radpost.interpret;
						buffer = globVar.interpret;
						bufferArray = bufferArray.add(buffer);
						this.bufferCount;
						tags = tags.add( ("alloc" ++ countTag).asSymbol );
						countTag = countTag + 1;
						returnArr = returnArr.add(buffer);
						server.sync;
					};
					bufAlloc = false;
					function.(returnArr);
				}.fork;
			});
		}, {
			"Server is not running".warn;
		});
	}

	*cue {arg pathName, startFrame=0, bufSize=1, function;
		var buffer, chanNum, cueSize, globVar;
		this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					chanNum = this.fileNumChannels(pathName);
					cueSize = 32768*bufSize;
					globVar = "~buffer" ++ bufcount;
					globVarArray = globVarArray.add(globVar);
					(globVar ++ " = " ++ "Buffer.cueSoundFileBuf(s, " ++ pathName.cs ++
						", " ++ startFrame ++ ", " ++ chanNum ++  ", " ++ cueSize ++
						", bufnum: " ++ bufcount ++ ");").radpost.interpret;
					buffer = globVar.interpret;
					bufferArray = bufferArray.add(buffer);
					this.bufferCount;
					tags = tags.add(("disk" ++ countCue ++ "_" ++
						this.pathToTag(pathName)).asSymbol);
					countCue = countCue + 1;
					server.sync;
					bufAlloc = false;
					function.(buffer);
				}.fork;
			});
		}, {"Server not running".warn});
	}

	*cueAll {arg arr, function;
		//arr: [ [path1, [startFrame1, bufSize1], [path2, [startFrame2, bufSize2]...]
		var buffer, returnArray, file, chanNum, newArr, globVar, cueSize;
		this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					arr.do{|item|
						chanNum = this.fileNumChannels(item[0]);
						if(item[1].notNil, {
							newArr = item[1];
						}, {
							newArr = [0,1];
						});
						cueSize = 32768*newArr[1];
						globVar = "~buffer" ++ bufcount;
						globVarArray = globVarArray.add(globVar);
						(globVar ++ " = " ++ "Buffer.cueSoundFileBuf(s, " ++ item[0].cs ++
							", " ++ newArr[0] ++ ", " ++ chanNum ++  ", " ++ cueSize ++
							", bufnum: " ++ bufcount ++ ");").radpost.interpret;
						buffer = globVar.interpret;
						bufferArray = bufferArray.add(buffer);
						this.bufferCount;
						tags = tags.add(("disk" ++ countCue ++ "_" ++
							this.pathToTag(item[0])).asSymbol);
						countCue = countCue + 1;
						returnArray = returnArray.add(buffer);
						server.sync;
					};
					function.(returnArray);
					bufAlloc = false;
				}.fork;
			});
		}, {"Server not running".warn});
	}

	*add {arg arg1, arg2, function;
		var getPath, getIndex, getBufferPaths, cueBool, buffunction, readChanBuf;
		if(arg1.isNumber, {
			//allocate buffer: arg1: frames, arg2: channels, arg3: bufnum
			this.alloc(arg1, arg2, function)
		}, {
			//read buffer: arg1: fileName, arg2: pathDir
			arg2 ?? {arg2 = defaultPath};
			if(arg2.notNil, {
				cueBool = arg1.isArray.not;
				if(cueBool, {
					getPath = this.getPath(arg1, arg2);
				}, {
					getPath = this.getPath(arg1[0], arg2);
				});
				buffunction = {
					if(cueBool, {
						this.read(getPath, function);
					}, {
						if(arg1[1].isSymbol, {
							this.cue(getPath, arg1[2][0], arg1[2][1], function);
						}, {
							this.read(getPath, function, arg1[1]);
						});
					});
				};
				if(getPath.notNil, {
					getBufferPaths = this.bufferPaths;
					if(getBufferPaths.notNil, {
						getIndex = getBufferPaths.flop[0].indexOfEqual(getPath);
						if(getIndex.isNil, {
							buffunction.();
						}, {
							if(cueBool, {

								if(tags.includes(arg1), {
									("//File already allocated as: ~buffer" ++
										this.get(arg1).bufnum ).radpost;
									function.(this.get(arg1));
								}, {
									buffunction.();
								});
							}, {
								readChanBuf = ((arg1[0] ++ arg1[1]).asSymbol);
								if(tags.includes(readChanBuf), {
									("//File already allocated as: ~buffer" ++
										this.get(readChanBuf).bufnum ).radpost;
									function.(this.get(readChanBuf));
								}, {
									buffunction.();
								});
							});
						});
					}, {
						buffunction.();
					});
				});
			}, {
				"No path selected".warn;
			});
		});
	}

	*addAllPaths {arg arr, path, function;
		var allBuffers;
		{
			path ?? {path = defaultPath!arr.size};
			arr.do{|item, index|
				if(item.isArray.not, {
					this.add(item, path[index], {|buf| allBuffers = allBuffers.add(buf)} );
				}, {
					if(item[0].isNumber, {
						this.add(item[0], item[1], {|buf| allBuffers = allBuffers.add(buf)});
					}, {
						this.add(item, path[index], {|buf| allBuffers = allBuffers.add(buf)});
					});
				});
				server.sync;
			};
			server.sync;
			if(allBuffers.notNil, {
			function.(allBuffers.rejectSame);
			}, {
				function.();
			});
		}.fork;
	}

	*addAll {arg arr, path, function;
		var allBuffers;
		{
			path ?? {path = defaultPath};
			arr.do{|item|
				if(item.isArray.not, {
					this.add(item, path, {|buf| allBuffers = allBuffers.add(buf)} );
				}, {
					if(item[0].isNumber, {
						this.add(item[0], item[1], {|buf| allBuffers = allBuffers.add(buf)});
					}, {
						this.add(item, path, {|buf| allBuffers = allBuffers.add(buf)});
					});
				});
				server.sync;
			};
			server.sync;
			if(allBuffers.notNil, {
			function.(allBuffers.rejectSame);
			}, {
				function.();
			});
		}.fork;
	}

	*addPairs {arg arr, function, readChansArr;
		var newArr;
		if(arr.indexOf(nil).notNil.not, {
			if(arr.flat[0].isNumber, {
				//allocate arr: [frames, channels]
				this.addAllPaths(arr.clump(2), function: function)
			}, {
				newArr = arr.clump(2).flop;
				this.addAllPaths(newArr[0], newArr[1], function, readChansArr);
			});
		}, {
			"Not all infomation is specified".warn;
		});
	}

	*addDir {arg path, function;
		var myPath, newArr;
		path ?? {path = defaultPath};
		if(path.notNil, {
			myPath = PathName.new(path);
			myPath.files.do{|item|
				newArr = newArr.add(item.fileNameWithoutExtension.asSymbol;
			)};
			this.addAll(newArr, path, function);
		}, {
			"Not path specified".warn;
		});
	}

	*cueDir {arg path, startFrame=0, bufSize=1, function;
		var myPath, newArr;
		path ?? {path = defaultPath};
		if(path.notNil, {
			myPath = PathName.new(path);
			myPath.files.do{|item|
				newArr = newArr.add(
					[item.fileNameWithoutExtension.asSymbol, 'cue', [startFrame, bufSize]]
			)};
			newArr.postln;
			this.addAll(newArr, path, function);
		}, {
			"Not path specified".warn;
		});
	}

	*fileNames {var tagArr;
		if(bufferArray.notNil, {
			bufferArray.do{ |item|
				var filePath, allocTag;
				filePath = item.path;
				if(filePath.notNil, {
					tagArr = tagArr.add(PathName(filePath).fileName.asSymbol);
					/*function.(filePath).asSymbol;*/
				});
			};
			if(tagArr.notNil, {
				^tagArr;
			}, {
				"No files allocated".warn;
			});
		}, {
			"No buffers allocated".warn;
		});
	}

	*get {arg tag;
		var resultBuf, bufIndex, symbols;
		if(bufferArray.notNil, {
			symbols = this.tags;
			bufIndex = symbols.indexOfEqual(tag);
			if(bufIndex.notNil, {
				resultBuf = bufferArray[bufIndex];
			}, {
				"Tag not found".warn;
			});
		}, {
			"No buffers allocated".warn;
		});
		^resultBuf;
	}

	*getFile {arg string;
		var resultBuf;
		if(bufferArray.notNil, {
			bufferArray.do{|item|
				if(item.path.notNil, {
					if(string == item.path.basename, {resultBuf = item});
				});
			};
		}, {
			"No buffers allocated".warn;
		});
		^resultBuf;
	}

	*getFromPath {arg string;
		var resultBuf;
		if(bufferArray.notNil, {
			bufferArray.do{|item|
				if(item.path.notNil, {
					if(string == item.path, {resultBuf = item});
				});
			};
		}, {
			"No buffers allocated".warn;
		});
		^resultBuf;
	}

	*fileNumChannels {arg path;
		var file, chanNum;
		file = SoundFile.new;
		file.openRead(path);
		chanNum = file.numChannels;
		file.close;
		^chanNum;
	}

	*arrDir {
		^bufferArray.collect{|item| item.path.dirname }.rejectSame;
	}

	*bufferByDir {var indexArr, indexShape;
		this.arrDir.do{|subdir, index|
			bufferArray.do{|buf|
				if(buf.path.dirname == subdir, {indexArr = indexArr.add(index)});
			}
		};
		(indexArr.last+1).do{|item|	indexShape =
			indexShape.add(indexArr.indicesOfEqual(item) ); };
		^bufferArray.reshapeLike(indexShape);
	}

	*readSubDirs {arg path, function, readChansArr;
		var fullPaths;
		path ?? {path = defaultPath};
		PathName(path).entries.do{|subfolder|
			subfolder.entries.do{|file| fullPaths = fullPaths.add(file.fullPath) };
		};
		if(fullPaths.notNil, {
			this.readAll(fullPaths, { function.(this.bufferByDir); }, readChansArr);
		}, {
			"No subdirectories in this directory".warn;
		});
	}

	*addSubDirs {arg path, function;
		var arr;
		path ?? {path = defaultPath};
		PathName(path).entries.do{|subfolder|
			subfolder.entries.do{|file|
				arr = arr.add([file.fileNameWithoutExtension.asSymbol, file.fullPath.dirname]) };
		};
		if(arr.notNil, {
			this.addAllPaths(arr.flop[0], arr.flop[1], { function.(this.bufferByDir); });
		}, {
			"No subdirectories in this directory".warn;
		});
	}

	*freeAt {arg index;
		if(bufferArray.notNil, {
			if(bufferArray.isEmpty.not, {
				if(bufferArray[index].notNil, {
					(globVarArray[index] ++ ".free;").radpost.interpret;
					(globVarArray[index] ++ " = nil").interpret;
					bufferArray.removeAt(index);
					globVarArray.removeAt(index);
					tags.removeAt(index);
				}, {
					"Index not found".warn;
				});
			}, {
				"Buffers system is empty".warn;
			});
		}, {
			"No buffers found".warn;
		});
	}

	*freeAtAll {arg indexArr;
		var count=0;
		indexArr.do{|index| this.freeAt(index - count); count = count + 1};
	}

	*free {arg tag;
		var resultBuf, bufIndex, symbols;
		if(bufferArray.notNil, {
			symbols = this.tags;
			bufIndex = symbols.indexOfEqual(tag);
			if(bufIndex.notNil, {
				(globVarArray[bufIndex] ++ ".free;").radpost.interpret;
				(globVarArray[bufIndex] ++ " = nil").interpret;
				bufferArray.removeAt(bufIndex);
				globVarArray.removeAt(bufIndex);
				tags.removeAt(bufIndex);
			}, {
				"Tag not found".warn;
			});
		}, {
			"No buffers allocated".warn;
		});
	}

	*freeAll {arg tagArr;
		if(tagArr.notNil, {
			tagArr.do{|tag| this.free(tag);};
		}, {
			globVarArray.do{|item|
				(item ++ ".free;").radpost.interpret;
				(item ++ " = nil").interpret;
			};
			bufferArray = nil;
			globVarArray = nil;
			tags = nil;
			bufcount = 0;
		});
	}

	*pathToTag {arg path;
		var myPath;
		myPath = PathName.new(path);
		^myPath.fileNameWithoutExtension.asSymbol;
	}

	*dirTags {arg path;
		var myPath, finalArr;
		path ?? {path = defaultPath};
		myPath = PathName.new(path);
		myPath.files.do{|item|
			finalArr = finalArr.add(item.fileNameWithoutExtension.asSymbol);
		};
		^finalArr;
	}

	*dirDialog {
		FileDialog({ |path|
			this.dirTags(path[0]).radpost(\doln);
			path[0].radpost(\post);
		}, {
			"cancelled".warn;
		}, 2);
	}

	*setDefaultPath {
		FileDialog({ |path|
			defaultPath = path[0];
			defaultPath.radpost(\post)
		}, {
			"cancelled".warn;
		}, 2);
	}

}