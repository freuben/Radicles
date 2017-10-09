+ Function {

	patternToPdef {
		var cs, patType, argNames, part2, part1, fcs;
		cs = this.cs;
		part2 = cs.copyRange(cs.find("Pbind"), cs.size);
		part1 = cs.copyRange(0, cs.find("Pbind")-1);
		argNames = this.argNames;
		patType = Pseq;
		argNames.do{|item|
			var string;
			string = ("Pdefn('" ++ argNames[0] ++ "_" ++ item ++ "', " ++ patType.asString ++ "(" ++ item ++ ", inf))").asString;
			part2 = part2.replace(item.asString, string);
		};
		fcs = part1 ++ "Pdef(key).quant = quant;" ++ 10.asAscii ++ "Pdef(key, " ++ part2;
		fcs = fcs.replace("}", ");}");
		^fcs.interpret;
	}

	toSynthDef {arg key=\bla, post=true;
		var arr, arr2, string, string2, synthDef;
		this.argNames.do{|item| arr = arr.add(item) };
		string = arr.asString.replace("[", "").replace("]", "");
		arr.do{|item, index| arr2 = arr2.add(item ++ "=" ++ this.defaultArgs[index].cs) };
		string2 = arr2.asString.replace("[", "").replace("]", "");
		synthDef = "SynthDef(" ++ key.cs ++ ", {arg out=0," ++ string2 ++ "; var output;" ++
		10.asAscii ++ "output = Out.ar(out, " ++ this.cs ++ ".(" ++ string ++ "))});";
		if(post, {synthDef.postln});
		^synthDef.interpret;
	}

}