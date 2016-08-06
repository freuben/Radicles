MainImprov {var <>mainPath;

*new {
		^super.new.initMainImprov;
	}

	initMainImprov {
		mainPath = ("~/Library/Application Support/SuperCollider/" ++
			"Extensions/ModImprov/").standardizePath;
	}

}