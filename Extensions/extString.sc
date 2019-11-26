//from crucialLib

+ Object {
	loadPath {}
	enpath {}
	loadDocument {}
	didLoadFromPath {}
}

+ String {
	loadPath { arg warnIfNotFound=true;
		var obj,path;
		path = this.standardizePath;
		if(File.exists(path),{
			{
				obj = thisProcess.interpreter.executeFile(path);
				obj.didLoadFromPath(this);
			}.try({ arg err;
				("In file: " + this).postln;
				err.throw;
			});
		},{
			if(warnIfNotFound,{
				warn("String:loadPath file not found " + this + path);
				^nil
			});
		});
		if(obj.isNil and: warnIfNotFound, {
			warn("String:loadPath found nil, empty contents or parse error in " + path);

			//^ObjectNotFound.new(path)
		});
		^obj
	}
	loadDocument { arg warnIfNotFound=true;
		var path,obj;
		path = Document.standardizePath(this);
		if(File.exists(path),{
			obj = thisProcess.interpreter.executeFile(path);
			obj.didLoadFromPath(path);
			^obj
		},{
			if(warnIfNotFound,{
				warn("String:loadDocument file not found " + this + path);
			});
			^ObjectNotFound.new(path)
		});
	}
	enpath {
		^Document.abrevPath(this)
	}

	guiDocument {
		var doc;
		doc = this.loadDocument;
		doc.gui;
		^doc
	}

	isStringNumber {
		var bol, item;
		item = this;
		if(item.contains("."), {item = item.replace(".")});
		bol = "1234567890".ascii.includesAll(item.ascii);
		^bol;
	}

	lineFormat {arg line = 73;
		var newString, returns, count, countThis;
		newString = this;
		returns = newString.findAll(10.asAscii);
		count = 0;
		countThis = 1;
		newString.do{|item, index|
			if(returns.notNil, {
				if(returns.includes(index), {
					countThis = 0;
				});
			});
			if((countThis > line), {
				if(item.asString == " ", {
					countThis = 0;
					newString = newString.insert(index+count, 10.asAscii.asString);
					count = count + 1;
				});
			});
			countThis = countThis + 1;
		};
		^newString;
	}

	lineFormat2 {arg chars=82;
		var newString, return, spaces, largeInd, snapShot, returnIndex;
		var size=2, ind=0, string = "", end = true;
		if(this.size > chars, {
			newString = this.replace([32,10].asAscii, 10.asAscii);
			while ({ end }, {
				if(ind == 0, {return=""; ind=1}, {return=10.asAscii});
				spaces = newString.findAll(" ");
				if(spaces.notNil, {
					largeInd = spaces[spaces.indexInBetween(chars)-1];
					if(largeInd == 0, {
						largeInd = newString.size-1;
						return="";
					});
					if(largeInd.isNil, {
						largeInd = newString.size-1;
					});
					snapShot = newString.copyRange(0, largeInd);
					if(snapShot.includes(10.asAscii).not, {
						string = string ++ return ++ snapShot;
						newString = newString.copyRange(largeInd, newString.size-1);
					}, {
						returnIndex = snapShot.find(10.asAscii)+1;
						snapShot = snapShot.copyRange(0, returnIndex-1);
						string = string ++ return ++ snapShot.replace(10.asAscii, "");
						newString = newString.copyRange(returnIndex, newString.size-1);
					});
				}, {
					if(size <= 1, {
						string = string ++ return ++ newString.replace(10.asAscii, "");
					});
					end = false;
				});
			});
		}, {
			string = this;
		});
		^string
	}

	capitalise {
		var string, capital, ascii;
		string = this;
		capital = string[0].toUpper.ascii;
		ascii = string.ascii;
		ascii[0] = capital;
		^ascii.asAscii;
	}

	includesString {arg string;
		var out = this.findAll(string).size > 0;
		^out;
	}

	squareToRound {
		^this.replace("[", "(").replace("]", ")");
	}

	roundToSquare {
		^this.replace("(", "[").replace(")", "]");
	}

	divNumStr {var nums;
		nums = this.select{|item| (48..57).includes(item.ascii) };
		^[this.replace(nums, ""), nums.interpret];
	}

	interpretRad {var str, str2, arrStr, arrStr2;
		str = this;
		str2 = str.select{|item|
		("0123456789[](){},.".ascii).includes(item.ascii).not;};
		arrStr = str2.split($ ).reject({|item| item == "" });
		arrStr2 = arrStr.do{|item|  str = str.replace(item, item.asSymbol.cs) };
		^str.interpret;
	}

}
