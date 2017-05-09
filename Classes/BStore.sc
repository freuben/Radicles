BStore : Store {

	*add {arg type, format=\wav, settings;
		var bstore;

		case
		{type == \play} { bstore = PlayStore.read(settings, format) }
		{type == \sample} { bstore = SampleStore.read(settings, format) }
		{} {};

		if(type.notNil, {
		this.new(\bstore, type, format, settings);
		stores = stores.add(bstore);
		^bstore;
		}, {
			"BStore type not understood".warn;
	});
	}

	*bstores {var resultArr, storeArr;
		storeArr = super.indexArr;
		storeArr.do{|item, index|
			if((item[1][0] == \bstore), {
				resultArr = resultArr.add(item);
			});
		}
		^resultArr;
	}

	*indexArr {var arr, result, bstoreArr;
		arr = this.bstores;
		if(arr.notNil, {
			bstoreArr = this.bstores.flop[1];
			result = ([Array.series(bstoreArr.size)] ++ [bstoreArr]).flop;
		});
		^result;
	}

	*info {var arr;
		arr = this.indexArr;
		if(arr.notNil, {
			arr.postin(\ide, \doln);
		}, {
			"No active bstores".warn;
		});
	}

	*storeIndeces {var arr, resultArr;
		arr = this.bstores;
		if(arr.notNil, {
			resultArr = arr.flop[0];
		});
		^resultArr;
	}

	*removeAt {arg index;
		var arr;
		arr = this.storeIndeces;
		if(arr.notNil, {
			if((index > (arr.size-1)).or(index.isNegative), {
				"Index out of bounds".warn;
			}, {
				super.removeAt(arr[index]);
			});
		}, {
			"No active bstores".warn;
		});
	}

}

PlayStore : BStore {

	*read {arg settings;
		settings.postln;
	}

	*remove {
		"remove play store".postln;
	}

}

SampleStore : BStore {

	*read {arg settings;

		settings.postln;
	}

		*remove {
		"remove sample store".postln;
	}

}