CallWindow : MainImprov {var <text, <>storeArr, <>lang, <>post=true, <>rootDir;
	var <>recordHistory, startTime, historyPath, <keyFunc;

	*new {arg window, bounds, font, qpalette, settings,
		postWhere, postType, postWin;
		^super.new.initCallWindow(window, bounds, font, qpalette,
			settings, postWhere, postType, postWin);
	}

	initCallWindow {arg window, bounds, font, qpalette, settings,
		postWhere=\ide, postType=\ln, postWin;

		storeArr=[];
		lang=\cmd;
		recordHistory = false;

		bounds ?? {bounds = Window.win2Right};
		font ?? {font=Font("Monaco", 12)};
		qpalette ?? {qpalette = QPalette.dark};

		if(window.isNil, {
			("You must provide a window as first argument, " ++
				"otherwise use the .window class method").warn;
		}, {
			text = TextView(window.asView, bounds).focus(true);
			text.font_(font).palette_(qpalette);

			keyFunc = {arg keycode, input=\keyboard;
				var string;
				case
				{keycode == 13} { //if return, then evaluate line in textView as string
					if(input == \keyboard, {
						string = text.currentLine;
					}, {
						text.backspace;
						string = text.currentLine;
						text.addString($\r);
					});
					string.postln;
					if(string != "", {
						string.do({arg char, i;
							if(char.ascii == 10, {
								string = string.copyRange(0, string.size-2);
							}); //if you get return in string, get rid of it
						});
						this.callFunc(string, postWin, postWhere, postType);
					});
				}
				{keycode == 67} {
					{1.do({
						this.clearWindow;
						0.01.yield;
					})}.fork(AppClock);
				}
			};

			text.keyDownAction_({arg text, key, modifiers, keycode;
				keyFunc.value(keycode);
				if(recordHistory, {
					SystemClock.sched(0.0, {arg time;
						this.writeHistory(\keyDownAction, time, key);
						nil;
					});
				});

			});

			//add from file
			rootDir = (mainPath ++ "Settings/CallWindow/");

			if(settings.notNil, {
				this.addSettings(settings.asString);
			});
		});
	}

	*window {arg name="Call Window", bounds, font, qpalette, settings,
		postWhere, postType, postWin;
		var window;

		bounds ?? {bounds = Window.win2Right};

		window = Window.new(name, bounds.asRect).front;
		^this.new(window, Rect(0,0,bounds.width, bounds.height), font, qpalette,
			settings, postWhere, postType, postWin);
	}

	callFunc {arg string, postWin, postWhere=\both, postType=\ln;
		var inputArr, typeArr, index, selectArr, selectItem, funcArr;
		var arrString, arrInterpret, finalArr;

		if(lang != \sc, {

			inputArr = string.split($ ); //split string by the space and convert as array

			if(postWin.notNil, {
				inputArr.postin(postWhere, postType, postWin);
			}, {
				inputArr.postln;
			}); //post in any 'post' window

			inputArr.do({|item|
				if((item.isStringNumber).or(item.contains("-")).or((item.contains("["))
					.and(item.contains("]"))).or((item.contains("{")).and(item.contains("}"))), {
					case
					{item.isStringNumber} {
						typeArr = typeArr.add(\num);
						funcArr = funcArr.add(item.interpret);			}
					{item.contains("-")} {
						typeArr = typeArr.add(\dash);
						arrString = nil;
						arrString = item.replace("-", ",");
						arrString = arrString.insert(0, "[");
						arrString = arrString.insert(arrString.size, "]");
						arrInterpret = nil;
						arrInterpret = arrString.interpret;
						finalArr = nil;
						finalArr = Array.series(arrInterpret[1]-arrInterpret[0]+1, arrInterpret[0], 1);
						funcArr = funcArr.add(finalArr);
					}
					{(item.contains("[")).and(item.contains("]"))} {
						typeArr = typeArr.add(\arr);
						funcArr = funcArr.add(item.interpret);
					}
					{(item.contains("{")).and(item.contains("}"))} {
						typeArr = typeArr.add(\func);
						funcArr = funcArr.add(item.interpret);
					}; //note: brackets

				}, {
					typeArr = typeArr.add(\str);
					funcArr = funcArr.add(item.asSymbol);
				});
			});

			//look for commands
			selectArr = storeArr.select({|item| item[0] == inputArr[0].asSymbol});

			if(selectArr.isEmpty, {
				if((funcArr[0].isArray).or(funcArr[0].isNumber), {
					case
					{funcArr[0].isArray
					} {
						this.callFunc(("arrayID " ++ string), postWin, postWhere, postType);
					}
					{funcArr[0].isNumber
					} {
						this.callFunc(("numberID " ++ string), postWin, postWhere, postType);
					};
				}, {
					"Command not Found 1".warn;
				});
			}, {

				selectItem = selectArr.select({|item| item[1] == typeArr}).unbubble;

				if(selectItem.isNil, {
					"Command not Found 2".warn;
				}, {
					selectItem[2].valueArray(funcArr);
				});

			});

		}, {
			string.interpret;
		});
	}

	add {arg cmdID=\test, cmdTypes=[\str], function={"test".postln},
		description="test command", replace=false;
		var conflict;

		if([\array, \number].includes(cmdID), {
			this.add((cmdID ++ \ID).asSymbol, ([\str] ++ cmdTypes),
				function, description, replace);
		},{
			if((cmdTypes.isArray).and(cmdID.asCompileString.contains("'"))
				.and(function.isFunction).and(description.isString), {
					if(replace, {
						if(storeArr.select({|item| item.atAll([0,1]) == [cmdID, cmdTypes]}).isEmpty, {
							"Command doesn't exist - try without replacing".warn;
						}, {
							storeArr.do({|item, index|
								if(item.atAll([0,1]) == [cmdID, cmdTypes], {
									storeArr[index] = [cmdID, cmdTypes, function, description];
								});
							});
						});
					}, {
						if(storeArr.select({|item| item.atAll([0,1]) == [cmdID, cmdTypes]}).isEmpty, {
							storeArr = storeArr.add([cmdID, cmdTypes, function, description]);
						}, {
							([cmdID, cmdTypes].asString ++
								" - command already exists - remove old command to add new one").warn;
						});
					});
				}, {
					"Wrong syntax - should be symbol, array, function, string".warn;
			});
		});
	}

	removeAt {arg index=0;
		storeArr.removeAt(index);
	}

	clearWindow {
		text.string_(""); //clear call window
	}

	cmd {
		lang = \cmd;
	}

	addAll {arg arr;
		arr.do({|item| this.add(item[0], item[1], item[2], item[3]); });
	}

	addArr {arg idArr=[\test1,\test2], cmdTypes=[\str], function={|a| a.postln},
		description="test command", replace=false;
		idArr.do({|item| this.add(item, cmdTypes, function, description, replace) });
	}

	addFromFile {arg filePath;
		var arr1;
		arr1 = filePath.loadPath;
		this.addAll(arr1);
	}

	addSettings {arg file;
		var settingsDir;
		settingsDir = rootDir ++ "Settings/";
		this.addFromFile(settingsDir ++ file.asString ++ ".scd");
	}

	writeSettings {arg fileName, replace=false;
		var file, path, writeFunc, settingsDir, warnWin;
		settingsDir = rootDir ++ "Settings/";
		writeFunc = {
			file = File(path, "w");
			file.write(storeArr.asCompileString);
			file.close;
		};
		path = (settingsDir ++ fileName ++ ".scd");
		if(File.existsCaseSensitive(path), {
			if(replace.not, {
				Window.warnQuestion(("These settings already exist: " ++
					"Are you sure you want to replace them?"), {writeFunc.value;});
			});
		}, {
			writeFunc.value;
		});
	}

	stringArr {
		var newArr;
		newArr=[];
		storeArr.do{|item|
			newArr = newArr.add([item[0], item[1], item[2].postString, item[3]])
		};
		^newArr;
	}

	listSettings {arg postWhere=\ide, postType=\doln, extraPost;
		this.stringArr.postin(postWhere, postType, extraPost: extraPost);
	}

	startHistory {arg fileName, replace=false;
		var file, date, writeHistoryFunc;
		fileName ?? {fileName = Date.getDate.asString};
		historyPath = (rootDir ++ "History/" ++ fileName ++ ".scd");

		writeHistoryFunc = {
			file = File(historyPath, "a+");
		file.close;
		recordHistory = true;
		text.front;
		SystemClock.sched(0.0, {arg time;
			startTime = time;
			nil;
		});
		};
		if(File.existsCaseSensitive(historyPath), {
			if(replace.not, {
				Window.warnQuestion(("This history already exist: " ++
					"Are you sure you want to replace it?"), {writeHistoryFunc.value;});
			});
		}, {
			writeHistoryFunc.value;
		});
	}

	stopHistory {
		recordHistory = false;
	}

	writeHistory {arg id, time, key, keycode;
		var file, now;
		now = time - startTime;
		file = File(historyPath, "a+");
		file.write([now, key].asCompileString);
		file.write(", ");
		file.close;
	}

	readHistory {arg fileName;
		var file, path, array, newArray;
		path = (rootDir ++ "History/" ++ fileName ++ ".scd");
		file = File(path, "r");
		array = ("[ " ++ file.readAllString ++ " ]").interpret;
		file.close;
		newArray = [array.flop[0].differentiate, array.flop[1]].flop;
		^newArray;
	}

	viewHistory {arg fileName;
		var array;
		text.front;
		array = this.readHistory(fileName);
		text.string = "";
		{
			array.do{|item|
				item[0].yield;
				if(item[1].ascii == 127, {
					text.backspace;
				}, {
					text.addString(item[1]);
				});
			};
		}.fork(AppClock);
	}

	playHistory {arg fileName;
		var array;
		text.front;
		array = this.readHistory(fileName);
		text.string = "";
		{
			array.do{|item|
				item[0].yield;
				if(item[1].ascii == 127, {
					text.backspace;
				}, {
				text.addString(item[1]);
				keyFunc.value(item[1].ascii, \file);
				});
			};
		}.fork(AppClock);
	}

	arrHistoryFiles {
		var arr;
		arr = (rootDir ++ "History/").fileNameWithoutExtension;
		^arr;
	}

	listHistoryFiles {
		this.arrHistoryFiles.do{|item| item.postln};
	}

}