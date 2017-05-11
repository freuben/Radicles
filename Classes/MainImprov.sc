MainImprov {var <>mainPath, <>nodeTime, <server;

*new {
		^super.new.initMainImprov;
	}

	initMainImprov {
		mainPath = ("~/Library/Application Support/SuperCollider/" ++
			"Extensions/ModImprov/").standardizePath;
		server = Server.default;
		nodeTime = 0.08;
	}

}