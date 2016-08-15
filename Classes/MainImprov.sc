MainImprov {var <>mainPath, <>nodeTime;

*new {
		^super.new.initMainImprov;
	}

	initMainImprov {
		mainPath = ("~/Library/Application Support/SuperCollider/" ++
			"Extensions/ModImprov/").standardizePath;
		nodeTime = 0.08;
	}

}