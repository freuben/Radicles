+ Symbol {

	spaceToChans {
		var chanNum, spaceType;
		spaceType = this;
		case
		{spaceType == \pan2} {chanNum = 2}
		{spaceType == \bal2} {chanNum = 2}
		{spaceType == \panAz3} {chanNum = 3}
		{spaceType == \pan4} {chanNum = 4}
		{spaceType == \panAz5} {chanNum = 5}
		{spaceType == \panAz6} {chanNum = 6}
		{spaceType == \panAz7} {chanNum = 7}
		{spaceType == \panAz8} {chanNum = 8}
		{spaceType == \rymer1} {chanNum = 16}
		{spaceType == \rymer2} {chanNum = 16}
		;
		^chanNum;
	}

}