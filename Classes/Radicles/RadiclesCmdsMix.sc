// Radicles: call window commands for the mixer: asm, tracks, vol/pan/trim, sends, mute/solo, in/out, ndef.
// Part of the Radicles class: the class and its variables are in Radicles.sc.

+ Radicles {
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
					{aZ.refreshFunc}.defer; //only if the mixer is open
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
					{aZ.refreshFunc}.defer; //only if the mixer is open
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
		}, "sndset: [mixTrackNum, slotNum, busNum, val]");

		cW.add(\sndset, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSendKnob(\track, num1, num2, num3);}
				{str2 == 'b'} {aZ.setSendKnob(\bus, num1, num2, num3);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "sndset: [trackType, trackNum, slotNum, busNum]");

		cW.add(\sndset, [\str, \str, \num, \num, \num, \num], {|str1, str2, num1, num2, num3, num4|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.setSendKnob(\track, num1, num2, num3, num4);}
				{str2 == 'b'} {aZ.setSendKnob(\bus, num1, num2, num3, num4);}
				;
			}, {
				"could not find assemblage".warn;
			});
		}, "sndset: [trackType, trackNum, slotNum, busNum, val]");

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
		}, "sndset: [mixTrackNum, busNum]");

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
		}, "sndset: [mixTrackNum, slotNum, busNum]");

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
		}, "sndset: [mixTrackNum, slotNum, busNum, val]");

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
			).radcode;
		}, "ndef set");

		cW.add(\ndefset, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			("Ndef(" ++ str2.cs ++ ").set(" ++ str3.cs ++ ", " ++ num1 ++ ");").radcode;
		}, "ndef lag arg in ndef");

		cW.add(\ndefset, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			("Ndef(" ++ str2.cs ++ ").set(" ++ Ndef(str2).controlKeys[num1-1].cs ++ ", " ++ num2 ++ ");").radcode;
		}, "ndef lag arg in ndef");

		cW.add(\ndefxset, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			("Ndef(" ++ str2.cs ++ ").xset(" ++ str3.cs ++ ", " ++ num1 ++ ");").radcode;
		}, "ndef lag arg in ndef");

		cW.add(\ndefxset, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			("Ndef(" ++ str2.cs ++ ").xset(" ++ Ndef(str2).controlKeys[num1-1].cs ++ ", " ++ num2 ++ ");").radcode;
		}, "ndef lag arg in ndef");

		cW.add(\ndeflag, [\str, \str, \num], {|str1, str2, num1|
			Ndef(str2).controlKeys.do{|item|
				("Ndef(" ++ str2.cs ++ ").lag(" ++ item.cs ++ ", " ++ num1 ++ ");").radcode;
			}
		}, "ndef lag ndef");

		cW.add(\ndeflag, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			("Ndef(" ++ str2.cs ++ ").lag(" ++ str3.cs ++ ", " ++ num1 ++ ");").radcode;
		}, "ndef lag arg in ndef");

		cW.add(\ndeflag, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			("Ndef(" ++ str2.cs ++ ").lag(" ++ Ndef(str2).controlKeys[num1-1].cs ++ ", " ++ num2 ++ ");").radcode;
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
}
