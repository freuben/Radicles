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

}