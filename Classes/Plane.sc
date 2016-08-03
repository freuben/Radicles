Plane {classvar <planes;

	*new {arg type, subtype, voice, settings;
		^super.new.initNewPlane(type, subtype, voice, settings);
	}

	initNewPlane {arg type, subtype, voice, settings;
		planes = planes.add([type, subtype, voice, settings]);
	}
}

APlane : Plane {classvar <aplanes;

	* add {arg type, settings;
		var voice, aplane;

		case
		{type == \fft} {voice = \poly; aplane = FFTPlane.start(settings) }
		{type == \pitch} {voice = \mono; aplane = PitchPlane.start(settings) }
		{} {};

		this.new(\aplane, type, voice, settings);
		aplanes = aplanes.add(aplane);
		^aplane;
	}

	*remove {

	}

}

FFTPlane : APlane {

	*start {arg settings;
		settings.postln;
	}

}

PitchPlane : APlane {

	*start {arg settings;

		settings.postln;
	}

}

DPlane : Plane {

	* add {arg type, settings;
		var voice;

		case
		{type == \midiFile} { voice = \poly; }
		{type == \midiFileTrack} { voice = \mono };

		this.new(\dplane, type, voice, settings);
	}

}

GPlane : Plane {

	* add {arg type, settings;
		var voice;

		case
		{type == \chain} { voice = \mono }
		{type == \network} { voice = \poly };
		this.new(\gplane, type, voice, settings);
	}

}