// Radicles: call window commands for blocks: blk, pl/lp, in, st, pn, metro, record buffers, clock/bpm.
// Part of the Radicles class: the class and its variables are in Radicles.sc.

+ Radicles {
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
			(BStore.buffStrByID(Block.recBuf(str2.asString.divNumStr[1])) ++ ".write(\"" ++ recPath ++ timestamp ++ ".wav\", \"wav\", \"int16\")").radcode;
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
					".wav\", \"wav\", \"int16\")").radcode;
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
}
