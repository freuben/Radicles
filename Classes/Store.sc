Store : MainImprov {classvar <storeIDs, <stores;

	*new {
		^super.new;
	}

	*store {arg type, subtype, format, settings;
		var return;
		if(storeIDs.notNil, {
		storeIDs.includesEqual([type, subtype, format, settings]).postln;
		if(storeIDs.includesEqual([type, subtype, format, settings]).not, {
		storeIDs = storeIDs.add([type, subtype, format, settings]);
				return = true;
		}, {
				"This store already exists".warn;
				return = false;
		});
		}, {
		storeIDs = storeIDs.add([type, subtype, format, settings]);
			return = true;
		});
		^return;
	}

	*removeAt {arg index;
		stores.removeAt(index);
		storeIDs.removeAt(index);
	}

	*indexArr {var result;
		if(storeIDs.notNil, {
			result = ([Array.series(storeIDs.size)] ++ [storeIDs]).flop;
		});
		^result;
	}

	*info {
		this.indexArr.postin(\ide, \doln);
	}

	*getStores {arg type=\bstore;
		var arr, select;
		select = this.storeIDs.flop[0].indicesOfEqual(type);
		select.do{|item| arr = arr.add(stores[item])};
		^arr;
	}

	 *getBStores {
	 	^this.getStores(\bstore);
	 }

	 *getDStores {
	 	^this.getStores(\dstore);
	 }

	*bstoreIDs {var result;
		storeIDs.do{|item|
			if(item[0] == \bstore, {
				result = result.add([item[1], item[2], item[3]]);
			});
			};
			^result;
	}

		*bstores {var result;
		storeIDs.do{|item, index|
			if(item[0] == \bstore, {
				result = result.add(stores[index]);
			});
			};
			^result;
	}

}

