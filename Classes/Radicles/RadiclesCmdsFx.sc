// Radicles: call window commands for effects: fx, fxset, fxget, effect modulation, effect and channel-strip presets.
// Part of the Radicles class: the class and its variables are in Radicles.sc.

+ Radicles {
	*loadFxCmds {
		this.prFxAddCmds;
		this.prFxSetCmds;
		this.prFxGetCmds;
		this.prFxModCmds;
		this.prFxPresetCmds;
		this.prChannelStripCmds;
	}

	// fx, fxclear, clearfx
	*prFxAddCmds {
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
	}

	// fxset, fxlag, setfx, fxsetn, fxlagn, lagfx
	*prFxSetCmds {
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
	}

	// fxget, fxgetlag, getfx, getlagfx, fxgetspec, getfxspec
	*prFxGetCmds {
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
	}

	// fxset/setfx with a modulator, modfxset, modsetfx, unmodfx, fxunmod
	*prFxModCmds {
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
	}

	// fxload, loadfx, fxsave, savefx, fxlags, lagsfx
	*prFxPresetCmds {
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
	}

	// csload, cssave, csfree
	*prChannelStripCmds {
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
}
