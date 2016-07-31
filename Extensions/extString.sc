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

	fileNameWithoutExtension {
		var myPath, newArray;
		myPath = PathName.new(this);
		myPath.files.do{|item| newArray = newArray.add(item.fileNameWithoutExtension)};
		^newArray
	}

	fileName {
		var myPath, newArray;
		myPath = PathName.new(this);
		myPath.files.do{|item| newArray = newArray.add(item.fileName)};
		^newArray
	}

}
