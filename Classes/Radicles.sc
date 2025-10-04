Radicles {classvar <>mainPath, <>libPath, <>nodeTime=0.08, <server, <>postWin=nil,
	<>postWhere=\ide, <>fadeTime=0.5, <>schedFunc, <>schedFuncAction, <>schedDiv=1,
	<bpm, <postDoc, <>lineSize=68, <>logCodeTime=false, <>oscCode, <>reducePostControl=false,
	<>ignorePost=false, <>ignorePostcont=false, <>colorCritical, <>colorMeter, <>colorWarning,
	<>colorTrack, <>colorBus, <>colorMaster, <>colorTextField, <>cW, <aZ, <excludeLibs,
	<>filesPath, <>soundFilePath, <>postWindow, <>beatsFuncArr, traceFunc, <>liveBlockArr,
	<>numOutputs = 8, <>numInputs = 8, <>numBuffers = 512, <>numBuses = 1024,
	<>numControlBuses=16384, <>memorySize=409600, <>modelNum=0;

	*new {
		this.setColors;
		^super.new.initRadicles;
	}

	initRadicles {var dash;
		dash = "/";
		Platform.case(
			\windows,   {dash = "\\"; }
		);
		mainPath = Quark("Radicles").localPath;
		libPath = Quark("RadiclesLibs").localPath;
/*		mainPath = (Platform.userExtensionDir ++ dash ++ "Radicles");
		libPath = (Platform.userExtensionDir ++ dash ++ "RadiclesLibs");*/
		filesPath = (Platform.userExtensionDir ++ dash ++ "RadiclesFiles");
		soundFilePath = (filesPath ++ dash ++ "SoundFiles");
		server = Server.default;
	}

	*start {
		this.callWindow;
		this.synthDefsPn;
		this.raveFunc;
		MIDIIn.connectAll;
	}

	*version {
		"Version 0.0.4".postln;
	}

	*document {
		postDoc = Document.new("Radicles: " ++ Date.getDate.asString);
	}

	*libraries {var libsFolder;
		this.new;
		libsFolder = PathName(libPath).folders;
		libsFolder = libsFolder.reject({|item| item.folderName == ".git" });
		^(["Main"] ++ libsFolder.collect({|item| item.folderName }));
	}

	*getLib {arg library;
		var result;
		result = this.libraries.indexOfEqual(library);
		^result;
	}

	*options {arg outputs, inputs, buffers, buses, controlBuses, memory;
		outputs ?? {outputs = numOutputs};
		numOutputs = outputs;
		inputs ?? {inputs = numInputs};
		numInputs = inputs;
		buffers ?? {buffers = numBuffers};
		numBuffers = buffers;
		buses ?? {buses = numBuses};
		numBuses = buses;
		controlBuses ?? {controlBuses = numControlBuses};
		numControlBuses = controlBuses;
		memory ?? {memory = memorySize};
		memorySize = memory;

	}

	*serveroptions {
		this.new;
		server.options.numOutputBusChannels = numOutputs;
		server.options.numInputBusChannels = numInputs;
		server.options.numWireBufs = numBuffers;
		server.options.numAudioBusChannels = numBuses;
		server.options.numControlBusChannels = numControlBuses;
		server.options.memSize = memorySize;
	}

	*clock {var clock, tclock;
		clock = "Ndef('metronome').proxyspace.makeTempoClock(1.0)";
		clock.radpost;
		clock.interpret;
		tclock = Ndef('metronome').proxyspace.clock;
		tclock.schedAbs(tclock.beats.ceil, { arg beat, sec;
			schedFunc.(beat, sec);
			schedFuncAction.(beat, sec);
			schedDiv });
	}

	*tempo {arg newBPM;
		var metroCS;
		bpm = newBPM;
		if(bpm.notNil, {
			metroCS = "Ndef('metronome').clock.tempo = " ++ (bpm/60).cs;
			metroCS.radpost;
			metroCS.interpret;
		}, {
			("BPM = " ++ ((Ndef('metronome').clock.tempo*60).round(0.01)).cs).postln;
		});
	}

	*schedAction {arg func;
		schedFuncAction = { func.(); func=nil};
	}

	*schedCount {arg func, min=1, max=100, loop=false;
		var count;
		count = min;
		schedFunc = { |a,b| if(a.isNumber, {
			if(loop, { func.(count.wrap(min,max)); count = count + 1}, {
				if(count <= max, {
					func.(count); count = count + 1
				});
			});
		}); };
	}

	*specUnmap {arg unmapVal, thisSpec, thisFunc;
		var specOptions, thisResult;
		if(thisFunc.notNil, {
			specOptions= (0..127).linlin(0,127,0,1.0).collect({|item|
				thisFunc.(thisSpec.map(item.round(0.01))) });
			thisResult = specOptions.indexIn(unmapVal).linlin(0,127,0,1);
		}, {
			thisResult = thisSpec.unmap(unmapVal);
		});
		^thisResult;
	}

	*allLibs {var libsFolder;
		this.new;
		libsFolder = PathName(libPath).folders;
		libsFolder = libsFolder.reject({|item| item.folderName == ".git" });
		^(libsFolder.collect({|item| item.folderName }) ++ ["Main"]);
	}

	*selLibs {arg selLibs;
		var allLibs;
		allLibs = this.allLibs;
		if(selLibs.collect({|item| allLibs.indexOfEqual(item) }).includes(nil).not, {
			excludeLibs = allLibs.reject({|item| selLibs.indexOfEqual(item).notNil });
		}, {
			"Libary not found".warn;
		});
	}

	*setColors {
		colorCritical = Color.new255(211, 14, 14);
		colorMeter = Color.new255(78, 109, 38);
		colorWarning = Color.new255(232, 90, 13);
		colorTrack = Color.new255(58, 162, 175);
		colorBus = Color.new255(132, 124, 10);
		colorMaster = Color.new255(102, 57, 130);
		colorTextField = Color.new255(246, 246, 246);
	}

	* savePreset {arg presetName, saveCmds=true;
		var resultArr, trk, bs, ms, mastArr, arr1, arr2, arr3,
		blkArr, blkArrSettings, hids, cmds;
		if(aZ.notNil, {
			trk = aZ.mixTrackNdefs.select{|item|
				item.key.asString.contains("track") };
			bs = aZ.mixTrackNdefs.select{|item|
				item.key.asString.contains("bus") };
			ms = aZ.mixTrackNdefs.select{|item|
				item.key.asString.contains("master") };
			mastArr = [trk, bs, ms];
			arr1 = mastArr.copyFromStart(1).collect{|item| item.size};
			arr2 = mastArr.collect{|item| item.collect{|it| it.numChannels } };
			arr2 = arr2.collect{|item| if(item.size == 1, {item[0]}, {item});};
			arr3 = [arr1, arr2];
			blkArr = Block.liveBlocks;
			if(blkArr.notNil, {
				blkArrSettings = blkArr.flop[0].select{|item| item.notNil}.collect{|item|
					Ndef(item).controlKeysValues };
			});
			hids = [HIDMap.hidInfoArr, HIDMap.hidCmds];
			cmds = [Radicles.cW.text.string, Radicles.aZ.mixerWin.visible];
			resultArr = [arr3] ++ [aZ.prepWritePreset] ++
			[[blkArr, blkArrSettings]] ++ [hids] ++ [cmds];
			/*resultArr.postln;*/
			PresetFile.write(\radicles, presetName, resultArr, path: 0);
		});
	}

	* loadPreset {arg presetName, playblks=false;
		var firstArr, dataArr, cond, modSettings, mixSettings, fxSettings,
		arrSettings, prepSettings, trackInfoArr, extraArgs, modMixSettings,
		busSettings, outSettings, ioSettings, assemblageArr, hidArr,
		cmdArr, string, mixView;
		{
			firstArr = PresetFile.read(\radicles, presetName);
			assemblageArr = firstArr[0];
			dataArr = firstArr[1];
			liveBlockArr = firstArr[2];
			hidArr = firstArr[3];
			cmdArr = firstArr[4];
			string = cmdArr[0];
			mixView = cmdArr[1];
			cond = Condition(false);
			if(aZ.isNil, {
				aZ = Assemblage(assemblageArr[0][0], assemblageArr[1][1],
					assemblageArr[1], action: {|number, channels|
						/*[number, channels].postln;*/
						Block.addNum(number, channels[0], {|arr|
							arr.do{|item, index|
								aZ.input(item, \track, (index+1));
							};
						});
						cond.test = true; cond.signal;
				});
				cond.wait;
				mixSettings = dataArr[0];
				prepSettings = dataArr[1];
				busSettings = dataArr[2];
				outSettings = dataArr[3];
				ioSettings = dataArr[4];
				prepSettings.do{|item|
					var infoArr;
					infoArr = item.flop[0][0].asString.divNumStr;
					if(infoArr[1].isNil, {infoArr[1] = 1});
					arrSettings = aZ.prepLoadTrackPreset(infoArr[0].asSymbol,
						infoArr[1], item.flop[1]);
					fxSettings = fxSettings ++ arrSettings[0];
					if(arrSettings[1].notNil, {
						modSettings = modSettings ++ arrSettings[1];
					});
				};
				extraArgs = aZ.prepMixSettings(mixSettings);
				extraArgs.do({|item, index| ("Ndef(" ++ item[0].cs ++ ").set" ++
					item[1].cs.replace("[", "(").replace("]", ");") ).radpost.interpret;
				server.sync;
				case
				{item[1].includes(\mute)} {
					aZ.muteStates[aZ.mixTrackNames.indexOf(item[0])] =
					item[1][(item[1].indexOfEqual(\mute)+1)];
				}
				{item[1].includes(\solo)} {
					aZ.soloStates[aZ.mixTrackNames.indexOf(item[0])] =
					item[1][(item[1].indexOfEqual(\solo)+1)];
				};
				});
				modMixSettings =mixSettings[1];
				if(modMixSettings.notNil, {
					modMixSettings.flop[1].do{|item|
						cond.test = false;
						aZ.modRawPreset(item[0][0], item, {
							cond.test = true; cond.signal;});
						cond.wait;
					};
				});
				if(outSettings.notNil, {
					aZ.setOutputSettings(outSettings);
					nodeTime.yield;
				});
				if(busSettings.notNil, {
					cond.test = false;
					aZ.setBusForPreset(busSettings, false, {
						cond.test = true; cond.signal;});
					cond.wait;
				});
				if(ioSettings.notNil, {
					aZ.recStates = ioSettings[0];
					aZ.recInputArr = ioSettings[1];
					aZ.mastOutArr = ioSettings[2];
					aZ.mapOutFunc;
					nodeTime.yield;
				});
				if(fxSettings.notNil, {
					cond.test = false;
					aZ.setFxs(fxSettings, {
						cond.test = false;
						modSettings.do{|item|
							server.sync;
							aZ.modRawPreset(item[0], item[1], {
								server.sync;
								cond.test = true; cond.signal;});
							cond.wait;
						};
						server.sync;
						nodeTime.yield;
						cond.test = true; cond.signal;
					});
				}, {
					cond.test = true; cond.signal;
				});
				cond.wait;
				if(hidArr[0].notNil, {
					hidArr[0].do{|item|
						("HIDMap.map" ++ item.cs.replaceAt("(", 0).replaceAt(")", item.cs.size-1)
							++ ";" ).interpret;
						server.sync;
					};
				});
				if(hidArr[1].notNil, {
					hidArr[1].do{|item|
						("HIDMap.mapFunc" ++ item.cs.replaceAt("(", 0).replaceAt(")", item.cs.size-1)
							++ ";" ).interpret;
					};
				});
				server.sync;
				Radicles.cW.text.string = string;
				Radicles.cW.text.select(string.size,string.size);

				if(Ndef(\masterOut).source.isNil, {
					Ndef('master').play;
				}, {
					Ndef(\masterOut).play;
				});
				nodeTime.yield;
				if(playblks, {
					this.runLiveBlocks;
				});
				server.sync;
				if(mixView.notNil, {
					{Radicles.aZ.mixer;}.defer;
				});
			});
		}.fork(AppClock);
	}

	* runLiveBlocks {
		if(liveBlockArr.notNil, {
			liveBlockArr[0].flop[0].do{|item, index|
				if(item.notNil, {
					("Ndef(" ++ item.cs ++ ").setn" ++
						liveBlockArr[1][index].cs.replace("[", "(").replace("]", ")"))
					.radpost.interpret;
				});
			};
			liveBlockArr[0].do{|item|
				if(item.notNil, {
					item[0] = item[0].asString.divNumStr[1];
					if(item[2].notNil, {
						item[2] = item[2][2];
					});
					("Block.play" ++ item.cs.replaceAt("(", 0).replaceAt(")",
						item.cs.size-1) ).radpost.interpret;
				});
			};
		});
	}

	*callWindow {arg name;
		name ?? {name = "Radicles";};
		cW = CallWindow.window(name, Window.win4TopRight, postBool: true);
		this.selLibs(["Main"]);

		//server boot
		cW.add(\boot, [\str], {
			server.boot;
		});

		this.loadAssemblageCmds;
		this.loadMixCmds;
		this.loadFxCmds;
		this.loadBlkCmds;
		this.loadBaseCmds;
	}

	*synthDefsPn {
		SynthDef(\metro, 	{arg out=0, amp=0.2; var sig;
			sig = WhiteNoise.ar(amp)*Env.perc(0.001,0.04).kr(doneAction: 2);
			Out.ar(out, sig);
		}).add;
	}

	*hidconnect {arg string;
		var str, str2, arr, ind, arr1, arr2, arr3, spec;
		str = string.copyRange(1, string.size-2);
		str = str.replace(" ,", ",").replace(", ", ",").replace("[ ", "[").replace(" ]", "]");
		arr = str.split($ );
		ind = arr.indexOfEqual("<>");
		arr1 = arr.copyFromStart(ind-1);
		arr2 = arr.copyToEnd(ind+1);
		arr2 = arr2.collect({|item, index| if((item == "$").and(index == (arr2.size-1)), {"#"}, {item});});
		str = arr2.asString;
		str = str.copyRange(2, str.size-3).replace(",");
		str = str.cs;
		str = str.replace("$", "\" ++ val ++ \"");
		str = str.replace("#\"", "\" ++ val");
		arr3 = arr1.copyToEnd(1);
		arr3 = arr3.collect({|item| if(item.isStringNumber, {item.interpret}, {item}); });
		str2 = [];
		arr3 = arr3.collect{|item| if(item.isString, {
			if((item.contains("[")).and(item.contains("]")), {
				item.interpret
			}, {
				item.asSymbol
			});
		}, {item}); };
		arr3.do{|item| if(item.isArray, {spec = item }, {str2 = (str2 ++ item)}); };
		if(spec.isNil, {spec = [0,1]});
		if(str.contains("%").not, {
			("HIDMap.mapFunc({|val| {~callWindowGlobVar.callFunc(" ++ str ++ ")}.defer }, " ++
				arr1[0].asSymbol.cs ++ ", " ++ spec ++ ", " ++ str2.cs ++ ");").interpret;
		}, {
			str = str.split($%);
			("HIDMap.mapFunc({|val| {if(val != 0, {~callWindowGlobVar.callFunc("
				++ str[0].nospaceFunc ++ "\") }, {~callWindowGlobVar.callFunc(\""
				++ str[1].nospaceFunc ++ ") }); }.defer }, " ++ arr1[0].asSymbol.cs
				++ ", " ++ spec ++ ", " ++ str2.cs ++ ");").interpret;
		});
	}

	*loadAssemblageCmds {
		cW.add(\asm, [\str, \num], {|str, num|
			if(aZ.isNil, {
				aZ = Assemblage(num, 1);
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum");
		cW.add(\asm, [\str, \num, \num], {|str, num1, num2|
			if(aZ.isNil, {
				aZ = Assemblage(num1, num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum, busNum");
		cW.add(\asm, [\str, \num, \num, \num], {|str, num1, num2, num3|
			if(aZ.isNil, {
				aZ = Assemblage(num1, num2, num3);
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum, busNum, chanNum");
		//chanNumArr can be multidimentional for different chanNums in different tracks
		cW.add(\asm, [\str, \num, \num, \arr], {|str, num1, num2, arr|
			if(aZ.isNil, {
				aZ = Assemblage(num1, num2, arr);
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum, busNum, chanNumArr");

		cW.add(\asm, [\str, \num, \num, \num, \str], {|str1, num1, num2, num3, str2|
			if(aZ.isNil, {
				aZ = Assemblage(num1, num2, num3, str2);
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblade: trackNum, busNum, chanNum, spaceType");

		cW.add(\asm, [\str, \num, \num, \arr, \str], {|str1, num1, num2, arr, str2|
			if(aZ.isNil, {
				aZ = Assemblage(num1, num2, arr, str2);
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum, busNum, chanNumArr, spaceType");
	}

	*loadFxCmds {
		cW.add(\fx, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFx(\track, num1, 1, str3);}
				{str2 == 'b'} {aZ.setFx(\bus, num1, 1, str3);}
				{str2 == 'm'} {aZ.setFx(\master, 1, num1, str3);}
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: trackType, trackNum, filter");

		cW.add(\fx, [\str, \str, \num, \str, \arr], {|str1, str2, num1, str3, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFx(\track, num1, 1, str3, arr1);}
				{str2 == 'b'} {aZ.setFx(\bus, num1, 1, str3, arr1);}
				{str2 == 'm'} {aZ.setFx(\master, 1, num1, str3, arr1);}
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: trackType, trackNum, filter, extraArgs");

		cW.add(\fx, [\str, \str, \str], {|str1, str2, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFx(\track, 1, 1, str3);}
				{str2 == 'b'} {aZ.setFx(\bus, 1, 1, str3);}
				{str2 == 'm'} {aZ.setFx(\master, 1, 1, str3);}
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: trackType, filter");

		cW.add(\fx, [\str, \str, \str, \arr], {|str1, str2, str3, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFx(\track, 1, 1, str3, arr1);}
				{str2 == 'b'} {aZ.setFx(\bus, 1, 1, str3, arr1);}
				{str2 == 'm'} {aZ.setFx(\master, 1, 1, str3, arr1);}
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: trackType, filter, extraArgs");

		cW.add(\fx, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFx(\track, num1, num2, str3);}
				{str2 == 'b'} {aZ.setFx(\bus, num1, num2, str3);}
				{str2 == 'm'} {aZ.setFx(\master, num1, num2, str3);}
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: trackType, trackNum, slotNum, filter");

		cW.add(\fx, [\str, \str, \num, \num, \str, \arr], {|str1, str2, num1, num2, str3, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFx(\track, num1, num2, str3, arr1);}
				{str2 == 'b'} {aZ.setFx(\bus, num1, num2, str3, arr1);}
				{str2 == 'm'} {aZ.setFx(\master, num1, num2, str3, arr1);}
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: trackType, trackNum, slotNum, filter");

		cW.add(\fx, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFx(thisArr[0].asSymbol, thisArr[1], 1, str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: mixTrackNum, filter");

		cW.add(\fx, [\str, \num, \str, \arr], {|str1, num1, str2, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFx(thisArr[0].asSymbol, thisArr[1], 1, str2, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: mixTrackNum, filter, extraArgs");

		cW.add(\fx, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFx(thisArr[0].asSymbol, thisArr[1], num2, str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: mixTrackNum, slotNum, filter");

		cW.add(\fx, [\str, \num, \num, \str, \arr], {|str1, num1, num2, str2, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFx(thisArr[0].asSymbol, thisArr[1], num2, str2, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: mixTrackNum, slotNum, filter, extraArgs");
		//
		cW.add(\fxclear, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 'm'} {
					aZ.setFx(\master, 1, num1, remove: true);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxclear: trackType, slot");

		cW.add(\fxclear, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFx(\track, num1, num2, remove: true);}
				{str2 == 'b'} {aZ.setFx(\bus, num1, num2, remove: true);}
				{str2 == 'm'} {aZ.setFx(\master, 1, num2, remove: true);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxclear: trackType, trackNum, slot");

		cW.add(\fxclear, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFx(thisArr[0].asSymbol, thisArr[1], 1, remove: true);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxclear: mixTrackNum, filter");

		cW.add(\fxclear, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFx(thisArr[0].asSymbol, thisArr[1], num2, remove: true);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxclear: mixTrackNum, filter");

		cW.add(\clearfx, [\str, \num], {|str1, num1|
			var filterTag, filterTagArr;
			if(aZ.notNil, {
				filterTag = aZ.filters[num1-1][0];
				if(filterTag.notNil, {
					filterTagArr = aZ.convFilterTag(filterTag).postln;
					aZ.setFx(filterTagArr[0].asSymbol, filterTagArr[1],
						filterTagArr[2], remove: true);
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "clearfx: filterNum");

		cW.add(\fxset, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 'm'} {
					aZ.setFxArgTrack(\master, 1, num1);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, fxArg");

		cW.add(\fxlag, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 'm'} {
					aZ.lagFxArgTrack(\master, 1, num1);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: trackType, fxArg");

		cW.add(\fxset, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFxArgTrack(\track, num1, num2);}
				{str2 == 'b'} {aZ.setFxArgTrack(\bus, num1, num2);}
				{str2 == 'm'} {aZ.fxTrackWarn(\master, 1, num1, {|item|
					Ndef(item).getKeysValues[num2-1].radpostwarn;

				});
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg");

		cW.add(\fxlag, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var getKey, controlKeys, ratesFor;
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.lagFxArgTrack(\track, num1, num2);}
				{str2 == 'b'} {aZ.lagFxArgTrack(\bus, num1, num2);}
				{str2 == 'm'} {aZ.fxTrackWarn(\master, 1, num1, {|item|
					getKey = item;
					controlKeys = Ndef(getKey).controlKeys;
					ratesFor = Ndef(getKey).nodeMap.ratesFor(
						controlKeys);
					if(ratesFor.notNil, {
						[controlKeys[num2-1],
							ratesFor[num2-1]].radpostwarn;
					});
				});
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: trackType, trackNum, fxArg");

		cW.add(\fxset, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.fxTrackWarn(\track, num1, num2, {|item|
					Ndef(item).getKeysValues[num3-1].radpostwarn;
				});}
				{str2 == 'b'} {aZ.fxTrackWarn(\bus, num1, num2, {|item|
					Ndef(item).getKeysValues[num3-1].radpostwarn;
				});}
				{str2 == 'm'} {
					aZ.setFxArgTrack(\master, 1, num1, num2, num3);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg, val");

		cW.add(\fxlag, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			var getKey, controlKeys, ratesFor;
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.fxTrackWarn(\track, num1, num2, {|item|
					getKey = item;
					controlKeys = Ndef(getKey).controlKeys;
					ratesFor = Ndef(getKey).nodeMap.ratesFor(
						controlKeys);
					if(ratesFor.notNil, {
						[controlKeys[num3-1],
							ratesFor[num3-1]].radpostwarn;
					});

				});}
				{str2 == 'b'} {aZ.fxTrackWarn(\bus, num1, num2, {|item|
					getKey = item;
					controlKeys = Ndef(getKey).controlKeys;
					ratesFor = Ndef(getKey).nodeMap.ratesFor(
						controlKeys);
					if(ratesFor.notNil, {
						[controlKeys[num3-1],
							ratesFor[num3-1]].radpostwarn;
					});
				});}
				{str2 == 'm'} {
					aZ.lagFxArgTrack(\master, 1, num1, num2, num3);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: trackType, trackNum, fxArg, val");

		cW.add(\fxset, [\str, \str, \num, \num, \num, \num], {|str1, str2, num1, num2, num3, num4|
			if(aZ.notNil, {
				case
				{str2 == 't'} { aZ.setFxArgTrack(\track, num1, num2, num3, num4);}
				{str2 == 'b'} { aZ.setFxArgTrack(\bus, num1, num2, num3, num4);}
				{str2 == 'm'} {aZ.setFxArgTrack(\master, 1, num2, num3, num4);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, slotNum, fxArg, val");

		cW.add(\fxlag, [\str, \str, \num, \num, \num, \num], {|str1, str2, num1, num2, num3, num4|
			if(aZ.notNil, {
				case
				{str2 == 't'} { aZ.lagFxArgTrack(\track, num1, num2, num3, num4);}
				{str2 == 'b'} { aZ.lagFxArgTrack(\bus, num1, num2, num3, num4);}
				{str2 == 'm'} {aZ.lagFxArgTrack(\master, 1, num2, num3, num4);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: trackType, trackNum, slotNum, fxArg, val");

		cW.add(\fxset, [\str, \str, \num, \num, \str, \num], {|str1, str2, num1, num2, str3, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} { aZ.setFxArgTrack(\track, num1, num2, str3, num3);}
				{str2 == 'b'} { aZ.setFxArgTrack(\track, num1, num2, str3, num3);}
				{str2 == 'm'} {aZ.setFxArgTrack(\master, 1, num2, str3, num3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, slotNum, fxArg, val");

		cW.add(\fxlag, [\str, \str, \num, \num, \str, \num], {|str1, str2, num1, num2, str3, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} { aZ.lagFxArgTrack(\track, num1, num2, str3, num3);}
				{str2 == 'b'} { aZ.lagFxArgTrack(\track, num1, num2, str3, num3);}
				{str2 == 'm'} {aZ.lagFxArgTrack(\master, 1, num2, str3, num3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: trackType, trackNum, slotNum, fxArg, val");

		cW.add(\fxset, [\str, \str, \num, \str, \num], {|str1, str2, num1, str3, num2|
			if(aZ.notNil, {
				case
				{str2 == 'm'} {aZ.setFxArgTrack(\master, 1, num1, str3, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, slotNum, fxArg, val");

		cW.add(\fxlag, [\str, \str, \num, \str, \num], {|str1, str2, num1, str3, num2|
			if(aZ.notNil, {
				case
				{str2 == 'm'} {aZ.lagFxArgTrack(\master, 1, num1, str3, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: trackType, slotNum, fxArg, val");

		cW.add(\fxset, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFxArgTrack(thisArr[0].asSymbol, thisArr[1], num2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\fxlag, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.lagFxArgTrack(thisArr[0].asSymbol, thisArr[1], num2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: mixTrackNum, filter");

		cW.add(\fxset, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.fxTrackWarn(thisArr[0].asSymbol, thisArr[1], num2, {|item|
						Ndef(item).getKeysValues[num3-1].radpostwarn;
					});
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: mixTrackNum, filter");

		cW.add(\fxlag, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr, getKey, controlKeys, ratesFor;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.fxTrackWarn(thisArr[0].asSymbol, thisArr[1], num2, {|item|
						getKey = item;
						controlKeys = Ndef(getKey).controlKeys;
						ratesFor = Ndef(getKey).nodeMap.ratesFor(
							controlKeys);
						if(ratesFor.notNil, {
							[controlKeys[num3-1],
								ratesFor[num3-1]].radpostwarn;
						});
					});
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: mixTrackNum, filter");

		cW.add(\fxset, [\str, \num, \num, \num, \num], {|str1, num1, num2, num3, num4|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFxArgTrack(thisArr[0].asSymbol, thisArr[1], num2, num3, num4);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\fxlag, [\str, \num, \num, \num, \num], {|str1, num1, num2, num3, num4|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.lagFxArgTrack(thisArr[0].asSymbol, thisArr[1], num2, num3, num4);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: mixTrackNum, filter");

		cW.add(\fxset, [\str, \num, \num, \str, \num], {|str1, num1, num2, str2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFxArgTrack(thisArr[0].asSymbol, thisArr[1], num2, str2, num3);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\fxlag, [\str, \num, \num, \str, \num], {|str1, num1, num2, str2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.lagFxArgTrack(thisArr[0].asSymbol, thisArr[1], num2, str2, num3);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: mixTrackNum, filter");

		cW.add(\fxset, [\str, \num, \num, \arr], {|str1, num1, num2, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFxArgTrack(thisArr[0].asSymbol, thisArr[1], num2, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\fxlag, [\str, \num, \num, \arr], {|str1, num1, num2, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.lagFxArgTrack(thisArr[0].asSymbol, thisArr[1], num2, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fx: mixTrackNum, filter");

		cW.add(\fxset, [\str, \str, \num, \arr], {|str1, str2, num1, arr1|
			if(aZ.notNil, {
				case
				{str2 == 'm'} {
					aZ.setFxArgTrack(\master, 1, num1, arr1);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg");

		cW.add(\fxlag, [\str, \str, \num, \arr], {|str1, str2, num1, arr1|
			if(aZ.notNil, {
				case
				{str2 == 'm'} {
					aZ.lagFxArgTrack(\master, 1, num1, arr1);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: trackType, trackNum, fxArg");

		cW.add(\fxset, [\str, \str, \num, \num, \arr], {|str1, str2, num1, num2, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} { aZ.setFxArgTrack(\track, num1, num2, arr1);}
				{str2 == 'b'} { aZ.setFxArgTrack(\bus, num1, num2, arr1);}
				{str2 == 'm'} {aZ.setFxArgTrack(\master, 1, num2, arr1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, slotNum, fxArg, val");

		cW.add(\fxlag, [\str, \str, \num, \num, \arr], {|str1, str2, num1, num2, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} { aZ.lagFxArgTrack(\track, num1, num2, arr1);}
				{str2 == 'b'} { aZ.lagFxArgTrack(\bus, num1, num2, arr1);}
				{str2 == 'm'} {aZ.lagFxArgTrack(\master, 1, num2, arr1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: trackType, trackNum, slotNum, fxArg, val");

		cW.add(\setfx, [\str], {|str1|
			if(aZ.notNil, {
				if(aZ.filters.notNil, {
					aZ.filters.collect{|item| [aZ.convFilterTag(item[0]), item[1] ].flat;}.dopostln;
				}, {
					"no active filters".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: posts active filters");

		cW.add(\fxsetn, [\str, \arr], {|str1, arr1|
			if(aZ.notNil, {
				aZ.setFxArgTracks(arr1);
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsetn: set arr of filters with values");

		cW.add(\fxlagn, [\str, \arr], {|str1, arr1|
			if(aZ.notNil, {
				aZ.lagFxArgTracks(arr1);
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlagn: set arr of filters with values");

		cW.add(\fxlagn, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 'm'} {
					aZ.fxTrackLags(\master, 1, 1, num1);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlagn: trackType, fxArg");

		cW.add(\fxlagn, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 'm'} {aZ.fxTrackLags(\master, 1, num1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlagn: trackType, trackNum, fxArg");

		cW.add(\fxlagn, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.fxTrackLags(\track, num1, num2, num3);}
				{str2 == 'b'} {aZ.fxTrackLags(\bus, num1, num2, num3);}
				{str2 == 'm'} {aZ.fxTrackLags(\master, 1, num2, num3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlagn: trackType, trackNum, fxArg");

		cW.add(\fxlagn, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.fxTrackLags(thisArr[0].asSymbol, thisArr[1], num2, num3);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlagn: trackType, trackNum, fxArg");

		cW.add(\setfx, [\str, \num], {|str1, num1|
			if(aZ.notNil, {
				aZ.setFxArg(num1);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum");

		cW.add(\lagfx, [\str, \num], {|str1, num1|
			if(aZ.notNil, {
				aZ.lagFxArg(num1);
			}, {
				"could not find assemblage".warn;
			});
		}, "lagfx: filterNum");

		cW.add(\setfx, [\str, \num, \num], {|str1, num1, num2|
			if(aZ.notNil, {
				Ndef(aZ.filters[num1-1][0]).getKeysValues[num2-1].radpostwarn;
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\lagfx, [\str, \num, \num], {|str1, num1, num2|
			var getKey, controlKeys, ratesFor;
			if(aZ.notNil, {
				getKey = aZ.filters[num1-1][0];
				controlKeys = Ndef(getKey).controlKeys;
				ratesFor = Ndef(getKey).nodeMap.ratesFor(
					controlKeys);
				if(ratesFor.notNil, {
					[controlKeys[num2-1],
						ratesFor[num2-1]].radpostwarn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "lagfx: filterNum, fxArg");

		cW.add(\setfx, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			if(aZ.notNil, {
				aZ.setFxArg(num1, num2, num3);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg, val");

		cW.add(\lagfx, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			if(aZ.notNil, {
				aZ.lagFxArg(num1, num2, num3);
			}, {
				"could not find assemblage".warn;
			});
		}, "lagfx: filterNum, fxArg, val");

		cW.add(\setfx, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			if(aZ.notNil, {
				aZ.setFxArg(num1, str2, num2);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg, val");

		cW.add(\lagfx, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			if(aZ.notNil, {
				aZ.lagFxArg(num1, str2, num2);
			}, {
				"could not find assemblage".warn;
			});
		}, "lagfx: filterNum, fxArg, val");

		cW.add(\setfx, [\str, \num, \arr], {|str1, num1, arr1|
			if(aZ.notNil, {
				aZ.setFxArg(num1, arr1);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg, val");

		cW.add(\lagfx, [\str, \num, \arr], {|str1, num1, arr1|
			if(aZ.notNil, {
				aZ.lagFxArg(num1, arr1);
			}, {
				"could not find assemblage".warn;
			});
		}, "lagfx: filterNum, fxArg, val");

		cW.add(\fxget, [\str, \str, \num], {|str1, str2, num1|
			var filterTag;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					filterTag = aZ.findFilterTag(\track, num1, 1);
					if(filterTag.notNil, {
						aZ.getFilterKeys(aZ.findFilterTag(\track, num1, 1), \pairs).postln;
					});
				}
				{str2 == 'b'} {
					filterTag = aZ.findFilterTag(\bus, num1, 1);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \pairs).postln;
					});
				}
				{str2 == 'm'} {
					filterTag = aZ.findFilterTag(\master, num1, 1);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \pairs).postln;
					});
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg");

		cW.add(\fxget, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var filterTag, result;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					filterTag = aZ.findFilterTag(\track, num1, num2);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \pairs).postln;
					});
				}
				{str2 == 'b'} {
					filterTag = aZ.findFilterTag(\bus, num1, num2);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \pairs).postln;
					});
				}
				{str2 == 'm'} {
					filterTag = aZ.findFilterTag(\master, num1, 1);
					if(filterTag.notNil, {
						result = aZ.getFilterKeys(filterTag, \pairs)[num2-1][1];
						if(result.cs.contains("Ndef"), {
							result.key.postln;
						}, {
							result.postln;
						});
					});
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg");

		cW.add(\fxget, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			var filterTag, result, index;
			if(aZ.notNil, {
				case
				{str2 == 'm'} {
					filterTag = aZ.findFilterTag(\master, num1, 1);
					if(filterTag.notNil, {
						index = Ndef(filterTag).controlKeys.indexOf(str3);
						result = aZ.getFilterKeys(filterTag, \pairs)[index][1];
						if(result.cs.contains("Ndef"), {
							result.key.postln;
						}, {
							result.postln;
						});
					});
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg");

		cW.add(\fxget, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			var filterTag, result;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					filterTag = aZ.findFilterTag(\track, num1, num2);
					if(filterTag.notNil, {
						result = aZ.getFilterKeys(filterTag, \pairs)[num3-1][1];
						if(result.cs.contains("Ndef"), {
							result.key.postln;
						}, {
							result.postln;
						});
					});
				}
				{str2 == 'b'} {
					filterTag = aZ.findFilterTag(\bus, num1, num2);
					if(filterTag.notNil, {
						result = aZ.getFilterKeys(filterTag, \pairs)[num3-1][1];
						if(result.cs.contains("Ndef"), {
							result.key.postln;
						}, {
							result.postln;
						});
					});
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg");

		cW.add(\fxget, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			var filterTag, result, index;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					filterTag = aZ.findFilterTag(\track, num1, num2);
					if(filterTag.notNil, {
						index = Ndef(filterTag).controlKeys.indexOf(str3);
						result = aZ.getFilterKeys(filterTag, \pairs)[index][1];
						if(result.cs.contains("Ndef"), {
							result.key.postln;
						}, {
							result.postln;
						});
					});
				}
				{str2 == 'b'} {
					filterTag = aZ.findFilterTag(\bus, num1, num2);
					if(filterTag.notNil, {
						index = Ndef(filterTag).controlKeys.indexOf(str3);
						result = aZ.getFilterKeys(filterTag, \pairs)[index-1][1];
						if(result.cs.contains("Ndef"), {
							result.key.postln;
						}, {
							result.postln;
						});
					});
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg");

		cW.add(\fxgetlag, [\str, \str, \num], {|str1, str2, num1|
			var filterTag;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					filterTag = aZ.findFilterTag(\track, num1, 1);
					if(filterTag.notNil, {
						aZ.getFilterKeys(aZ.findFilterTag(\track, num1, 1), \lags).postln;
					});
				}
				{str2 == 'b'} {
					filterTag = aZ.findFilterTag(\bus, num1, 1);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \lags).postln;
					});
				}
				{str2 == 'm'} {
					filterTag = aZ.findFilterTag(\master, num1, 1);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \lags).postln;
					});
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg");

		cW.add(\fxgetlag, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var filterTag;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					filterTag = aZ.findFilterTag(\track, num1, num2);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \lags).postln;
					});
				}
				{str2 == 'b'} {
					filterTag = aZ.findFilterTag(\bus, num1, num2);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \lags).postln;
					});
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "fxgetlag: trackType, trackNum, fxArg");

		cW.add(\fxget, [\str, \num], {|str1, num1|
			var trackArr, thisArr, filterTag;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					filterTag = aZ.findFilterTag(thisArr[0].asSymbol, thisArr[1], 1);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \pairs).postln;
					});
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\fxget, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr, filterTag;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					filterTag = aZ.findFilterTag(thisArr[0].asSymbol, thisArr[1], num2);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \pairs).postln;
					});
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\fxget, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr, filterTag, result;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					filterTag = aZ.findFilterTag(thisArr[0].asSymbol, thisArr[1], num2);
					if(filterTag.notNil, {
						result = aZ.getFilterKeys(filterTag, \pairs)[num3-1][1];
						if(result.cs.contains("Ndef"), {
							result.key.postln;
						}, {
							result.postln;
						});
					});
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\fxget, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var trackArr, thisArr, filterTag, result, index;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					filterTag = aZ.findFilterTag(thisArr[0].asSymbol, thisArr[1], num2);
					if(filterTag.notNil, {
						index = Ndef(filterTag).controlKeys.indexOf(str2);
						result = aZ.getFilterKeys(filterTag, \pairs)[index][1];
						if(result.cs.contains("Ndef"), {
							result.key.postln;
						}, {
							result.postln;
						});
					});
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\fxgetlag, [\str, \num], {|str1, num1|
			var trackArr, thisArr, filterTag;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					filterTag = aZ.findFilterTag(thisArr[0].asSymbol, thisArr[1], 1);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \lags).postln;
					});
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsetlag: mixTrackNum, filter");

		cW.add(\fxgetlag, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr, filterTag;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					filterTag = aZ.findFilterTag(thisArr[0].asSymbol, thisArr[1], num2);
					if(filterTag.notNil, {
						aZ.getFilterKeys(filterTag, \lags).postln;
					});
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsetlag: mixTrackNum, filter");

		cW.add(\getfx, [\str, \num], {|str1, num1|
			var filterTag;
			if(aZ.notNil, {
				filterTag = aZ.filters[num1-1][0];
				if(filterTag.notNil, {
					aZ.getFilterKeys(filterTag, \pairs).postln;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "getfx: filterNum");

		cW.add(\getfx, [\str, \num, \num], {|str1, num1, num2|
			var filterTag, result;
			if(aZ.notNil, {
				filterTag = aZ.filters[num1-1][0];
				if(filterTag.notNil, {
					result = aZ.getFilterKeys(filterTag, \pairs)[num2-1][1];
					if(result.cs.contains("Ndef"), {
						result.key.postln;
					}, {
						result.postln;
					});
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "getfx: filterNum");

		cW.add(\getfx, [\str, \num, \str], {|str1, num1, str2|
			var filterTag, result, index;
			if(aZ.notNil, {
				filterTag = aZ.filters[num1-1][0];
				if(filterTag.notNil, {
					index = Ndef(filterTag).controlKeys.indexOf(str2);
					result = aZ.getFilterKeys(filterTag, \pairs)[index][1];
					if(result.cs.contains("Ndef"), {
						result.key.postln;
					}, {
						result.postln;
					});
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "getfx: filterNum");

		cW.add(\getlagfx, [\str, \num], {|str1, num1|
			var filterTag;
			if(aZ.notNil, {
				filterTag = aZ.filters[num1-1][0];
				if(filterTag.notNil, {
					aZ.getFilterKeys(filterTag, \lags).postln;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "getlagfx: filterNum");

		cW.add(\fxgetspec, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var filterTag, thisArg;
			if(aZ.notNil, {
				case
				{str2 == 'm'} {
					filterTag = aZ.findFilterTag(\master, 1, num1);
					thisArg = Ndef(filterTag).controlKeys[num2-1];
					aZ.getSpec(filterTag, thisArg).radpostwarn;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxgetspec: trackType, trackNum, fxArg");

		cW.add(\fxgetspec, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			var filterTag;
			if(aZ.notNil, {
				case
				{str2 == 'm'} {
					filterTag = aZ.findFilterTag(\master, 1, num1);
					aZ.getSpec(filterTag, str3).radpostwarn;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg, modType");

		cW.add(\fxgetspec, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			var filterTag, thisArg, trackType;
			if(aZ.notNil, {
				case
				{str2 == 't'} {trackType = \track}
				{str2 == 'b'} {trackType = \bus}
				{str2 == 'm'} {trackType = \master};
				filterTag = aZ.findFilterTag(trackType, num1, num2);
				thisArg = Ndef(filterTag).controlKeys[num3-1];
				aZ.getSpec(filterTag, thisArg).radpostwarn;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxgetset: trackType, trackNum, slotNum, fxArg");

		cW.add(\fxgetspec, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			var filterTag, trackType;
			if(aZ.notNil, {
				case
				{str2 == 't'} {trackType = \track}
				{str2 == 'b'} {trackType = \bus}
				{str2 == 'm'} {trackType = \master};
				filterTag = aZ.findFilterTag(trackType, num1, num2);
				aZ.getSpec(filterTag, str3).radpostwarn;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxgetset: trackType, trackNum, slotNum, fxArg");

		cW.add(\fxgetspec, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr, filterTag, thisArg;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					filterTag = aZ.findFilterTag(thisArr[0].asSymbol, thisArr[1], num2);
					thisArg = Ndef(filterTag).controlKeys[num3-1];
					aZ.getSpec(filterTag, thisArg).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxgetset: mixTrackNum, filter");

		cW.add(\fxgetspec, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var trackArr, thisArr, filterTag;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					filterTag = aZ.findFilterTag(thisArr[0].asSymbol, thisArr[1], num2);
					aZ.getSpec(filterTag, str2).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxgetset: mixTrackNum, filter");

		cW.add(\getfxspec, [\str, \num, \num], {|str1, num1, num2|
			var filterTag, thisArg;
			if(aZ.notNil, {
				filterTag = aZ.filters[num1-1][0];
				thisArg = Ndef(filterTag).controlKeys[num2-1];
				aZ.getSpec(filterTag, thisArg).radpostwarn;
			}, {
				"could not find assemblage".warn;
			});
		}, "getfxspec: filterNum, fxArg");

		cW.add(\getfxspec, [\str, \num, \str], {|str1, num1, str2|
			var filterTag;
			if(aZ.notNil, {
				filterTag = aZ.filters[num1-1][0];
				aZ.getSpec(filterTag, str2).radpostwarn;
			}, {
				"could not find assemblage".warn;
			});
		}, "getfxspec: filterNum, fxArg");
		//fx modulation
		cW.add(\fxset, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {aZ.modFxTrack(\track, 1, num1, num2, modArgs[0], modArgs[1],
					modifier: modifier);}
				{str2 == 'b'} {aZ.modFxTrack(\bus, 1, num1, num2, modArgs[0], modArgs[1],
					modifier: modifier);}
				{str2 == 'm'} {aZ.modFxTrack(\master, 1, num1, num2, modArgs[0], modArgs[1],
					modifier: modifier);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg, modType");

		cW.add(\fxset, [\str, \str, \num, \str, \str], {|str1, str2, num1, str3, str4|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str4.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {aZ.modFxTrack(\track, 1, num1, str3, modArgs[0], modArgs[1],
					modifier: modifier);}
				{str2 == 'b'} {aZ.modFxTrack(\bus, 1, num1, str3, modArgs[0], modArgs[1],
					modifier: modifier);}
				{str2 == 'm'} {aZ.modFxTrack(\master, 1, num1, str3, modArgs[0], modArgs[1],
					modifier: modifier);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackSlot, fxArg, modType");

		cW.add(\fxset, [\str, \str, \num, \num, \num, \str], {|str1, str2, num1, num2, num3, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {aZ.modFxTrack(\track, num1, num2, num3, modArgs[0], modArgs[1],
					modifier: modifier);}
				{str2 == 'b'} {aZ.modFxTrack(\bus, num1, num2, num3, modArgs[0], modArgs[1],
					modifier: modifier);}
				{str2 == 'm'} {aZ.modFxTrack(\master, 1, num2, num3, modArgs[0], modArgs[1],
					modifier: modifier);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, slotNum, fxArg, modType");

		cW.add(\fxset, [\str, \str, \num, \num, \str, \str], {|str1, str2, num1, num2, str3, str4|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str4.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {aZ.modFxTrack(\track, num1, num2, str3, modArgs[0], modArgs[1],
					modifier: modifier
				);}
				{str2 == 'b'} {aZ.modFxTrack(\bus, num1, num2, str3, modArgs[0], modArgs[1],
					modifier: modifier
				);}
				{str2 == 'm'} {aZ.modFxTrack(\master, 1, num2, str3, modArgs[0], modArgs[1],
					modifier: modifier
				);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, slotNum, fxArg, modType");

		cW.add(\fxset, [\str, \num, \num, \num, \str], {|str1, num1, num2, num3, str2|
			var trackArr, thisArr, modArgs, modifier;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				modArgs = str2.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.modFxTrack(thisArr[0].asSymbol, thisArr[1], num2, num3, modArgs[0], modArgs[1],
						modifier: modifier
					);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\fxset, [\str, \num, \num, \str, \str], {|str1, num1, num2, str2, str3|
			var trackArr, thisArr, modArgs, modifier;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.modFxTrack(thisArr[0].asSymbol, thisArr[1], num2, str2, modArgs[0], modArgs[1],
						modifier: modifier);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\setfx, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str2.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				aZ.modFx(num1, num2, modArgs[0], modArgs[1], modifier: modifier);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg, mod");

		cW.add(\setfx, [\str, \num, \str, \str], {|str1, num1, str2, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				aZ.modFx(num1, str2, modArgs[0], modArgs[1], modifier: modifier);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg, mod");

		//get and set fx modulation
		cW.add(\modfxset, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getFxMod(\track, 1, num1, num2).radpostwarn;}
				{str2 == 'b'} {aZ.getFxMod(\bus, 1, num1, num2).radpostwarn;}
				{str2 == 'm'} {aZ.getFxMod(\master, 1, num1, num2).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackSlot, arg");

		cW.add(\modfxset, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getFxMod(\track, 1, num1, str3).radpostwarn;}
				{str2 == 'b'} {aZ.getFxMod(\bus, 1, num1, str3).radpostwarn;}
				{str2 == 'm'} {aZ.getFxMod(\master, 1, num1, str3).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackSlot, arg");

		cW.add(\modfxset, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getFxMod(\track, num1, num2, num3).radpostwarn;}
				{str2 == 'b'} {aZ.getFxMod(\bus, num1, num2, num3).radpostwarn;}
				{str2 == 'm'} {aZ.getFxMod(\master, 1, num2, num3).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackNum, trackSlot, arg");

		cW.add(\modfxset, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getFxMod(\track, num1, num2, str3).radpostwarn;}
				{str2 == 'b'} {aZ.getFxMod(\bus, num1, num2, str3).radpostwarn;}
				{str2 == 'm'} {aZ.getFxMod(\master, 1, num2, str3).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackNum, trackSlot, arg");

		cW.add(\modfxset, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.getFxMod(thisArr[0].asSymbol, thisArr[1], num2, 1).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: mixTrackNum, trackSlot, arg");

		cW.add(\modfxset, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.getFxMod(thisArr[0].asSymbol, thisArr[1], num2, num3).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: mixTrackNum, trackSlot, arg");

		cW.add(\modfxset, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.getFxMod(thisArr[0].asSymbol, thisArr[1], num2, str2).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: mixTrackNum, trackSlot, arg");

		cW.add(\modsetfx, [\str, \num, \num], {|str1, num1, num2|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.getFxMod(filterKey[0], filterKey[1], filterKey[1], num2).radpostwarn;
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\modsetfx, [\str, \num, \str], {|str1, num1, str2|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.getFxMod(filterKey[0], filterKey[1], filterKey[1], str2).radpostwarn;
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\modfxset, [\str, \str, \num, \num, \arr], {|str1, str2, num1, num2, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFxMod(\track, 1, num1, num2, arr1);}
				{str2 == 'b'} {aZ.setFxMod(\bus, 1, num1, num2, arr1);}
				{str2 == 'm'} {aZ.setFxMod(\master, 1, num1, num2, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackSlot, arg");

		cW.add(\modfxset, [\str, \str, \num, \str, \arr], {|str1, str2, num1, str3, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFxMod(\track, 1, num1, str3, arr1);}
				{str2 == 'b'} {aZ.setFxMod(\bus, 1, num1, str3, arr1);}
				{str2 == 'm'} {aZ.setFxMod(\master, 1, num1, str3, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackSlot, arg");

		cW.add(\modfxset, [\str, \str, \num, \num, \num, \arr], {|str1, str2, num1, num2, num3, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFxMod(\track, num1, num2, num3, arr1);}
				{str2 == 'b'} {aZ.setFxMod(\bus, num1, num2, num3, arr1);}
				{str2 == 'm'} {aZ.setFxMod(\master, 1, num2, num3, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackNum, trackSlot, arg");

		cW.add(\modfxset, [\str, \str, \num, \num, \str, \arr], {|str1, str2, num1, num2, str3, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setFxMod(\track, num1, num2, str3, arr1);}
				{str2 == 'b'} {aZ.setFxMod(\bus, num1, num2, str3, arr1);}
				{str2 == 'm'} {aZ.setFxMod(\master, 1, num2, str3, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackNum, trackSlot, arg");

		cW.add(\modfxset, [\str, \num, \num, \arr], {|str1, num1, num2, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFxMod(thisArr[0].asSymbol, thisArr[1], 1, num2, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: mixTrackNum, trackSlot, arg");

		cW.add(\modfxset, [\str, \num, \str, \arr], {|str1, num1, str2, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFxMod(thisArr[0].asSymbol, thisArr[1], 1, str2, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: mixTrackNum, trackSlot, arg");

		cW.add(\modfxset, [\str, \num, \num, \num, \arr], {|str1, num1, num2, num3, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFxMod(thisArr[0].asSymbol, thisArr[1], num2, num3, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: mixTrackNum, trackSlot, arg");

		cW.add(\modfxset, [\str, \num, \num, \str, \arr], {|str1, num1, num2, str2, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setFxMod(thisArr[0].asSymbol, thisArr[1], num2, str2, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: mixTrackNum, trackSlot, arg");

		cW.add(\modsetfx, [\str, \num, \num, \num, \arr], {|str1, num1, num2, num3, arr1|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.setFxMod(filterKey[0], filterKey[1], num2, num3, arr1);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\modsetfx, [\str, \num, \num, \str, \arr], {|str1, num1, num2, str2, arr1|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.setFxMod(filterKey[0], filterKey[1], num2, str2, arr1);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\unmodfx, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodFxTrack(\track, 1, num1, num2);}
				{str2 == 'b'} {aZ.unmodFxTrack(\bus, 1, num1, num2);}
				{str2 == 'm'} {aZ.unmodFxTrack(\master, 1, num1, num2);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodFxTrack(\track, 1, num1, str3);}
				{str2 == 'b'} {aZ.unmodFxTrack(\bus, 1, num1, str3);}
				{str2 == 'm'} {aZ.unmodFxTrack(\master, 1, num1, str3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodFxTrack(\track, num1, num2, num3);}
				{str2 == 'b'} {aZ.unmodFxTrack(\bus, num1, num2, num3);}
				{str2 == 'm'} {aZ.unmodFxTrack(\master, 1, num2, num3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodFxTrack(\track, num1, num2, str3);}
				{str2 == 'b'} {aZ.unmodFxTrack(\bus, num1, num2, str3);}
				{str2 == 'm'} {aZ.unmodFxTrack(\master, 1, num2, str3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \str, \num], {|str1, str2, num1, str3, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodFxTrack(\track, 1, num1, str3, num3);}
				{str2 == 'b'} {aZ.unmodFxTrack(\bus, 1, num1, str3, num3);}
				{str2 == 'm'} {aZ.unmodFxTrack(\master, 1, num1, str3, num3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \num, \num, \num], {|str1, str2, num1, num2, num3, num4|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodFxTrack(\track, num1, num2, num3, num4);}
				{str2 == 'b'} {aZ.unmodFxTrack(\bus, num1, num2, num3, num4);}
				{str2 == 'm'} {aZ.unmodFxTrack(\master, 1, num2, num3, num4);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \num, \str, \num], {|str1, str2, num1, num2, str3, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodFxTrack(\track, num1, num2, str3, num3);}
				{str2 == 'b'} {aZ.unmodFxTrack(\bus, num1, num2, str3, num3);}
				{str2 == 'm'} {aZ.unmodFxTrack(\master, 1, num2, str3, num3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: trackType, trackNum, trackSlot, arg");

		/*		cW.add(\unmodfx, [\str, \num, \num], {|str1, num1, num2|
		var trackArr, thisArr;
		if(aZ.notNil, {
		trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
		if(num1 <= (trackArr.size), {
		thisArr = trackArr[num1-1];
		if(thisArr[1].isNil, {thisArr[1] = 1});
		aZ.unmodFxTrack(thisArr[0].asSymbol, thisArr[1], 1, num2);
		}, {
		"track not found".warn;
		});
		}, {
		"could not find assemblage".warn;
		});
		}, "modfxset: mixTrackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \num, \str], {|str1, num1, str2|
		var trackArr, thisArr;
		if(aZ.notNil, {
		trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
		if(num1 <= (trackArr.size), {
		thisArr = trackArr[num1-1];
		if(thisArr[1].isNil, {thisArr[1] = 1});
		aZ.unmodFxTrack(thisArr[0].asSymbol, thisArr[1], 1, str2);
		}, {
		"track not found".warn;
		});
		}, {
		"could not find assemblage".warn;
		});
		}, "modfxset: mixTrackNum, trackSlot, arg");*/

		cW.add(\fxunmod, [\str, \num, \num], {|str1, num1, num2|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.unmodFxTrack(filterKey[0], filterKey[1], filterKey[1], num2);
			}, {
				"could not find assemblage".warn;
			});
		}, "fxunmod: filterNum, fxArg");

		cW.add(\fxunmod, [\str, \num, \str], {|str1, num1, str2|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.unmodFxTrack(filterKey[0], filterKey[1], filterKey[1], str2);
			}, {
				"could not find assemblage".warn;
			});
		}, "fxunmod: filterNum, fxArg");

		cW.add(\unmodfx, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmodFxTrack(thisArr[0].asSymbol, thisArr[1], num2, num3);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodfx: mixTrackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmodFxTrack(thisArr[0].asSymbol, thisArr[1], num2, str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodfx: mixTrackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \num, \num, \num, \num], {|str1, num1, num2, num3, num4|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmodFxTrack(thisArr[0].asSymbol, thisArr[1], num2, num3, num4);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodfx: mixTrackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \num, \str, \num, \num], {|str1, num1, str2, num2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmodFxTrack(thisArr[0].asSymbol, thisArr[1], num2, str2, num3);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodfx: mixTrackNum, trackSlot, arg");

		cW.add(\fxunmod, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.unmodFxTrack(filterKey[0], filterKey[1], filterKey[1], num2, num3);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\fxunmod, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.unmodFxTrack(filterKey[0], filterKey[1], filterKey[1], str2, num2);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		//assemblage presets:
		//fx presets
		cW.add(\fxload, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.loadFxTrackPreset(\track, num1, 1, str3);}
				{str2 == 'b'} {aZ.loadFxTrackPreset(\bus, num1, 1, str3);}
				{str2 == 'm'} {aZ.loadFxTrackPreset(\master, 1, num1, str3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxload: trackType, slot, preset");

		cW.add(\fxload, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.loadFxTrackPreset(\track, num1, num2, str3);}
				{str2 == 'b'} {aZ.loadFxTrackPreset(\bus, num1, num2, str3);}
				{str2 == 'm'} {aZ.loadFxTrackPreset(\master, 1, num2, str3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxload: trackType, trackNum, slot");

		cW.add(\fxload, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.loadFxTrackPreset(thisArr[0].asSymbol, thisArr[1], 1, str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxload: mixTrackNum, slot1, preset");

		cW.add(\fxload, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.loadFxTrackPreset(thisArr[0].asSymbol, thisArr[1], num2, str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxload: mixTrackNum, slot, preset");

		cW.add(\loadfx, [\str, \num, \str], {|str1, num1, str2|
			var filterTag, filterTagArr;
			if(aZ.notNil, {
				filterTag = aZ.filters[num1-1][0];
				if(filterTag.notNil, {
					filterTagArr = aZ.convFilterTag(filterTag).postln;
					aZ.loadFxTrackPreset(filterTagArr[0].asSymbol, filterTagArr[1],
						filterTagArr[2], str2);
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "load: filterNum, preset");

		cW.add(\fxsave, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.writeFxTrackPreset(\track, num1, 1, str3);}
				{str2 == 'b'} {aZ.writeFxTrackPreset(\bus, num1, 1, str3);}
				{str2 == 'm'} {aZ.writeFxTrackPreset(\master, 1, num1, str3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsave: trackType, slot, preset");

		cW.add(\fxsave, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.writeFxTrackPreset(\track, num1, num2, str3);}
				{str2 == 'b'} {aZ.writeFxTrackPreset(\bus, num1, num2, str3);}
				{str2 == 'm'} {aZ.writeFxTrackPreset(\master, 1, num2, str3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsave: trackType, trackNum, slot");

		cW.add(\fxsave, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.writeFxTrackPreset(thisArr[0].asSymbol, thisArr[1], 1, str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsave: mixTrackNum, slot1, preset");

		cW.add(\fxsave, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.writeFxTrackPreset(thisArr[0].asSymbol, thisArr[1], num2, str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsave: mixTrackNum, slot, preset");

		cW.add(\savefx, [\str, \num, \str], {|str1, num1, str2|
			var filterTag, filterTagArr;
			if(aZ.notNil, {
				filterTag = aZ.filters[num1-1][0];
				if(filterTag.notNil, {
					filterTagArr = aZ.convFilterTag(filterTag).postln;
					aZ.writeFxTrackPreset(filterTagArr[0].asSymbol, filterTagArr[1],
						filterTagArr[2], str2);
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "save: filterNum, preset");

		//print fx presets
		cW.add(\fxload, [\str], {|str1|
			PresetFile.read(\filter).collect{|item|
				[PresetFile.read(\filter, item)[0], item]; }
			.sort({ arg a, b; a[0] <= b[0] }).dopostln;
		}, "fxload: prints all fx presets that can be loaded");

		cW.add(\fxload, [\str, \str], {|str1, str2|
			var resultArr;
			resultArr = PresetFile.read(\filter).collect{|item|
				[PresetFile.read(\filter, item)[0], item]; }
			.sort({ arg a, b; a[0] <= b[0] });
			resultArr.select({|item| item[0] == str2 }).dopostln;
		}, "fxload: fxType, fx presets with for this type");
		//fxs lags
		cW.add(\fxlags, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.fxTrackLags(\track, num1, 1, num2);}
				{str2 == 'b'} {aZ.fxTrackLags(\bus, num1, 1, num2);}
				{str2 == 'm'} {aZ.fxTrackLags(\master, 1, num1, num2);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsave: trackType, slot, preset");

		cW.add(\fxlags, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.fxTrackLags(\track, num1, num2, num3);}
				{str2 == 'b'} {aZ.fxTrackLags(\bus, num1, num2, num3);}
				{str2 == 'm'} {aZ.fxTrackLags(\master, 1, num2, num3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsave: trackType, trackNum, slot");

		cW.add(\fxlags, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.fxTrackLags(thisArr[0].asSymbol, thisArr[1], 1, num2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsave: mixTrackNum, slot1, preset");

		cW.add(\fxlags, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.fxTrackLags(thisArr[0].asSymbol, thisArr[1], num2, num3);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxsave: mixTrackNum, slot, preset");

		cW.add(\lagsfx, [\str, \num, \num], {|str1, num1, num2|
			var filterTag, filterTagArr;
			if(aZ.notNil, {
				filterTag = aZ.filters[num1-1][0];
				if(filterTag.notNil, {
					filterTagArr = aZ.convFilterTag(filterTag);
					aZ.fxTrackLags(filterTagArr[0].asSymbol, filterTagArr[1],
						filterTagArr[2], num2);
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "save: filterNum, preset");

		//assmeblage track presets
		cW.add(\csload, [\str], {|str1|
			if(aZ.notNil, {
				aZ.listTrackPresets(nil).dopostln;
			}, {
				"could not find assemblage".warn;
			});
		}, "csload: [trackType, presetName]");

		cW.add(\csload, [\str, \str], {|str1, str2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.listTrackPresets(\track);}
				{str2 == 'b'} {aZ.listTrackPresets(\bus);}
				{str2 == 'm'} {aZ.listTrackPresets(\master);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "csload: [trackType, presetName]");

		cW.add(\csload, [\str, \str, \str], {|str1, str2, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.loadTrackPreset(\track, 1, str3);}
				{str2 == 'b'} {aZ.loadTrackPreset(\bus, 1, str3);}
				{str2 == 'm'} {aZ.loadTrackPreset(\master, 1, str3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "csload: [trackType, presetName]");

		cW.add(\csload, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.loadTrackPreset(\track, num1, str3);}
				{str2 == 'b'} {aZ.loadTrackPreset(\bus, num1, str3);}
				{str2 == 'm'} {aZ.loadTrackPreset(\master, num1, str3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "csload: [trackType, trackNum, presetName]");

		cW.add(\csload, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.loadTrackPreset(thisArr[0].asSymbol, thisArr[1], str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "csload: mixTrackNum, slot1, preset");

		cW.add(\cssave, [\str, \str, \str], {|str1, str2, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.writeTrackPreset(\track, 1, str3);}
				{str2 == 'b'} {aZ.writeTrackPreset(\bus, 1, str3);}
				{str2 == 'm'} {aZ.writeTrackPreset(\master, 1, str3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "cssave: [trackType, presetName]");

		cW.add(\cssave, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.writeTrackPreset(\track, num1, str3);}
				{str2 == 'b'} {aZ.writeTrackPreset(\bus, num1, str3);}
				{str2 == 'm'} {aZ.writeTrackPreset(\master, num1, str3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "cssave: [trackType, trackNum, presetName]");

		cW.add(\cssave, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.writeTrackPreset(thisArr[0].asSymbol, thisArr[1], str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "cssave: mixTrackNum, slot1, preset");

			cW.add(\csfree, [\str, \str], {|str1, str2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.removeTrackFilters(\track, 1);}
				{str2 == 'b'} {aZ.removeTrackFilters(\bus, 1);}
				{str2 == 'm'} {aZ.removeTrackFilters(\master, 1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "csfree: [trackType]");

		cW.add(\csfree, [\str, \str, \num], {|str1, str2, num1, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.removeTrackFilters(\track, num1);}
				{str2 == 'b'} {aZ.removeTrackFilters(\bus, num1);}
				{str2 == 'm'} {aZ.removeTrackFilters(\master, 1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "cssave: [trackType, trackNum, presetName]");

		cW.add(\csfree, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.removeTrackFilters(thisArr[0].asSymbol, thisArr[1]);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "cssave: mixTrackNum, slot1, preset");

	}

	*loadMixCmds {

		cW.add(\mixnames, [\str], {|str1|
			if(aZ.notNil, {
				aZ.mixTrackNames.radpostwarn;
			}, {
				"could not find assemblage".warn;
			});
		}, "mixnames: posts mix track names");

		cW.add(\t, [\str, \str], {|str1, str2|
			var getTrack;
			if(aZ.notNil, {
				case
				{str2 == 'add'} {
					aZ.autoAddTrack(\track, 1, action: {|item|
						var ndefKey;
						ndefKey = item[0][0];
						Block.add(Ndef(item[0][0]).numChannels, {|item|
							aZ.input(item, \track, ndefKey.asString.divNumStr[1]);
							{aZ.refreshFunc}.defer;
						});
					});
				}
				{str2 == 'rmv'} {
					getTrack = (aZ.mixTrackNames.select{|item|
						item.asString.contains("track")
					}.last).asString.divNumStr;
					aZ.removeTrack(\track, getTrack[1]);
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "track: ['names', 'add', 'rmv']");

		cW.add(\b, [\str, \str], {|str1, str2|
			var getTrack;
			if(aZ.notNil, {
				case
				{str2 == 'add'} {
					aZ.autoAddTrack(\bus, 1);
				}
				{str2 == 'rmv'} {
					getTrack = (aZ.mixTrackNames.select{|item|
						item.asString.contains("bus");
					}.last).asString.divNumStr;
					aZ.removeTrack(\bus, getTrack[1]);
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "bus: ['add', 'rmv']");

		cW.add(\t, [\str, \str, \num], {|str1, str2, num1|
			var cond, getTrack;
			cond = Condition.new(false);
			if(aZ.notNil, {
				case
				{str2 == 'add'} {
					aZ.autoAddTrack(\track, num1, action: {|item|
						var ndefKey;
						ndefKey = item[0][0];
						Block.add(Ndef(item[0][0]).numChannels, {|item|
							aZ.input(item, \track, ndefKey.asString.divNumStr[1]);
						});
					});
				}
				{str2 == 'addn'} {
					{num1.do{
						cond.test = false;
						aZ.autoAddTrack(\track, action: {|item|
							var ndefKey;
							ndefKey = item[0][0];
							Block.add(Ndef(item[0][0]).numChannels, {|it|
								aZ.input(it, \track, ndefKey.asString.divNumStr[1]);
								server.sync;
								cond.test = true; cond.signal;
							});
							cond.wait;
							cond.test = true; cond.signal});
						cond.wait;
					};
					{aZ.refreshMixGUI}.defer;
					}.fork;
				}
				{str2 == 'rmvn'} {
					{num1.do{
						cond.test = false;
						getTrack = (aZ.mixTrackNames.select{|item|
							item.asString.contains("track")
						}.last).asString.divNumStr;
						aZ.removeTrack(\track, getTrack[1],
							{cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "track: add, chanNum");

		cW.add(\b, [\str, \str, \num], {|str1, str2, num1|
			var cond, getTrack;
			cond = Condition.new(false);
			if(aZ.notNil, {
				case
				{str2 == 'add'} {
					aZ.autoAddTrack(\bus, num1);
				}
				{str2 == 'addn'} {
					{num1.do{
						cond.test = false;
						aZ.autoAddTrack(\bus, action: {cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
				}
				{str2 == 'rmvn'} {
					{num1.do{
						cond.test = false;
						getTrack = (aZ.mixTrackNames.select{|item|
							item.asString.contains("bus")
						}.last).asString.divNumStr;
						aZ.removeTrack(\bus, getTrack[1],
							{cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "bus: add, chanNum");

		cW.add(\t, [\str, \str, \arr], {|str1, str2, arr1|
			var cond;
			cond = Condition.new(false);
			if(aZ.notNil, {
				case
				{str2 == 'rmvn'} {
					{arr1.do{|item|
						cond.test = false;
						aZ.removeTrack(\track, item,
							{cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "track: rmvn,");

		cW.add(\b, [\str, \str, \arr], {|str1, str2, arr1|
			var cond;
			cond = Condition.new(false);
			if(aZ.notNil, {
				case
				{str2 == 'rmvn'} {
					{arr1.do{|item|
						cond.test = false;
						aZ.removeTrack(\bus, item,
							{cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "track: rmvn,");

		cW.add(\t, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var cond;
			cond = Condition.new(false);
			if(aZ.notNil, {
				case
				{str2 == 'addn'} {
					{num1.do{
						cond.test = false;
						aZ.autoAddTrack(\track, num2, action: {|item|
							var ndefKey;
							ndefKey = item[0][0];
							Block.add(Ndef(item[0][0]).numChannels, {|it|
								aZ.input(it, \track, ndefKey.asString.divNumStr[1]);
								server.sync;
								cond.test = true; cond.signal;
							});
							cond.wait;
							cond.test = true; cond.signal});
						cond.wait;
					};
					{aZ.refreshMixGUI}.defer;
					}.fork;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "track: add, chanNum");

		cW.add(\b, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var cond;
			cond = Condition.new(false);
			if(aZ.notNil, {
				case
				{str2 == 'addn'} {
					{num1.do{
						cond.test = false;
						aZ.autoAddTrack(\bus, num2, action: {cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "bus: add, chanNum");

		cW.add(\vol, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setVolume(\track, num1, num2);}
				{str2 == 'b'} {aZ.setVolume(\bus, num1, num2);}
				{str2 == 'm'} {aZ.setVolume(\master, num1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, trackNum, val]");

		cW.add(\vollag, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setVolumeLag(\track, num1, num2);}
				{str2 == 'b'} {aZ.setVolumeLag(\bus, num1, num2);}
				{str2 == 'm'} {aZ.setVolumeLag(\master, num1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "vollag: [trackType, trackNum, val]");

		cW.add(\pan, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setPan(\track, num1, num2);}
				{str2 == 'b'} {aZ.setPan(\bus, num1, num2);}
				{str2 == 'm'} {aZ.setPan(\master, num1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNum, val]");

		cW.add(\panlag, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setPanLag(\track, num1, num2);}
				{str2 == 'b'} {aZ.setPanLag(\bus, num1, num2);}
				{str2 == 'm'} {aZ.setPanLag(\master, num1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "panlag: [trackType, trackNum, val]");

		cW.add(\trim, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setTrim(\track, num1, num2);}
				{str2 == 'b'} {aZ.setTrim(\bus, num1, num2);}
				{str2 == 'm'} {aZ.setTrim(\master, num1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, trackNum, val]");

		cW.add(\trimlag, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setTrimLag(\track, num1, num2);}
				{str2 == 'b'} {aZ.setTrimLag(\bus, num1, num2);}
				{str2 == 'm'} {aZ.setTrimLag(\master, num1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "trimlag: [trackType, trackNum, val]");

		cW.add(\vol, [\str, \num, \num], {|str, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				aZ.setVolume(trackArr[0], trackArr[1], num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "vol: masterTrackNum, value");

		cW.add(\vollag, [\str, \num, \num], {|str, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				aZ.setVolumeLag(trackArr[0], trackArr[1], num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "vollag: masterTrackNum, value");

		cW.add(\pan, [\str, \num, \num], {|str, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				aZ.setPan(trackArr[0], trackArr[1], num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, value");

		cW.add(\panlag, [\str, \num, \num], {|str, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				aZ.setPanLag(trackArr[0], trackArr[1], num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "panlag: masterTrackNum, value");

		cW.add(\trim, [\str, \num, \num], {|str, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				aZ.setTrim(trackArr[0], trackArr[1], num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "trim: masterTrackNum, value");

		cW.add(\trimlag, [\str, \num, \num], {|str, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				aZ.setTrimLag(trackArr[0], trackArr[1], num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "trimlag: masterTrackNum, value");
		//dash for multiple tracks
		cW.add(\vol, [\str, \str, \dash, \num], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setVolume(\track, index, num1);}
					{str2 == 'b'} {aZ.setVolume(\bus, index, num1);}
					{str2 == 'm'} {aZ.setVolume(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, trackNums, val]");

		cW.add(\vollag, [\str, \str, \dash, \num], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setVolumeLag(\track, index, num1);}
					{str2 == 'b'} {aZ.setVolumeLag(\bus, index, num1);}
					{str2 == 'm'} {aZ.setVolumeLag(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "vollag: [trackType, trackNum, val]");

		cW.add(\pan, [\str, \str, \dash, \num], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setPan(\track, index, num1);}
					{str2 == 'b'} {aZ.setPan(\bus, index, num1);}
					{str2 == 'm'} {aZ.setPan(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNum, val]");
		cW.add(\panlag, [\str, \str, \dash, \num], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setPanLag(\track, index, num1);}
					{str2 == 'b'} {aZ.setPanLag(\bus, index, num1);}
					{str2 == 'm'} {aZ.setPanLag(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "panlag: [trackType, trackNum, val]");

		cW.add(\trim, [\str, \str, \dash, \num], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setTrim(\track, index, num1);}
					{str2 == 'b'} {aZ.setTrim(\bus, index, num1);}
					{str2 == 'm'} {aZ.setTrim(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, trackNum, val]");

		cW.add(\trimlag, [\str, \str, \dash, \num], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setTrimLag(\track, index, num1);}
					{str2 == 'b'} {aZ.setTrimLag(\bus, index, num1);}
					{str2 == 'm'} {aZ.setTrimLag(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "trimlag: [trackType, trackNum, val]");

		cW.add(\vol, [\str, \dash, \num], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setVolume(trackArr[0], trackArr[1], num1);
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "vol: masterTrackNum, value");

		cW.add(\vollag, [\str, \dash, \num], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setVolumeLag(trackArr[0], trackArr[1], num1);
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "vollag: masterTrackNum, value");

		cW.add(\pan, [\str, \dash, \num], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setPan(trackArr[0], trackArr[1], num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, value");

		cW.add(\panlag, [\str, \dash, \num], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setPanLag(trackArr[0], trackArr[1], num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "panlag: masterTrackNum, value");

		cW.add(\trim, [\str, \dash, \num], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setTrim(trackArr[0], trackArr[1], num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "trim: masterTrackNum, value");

		cW.add(\trimlag, [\str, \dash, \num], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setTrimLag(trackArr[0], trackArr[1], num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "trimlag: masterTrackNum, value");
		//arrays instead of dashes
		cW.add(\vol, [\str, \str, \arr, \num], {|str1, str2, arr, num1|
			if(aZ.notNil, {
				arr.do{|index|
					case
					{str2 == 't'} {aZ.setVolume(\track, index, num1);}
					{str2 == 'b'} {aZ.setVolume(\bus, index, num1);}
					{str2 == 'm'} {aZ.setVolume(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, trackNums, val]");

		cW.add(\vollag, [\str, \str, \arr, \num], {|str1, str2, arr, num1|
			if(aZ.notNil, {
				arr.do{|index|
					case
					{str2 == 't'} {aZ.setVolumeLag(\track, index, num1);}
					{str2 == 'b'} {aZ.setVolumeLag(\bus, index, num1);}
					{str2 == 'm'} {aZ.setVolumeLag(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "vollag: [trackType, trackNum, val]");

		cW.add(\pan, [\str, \str, \arr, \num], {|str1, str2, arr, num1|
			if(aZ.notNil, {
				arr.do{|index|
					case
					{str2 == 't'} {aZ.setPan(\track, index, num1);}
					{str2 == 'b'} {aZ.setPan(\bus, index, num1);}
					{str2 == 'm'} {aZ.setPan(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNum, val]");

		cW.add(\panlag, [\str, \str, \arr, \num], {|str1, str2, arr, num1|
			if(aZ.notNil, {
				arr.do{|index|
					case
					{str2 == 't'} {aZ.setPanLag(\track, index, num1);}
					{str2 == 'b'} {aZ.setPanLag(\bus, index, num1);}
					{str2 == 'm'} {aZ.setPanLag(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "panlag: [trackType, trackNum, val]");

		cW.add(\trim, [\str, \str, \arr, \num], {|str1, str2, arr, num1|
			if(aZ.notNil, {
				arr.do{|index|
					case
					{str2 == 't'} {aZ.setTrim(\track, index, num1);}
					{str2 == 'b'} {aZ.setTrim(\bus, index, num1);}
					{str2 == 'm'} {aZ.setTrim(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, trackNum, val]");

		cW.add(\trimlag, [\str, \str, \arr, \num], {|str1, str2, arr, num1|
			if(aZ.notNil, {
				arr.do{|index|
					case
					{str2 == 't'} {aZ.setTrimLag(\track, index, num1);}
					{str2 == 'b'} {aZ.setTrimLag(\bus, index, num1);}
					{str2 == 'm'} {aZ.setTrimLag(\master, index, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "trimlag: [trackType, trackNum, val]");

		cW.add(\vol, [\str, \arr, \num], {|str, arr, num1|
			var trackArr;
			if(aZ.notNil, {
				arr.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setVolume(trackArr[0], trackArr[1], num1);
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "vol: masterTrackNum, value");

		cW.add(\vollag, [\str, \arr, \num], {|str, arr, num1|
			var trackArr;
			if(aZ.notNil, {
				arr.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setVolumeLag(trackArr[0], trackArr[1], num1);
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "vollag: masterTrackNum, value");

		cW.add(\pan, [\str, \arr, \num], {|str, arr, num1|
			var trackArr;
			if(aZ.notNil, {
				arr.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setPan(trackArr[0], trackArr[1], num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, value");

		cW.add(\panlag, [\str, \arr, \num], {|str, arr, num1|
			var trackArr;
			if(aZ.notNil, {
				arr.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setPanLag(trackArr[0], trackArr[1], num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "panlag: masterTrackNum, value");

		cW.add(\trim, [\str, \arr, \num], {|str, arr, num1|
			var trackArr;
			if(aZ.notNil, {
				arr.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setTrim(trackArr[0], trackArr[1], num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "trim: masterTrackNum, value");

		cW.add(\trimlag, [\str, \arr, \num], {|str, arr, num1|
			var trackArr;
			if(aZ.notNil, {
				arr.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setTrimLag(trackArr[0], trackArr[1], num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "trimlag: masterTrackNum, value");
		//bulk track controls
		cW.add(\vol, [\str, \str, \num], {|str1, str2, num1|
			var trackArr, string;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					aZ.setVolume(item[0].asSymbol, item[1], num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, val]");

		cW.add(\vollag, [\str, \str, \num], {|str1, str2, num1|
			var trackArr, string;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					aZ.setVolumeLag(item[0].asSymbol, item[1], num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "vollag: [trackType, val]");

		cW.add(\pan, [\str, \str, \num], {|str1, str2, num1|
			var trackArr, string;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					aZ.setPan(item[0].asSymbol, item[1], num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, val]");

		cW.add(\panlag, [\str, \str, \num], {|str1, str2, num1|
			var trackArr, string;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					aZ.setPanLag(item[0].asSymbol, item[1], num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "panlag: [trackType, val]");

		cW.add(\trim, [\str, \str, \num], {|str1, str2, num1|
			var trackArr, string;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					aZ.setTrim(item[0].asSymbol, item[1], num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, val]");

		cW.add(\trimlag, [\str, \str, \num], {|str1, str2, num1|
			var trackArr, string;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					aZ.setTrimLag(item[0].asSymbol, item[1], num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "trimlag: [trackType, val]");

		cW.add(\vollag, [\str, \num], {|str1, num1|
			var trackArr, string;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				trackArr.do{|item| if(item[1].isNil, {item[1] = 1}) };
				trackArr.do{|item|
					aZ.setVolumeLag(item[0].asSymbol, item[1], num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "vollag: val");

		cW.add(\panlag, [\str, \num], {|str1, num1|
			var trackArr, string;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				trackArr.do{|item| if(item[1].isNil, {item[1] = 1}) };
				trackArr.do{|item|
					aZ.setPanLag(item[0].asSymbol, item[1], num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "panlag: val");

		cW.add(\trimlag, [\str, \num], {|str1, num1|
			var trackArr, string;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				trackArr.do{|item| if(item[1].isNil, {item[1] = 1}) };
				trackArr.do{|item|
					aZ.setTrimLag(item[0].asSymbol, item[1], num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "trimlag: val");

		//mod mixer
		cW.add(\modvol, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getMixMod(\track, num1, \vol).radpostwarn;}
				{str2 == 'b'} {aZ.getMixMod(\bus, num1, \vol).radpostwarn;}
				{str2 == 'm'} {aZ.getMixMod(\master, num1, \vol).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "modvol: trackType, arg");

		cW.add(\modvol, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.getMixMod(thisArr[0].asSymbol, thisArr[1], \vol).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modvol: mixTrackNum, arg");

		cW.add(\modvol, [\str, \str, \arr], {|str1, str2, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setMixMod(\track, 1, \vol, arr1);}
				{str2 == 'b'} {aZ.setMixMod(\bus, 1, \vol, arr1);}
				{str2 == 'm'} {aZ.setMixMod(\master, 1, \vol, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modvol: trackType, arg");

		cW.add(\modvol, [\str, \str, \num, \arr], {|str1, str2, num1, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setMixMod(\track, num1, \vol, arr1);}
				{str2 == 'b'} {aZ.setMixMod(\bus, num1, \vol, arr1);}
				{str2 == 'm'} {aZ.setMixMod(\master, 1, \vol, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modvol: trackType, trackNum, extraArgs");

		cW.add(\modvol, [\str, \num, \arr], {|str1, num1, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setMixMod(thisArr[0].asSymbol, thisArr[1], \vol, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modvol: mixTrackNum, extraArgs");

		cW.add(\unmodvol, [\str, \str], {|str1, str2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodMix(\track, 1, \vol).radpostwarn;}
				{str2 == 'b'} {aZ.unmodMix(\bus, 1, \vol).radpostwarn;}
				{str2 == 'm'} {aZ.unmodMix(\master, 1, \vol).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodtrim: trackType");

		cW.add(\unmodvol, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodMix(\track, num1, \vol).radpostwarn;}
				{str2 == 'b'} {aZ.unmodMix(\bus, num1, \vol).radpostwarn;}
				{str2 == 'm'} {aZ.unmodMix(\master, num1, \vol).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodvol: trackType, trackNum");

		cW.add(\unmodvol, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmodMix(thisArr[0].asSymbol, thisArr[1], \vol).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodvol: mixTrackNum");

		cW.add(\modpan, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getMixMod(\track, num1, \pan).radpostwarn;}
				{str2 == 'b'} {aZ.getMixMod(\bus, num1, \pan).radpostwarn;}
				{str2 == 'm'} {aZ.getMixMod(\master, num1, \pan).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "modpan: trackType, arg");

		cW.add(\modpan, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.getMixMod(thisArr[0].asSymbol, thisArr[1], \pan).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modpan: mixTrackNum, arg");

		cW.add(\modpan, [\str, \str, \arr], {|str1, str2, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setMixMod(\track, 1, \pan, arr1);}
				{str2 == 'b'} {aZ.setMixMod(\bus, 1, \pan, arr1);}
				{str2 == 'm'} {aZ.setMixMod(\master, 1, \pan, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modpan: trackType, arg");

		cW.add(\modpan, [\str, \str, \num, \arr], {|str1, str2, num1, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setMixMod(\track, num1, \pan, arr1);}
				{str2 == 'b'} {aZ.setMixMod(\bus, num1, \pan, arr1);}
				{str2 == 'm'} {aZ.setMixMod(\master, 1, \pan, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modpan: trackType, trackNum, extraArgs");

		cW.add(\modpan, [\str, \num, \arr], {|str1, num1, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setMixMod(thisArr[0].asSymbol, thisArr[1], \pan, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modpan: mixTrackNum, extraArgs");

		cW.add(\unmodpan, [\str, \str], {|str1, str2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodMix(\track, 1, \pan).radpostwarn;}
				{str2 == 'b'} {aZ.unmodMix(\bus, 1, \pan).radpostwarn;}
				{str2 == 'm'} {aZ.unmodMix(\master, 1, \pan).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodtrim: trackType");

		cW.add(\unmodpan, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodMix(\track, num1, \pan).radpostwarn;}
				{str2 == 'b'} {aZ.unmodMix(\bus, num1, \pan).radpostwarn;}
				{str2 == 'm'} {aZ.unmodMix(\master, num1, \pan).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodpan: trackType, trackNum");

		cW.add(\unmodpan, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmodMix(thisArr[0].asSymbol, thisArr[1], \pan).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodpan: mixTrackNum");

		cW.add(\modtrim, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getMixMod(\track, num1, \trim).radpostwarn;}
				{str2 == 'b'} {aZ.getMixMod(\bus, num1, \trim).radpostwarn;}
				{str2 == 'm'} {aZ.getMixMod(\master, num1, \trim).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "modtrim: trackType, arg");

		cW.add(\modtrim, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.getMixMod(thisArr[0].asSymbol, thisArr[1], \trim).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modtrim: mixTrackNum, arg");

		cW.add(\modtrim, [\str, \str, \arr], {|str1, str2, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setMixMod(\track, 1, \trim, arr1);}
				{str2 == 'b'} {aZ.setMixMod(\bus, 1, \trim, arr1);}
				{str2 == 'm'} {aZ.setMixMod(\master, 1, \trim, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modtrim: trackType, arg");

		cW.add(\modtrim, [\str, \str, \num, \arr], {|str1, str2, num1, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setMixMod(\track, num1, \trim, arr1);}
				{str2 == 'b'} {aZ.setMixMod(\bus, num1, \trim, arr1);}
				{str2 == 'm'} {aZ.setMixMod(\master, 1, \trim, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modtrim: trackType, trackNum, extraArgs");

		cW.add(\modtrim, [\str, \num, \arr], {|str1, num1, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.setMixMod(thisArr[0].asSymbol, thisArr[1], \trim, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modtrim: mixTrackNum, extraArgs");

		cW.add(\unmodtrim, [\str, \str], {|str1, str2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodMix(\track, 1, \trim).radpostwarn;}
				{str2 == 'b'} {aZ.unmodMix(\bus, 1, \trim).radpostwarn;}
				{str2 == 'm'} {aZ.unmodMix(\master, 1, \trim).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodtrim: trackType");

		cW.add(\unmodtrim, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodMix(\track, num1, \trim).radpostwarn;}
				{str2 == 'b'} {aZ.unmodMix(\bus, num1, \trim).radpostwarn;}
				{str2 == 'm'} {aZ.unmodMix(\master, num1, \trim).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodtrim: trackType, trackNum");

		cW.add(\unmodtrim, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmodMix(thisArr[0].asSymbol, thisArr[1], \trim).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodtrim: mixTrackNum");

		//sends
		cW.add(\snd, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSend(\track, num1, 1, num2);}
				{str2 == 'b'} {aZ.setSend(\bus, num1, 1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [trackType, trackNum, busNum]");

		cW.add(\snd, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSend(\track, num1, num2, num3);}
				{str2 == 'b'} {aZ.setSend(\bus, num1, num2, num3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [trackType, trackNum, slotNum, busNum]");

		cW.add(\snd, [\str, \str, \num, \num, \num, \num], {|str1, str2, num1, num2, num3, num4|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSend(\track, num1, num2, num3, num4);}
				{str2 == 'b'} {aZ.setSend(\bus, num1, num2, num3, num4);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [trackType, trackNum, slotNum, busNum, val]");

		cW.add(\snd, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSend(thisArr[0].asSymbol, thisArr[1], 1, num2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [mixTrackNum, busNum]");

		cW.add(\snd, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSend(thisArr[0].asSymbol, thisArr[1], num2, num3);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [mixTrackNum, slotNum, busNum]");

		cW.add(\snd, [\str, \num, \num, \num, \num], {|str1, num1, num2, num3, num4|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSend(thisArr[0].asSymbol, thisArr[1], num2, num3, num4);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [mixTrackNum, slotNum, busNum, val]");

		cW.add(\sndry, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSend(\track, num1, 1, num2, dirMaster: false);}
				{str2 == 'b'} {aZ.setSend(\bus, num1, 1, num2, dirMaster: false);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [trackType, trackNum, busNum]");
		cW.add(\sndry, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSend(\track, num1, num2, num3, dirMaster: false);}
				{str2 == 'b'} {aZ.setSend(\bus, num1, num2, num3, dirMaster: false);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [trackType, trackNum, slotNum, busNum]");

		cW.add(\sndry, [\str, \str, \num, \num, \num, \num], {|str1, str2, num1, num2, num3, num4|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSend(\track, num1, num2, num3, num4);}
				{str2 == 'b'} {aZ.setSend(\bus, num1, num2, num3, num4);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "sndry: [trackType, trackNum, slotNum, busNum, val]");

		cW.add(\sndry, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSend(thisArr[0].asSymbol, thisArr[1], 1, num2, dirMaster: false);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [mixTrackNum, busNum]");

		cW.add(\sndry, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSend(thisArr[0].asSymbol, thisArr[1], num2, num3, dirMaster: false);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [mixTrackNum, slotNum, busNum]");

		cW.add(\sndry, [\str, \num, \num, \num, \num], {|str1, num1, num2, num3, num4|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSend(thisArr[0].asSymbol, thisArr[1], num2, num3, num4, false);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "snd: [mixTrackNum, slotNum, busNum, val]");

		cW.add(\sndset, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSendKnob(\track, num1, 1, num2);}
				{str2 == 'b'} {aZ.setSendKnob(\bus, num1, 1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "setsnd: [mixTrackNum, slotNum, busNum, val]");

		cW.add(\sndset, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSendKnob(\track, num1, num2, num3);}
				{str2 == 'b'} {aZ.setSendKnob(\bus, num1, num2, num3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "setsnd: [trackType, trackNum, slotNum, busNum]");

		cW.add(\sndset, [\str, \str, \num, \num, \num, \num], {|str1, str2, num1, num2, num3, num4|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSendKnob(\track, num1, num2, num3, num4);}
				{str2 == 'b'} {aZ.setSendKnob(\bus, num1, num2, num3, num4);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "setsnd: [trackType, trackNum, slotNum, busNum, val]");

		cW.add(\sndset, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSendKnob(thisArr[0].asSymbol, thisArr[1], 1, num2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "setsnd: [mixTrackNum, busNum]");

		cW.add(\sndset, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSendKnob(thisArr[0].asSymbol, thisArr[1], num2, num3);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "setsnd: [mixTrackNum, slotNum, busNum]");

		cW.add(\sndset, [\str, \num, \num, \num, \num], {|str1, num1, num2, num3, num4|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSendKnob(thisArr[0].asSymbol, thisArr[1], num2, num3, num4);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "setsnd: [mixTrackNum, slotNum, busNum, val]");

		cW.add(\sndset, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {
					aZ.modSend(\track, 1, num1, modArgs[0], modArgs[1], modifier: modifier);
				}
				{str2 == 'b'} {
					aZ.modSend(\bus, 1, num1, modArgs[0], modArgs[1], modifier: modifier);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "sndset: trackType, fxArg, modType");

		cW.add(\sndset, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {
					aZ.modSend(\track, num1, num2, modArgs[0], modArgs[1], modifier: modifier);
				}
				{str2 == 'b'} {
					aZ.modSend(\bus, num1, num2, modArgs[0], modArgs[1], modifier: modifier);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "sndset: trackType, trackNum, fxArg, modType");

		cW.add(\sndset, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var trackArr, thisArr, modArgs, modifier;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				modArgs = str2.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					thisArr.postln;
					aZ.modSend(thisArr[0].asSymbol, thisArr[1], num2, modArgs[0], modArgs[1],
						modifier: modifier);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "sndset: mixTrackNum, filter");

		cW.add(\sndset, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, thisArr, modArgs, modifier;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				modArgs = str2.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					thisArr.postln;
					aZ.modSend(thisArr[0].asSymbol, thisArr[1], 1, modArgs[0], modArgs[1],
						modifier: modifier);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "sndset: mixTrackNum, filter");

		cW.add(\modsndset, [\str, \str, \num, \arr], {|str1, str2, num1, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSndMod(\track, num1, 1, arr1);}
				{str2 == 'b'} {aZ.setSndMod(\bus, num1, 1, arr1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "modsndset: mixTrackNum, slotNum");

		cW.add(\modsndset, [\str, \num, \arr], {|str1, num1, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSndMod(thisArr[0].asSymbol, thisArr[1], 1, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modsndset: mixTrackNum, slotNum");

		cW.add(\modsndset, [\str, \str, \num, \num, \arr], {|str1, str2, num1, num2, arr1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSndMod(\track, num1, num2, arr1);}
				{str2 == 'b'} {aZ.setSndMod(\bus, num1, num2, arr1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "modsndset: mixTrackNum, slotNum");

		cW.add(\modsndset, [\str, \num, \num, \arr], {|str1, num1, num2, arr1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSndMod(thisArr[0].asSymbol, thisArr[1], num2, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modsndset: mixTrackNum, slotNum");

		cW.add(\modsndset, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getSndMod(\track, num1, 1).radpostwarn;}
				{str2 == 'b'} {aZ.getSndMod(\bus, num1, 1).radpostwarn;}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "modsndset: mixTrackNum, slotNum");

		cW.add(\modsndset, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.getSndMod(thisArr[0].asSymbol, thisArr[1], 1).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modsndset: mixTrackNum, slotNum");

		cW.add(\modsndset, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getSndMod(\track, num1, num2).radpostwarn;}
				{str2 == 'b'} {aZ.getSndMod(\bus, num1, num2).radpostwarn;}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "modsndset: mixTrackNum, slotNum");

		cW.add(\modsndset, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.getSndMod(thisArr[0].asSymbol, thisArr[1], num2).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modsndset: mixTrackNum, slotNum");

		cW.add(\unmodsnd, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodSend(\track, num1, 1);}
				{str2 == 'b'} {aZ.unmodSend(\bus, num1, 1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodsnd: mixTrackNum, slotNum");

		cW.add(\unmodsnd, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.unmodSend(thisArr[0].asSymbol, thisArr[1], 1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodsnd: mixTrackNum, slotNum");

		cW.add(\unmodsnd, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmodSend(\track, num1, num2);}
				{str2 == 'b'} {aZ.unmodSend(\bus, num1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodsnd: mixTrackNum, slotNum");

		cW.add(\unmodsnd, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.unmodSend(thisArr[0].asSymbol, thisArr[1], num2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodsnd: mixTrackNum, slotNum");

		cW.add(\mute, [\str, \str], {|str1, str2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setMute(\track, 1, 1);}
				{str2 == 'b'} {aZ.setMute(\bus, 1, 1);}
				{str2 == 'm'} {aZ.setMute(\master, 1, 1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "mute: [trackType, trackNum]");

		cW.add(\unmute, [\str, \str], {|str1, str2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setMute(\track, 1, 0);}
				{str2 == 'b'} {aZ.setMute(\bus, 1, 0);}
				{str2 == 'm'} {aZ.setMute(\master, 1, 0);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "unmute: [trackType, trackNum]");

		cW.add(\mute, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setMute(\track, num1, 1);}
				{str2 == 'b'} {aZ.setMute(\bus, num1, 1);}
				{str2 == 'm'} {aZ.setMute(\master, num1, 1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "mute: [trackType, trackNum]");

		cW.add(\unmute, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setMute(\track, num1, 0);}
				{str2 == 'b'} {aZ.setMute(\bus, num1, 0);}
				{str2 == 'm'} {aZ.setMute(\master, num1, 0);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "unmute: [trackType, trackNum]");

		cW.add(\recen, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setRec(\track, num1, 1);}
				{str2 == 'b'} {aZ.setRec(\bus, num1, 1);}
				{str2 == 'm'} {aZ.setRec(\master, num1, 1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "mute: [trackType, trackNum]");

		cW.add(\recdis, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setRec(\track, num1, 0);}
				{str2 == 'b'} {aZ.setRec(\bus, num1, 0);}
				{str2 == 'm'} {aZ.setRec(\master, num1, 0);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "unmute: [trackType, trackNum]");

		cW.add(\solo, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSolo(\track, num1, 1);}
				{str2 == 'b'} {aZ.setSolo(\bus, num1, 1);}
				{str2 == 'm'} {aZ.setSolo(\master, num1, 1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "solo: [trackType, trackNum]");

		cW.add(\unsolo, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSolo(\track, num1, 0);}
				{str2 == 'b'} {aZ.setSolo(\bus, num1, 0);}
				{str2 == 'm'} {aZ.setSolo(\master, num1, 0);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "unsolo: [trackType, trackNum]");

		cW.add(\mute, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setMute(thisArr[0].asSymbol, thisArr[1], 1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "mute: [mixTrackNum]");

		cW.add(\unmute, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setMute(thisArr[0].asSymbol, thisArr[1], 0);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmute: [mixTrackNum]");

		cW.add(\recen, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setRec(thisArr[0].asSymbol, thisArr[1], 1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "recen: [mixTrackNum]");

		cW.add(\recdis, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setRec(thisArr[0].asSymbol, thisArr[1], 0);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "recdic: [mixTrackNum]");

		cW.add(\solo, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSolo(thisArr[0].asSymbol, thisArr[1], 1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "solo: [mixTrackNum]");

		cW.add(\unsolo, [\str, \num], {|str1, num1|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 < (trackArr.size-1), {
					thisArr = trackArr[num1-1];
					aZ.setSolo(thisArr[0].asSymbol, thisArr[1], 0);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unsolo: [mixTrackNum]");

		//bulk for mute, solo and rec-enable
		cW.add(\mute, [\str, \str, \dash], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setMute(\track, index, 1);}
					{str2 == 'b'} {aZ.setMute(\bus, index, 1);}
					{str2 == 'm'} {aZ.setMute(\master, index, 1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "mute: [trackType, trackNums]");

		cW.add(\unmute, [\str, \str, \dash], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setMute(\track, index, 0);}
					{str2 == 'b'} {aZ.setMute(\bus, index, 0);}
					{str2 == 'm'} {aZ.setMute(\master, index, 0);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "unmute: [trackType, trackNums]");

		cW.add(\recen, [\str, \str, \dash], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setRec(\track, index, 1);}
					{str2 == 'b'} {aZ.setRec(\bus, index, 1);}
					{str2 == 'm'} {aZ.setRec(\master, index, 1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "recen: [trackType, trackNums]");

		cW.add(\recdis, [\str, \str, \dash], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setRec(\track, index, 0);}
					{str2 == 'b'} {aZ.setRec(\bus, index, 0);}
					{str2 == 'm'} {aZ.setRec(\master, index, 0);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "recdis: [trackType, trackNums]");

		cW.add(\solo, [\str, \str, \dash], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setSolo(\track, index, 1);}
					{str2 == 'b'} {aZ.setSolo(\bus, index, 1);}
					{str2 == 'm'} {aZ.setSolo(\master, index, 1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "solo: [trackType, trackNums]");

		cW.add(\unsolo, [\str, \str, \dash], {|str1, str2, dash, num1|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setSolo(\track, index, 0);}
					{str2 == 'b'} {aZ.setSolo(\bus, index, 0);}
					{str2 == 'm'} {aZ.setSolo(\master, index, 0);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "unsolo: [trackType, trackNums]");

		cW.add(\mute, [\str, \dash], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setMute(trackArr[0], trackArr[1], 1);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "mute: masterTrackNums");

		cW.add(\unmute, [\str, \dash], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setMute(trackArr[0], trackArr[1], 0);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "unmute: masterTrackNums");

		cW.add(\recen, [\str, \dash], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setRec(trackArr[0], trackArr[1], 1);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "recen: masterTrackNums");

		cW.add(\recdis, [\str, \dash], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setRec(trackArr[0], trackArr[1], 0);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "recdis: masterTrackNums");

		cW.add(\solo, [\str, \dash], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setSolo(trackArr[0], trackArr[1], 1);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "solo: masterTrackNums");

		cW.add(\unsolo, [\str, \dash], {|str, dash, num1|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setSolo(trackArr[0], trackArr[1], 0);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "unsolo: masterTrackNums");

		cW.add(\inmenu, [\str, \num], {|str, num1|
			var trackArr;
			if(aZ.notNil, {
				aZ.setTrackIn(num1, 0);
			}, {
				"could not find assemblage".warn;
			});
		}, "inmenu: masterTrackNums");

		cW.add(\getinmenu, [\str, \num], {|str, num1|
			var trackArr;
			if(aZ.notNil, {
				aZ.getTrackInItem(num1);
			}, {
				"could not find assemblage".warn;
			});
		}, "getinmenu: masterTrackNums");

		cW.add(\inmenu, [\str, \num, \num], {|str, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				aZ.setTrackIn(num1, num2-1);
			}, {
				"could not find assemblage".warn;
			});
		}, "inmenu: masterTrackNums");

		cW.add(\getoutmenu, [\str, \num], {|str1, num1|
			var trackArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr}[num1-1];
				aZ.getTrackOutItem(trackArr[0], trackArr[1]);
			}, {
				"could not find assemblage".warn;
			});
		}, "getoutmenu: [mixTrackNum]");

		cW.add(\getoutmenu, [\str, \str, \num], {|str1, str2, num1|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getTrackOutItem(\track, num1);}
				{str2 == 'b'} {aZ.getTrackOutItem(\bus, num1);}
				{str2 == 'm'} {aZ.getTrackOutItem(\master, num1);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "getoutmenu: [trackType, trackNum]");

		cW.add(\outmenu, [\str, \num, \num], {|str1, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr}[num1-1];
				aZ.setTrackOut(trackArr[0], trackArr[1], num2);
			}, {
				"could not find assemblage".warn;
			});
		}, "outmenu: [mixTrackNum, index]");

		cW.add(\outmenu, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setTrackOut(\track, num1, num2);}
				{str2 == 'b'} {aZ.setTrackOut(\bus, num1, num2);}
				{str2 == 'm'} {aZ.setTrackOut(\master, num1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "outmenu: [trackType, trackNum, index]");

		cW.add(\dirinrec, [\str, \arr], {|str1, arr1|
			var trackArr;
			if(aZ.notNil, {
				aZ.setDirInRec(arr1-1);
			}, {
				"could not find assemblage".warn;
			});
		}, "dirinrec: drInArr");

		cW.add(\mapouts, [\str, \arr], {|str1, arr1|
			var trackArr;
			if(aZ.notNil, {
				aZ.mapOuts(arr1);
			}, {
				"could not find assemblage".warn;
			});
		}, "mapouts: outArr");

		cW.add(\ndef, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			if(str3 == '<>', {
				if(aZ.notNil, {
					aZ.input(Ndef(str2), \track, num1);
				}, {
					"could not find assemblage".warn;
				});
			});
		}, "ndef into assemblage");

		cW.add(\ndefset, [\str, \str, \arr], {|str1, str2, arr1|
			("Ndef(" ++ str2.cs ++ ").setn" ++
				arr1.cs.replaceAt("(", 0).replaceAt(")", arr1.cs.size-1);
			).radpost.interpret;
		}, "ndef set");

		cW.add(\ndefset, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			("Ndef(" ++ str2.cs ++ ").set(" ++ str3.cs ++ ", " ++ num1 ++ ");").radpost.interpret;
		}, "ndef lag arg in ndef");

		cW.add(\ndefset, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			("Ndef(" ++ str2.cs ++ ").set(" ++ Ndef(str2).controlKeys[num1-1].cs ++ ", " ++ num2 ++ ");").radpost.interpret;
		}, "ndef lag arg in ndef");

		cW.add(\ndefxset, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			("Ndef(" ++ str2.cs ++ ").xset(" ++ str3.cs ++ ", " ++ num1 ++ ");").radpost.interpret;
		}, "ndef lag arg in ndef");

		cW.add(\ndefxset, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			("Ndef(" ++ str2.cs ++ ").xset(" ++ Ndef(str2).controlKeys[num1-1].cs ++ ", " ++ num2 ++ ");").radpost.interpret;
		}, "ndef lag arg in ndef");

		cW.add(\ndeflag, [\str, \str, \num], {|str1, str2, num1|
			Ndef(str2).controlKeys.do{|item|
				("Ndef(" ++ str2.cs ++ ").lag(" ++ item.cs ++ ", " ++ num1 ++ ");").radpost.interpret;
			}
		}, "ndef lag ndef");

		cW.add(\ndeflag, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			("Ndef(" ++ str2.cs ++ ").lag(" ++ str3.cs ++ ", " ++ num1 ++ ");").radpost.interpret;
		}, "ndef lag arg in ndef");

		cW.add(\ndeflag, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			("Ndef(" ++ str2.cs ++ ").lag(" ++ Ndef(str2).controlKeys[num1-1].cs ++ ", " ++ num2 ++ ");").radpost.interpret;
		}, "ndef lag arg in ndef");

		//modulation
		cW.add(\vol, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {aZ.modMix(\track, num1, \vol, modArgs[0], modArgs[1], modifier: modifier)}
				{str2 == 'b'} {aZ.modMix(\bus, num1, \vol, modArgs[0], modArgs[1], modifier: modifier)}
				{str2 == 'm'} {aZ.modMix(\master, num1, \vol, modArgs[0], modArgs[1], modifier: modifier)}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, trackNum, mod]");

		cW.add(\pan, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {aZ.modMix(\track, num1, \pan, modArgs[0], modArgs[1], modifier: modifier)}
				{str2 == 'b'} {aZ.modMix(\bus, num1, \pan, modArgs[0], modArgs[1], modifier: modifier)}
				{str2 == 'm'} {aZ.modMix(\master, num1, \pan, modArgs[0], modArgs[1], modifier: modifier)}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNum, mod]");

		cW.add(\trim, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {aZ.modMix(\track, num1, \trim, modArgs[0], modArgs[1], modifier: modifier);}
				{str2 == 'b'} {aZ.modMix(\bus, num1, \trim, modArgs[0], modArgs[1], modifier: modifier);}
				{str2 == 'm'} {aZ.modMix(\master, num1, \trim, modArgs[0], modArgs[1], modifier: modifier);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, trackNum, mod]");

		cW.add(\vol, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, modArgs, modifier;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				modArgs = str2.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				aZ.modMix(trackArr[0], trackArr[1], \vol, modArgs[0], modArgs[1], modifier: modifier)
			}, {
				"assemblage is already running".warn;
			});
		}, "vol: masterTrackNum, mod");

		cW.add(\pan, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, modArgs, modifier;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				modArgs = str2.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				aZ.modMix(trackArr[0], trackArr[1], \pan, modArgs[0], modArgs[1], modifier: modifier)
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, mod");

		cW.add(\trim, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, modArgs, modifier;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				modArgs = str2.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				aZ.modMix(trackArr[0], trackArr[1], \trim, modArgs[0], modArgs[1], modifier: modifier)
			}, {
				"assemblage is already running".warn;
			});
		}, "trim: masterTrackNum, mod");

		//dash for multiple tracks
		cW.add(\vol, [\str, \str, \dash, \str], {|str1, str2, dash, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				dash.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \vol, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'b'} {aZ.modMix(\bus, item, \vol, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'm'} {aZ.modMix(\master, item, \vol, modArgs[0], modArgs[1], modifier: modifier)}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, trackNums, mod]");

		cW.add(\pan, [\str, \str, \dash, \str], {|str1, str2, dash, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				dash.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \pan, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'b'} {aZ.modMix(\bus, item, \pan, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'm'} {aZ.modMix(\master, item, \pan, modArgs[0], modArgs[1], modifier: modifier)}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNums, mod]");

		cW.add(\trim, [\str, \str, \dash, \str], {|str1, str2, dash, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				dash.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \trim, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'b'} {aZ.modMix(\bus, item, \trim, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'm'} {aZ.modMix(\master, item, \trim, modArgs[0], modArgs[1], modifier: modifier)}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, trackNums, mod]");

		cW.add(\vol, [\str, \dash, \str], {|str1, dash, str2|
			var trackArr, modArgs, modifier;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					modArgs = str2.asString.radStringMod;
					if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
					{aZ.modMix(trackArr[0], trackArr[1], \vol, modArgs[0], modArgs[1], modifier: modifier)}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "vol: masterTrackNum, mod");

		cW.add(\pan, [\str, \dash, \str], {|str1, dash, str2|
			var trackArr, modArgs, modifier;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					modArgs = str2.asString.radStringMod;
					if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
					{aZ.modMix(trackArr[0], trackArr[1], \pan, modArgs[0], modArgs[1], modifier: modifier)}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, mod");

		cW.add(\trim, [\str, \dash, \str], {|str1, dash, str2|
			var trackArr, modArgs, modifier;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					modArgs = str2.asString.radStringMod;
					if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
					{aZ.modMix(trackArr[0], trackArr[1], \trim, modArgs[0], modArgs[1], modifier: modifier)}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "trim: masterTrackNum, mod");

		//arrays instead of dashes
		cW.add(\vol, [\str, \str, \arr, \str], {|str1, str2, arr, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				arr.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \vol, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'b'} {aZ.modMix(\bus, item, \vol, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'm'} {aZ.modMix(\master, item, \vol, modArgs[0], modArgs[1], modifier: modifier)}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, trackNums, mod]");

		cW.add(\pan, [\str, \str, \arr, \str], {|str1, str2, arr, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				arr.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \pan, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'b'} {aZ.modMix(\bus, item, \pan, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'm'} {aZ.modMix(\master, item, \pan, modArgs[0], modArgs[1], modifier: modifier)}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNums, mod]");

		cW.add(\trim, [\str, \str, \arr, \str], {|str1, str2, arr, str3|
			var modArgs, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				arr.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \trim, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'b'} {aZ.modMix(\bus, item, \trim, modArgs[0], modArgs[1], modifier: modifier)}
						{str2 == 'm'} {aZ.modMix(\master, item, \trim, modArgs[0], modArgs[1], modifier: modifier)}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, trackNums, mod]");

		cW.add(\vol, [\str, \arr, \str], {|str1, arr, str2|
			var trackArr, modArgs, modifier;
			if(aZ.notNil, {
				arr.do{|item|
					{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
						modArgs = str2.asString.radStringMod;
						if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
						aZ.modMix(trackArr[0], trackArr[1], \vol, modArgs[0], modArgs[1], modifier: modifier)}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "vol: masterTrackNum, value");

		cW.add(\pan, [\str, \arr, \str], {|str1, arr, str2|
			var trackArr, modArgs, modifier;
			if(aZ.notNil, {
				arr.do{|item|
					{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
						modArgs = str2.asString.radStringMod;
						if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
						aZ.modMix(trackArr[0], trackArr[1], \pan, modArgs[0], modArgs[1], modifier: modifier)}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, value");

		cW.add(\trim, [\str, \arr, \str], {|str1, arr, str2|
			var trackArr, modArgs, modifier;
			if(aZ.notNil, {
				arr.do{|item|
					{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
						modArgs = str2.asString.radStringMod;
						if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
						aZ.modMix(trackArr[0], trackArr[1], \trim, modArgs[0], modArgs[1], modifier: modifier)}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "trim: masterTrackNum, value");

		//midi assign with dash and array
		cW.add(\vol, [\str, \dash, \str, \str, \dash], {|str1, dash1, str2, str3, dash2|
			var trackArr, modArg;
			if(str2 == '<>', {
				if(aZ.notNil, {
					dash1.do{|item, index|
						{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
							modArg = dash2[index];
							aZ.modMix(trackArr[0], trackArr[1], \vol, str3, modArg, modifier: \hid);
						}.defer;
					};
				}, {
					"could not find assemblage".warn;
				});
			});
		});
		cW.add(\pan, [\str, \dash, \str, \str, \dash], {|str1, dash1, str2, str3, dash2|
			var trackArr, modArg;
			if(str2 == '<>', {
				if(aZ.notNil, {
					dash1.do{|item, index|
						{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
							modArg = dash2[index];
							aZ.modMix(trackArr[0], trackArr[1], \pan, str3, modArg, modifier: \hid);
						}.defer;
					};
				}, {
					"could not find assemblage".warn;
				});
			});
		});
		cW.add(\trim, [\str, \dash, \str, \str, \dash], {|str1, dash1, str2, str3, dash2|
			var trackArr, modArg;
			if(str2 == '<>', {
				if(aZ.notNil, {
					dash1.do{|item, index|
						{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
							modArg = dash2[index];
							aZ.modMix(trackArr[0], trackArr[1], \trim, str3, modArg, modifier: \hid);
						}.defer;
					};
				}, {
					"could not find assemblage".warn;
				});
			});
		});

		cW.add(\vol, [\str, \arr, \str, \str, \arr], {|str1, arr1, str2, str3, arr2|
			var trackArr, modArg;
			if(str2 == '<>', {
				if(aZ.notNil, {
					arr1.do{|item, index|
						{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
							modArg = arr2[index];
							aZ.modMix(trackArr[0], trackArr[1], \vol, str3, modArg, modifier: \hid);
						}.defer;
					};
				}, {
					"could not find assemblage".warn;
				});
			});
		});
		cW.add(\pan, [\str, \arr, \str, \str, \arr], {|str1, arr1, str2, str3, arr2|
			var trackArr, modArg;
			if(str2 == '<>', {
				if(aZ.notNil, {
					arr1.do{|item, index|
						{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
							modArg = arr2[index];
							aZ.modMix(trackArr[0], trackArr[1], \pan, str3, modArg, modifier: \hid);
						}.defer;
					};
				}, {
					"could not find assemblage".warn;
				});
			});
		});
		cW.add(\trim, [\str, \arr, \str, \str, \arr], {|str1, arr1, str2, str3, arr2|
			var trackArr, modArg;
			if(str2 == '<>', {
				if(aZ.notNil, {
					arr1.do{|item, index|
						{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
							modArg = arr2[index];
							aZ.modMix(trackArr[0], trackArr[1], \trim, str3, modArg, modifier: \hid);
						}.defer;
					};
				}, {
					"could not find assemblage".warn;
				});
			});
		});
		//bulk track controls
		cW.add(\vol, [\str, \str, \str], {|str1, str2, str3|
			var trackArr, modArgs, string, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					{
						aZ.modMix(item[0].asSymbol, item[1], \vol, modArgs[0], modArgs[1], modifier: modifier);
					}.defer;
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, val]");

		cW.add(\pan, [\str, \str, \str], {|str1, str2, str3|
			var trackArr, modArgs, string, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					{
						aZ.modMix(item[0].asSymbol, item[1], \pan, modArgs[0], modArgs[1], modifier: modifier)
					}.defer;
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, val]");

		cW.add(\trim, [\str, \str, \str], {|str1, str2, str3|
			var trackArr, modArgs, string, modifier;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					{
						aZ.modMix(item[0].asSymbol, item[1], \trim, modArgs[0], modArgs[1], modifier: modifier)
					}.defer;
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, val]");

		//space pan
		cW.add(\pan, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setPan(\track, num1, num3, num2);}
				{str2 == 'b'} {aZ.setPan(\bus, num1, num3, num2);}
				{str2 == 'm'} {aZ.setPan(\master, num1, num3, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNum, val]");

		cW.add(\panlag, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setPanLag(\track, num1, num3, num2);}
				{str2 == 'b'} {aZ.setPanLag(\bus, num1, num3, num2);}
				{str2 == 'm'} {aZ.setPanLag(\master, num1, num3, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "panlag: [trackType, trackNum, val]");

		cW.add(\pan, [\str, \num, \num, \num], {|str, num1, num2, num3|
			var trackArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				aZ.setPan(trackArr[0], trackArr[1], num3, num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, value");

		cW.add(\panlag, [\str, \num, \num, \num], {|str, num1, num2, num3|
			var trackArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				aZ.setPanLag(trackArr[0], trackArr[1], num3, num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "panlag: masterTrackNum, value");

		cW.add(\pan, [\str, \str, \dash, \num, \num], {|str1, str2, dash, num1, num2|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setPan(\track, index, num2, num1);}
					{str2 == 'b'} {aZ.setPan(\bus, index, num2, num1);}
					{str2 == 'm'} {aZ.setPan(\master, index, num2, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNum, val]");
		cW.add(\panlag, [\str, \str, \dash, \num, \num], {|str1, str2, dash, num1, num2|
			if(aZ.notNil, {
				dash.do{|index|
					case
					{str2 == 't'} {aZ.setPanLag(\track, index, num2, num1);}
					{str2 == 'b'} {aZ.setPanLag(\bus, index, num2, num1);}
					{str2 == 'm'} {aZ.setPanLag(\master, index, num2, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "panlag: [trackType, trackNum, val]");

		cW.add(\pan, [\str, \dash, \num, \num], {|str, dash, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setPan(trackArr[0], trackArr[1], num2, num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, value");

		cW.add(\panlag, [\str, \dash, \num, \num], {|str, dash, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setPanLag(trackArr[0], trackArr[1], num2, num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "panlag: masterTrackNum, value");

		cW.add(\pan, [\str, \str, \arr, \num, \num], {|str1, str2, arr, num1, num2|
			if(aZ.notNil, {
				arr.do{|index|
					case
					{str2 == 't'} {aZ.setPan(\track, index, num2, num1);}
					{str2 == 'b'} {aZ.setPan(\bus, index, num2, num1);}
					{str2 == 'm'} {aZ.setPan(\master, index, num2, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNum, val]");

		cW.add(\panlag, [\str, \str, \arr, \num, \num], {|str1, str2, arr, num1, num2|
			if(aZ.notNil, {
				arr.do{|index|
					case
					{str2 == 't'} {aZ.setPanLag(\track, index, num2, num1);}
					{str2 == 'b'} {aZ.setPanLag(\bus, index, num2, num1);}
					{str2 == 'm'} {aZ.setPanLag(\master, index, num2, num1);}
					;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "panlag: [trackType, trackNum, val]");

		cW.add(\pan, [\str, \arr, \num, \num], {|str, arr, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				arr.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setPan(trackArr[0], trackArr[1], num2, num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, value");

		cW.add(\panlag, [\str, \arr, \num, \num], {|str, arr, num1, num2|
			var trackArr;
			if(aZ.notNil, {
				arr.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					aZ.setPanLag(trackArr[0], trackArr[1], num2, num1);
				}
			}, {
				"assemblage is already running".warn;
			});
		}, "panlag: masterTrackNum, value");

		cW.add(\panx, [\str, \str, \num, \num], {|str1, str2, num1,  num2|
			var trackArr, string;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					aZ.setPan(item[0].asSymbol, item[1], num2, num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, val]");

		cW.add(\panxlag, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var trackArr, string;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					string = "track";
				}
				{str2 == 'b'} {
					string = "bus";
				}
				{str2 == 'm'} {
					string = "master";
				}
				;
				trackArr = aZ.mixTrackNames.select{|item| item.asString.contains(string) };
				trackArr = trackArr.collect{|item| item.asString.divNumStr};
				trackArr.do{|item|
					aZ.setPanLag(item[0].asSymbol, item[1], num2, num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "panlag: [trackType, val]");

		cW.add(\panxlag, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, string;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				trackArr.do{|item| if(item[1].isNil, {item[1] = 1}) };
				trackArr.do{|item|
					aZ.setPanLag(item[0].asSymbol, item[1], num2, num1);
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "panlag: val");

		//space mods
		cW.add(\pan, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			var modArgs, modifier, keyValues;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				case
				{str2 == 't'} {
					keyValues = Ndef(("spaceTrack" ++ num1).asSymbol).getKeysValues[num2-1][0];
					aZ.modMix(\track, num1, keyValues, modArgs[0], modArgs[1], modifier: modifier)}
				{str2 == 'b'} {
					keyValues = Ndef(("spaceBus" ++ num1).asSymbol).getKeysValues[num2-1][0];
					aZ.modMix(\bus, num1, keyValues, modArgs[0], modArgs[1], modifier: modifier)}
				{str2 == 'm'} {
					keyValues = Ndef(("spaceMaster").asSymbol).getKeysValues[num2-1][0];
					aZ.modMix(\master, num1, keyValues, modArgs[0], modArgs[1], modifier: modifier)}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNum, panNum, mod]");

		cW.add(\pan, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var trackArr, modArgs, modifier, keyValues;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				keyValues = Ndef(("space" ++ trackArr[0].asString.capitalise ++
					trackArr[1]).asSymbol).getKeysValues[num2-1][0];
				modArgs = str2.asString.radStringMod;
				if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
				aZ.modMix(trackArr[0], trackArr[1], keyValues, modArgs[0], modArgs[1], modifier: modifier)
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, panNum, mod");

		/*		cW.add(\pan, [\str, \str, \dash, \num, \str], {|str1, str2, dash, num1, str3|
		var modArgs, modifier, keyValues;
		if(aZ.notNil, {
		modArgs = str3.asString.radStringMod;modArgs = str3.asString.radStringMod;
		if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
		dash.do{|item|
		{
		case
		{str2 == 't'} {
		keyValues = Ndef(("spaceTrack" ++ item).asSymbol).getKeysValues[num1-1][0];
		aZ.modMix(\track, item, keyValues, modArgs[0], modArgs[1], modifier: modifier)}
		{str2 == 'b'} {
		keyValues = Ndef(("spaceBus" ++ item).asSymbol).getKeysValues[num1-1][0];
		aZ.modMix(\bus, item, keyValues, modArgs[0], modArgs[1], modifier: modifier)}
		{str2 == 'm'} {
		keyValues = Ndef(("spaceMaster").asSymbol).getKeysValues[num1-1][0];
		aZ.modMix(\master, item, keyValues, modArgs[0], modArgs[1], modifier: modifier)}
		;
		}.defer;
		};
		}, {
		"could not find assemblage".warn;
		});
		}, "pan: [trackType, trackNums, panNum, mod]");

		cW.add(\pan, [\str, \dash, \num, \str], {|str1, dash, num1, str2|
		var trackArr, modArgs, modifier, keyValues;
		if(aZ.notNil, {
		dash.do{|item|
		trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
		keyValues = Ndef(("space" ++ trackArr[0].asString.capitalise ++
		trackArr[1]).asSymbol.postln).getKeysValues[num1-1][0];
		modArgs = str2.asString.radStringMod;
		if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
		{aZ.modMix(trackArr[0], trackArr[1], keyValues, modArgs[0], modArgs[1],
		modifier: modifier)}.defer;
		};
		}, {
		"assemblage is already running".warn;
		});
		}, "pan: masterTrackNum, mod");*/

		cW.add(\modpan, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var keyValues;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					keyValues = Ndef(("spaceTrack" ++ num1).asSymbol).getKeysValues[num2-1][0];
					aZ.getMixMod(\track, num1, keyValues).radpostwarn;}
				{str2 == 'b'} {
					keyValues = Ndef(("spaceBus" ++ num1).asSymbol).getKeysValues[num2-1][0];
					aZ.getMixMod(\bus, num1, keyValues).radpostwarn;}
				{str2 == 'm'} {
					keyValues = Ndef(("spaceMaster").asSymbol).getKeysValues[num2-1][0];
					aZ.getMixMod(\master, num1, keyValues).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "modpan: trackType, panNum, arg");

		cW.add(\modpan, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr, keyValues;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					keyValues = Ndef(("space" ++ thisArr[0].capitalise ++
						thisArr[1]).asSymbol).getKeysValues[num2-1][0];
					aZ.getMixMod(thisArr[0].asSymbol, thisArr[1], keyValues).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modpan: mixTrackNum, panNum, arg");

		cW.add(\modpanx, [\str, \str, \num, \arr], {|str1, str2, num1, arr1|
			var keyValues;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					keyValues = Ndef(("spaceTrack" ++ 1).asSymbol).getKeysValues[num1-1][0];
					aZ.setMixMod(\track, 1, keyValues, arr1);}
				{str2 == 'b'} {
					keyValues = Ndef(("spaceBus" ++ 1).asSymbol).getKeysValues[num1-1][0];
					aZ.setMixMod(\bus, 1, keyValues, arr1);}
				{str2 == 'm'} {
					keyValues = Ndef(("spaceMaster").asSymbol).getKeysValues[num1-1][0];
					aZ.setMixMod(\master, 1, keyValues, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modpan: trackType, panNum, extraArgs");

		cW.add(\modpan, [\str, \str, \num, \num, \arr], {|str1, str2, num1, num2, arr1|
			var keyValues;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					keyValues = Ndef(("spaceTrack" ++ 1).asSymbol).getKeysValues[num2-1][0];
					aZ.setMixMod(\track, num1, keyValues, arr1);}
				{str2 == 'b'} {
					keyValues = Ndef(("spaceBus" ++ 1).asSymbol).getKeysValues[num2-1][0];
					aZ.setMixMod(\bus, num1, keyValues, arr1);}
				{str2 == 'm'} {
					keyValues = Ndef(("spaceMaster").asSymbol).getKeysValues[num2-1][0];
					aZ.setMixMod(\master, 1, keyValues, arr1);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modpan: trackType, trackNum, panNum, extraArgs");

		cW.add(\modpan, [\str, \num, \num, \arr], {|str1, num1, num2, arr1|
			var trackArr, thisArr, keyValues;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					keyValues = Ndef(("space" ++ thisArr[0].capitalise ++
						thisArr[1]).asSymbol).getKeysValues[num2-1][0];
					aZ.setMixMod(thisArr[0].asSymbol, thisArr[1], keyValues, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modpan: mixTrackNum, panNum, extraArgs");

		//unmod space
		cW.add(\unmodpanx, [\str, \str, \num], {|str1, str2, num1|
			var keyValues;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					keyValues = Ndef(("spaceTrack" ++ 1).asSymbol).getKeysValues[num1-1][0];
					aZ.unmodMix(\track, keyValues, \pan).radpostwarn;}
				{str2 == 'b'} {
					keyValues = Ndef(("spaceBus" ++ 1).asSymbol).getKeysValues[num1-1][0];
					aZ.unmodMix(\bus, 1, keyValues).radpostwarn;}
				{str2 == 'm'} {
					keyValues = Ndef(("spaceMaster").asSymbol).getKeysValues[num1-1][0];
					aZ.unmodMix(\master, 1, keyValues).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodtrim: trackType");

		cW.add(\unmodpan, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var keyValues;
			if(aZ.notNil, {
				case
				{str2 == 't'} {
					keyValues = Ndef(("spaceTrack" ++ num1).asSymbol).getKeysValues[num2-1][0];
					aZ.unmodMix(\track, num1, keyValues).radpostwarn;}
				{str2 == 'b'} {
					keyValues = Ndef(("spaceBus" ++ num1).asSymbol).getKeysValues[num2-1][0];
					aZ.unmodMix(\bus, num1, keyValues).radpostwarn;}
				{str2 == 'm'} {
					keyValues = Ndef(("spaceMaster").asSymbol).getKeysValues[num2-1][0];
					aZ.unmodMix(\master, num1, keyValues).radpostwarn;};
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodpan: trackType, trackNum");

		cW.add(\unmodpan, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr, keyValues;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					keyValues = Ndef(("space" ++ thisArr[0].capitalise ++
						thisArr[1]).asSymbol).getKeysValues[num2-1][0];
					aZ.unmodMix(thisArr[0].asSymbol, thisArr[1], keyValues).radpostwarn;
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "unmodpan: mixTrackNum");

	}

	*loadBlkCmds {
		//blocks
		cW.add(\blk, [\str, \str], {|str1, str2|
			case
			{str2 == 'add'} {Block.add(1);}
			{str2 == 'clock'} {Block.clock;}
			{str2 == 'play'} {Block.playNdefs;}
			{str2 == 'ply'} {Block.playNdefs;}
			{str2 == 'bpm'} {Block.bpm;}
			{str2 == 'sstart'} {Block.syncStart;}
			{str2 == 'ndef'} {Block.ndef;}
			{str2 == 'blocks'} {Block.blocks;}
			{str2 == 'clear'} {Block.clear;}
			;
		}, "block: ['add', 'clock']");

		cW.add(\blk, [\str, \str, \num], {|str1, str2, num|
			case
			{str2 == 'add'} {Block.add(num);}
			{str2 == 'addn'} {Block.addNum(num);}
			{str2 == 'remove'} {Block.remove(num);}
			{str2 == 'stop'} {Block.stop(num);}
			{str2 == 'bpm'} {Block.tempo(num);}
			{str2 == 'tempo'} {Block.tempo(num);}
			{str2 == 'sdiv'} {Block.schedDiv = num;}
			;
		}, "block: ['add', 'addn', 'remove', 'stop'], [channels, number, block, block]");

		cW.add(\blk, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			case
			{str2 == 'addn'} {Block.addNum(num1, num2);}
			{str2 == 'stop'} {Block.stop(num1, num2);}
			;
		}, "block: ['addn', 'stop'], [numer, block], [channels, fadeOut]");

		cW.add(\blk, [\str, \num, \str], {|str1, num, str2|
			Block.play(num, str2);
		}, "block: blockNum, blockName");

		cW.add(\blk, [\str, \num, \str, \str], {|str1, num, str2, str3|
			Block.play(num, str2, str3);
		}, "block: blockNum, blockName, buffer");

		cW.add(\blk, [\str, \num, \str, \arr], {|str1, num, str2, arr|
			Block.play(num, str2, extraArgs: arr);
		}, "block: blockNum, blockName, extraArgs");

		cW.add(\blk, [\str, \num, \str, \str, \arr], {|str1, num, str2, str3, arr|
			Block.play(num, str2, str3, arr);
		}, "block: blockNum, blockName, buffer, extraArgs");

		cW.add(\blk, [\str, \num, \str, \str, \str], {|str1, num, str2, str3, str4|
			Block.play(num, str2, str3, str4);
		}, "block: blockNum, blockName, extraArgs");
		//blk ply 1 pattern nobuf [ [\note, \seq, [10,6,2,2]], [\dur, 0.5], [\instrument, \perkysine] ]
		//rounting blocks to assemblage
		cW.add(\blk, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			if(str2 == '<>', {
				if(aZ.notNil, {
					aZ.input(Block.ndefs[num1-1], \track, num2);
					aZ.mixWinBool({aZ.refreshMixGUI;});
				}, {
					"could not find assemblage".warn;
				});
			});
		});
		cW.add(\blk, [\str, \dash, \str, \num], {|str1, arr, str2, num2|
			var ndefArr;
			if(str2 == '<>', {
				if(aZ.notNil, {
					ndefArr = arr.collect{|item| Block.ndefs[item-1] };
					aZ.input(ndefArr, \track, num2);
					aZ.mixWinBool({aZ.refreshMixGUI;});
				}, {
					"could not find assemblage".warn;
				});
			});
		});
		cW.add(\blk, [\str, \dash, \str, \dash], {|str1, arr1, str2, arr2|
			var ndefArr;
			if(str2 == '<>', {
				if(aZ.notNil, {
					arr1.do{|item, index|
						aZ.input(Block.ndefs[item-1], \track, arr2[index]);
					};
					aZ.mixWinBool({aZ.refreshMixGUI;});
				}, {
					"could not find assemblage".warn;
				});
			});
		});

		cW.add(\blkset, [\str, \num], {|str1, num1|
			var thisIndex, thisKey;
			thisIndex = Block.ndefs.indexOf(Ndef(("block" ++ num1).asSymbol));
			thisKey = Block.ndefs[thisIndex].getKeysValues.radpostwarn;
		}, "blocksetn: blk, arg");

		cW.add(\blkset, [\str, \num, \num], {|str1, num1, num2|
			var thisIndex, thisKey;
			thisIndex = Block.ndefs.indexOf(Ndef(("block" ++ num1).asSymbol));
			thisKey = Block.ndefs[thisIndex].getKeysValues[num2-1][1].radpostwarn;
		}, "blocksetn: blk, arg");

		cW.add(\blkset, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var thisIndex, thisKey;
			thisIndex = Block.ndefs.indexOf(Ndef(("block" ++ num1).asSymbol));
			thisKey = Block.ndefs[thisIndex].controlKeys[num2-1];
			Block.set(num1, [thisKey, num3]);
		}, "blocksetn: blk, arg");

		cW.add(\blkset, [\str, \num, \str, \num], {|str1, num1, str2, num3|
			Block.set(num1, [str2, num3]);
		}, "blocksetn: blk, arg");

		cW.add(\blkset, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var thisIndex, thisKey, modArgs, modifier;
			thisIndex = Block.ndefs.indexOf(Ndef(("block" ++ num1).asSymbol));
			thisKey = Block.ndefs[thisIndex].controlKeys[num2-1];
			modArgs = str2.asString.radStringMod;
			if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
			Block.modBlk(num1, thisKey, modArgs[0], modArgs[1], modifier: modifier);
		}, "blocksetn: blk, arg");

		cW.add(\blkset, [\str, \num, \str, \str], {|str1, num1, str2, str3|
			var modArgs, modifier;
			modArgs = str3.asString.radStringMod;
			if(HIDMap.getHIDType(modArgs[0]).notNil, {modifier = \hid}, {modifier = \mod});
			Block.modBlk(num1, str2, modArgs[0], modArgs[1], modifier: modifier);
		}, "blocksetn: blk, arg");

		cW.add(\blklag, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var thisIndex, thisKey;
			thisIndex = Block.ndefs.indexOf(Ndef(("block" ++ num1).asSymbol));
			thisKey = Block.ndefs[thisIndex].controlKeys[num2-1];
			Block.lag(num1, [thisKey, num3]);
		}, "blocksetn: blk, arg");

		cW.add(\blklag, [\str, \num, \str, \num], {|str1, num1, str2, num3|
			Block.lag(num1, [str2, num3]);
		}, "blocklag: blk, arg");

		cW.add(\blkxset, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var thisIndex, thisKey;
			thisIndex = Block.ndefs.indexOf(Ndef(("block" ++ num1).asSymbol));
			thisKey = Block.ndefs[thisIndex].controlKeys[num2-1];
			Block.xset(num1, [thisKey, num3]);
		}, "blocksetn: blk, arg");

		cW.add(\blkxset, [\str, \num, \str, \num], {|str1, num1, str2, num3|
			var thisIndex;
			thisIndex = Block.ndefs.indexOf(Ndef(("block" ++ num1).asSymbol));
			Block.xset(num1, [str2, num3]);
		}, "blocksetn: blk, arg");

		cW.add(\blksetn, [\str, \num, \arr], {|str1, num1, arr1|
			Block.set(num1, arr1);
		}, "blocksetn: blk, arg");

		cW.add(\blkxsetn, [\str, \num, \arr], {|str1, num1, arr1|
			Block.xset(num1, arr1);
		}, "blockxsetn: blk, arg");

		cW.add(\blklagn, [\str, \num, \arr], {|str1, num1, arr1|
			Block.lag(num1, arr1);
		}, "blocksetn: blk, val");

		cW.add(\blkget, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			Block.set(num1, [str2, num2]);
		}, "blocksetn: blk, arg");

		cW.add(\modblkset, [\str, \num, \num], {|str1, num1, num2|
			Block.getBlkMod(num1, num2);
		}, "modblockget: blk, arg");

		cW.add(\modblkset, [\str, \num, \str], {|str1, num1, str2|
			Block.getBlkMod(num1, str2);
		}, "modblockget: blk, arg");

		cW.add(\modblkset, [\str, \num, \num, \arr], {|str1, num1, num2, arr1|
			Block.setBlkMod(num1, num2, arr1);
		}, "modblockset: blk, arg");

		cW.add(\modblkset, [\str, \num, \str, \arr], {|str1, num1, str2, arr1|
			Block.setBlkMod(num1, str2, arr1);
		}, "mod block set: blk, arg");

		cW.add(\blkgetspec, [\str, \num, \num], {|str1, num1, num2|
			var thisIndex, thisKey;
			thisIndex = Block.ndefs.indexOf(Ndef(("block" ++ num1).asSymbol));
			thisKey = Block.ndefs[thisIndex].controlKeys[num2-1];
			Block.getSpec(num1, thisKey).radpostwarn;
		}, "blocksetn: blk, arg");

		cW.add(\blkgetspec, [\str, \num, \str], {|str1, num1, str2|
			Block.getSpec(num1, str2).radpostwarn;
		}, "blocksetn: blk, arg");

		cW.add(\unmodblk, [\str, \num, \num], {|str1, num1, num2|
			Block.unmodBlk(num1, num2);
		}, "unmodblk: blkNum, arg");

		cW.add(\unmodblk, [\str, \num, \str], {|str1, num1, str2|
			Block.unmodBlk(num1, str2);
		}, "unmodblk: blkNum, arg");

		cW.add(\unmodblk, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			Block.unmodBlk(num1, num2, num3);
		}, "unmodblk: blkNum, arg");

		cW.add(\unmodblk, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			Block.unmodBlk(num1, str2, num2);
		}, "unmodblk: blkNum, arg");

		//blk shortcuts
		cW.add(\pl, [\str, \num, \str], {|str1, num1, str2|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \play, str2);
			}, {
				Block.play(num1, \play, Block.recBuf(string.divNumStr[1]););
			});
		}, "pl: block, buffer");
		cW.add(\pl2, [\str, \num, \str], {|str1, num1, str2|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \play2, str2);
			}, {
				Block.play(num1, \play2, Block.recBuf(string.divNumStr[1]););
			});
		}, "pl2: block, buffer");
		cW.add(\lp, [\str, \num, \str], {|str1, num1, str2|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \loop, str2);
			}, {
				Block.play(num1, \loop, Block.recBuf(string.divNumStr[1]););
			});
		}, "lp: block, buffer");
		cW.add(\lp2, [\str, \num, \str], {|str1, num1, str2|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \loop2, str2);
			}, {
				Block.play(num1, \loop2, Block.recBuf(string.divNumStr[1]););
			});
		}, "lp2: block, buffer");
		cW.add(\plpv, [\str, \num, \str], {|str1, num1, str2|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \playpv, str2);
			}, {
				Block.play(num1, \playpv, Block.recBuf(string.divNumStr[1]););
			});
		}, "plpv: block, buffer");
		cW.add(\lppv, [\str, \num, \str], {|str1, num1, str2|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \looppv, str2);
			}, {
				Block.play(num1, \looppv, Block.recBuf(string.divNumStr[1]););
			});
		}, "lppv: block, buffer");

		cW.add(\pl, [\str,  \num, \str, \arr], {|str1, num1, str2, arr1|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \play, str2, arr1);
			}, {
				Block.play(num1, \play, Block.recBuf(string.divNumStr[1]), arr1);
			});
		}, "pl: block, buffer, extraArgs]");
		cW.add(\lp, [\str,  \num, \str, \arr], {|str1, num1, str2, arr1|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \loop, str2, arr1);
			}, {
				Block.play(num1, \loop, Block.recBuf(string.divNumStr[1]), arr1);
			});
		}, "lp: block, buffer, extraArgs]");

		cW.add(\plpv, [\str,  \num, \str, \arr], {|str1, num1, str2, arr1|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \playpv, str2, arr1);
			}, {
				Block.play(num1, \playpv, Block.recBuf(string.divNumStr[1]), arr1);
			});
		}, "plpv: block, buffer, extraArgs]");
		cW.add(\lppv, [\str,  \num, \str, \arr], {|str1, num1, str2, arr1|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \looppv, str2, arr1);
			}, {
				Block.play(num1, \looppv, Block.recBuf(string.divNumStr[1]), arr1);
			});
		}, "lppv: block, buffer, extraArgs]");

		cW.add(\pl, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \play, str2, [\rate, num2]);
			}, {
				Block.play(num1, \play, Block.recBuf(string.divNumStr[1]), [\rate, num2]);
			});
		}, "pl: block, buffer, rate");
		cW.add(\lp, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \loop, str2, [\rate, num2]);
			}, {
				Block.play(num1, \loop, Block.recBuf(string.divNumStr[1]), [\rate, num2]);
			});
		}, "lp: block, buffer, rate");

		cW.add(\plpv, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \playpv, str2, [\rate, num2]);
			}, {
				Block.play(num1, \playpv, Block.recBuf(string.divNumStr[1]), [\rate, num2]);
			});
		}, "plpv: block, buffer, rate");
		cW.add(\lppv, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			var string;
			string = str2.asString;
			if(string[0].ascii != 64, {
				Block.play(num1, \looppv, str2, [\rate, num2]);
			}, {
				Block.play(num1, \looppv, Block.recBuf(string.divNumStr[1]), [\rate, num2]);
			});
		}, "lppv: block, buffer, rate");

		cW.add(\in, [\str, \num, \str, \str, \num], {|str1, num1, str2, str3, num2|
			if((str2 == '<>').and(str3 == 'blk'), {
				Block.play(num2, \audioin, \nobuf, [\bus, num1]);
			});
		}, "audioin: bus, block");
		cW.add(\in, [\str, \num, \num], {|str1, num1, num2|
			Block.play(num2, \audioin, \nobuf, [\bus, num1]);
		}, "audioin: bus, block");
		cW.add(\in2, [\str, \num, \num], {|str1, num1, num2|
			Block.play(num2, \audioin2, \nobuf, [\bus, num1]);
		}, "audioin: bus, block");
		cW.add(\in2, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			Block.play(num2, \audioin2, \nobuf, [\bus, num1, \pan, num3]);
		}, "audioin: bus, block");
		cW.add(\blkn, [\str, \num], {|str1, num1|
			Block.addNum(num1);
		}, "blkn: blockNum");
		cW.add(\blkn, [\str, \num, \num], {|str1, num1, num2|
			Block.addNum(num1, num2);
		}, "blkn: blockNum, chans");
		cW.add(\st, [\str, \num], {|str1, num1|
			Block.stop(num1);
		}, "stop: blockNum");
		cW.add(\st, [\str, \dash], {|str1, dash|
			dash.do{|item|
				Block.stop(item);
			};
		}, "stop: blockNum");
		cW.add(\st, [\str, \arr], {|str1, arr1|
			arr1.do{|item|
				Block.stop(item);
			};
		}, "stop: blockNum");
		cW.add(\st, [\str, \num, \num], {|str1, num1, num2|
			Block.stop(num1, num2);
		}, "stop: blockNum");
		cW.add(\st, [\str, \dash, \num], {|str1, dash, num1|
			dash.do{|item|
				Block.stop(item, num1);
			};
		}, "stop: blockNum");
		cW.add(\st, [\str, \arr, \num], {|str1, arr1, num1|
			arr1.do{|item|
				Block.stop(item, num1);
			};
		}, "stop: blockNum");

		cW.add(\pn, [\str, \num, \str], {|str1, num, str2|
			Block.play(num, \pattern, \nobuf, str2);
		}, "pn: blockNum, patternPreset");

		cW.add(\pn, [\str, \num, \arr], {|str1, num, arr|
			Block.play(num, \pattern, \nobuf, arr);
		}, "pn: blockNum, patternArr");

		cW.add(\metro, [\str, \num], {|str, num|
			Block.play(num, \pattern, \nobuf, [[\instrument, \metro]]);
		});

		cW.add('@', [\str], {|str1|
			Block.recBufInfo.dopostln;
		}, "print all recbufs");

		cW.add('@', [\str, \num], {|str1, num1|
			var bpm;
			if(Ndef('metronome').clock.notNil, {
				bpm = Ndef('metronome').clock.tempo;
			}, {bpm = 1});
			Block.addRec(num1/bpm);
		}, "record buffer: seconds");

		cW.add('@', [\str, \num, \num], {|str1, num1, num2|
			var bpm;
			if(Ndef('metronome').clock.notNil, {
				bpm = Ndef('metronome').clock.tempo;
			}, {bpm = 1});
			Block.addRec(num1/bpm, num2);
		}, "record buffer: seconds, channels");

		cW.add('@', [\str, \num, \num, \str2], {|str1, num1, num2, str2|
			var bpm;
			if(Ndef('metronome').clock.notNil, {
				bpm = Ndef('metronome').clock.tempo;
			}, {bpm = 1});
			Block.addRec(num1/bpm, num2, str2);
		}, "record buffer: seconds, channels, format");

		cW.add('@pv', [\str, \num], {|str1, num1|
			var bpm;
			if(Ndef('metronome').clock.notNil, {
				bpm = Ndef('metronome').clock.tempo;
			}, {bpm = 1});
			Block.addRec(num1/bpm, format: 'pv');
		}, "record buffer: seconds");

		cW.add('@pv', [\str, \num, \num], {|str1, num1, num2|
			var bpm;
			if(Ndef('metronome').clock.notNil, {
				bpm = Ndef('metronome').clock.tempo;
			}, {bpm = 1});
			Block.addRec(num1/bpm, num2, 'pv');
		}, "record buffer: seconds, channels");

		cW.add('@n', [\str, \num, \num], {|str1, num1, num2|
			var bpm;
			if(Ndef('metronome').clock.notNil, {
				bpm = Ndef('metronome').clock.tempo;
			}, {bpm = 1});
			Block.addRecNum(num1, num2/bpm);
		}, "record buffers: number, seconds");

		cW.add('@n', [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var bpm;
			if(Ndef('metronome').clock.notNil, {
				bpm = Ndef('metronome').clock.tempo;
			}, {bpm = 1});
			Block.addRecNum(num1, num2/bpm, num3);
		}, "record buffers: number, seconds");

		cW.add('@npv', [\str, \num, \num], {|str1, num1, num2|
			var bpm;
			if(Ndef('metronome').clock.notNil, {
				bpm = Ndef('metronome').clock.tempo;
			}, {bpm = 1});
			Block.addRecNum(num1, num2/bpm, format: 'pv');
		}, "record buffers: number, seconds");

		cW.add('@npv', [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var bpm;
			if(Ndef('metronome').clock.notNil, {
				bpm = Ndef('metronome').clock.tempo;
			}, {bpm = 1});
			Block.addRecNum(num1, num2/bpm, num3, 'pv');
		}, "record buffers: number, seconds");

		cW.add('sr', [\str, \str], {|str1, str2|
			Block.rec(str2.asString.divNumStr[1]);
		}, "start recording: rec buffer");

		cW.add('sr', [\str, \str, \num], {|str1, str2, num1|
			Block.rec(str2.asString.divNumStr[1], num1);
		}, "start recording: rec buffer, input");

		cW.add('sr', [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			Block.rec(str2.asString.divNumStr[1], num1, 0, num2, num3);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('sr', [\str, \str, \str], {|str1, str2, str3|
			Block.rec(str2.asString.divNumStr[1], Ndef(str3));
		}, "start recording: rec buffer, input");

		cW.add('sr', [\str, \str, \str, \num, \num], {|str1, str2, str3, num1, num2|
			Block.rec(str2.asString.divNumStr[1], Ndef(str3), 0, num1, num2);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('sr', [\str, \str, \str, \num], {|str1, str2, str3, num1|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol}
			;
			Block.rec(str2.asString.divNumStr[1], Ndef(ndef));
		}, "start recording: rec buffer, input");

		cW.add('sr', [\str, \str, \str, \num, \num, \num], {|str1, str2, str3, num1, num2, num3|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol};
			Block.rec(str2.asString.divNumStr[1], Ndef(ndef), 0, num2, num3);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('lr', [\str, \str], {|str1, str2|
			Block.rec(str2.asString.divNumStr[1], 1, 1);
		}, "loop recording: rec buffer, input");

		cW.add('lr', [\str, \str, \num], {|str1, str2, num1|
			Block.rec(str2.asString.divNumStr[1], num1, 1);
		}, "loop recording: rec buffer, input");

		cW.add('lr', [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			Block.rec(str2.asString.divNumStr[1], num1, 1, num2, num3);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('lr', [\str, \str, \str], {|str1, str2, str3|
			Block.rec(str2.asString.divNumStr[1], Ndef(str3), 1);
		}, "loop recording: rec buffer, input");

		cW.add('lr', [\str, \str, \str, \num, \num], {|str1, str2, str3, num1, num2|
			Block.rec(str2.asString.divNumStr[1], Ndef(str3), 1, num1, num2);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('lr', [\str, \str, \str, \num], {|str1, str2, str3, num1|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol}
			;
			Block.rec(str2.asString.divNumStr[1], Ndef(ndef), 1);
		}, "start recording: rec buffer, input");

		cW.add('lr', [\str, \str, \str, \num, \num, \num], {|str1, str2, str3, num1, num2, num3|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol};
			Block.rec(str2.asString.divNumStr[1], Ndef(ndef), 1, num2, num3);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('or', [\str, \str], {|str1, str2|
			Block.rec(str2.asString.divNumStr[1], 1, 0, 0.5, 0.5);
		}, "overdub recording: rec buffer, input");

		cW.add('ol', [\str, \str], {|str1, str2|
			Block.rec(str2.asString.divNumStr[1], 1, 1, 0.5, 0.5);
		}, "overdub loop recording: rec buffer, input");

		cW.add('or', [\str, \str, \num], {|str1, str2, num1|
			Block.rec(str2.asString.divNumStr[1], num1, 0, 0.5, 0.5);
		}, "overdub recording: rec buffer, input");

		cW.add('ol', [\str, \str, \num], {|str1, str2, num1|
			Block.rec(str2.asString.divNumStr[1], num1, 1, 0.5, 0.5);
		}, "overdub loop recording: rec buffer, input");

		cW.add('or', [\str, \str, \str], {|str1, str2, str3|
			Block.rec(str2.asString.divNumStr[1], Ndef(str3), 0, 0.5, 0.5);
		}, "overdub recording: rec buffer, input");

		cW.add('ol', [\str, \str, \str], {|str1, str2, str3|
			Block.rec(str2.asString.divNumStr[1], Ndef(str3), 1, 0.5, 0.5);
		}, "overdub loop recording: rec buffer, input");

		cW.add('or', [\str, \str, \str, \num], {|str1, str2, str3, num1|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol}
			;
			Block.rec(str2.asString.divNumStr[1], Ndef(ndef), 0, 0.5, 0.5);
		}, "start recording: rec buffer, input");

		cW.add('ol', [\str, \str, \str, \num], {|str1, str2, str3, num1|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol}
			;
			Block.rec(str2.asString.divNumStr[1], Ndef(ndef), 1, 0.5, 0.5);
		}, "start recording: rec buffer, input");
		//timed
		cW.add('tsr', [\str, \str], {|str1, str2|
			Block.recTimer(str2.asString.divNumStr[1]);
		}, "start recording: rec buffer");

		cW.add('tsr', [\str, \str, \num], {|str1, str2, num1|
			Block.recTimer(str2.asString.divNumStr[1], num1);
		}, "start recording: rec buffer, input");

		cW.add('tsr', [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			Block.recTimer(str2.asString.divNumStr[1], num1, 0, num2, num3);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('tsr', [\str, \str, \str], {|str1, str2, str3|
			Block.recTimer(str2.asString.divNumStr[1], Ndef(str3));
		}, "start recording: rec buffer, input");

		cW.add('tsr', [\str, \str, \str, \num, \num], {|str1, str2, str3, num1, num2|
			Block.recTimer(str2.asString.divNumStr[1], Ndef(str3), 0, num1, num2);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('tsr', [\str, \str, \str, \num], {|str1, str2, str3, num1|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol}
			;
			Block.recTimer(str2.asString.divNumStr[1], Ndef(ndef));
		}, "start recording: rec buffer, input");

		cW.add('tsr', [\str, \str, \str, \num, \num, \num], {|str1, str2, str3, num1, num2, num3|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol};
			Block.recTimer(str2.asString.divNumStr[1], Ndef(ndef), 0, num2, num3);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('tlr', [\str, \str], {|str1, str2|
			Block.recTimer(str2.asString.divNumStr[1], 1, 1);
		}, "loop recording: rec buffer, input");

		cW.add('tlr', [\str, \str, \num], {|str1, str2, num1|
			Block.recTimer(str2.asString.divNumStr[1], num1, 1);
		}, "loop recording: rec buffer, input");

		cW.add('tlr', [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			Block.recTimer(str2.asString.divNumStr[1], num1, 1, num2, num3);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('tlr', [\str, \str, \str], {|str1, str2, str3|
			Block.recTimer(str2.asString.divNumStr[1], Ndef(str3), 1);
		}, "loop recording: rec buffer, input");

		cW.add('tlr', [\str, \str, \str, \num, \num], {|str1, str2, str3, num1, num2|
			Block.recTimer(str2.asString.divNumStr[1], Ndef(str3), 1, num1, num2);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('tlr', [\str, \str, \str, \num], {|str1, str2, str3, num1|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol}
			;
			Block.recTimer(str2.asString.divNumStr[1], Ndef(ndef), 1);
		}, "start recording: rec buffer, input");

		cW.add('tlr', [\str, \str, \str, \num, \num, \num], {|str1, str2, str3, num1, num2, num3|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol};
			Block.recTimer(str2.asString.divNumStr[1], Ndef(ndef), 1, num2, num3);
		}, "start recording: rec buffer, input, recLevel, pre");

		cW.add('tor', [\str, \str], {|str1, str2|
			Block.recTimer(str2.asString.divNumStr[1], 1, 0, 0.5, 0.5);
		}, "overdub recording: rec buffer, input");

		cW.add('tol', [\str, \str], {|str1, str2|
			Block.recTimer(str2.asString.divNumStr[1], 1, 1, 0.5, 0.5);
		}, "overdub loop recording: rec buffer, input");

		cW.add('tor', [\str, \str, \num], {|str1, str2, num1|
			Block.recTimer(str2.asString.divNumStr[1], num1, 0, 0.5, 0.5);
		}, "overdub recording: rec buffer, input");

		cW.add('tol', [\str, \str, \num], {|str1, str2, num1|
			Block.recTimer(str2.asString.divNumStr[1], num1, 1, 0.5, 0.5);
		}, "overdub loop recording: rec buffer, input");

		cW.add('tor', [\str, \str, \str], {|str1, str2, str3|
			Block.recTimer(str2.asString.divNumStr[1], Ndef(str3), 0, 0.5, 0.5);
		}, "overdub recording: rec buffer, input");

		cW.add('tol', [\str, \str, \str], {|str1, str2, str3|
			Block.recTimer(str2.asString.divNumStr[1], Ndef(str3), 1, 0.5, 0.5);
		}, "overdub loop recording: rec buffer, input");

		cW.add('tor', [\str, \str, \str, \num], {|str1, str2, str3, num1|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol}
			;
			Block.recTimer(str2.asString.divNumStr[1], Ndef(ndef), 0, 0.5, 0.5);
		}, "start recording: rec buffer, input");

		cW.add('tol', [\str, \str, \str, \num], {|str1, str2, str3, num1|
			var ndef;
			case
			{str3 == 't'} {ndef = ("track" ++ num1).asSymbol}
			{str3 == 'b'} {ndef = ("bus" ++ num1).asSymbol}
			{str3 == 'm'} {ndef = ("master" ++ num1).asSymbol}
			;
			Block.recTimer(str2.asString.divNumStr[1], Ndef(ndef), 1, 0.5, 0.5);
		}, "start recording: rec buffer, input");

		cW.add('lt', [\str, \num], {|str1, num1|
			Block.loopTimer(num1);
		}, "loop timer: block num");

		//

		cW.add('wr', [\str, \str], {|str1, str2|
			var dash, timestamp, recPath;
			dash = "/";
			timestamp = Date.localtime.format("%d_%m_%Y_%H_%M_%S");
			Platform.case(
				\windows,   {dash = "\\"; }
			);
			recPath = Radicles.soundFilePath ++ dash ++  "Record" ++ dash;
			(BStore.buffStrByID(Block.recBuf(str2.asString.divNumStr[1])) ++ ".write(\"" ++ recPath ++ timestamp ++ ".wav\", \"wav\", \"int16\")").radpost.interpret;
		}, "write rec buffer to file: rec buffer, input");

		cW.add('@wr', [\str], {|str1|
			var dash, timestamp, recPath;
			dash = "/";
			timestamp = Date.localtime.format("%d_%m_%Y_%H_%M_%S");
			Platform.case(
				\windows,   {dash = "\\"; }
			);
			recPath = Radicles.soundFilePath ++ dash ++  "Record" ++ dash;
			Block.recbuffers.do{|item, index|
				(BStore.buffStrByID(item) ++ ".write(\"" ++ recPath ++ timestamp ++ index.cs ++
					".wav\", \"wav\", \"int16\")").radpost.interpret;
			};
		}, "write rec buffer to file: rec buffer, input");

		cW.add('fr', [\str, \str], {|str1, str2|
			var num, recBuf;
			num = str2.asString.divNumStr[1];
			recBuf = Block.recBuf(num);
			BStore.removeID(recBuf);
			Block.recbuffers.remove(recBuf);
			if(recBuf.notNil, {
				Block.recBufInfo.removeAt((num-1));
			}, {
				"Buffer doesn\'t exist".warn;
			});
		}, "free rec buffer to file: rec buffer, input");

		cW.add('clock', [\str], {|str1|
			Block.clock;
		}, "starts a clock");

		cW.add('bpm', [\str], {|str1|
			("bpm: " ++ Block.bpm).radpostwarn;
		}, "starts a clock");

		cW.add('bpm', [\str, \num], {|str1, num1|
			Block.tempo(num1);
		}, "set bpm");

		cW.add('beats', [\str], {|str1|
			Radicles.schedCount({|a| "stop".postln; Radicles.beatsFuncArr = [] }, 1, 1, false);
		}, "stop counting beats");

		cW.add('beats', [\str, \num], {|str1, num1|
			Radicles.schedCount({|a|
				a.postln;
				if(Radicles.beatsFuncArr.notNil, {
					Radicles.beatsFuncArr.do{|item| item.(a) };
				});
			}, 1, num1, true);
		}, "start counding beats: count");

	}

	*loadBaseCmds {

		cW.add(\modspec, [\str], {|str1, str2, str3, str4|
			SpecFile.read('modulation').postln;
		}, "modspec: posts modulation specfile");

		cW.add(\mods, [\str], {|str1, str2, str3, str4|
			ControlFile.read(\modulation).dopostln;
			Post << ControlFile.read(\modulation);
		}, "mods: posts mod types");

		cW.add(\mod, [\str, \str, \str, \str], {|str1, str2, str3, str4|
			var index, ndef;
			if(str2.asString.contains("hid"), {
				if(HIDMap.hidNodes.notNil, {
					index = HIDMap.hidNodes.flop[0].indexOfEqual(str2.cs);
					if(index.notNil, {
						ndef = HIDMap.hidNodes.flop[1][index];
						HIDMap.map(ndef, str3, str4);
					});
				});
			}, {
				ModMap.map(Ndef(str2), str3, str4);
			});
		}, "map: ndef, key, type");

		cW.add(\mod, [\str, \str, \str, \str, \str], {|str1, str2, str3, str4, str5|
			var index, ndef;
			if(str2.asString.contains("hid"), {
				if(HIDMap.hidNodes.notNil, {
					index = HIDMap.hidNodes.flop[0].indexOfEqual(str2.cs);
					if(index.notNil, {
						ndef = HIDMap.hidNodes.flop[1][index];
						HIDMap.map(ndef, str3, str4, str5);
					});
				});
			}, {
				ModMap.map(Ndef(str2), str3, str4, str5);
			});
		}, "modMap: ndef, key, type, spec");

		cW.add(\mod, [\str, \str, \str, \str, \arr], {|str1, str2, str3, str4, arr1|
			var index, ndef;
			if(str2.asString.contains("hid"), {
				if(HIDMap.hidNodes.notNil, {
					index = HIDMap.hidNodes.flop[0].indexOfEqual(str2.cs);
					if(index.notNil, {
						ndef = HIDMap.hidNodes.flop[1][index];
						HIDMap.map(ndef, str3, str4, arr1);
					});
				});
			}, {
				ModMap.map(Ndef(str2), str3, str4, arr1);
			});
		}, "modMap: ndef, key, type, spec");

		cW.add(\mod, [\str, \str, \str, \str, \arr, \arr], {|str1, str2, str3, str4, arr1, arr2|
			var index, ndef;
			if(str2.asString.contains("hid"), {
				if(HIDMap.hidNodes.notNil, {
					index = HIDMap.hidNodes.flop[0].indexOfEqual(str2.cs);
					if(index.notNil, {
						ndef = HIDMap.hidNodes.flop[1][index];
						HIDMap.map(ndef, str3, str4, arr1, arr2);
					});
				});
			}, {
				ModMap.map(Ndef(str2), str3, str4, arr1, arr2);
			});
		}, "modMap: ndef, key, type, spec");

		cW.add(\mod, [\str, \str, \str, \str, \str, \arr], {|str1, str2, str3, str4, str5, arr1|
			var index, ndef;
			if(str2.asString.contains("hid"), {
				if(HIDMap.hidNodes.notNil, {
					index = HIDMap.hidNodes.flop[0].indexOfEqual(str2.cs);
					if(index.notNil, {
						ndef = HIDMap.hidNodes.flop[1][index];
						HIDMap.map(ndef, str3, str4, str5, arr1);
					});
				});
			}, {
				ModMap.map(Ndef(str2), str3, str4, str5, arr1);
			});
		}, "modMap: ndef, key, type, spec");

		cW.add(\unmod, [\str, \str, \str], {|str1, str2, str3|
			var index, ndef;
			if(str2.asString.contains("hid"), {
				if(HIDMap.hidNodes.notNil, {
					index = HIDMap.hidNodes.flop[0].indexOfEqual(str2.cs);
					if(index.notNil, {
						ndef = HIDMap.hidNodes.flop[1][index];
						HIDMap.unmap(ndef, str3);
					});
				});
			}, {
				ModMap.unmap(Ndef(str2), str3);
			});
		}, "mod unmap: ndef, key, type");

		cW.add(\unmod, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			var ndef, index;
			if(str2.asString.contains("hid"), {
				if(HIDMap.hidNodes.notNil, {
					index = HIDMap.hidNodes.flop[0].indexOfEqual(str2.cs);
					if(index.notNil, {
						ndef = HIDMap.hidNodes.flop[1][index];
						HIDMap.unmap(ndef, str3, num1);
					});
				});
			}, {
				ModMap.unmap(Ndef(str2), str3, num1);
			});
		}, "mod unmap: ndef, key, type, value");

		cW.add(\unmod, [\str, \str, \num], {|str1, str2, num1|
			if(str2 == \hid, {
				HIDMap.unmapAt(num1-1);
			}, {
				ModMap.unmapAt(num1-1);
			});
		}, "unmap: type, index");

		cW.add(\unmod, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var nodes;
			if(str2 == \hid, {
				nodes = HIDMap.hidNodes[num1-1];
				ModMap.unmap(nodes[1], nodes[2], num2);
			}, {
				nodes = ModMap.modNodes[num1-1];
				ModMap.unmap(nodes[1], nodes[2], num2);
			});
		}, "unmap: type, index, val");

		cW.add(\getmods, [\str], {|str1|
			var nodes, modmap;
			modmap = ModMap.modNodes.collect({|item|
				[item[0].key.cs, item[1], item[2], item[3]] });
			nodes = [modmap, HIDMap.hidNodes];
			nodes = nodes.reject({|item| item.isNil });
			nodes.do{|item| item.do{|it| it.postln;} };
		}, "getmods: modNodes");

		//radicles
		cW.add(\rad, [\str, \str], {|str1, str2|
			case
			{str2 == 'doc'} {Radicles.document;}
			{str2 == 'fade'} {Radicles.fadeTime.postln};
		}, "radicles: ['doc', 'fade']");

		cW.add(\rad, [\str, \str, \num], {|str1, str2, num1|
			case
			{str2 == 'fade'} {Radicles.fadeTime = num1};
		}, "radicles: ['fade']");

		cW.add(\start, [\str, \num], {|str, num|
			if(aZ.isNil, {
				aZ = Assemblage(num, 1, action: {|number, channels|
					Block.addNum(number, channels, {|arr|
						arr.do{|item, index|
							aZ.input(item, \track, (index+1));
						};
					});
				});
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum");

		cW.add(\start, [\str, \num, \num], {|str, num1, num2|
			if(aZ.isNil, {
				aZ = Assemblage(num1, num2, action: {|number, channels|
					Block.addNum(number, channels, {|arr|
						arr.do{|item, index|
							aZ.input(item, \track, (index+1));
						};
					});
				});
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum, busNum");
		cW.add(\start, [\str, \num, \num, \num], {|str, num1, num2, num3|
			var array, spaceArr;
			if(aZ.isNil, {
				array = [1!num1, 1!num2, num3];
				spaceArr = Array.fill(array.size, num3.spaceType);
				spaceArr[spaceArr.size-1] = \dir;
				spaceArr.postln;
				aZ = Assemblage(num1, num2, array, spaceArr, action: {|number, channels|
					Block.addAll(channels[0], {|arr|
						arr.do{|item, index|
							aZ.input(item, \track, (index+1));
						};
					});
				});
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum, busNum, chanNum");
		cW.add(\start, [\str, \num, \num, \arr], {|str, num1, num2, arr1|
			if(aZ.isNil, {
				aZ = Assemblage(num1, num2, arr1, action: {|number, channels|
					[number, channels].postln;
					Block.addAll(channels[0]!number, {|arr|
						arr.do{|item, index|
							aZ.input(item, \track, (index+1));
						};
					});
				});
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum, busNum, chanNumArr");
		cW.add(\start, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var array, spaceArr;
			if(aZ.isNil, {
				array = [1!num1, 1!num2, str2.spaceToChans];
				spaceArr = Array.fill(array.size, str2);
				spaceArr[spaceArr.size-1] = \dir;
				spaceArr.postln;
				aZ = Assemblage(num1, num2, array, spaceArr, action: {|number, channels|
					//this line might be wrong... channels[0]!number :
					Block.addAll(channels[0], {|arr|
						arr.do{|item, index|
							aZ.input(item, \track, (index+1));
						};
					});
				});
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum, busNum, spaceType");
		//new
		cW.add(\start, [\str, \num, \num, \arr, \str], {|str1, num1, num2, arr1, str2|
			if(aZ.isNil, {
				aZ = Assemblage(num1, num2, arr1, str2, action: {|number, channels|
					[number, channels].postln;
					Block.addAll(channels[0]!number, {|arr|
						arr.postln;
						arr.do{|item, index|
							aZ.input(item, \track, (index+1));
						};
					});
				});
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum, busNum, chanNumArr, spaceType");

		cW.add(\start, [\str, \num, \num, \arr, \arr], {|str1, num1, num2, arr1, arr2|
			if(aZ.isNil, {
				aZ = Assemblage(num1, num2, arr1, arr2, action: {|number, channels|
					[number, channels].postln;
					Block.addAll(channels[0]!number, {|arr|
						arr.postln;
						arr.do{|item, index|
							aZ.input(item, \track, (index+1));
						};
					});
				});
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: trackNum, busNum, chanNumArr, spaceTypeArr");
		//

		cW.add(\net, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			("~net" ++ num1 ++ " = NetAddr(" ++ str2.asString.cs ++ ", " ++
				num2 ++ ");").radpost.interpret;
		}, "netaddrs: id, ip, port");

		cW.add(\osc, [\str], {|str1|
			traceFunc = {|msg, time, addr, recvPort|
				if(((msg[0] == '/status.reply').not).and(
					(msg[0] == '/AssemblageGUI').not), {
					msg.postln;
				});
			};
			thisProcess.addOSCRecvFunc(traceFunc);
		}, "prints osc messages");

		cW.add(\oscst, [\str], {|str1|
			thisProcess.removeOSCRecvFunc(traceFunc);
		}, "stop printing osc messages");

		cW.add(\color, [\str, \str], {|str1, str2|
			cW.background(Color.newName(str2););
		});

		cW.add(\colortext, [\str, \str], {|str1, str2|
			cW.fontColor(Color.newName(str2););
		});

		cW.add(\colors, [\str], {|str1|
			cW.colorList;
		});

		cW.add(\selcolor, [\str], {|str1|
			cW.background(Color.newName(cW.randColorList););
		});

		cW.add(\selcolortext, [\str], {|str1|
			cW.fontColor(Color.newName(cW.randColorList););
		});

		cW.add(\refresh, [\str], {|str1|
			{
				Quarks.uninstall("Radicles");
				Quarks.uninstall("RadiclesLibs");
				1.yield;
				Quarks.install("https://github.com/freuben/Radicles");
				Quarks.install("https://github.com/freuben/RadiclesLibs");
			}.fork;
		});

		cW.add(\update, [\str], {|str1|
			Quarks.update("Radicles");
			Quarks.update("RadiclesLibs");
		});

		cW.add(\recompile, [\str], {|str1|
			thisProcess.platform.recompile;
		});

		cW.add(\kill, [\str], {|str1|
			thisProcess.platform.recompile;
		});

		//base associations
		(0..9).do{|dim|
			cW.storeIndex = dim;
			cW.add(\mix, [\str], {
				if(aZ.notNil, {
					aZ.mixer;
				}, {
					"could not find assemblage".warn;
				});
			}, "mix");
			cW.add(\nomix, [\str], {
				if(aZ.notNil, {
					aZ.nomixer;
				}, {
					"could not find assemblage".warn;
				});
			}, "nomix");
			cW.add(\prec, [\str], {
				if(aZ.notNil, {
					aZ.prepareRecording;
				}, {
					"could not find assemblage".warn;
				});
			}, "prepare record");
			cW.add(\rec, [\str], {
				if(aZ.notNil, {
					aZ.startRecording;
				}, {
					"could not find assemblage".warn;
				});
			}, "start record");
			cW.add(\srec, [\str], {
				if(aZ.notNil, {
					aZ.stopRecording;
				}, {
					"could not find assemblage".warn;
				});
			}, "stop record");
			cW.add(\doc, [\str], {
				Radicles.document;
			}, "open SC document");

			cW.add(\fade, [\str], {|str1|
				Radicles.fadeTime.radpostwarn;
			}, "fade: fadeTime");

			cW.add(\fade, [\str, \num], {|str1, num1|
				Radicles.fadeTime = num1;
			}, "fade: fadeTime");

			cW.add(\imp, [\str, \str], {|str1, str2|
				if((str2 == 'All').or(str2 == 'all'), {
					Radicles.selLibs(Radicles.allLibs);
				}, {
					Radicles.selLibs([str2.asString]);
				});
			}, "imp: 'library");

			cW.add(\imp, [\str], {|str1|
				Radicles.libraries.radpostwarn;
			}, "imp: posts libraries");

			cW.add(\libs, [\str], {|str1|
				Radicles.libraries.radpostwarn;
			}, "libs: posts libraries");

			cW.add(\imp, [\str, \arr], {|str1, arr1|
				var strArr;
				strArr = arr1.collect({|item| item.asString });
				Radicles.selLibs(strArr);
			}, "imp: arr");
			cW.add(\blks, [\str], {|str1|
				var synthFile, arr;
				synthFile = SynthDefFile.read(\block, exclude: Radicles.excludeLibs);
				arr = synthFile.collect{|item|
					var desc;
					desc = DescriptionFile.read(\block, item, false, Radicles.excludeLibs);
					if(desc.isNil, {desc = "??"});
					[item, " -> ", desc];
				};
				arr = arr.sort({ arg a, b; a[0] <= b[0] });
				arr.do{|item| (item[0] ++ item[1] ++ item[2]).radpostwarn};
				arr.flop[0].radpostwarn;
			}, "blks: posts available blk synths");
			cW.add(\fxs, [\str], {|str1|
				var synthFile, arr;
				synthFile = SynthDefFile.read(\filter, exclude: Radicles.excludeLibs);
				arr = synthFile.collect{|item|
					var desc;
					desc = DescriptionFile.read(\filter, item, false, Radicles.excludeLibs);
					if(desc.isNil, {desc = "??"});
					[item, " -> ", desc];
				};
				arr = arr.sort({ arg a, b; a[0] <= b[0] });
				arr.do{|item| (item[0] ++ item[1] ++ item[2]).radpostwarn};
				arr.flop[0].radpostwarn;
			}, "fxs: posts available filters");
			cW.add(\pl, [\str], {|str1|
				var path;
				("Play Folder: " ++ BStore.playFolder).radpostwarn;
				"SoundFiles: ".postln;
				path = PathName(BStore.getPlayPath);
				path.files.collect({|item| item.fileNameWithoutExtension }).dopostln;
			}, "lists play soundfiles");
			cW.add(\pl, [\str, \num], {|str1, num1|
				var path;
				BStore.playFolder = num1;
				("Play Folder: " ++ BStore.playFolder).radpostwarn;
			}, "change play folder");
			cW.add(\ssave, [\str, \str], {|str1, str2|
				PresetFile.write(\session, str2, Radicles.cW.text.string);
			}, "save string in call window");
			cW.add(\sload, [\str], {|str|
				PresetFile.read(\session).sort.dopostln;
			}, "posts saved strings for call window");
			cW.add(\sload, [\str, \str], {|str1, str2|
				var string;
				string = PresetFile.read(\session, str2);
				Radicles.cW.text.string = "";
				Radicles.cW.text.string = string;
				Radicles.cW.text.select(string.size,string.size);
			}, "load string in call window");
			cW.add(\sconcat, [\str, \str], {|str1, str2|
				var string;
				string =  PresetFile.read(\session, str2);
				string = Radicles.cW.text.string ++ 10.asAscii ++ string;
				Radicles.cW.text.string = string;
				Radicles.cW.text.select(string.size,string.size);
			}, "concatenate string in call window");

			cW.add(\midi, [\str], {|str1|
				MIDIIn.connectAll;
			}, "start midiin");
			cW.add(\midipost, [\str], {|str1|
				MIDIFunc.trace(true);
			}, "post all incomming midi messages");
			cW.add(\midipostst, [\str], {|str1|
				MIDIFunc.trace(false);
			}, "stop posting all incomming midi messages");
			cW.add(\midicc, [\str], {|str1|
				MIDIdef.cc(\postcc, {arg ...args; args.postln});
			}, "post midi cc");
			cW.add(\midiccst, [\str], {|str1|
				MIDIdef.cc(\postcc).free;
			}, "stops posting midi cc");
			cW.add(\midion, [\str], {|str1|
				MIDIdef.noteOn(\poston, {arg ...args; args.postln});
			}, "post midi cc");
			cW.add(\midionst, [\str], {|str1|
				MIDIdef.noteOn(\poston).free;
			}, "stops posting midi cc");
			cW.add(\midioff, [\str], {|str1|
				MIDIdef.noteOff(\postoff, {arg ...args; args.postln});
			}, "post midi cc");
			cW.add(\midioffst, [\str], {|str1|
				MIDIdef.noteOff(\postoff).free;
			}, "stops posting midi cc");

			cW.add(\hidsave, [\str, \str], {|str1, str2|
				HIDMap.writePreset(str2)
			}, "save hid mappings");
			cW.add(\hidload, [\str, \str], {|str1, str2|
				HIDMap.loadPreset(str2, true);
			}, "load hid mappings");
			cW.add(\hidload, [\str], {|str1|
				PresetFile.read(\dstore).sort.dopostln;
			}, "post hid mappings");

			cW.add(\save, [\str, \str], {|str1, str2|
				Radicles.savePreset(str2);
			}, "save radicles project");
			cW.add(\load, [\str], {|str1|
				PresetFile.read(\radicles).sort.dopostln;
			}, "post saved projects");
			cW.add(\load, [\str, \str], {|str1, str2|
				Radicles.loadPreset(str2);
			}, "load saved project");
			cW.add(\load, [\str, \str, \str], {|str1, str2, str3|
				if(str3 == \run, {
					Radicles.loadPreset(str2, true);
				}, {
					Radicles.loadPreset(str2, false);
				});
			}, "load saved project, and run blocks");
			cW.add(\run, [\str], {|str1|
				Radicles.runLiveBlocks;
			}, "run saved blocks");

			cW.add(\memory, [\str], {|str1|
				Radicles.memorySize.radpostwarn;
			}, "posts memory size");
			cW.add(\memory, [\str, \num], {|str1, num1|
				Radicles.memorySize = num1;
			}, "change memory size");

			cW.add(\outputs, [\str], {|str1|
				Radicles.numOutputs.radpostwarn;
			}, "posts amount of outputs");

			cW.add(\outputs, [\str, \num], {|str1, num1|
				Radicles.options(num1);
			}, "change amount of outputs");

			cW.add(\rymerout, [\str], {|str1|
				Radicles.aZ.mastOutArr = [ [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ], [ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ], [ 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ], [ 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ], [ 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ], [ 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ], [ 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ], [ 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ], [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 ], [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 ], [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0 ], [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 ], [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0 ], [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 ], [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 ], [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 ] ];
				Radicles.aZ.mapOutFunc;
			}, "rymer output settings");

		};
		cW.storeIndex = 0;
	}

	*oscForCode {arg ip="127.0.0.1", port=57120;
		oscCode = NetAddr(ip, port);
	}

	*raveFunc {var getModelPath, getModels, postModels, modelArr, modelLoad;
		getModelPath = {arg type=\rave;
			var modelPath;
			case
			{type == \rave} {modelPath = filesPath ++ "/Models/RAVE";}
			{type == \prior} {modelPath = filesPath ++ "/Models/MSPrior";};
			if(modelPath.notNil, {
				modelPath;
			}, {
				"model type not found".postln;
				nil
			});
		};

		getModels = {arg type=\rave;
			var modelPath;
			modelPath = getModelPath.(type);
			if(modelPath.notNil,{
				PathName(modelPath).files.collect{|item| item.fileNameWithoutExtension };
			});
		};

		postModels = {arg type = \rave; getModels.(type).do{|item, index| [index, item].radpost;} };

		modelArr = nil!10;

		modelLoad = {arg type=\rave, num=0, tsfile=0, action;
			var key, fileName, indexFile, modelNames;
			if((num.isPositive).and(num.isInteger), {
				key = (type.asString ++ num.asString).asSymbol;
				modelNames = getModels.(type);
				if(modelNames.notNil, {
					if(tsfile.isString, {
						indexFile = modelNames.indexOfEqual(tsfile);
						if(indexFile.notNil, {
							fileName = modelNames[indexFile];
						}, {
							"Model not found : wrong file name".radpost;
						});
					}, {
						if(tsfile < (modelNames.size), {
							fileName = modelNames[tsfile];
						}, {
							"Model not found : wrong file index".radpost;
						});
					});
					if(fileName.notNil, {
						{
							"/\/\Loading model...".radpost;
							1.yield;
							"/\/\Model loaded:".radpost;
							("/\/\Model Type: " ++ type.asString.capitalise).radpost;
							("/\/\Model Name: " ++ fileName).radpost;
							("/\/\Model Key: " ++ key).radpost;
						}.fork;
						modelArr[num] = [key, fileName];
						if(action.notNil, {
						NN.load(key, (getModelPath.(type) ++ "/" ++ fileName ++ ".ts"),
								action: action);
						}, {
						NN.load(key, (getModelPath.(type) ++ "/" ++ fileName ++ ".ts"),
								action: _.describe);
						});
						modelNum = num; //changes automatically the key number
					});
				}, {
					"model type not found".radpost;
				});
			}, {
				"model number is wrong - try numbers between 0 and 9".radpost;
			});
		};

		cW.addAll([
			[\rave, [\str], { postModels.(\rave); }, "post rave models"],
			[\prior, [\str], { postModels.(\prior); }, "post prior models"],
			[\models, [\str], { modelArr.do({|item, index|
				if(item.notNil, {([index+1] ++ item).radpost}); }); },
				"post loaded models"],
			[\modelNum, [\str], { ("/\/\Current model number: " ++ modelNum).radpost; },
				"change current model number"],
			[\modelNum, [\str, \num], {|str, num| modelNum = num; },
				"change current model number"],
			[\modelInfo, [\str], { NN.describeAll },
				"post info about all loaded models"],
			[\modelInfo, [\str, \str], {|str1, str2| NN(str2.asSymbol).describe },
				"post info about this model key"],
			[\rave, [\str, \num, \num], {|str, num1, num2| modelLoad.(\rave,num1,num2);},
				"load rave model with index"],
			[\rave, [\str, \num, \str], {|str1, num, str2| modelLoad.(\rave,num,str2.asString);},
				"load rave model with name"],
			[\prior, [\str, \num, \num], {|str, num1, num2| modelLoad.(\prior,num1,num2);},
				"load rave model with index"],
			[\prior, [\str, \num, \str], {|str1, num, str2| modelLoad.(\prior,num,str2.asString);},
				"load rave model with name"],
			[\raveins, [\str], {|str| cW.callFunc("(in 1 1 & in 2 2 & in 3 3 & in 4 4 & blk 5 audioins)" )},
				"rave inputs"],
			[\brave1, [\str, \num, \num], {|str, num1, num2|
				var raveNum;
				raveNum = num1-1;
				modelLoad.(\rave,raveNum,num2, action: {
					if(~panArr.isNil, {~panArr = 0!Radicles.aZ.busCount;});
					Radicles.aZ.setFx(\bus, num1, 3, \nntImprov,
						[\pan, ~panArr[raveNum]]);
				});
			},
			"load rave model and fx in bus number"],

			[\brave, [\str, \num, \num], {|str, num1, num2|
				var raveNum;
				raveNum = num1-1;
				modelLoad.(\rave,raveNum,num2, action: {
					Radicles.aZ.setFx(\bus, num1, 3, \nntImprov2);
				});
			},
			"load rave model fake stereo and fx in bus number"],

			[\brave1, [\str, \arr, \num], {|str, arr1, num2|
				var raveNum, cond1;
				cond1 = Condition.new(false);

				arr1.postln;
				{
				arr1.do{|item|
				raveNum = item-1;

					if(~panArr.isNil, {~panArr = 0!Radicles.aZ.busCount;});

					modelLoad.(\rave,raveNum,num2, action: {
							Radicles.aZ.setFx(\bus, item, 3, \nntImprov,
								[\pan, ~panArr[raveNum]], action:
							{
								cond1.test = true;
								cond1.signal;
						})
					}
					);

					cond1.test = false;
					cond1.wait;
				};
				}.fork;


			},
		"load array of rave models and fx in bus number"],

			[\brave, [\str, \arr, \num], {|str, arr1, num2|
				var raveNum, cond1;
				cond1 = Condition.new(false);

				arr1.postln;
				{
				arr1.do{|item|
				raveNum = item-1;

					modelLoad.(\rave,raveNum,num2, action: {
						Radicles.aZ.setFx(\bus, item, 3, \nntImprov2, action:
							{
								cond1.test = true;
								cond1.signal;
						})
					}
					);

					cond1.test = false;
					cond1.wait;
				};
				}.fork;


			},
		"load array of fake stereo rave models and fx in bus number"],

			[\braven, [\str, \num, \num], {|str, num1, num2|
				var raveNum;
				raveNum = num1-1;
				modelLoad.(\rave,raveNum,num2, action: {
					Radicles.aZ.setFx(\bus, num1, 3, \nnt);
					/*cW.callFunc( ("fx b " ++ num1 ++ 	" 3 nntImprov") );*/
				});
			},
			"load rave model and fx in bus number"],

			[\braven, [\str, \arr, \num], {|str, arr1, num2|
				var raveNum, cond1;
				cond1 = Condition.new(false);

				arr1.postln;
				{
				arr1.do{|item|
				raveNum = item-1;

					modelLoad.(\rave,raveNum,num2, action: {
						Radicles.aZ.setFx(\bus, item, 3, \nnt, action:
							{
								cond1.test = true;
								cond1.signal;
						})
					}
					);

					cond1.test = false;
					cond1.wait;
				};
				}.fork;


			},
		"load array of rave models and fx in bus number"],

		]);




	}

	*defaultNntImprov {arg ndef; var defaultRAVEVals;
		defaultRAVEVals = "'lowCut', 20, 'hiCut', 20000, 'preVol', 6, 'cpthresh', -20, 'ratio', 1, 'cpatk', 0.01, 'cprel', 0.01, 'transp', 0, 'disper', 0, 'postVol', -6, 'amp', 1, 'dryDim', 1, 'wetDim', 0, 'mulSpace', 0, 'addSpace', 0, 'mul1 ', 0, 'mul2', 0, 'mul3', 0, 'mul4', 0, 'mul5', 0, 'mul6', 0, 'mul7', 0, 'mul8', 0, 'add1', 0, 'add2', 0, 'add3', 0, 'add4', 0, 'add5', 0, 'add6', 0, 'add7', 0, 'add8', 0, 'modWhich1', 0, 'modWhich2', 0, 'modWhich3', 0, 'modWhich4', 0, 'modWhich5', 0, 'modWhich6', 0, 'modWhich7', 0, 'modWhich8', 0, 'modFreq1', 23, 'modFreq2', 23, 'modFreq3', 23, 'modFreq4', 23, 'modFreq5', 23, 'modFreq6', 23, 'modFreq7', 23, 'modFreq8', 23";
		(ndef.cs ++ ".set(" ++  defaultRAVEVals ++ ");").interpret;
	}

}