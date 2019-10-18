CallWindow : Radicles {var <text, <>storeArr, <>storeIndex=0, <>lang, <>post=true, <>rootDir;
	var <>recordHistory, startTime, historyPath, <keyFunc, <callwin, <backgroundColor,
	<>varString;

	*new {arg window, bounds, font, qpalette, settings,
		postWhere, postType, postWin, postBool, storeSize;
		^super.new.initCallWindow(window, bounds, font, qpalette,
			settings, postWhere, postType, postWin, postBool, storeSize);
	}

	initCallWindow {arg window, bounds, font, qpalette, settings,
		postWhere=\ide, postType=\ln, postWin, postBool, storeSize=10;

		~callWindowGlobVar = this;
		varString = "~callWindowGlobVar";

		storeArr=[]!storeSize;
		lang=\cmd;
		recordHistory = false;

		bounds ?? {bounds = Window.win2Right};
		font ?? {font=Font("Monaco", 12)};
		qpalette ?? {qpalette = QPalette.dark; backgroundColor = Color.black};

		if(window.isNil, {
			("You must provide a window as first argument, " ++
				"otherwise use the .window class method").warn;
		}, {
			callwin = window;
			text = TextView(window.asView, bounds).focus(true);
			text.font_(font).palette_(qpalette).background_(backgroundColor);
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
					/*string.postln;*/
					if(string != "", {
						string.do({arg char, i;
							if(char.ascii == 10, {
								string = string.copyRange(0, string.size-2);
							}); //if you get return in string, get rid of it
						});
						this.callFunc(string, postWin, postWhere, postType, postBool);
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
				keyFunc.(keycode);
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
		postWhere, postType, postWin, postBool, storeSize=10;
		var window;

		bounds ?? {bounds = Window.win2Right};

		window = Window.new(name, bounds.asRect).front;
		^this.new(window, Rect(0,0,bounds.width, bounds.height), font, qpalette,
			settings, postWhere, postType, postWin, postBool, storeSize);
	}

	callFunc {arg string, postWin, postWhere=\both, postType=\ln, postBool=true, callIndex;
		var inputArr, typeArr, index, selectArr, selectItem, funcArr;
		var arrString, arrInterpret, finalArr;
		if(lang != \sc, {
			if((string.contains("(").and(string.contains(")")))
				.or(string.contains("{").and(string.contains("}"))), {
					inputArr = string;
				}, {
					inputArr = string.split($ ); //split string by the space and convert as array
			});
			if(postBool, {
				if(postWin.notNil, {
					inputArr.postin(postWhere, postType, postWin);
				}, {
					inputArr.postln;
				}); //post in any 'post' window
			});
			if(inputArr.isString.not, {
				inputArr.do({|item|
					if((item.isStringNumber).or(item.contains("-"))
						.or((item.contains("["))
							.and(item.contains("]")))
						.or((item.contains("{"))
							.and(item.contains("}"))), {
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
							}
							; //note: brackets
						}, {
							typeArr = typeArr.add(\str);
							funcArr = funcArr.add(item.asSymbol);
					});
				});
			});
			if(funcArr.notNil, {
				//look for commands
				callIndex ?? {callIndex = storeIndex};
				selectArr = storeArr[callIndex].select({|item| item[0] == inputArr[0].asSymbol});

				if(selectArr.isEmpty, {
					if((funcArr[0].isArray).or(funcArr[0].isNumber), {
						case
						{funcArr[0].isArray
						} {
							this.callFunc(("arrayID " ++ string), postWin, postWhere, postType, postBool);
						}
						{funcArr[0].isNumber
						} {
							this.callFunc(("numberID " ++ string), postWin, postWhere, postType, postBool);
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
				/*"this a string containing brackets".postln;*/
				if(string.contains("(").and(string.contains(")")), {
					/*"round brackets".postln;*/
					if(inputArr.contains("->"), {
					/*	"association string".postln;*/
						this.association(inputArr.asString);
					});
				}, {
					"function brackets".postln;
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
						if(storeArr[storeIndex].select({|item| item.atAll([0,1]) == [cmdID, cmdTypes]}).isEmpty, {
							"Command doesn't exist - try without replacing".warn;
						}, {
							storeArr[storeIndex].do({|item, index|
								if(item.atAll([0,1]) == [cmdID, cmdTypes], {
									storeArr[storeIndex][index] = [cmdID, cmdTypes, function, description];
								});
							});
						});
					}, {
						if(storeArr[storeIndex].select({|item| item.atAll([0,1]) == [cmdID, cmdTypes]}).isEmpty, {
							storeArr[storeIndex] = storeArr[storeIndex].add([cmdID, cmdTypes, function, description]);
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
		storeArr[storeIndex].removeAt(index);
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
					"Are you sure you want to replace them?"), {writeFunc.();});
			});
		}, {
			writeFunc.();
		});
	}

	stringArr {
		var newArr;
		newArr=[];
		storeArr[storeIndex].do{|item|
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
					"Are you sure you want to replace it?"), {writeHistoryFunc.();});
			});
		}, {
			writeHistoryFunc.();
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
					keyFunc.(item[1].ascii, \file);
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

	associationFunc {arg cmdString;
		var cmd2, cmd3A, cmd3B, varArr, secondInd, argSel, cmd4B, cmd5B, varSel;
		cmd2 = cmdString.replace("(", "").replace(")","").replace("->", "|").split($|);
		cmd3A = cmd2[1].split($ );
		cmd3B = cmd2[0].split($ );
		cmd3A = cmd3A.reject({|item| item == "" });
		cmd3B = cmd3B.reject({|item| item == "" });
		varArr = 	cmd3A.collect({|item|
			if(item.find("$").notNil, {
				case
				{item.find("$n").notNil} {[\num, ("num" ++ item.replace("$n", ""))]}
				{item.find("$s").notNil} {[\str, ("str" ++ item.replace("$s", ""))]}
				{item.find("$a").notNil} {[\arr, ("arr" ++ item.replace("$a", ""))]}
				{item.find("$f").notNil} {[\func, ("func" ++ item.replace("$f", ""))]};
			}, {
				case
				{item.isNumber} {[\num, "num"]}
				{item.isSymbol} {[\str, "str"]}
				{item.isString} {[\str, "str"]}
				{item.isArray} {[\arr, "arr"]}
				{item.isFunction} {[\func, "func"]};
			});
		});
		secondInd = [];
		cmd3B.do{|item|
			if(item.asString.find("$").notNil, {
				secondInd = secondInd.add(cmd3A.indexOfEqual(item.asString););
			});
		};
		varSel = cmd3A.atAll(secondInd);
		argSel = varArr.flop[1].atAll(secondInd);
		cmd4B = cmd3B;
		varSel.do{|item, index|
			var concStr;
			if(index == (varSel.size-1), {
				concStr = (argSel[index] ++ " ++ " ++ "\"" ++ "\"" );
			}, {
				concStr =  (argSel[index] ++ " ++ " ++ "\"" ++ " "++ "\"" );
			});
			cmd4B[cmd4B.indexOfEqual(item)] = concStr;
		};
		cmd5B	= cmd4B.collect({|item| if(item.find("++").isNil, {
			("\"" ++ item ++ " \"");
		}, {
			item;
		});
		});
		cmd5B	= cmd5B.collect({|item, index|
			if(cmd5B.size-1 == index, {
				item.replace(" ++ " ++ "\"" ++ "\"" , "").replace(" ", "");
			}, {
				(item ++ " ++ ");
			});
		});
		^[cmd3A, varArr, cmd5B];
	}

	association {arg cmd;
		var assoArr, firstFunc;
		assoArr = this.associationFunc(cmd);
		firstFunc =	varString ++ ".add(" ++ assoArr[0][0].asSymbol.cs ++ ", " ++ assoArr[1].flop[0].cs
		++ ", {arg" ++ assoArr[1].flop[1].asString.replace("[", "").replace(" ]", ";") ++ ($\n
			++ varString ++ ".callFunc(" ++ assoArr[2].asString.replace("[", "(").replace("]", ")").replace(",", "")
			++ ");" ++ $\n) ++ "}, \"" ++ assoArr[0][0].asSymbol ++ " : " ++ assoArr[1].flop[0] ++ "\");";
		firstFunc.radpost.interpret;
	}

}