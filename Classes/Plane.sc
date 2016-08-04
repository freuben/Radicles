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