GPlane : Plane {

	* add {arg type, settings;
		var voice, gplane;

		case
		{type == \chain} { voice = \mono; gplane = "a chain" }
		{type == \network} { voice = \poly; gplane = "a network" };

		this.new(\gplane, type, voice, settings);
		planes = planes.add(gplane);
		^gplane;
	}

	*gplanes {var resultArr, planeArr;
		planeArr = super.indexArr;
		planeArr.do{|item, index|
			if((item[1][0] == \gplane), {
				resultArr = resultArr.add(item);
			});
		}
		^resultArr;
	}

	*indexArr {var arr, result, aplaneArr;
		arr = this.gplanes;
		if(arr.notNil, {
			aplaneArr = this.gplanes.flop[1];
			result = ([Array.series(aplaneArr.size)] ++ [aplaneArr]).flop;
		});
		^result;
	}

	*info {var arr;
		arr = this.indexArr;
		if(arr.notNil, {
			arr.postin(\ide, \doln);
		}, {
			"No active GPlanes".warn;
		});
	}

	*planeIndeces {var arr, resultArr;
		arr = this.gplanes;
		if(arr.notNil, {
			resultArr = arr.flop[0];
		});
		^resultArr;
	}

	*removeAt {arg index;
		var arr;
		arr = this.planeIndeces;
		if(arr.notNil, {
			if((index > (arr.size-1)).or(index.isNegative), {
				"Index out of bounds".warn;
			}, {
				super.removeAt(arr[index]);
			});
		}, {
			"No active GPlanes".warn;
		});
	}

}