BufferSystem {classvar condition, server, <bufferArray, <bufAlloc, <>defaultPath, <>postWhere=\ide, <>postWin;

	*new {
		condition = Condition.new;
		server = Server.default;
	}

	*read {arg pathName, function;
		var main, buffer;
		main = this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					buffer = Buffer.read(server, pathName).postin(postWhere, \ln, postWin);
					bufferArray = bufferArray.add(buffer);
					server.sync(condition);
					bufAlloc = false;
					function.value(buffer);
				}.fork;
			});
		}, {"Server not running".warn});
	}

	*readAll {arg pathArr, function;
		var main, buffer, returnArray;
		main = this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					pathArr.do{|item|
						buffer = Buffer.read(server, item).postin(postWhere, \ln, postWin);
						bufferArray = bufferArray.add(buffer);
						returnArray = returnArray.add(buffer);
						server.sync(condition);
					};
					function.value(returnArray);
					bufAlloc = false;
				}.fork;
			});
		}, {"Server not running".warn});
	}

	*readDir {arg path, function;
		var myPath, newArr;
		if(path.notNil, {
			myPath = PathName.new(path);
			myPath.files.do{|item| newArr = newArr.add(item.fullPath)};
			this.readAll(newArr, function);
		}, {
			"Not path specified".warn;
		});
	}

	*bufferInfo {var array, tag, count=0;
		bufferArray.do{|item|
			if(item.path.notNil, {
				tag = item.path.basename;
			}, {
				tag = ("alloc" ++ count).asString;
				count = count + 1;
			});
			array = array.add([tag, item.numChannels, item.bufnum,
				item.numFrames, item.sampleRate]);
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
		myPath.files.do{|item| newArray = newArray.add(item.fileNameWithoutExtension)};

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
		var main, buffer;
		main = this.new;

		numFrames ?? {numFrames = 44100};
		numChannels ?? {numChannels = 1};

		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;
					buffer = Buffer.alloc(server, numFrames, numChannels).postin(postWhere, \ln, postWin);
					bufferArray = bufferArray.add(buffer);
					server.sync(condition);
					bufAlloc = false;
					function.value(buffer);
				}.fork;
			});
		}, {
			"Server is not running".warn;
		});

	}

	*allocAll {arg argArr, function;
		var main, buffer, returnArr;
		main = this.new;
		if(server.serverRunning, {
			server.makeBundle(nil, {
				{
					bufAlloc = true;

					argArr.do{|item|
						item[0] ?? {item[0] = 44100};
						item[1] ?? {item[1] = 1};

						buffer = Buffer.alloc(server, item[0], item[1]).postin(postWhere, \ln, postWin);
						bufferArray = bufferArray.add(buffer);
						returnArr = returnArr.add(buffer);
						server.sync(condition);

					};
					bufAlloc = false;
					function.value(returnArr);

				}.fork;
			});
		}, {
			"Server is not running".warn;
		});
	}

	*add {arg arg1, arg2, function;
		var getPath, getIndex, getBufferPaths;
		if(arg1.isNumber, {
			//allocate buffer: arg1: frames, arg2: channels
			this.alloc(arg1, arg2, function)
		}, {
			//read buffer: arg1: fileName, arg2: pathDir
			arg2 ?? {arg2 = defaultPath};

			if(arg2.notNil, {
				getPath = this.getPath(arg1, arg2);
				if(getPath.notNil, {

					getBufferPaths = this.bufferPaths;

					if(getBufferPaths.notNil, {
						getIndex = getBufferPaths.flop[0].indexOfEqual(getPath);
						if(getIndex.isNil, {
							this.read(getPath, function);
						}, {
							"File already allocated as: ".postin(postWhere, \ln, postWin);
							function.value(getBufferPaths.flop[1][getIndex].postin(postWhere, \ln, postWin) );
						});
					}, {
						this.read(getPath, function);
					});
				});
			}, {
				"No path selected".warn;
			});
		});
	}

	*addAllPaths {arg arr, path, function;
		var getPath, getIndex, getBufferPaths, pathArr, stringArr, existingBuffArr, finalArr;
		if(arr.flat[0].isNumber, {
			this.allocAll(arr, function)
		}, {
			if(path.notNil, {
				arr.do{|item, index|
					getPath = this.getPath(item, path[index]);
					if(getPath.notNil, {

						getBufferPaths = this.bufferPaths;

						if(getBufferPaths.notNil, {
							getIndex = getBufferPaths.flop[0].indexOfEqual(getPath);
							if(getIndex.isNil, {
								pathArr = pathArr.add(getPath);
							}, {
								pathArr = pathArr.add(getBufferPaths.flop[1][getIndex];);
							});
						}, {
							pathArr = pathArr.add(getPath);
						});
					})
				};
				pathArr.do{|item|
					if(item.isString, {
						stringArr = stringArr.add(item);
					}, {
						existingBuffArr = existingBuffArr.add(item);
						"File already allocated as: ".postin(postWhere, \ln, postWin);
						item.postin(postWhere, \ln, postWin);
					});
				};
				if(stringArr.notNil, {
					this.readAll(stringArr, {|buffs|
						finalArr = arr.collect{|tag|	this.get(tag)};
						function.value(finalArr);
					});
				}, {
					function.value(existingBuffArr);
				});

			}, {
				"No path specified".warn;
			});
		});
	}

	*addAll {arg arr, path, function;
		if(arr.flat[0].isNumber, {
			//allocate arr: [frames, channels]
			this.addAllPaths(arr, function: function)
		}, {
			if(path.notNil, {
				this.addAllPaths(arr, path!arr.size, function);
			}, {
				"No path specified".warn;
			});
		});
	}

	*addPairs {arg arr, function;
		var newArr;
		if(arr.indexOf(nil).notNil.not, {
			if(arr.flat[0].isNumber, {
				//allocate arr: [frames, channels]
				this.addAllPaths(arr.clump(2), function: function)
			}, {
				newArr = arr.clump(2).flop;
				this.addAllPaths(newArr[0], newArr[1], function);
			});
		}, {
			"Not all infomation is specified".warn;
		});
	}

	*addDir {arg path, function;
		var myPath, newArr;
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

	*tags {var tagArr;
		if(bufferArray.notNil, {
		tagArr = bufferArray.collect{ |item|
			PathName(item.path).fileNameWithoutExtension.asSymbol
		};
		^tagArr;
		}, {
			"No buffers allocated".warn;
		});
	}

	*get {arg tag;
		var resultBuf, bufInfo, bufIndex, symbols;
		bufInfo = this.bufferInfo;
		if(bufInfo.notNil, {
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
		var resultBuf, bufInfo, bufIndex;
		bufInfo = this.bufferInfo;
		if(bufInfo.notNil, {
			bufIndex = bufInfo.flop[0].indexOfEqual(string.asString);
			if(bufIndex.notNil, {
				resultBuf = bufferArray[bufIndex];
			}, {
				"File not found".warn;
			});
		}, {
			"No buffers allocated".warn;
		});
		^resultBuf;
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
		(indexArr.last+1).do{|item|	indexShape = indexShape.add(indexArr.indicesOfEqual(item) ); };
		^bufferArray.reshapeLike(indexShape);
	}

	*readSubDirs {arg path, function;
		var fullPaths;
		PathName(path).entries.do{|subfolder|
			subfolder.entries.do{|file| fullPaths = fullPaths.add(file.fullPath) };
		};
				if(fullPaths.notNil, {
		this.readAll(fullPaths, { function.value(this.bufferByDir); });
				}, {
			"No subdirectories in this directory".warn;
		});
	}

	*addSubDirs {arg path, function;
		var arr;
		PathName(path).entries.do{|subfolder|
	subfolder.entries.do{|file|
		arr = arr.add([file.fileNameWithoutExtension.asSymbol, file.fullPath.dirname]) };
};
		if(arr.notNil, {
this.addAllPaths(arr.flop[0], arr.flop[1], { function.value(this.bufferByDir); });
		}, {
			"No subdirectories in this directory".warn;
		});

	}

}