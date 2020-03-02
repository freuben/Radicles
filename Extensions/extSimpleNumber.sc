+ SimpleNumber {

	spaceType {
		var spaceType, chanNum;
		chanNum = this;
		case
		{chanNum == 1} {spaceType = \pan2}
		{chanNum == 2} {spaceType = \bal2}
		{chanNum == 3} {spaceType = \panAz3}
		{chanNum == 4} {spaceType = \pan4}
		{chanNum == 5} {spaceType = \panAz5}
		{chanNum == 6} {spaceType = \panAz6}
		{chanNum == 7} {spaceType = \panAz7}
		{chanNum == 8} {spaceType = \panAz8}
		{chanNum == 16} {spaceType =\rymer1}
		{chanNum == 18} {spaceType =\rymer2}
		;
		^spaceType;
	}

}