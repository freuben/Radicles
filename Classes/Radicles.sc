Radicles {classvar <>mainPath, <>fileExtFile, <>nodeTime=0.08, <server, <>postWin=nil,
	<>postWhere=\ide, <>fadeTime=0.5, <>schedFunc, <>schedDiv=1,
	<bpm, <postDoc, <>lineSize=68, <>logCodeTime=false, <>reducePostControl=false,
	<>ignorePost=false, <>ignorePostcont=false, <>colorCritical, <>colorMeter, <>colorWarning, <>colorTrack, <>colorBus, <>colorMaster, <>colorTextField, <>cW, <aZ;

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
		fileExtFile ?? {fileExtFile = mainPath ++ "Files/"};
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

	*callWindow {arg name;
		name ?? {name = "Call Window";};
		cW = CallWindow.window(name, Window.win4TopRight, postBool: true);

		//server boot
		cW.add(\boot, [\str], {
			server.boot;
		});

		//assemblage
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
					aZ.mixTrackNames.radpost;
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
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: ['mix', 'nomix']");
		//
		cW.add(\asm, [\str, \str, \str], {|str1, str2, str3|
			if(aZ.notNil, {
				case
				{str2 == 'add'} {
					aZ.autoAddTrack(str3);
				};
			}, {
				"could not find assemblage".warn;
			});
		}, "assemblage: trackNum, busNum, chanNumArr, spaceType");
		//
		cW.add(\asm, [\str, \str, \str, \num], {|str1, str2, str3, num|
			if(aZ.notNil, {
				case
				{str2 == 'in'} {
					aZ.input(Ndef(str3), \track, num);
				}
				{str2 == 'add'} {
					aZ.autoAddTrack(str3, num);
				}
				{str2 == 'fxremove'} {
					aZ.setFx(str3, num, 1, remove: true);
				}
				;
			}, {
				"could not find assemblage".warn;
			});
		});

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
				{str2 == 'fxset'} {
					aZ.setFxArgTrack(str3, num1, num2);
				}
				{str2 == 'fxremove'} {
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
				;
			}, {
				"could not find assemblage".warn;
			});
		});

		cW.add(\asm, [\str, \str, \str, \num, \str], {|str1, str2, str3, num, str4|
			if(aZ.notNil, {
				case
				{str2 == 'fx'} {
					aZ.setFx(str3, num, 1, str4);
				};
			}, {
				"could not find assemblage".warn;
			});
		});

		cW.add(\asm, [\str, \str, \str, \num, \num, \str], {|str1, str2, str3, num1, num2, str4|
			if(aZ.notNil, {
				case
				{str2 == 'fx'} {
					aZ.setFx(str3, num1, num2, str4);
				};
			}, {
				"could not find assemblage".warn;
			});
		});

		cW.add(\asm, [\str, \str, \str, \num, \num, \num], {|str1, str2, str3, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 'fxset'} {
					aZ.fxTrackWarn(str3, num1, num2, {|item| Ndef(item).getKeysValues[num3-1].postln });
				}
						{str2 == 'snd'} {
			aZ.setSend(str3, num1, num2, num3);
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
			}, {
				"could not find assemblage".warn;
			});
		});

		//radicles
		cW.add(\rad, [\str, \str], {|str1, str2|
			case
			{str2 == 'doc'} {Radicles.document;}
			{str2 == 'fade'} {Radicles.fadeTime.postln};
		}, "radicles: ['doc', 'fade']");
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

		//shortcuts
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

cW.add(\vol, [\str, \num, \num], {|str, num1, num2|
	var trackArr;
			if(aZ.notNil, {
		trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
		aZ.setVolume(trackArr[0], trackArr[1], num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: masterTrackNum, value");

		cW.add(\vollag, [\str, \num, \num], {|str, num1, num2|
	var trackArr;
			if(aZ.notNil, {
		trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
		aZ.setVolumeLag(trackArr[0], trackArr[1], num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: masterTrackNum, value");

cW.add(\pan, [\str, \num, \num], {|str, num1, num2|
	var trackArr;
			if(aZ.notNil, {
		trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
		aZ.setPan(trackArr[0], trackArr[1], num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: masterTrackNum, value");

cW.add(\panlag, [\str, \num, \num], {|str, num1, num2|
	var trackArr;
			if(aZ.notNil, {
		trackArr = aZ.mixTrackNames[num1-1].asString.divNumStr;
		aZ.setPanLag(trackArr[0], trackArr[1], num2);
			}, {
				"assemblage is already running".warn;
			});
		}, "assemblage: masterTrackNum, value");
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
		}, "assemblage: masterTrackNum, value");

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
		}, "assemblage: masterTrackNum, value");

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
		}, "assemblage: masterTrackNum, value");

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
		}, "assemblage: masterTrackNum, value");
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
		}, "assemblage: masterTrackNum, value");

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
		}, "assemblage: masterTrackNum, value");

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
		}, "assemblage: masterTrackNum, value");

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
		}, "assemblage: masterTrackNum, value");
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

		cW.add(\vol, [\str, \num], {|str1, num1|
	var trackArr, string;
			if(aZ.notNil, {
		trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
		trackArr.do{|item| if(item[1].isNil, {item[1] = 1}) };
			trackArr.do{|item|
			aZ.setVolume(item[0].asSymbol, item[1], num1);
			}
			}, {
				"could not find assemblage".warn;
			});
		}, "vol: val");
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
		cW.add(\pan, [\str, \num], {|str1, num1|
	var trackArr, string;
			if(aZ.notNil, {
		trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
		trackArr.do{|item| if(item[1].isNil, {item[1] = 1}) };
			trackArr.do{|item|
			aZ.setPan(item[0].asSymbol, item[1], num1);
			}
			}, {
				"could not find assemblage".warn;
			});
		}, "pan: val");
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

		//rounting blocks to assemblage
		cW.add('blk', [\str, \num, \str, \num], {|str1, num1, str2, num2|
			if(str2 == '<>', {
				if(aZ.notNil, {
					aZ.input(Block.ndefs[num1-1], \track, num2);
				}, {
					"could not find assemblage".warn;
				});
			});
		});
		cW.add('blk', [\str, \dash, \str, \num], {|str1, arr, str2, num2|
			var ndefArr;
			if(str2 == '<>', {
				if(aZ.notNil, {
					ndefArr = arr.collect{|item| Block.ndefs[item-1] };
					aZ.input(ndefArr, \track, num2);
				}, {
					"could not find assemblage".warn;
				});
			});
		});
		cW.add('blk', [\str, \dash, \str, \dash], {|str1, arr1, str2, arr2|
			var ndefArr;
			if(str2 == '<>', {
				if(aZ.notNil, {
					arr1.do{|item, index|
						aZ.input(Block.ndefs[item-1], \track, arr2[index]);
					};
				}, {
					"could not find assemblage".warn;
				});
			});
		});

		//base associations
		(0..9).do{|dim|
			cW.storeIndex = dim;
			cW.add(\mix, [\str], {
				cW.callFunc("asm mix", callIndex: 0);
			}, "mix");
			cW.add(\nomix, [\str], {
				cW.callFunc("asm nomix", callIndex: 0);
			}, "nomix");
		};
		cW.storeIndex = 0;
	}

}