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

}