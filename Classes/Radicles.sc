Radicles {classvar <>mainPath, <>libPath, <>nodeTime=0.08, <server, <>postWin=nil,
	<>postWhere=\ide, <>fadeTime=0.5, <>schedFunc, <>schedDiv=1,
	<bpm, <postDoc, <>lineSize=68, <>logCodeTime=false, <>reducePostControl=false,
	<>ignorePost=false, <>ignorePostcont=false, <>colorCritical, <>colorMeter, <>colorWarning, <>colorTrack, <>colorBus, <>colorMaster, <>colorTextField, <>cW, <aZ, <excludeLibs,
	<>soundFilePath;

	*new {
		colorCritical = Color.new255(211, 14, 14);
		colorMeter = Color.new255(78, 109, 38);
		colorWarning = Color.new255(232, 90, 13);
		colorTrack = Color.new255(58, 162, 175);
		colorBus = Color.new255(132, 124, 10);
		colorMaster = Color.new255(102, 57, 130);
		colorTextField = Color.new255(246, 246, 246);
		^super.new.initRadicles;
	}

	initRadicles {arg doc=false;
		mainPath = (Platform.userExtensionDir ++ "/Radicles/");
		libPath = (Platform.userExtensionDir ++ "/RadiclesLibs");
		soundFilePath = (Platform.userExtensionDir ++ "/RadiclesSFs/");
		server = Server.default;
	}

	*document {
		postDoc = Document.new("Radicles: " ++ Date.getDate.asString);
	}

	*clock {var clock, tclock;
		clock = "Ndef('metronome').proxyspace.makeTempoClock(1.0)";
		clock.radpost;
		clock.interpret;
		tclock = Ndef('metronome').proxyspace.clock;
		tclock.schedAbs(tclock.beats.ceil, { arg beat, sec; schedFunc.(beat, sec); schedDiv });
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
		schedFunc = { func.(); func=nil};
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

	/**plugs {var result;
	this.new;
	result = PathName(libPath).folders.collect{|item| item.folderName };
	^result;
	}*/

	*allLibs {
		this.new;
		^(PathName.new(libPath).folders.collect({|item| item.folderName }) ++ ["Main"]);
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

	*callWindow {arg name;
		name ?? {name = "Call Window";};
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

		cW.add(\asm, [\str, \str], {|str1, str2|
			if(aZ.notNil, {
				case
				{str2 == 'mix'} {
					aZ.mixer;
				}
				{str2 == 'nomix'} {
					aZ.nomixer;
				}
				{str2 == 'names'} {
					aZ.mixTrackNames.radpostwarn;
				}
				{str2 == 'preprec'} {
					aZ.prepareRecording;
				}
				{str2 == 'startrec'} {
					aZ.startRecording;
				}
				{str2 == 'stoprec'} {
					aZ.stopRecording;
				}
				{str2 == 'fxs'} {
					if(aZ.filters.notNil, {
						aZ.filters.collect{|item| [aZ.convFilterTag(item[0]), item[1] ].flat;}.dopostln;
					}, {
						"no active filters".warn;
					});
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['mix', 'nomix', 'names', 'prerec', 'startrec', 'stoprec', 'fxs']");

		cW.add(\asm, [\str, \str, \str], {|str1, str2, str3|
			var getTrack;
			if(aZ.notNil, {
				case
				{str2 == 'add'} {
					aZ.autoAddTrack(str3);
				}
				{str2 == 'remove'} {
					getTrack = (aZ.mixTrackNames.select{|item|
						item.asString.contains(str3.asString)
					}.last).asString.divNumStr;
					aZ.removeTrack(getTrack[0].asSymbol, getTrack[1]);
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: trackNum, busNum, chanNumArr, spaceType");

		cW.add(\asm, [\str, \str, \arr], {|str1, str2, arr1|
			if(aZ.notNil, {
				case
				{str2 == 'fxsetn'} {aZ.setFxArgTracks(arr1)  }
				{str2 == 'fxlagn'} {aZ.lagFxArgTracks(arr1) }
				{str2 == 'dirinrec'} {aZ.setDirInRec(arr1-1) }
				{str2 == 'mapouts'} {aZ.mapOuts(arr1) }
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: trackNum, busNum, chanNumArr, spaceType");

		cW.add(\asm, [\str, \str, \num, \str], {|str1, str2, num, str3|
			var cond, getTrack;
			cond = Condition.new(false);
			if(aZ.notNil, {
				case
				{str2 == 'addn'} {
					{num.do{
						cond.test = false;
						aZ.autoAddTrack(str3, action: {cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
				}
				{str2 == 'removen'} {
					{num.do{
						cond.test = false;
						getTrack = (aZ.mixTrackNames.select{|item|
							item.asString.contains(str3.asString)
						}.last).asString.divNumStr;
						aZ.removeTrack(getTrack[0].asSymbol, getTrack[1],
							{cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: [addn]");

		cW.add(\asm, [\str, \str, \num], {|str1, str2, num1|
			var trackArr;
			if(aZ.notNil, {
				case
				{str2 == 'setfx'} {aZ.setFxArg(1, num1); }
				{str2 == 'inmenu'} {aZ.setTrackIn(num1, 0); }
				{str2 == 'getinmenu'} {aZ.getTrackInItem(num1); }
				{str2 == 'getoutmenu'} {
					trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr}[num1-1];
					aZ.getTrackOutItem(trackArr[0], trackArr[1]);
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['setfx']");

		cW.add(\asm, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			var getKey, controlKeys, ratesFor, trackArr;
			if(aZ.notNil, {
				case
				{str2 == 'setfx'} {
					Ndef(aZ.filters[num1-1][0]).getKeysValues[num2-1].radpostwarn;
				}
				{str2 == 'lagfx'} {
					getKey = aZ.filters[num1-1][0];
					controlKeys = Ndef(getKey).controlKeys;
					ratesFor = Ndef(getKey).nodeMap.ratesFor(
						controlKeys);
					if(ratesFor.notNil, {
						[controlKeys[num2-1],
							ratesFor[num2-1]].radpostwarn;
					});
				}
				{str2 == 'inmenu'} {aZ.setTrackIn(num1, num2-1); }
				{str2 == 'outmenu'} {
					trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr}[num1-1];
					aZ.setTrackOut(trackArr[0], trackArr[1], num2);
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['setfx', 'lagfx']");

		cW.add(\asm, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 'setfx'} {aZ.setFxArg(num1, num2, num3); }
				{str2 == 'lagfx'} {aZ.lagFxArg(num1, num2, num3); }
				{str2 == 'fxlagn'} {aZ.fxTrackLags(num1, num2, num3); }
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['setfx', 'lagfx', 'fxlagn']");

		cW.add(\asm, [\str, \str, \num, \arr], {|str1, str2, num1, arr1|
			if(aZ.notNil, {
				case
				{str2 == 'setfx'} {aZ.setFxArg(num1, arr1); }
				{str2 == 'lagfx'} {aZ.lagFxArg(num1, arr1); }
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['setfx', 'lagfx']");

		cW.add(\asm, [\str, \str, \arr, \str], {|str1, str2, arr1, str3|
			var cond;
			cond = Condition.new(false);
			case
			{str2 == 'removen'} {
				{arr1.do{|item|
					cond.test = false;
					aZ.removeTrack(str3, item,
						{cond.test = true; cond.signal});
					cond.wait;
				};}.fork;
			};
		}, "assemblage: [addn]");

		cW.add(\asm, [\str, \str, \num, \str, \num], {|str1, str2, num1, str3, num2|
			var cond;
			cond = Condition.new(false);
			if(aZ.notNil, {
				case
				{str2 == 'addn'} {
					{num1.do{
						cond.test = false;
						aZ.autoAddTrack(str3, num2, action: {cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
				}
				{str2 == 'setfx'} {
					aZ.setFxArg(num1, str3, num2);
				}
				{str2 == 'lagfx'} {
					aZ.lagFxArg(num1, str3, num2);
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: [addn]");
		//
		cW.add(\asm, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			if(aZ.notNil, {
				case
				{str2 == 'in'} {
					aZ.input(Ndef(str3), \track, num1);
				}
				{str2 == 'add'} {
					aZ.autoAddTrack(str3, num1);
				}
				{str2 == 'remove'} {
					aZ.removeTrack(str3, num1);
				}
				{str2 == 'fxclear'} {
					aZ.setFx(str3, num1, 1, remove: true);
				}
				{str2 == 'getoutmenu'} {
					aZ.getTrackOutItem(str3, num1);
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['in', 'add', 'remove', 'fxclear']");

		cW.add(\asm, [\str, \str, \str, \num, \num], {|str1, str2, str3, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 'vol'} {
					aZ.setVolume(str3, num1, num2);
				}
				{str2 == 'vollag'} {
					aZ.setVolumeLag(str3, num1, num2);
				}
				{str2 == 'pan'} {
					aZ.setPan(str3, num1, num2);
				}
				{str2 == 'panlag'} {
					aZ.setPanLag(str3, num1, num2);
				}
				{str2 == 'trim'} {
					aZ.setPan(str3, num1, num2);
				}
				{str2 == 'trimlag'} {
					aZ.setPanLag(str3, num1, num2);
				}
				{str2 == 'fxset'} {
					aZ.setFxArgTrack(str3, num1, num2);
				}
				{str2 == 'fxlag'} {
					aZ.lagFxArgTrack(str3, num1, num2);
				}
				{str2 == 'fxclear'} {
					aZ.setFx(str3, num1, num2, remove: true);
				}
				{str2 == 'mute'} {
					aZ.setMute(str3, num1, num2);
				}
				{str2 == 'rec'} {
					aZ.setRec(str3, num1, num2);
				}
				{str2 == 'solo'} {
					aZ.setSolo(str3, num1, num2);
				}
				{str2 == 'snd'} {
					aZ.setSend(str3, num1, 1, num2);
				}
				{str2 == 'sndry'} {
					aZ.setSend(str3, num1, 1, num2, dirMaster: false);
				}
				{str2 == 'setsnd'} {
					aZ.setSendKnob(str3, num1, 1, num2);
				}
				{str2 == 'outmenu'} {
					aZ.setTrackOut(str3, num1, num2);
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['vol', 'vollag', 'pan', 'panlag', 'trim', 'trimlag', 'fxset', 'fxlag', 'fxclear', 'mute', 'rec', 'solo', 'snd', 'sndry', 'setsnd']");

		cW.add(\asm, [\str, \str, \str, \num, \arr], {|str1, str2, str3, num1, arr|
			if(aZ.notNil, {
				case
				{str2 == 'fxset'} {
					aZ.setFxArgTrack(str3, num1, 1, arr);
				}
				{str2 == 'fxlag'} {
					aZ.lagFxArgTrack(str3, num1, 1, arr);
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['fxset', 'fxlag']");

		cW.add(\asm, [\str, \str, \str, \num, \num, \arr], {|str1, str2, str3, num1, num2, arr|
			if(aZ.notNil, {
				case
				{str2 == 'fxset'} {
					aZ.setFxArgTrack(str3, num1, num2, arr);
				}
				{str2 == 'fxlag'} {
					aZ.lagFxArgTrack(str3, num1, num2, arr);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['fxset', 'fxlag']");

		cW.add(\asm, [\str, \str, \str, \num, \str], {|str1, str2, str3, num, str4|
			if(aZ.notNil, {
				case
				{str2 == 'fx'} {
					aZ.setFx(str3, num, 1, str4);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['fx']");

		cW.add(\asm, [\str, \str, \str, \num, \num, \str], {|str1, str2, str3, num1, num2, str4|
			if(aZ.notNil, {
				case
				{str2 == 'fx'} {
					aZ.setFx(str3, num1, num2, str4);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['fx']");

		cW.add(\asm, [\str, \str, \str, \num, \str, \arr], {|str1, str2, str3, num1, str4, arr1|
			if(aZ.notNil, {
				case
				{str2 == 'fx'} {
					arr1.cs.postln;
					aZ.setFx(str3, num1, 1, str4, arr1);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['fx']");

		cW.add(\asm, [\str, \str, \str, \num, \num, \str, \arr], {|str1, str2, str3, num1, num2, str4, arr1|
			if(aZ.notNil, {
				case
				{str2 == 'fx'} {
					arr1.cs.postln;
					aZ.setFx(str3, num1, num2, str4, arr1);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['fx']");

		cW.add(\asm, [\str, \str, \str, \num, \num, \num], {|str1, str2, str3, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 'fxset'} {
					aZ.fxTrackWarn(str3, num1, num2, {|item| Ndef(item).getKeysValues[num3-1].postln

					});
				}
				{str2 == 'fxlag'} {
					aZ.fxTrackWarn(str3, num1, num2, {|item|
						var getKey, controlKeys, ratesFor;
						getKey = item;
						controlKeys = Ndef(getKey).controlKeys;
						ratesFor = Ndef(getKey).nodeMap.ratesFor(
							controlKeys);
						if(ratesFor.notNil, {
							[controlKeys[num3-1],
								ratesFor[num3-1]].radpostwarn;
						});
					});
				}
				{str2 == 'snd'} {
					aZ.setSend(str3, num1, num2, num3);
				}
				{str2 == 'sndry'} {
					aZ.setSend(str3, num1, num2, num3, dirMaster: false);
				}
				{str2 == 'setsnd'} {
					aZ.setSendKnob(str3, num1, num2, num3);
				}
			}, {
				"could not find assemblage".warn;
			});
		});

		cW.add(\asm, [\str, \str, \str, \num, \num, \num, \num], {|str1, str2, str3, num1, num2, num3, num4|
			if(aZ.notNil, {
				case
				{str2 == 'fxset'} {
					aZ.setFxArgTrack(str3, num1, num2, num3, num4);
				}
				{str2 == 'fxlag'} {
					aZ.lagFxArgTrack(str3, num1, num2, num3, num4);
				}
				{str2 == 'snd'} {
					aZ.setSend(str3, num1, num2, num3, num4);
				}
				{str2 == 'sndry'} {
					aZ.setSend(str3, num1, num2, num3, num4, false);
				}
				{str2 == 'setsnd'} {
					aZ.setSendKnob(str3, num1, num2, num3, num4);
				}
			}, {
				"could not find assemblage".warn;
			});
		});

		cW.add(\asm, [\str, \str, \str, \num, \num, \str, \num], {|str1, str2, str3, num1, num2, str4, num3|
			if(aZ.notNil, {
				case
				{str2 == 'fxset'} {
					aZ.setFxArgTrack(str3, num1, num2, str4, num3);
				}
				{str2 == 'fxlag'} {
					aZ.lagFxArgTrack(str3, num1, num2, str4, num3);
				}
			}, {
				"could not find assemblage".warn;
			});
		});
	}

	*loadFxCmds {
		//shortcuts
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

		cW.add(\fxs, [\str], {|str1|
			var synthFile;
			synthFile = SynthDefFile.read(\filter, exclude: Radicles.excludeLibs);
			synthFile.do{|item|
				var desc;
				desc = DescriptionFile.read(\filter, item, false, Radicles.excludeLibs);
				if(desc.isNil, {desc = "??"});
				(item ++ " -> " ++ desc).radpostwarn;
			};
			synthFile.radpostwarn;
		}, "fxs: posts available filters");

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
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				case
				{str2 == 't'} {aZ.modFxTrack(\track, 1, num1, num2, modArgs[0], modArgs[1]);}
				{str2 == 'b'} {aZ.modFxTrack(\bus, 1, num1, num2, modArgs[0], modArgs[1]);}
				{str2 == 'm'} {aZ.modFxTrack(\master, 1, num1, num2, modArgs[0], modArgs[1]);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, fxArg, modType");

		cW.add(\fxset, [\str, \str, \num, \str, \str], {|str1, str2, num1, str3, str4|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str4.asString.radStringMod;
				case
				{str2 == 't'} {aZ.modFxTrack(\track, 1, num1, str3, modArgs[0], modArgs[1]);}
				{str2 == 'b'} {aZ.modFxTrack(\bus, 1, num1, str3, modArgs[0], modArgs[1]);}
				{str2 == 'm'} {aZ.modFxTrack(\master, 1, num1, str3, modArgs[0], modArgs[1]);};
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackSlot, fxArg, modType");

		cW.add(\fxset, [\str, \str, \num, \num, \num, \str], {|str1, str2, num1, num2, num3, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				case
				{str2 == 't'} {aZ.modFxTrack(\track, num1, num2, num3, modArgs[0], modArgs[1]);}
				{str2 == 'b'} {aZ.modFxTrack(\bus, num1, num2, num3, modArgs[0], modArgs[1]);}
				{str2 == 'm'} {aZ.modFxTrack(\master, 1, num2, num3, modArgs[0], modArgs[1]);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, slotNum, fxArg, modType");

		cW.add(\fxset, [\str, \str, \num, \num, \str, \str], {|str1, str2, num1, num2, str3, str4|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str4.asString.radStringMod;
				case
				{str2 == 't'} {aZ.modFxTrack(\track, num1, num2, str3, modArgs[0], modArgs[1]);}
				{str2 == 'b'} {aZ.modFxTrack(\bus, num1, num2, str3, modArgs[0], modArgs[1]);}
				{str2 == 'm'} {aZ.modFxTrack(\master, 1, num2, str3, modArgs[0], modArgs[1]);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: trackType, trackNum, slotNum, fxArg, modType");

		cW.add(\fxset, [\str, \num, \num, \num, \str], {|str1, num1, num2, num3, str2|
			var trackArr, thisArr, modArgs;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				modArgs = str2.asString.radStringMod;
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.modFxTrack(thisArr[0].asSymbol, thisArr[1], num2, num3, modArgs[0], modArgs[1]);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\fxset, [\str, \num, \num, \str, \str], {|str1, num1, num2, str2, str3|
			var trackArr, thisArr, modArgs;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				modArgs = str3.asString.radStringMod;
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.modFxTrack(thisArr[0].asSymbol, thisArr[1], num2, str2, modArgs[0], modArgs[1]);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxset: mixTrackNum, filter");

		cW.add(\setfx, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str2.asString.radStringMod;
				aZ.modFx(num1, num2, modArgs[0], modArgs[1]);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg, mod");

		cW.add(\setfx, [\str, \num, \str, \str], {|str1, num1, str2, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				aZ.modFx(num1, str2, modArgs[0], modArgs[1]);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg, mod");

		//get and set fx modulation
		cW.add(\modfxget, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getFxMod(\track, 1, num1, num2);}
				{str2 == 'b'} {aZ.getFxMod(\bus, 1, num1, num2);}
				{str2 == 'm'} {aZ.getFxMod(\master, 1, num1, num2);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackSlot, arg");

		cW.add(\modfxget, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getFxMod(\track, 1, num1, str3);}
				{str2 == 'b'} {aZ.getFxMod(\bus, 1, num1, str3);}
				{str2 == 'm'} {aZ.getFxMod(\master, 1, num1, str3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackSlot, arg");

		cW.add(\modfxget, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getFxMod(\track, num1, num2, num3);}
				{str2 == 'b'} {aZ.getFxMod(\bus, num1, num2, num3);}
				{str2 == 'm'} {aZ.getFxMod(\master, 1, num2, num3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackNum, trackSlot, arg");

		cW.add(\modfxget, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.getFxMod(\track, num1, num2, str3);}
				{str2 == 'b'} {aZ.getFxMod(\bus, num1, num2, str3);}
				{str2 == 'm'} {aZ.getFxMod(\master, 1, num2, str3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackNum, trackSlot, arg");

		cW.add(\modfxget, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.getFxMod(thisArr[0].asSymbol, thisArr[1], num1, num2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: mixTrackNum, trackSlot, arg");

		cW.add(\modfxget, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.getFxMod(thisArr[0].asSymbol, thisArr[1], num1, str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: mixTrackNum, trackSlot, arg");

		cW.add(\modgetfx, [\str, \num, \num], {|str1, num1, num2|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.getFxMod(filterKey[0], filterKey[1], filterKey[1], num2);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\modgetfx, [\str, \num, \str], {|str1, num1, str2|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.getFxMod(filterKey[0], filterKey[1], filterKey[1], str2);
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
					aZ.setFxMod(thisArr[0].asSymbol, thisArr[1], num1, num2, arr1);
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
					aZ.setFxMod(thisArr[0].asSymbol, thisArr[1], num1, str2, arr1);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxset: mixTrackNum, trackSlot, arg");

		cW.add(\modsetfx, [\str, \num, \num, \arr], {|str1, num1, num2, arr1|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.setFxMod(filterKey[0], filterKey[1], filterKey[1], num2, arr1);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\modsetfx, [\str, \num, \str, \arr], {|str1, num1, str2, arr1|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.setFxMod(filterKey[0], filterKey[1], filterKey[1], str2, arr1);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\unmodfx, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmapFxTrack(\track, 1, num1, num2);}
				{str2 == 'b'} {aZ.unmapFxTrack(\bus, 1, num1, num2);}
				{str2 == 'm'} {aZ.unmapFxTrack(\master, 1, num1, num2);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmapFxTrack(\track, 1, num1, str3);}
				{str2 == 'b'} {aZ.unmapFxTrack(\bus, 1, num1, str3);}
				{str2 == 'm'} {aZ.unmapFxTrack(\master, 1, num1, str3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmapFxTrack(\track, num1, num2, num3);}
				{str2 == 'b'} {aZ.unmapFxTrack(\bus, num1, num2, num3);}
				{str2 == 'm'} {aZ.unmapFxTrack(\master, 1, num2, num3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmapFxTrack(\track, num1, num2, str3);}
				{str2 == 'b'} {aZ.unmapFxTrack(\bus, num1, num2, str3);}
				{str2 == 'm'} {aZ.unmapFxTrack(\master, 1, num2, str3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \str, \num], {|str1, str2, num1, str3, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmapFxTrack(\track, 1, num1, str3, num3);}
				{str2 == 'b'} {aZ.unmapFxTrack(\bus, 1, num1, str3, num3);}
				{str2 == 'm'} {aZ.unmapFxTrack(\master, 1, num1, str3, num3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \num, \num, \num], {|str1, str2, num1, num2, num3, num4|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmapFxTrack(\track, num1, num2, num3, num4);}
				{str2 == 'b'} {aZ.unmapFxTrack(\bus, num1, num2, num3, num4);}
				{str2 == 'm'} {aZ.unmapFxTrack(\master, 1, num2, num3, num4);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \str, \num, \num, \str, \num], {|str1, str2, num1, num2, str3, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.unmapFxTrack(\track, num1, num2, str3, num3);}
				{str2 == 'b'} {aZ.unmapFxTrack(\bus, num1, num2, str3, num3);}
				{str2 == 'm'} {aZ.unmapFxTrack(\master, 1, num2, str3, num3);};
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: trackType, trackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \num, \num], {|str1, num1, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmapFxTrack(thisArr[0].asSymbol, thisArr[1], num1, num2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: mixTrackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmapFxTrack(thisArr[0].asSymbol, thisArr[1], num1, str2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: mixTrackNum, trackSlot, arg");

		cW.add(\fxunmod, [\str, \num, \num], {|str1, num1, num2|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.unmapFxTrack(filterKey[0], filterKey[1], filterKey[1], num2);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\fxunmod, [\str, \num, \str], {|str1, num1, str2|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.unmapFxTrack(filterKey[0], filterKey[1], filterKey[1], str2);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\unmodfx, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmapFxTrack(thisArr[0].asSymbol, thisArr[1], num1, num2, num3);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: mixTrackNum, trackSlot, arg");

		cW.add(\unmodfx, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.unmapFxTrack(thisArr[0].asSymbol, thisArr[1], num1, str2, num2);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "modfxget: mixTrackNum, trackSlot, arg");

		cW.add(\fxunmod, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.unmapFxTrack(filterKey[0], filterKey[1], filterKey[1], num2, num3);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

		cW.add(\fxunmod, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			var filterKey;
			if(aZ.notNil, {
				filterKey = aZ.convFilterTag(aZ.filters[num1-1][0]);
				aZ.unmapFxTrack(filterKey[0], filterKey[1], filterKey[1], str2, num2);
			}, {
				"could not find assemblage".warn;
			});
		}, "setfx: filterNum, fxArg");

	}

	*loadMixCmds {
		cW.add(\t, [\str, \str], {|str1, str2|
			var getTrack;
			if(aZ.notNil, {
				case
				{str2 == 'names'} {
					aZ.mixTrackNames.postln;
				}
				{str2 == 'add'} {
					aZ.autoAddTrack(\track);
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
					aZ.autoAddTrack(\bus);
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
					aZ.autoAddTrack(\track, num1);
				}
				{str2 == 'addn'} {
					{num1.do{
						cond.test = false;
						aZ.autoAddTrack(\track, action: {cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
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
						aZ.autoAddTrack(\track, num2, action: {cond.test = true; cond.signal});
						cond.wait;
					};}.fork;
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

		cW.add(\setsnd, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSendKnob(\track, num1, 1, num2);}
				{str2 == 'b'} {aZ.setSendKnob(\bus, num1, 1, num2);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "setsnd: [mixTrackNum, slotNum, busNum, val]");

		cW.add(\setsnd, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSendKnob(\track, num1, num2, num3);}
				{str2 == 'b'} {aZ.setSendKnob(\bus, num1, num2, num3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "setsnd: [trackType, trackNum, slotNum, busNum]");

		cW.add(\setsnd, [\str, \str, \num, \num, \num, \num], {|str1, str2, num1, num2, num3, num4|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSendKnob(\track, num1, num2, num3, num4);}
				{str2 == 'b'} {aZ.setSendKnob(\bus, num1, num2, num3, num4);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "setsnd: [trackType, trackNum, slotNum, busNum, val]");

		cW.add(\setsnd, [\str, \num, \num], {|str1, num1, num2|
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

		cW.add(\setsnd, [\str, \num, \num, \num], {|str1, num1, num2, num3|
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

		cW.add(\setsnd, [\str, \num, \num, \num, \num], {|str1, num1, num2, num3, num4|
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

		cW.add(\setsnd, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				case
				{str2 == 't'} {
					aZ.modSend(\track, 1, num1, modArgs[0], modArgs[1]);
				}
				{str2 == 'b'} {
					aZ.modSend(\bus, 1, num1, modArgs[0], modArgs[1]);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "sndset: trackType, fxArg, modType");

		cW.add(\setsnd, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				case
				{str2 == 't'} {
					aZ.modSend(\track, num1, num2, modArgs[0], modArgs[1]);
				}
				{str2 == 'b'} {
					aZ.modSend(\bus, num1, num2, modArgs[0], modArgs[1]);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "sndset: trackType, trackNum, fxArg, modType");

		cW.add(\setsnd, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			var trackArr, thisArr, modArgs;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				modArgs = str2.asString.radStringMod;
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					thisArr.postln;
					aZ.modSend(thisArr[0].asSymbol, thisArr[1], num2, modArgs[0], modArgs[1]);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "sndset: mixTrackNum, filter");

		cW.add(\setsnd, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, thisArr, modArgs;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				modArgs = str2.asString.radStringMod;
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					thisArr.postln;
					aZ.modSend(thisArr[0].asSymbol, thisArr[1], 1, modArgs[0], modArgs[1]);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "sndset: mixTrackNum, filter");

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
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				case
				{str2 == 't'} {aZ.modMix(\track, num1, \vol, modArgs[0], modArgs[1])}
				{str2 == 'b'} {aZ.modMix(\bus, num1, \vol, modArgs[0], modArgs[1])}
				{str2 == 'm'} {aZ.modMix(\master, num1, \vol, modArgs[0], modArgs[1])}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, trackNum, mod]");

		cW.add(\pan, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				case
				{str2 == 't'} {aZ.modMix(\track, num1, \pan, modArgs[0], modArgs[1])}
				{str2 == 'b'} {aZ.modMix(\bus, num1, \pan, modArgs[0], modArgs[1])}
				{str2 == 'm'} {aZ.modMix(\master, num1, \pan, modArgs[0], modArgs[1])}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNum, mod]");

		cW.add(\trim, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				case
				{str2 == 't'} {aZ.modMix(\track, num1, \trim, modArgs[0], modArgs[1]);}
				{str2 == 'b'} {aZ.modMix(\bus, num1, \trim, modArgs[0], modArgs[1]);}
				{str2 == 'm'} {aZ.modMix(\master, num1, \trim, modArgs[0], modArgs[1]);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, trackNum, mod]");

		cW.add(\vol, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, modArgs;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				modArgs = str2.asString.radStringMod;
				aZ.modMix(trackArr[0], trackArr[1], \vol, modArgs[0], modArgs[1])
			}, {
				"assemblage is already running".warn;
			});
		}, "vol: masterTrackNum, mod");

		cW.add(\pan, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, modArgs;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				modArgs = str2.asString.radStringMod;
				aZ.modMix(trackArr[0], trackArr[1], \pan, modArgs[0], modArgs[1])
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, mod");

		cW.add(\trim, [\str, \num, \str], {|str1, num1, str2|
			var trackArr, modArgs;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
				modArgs = str2.asString.radStringMod;
				aZ.modMix(trackArr[0], trackArr[1], \trim, modArgs[0], modArgs[1])
			}, {
				"assemblage is already running".warn;
			});
		}, "trim: masterTrackNum, mod");

		//dash for multiple tracks
		cW.add(\vol, [\str, \str, \dash, \str], {|str1, str2, dash, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				dash.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \vol, modArgs[0], modArgs[1])}
						{str2 == 'b'} {aZ.modMix(\bus, item, \vol, modArgs[0], modArgs[1])}
						{str2 == 'm'} {aZ.modMix(\master, item, \vol, modArgs[0], modArgs[1])}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, trackNums, mod]");

		cW.add(\pan, [\str, \str, \dash, \str], {|str1, str2, dash, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				dash.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \pan, modArgs[0], modArgs[1])}
						{str2 == 'b'} {aZ.modMix(\bus, item, \pan, modArgs[0], modArgs[1])}
						{str2 == 'm'} {aZ.modMix(\master, item, \pan, modArgs[0], modArgs[1])}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNums, mod]");

		cW.add(\trim, [\str, \str, \dash, \str], {|str1, str2, dash, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				dash.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \trim, modArgs[0], modArgs[1])}
						{str2 == 'b'} {aZ.modMix(\bus, item, \trim, modArgs[0], modArgs[1])}
						{str2 == 'm'} {aZ.modMix(\master, item, \trim, modArgs[0], modArgs[1])}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, trackNums, mod]");

		cW.add(\vol, [\str, \dash, \str], {|str1, dash, str2|
			var trackArr, modArgs;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					modArgs = str2.asString.radStringMod;
					{aZ.modMix(trackArr[0], trackArr[1], \vol, modArgs[0], modArgs[1])}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "vol: masterTrackNum, mod");

		cW.add(\pan, [\str, \dash, \str], {|str1, dash, str2|
			var trackArr, modArgs;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					modArgs = str2.asString.radStringMod;
					{aZ.modMix(trackArr[0], trackArr[1], \pan, modArgs[0], modArgs[1])}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, mod");

		cW.add(\trim, [\str, \dash, \str], {|str1, dash, str2|
			var trackArr, modArgs;
			if(aZ.notNil, {
				dash.do{|item|
					trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
					modArgs = str2.asString.radStringMod;
					{aZ.modMix(trackArr[0], trackArr[1], \trim, modArgs[0], modArgs[1])}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "trim: masterTrackNum, mod");

		//arrays instead of dashes
		cW.add(\vol, [\str, \str, \arr, \str], {|str1, str2, arr, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				arr.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \vol, modArgs[0], modArgs[1])}
						{str2 == 'b'} {aZ.modMix(\bus, item, \vol, modArgs[0], modArgs[1])}
						{str2 == 'm'} {aZ.modMix(\master, item, \vol, modArgs[0], modArgs[1])}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, trackNums, mod]");

		cW.add(\pan, [\str, \str, \arr, \str], {|str1, str2, arr, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				arr.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \pan, modArgs[0], modArgs[1])}
						{str2 == 'b'} {aZ.modMix(\bus, item, \pan, modArgs[0], modArgs[1])}
						{str2 == 'm'} {aZ.modMix(\master, item, \pan, modArgs[0], modArgs[1])}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, trackNums, mod]");

		cW.add(\trim, [\str, \str, \arr, \str], {|str1, str2, arr, str3|
			var modArgs;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
				arr.do{|item|
					{
						case
						{str2 == 't'} {aZ.modMix(\track, item, \trim, modArgs[0], modArgs[1])}
						{str2 == 'b'} {aZ.modMix(\bus, item, \trim, modArgs[0], modArgs[1])}
						{str2 == 'm'} {aZ.modMix(\master, item, \trim, modArgs[0], modArgs[1])}
						;
					}.defer;
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, trackNums, mod]");

		cW.add(\vol, [\str, \arr, \str], {|str1, arr, str2|
			var trackArr, modArgs;
			if(aZ.notNil, {
				arr.do{|item|
					{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
						modArgs = str2.asString.radStringMod;
						aZ.modMix(trackArr[0], trackArr[1], \vol, modArgs[0], modArgs[1])}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "vol: masterTrackNum, value");

		cW.add(\pan, [\str, \arr, \str], {|str1, arr, str2|
			var trackArr, modArgs;
			if(aZ.notNil, {
				arr.do{|item|
					{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
						modArgs = str2.asString.radStringMod;
						aZ.modMix(trackArr[0], trackArr[1], \pan, modArgs[0], modArgs[1])}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "pan: masterTrackNum, value");

		cW.add(\trim, [\str, \arr, \str], {|str1, arr, str2|
			var trackArr, modArgs;
			if(aZ.notNil, {
				arr.do{|item|
					{trackArr = aZ.mixTrackNames[item-1].asString.divNumStr;
						modArgs = str2.asString.radStringMod;
						aZ.modMix(trackArr[0], trackArr[1], \trim, modArgs[0], modArgs[1])}.defer;
				};
			}, {
				"assemblage is already running".warn;
			});
		}, "trim: masterTrackNum, value");

		//bulk track controls
		cW.add(\vol, [\str, \str, \str], {|str1, str2, str3|
			var trackArr, modArgs, string;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
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
						aZ.modMix(item[0].asSymbol, item[1], \vol, modArgs[0], modArgs[1])
					}.defer;
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: [trackType, val]");

		cW.add(\pan, [\str, \str, \str], {|str1, str2, str3|
			var trackArr, modArgs, string;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
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
						aZ.modMix(item[0].asSymbol, item[1], \pan, modArgs[0], modArgs[1])
					}.defer;
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: [trackType, val]");

		cW.add(\trim, [\str, \str, \str], {|str1, str2, str3|
			var trackArr, modArgs, string;
			if(aZ.notNil, {
				modArgs = str3.asString.radStringMod;
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
						aZ.modMix(item[0].asSymbol, item[1], \trim, modArgs[0], modArgs[1])
					}.defer;
				}
			}, {
				"could not find assemblage".warn;
			});
		}, "trim: [trackType, val]");
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

		cW.add(\blk, [\str, \str, \num, \str], {|str1, str2, num, str3|
			case
			{str2 == 'play'} {Block.play(num, str3);}
			{str2 == 'ply'} {Block.play(num, str3);}
			{str2 == 'pn'} {Block.play(num, \pattern, \nobuf, str3);}
			;
		}, "block: ['play', 'ply', 'pn'], [block, block, block], [blockName, blockName, pnpreset]");

		cW.add(\blk, [\str, \str, \num, \arr], {|str1, str2, num, arr|
			case
			{str2 == 'pn'} {Block.play(num, \pattern, \nobuf, arr);}
			;
		}, "block: ['play', 'ply', 'pn'], [block, block, block], [blockName, blockName, pnarr]");

		cW.add(\blk, [\str, \str, \num, \str, \str], {|str1, str2, num, str3, str4|
			case
			{str2 == 'play'} {Block.play(num, str3, str4);}
			{str2 == 'ply'} {Block.play(num, str3, str4);}
			;
		}, "block: ['play', 'ply'], [block, block], [blockName, blockName], [buffer, buffer]");

		cW.add(\blk, [\str, \str, \num, \str, \arr], {|str1, str2, num, str3, arr|
			case
			{str2 == 'play'} {Block.play(num, str3, extraArgs: arr);}
			{str2 == 'ply'} {Block.play(num, str3, extraArgs: arr);}
			;
		}, "block: ['play', 'ply'], [block, block], [blockName, blockName], [extraArgs, extraArgs]");

		cW.add(\blk, [\str, \str, \num, \str, \str, \arr], {|str1, str2, num, str3, str4, arr|
			case
			{str2 == 'play'} {Block.play(num, str3, str4, arr);}
			{str2 == 'ply'} {Block.play(num, str3, str4, arr);}
			;
		}, "block: ['play', 'ply'], [block, block], [blockName, blockName], [buffer, buffer], [extraArgs, extraArgs]");

		cW.add(\blk, [\str, \str, \num, \str, \str, \str], {|str1, str2, num, str3, str4, str5|
			case
			{str2 == 'play'} {Block.play(num, str3, str4, str4);}
			{str2 == 'ply'} {Block.play(num, str3, str4, str5);}
			;
		}, "block: ['play', 'ply'], [block, block], [blockName, blockName], [extraArgs, extraArgs]");
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
			var thisIndex, thisKey, modArgs;
			thisIndex = Block.ndefs.indexOf(Ndef(("block" ++ num1).asSymbol));
			thisKey = Block.ndefs[thisIndex].controlKeys[num2-1];
			modArgs = str2.asString.radStringMod;
			Block.modBlk(num1, thisKey, modArgs[0], modArgs[1]);
		}, "blocksetn: blk, arg");

		cW.add(\blkset, [\str, \num, \str, \str], {|str1, num1, str2, str3|
			var modArgs;
			modArgs = str3.asString.radStringMod;
			Block.modBlk(num1, str2, modArgs[0], modArgs[1]);
		}, "blocksetn: blk, arg");

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

		cW.add(\blksetn, [\str, \num, \num, \arr], {|str1, num1, arr1|
			Block.set(num1, arr1);
		}, "blocksetn: blk, arg");

		cW.add(\blkxsetn, [\str, \num, \num, \arr], {|str1, num1, arr1|
			Block.xset(num1, arr1);
		}, "blockxsetn: blk, arg");

		cW.add(\blklagn, [\str, \num, \num, \arr], {|str1, num1, arr1|
			Block.lag(num1, arr1);
		}, "blocksetn: blk, val");

		cW.add(\blkget, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			Block.set(num1, [str2, num2]);
		}, "blocksetn: blk, arg");

		cW.add(\modblkget, [\str, \num, \num], {|str1, num1, num2|
			Block.getBlkMod(num1, num2);
		}, "modblockget: blk, arg");

		cW.add(\modblkget, [\str, \num, \str], {|str1, num1, str2|
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
			Block.unmapBlk(num1, num2);
		}, "modblockget: blk, arg");

		cW.add(\unmodblk, [\str, \num, \str], {|str1, num1, str2|
			Block.unmapBlk(num1, str2);
		}, "modblockget: blk, arg");

		cW.add(\unmodblk, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			Block.unmapBlk(num1, num2, num3);
		}, "modblockget: blk, arg");

		cW.add(\unmodblk, [\str, \num, \str, \num], {|str1, num1, str2, num2|
			Block.unmapBlk(num1, str2, num2);
		}, "modblockget: blk, arg");

		//blk shortcuts
		cW.add(\pl, [\str, \num, \str], {|str1, num1, str2|
			Block.play(num1, \play, str2);
		}, "pl: block, buffer");
		cW.add(\lp, [\str, \num, \str], {|str1, num1, str2|
			Block.play(num1, \loop, str2);
		}, "lp: block, buffer");
		cW.add(\pl, [\str,  \num, \str, \arr], {|str1, num1, str2, arr1|
			Block.play(num1, \play, str2, arr1);
		}, "pl: block, buffer, extraArgs]");
		cW.add(\lp, [\str,  \num, \str, \arr], {|str1, num1, str2, arr1|
			Block.play(num1, \loop, str2, arr1);
		}, "lp: block, buffer, extraArgs]");

	}

	*loadBaseCmds {

		cW.add(\modspec, [\str], {|str1, str2, str3, str4|
			SpecFile.read('modulation').postln;
		}, "modspec: posts modulation specfile");

		cW.add(\mods, [\str], {|str1, str2, str3, str4|
			ControlFile.read(\modulation).postln;
		}, "mods: posts mod types");

		cW.add(\mod, [\str, \str, \str, \str], {|str1, str2, str3, str4|
			ModMap.map(Ndef(str2), str3, str4);
		}, "modMap: ndef, key, type");

		cW.add(\mod, [\str, \str, \str, \str, \str], {|str1, str2, str3, str4, str5|
			ModMap.map(Ndef(str2), str3, str4, str5);
		}, "modMap: ndef, key, type, spec");

		cW.add(\mod, [\str, \str, \str, \str, \arr], {|str1, str2, str3, str4, arr1|
			ModMap.map(Ndef(str2), str3, str4, arr1);
		}, "modMap: ndef, key, type, spec");

		cW.add(\mod, [\str, \str, \str, \str, \arr, \arr], {|str1, str2, str3, str4, arr1, arr2|
			ModMap.map(Ndef(str2), str3, str4, arr1, arr2);
		}, "modMap: ndef, key, type, spec");

		cW.add(\mod, [\str, \str, \str, \str, \str, \arr], {|str1, str2, str3, str4, str5, arr1|
			ModMap.map(Ndef(str2), str3, str4, str5, arr1);
		}, "modMap: ndef, key, type, spec");

		cW.add(\unmod, [\str, \str, \str], {|str1, str2, str3|
			ModMap.unmap(Ndef(str2), str3);
		}, "mod unmap: ndef, key, type");

		cW.add(\unmod, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			ModMap.unmap(Ndef(str2), str3, num1);
		}, "mod unmap: ndef, key, type, value");

		cW.add(\unmod, [\str, \num], {|str1, num1|
			var nodes;
			ModMap.unmapAt(num1-1);
		}, "mod unmap: index");

		cW.add(\unmod, [\str, \num, \num], {|str1, num1, num2|
			var nodes;
			nodes = ModMap.modNodes[num1-1];
			ModMap.unmap(nodes[1], nodes[2], num2);
		}, "mod unmap: index, val");

		cW.add(\getmods, [\str], {|str1|
			var nodes;
			ModMap.modNodes.dopostln;
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

		cW.add(\rad, [\str, \str, \str], {|str1, str2, str3|
			case
			{str2 == 'imp'} {
				if((str3 == 'All').or(str3 == 'all'), {
					Radicles.selLibs(Radicles.allLibs);
				}, {
					Radicles.selLibs([str3.asString]);
				});
			}
			;
		}, "radicles: [['imp'], ['lib']]");

		cW.add(\rad, [\str, \str, \arr], {|str1, str2, arr1|
			var strArr;
			strArr = arr1.collect({|item| item.asString });
			case
			{str2 == 'imp'} {Radicles.selLibs(strArr);}
			;
		}, "radicles: [['imp'], ['libs']]");

		//base associations
		(0..9).do{|dim|
			cW.storeIndex = dim;
			cW.add(\mix, [\str], {
				cW.callFunc("asm mix", callIndex: 0);
			}, "mix");
			cW.add(\nomix, [\str], {
				cW.callFunc("asm nomix", callIndex: 0);
			}, "nomix");
			cW.add(\prec, [\str], {
				cW.callFunc("asm preprec", callIndex: 0);
			}, "prepare record");
			cW.add(\rec, [\str], {
				cW.callFunc("asm startrec", callIndex: 0);
			}, "start record");
			cW.add(\srec, [\str], {
				cW.callFunc("asm stoprec", callIndex: 0);
			}, "stop record");
			cW.add(\imp, [\str, \str], {|str1, str2|
				cW.callFunc(("rad imp " ++ str2.asString), callIndex: 0);
			}, "imp");
			cW.add(\imp, [\str, \arr], {|str1, arr1|
				var strArr;
				strArr = arr1.collect({|item| item.asString });
				Radicles.selLibs(strArr);
			}, "imp: arr");
		};
		cW.storeIndex = 0;
	}

}