+ Array {

	*panDis {arg size, maxSize;
		var frac, list, arr1, arr2, final;
		if(maxSize.isNumber, {
			frac = 1/(maxSize+1);
			list = Array.series(maxSize, 1, 1);
			arr1 = list/ (maxSize+1) * 2 - 1;
		}, {
			arr1 = [-1, 1];
		});
		frac = 1/(size+1);
		list = Array.series(size, 1, 1);
		arr2 = list/ (size+1) * 2 - 1;
		final = arr2.linlin(arr1.first, arr1.last, -1, 1).round(0.0000001);
		^final;
	}


		funcSpec {var func, spec, specFunc;
		if( this.collect({|item| item.isFunction }).includes(true), {
			func = this.select({|item| item.isFunction })[0];
			spec = this.reject({|item| item.isFunction }).asSpec;
		}, {
			spec = this.asSpec;
		}
		);
		if(func.isNil, {
			specFunc = {arg val=0; spec.map(val) };
		}, {
			specFunc = {arg val=0; func.value(spec.map(val) )  };
		});
		^specFunc;
	}

}

