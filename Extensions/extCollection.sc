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
		//each sender's level moves as a steady fader move, like a strip's
		//volume (Assemblage:addTrack): volN is the level, rampN the time of
		//the move under way, lagN the send's own time for typed values (sndlag)
		this.do{|item, index| var n = (index+1).asString;
			args = args ++ "vol" ++ n ++ " = -inf, lag" ++ n ++ " = 0.05, ramp" ++ n ++ " = 0.05, " };
		args = args.replaceAt(";", args.size-2);
		args = args ++ brak1;
		this.do{|item, index|
			args = args ++ ("Ndef.ar(" ++ item.key.cs ++ ", " ++
				item.numChannels ++ ")*(VarLag.kr((vol" ++ (index+1).asString ++ ".dbamp / 6.dbamp).sqrt, ramp" ++ (index+1).asString
				++ ".max(ControlDur.ir), warp: \\lin).squared * 6.dbamp), ";);
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

