APlane : Plane {

	*add {arg type, settings;
		var voice, aplane;

		case
		{type == \fft} {voice = \poly; aplane = FFTPlane.start(settings) }
		{type == \pitch} {voice = \mono; aplane = PitchPlane.start(settings) }
		{} {};

		this.new(\aplane, type, voice, settings);
		planes = planes.add(aplane);
		^aplane;
	}

	*aplanes {var resultArr, planeArr;
		planeArr = super.indexArr;
		planeArr.do{|item, index|
			if((item[1][0] == \aplane), {
				resultArr = resultArr.add(item);
			});
		}
		^resultArr;
	}

	*indexArr {var arr, result, aplaneArr;
		arr = this.aplanes;
		if(arr.notNil, {
			aplaneArr = this.aplanes.flop[1];
			result = ([Array.series(aplaneArr.size)] ++ [aplaneArr]).flop;
		});
		^result;
	}

	*info {var arr;
		arr = this.indexArr;
		if(arr.notNil, {
			arr.postin(\ide, \doln);
		}, {
			"No active APlanes".warn;
		});
	}

	*planeIndeces {var arr, resultArr;
		arr = this.aplanes;
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
			"No active APlanes".warn;
		});
	}

}

FFTPlane : APlane {

	*start {arg settings;
		settings.postln;
	}

	*remove {
		"remove FFT plane".postln;
	}

}

PitchPlane : APlane {

	*start {arg settings;

		settings.postln;
	}

		*remove {
		"remove Pitch plane".postln;
	}

}