+ Collection {

	rejectSame {
		this.do({|item|
			var array;
			array = this.indicesOfEqual(item);
			if(array.size > 1, {
				array.copyRange(1, array.size-1).do({|item,index|
					this.removeAt(item-index);
				});
			});
		});
	}

	busFunc {
		var args, brak1, brak2;
		if(this.size > 1, {
			brak1 = "["; brak2 = "].sum" ;
		}, {
			brak1 = ""; brak2 = "";
		});
		args = "{arg ";
		this.do{|item, index| args = args ++ "vol" ++ (index+1).asString ++ " = -inf, " };
		args = args.replaceAt(";", args.size-2);
		args = args ++ brak1;
		this.do{|item, index|
			args = args ++ ("Ndef.ar(" ++ item.key.cs ++ ", " ++
				item.numChannels ++ ")*vol" ++  (index+1).asString ++ ".dbamp, ";);
		};
		args = args.replaceAt(";", args.size-2);
		args= args ++ brak2 ++ "};"
		^args.interpret;
	}

}

+ ArrayedCollection {

	removeAtAll {arg indexArr;
		var count = 0, removeItem;
		indexArr.do{|item|
			removeItem = item - count;
			this.removeAt(removeItem);
			count = count + 1;
		};
	}

}

