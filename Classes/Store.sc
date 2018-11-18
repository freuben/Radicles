Store : Radicles {classvar <storeIDs, <stores;

	*store {arg type, subtype, format, settings;
		var return;
		if(storeIDs.notNil, {
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
		this.indexArr.radpost(\doln);
	}

	*bstoreIDs {var result, bufArgs;
		storeIDs.do{|item|
			if(item[0] == \bstore, {
				if(item[3].isNumber, {
					bufArgs = [item[3], 1];
				}, {
					bufArgs = item[3];
				});
				result = result.add([item[1], item[2], bufArgs]);
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

	*removeStores {
		storeIDs = nil;
		stores = nil;
		BufferSystem.freeAll;
	}

	*removeBStores {
		var arr, count=0;
		storeIDs.do{|item, index|
			if(item[0] == \bstore, {
				arr= arr.add(index);
			});
		};
		arr.do{|item|
			storeIDs.removeAt(item-count);
			stores.removeAt(item-count);
			count = count+1;
		};
		BufferSystem.freeAll;
	}

	*removeDStores {
		var arr, count=0;
		storeIDs.do{|item, index|
			if(item[0] == \dstore, {
				arr= arr.add(index);
			});
		};
		arr.do{|item|
			storeIDs.removeAt(item-count);
			stores.removeAt(item-count);
			count = count+1;
		};
	}

	*dstoreIDs {var result;
		storeIDs.do{|item|
			if(item[0] == \dstore, {
				result = result.add([item[1], item[2], item[3]]);
			});
		};
		^result;
	}

	*dstores {var result;
		storeIDs.do{|item, index|
			if(item[0] == \dstore, {
				result = result.add(stores[index]);
			});
		};
		^result;
	}

	*savePreset {arg name;
		PresetFile.write(\store, name, storeIDs);
	}

	*loadPreset {arg name, function;
		var storeArr, bstoreArr, dstoreArr;
		this.removeStores;
		storeArr = PresetFile.read(\store, name);
		storeArr.do{|item|
			case
			{item[0] == \bstore} {
				bstoreArr = bstoreArr.add([item[1], item[2], item[3]]);
			}
			{item[0] == \dstore} {
				dstoreArr = dstoreArr.add([item[1], item[2], item[3]]);
			};
		};

		if(bstoreArr.notNil, {
			BStore.addAll(bstoreArr.radpost, function);
		});
		if(dstoreArr.notNil, {
			"this is a DStrore preset".postln;
			DStore.addAll(dstoreArr);
		});
	}

	*saveBPreset {arg name;
		PresetFile.write(\bstore, name, this.bstoreIDs);
	}

	*loadBPreset {arg name, function;
		this.removeBStores;
		BStore.addAll(PresetFile.read(\bstore, name), function);
	}

	*saveDPreset {arg name;
		PresetFile.write(\dstore, name, this.dstoreIDs);
	}

	*loadDPreset {arg name;
		PresetFile.read(\dstore, name).radpost;
	}

}

