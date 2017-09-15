MainImprov {classvar <>mainPath, <>nodeTime=0.08, <server, <>postWin=nil, <>postWhere=\ide, <>fadeTime=0.5;

*new {
		^super.new.initMainImprov;
	}

	initMainImprov {
		mainPath = ("~/Library/Application Support/SuperCollider/" ++
			"Extensions/ModImprov/").standardizePath;
		server = Server.default;
	}

}