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