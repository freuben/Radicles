HIDMap : Radicles {
	classvar <hidNodes, <hidInfoArr, hidIndex=0, <lagArr;

	*map {arg ndef, key=\freq, type=\midicc, spec=[-1,1], extraArgs, func, mul=1, add=0,
		min, val, warp, lag, action;
		var hidMap, keyVals, defaultVal, inFunc;
		if(spec.isSymbol, {spec = SpecFile.read(\common, spec); });
		if((spec.isArray).and(spec[0].isSymbol), {spec = SpecFile.read(spec[0], spec[1]); });
		if(hidNodes.isNil, {
			hidIndex=0;
		}, {
			if(hidNodes.isEmpty, {
				hidIndex=0;
			});
		});
		keyVals = ndef.getKeysValues;
		defaultVal = keyVals.flop[1][keyVals.flop[0].indexOf(key)];
		spec = spec.specFactor(mul, add, min, val, warp);
		if(action.isNil, {
		inFunc = ("{|val| (\"" ++ ndef.cs ++ ".set(" ++ key.cs ++
			", \" ++ val ++ \")\").radpostcont.interpret; }").interpret;
		}, {
		inFunc = action;
		});
		hidMap = this.getFunc(inFunc, type, spec, extraArgs, func);
		/*hidNodes.do{|item, index| if( [item[1], item[2]] == [ndef, key], {
			(item[0].cs ++ ".clear(" ++ fadeTime.cs ++ ");").radpost.interpret;
			hidNodes.remove(item);
			hidInfoArr.removeAt(index);
		}); };*/
		hidNodes = hidNodes.add([hidMap[0], ndef, key, defaultVal]);
		hidInfoArr = hidInfoArr.add([hidMap[1], type, spec, extraArgs, func, mul, add, min, val, warp, lag]);
		if(lag.notNil, {
			this.lag(ndef, key, lag);
		});
		^hidMap;
	}

	*unmap {arg ndef, key, value;
		var indexNodes, thisArr, num;

		if(key.isInteger, {
			key = ndef.controlKeys[key];
		});
		hidNodes.do{|item, index| if( item.indexOfAll([ndef, key]).reject({|item| item == nil}).size == 2,
			{
				value ?? {value = hidNodes[index][3]};
				(ndef.cs ++ ".set(" ++ key.cs ++ ", " ++ value.cs ++ ");").radpost.interpret;
				(this.getHIDType(hidInfoArr[index][1]).split($.)[0] ++ "(" ++ hidNodes[index][0] ++
				").free;").radpost.interpret;
				hidNodes.removeAt(index);
				hidInfoArr.removeAt(index);
		});
		};
	}

	*unmapAt {arg index;
		if((hidNodes.isNil).or(hidNodes.isEmpty).not, {
			this.unmap(hidNodes[index][1], hidNodes[index][2]);
		}, {
			"no maps left".warn;
		});
	}

	*getHIDType {arg type;
		var funcData;
		case
		{type == \midicc} {funcData = "MIDIdef.cc"}
		{type == \midion} {funcData = "MIDIdef.noteOn"}
		{type == \midioff} {funcData = "MIDIdef.noteOff"}
		{type == \midiptouch} {funcData = "MIDIdef.polytouch"}
		{type == \miditouch} {funcData = "MIDIdef.touch"}
		{type == \midibend} {funcData = "MIDIdef.bend"}
		{type == \midiprogram} {funcData = "MIDIdef.program"}
		{type == \osc} {funcData = "OSCdef"}
		{type == \hidusage} {funcData = "HIDdef.usage"}
		{type == \hidusageID} {funcData = "HIDdef.usageID"}
		{type == \hiddevice} {funcData = "HIDdef.device"}
		{type == \hidelement} {funcData = "HIDdef.element"};
		^funcData;
	}

	*getFunc {arg inFunc={}, type=\midicc, spec=[-1,1], extraArgs, func, inMin, inMax, replaceIndex;
		var hidFuncString, compile, hidString, funcData;
		if(spec.isSymbol, {spec = SpecFile.read(\common, spec); });
		funcData = this.getHIDType(type);
		if([\midicc, \midion, \midioff, \midiptouch, \miditouch, \midibend, \midiprogram].includes(type), {
			inMin ?? {inMin=0};
			inMax ?? {inMax=127};
		}, {
			inMin ?? {inMin=0};
			inMax ?? {inMax=1};
		});
		if(funcData.notNil, {
			if(replaceIndex.isNil, {
				hidIndex = hidIndex + 1;
				hidString = "('hid" ++ hidIndex;
			}, {
				hidString = "('hid" ++ replaceIndex;
			});
			if(func.notNil, {
				if(type == \osc, {
				hidFuncString = "{arg ...args; " ++ inFunc.cs ++ ".(" ++ func.cs ++ ".(" ++ spec.cs
					++ ".asSpec.map(args[0][1].postln.linlin(" ++ inMin ++ ", " ++ inMax ++ ", 0, 1.0))) )}";
				}, {
				hidFuncString = "{arg ...args; " ++ inFunc.cs ++ ".(" ++ func.cs ++ ".(" ++ spec.cs
				++ ".asSpec.map(args[0].linlin(" ++ inMin ++ ", " ++ inMax ++ ", 0, 1.0))) )}";
				});
			}, {
					if(type == \osc, {
				hidFuncString = "{arg ...args; " ++ inFunc.cs ++ ".(" ++ spec.cs ++
					".asSpec.map(args[0][1].postln.linlin(" ++ inMin ++ ", " ++ inMax ++ ", 0, 1.0)) )}";
				}, {
				hidFuncString = "{arg ...args; " ++ inFunc.cs ++ ".(" ++ spec.cs ++
				".asSpec.map(args[0].linlin(" ++ inMin ++ ", " ++ inMax ++ ", 0, 1.0)) )}";
				});
			});
			if(extraArgs.isNil, {
				compile = funcData ++ hidString ++ "', " ++ hidFuncString ++ ");";
			}, {
				if(type == \osc, {
					if(extraArgs.size > 1, {
						extraArgs[1] = ("~" ++ extraArgs[1]);
						extraArgs = extraArgs.collect({|item, index| if(index == 1, {item}, {item.cs}); });
					});
				compile = funcData ++ hidString ++ "', " ++ hidFuncString ++ "," ++
				extraArgs.asString.replace("[", "").replace("]", "") ++ ");";
				}, {
				compile = funcData ++ hidString ++ "', " ++ hidFuncString ++ "," ++
				extraArgs.cs.replace("[", "").replace("]", "") ++ ");";
				});
			});
			compile.radpost.interpret;
			^[(hidString ++ "'").replace("(", ""), compile];
		}, {
			"HID type not Found".warn;
		});
	}

	*lag {arg ndef, key, value;
		var string;
		string = ndef.cs ++ ".lag(" ++ key.cs
		++ ", " ++ value.cs ++ ");";
		string.radpost.interpret;
		lagArr.do{|item| if( [item[0], item[1]] == [ndef, key], {
			lagArr.remove(item);
		}); };
		lagArr = lagArr.add([ndef, key, value]);
	}

	*ndefs {
		^hidNodes.flop[0];
	}

/*	*getPresets {var result;
		if(HIDMap.lagArr.notNil, {
			result = hidNodes.collect{|item, index| [item[0].key.cs, item[0].source.cs,
				item[0].controlKeysValues.cs] ++ [item[1].cs, item[2].cs]
			++ [HIDMap.lagArr[index].collect({|it| it.cs }) ] };
		}, {
			result = hidNodes.collect{|item, index| [item[0].key.cs, item[0].source.cs,
				item[0].controlKeysValues.cs] ++ [item[1].cs, item[2].cs] };
		});
		^result;
	}

	* presetToCode {arg arr, newNdef;
		newNdef ?? {newNdef = arr[3]};
		if(newNdef.isString.not, {newNdef = newNdef.cs});
		^[
			("Ndef(" ++ arr[0] ++ ", " ++ arr[1] ++ ");"),
			("Ndef(" ++ arr[0] ++ ").set(" ++ arr[2].replace("[", "").replace("]", ");");),
			if(arr[5].notNil, {
				(arr[5][0] ++ ".lag(" ++ arr[5][1] ++  ", " ++ arr[5][2] ++ ");");
			}),
			(newNdef ++ ".set(" ++ arr[4] ++ ", " ++ "Ndef(" ++ arr[0] ++ "));";)
		]
	}*/

}