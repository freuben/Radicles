Radicles {classvar <>mainPath, <>libPath, <>nodeTime=0.08, <>server, <>postWin=nil,
	<>postWhere=\ide, <>fadeTime=0.5, <>schedFunc, <>schedFuncAction, <>schedDiv=1,
	<bpm, <postDoc, <>lineSize=68, <>oscCode, <>myImprov, <>reducePostControl=false,
	<>ignorePost=false, <>ignorePostcont=false, <>postCapture, <>colorCritical, <>colorMeter, <>colorWarning,
	<>colorTrack, <>colorBus, <>colorMaster, <>colorTextField, <>cW, <aZ, <excludeLibs,
	<>filesPath, <>soundFilePath, <>postWindow, <>postWindowCont, <>oscCodeCont=true, <>beatsFuncArr, traceFunc, <>liveBlockArr, <>modelNum=0, <>codeTime=0, <>codeDev=0;
	/*	<>numOutputs = 16, <>numInputs = 16, <>numBuffers = 512, <>numBuses = 1024,
	<>numControlBuses=16384, <>memorySize=409600, <>sampleRate=41000, */

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

	/*	*options {arg outputs, inputs, buffers, buses, controlBuses,
	memory, samplingRate;
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
	samplingRate ?? {samplingRate = sampleRate};
	sampleRate = samplingRate;

	}*/

	*serveroptions {
		/*this.new;*/
		/*server.options.numOutputBusChannels = numOutputs;
		server.options.numInputBusChannels = numInputs;
		server.options.numWireBufs = numBuffers;
		server.options.numAudioBusChannels = numBuses;
		server.options.numControlBusChannels = numControlBuses;
		server.options.memSize = memorySize;
		server.options.sampleRate = sampleRate;*/
	}

	*clock {var clock, tclock;
		clock = "Ndef('metronome').proxyspace.makeTempoClock(1.0)";
		clock.radcode;
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
			metroCS.radcode;
		}, {
			("BPM = " ++ ((Ndef('metronome').clock.tempo*60).round(0.01)).cs).postln;
		});
	}

	*schedAction {arg func;
		schedFuncAction = { func.(); func=nil};
	}

	// Source tracking (Step 3). The source says what triggered the code that is
	// running: \typed (call window), \menu (a GUI PopUpMenu or list), \button
	// (a GUI button), \gui (sliders, knobs, text fields), \hid (MIDI and HID
	// controllers), \osc (incoming OSC), \hidcmd (a controller mapped to a
	// command with no value in it, a trigger or on/off: posts like the command
	// typed). nil means no source: code called directly from SuperCollider.
	// Routines started inside withSource keep the source; defer and sched do not.

	*source {
		^currentEnvironment[\radSource];
	}

	// Research code log (off by default): Radicles.codeLog = true / false.
	// See RadCodeLog for what it records and where.
	*codeLog_ {arg bool;
		if(bool, { RadCodeLog.on }, { RadCodeLog.off });
	}

	*codeLog {
		^RadCodeLog.isOn
	}

	*withSource {arg source, func;
		^RadSourceEnvir.run(source, func);
	}

	// Like {...}.defer, but the deferred code keeps the current source (defer
	// alone loses it: AppClock runs it outside the environment). `source`
	// replaces it when given.
	*defer {arg func, delta, source;
		source = source ?? { this.source };
		^{ this.withSource(source, func) }.defer(delta);
	}

	// A function that runs `func` with the given source: for GUI actions and
	// controller responders. A source already set wins, so a typed command that
	// moves a GUI widget (`mute t 1` presses the M button) stays typed.
	*srcFunc {arg source, func;
		^{|...args| RadSourceEnvir.run(this.source ? source, { func.valueArray(args) }) };
	}

	// Which posting channel the code goes to (Design Principle 1): typed
	// commands, menus and buttons post on radpost, continuous controls on
	// radpostcont.
	// With no source the method keeps its own channel (`default`).
	*channelFor {arg source, default=\radpost;
		^case
		{[\typed, \menu, \button, \hidcmd].includes(source)} {\radpost}
		{[\gui, \hid, \osc].includes(source)} {\radpostcont}
		{default};
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
			//mixerWin is nil until the mixer has been opened
			cmds = [Radicles.cW.text.string,
				Radicles.aZ.mixerWin.notNil and: { Radicles.aZ.mixerWin.visible == true }];
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
					item[1].cs.replace("[", "(").replace("]", ");") ).radcode;
				server.sync;
				//solo is not an Ndef value (it stays 1); it is restored from soloStates below
				if(item[1].includes(\mute), {
					aZ.muteStates[aZ.mixTrackNames.indexOf(item[0])] =
					item[1][(item[1].indexOfEqual(\mute)+1)];
				});
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
					aZ.restoreSoloStates(ioSettings[3]);
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
				if(mixView == true, {
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
					.radcode;
				});
			};
			liveBlockArr[0].do{|item|
				if(item.notNil, {
					item[0] = item[0].asString.divNumStr[1];
					if(item[2].notNil, {
						item[2] = item[2][2];
					});
					("Block.play" ++ item.cs.replaceAt("(", 0).replaceAt(")",
						item.cs.size-1) ).radcode;
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
		var str, str2, arr, ind, arr1, arr2, arr3, spec, cmdSource;
		// a command that takes the controller's value ($) is continuous control
		// (post window); a trigger or on/off command posts like the command typed
		cmdSource = if(string.contains("$"), {"'hid'"}, {"'hidcmd'"});
		str = string.copyRange(1, string.size-2);
		str = str.replace(" ,", ",").replace(", ", ",").replace("[ ", "[").replace(" ]", "]");
		arr = str.split($ );
		ind = arr.indexOfEqual("<>");
		arr1 = arr.copyFromStart(ind-1);
		arr2 = arr.copyToEnd(ind+1);
		arr2 = arr2.collect({|item, index| if((item == "$").and(index == (arr2.size-1)), {"#"}, {item});});
		// join the command words (printing the array and cutting "[ " and " ]"
		// broke in SC 3.14, which prints arrays without inner spaces)
		str = arr2.collect{|item| item.asString.replace(",") }.join(" ");
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
			("HIDMap.mapFunc({|val| Radicles.defer({~callWindowGlobVar.callFunc(" ++ str ++ ")}, source: " ++ cmdSource ++ ") }, " ++
				arr1[0].asSymbol.cs ++ ", " ++ spec ++ ", " ++ str2.cs ++ ");").interpret;
		}, {
			str = str.split($%);
			("HIDMap.mapFunc({|val| Radicles.defer({if(val != 0, {~callWindowGlobVar.callFunc("
				++ str[0].nospaceFunc ++ "\") }, {~callWindowGlobVar.callFunc(\""
				++ str[1].nospaceFunc ++ ") }); }, source: " ++ cmdSource ++ ") }, " ++ arr1[0].asSymbol.cs
				++ ", " ++ spec ++ ", " ++ str2.cs ++ ");").interpret;
		});
	}

	*oscForCode {arg ip="127.0.0.1", port=57120;
		oscCode = NetAddr(ip, port);
	}
}
