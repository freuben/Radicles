Radicles {classvar <>mainPath, <>nodeTime=0.08, <server, <>postWin=nil,
	<>postWhere=\ide, <>fadeTime=0.5, <>schedFunc, <>schedDiv=1,
	<bpm, <postDoc, <>lineSize=68, <>logCodeTime=false, <>reducePostControl=false,
	<>ignorePost=false, <>ignorePostcont=false, <>colorCritical, <>colorMeter, <>colorWarning, <>colorTrack, <>colorBus, <>colorMaster, <>colorTextField, <>callWin, <assemblage;

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
		callWin = CallWindow.window(name, Window.win4TopRight, postBool: true);

		//server boot
callWin.add(\boot, [\str], {
	server.boot;
});

//assemblage
callWin.add(\asm, [\str, \num], {|str, num|
	if(assemblage.isNil, {
		assemblage = Assemblage(num, 1);
	}, {
		"assemblage is already running".warn;
	});
}, "assemblage: trackNum");
callWin.add(\asm, [\str, \num, \num], {|str, num1, num2|
	if(assemblage.isNil, {
		assemblage = Assemblage(num1, num2);
	}, {
		"assemblage is already running".warn;
	});
}, "assemblage: trackNum, busNum");
callWin.add(\asm, [\str, \num, \num, \num], {|str, num1, num2, num3|
	if(assemblage.isNil, {
		assemblage = Assemblage(num1, num2, num3);
	}, {
		"assemblage is already running".warn;
	});
}, "assemblage: trackNum, busNum, chanNum");
//chanNumArr can be multidimentional for different chanNums in different tracks
callWin.add(\asm, [\str, \num, \num, \arr], {|str, num1, num2, arr|
	if(assemblage.isNil, {
		assemblage = Assemblage(num1, num2, arr);
	}, {
		"assemblage is already running".warn;
	});
}, "assemblage: trackNum, busNum, chanNumArr");

callWin.add(\asm, [\str, \num, \num, \num, \str], {|str1, num1, num2, num3, str2|
	if(assemblage.isNil, {
		assemblage = Assemblage(num1, num2, num3, str2);
	}, {
		"assemblage is already running".warn;
	});
}, "assemblade: trackNum, busNum, chanNum, spaceType");
callWin.add(\asm, [\str, \num, \num, \arr, \str], {|str1, num1, num2, arr, str2|
	if(assemblage.isNil, {
		assemblage = Assemblage(num1, num2, arr, str2);
	}, {
		"assemblage is already running".warn;
	});
}, "assemblage: trackNum, busNum, chanNumArr, spaceType");

callWin.add(\asm, [\str, \str], {|str1, str2|
	if(assemblage.notNil, {
		case
		{str2 == 'mix'} {
			assemblage.mixer;
		}
		{str2 == 'nomix'} {
			assemblage.nomixer;
		}
		{str2 == 'names'} {
			assemblage.mixTrackNames.radpost;
		}
		{str2 == 'preprec'} {
			assemblage.prepareRecording;
		}
		{str2 == 'startrec'} {
			assemblage.startRecording;
		}
		{str2 == 'stoprec'} {
			assemblage.stopRecording;
		}
		;
	}, {
		"could not find assemblage".warn;
	});
}, "assemblage: ['mix', 'nomix']");
//
callWin.add(\asm, [\str, \str, \str], {|str1, str2, str3|
		if(assemblage.notNil, {
		case
		{str2 == 'add'} {
		assemblage.autoAddTrack(str3);
		};
	}, {
			"could not find assemblage".warn;
	});
}, "assemblage: trackNum, busNum, chanNumArr, spaceType");
//
callWin.add(\asm, [\str, \str, \str, \num], {|str1, str2, str3, num|
	if(assemblage.notNil, {
		case
		{str2 == 'in'} {
			assemblage.input(Ndef(str3), \track, num);
		}
		{str2 == 'add'} {
		assemblage.autoAddTrack(str3, num);
		}
		{str2 == 'fxremove'} {
		assemblage.setFx(str3, num, 1, remove: true);
		}
		;
	}, {
		"could not find assemblage".warn;
	});
});

callWin.add(\asm, [\str, \str, \str, \num, \num], {|str1, str2, str3, num1, num2|
	if(assemblage.notNil, {
		case
		{str2 == 'vol'} {
				assemblage.setVolume(str3, num1, num2);
		}
		{str2 == 'vollag'} {
			assemblage.setVolumeLag(str3, num1, num2);
		}
		{str2 == 'pan'} {
				assemblage.setPan(str3, num1, num2);
		}
		{str2 == 'panlag'} {
			assemblage.setPanLag(str3, num1, num2);
		}
		{str2 == 'fxset'} {
			assemblage.setFxArgTrack(str3, num1, num2);
		}
		{str2 == 'fxremove'} {
		assemblage.setFx(str3, num1, num2, remove: true);
		}
		{str2 == 'mute'} {
			assemblage.setMute(str3, num1, num2);
		}
		{str2 == 'rec'} {
			assemblage.setRec(str3, num1, num2);
		}
		{str2 == 'solo'} {
			assemblage.setSolo(str3, num1, num2);
		}
		;
	}, {
		"could not find assemblage".warn;
	});
});

callWin.add(\asm, [\str, \str, \str, \num, \str], {|str1, str2, str3, num, str4|
		if(assemblage.notNil, {
		case
		{str2 == 'fx'} {
			assemblage.setFx(str3, num, 1, str4);
		};
			}, {
		"could not find assemblage".warn;
	});
	});

callWin.add(\asm, [\str, \str, \str, \num, \num, \str], {|str1, str2, str3, num1, num2, str4|
		if(assemblage.notNil, {
		case
		{str2 == 'fx'} {
			assemblage.setFx(str3, num1, num2, str4);
		};
			}, {
		"could not find assemblage".warn;
	});
	});

callWin.add(\asm, [\str, \str, \str, \num, \num, \num], {|str1, str2, str3, num1, num2, num3|
	if(assemblage.notNil, {
		case
		{str2 == 'fxset'} {
			assemblage.fxTrackWarn(str3, num1, num2, {|item| Ndef(item).getKeysValues[num3-1].postln });
		}
	}, {
		"could not find assemblage".warn;
	});
	});

callWin.add(\asm, [\str, \str, \str, \num, \num, \num, \num], {|str1, str2, str3, num1, num2, num3, num4|
	if(assemblage.notNil, {
		case
		{str2 == 'fxset'} {
			assemblage.setFxArgTrack(str3, num1, num2, num3, num4);
		}
	}, {
		"could not find assemblage".warn;
	});
	});

callWin.add(\asm, [\str, \str, \str, \num, \num, \str, \num], {|str1, str2, str3, num1, num2, str4, num3|
	if(assemblage.notNil, {
		case
		{str2 == 'fxset'} {
			assemblage.setFxArgTrack(str3, num1, num2, str4, num3);
		}
	}, {
		"could not find assemblage".warn;
	});
	});

//radicles
callWin.add(\rad, [\str, \str], {|str1, str2|
	case
	{str2 == 'doc'} {Radicles.document;}
	{str2 == 'fade'} {Radicles.fadeTime.postln};
}, "radicles: ['doc', 'fade']");
//blocks
callWin.add(\blk, [\str, \str], {|str1, str2|
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

callWin.add(\blk, [\str, \str, \num], {|str1, str2, num|
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

callWin.add(\blk, [\str, \str, \num, \num], {|str1, str2, num1, num2|
	case
	{str2 == 'addn'} {Block.addNum(num1, num2);}
	{str2 == 'stop'} {Block.stop(num1, num2);}
	;
}, "block: ['addn', 'stop'], [numer, block], [channels, fadeOut]");

callWin.add(\blk, [\str, \str, \num, \str], {|str1, str2, num, str3|
	case
	{str2 == 'play'} {Block.play(num, str3);}
	{str2 == 'ply'} {Block.play(num, str3);}
	{str2 == 'pn'} {Block.play(num, \pattern, \nobuf, str3);}
	;
}, "block: ['play', 'ply', 'pn'], [block, block, block], [blockName, blockName, pnpreset]");

callWin.add(\blk, [\str, \str, \num, \arr], {|str1, str2, num, arr|
	case
	{str2 == 'pn'} {Block.play(num, \pattern, \nobuf, arr);}
	;
}, "block: ['play', 'ply', 'pn'], [block, block, block], [blockName, blockName, pnarr]");

callWin.add(\blk, [\str, \str, \num, \str, \str], {|str1, str2, num, str3, str4|
	case
	{str2 == 'play'} {Block.play(num, str3, str4);}
	{str2 == 'ply'} {Block.play(num, str3, str4);}
	;
}, "block: ['play', 'ply'], [block, block], [blockName, blockName], [buffer, buffer]");

callWin.add(\blk, [\str, \str, \num, \str, \arr], {|str1, str2, num, str3, arr|
	case
	{str2 == 'play'} {Block.play(num, str3, extraArgs: arr);}
	{str2 == 'ply'} {Block.play(num, str3, extraArgs: arr);}
	;
}, "block: ['play', 'ply'], [block, block], [blockName, blockName], [extraArgs, extraArgs]");

callWin.add(\blk, [\str, \str, \num, \str, \str, \arr], {|str1, str2, num, str3, str4, arr|
	case
	{str2 == 'play'} {Block.play(num, str3, str4, arr);}
	{str2 == 'ply'} {Block.play(num, str3, str4, arr);}
	;
}, "block: ['play', 'ply'], [block, block], [blockName, blockName], [buffer, buffer], [extraArgs, extraArgs]");

callWin.add(\blk, [\str, \str, \num, \str, \str, \str], {|str1, str2, num, str3, str4, str5|
	case
	{str2 == 'play'} {Block.play(num, str3, str4, str4);}
	{str2 == 'ply'} {Block.play(num, str3, str4, str5);}
	;
}, "block: ['play', 'ply'], [block, block], [blockName, blockName], [extraArgs, extraArgs]");
//blk ply 1 pattern nobuf [ [\note, \seq, [10,6,2,2]], [\dur, 0.5], [\instrument, \perkysine] ]

//rounting blocks to assemblage
callWin.add('blk', [\str, \num, \str, \num], {|str1, num1, str2, num2|
	if(str2 == '<>', {
		if(assemblage.notNil, {
		assemblage.input(Block.ndefs[num1-1], \track, num2);
		}, {
		"could not find assemblage".warn;
	});
	});
});
callWin.add('blk', [\str, \dash, \str, \num], {|str1, arr, str2, num2|
	var ndefArr;
	if(str2 == '<>', {
		if(assemblage.notNil, {
			ndefArr = arr.collect{|item| Block.ndefs[item-1] };
		assemblage.input(ndefArr, \track, num2);
		}, {
		"could not find assemblage".warn;
	});
	});
});
callWin.add('blk', [\str, \dash, \str, \dash], {|str1, arr1, str2, arr2|
	var ndefArr;
	if(str2 == '<>', {
		if(assemblage.notNil, {
			arr1.do{|item, index|
				assemblage.input(Block.ndefs[item-1], \track, arr2[index]);
			};
		}, {
		"could not find assemblage".warn;
	});
	});
});

//base associations
(0..9).do{|dim|
	callWin.storeIndex = dim;
	callWin.add(\mix, [\str], {
	callWin.callFunc("asm mix", callIndex: 0);
	}, "mix");
	callWin.add(\nomix, [\str], {
	callWin.callFunc("asm nomix", callIndex: 0);
	}, "nomix");
};
callWin.storeIndex = 0;
	}

}