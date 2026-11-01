// Radicles: call window commands: modulation, start, OSC, colours, system, and commands shared by all command sets.
// Part of the Radicles class: the class and its variables are in Radicles.sc.

+ Radicles {
	*loadBaseCmds {
		this.prModCmds;
		this.prStartCmds;
		this.prNetCmds;
		this.prColorCmds;
		this.prSystemCmds;
		this.prSharedCmds;
	}

	// modspec, mods, mod, unmod, getmods
	*prModCmds {
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
	}

	// rad, start
	*prStartCmds {
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
				//The panner depends on a strip's channels in and the output channels.
				//For a 2-channel output, let Assemblage choose per strip, as start N M does:
				//mono tracks and buses get pan2, the (stereo) master gets bal2.
				//For more output channels, mono strips use num3's panner (pan4, panAz8...)
				//and the master plays directly.
				if(num3 == 2, {
					spaceArr = nil;
				}, {
					spaceArr = Array.fill(array.size, num3.spaceType);
					spaceArr[spaceArr.size-1] = \dir;
				});
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
	}

	// net, osc, oscst
	*prNetCmds {
		cW.add(\net, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			("~net" ++ num1 ++ " = NetAddr(" ++ str2.asString.cs ++ ", " ++
				num2 ++ ");").radcode;
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
	}

	// color, colortext, colors, selcolor, selcolortext
	*prColorCmds {
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
	}

	// refresh, update, recompile, kill
	*prSystemCmds {
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
	}

	// commands added to all 10 command sets (mix, nomix and others); keep last
	*prSharedCmds {
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
			cW.add(\codelog, [\str], {|str|
				("Radicles code log is " ++ RadCodeLog.isOn.if({"ON: " ++ RadCodeLog.path}, {"off"})).postln;
			}, "codelog: is the research code log on?");
			cW.add(\codelog, [\str, \str], {|str1, str2|
				case
				{str2.asSymbol == \on} { Radicles.codeLog = true }
				{str2.asSymbol == \off} { Radicles.codeLog = false }
				{ "codelog: use codelog on or codelog off".warn };
			}, "codelog: on/off (research code log, saved in RadiclesFiles/Logs)");
			cW.add(\sload, [\str], {|str|
				PresetFile.read(\session).sort.dopostln;
			}, "posts saved strings for call window");
			cW.add(\sload, [\str, \str], {|str1, str2|
				var string;
				string = PresetFile.read(\session, str2);
				Radicles.cW.text.string = "";
				Radicles.setCallWindowText(string);
			}, "load string in call window");
			cW.add(\sconcat, [\str, \str], {|str1, str2|
				var string;
				string =  PresetFile.read(\session, str2);
				string = Radicles.cW.text.string ++ 10.asAscii ++ string;
				Radicles.setCallWindowText(string);
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
}
