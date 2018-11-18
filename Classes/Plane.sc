Plane : Radicles {classvar <planeIDs, <planes;

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

	*indexArr {var result;
		if(planeIDs.notNil, {
			result = ([Array.series(planeIDs.size)] ++ [planeIDs]).flop;
		});
		^result;
	}

	*info {
		this.indexArr.postin(\ide, \doln);
	}

	*getPlanes {arg type=\aplane;
		var arr, select;
		select = this.planeIDs.flop[0].indicesOfEqual(type);
		select.do{|item| arr = arr.add(planes[item])};
		^arr;
	}

	*getAPlanes {
		^this.getPlanes(\aplane);
	}

	*getGPlanes {
		^this.getPlanes(\gplane);
	}

	*getDPlanes {
		^this.getPlanes(\dplane);
	}
}