MainImprov {var <>mainPath, <>nodeTime, <server, <>postWin, <>postWhere;

*new {
		^super.new.initMainImprov;
	}

	initMainImprov {
		mainPath = ("~/Library/Application Support/SuperCollider/" ++
			"Extensions/ModImprov/").standardizePath;
		server = Server.default;
		nodeTime = 0.08;
		postWhere = \ide;
		postWin = nil;
	}

}