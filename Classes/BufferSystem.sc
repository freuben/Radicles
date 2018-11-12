BufferSystem {classvar condition, server, <bufferArray, <globVarArray,
  <tags, countTag=0, <bufAlloc, <>defaultPath,
  <>postWin, countCue=0;

  *new {
    condition = Condition.new;
    server = Server.default;
  }

  *read {arg pathName, function;
    var main, buffer, globVar;
    main = this.new;
    if(server.serverRunning, {
      server.makeBundle(nil, {
        {
          bufAlloc = true;
          buffer = Buffer.read(server, pathName);
          bufferArray = bufferArray.add(buffer);
          globVar = "~buffer" ++ bufferArray.indexOf(buffer);
          globVarArray = globVarArray.add(globVar);
          (globVar ++ " = " ++
            "Buffer.read(s, " ++ pathName.cs ++ ");").radpost;
          (globVar ++ " =  s.cachedBufferAt(" ++ buffer.bufnum ++ ");").interpret;
          tags = tags.add(this.pathToTag(pathName));
          server.sync(condition);
          bufAlloc = false;
          function.(buffer);
        }.fork;
      });
    }, {"Server not running".warn});
  }

  *readAll {arg pathArr, function;
    var main, buffer, returnArray, globVar;
    main = this.new;
    if(server.serverRunning, {
      server.makeBundle(nil, {
        {
          bufAlloc = true;
          pathArr.do{|item|
            buffer = Buffer.read(server, item);
            bufferArray = bufferArray.add(buffer);
            globVar = "~buffer" ++ bufferArray.indexOf(buffer);
            globVarArray = globVarArray.add(globVar);
            (globVar ++ " = " ++
              "Buffer.read(s, " ++ item.cs ++ ");").radpost;
            (globVar ++ " =  s.cachedBufferAt(" ++ buffer.bufnum ++ ");").interpret;
            tags = tags.add(this.pathToTag(item));
            returnArray = returnArray.add(buffer);
            server.sync(condition);
          };
          function.(returnArray);
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

  *alloc {arg numFrames, numChannels, function, bufnum;
    var main, buffer, globVar;
    main = this.new;

    numFrames ?? {numFrames = 44100};
    numChannels ?? {numChannels = 1};

    if(server.serverRunning, {
      server.makeBundle(nil, {
        {
          bufAlloc = true;
          buffer = Buffer.alloc(server, numFrames, numChannels, bufnum: bufnum);
          bufferArray = bufferArray.add(buffer);
          globVar = "~buffer" ++ bufferArray.indexOf(buffer);
          globVarArray = globVarArray.add(globVar);
          (globVar ++ " = " ++ "Buffer.alloc(s, " ++ numFrames ++
            ", " ++ numChannels ++ ");").radpost;
          (globVar ++ " =  s.cachedBufferAt(" ++ buffer.bufnum ++ ");").interpret;
          tags = tags.add( ("alloc" ++ countTag).asSymbol );
          countTag = countTag + 1;
          server.sync(condition);
          bufAlloc = false;
          function.(buffer);
        }.fork;
      });
    }, {
      "Server is not running".warn;
    });

  }

  *allocAll {arg argArr, function;
    var main, buffer, returnArr, globVar;
    main = this.new;
    if(server.serverRunning, {
      server.makeBundle(nil, {
        {
          bufAlloc = true;
          argArr.do{|item|
            item[0] ?? {item[0] = 44100};
            item[1] ?? {item[1] = 1};
            buffer = Buffer.alloc(server, item[0], item[1], bufnum: item[2]);
            bufferArray = bufferArray.add(buffer);
            globVar = "~buffer" ++ bufferArray.indexOf(buffer);
            globVarArray = globVarArray.add(globVar);
            (globVar ++ " = " ++ "Buffer.alloc(s, " ++ item[0] ++
              ", " ++ item[1] ++ ");").radpost;
            (globVar ++ " =  s.cachedBufferAt(" ++ buffer.bufnum ++ ");").interpret;
            tags = tags.add( ("alloc" ++ countTag).asSymbol );
            countTag = countTag + 1;
            returnArr = returnArr.add(buffer);
            server.sync(condition);
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
    var main, buffer, chanNum, cueSize, globVar;
    main = this.new;
    if(server.serverRunning, {
      server.makeBundle(nil, {
        {
          bufAlloc = true;
          chanNum = this.fileNumChannels(pathName);
          cueSize = 32768*bufSize;
          buffer = Buffer.cueSoundFile(server, pathName, startFrame, chanNum,
            cueSize);
          bufferArray = bufferArray.add(buffer);
          globVar = "~buffer" ++ bufferArray.indexOf(buffer);
          globVarArray = globVarArray.add(globVar);
          (globVar ++ " = " ++ "Buffer.cueSoundFile(s, " ++ pathName.cs ++
            ", " ++ startFrame ++ ", " ++ chanNum ++  ", " ++ cueSize ++  ");").radpost;
          (globVar ++ " =  s.cachedBufferAt(" ++ buffer.bufnum ++ ");").interpret;
          tags = tags.add(("disk" ++ countCue ++ "_" ++
            this.pathToTag(pathName)).asSymbol);
          countCue = countCue + 1;
          server.sync(condition);
          bufAlloc = false;
          function.(buffer);
        }.fork;
      });
    }, {"Server not running".warn});
  }

  *cueAll {arg arr, function;
    //arr: [ [path1, [startFrame1, bufSize1], [path2, [startFrame2, bufSize2]...]
    var main, buffer, returnArray, file, chanNum, newArr, globVar, cueSize;
    main = this.new;
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
            buffer = Buffer.cueSoundFile(server, item[0], newArr[0], chanNum,
              cueSize);
            bufferArray = bufferArray.add(buffer);
            globVar = "~buffer" ++ bufferArray.indexOf(buffer);
            globVarArray = globVarArray.add(globVar);
            (globVar ++ " = " ++ "Buffer.cueSoundFile(s, " ++ item[0].cs ++
              ", " ++ newArr[0] ++ ", " ++ chanNum ++  ", " ++ cueSize ++  ");").radpost;
            (globVar ++ " =  s.cachedBufferAt(" ++ buffer.bufnum ++ ");").interpret;
            tags = tags.add(("disk" ++ countCue ++ "_" ++
              this.pathToTag(item[0])).asSymbol);
            countCue = countCue + 1;
            returnArray = returnArray.add(buffer);
            server.sync(condition);
          };
          function.(returnArray);
          bufAlloc = false;
        }.fork;
      });
    }, {"Server not running".warn});
  }

  *add {arg arg1, arg2, function, arg3;
    var getPath, getIndex, getBufferPaths, cueBool, buffunction;
    if(arg1.isNumber, {
      //allocate buffer: arg1: frames, arg2: channels, arg3: bufnum
      this.alloc(arg1, arg2, function, arg3)
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
            this.cue(getPath, arg1[1][0], arg1[1][1], function);
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
                    bufferArray.indexOf(this.get(arg1) ) ).radpost;
                  function.(this.get(arg1));
                }, {
                  buffunction.();
                });
              }, {
                buffunction.();
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
    var getPath, getIndex, getBufferPaths, pathArr, stringArr, existingBuffArr, finalArr;
    var cueBool, cueArgs;
    if(arr.flat[0].isNumber, {
      this.allocAll(arr, function)
    }, {
      if(arr[0].isSymbol, {arr = arr.rejectSame});
      if(path.notNil, {
        arr.do{|item, index|

          cueBool = item.isArray.not;

          if(cueBool, {
            getPath = this.getPath(item, path[index]);
          }, {
            getPath = this.getPath(item[0], path[index]);
            cueArgs = cueArgs.add([getPath, item[1]]);
          });

          if(getPath.notNil, {
            getBufferPaths = this.bufferPaths;
            if(getBufferPaths.notNil, {
              getIndex = getBufferPaths.flop[0].indexOfEqual(getPath);
              if(getIndex.isNil, {
                pathArr = pathArr.add(getPath);
              }, {
                if(tags.includes(item), {
                  pathArr = pathArr.add(this.get(item) );
                }, {
                  pathArr = pathArr.add(getPath);
                });
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
              ("File already allocated as: ~buffer" ++
              bufferArray.indexOf(item)).radpost;
          });
        };

        if(stringArr.notNil, {
          if(cueArgs.isNil, {
            this.readAll(stringArr, {
              finalArr = arr.collect{|tag|	this.get(tag)};
              function.(finalArr);
            });
          }, {
            this.cueAll(cueArgs, {|buf|
              function.(buf);
            });
          });
        }, {
          function.(existingBuffArr);
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
      path ?? {path = defaultPath};
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
          [item.fileNameWithoutExtension.asSymbol, [startFrame, bufSize]]
      )};
      this.addAll(newArr, path, function);
    }, {
      "Not path specified".warn;
    });
  }

  *addAllTypes {arg arr, path, function;
    var bufs, files, cues, finalArr, condition;
    var indexSort, sortIndex, newIndexArr;
    bufs=[];
    files=[];
    cues=[];
    arr.do{|bufInfo|
      if(bufInfo.isArray, {
        if(bufInfo[0].isNumber, {
          bufs = bufs.add(bufInfo);
        }, {
          cues = cues.add(bufInfo);
        });
      }, {
        files = files.add(bufInfo);
      });
    };
    arr.do{|it|
      if(it.isArray, {
        if(it[0].isNumber, {
          indexSort = indexSort.add(0);
        }, {
          indexSort = indexSort.add(2);
        });
      }, {
        indexSort = indexSort.add(1);
      });
    };
    indexSort.do{|it,in| sortIndex = sortIndex.add([it,in]) };
    sortIndex.sort{ arg a, b; a[0] <= b[0] };
    {
      condition = Condition(false);
      if(bufs.notEmpty, {
        BufferSystem.addAll(bufs, function: {|item|
          finalArr = finalArr.add(item);
          condition.test = true;
          condition.signal;
        });
      }, {
        condition.test = true;
        condition.signal;
      });
      condition.wait;
      condition.test = false;
      if(files.notEmpty, {
        BufferSystem.addAll(files, path, {|item|
          finalArr = finalArr.add(item);
          condition.test = true;
          condition.signal;
        });
      }, {
        condition.test = true;
        condition.signal;
      });
      condition.wait;
      condition.test = false;
      if(cues.notEmpty, {
        BufferSystem.addAll(cues, path, {|item|
          finalArr = finalArr.add(item);
          condition.test = true;
          condition.signal;
        });
      }, {
        condition.test = true;
        condition.signal;
      });
      condition.wait;
      condition.test = false;
      finalArr = finalArr.flat;
      newIndexArr = Array.fill(sortIndex.size, nil);
      sortIndex.flop[1].do{|item, index| newIndexArr[item] = finalArr[index] };
      newIndexArr.flat.radpost;
      function.(newIndexArr.flat);
    }.fork;

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
    (indexArr.last+1).do{|item|	indexShape = indexShape.add(indexArr.indicesOfEqual(item) ); };
    ^bufferArray.reshapeLike(indexShape);
  }

  *readSubDirs {arg path, function;
    var fullPaths;
    path ?? {path = defaultPath};
    PathName(path).entries.do{|subfolder|
      subfolder.entries.do{|file| fullPaths = fullPaths.add(file.fullPath) };
    };
    if(fullPaths.notNil, {
      this.readAll(fullPaths, { function.(this.bufferByDir); });
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
          (globVarArray[index] ++ ".free").radpost.interpret;
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
          (globVarArray[bufIndex] ++ ".free").radpost.interpret;
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
         (item ++ ".free").radpost.interpret;
          (item ++ " = nil").interpret;
      };
      bufferArray = nil;
      globVarArray = nil;
      tags = nil;
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