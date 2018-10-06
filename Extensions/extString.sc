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

	lineFormat {arg chars=82;
		var newString, return, spaces, largeInd, snapShot, returnIndex;
		var ind=0, string = "", end = true;
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
					snapShot = snapShot.copyRange(0, returnIndex);
					string = string ++ return ++ snapShot.replace(10.asAscii, "");
					newString = newString.copyRange(returnIndex, newString.size-1);
				});
			}, {
				end = false;
			});
		});
		^string
	}

	lineFormat2 {arg line = 73;
		var newString, returns, count, countThis;
		newString = this;
		returns = newString.findAll(10.asAscii);
		count = 0;
		countThis = 1;
		newString.do{|item, index|
			if(returns.includes(index), {
				countThis = 0;
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

}
