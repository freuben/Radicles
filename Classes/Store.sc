Store : MainImprov {classvar <storeIDs, <stores;

	*new {arg type, subtype, format, settings;
		^super.new.initNewPlane(type, subtype, format, settings);
	}

	initNewPlane {arg type, subtype, format, settings;
		storeIDs = storeIDs.add([type, subtype, format, settings]);
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

}

