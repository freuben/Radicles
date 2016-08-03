Plane {classvar <planeIDs, <planes;

	*new {arg type, subtype, voice, settings;
		^super.new.initNewPlane(type, subtype, voice, settings);
	}

	initNewPlane {arg type, subtype, voice, settings;
		planeIDs = planeIDs.add([type, subtype, voice, settings]);
	}

	*removeAt {arg index;
		planes.removeAt(index);
		planeIDs.removeAt(index);
	}

	*info {
		planeIDs.postin(\ide, \doln);
	}

	*indexArr {var result;
		if(planeIDs.notNil, {
			result = ([Array.series(planeIDs.size)] ++ [planeIDs]).flop;
		});
		^result;
	}

}

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

}

DPlane : Plane {

	* add {arg type, settings;
		var voice, dplane;

		case
		{type == \midiFile} { voice = \poly; dplane = "a MIDI File"}
		{type == \midiFileTrack} { voice = \mono; dplane = "a MIDI File Track" };

		this.new(\dplane, type, voice, settings);
		planes = planes.add(dplane);
		^dplane;
	}

*dplanes {var resultArr, planeArr;
		planeArr = super.indexArr;
		planeArr.do{|item, index|
			if((item[1][0] == \dplane), {
				resultArr = resultArr.add(item);
			});
		}
		^resultArr;
	}

	*indexArr {var arr, result, aplaneArr;
		arr = this.dplanes;
		if(arr.notNil, {
			aplaneArr = this.dplanes.flop[1];
			result = ([Array.series(aplaneArr.size)] ++ [aplaneArr]).flop;
		});
		^result;
	}

	*info {var arr;
		arr = this.indexArr;
		if(arr.notNil, {
			arr.postin(\ide, \doln);
		}, {
			"No active DPlanes".warn;
		});
	}

	*planeIndeces {var arr, resultArr;
		arr = this.dplanes;
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
			"No active DPlanes".warn;
		});
	}

}

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