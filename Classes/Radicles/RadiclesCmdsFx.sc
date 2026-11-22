// Radicles: call window commands for effects: fx, fxset, fxget, effect modulation, effect and channel-strip presets.
// Part of the Radicles class: the class and its variables are in Radicles.sc.

+ Radicles {
	*loadFxCmds {
		this.prFxAddCmds;
		this.prFxSetCmds;
		this.prFxGetCmds;
		this.prFxModCmds;
		this.prFxPresetCmds;
		this.prFxBypassCmds;
		this.prChannelStripCmds;
	}

	// ---- loading a preset is a transition ("Time in Radicles", 19 Nov 2026) ----
	// fxload, blkload, csload and tload take, after the preset's name and in
	// any order: a time in seconds, a shape (xfade, glide or none) and the
	// options the command already had (nobuf; nosrc, nofx).

	// the type signatures of the words after the preset: up to maxStr words,
	// with or without one number, the number anywhere among them
	*prLoadExtras {|maxStr = 1|
		var list = [];
		(maxStr + 1).do{|k|
			var strs = Array.fill(k, { \str });
			list = list.add(strs);
			(k + 1).do{|pos| list = list.add(strs.keep(pos) ++ [\num] ++ strs.drop(pos)) };
		};
		^list
	}

	// the words after the preset as (time:, shape:, opts: [...]); nil, with
	// a warning, when one of them is not a time, a shape or one of options
	*prLoadWords {|cmd, rest, options|
		var time, shape, opts = [], bad = [];
		options = options ? [];
		(rest ? []).do{|item|
			case
			{ item.isNumber } { time = item }
			{ [\xfade, \glide, \none].includes(item.asSymbol) } { shape = item.asSymbol }
			{ options.includes(item.asSymbol) } { opts = opts.add(item.asSymbol) }
			{ bad = bad.add(item) };
		};
		if(bad.notEmpty, {
			(cmd ++ ": after the preset, a time in seconds, xfade, glide or none" ++
				if(options.notEmpty, { ", " ++ options.join(", ") }, { "" }) ++ "; not " ++ bad.join(", ")).warn;
			^nil
		});
		^(time: time, shape: shape, opts: opts)
	}

	// fxbypass: bypass an effect (Document Model, step 1, PR 4). Without a
	// state it toggles; 1 bypasses, 0 un-bypasses. Same forms as fx rm.
	*prFxBypassCmds {
		var byType, byMix;
		byType = {|str2, num1, num2, state|
			if(aZ.notNil, {
				case
				{str2 == 't'} {aZ.bypassFx(\track, num1, num2, state)}
				{str2 == 'b'} {aZ.bypassFx(\bus, num1, num2, state)}
				{str2 == 'm'} {aZ.bypassFx(\master, 1, num2, state)};
			}, {
				"could not find assemblage".warn;
			});
		};
		byMix = {|num1, num2, state|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num1 <= (trackArr.size), {
					thisArr = trackArr[num1-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					aZ.bypassFx(thisArr[0].asSymbol, thisArr[1], num2, state);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		};
		cW.add(\fxbypass, [\str, \str, \num], {|str1, str2, num1|
			if(str2 == 'm', { byType.(str2, 1, num1) }, { "fxbypass: m slot, or t/b num slot".warn });
		}, "fxbypass: m, slot");
		cW.add(\fxbypass, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			byType.(str2, num1, num2);
		}, "fxbypass: trackType, trackNum, slot");
		cW.add(\fxbypass, [\str, \str, \num, \num, \num], {|str1, str2, num1, num2, num3|
			byType.(str2, num1, num2, num3.asInteger.clip(0, 1));
		}, "fxbypass: trackType, trackNum, slot, state");
		cW.add(\fxbypass, [\str, \num, \num], {|str1, num1, num2|
			byMix.(num1, num2);
		}, "fxbypass: mixTrackNum, slot");
		cW.add(\fxbypass, [\str, \num, \num, \num], {|str1, num1, num2, num3|
			byMix.(num1, num2, num3.asInteger.clip(0, 1));
		}, "fxbypass: mixTrackNum, slot, state");
	}

	// the active effects in the order fxl numbers them: tracks, buses, master,
	// each by number, then slot. Each item is [tag, [type, number, slot], name].
	*fxOrdered {
		var rank = (track: 0, bus: 1, master: 2);
		if(aZ.isNil, { ^[] });
		^(aZ.filters ? []).collect{|item| [item[0], aZ.convFilterTag(item[0]), item[1]] }
		.sort{|a, b|
			var key = {|x| [rank[x[1][0].asSymbol] ? 3, x[1][1].asInteger, x[1][2].asInteger] };
			var ka = key.(a), kb = key.(b);
			var diff = (ka - kb).detect{|x| x != 0 } ? 0;
			diff <= 0
		};
	}

	// the lines fxl posts: the active effects, numbered, each with where it is
	// ("2  track1 slot 2  dl1"); fx rml N removes number N
	*fxList {
		^this.fxOrdered.collect{|item, index|
			var tagArr = item[1];
			var where = if(tagArr[0].asSymbol == \master, { "master" }, { tagArr[0].asString ++ tagArr[1] });
			(index+1).asString ++ "  " ++ where ++ " slot " ++ tagArr[2] ++ "  " ++ item[2];
		};
	}

	// fx, fx rm, fx rml, fxl
	*prFxAddCmds {
		var rmByMix;
		rmByMix = {|num1, num2|
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
		};
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
		//fx rm: remove an effect by trackType and number, or by mixer position;
		//fx rml: by its number in the list of active effects (fxl posts it)
		cW.add(\fx, [\str, \str, \str, \num, \num], {|str1, str2, str3, num1, num2|
			if(str2 == 'rm', {
				if(aZ.notNil, {
					case
					{str3 == 't'} {aZ.setFx(\track, num1, num2, remove: true);}
					{str3 == 'b'} {aZ.setFx(\bus, num1, num2, remove: true);}
					{str3 == 'm'} {aZ.setFx(\master, 1, num2, remove: true);}
					{"Command not Found".warn};
				}, {
					"could not find assemblage".warn;
				});
			}, {
				"Command not Found".warn;
			});
		}, "fx: rm, trackType, trackNum, slot");

		cW.add(\fx, [\str, \str, \str, \num], {|str1, str2, str3, num1|
			if(str2 == 'rm', {
				if(aZ.notNil, {
					case
					{str3 == 'm'} {aZ.setFx(\master, 1, num1, remove: true);}
					{"Command not Found".warn};
				}, {
					"could not find assemblage".warn;
				});
			}, {
				"Command not Found".warn;
			});
		}, "fx: rm, m, slot");

		cW.add(\fx, [\str, \str, \num, \num], {|str1, str2, num1, num2|
			if(str2 == 'rm', {
				rmByMix.(num1, num2);
			}, {
				"Command not Found".warn;
			});
		}, "fx: rm, mixTrackNum, slot");

		cW.add(\fx, [\str, \str, \num], {|str1, str2, num1|
			var filters, filterTagArr;
			case
			{str2 == 'rm'} {rmByMix.(num1, 1);}
			{str2 == 'rml'} {
				if(aZ.notNil, {
					filters = this.fxOrdered;
					if((num1 >= 1) and: { num1 <= filters.size }, {
						filterTagArr = filters[num1-1][1];
						aZ.setFx(filterTagArr[0].asSymbol, filterTagArr[1],
							filterTagArr[2], remove: true);
					}, {
						("No effect number " ++ num1 ++ "; fxl lists them").warn;
					});
				}, {
					"could not find assemblage".warn;
				});
			}
			{"Command not Found".warn};
		}, "fx: ['rm', 'rml'], [mixTrackNum, fxlNum]");

		cW.add(\fxl, [\str], {|str1|
			var lines;
			if(aZ.notNil, {
				lines = this.fxList;
				if(lines.isEmpty, { "no effects".radpostwarn }, { lines.do{|line| line.radpostwarn } });
			}, {
				"could not find assemblage".warn;
			});
		}, "fxl: list the active effects (fx rml N removes number N)");
	}

	// The times an effect's parameters travel over (docs/design/
	// lags-blocks-effects.md). A parameter that can travel is made with its
	// ramp off; these switch it on and give it a time, with nothing rebuilt.
	//   fxlag               posts the effects' lag time
	//   fxlag 4             the effects' lag time: every parameter that is on
	//   fxlag t 1 2 on      every parameter of track 1's effect 2 that can
	//   fxlag t 1 2 off     travel, on (with the effects' lag time) or off
	//   fxlag t 1 2 4       its parameters that are on: 4 s
	//   fxlag t 1 2 freq 4  one parameter (by name or number; with the others)
	//   fxlag t 1 2 exc [freq,amp]   those off; inc: only those on
	//   fxlags t 1 2 [4,1,0,2]       times by position
	//   fxset t 1 2 freq 800 4       a value, and the time of that one move
	// The master has no number (fxlag m 1 on), and a mixer position can stand
	// for the strip (fxlag 1 2 on).
	*prFxLagCmds {
		var strip = {|num, func|
			var trackArr, thisArr;
			if(aZ.notNil, {
				trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
				if(num <= (trackArr.size), {
					thisArr = trackArr[num-1];
					if(thisArr[1].isNil, {thisArr[1] = 1});
					func.(thisArr[0].asSymbol, thisArr[1]);
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		};
		var typed = {|str, num, func|
			if(aZ.notNil, {
				case
				{str == 't'} { func.(\track, num) }
				{str == 'b'} { func.(\bus, num) }
				{str == 'm'} { func.(\master, 1) }
				{ ("track type t, b or m, not " ++ str).warn };
			}, {
				"could not find assemblage".warn;
			});
		};
		cW.add(\fxlag, [\str], {|str1|
			Radicles.fxLag.radpostwarn;
		}, "fxlag: (posts the effects' lag time)");
		cW.add(\fxlag, [\str, \num], {|str1, num1|
			if(aZ.notNil, { aZ.setFxLag(num1) }, { ("Radicles.fxLag = " ++ num1.max(0) ++ ";").radcode });
		}, "fxlag: time (every effect parameter that is on, and what is switched on later)");
		//on, off
		cW.add(\fxlag, [\str, \str, \num, \num, \str], {|str1, str2, num1, num2, str3|
			typed.(str2, num1, {|type, num| aZ.fxTrackLagMode(type, num, num2, str3.asSymbol) });
		}, "fxlag: trackType, trackNum, slot, on|off");
		cW.add(\fxlag, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(str2 == 'm', { typed.(str2, 1, {|type, num| aZ.fxTrackLagMode(type, num, num1, str3.asSymbol) }) }, {
				"fxlag: track, slot, on|off (fxlag t 1 2 on)".warn });
		}, "fxlag: m, slot, on|off");
		cW.add(\fxlag, [\str, \num, \num, \str], {|str1, num1, num2, str2|
			strip.(num1, {|type, num| aZ.fxTrackLagMode(type, num, num2, str2.asSymbol) });
		}, "fxlag: mixTrackNum, slot, on|off");
		//exc, inc
		cW.add(\fxlag, [\str, \str, \num, \num, \str, \arr], {|str1, str2, num1, num2, str3, arr1|
			typed.(str2, num1, {|type, num| aZ.fxTrackLagMode(type, num, num2, str3.asSymbol, arr1) });
		}, "fxlag: trackType, trackNum, slot, exc|inc, [parameters]");
		cW.add(\fxlag, [\str, \str, \num, \str, \arr], {|str1, str2, num1, str3, arr1|
			if(str2 == 'm', { typed.(str2, 1, {|type, num| aZ.fxTrackLagMode(type, num, num1, str3.asSymbol, arr1) }) }, {
				"fxlag: track, slot, exc|inc, [parameters] (fxlag t 1 2 exc [freq])".warn });
		}, "fxlag: m, slot, exc|inc, [parameters]");
		cW.add(\fxlag, [\str, \num, \num, \str, \arr], {|str1, num1, num2, str2, arr1|
			strip.(num1, {|type, num| aZ.fxTrackLagMode(type, num, num2, str2.asSymbol, arr1) });
		}, "fxlag: mixTrackNum, slot, exc|inc, [parameters]");
		//times by position
		cW.add(\fxlags, [\str, \str, \num, \num, \arr], {|str1, str2, num1, num2, arr1|
			typed.(str2, num1, {|type, num| aZ.fxTrackLags(type, num, num2, arr1) });
		}, "fxlags: trackType, trackNum, slot, [times by position]");
		cW.add(\fxlags, [\str, \str, \num, \arr], {|str1, str2, num1, arr1|
			if(str2 == 'm', { typed.(str2, 1, {|type, num| aZ.fxTrackLags(type, num, num1, arr1) }) }, {
				"fxlags: track, slot, [times] (fxlags t 1 2 [4,1])".warn });
		}, "fxlags: m, slot, [times by position]");
		cW.add(\fxlags, [\str, \num, \num, \arr], {|str1, num1, num2, arr1|
			strip.(num1, {|type, num| aZ.fxTrackLags(type, num, num2, arr1) });
		}, "fxlags: mixTrackNum, slot, [times by position]");
		//a value and the time of that one move
		cW.add(\fxset, [\str, \str, \num, \num, \num, \num, \num], {|str1, str2, num1, num2, num3, num4, num5|
			typed.(str2, num1, {|type, num|
				if(type == \master, { "fxset m: slot, parameter, value, time (fxset m 1 mix 0.3 4)".warn }, {
					aZ.setFxArgTrack(type, num, num2, num3, num4, time: num5.max(0)) }) });
		}, "fxset: trackType, trackNum, slot, fxArg, val, time");
		cW.add(\fxset, [\str, \str, \num, \num, \str, \num, \num], {|str1, str2, num1, num2, str3, num3, num4|
			typed.(str2, num1, {|type, num|
				if(type == \master, { "fxset m: slot, parameter, value, time (fxset m 1 mix 0.3 4)".warn }, {
					aZ.setFxArgTrack(type, num, num2, str3, num3, time: num4.max(0)) }) });
		}, "fxset: trackType, trackNum, slot, fxArg, val, time");
		cW.add(\fxset, [\str, \str, \num, \str, \num, \num], {|str1, str2, num1, str3, num2, num3|
			if(str2 == 'm', { typed.(str2, 1, {|type, num| aZ.setFxArgTrack(type, num, num1, str3, num2, time: num3.max(0)) }) }, {
				"fxset: track, slot, parameter, value, time (fxset t 1 2 freq 800 4)".warn });
		}, "fxset: m, slot, fxArg, val, time");
		cW.add(\fxset, [\str, \num, \num, \num, \num, \num], {|str1, num1, num2, num3, num4, num5|
			strip.(num1, {|type, num| aZ.setFxArgTrack(type, num, num2, num3, num4, time: num5.max(0)) });
		}, "fxset: mixTrackNum, slot, fxArg, val, time");
		cW.add(\fxset, [\str, \num, \num, \str, \num, \num], {|str1, num1, num2, str2, num3, num4|
			strip.(num1, {|type, num| aZ.setFxArgTrack(type, num, num2, str2, num3, time: num4.max(0)) });
		}, "fxset: mixTrackNum, slot, fxArg, val, time");
	}

	// fxset, fxlag, setfx, fxsetn, fxlagn, lagfx
	*prFxSetCmds {
		this.prFxLagCmds;
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
				//fxlag m 1 4: the master's effect 1, its parameters that are on
				{str2 == 'm'} {aZ.fxTrackLagMode(\master, 1, num1, num2.max(0));};
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
				//fxlag t 1 2 4: the parameters of track 1's effect 2 that are on
				{str2 == 't'} {aZ.fxTrackLagMode(\track, num1, num2, num3.max(0));}
				{str2 == 'b'} {aZ.fxTrackLagMode(\bus, num1, num2, num3.max(0));}
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
					aZ.fxTrackLagMode(thisArr[0].asSymbol, thisArr[1], num2, num3.max(0));
				}, {
					"track not found".warn;
				});
			}, {
				"could not find assemblage".warn;
			});
		}, "fxlag: mixTrackNum, slot, time (its parameters that are on)");

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
				controlKeys = Assemblage.paramKeys(Ndef(getKey));
				if(controlKeys[num2-1].notNil, {
					[controlKeys[num2-1],
						Assemblage.unitLag(Ndef(getKey), controlKeys[num2-1])].radpostwarn;
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

	// every effect preset as [effect, name], sorted: the new format (one file
	// per preset) and presets saved in the old format, without repeats
	*fxPresetList {
		var list = RadUnit.allPresets(\filter);
		(PresetFile.read(\filter) ? []).do{|item|
			var pair = [PresetFile.read(\filter, item)[0], item];
			if(list.any{|it| it == pair }.not, { list = list.add(pair) });
		};
		^list.sort({ arg a, b; var x = a[0].asString, y = b[0].asString;
			(x < y) or: { (x == y) and: { a[1].asString <= b[1].asString } } })
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
		//fx presets. fxload t 1 2 warm [time] [xfade|glide|none]: the effect
		//crossfades to the preset over its own fade time, or over the time
		//typed; glide: the values glide; none, or a time of 0: at once
		Radicles.prLoadExtras(1).do{|extra|
			var post = " [time] [xfade|glide|none]";
			//slot 1 when no slot is given (on the master the number is the slot)
			cW.add(\fxload, [\str, \str, \num, \str] ++ extra, {|str1, str2, num1, str3 ... rest|
				var words = Radicles.prLoadWords("fxload", rest);
				case
				{ words.isNil } { nil }
				{ aZ.isNil } { "could not find assemblage".warn }
				{str2 == 't'} {aZ.loadFxTrackPreset(\track, num1, 1, str3, words[\time], words[\shape]);}
				{str2 == 'b'} {aZ.loadFxTrackPreset(\bus, num1, 1, str3, words[\time], words[\shape]);}
				{str2 == 'm'} {aZ.loadFxTrackPreset(\master, 1, num1, str3, words[\time], words[\shape]);};
			}, "fxload: trackType, slot, preset" ++ post);

			cW.add(\fxload, [\str, \str, \num, \num, \str] ++ extra, {|str1, str2, num1, num2, str3 ... rest|
				var words = Radicles.prLoadWords("fxload", rest);
				case
				{ words.isNil } { nil }
				{ aZ.isNil } { "could not find assemblage".warn }
				{str2 == 't'} {aZ.loadFxTrackPreset(\track, num1, num2, str3, words[\time], words[\shape]);}
				{str2 == 'b'} {aZ.loadFxTrackPreset(\bus, num1, num2, str3, words[\time], words[\shape]);}
				{str2 == 'm'} {aZ.loadFxTrackPreset(\master, 1, num2, str3, words[\time], words[\shape]);};
			}, "fxload: trackType, trackNum, slot, preset" ++ post);

			cW.add(\fxload, [\str, \num, \str] ++ extra, {|str1, num1, str2 ... rest|
				var trackArr, thisArr, words = Radicles.prLoadWords("fxload", rest);
				case
				{ words.isNil } { nil }
				{ aZ.isNil } { "could not find assemblage".warn }
				{
					trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
					if(num1 <= (trackArr.size), {
						thisArr = trackArr[num1-1];
						if(thisArr[1].isNil, {thisArr[1] = 1});
						aZ.loadFxTrackPreset(thisArr[0].asSymbol, thisArr[1], 1, str2, words[\time], words[\shape]);
					}, {
						"track not found".warn;
					});
				};
			}, "fxload: mixTrackNum, preset (slot 1)" ++ post);

			cW.add(\fxload, [\str, \num, \num, \str] ++ extra, {|str1, num1, num2, str2 ... rest|
				var trackArr, thisArr, words = Radicles.prLoadWords("fxload", rest);
				case
				{ words.isNil } { nil }
				{ aZ.isNil } { "could not find assemblage".warn }
				{
					trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr};
					if(num1 <= (trackArr.size), {
						thisArr = trackArr[num1-1];
						if(thisArr[1].isNil, {thisArr[1] = 1});
						aZ.loadFxTrackPreset(thisArr[0].asSymbol, thisArr[1], num2, str2, words[\time], words[\shape]);
					}, {
						"track not found".warn;
					});
				};
			}, "fxload: mixTrackNum, slot, preset" ++ post);
		};

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

		//fxsave <effect> <preset> <library>: copy one of your presets into the
		//effect's library, to share it with the library
		cW.add(\fxsave, [\str, \str, \str, \str], {|str1, str2, str3, str4|
			RadUnit.publishPreset(\filter, str2, str3, str4);
		}, "fxsave: effect, preset, library (copies your preset into the library)");

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
			this.fxPresetList.dopostln;
		}, "fxload: prints all fx presets that can be loaded");

		cW.add(\fxload, [\str, \str], {|str1, str2|
			this.fxPresetList.select({|item| item[0] == str2 }).dopostln;
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
		var stripOf, mixOf, save, load;
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

		//effect-chain presets (Document Model, step 3, PR 2: RadUnitChains.sc).
		//csload / csload t lists them; cssave t 1 name; csload t 1 name|default
		//[time] [xfade|glide|none]: the chain crossfades to the preset over
		//the strip's own fade time, or the time typed (none, or 0: at once);
		//t 1 can be b 1, m, or a mixer position (csload 3 name)
		stripOf = {|str2, num1|
			case
			{str2 == 't'} {[\track, num1]}
			{str2 == 'b'} {[\bus, num1]}
			{str2 == 'm'} {[\master, 1]}
			{ ("csload/cssave: t, b or m, not " ++ str2).warn; nil };
		};
		mixOf = {|num1|
			var trackArr = aZ.mixTrackNames.collect{|item| item.asString.divNumStr}, thisArr;
			if(num1 <= trackArr.size and: { num1 >= 1 }, {
				thisArr = trackArr[num1-1];
				[thisArr[0].asSymbol, thisArr[1] ? 1]
			}, { "track not found".warn; nil });
		};
		save = {|strip, name| strip !? { RadUnit.saveChainPreset(strip[0], strip[1], name, az: aZ) } };
		load = {|strip, name, rest|
			var words = Radicles.prLoadWords("csload", rest);
			if(strip.notNil and: { words.notNil }, {
				RadUnit.loadChainPreset(strip[0], strip[1], name, az: aZ, time: words[\time], shape: words[\shape]) });
		};

		cW.add(\csload, [\str], {|str1|
			RadUnit.postChainPresets;
		}, "csload: lists chain presets");

		cW.add(\csload, [\str, \str], {|str1, str2|
			RadUnit.postChainPresets;
		}, "csload: [trackType] lists chain presets");

		Radicles.prLoadExtras(1).do{|extra|
			var post = " [time] [xfade|glide|none]";
			cW.add(\csload, [\str, \str, \str] ++ extra, {|str1, str2, str3 ... rest|
				if(aZ.notNil, { load.(stripOf.(str2, 1), str3, rest) }, { "could not find assemblage".warn });
			}, "csload: trackType, presetName" ++ post);

			cW.add(\csload, [\str, \str, \num, \str] ++ extra, {|str1, str2, num1, str3 ... rest|
				if(aZ.notNil, { load.(stripOf.(str2, num1), str3, rest) }, { "could not find assemblage".warn });
			}, "csload: trackType, trackNum, presetName" ++ post);

			cW.add(\csload, [\str, \num, \str] ++ extra, {|str1, num1, str2 ... rest|
				if(aZ.notNil, { load.(mixOf.(num1), str2, rest) }, { "could not find assemblage".warn });
			}, "csload: mixTrackNum, preset" ++ post);
		};

		cW.add(\cssave, [\str, \str, \str], {|str1, str2, str3|
			if(aZ.notNil, { save.(stripOf.(str2, 1), str3) }, { "could not find assemblage".warn });
		}, "cssave: [trackType, presetName]");

		cW.add(\cssave, [\str, \str, \num, \str], {|str1, str2, num1, str3|
			if(aZ.notNil, { save.(stripOf.(str2, num1), str3) }, { "could not find assemblage".warn });
		}, "cssave: [trackType, trackNum, presetName]");

		cW.add(\cssave, [\str, \num, \str], {|str1, num1, str2|
			if(aZ.notNil, { save.(mixOf.(num1), str2) }, { "could not find assemblage".warn });
		}, "cssave: mixTrackNum, preset");

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
