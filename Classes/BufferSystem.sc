BufferSystem {classvar condition, server, <bufferArray, <bufAlloc, <>defaultPath;

	*new {
		condition = Condition.new;
		server = Server.default;
	}

	*read {arg pathName, function;
		var main, buffer;
		main = this.new;
		server.makeBundle(nil, {
			{
				bufAlloc = true;
				buffer = Buffer.read(server, pathName).postin(\ide, \ln);
				bufferArray = bufferArray.add(buffer);
				server.sync(condition);
				bufAlloc = false;
				function.value(buffer);
			}.fork;
		});
	}

	*readAll {arg pathArr, function;
		var main, buffer, returnArray;
		main = this.new;
		server.makeBundle(nil, {
			{
				bufAlloc = true;
				pathArr.do{|item|
					buffer = Buffer.read(server, item).postin(\ide, \ln);
					bufferArray = bufferArray.add(buffer);
					returnArray = returnArray.add(buffer);
					server.sync(condition);
				};
				function.value(returnArray);
				bufAlloc = false;
			}.fork;
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

		server.makeBundle(nil, {
			{
				bufAlloc = true;
				buffer = Buffer.alloc(server, numFrames, numChannels).postin(\ide, \ln);
				bufferArray = bufferArray.add(buffer);
				server.sync(condition);
				bufAlloc = false;
				function.value(buffer);
			}.fork;
		});

	}

	*allocAll {arg argArr, function;
		var main, buffer, returnArr;
		main = this.new;

		server.makeBundle(nil, {
			{
				bufAlloc = true;

				argArr.do{|item|
					item[0] ?? {item[0] = 44100};
					item[1] ?? {item[1] = 1};

					buffer = Buffer.alloc(server, item[0], item[1]).postin(\ide, \ln);
					bufferArray = bufferArray.add(buffer);
					returnArr = returnArr.add(buffer);
					server.sync(condition);

				};
				bufAlloc = false;
				function.value(returnArr);

			}.fork;
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
							"File already allocated as: ".postin(\ide, \ln);
							function.value(getBufferPaths.flop[1][getIndex].postin(\ide, \ln););
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
						"File already allocated as: ".postin(\ide, \ln);
						item.postin(\ide, \ln);
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
		if(path.notNil, {
			if(arr.flat[0].isNumber, {
				//allocate arr: [frames, channels]
				this.addAllPaths(arr, function)
			}, {
				this.addAllPaths(arr, path!arr.size, function);
			});
		}, {
			"No path specified".warn;
		});
	}

	*addPairs {arg arr, function;
		var newArr;
		if(arr.indexOf(nil).notNil.not, {
			if(arr.flat[0].isNumber, {
				//allocate arr: [frames, channels]
				this.addAllPaths(arr.clump(2), function)
			}, {
				newArr = arr.clump(2).flop;
				this.addAllPaths(newArr[0], newArr[1], function);
			});
		}, {
			"Not all infomation is specified".warn;
		});
	}

	*get {arg tag;
		var resultBuf, bufInfo, bufIndex, symbols;
		bufInfo = this.bufferInfo;
		if(bufInfo.notNil, {
			symbols = bufInfo.flop[0].collect{|item| item.split($.)[0].asSymbol };
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

}