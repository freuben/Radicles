Plane {classvar <planeIDs, <planes;

	*new {arg type, subtype, voice, settings;
		^super.new.initNewPlane(type, subtype, voice, settings);
	}

	initNewPlane {arg type, subtype, voice, settings;
		planeIDs = planeIDs.add([type, subtype, voice, settings]);
	}

	*removeAt {arg index;
		planes[index].remove;
		planes.removeAt(index);
		planeIDs.removeAt(index);
	}

	*info {
		this.indexArr.postin(\ide, \doln);
	}

	*indexArr {var result;
		result = ([Array.series(planeIDs.size)] ++ [planeIDs]).flop;
		^result;
	}

}

APlane : Plane {
	classvar <aplanes;

	* add {arg type, settings;
		var voice, aplane;

		case
		{type == \fft} {voice = \poly; aplane = FFTPlane.start(settings) }
		{type == \pitch} {voice = \mono; aplane = PitchPlane.start(settings) }
		{} {};

		this.new(\aplane, type, voice, settings);
		planes = planes.add(aplane);
		^aplane;
	}

	* info {var planeArr, resultArr;

		planeArr = this.indexArr;

		planeArr.do{|item, index|
			if((item[1][0] == \aplane), {
				resultArr = resultArr.add(item);
			});
		}

		^resultArr.postin(\ide, \doln);
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

}