[ [ 'pan2', {arg pan;
	Pan2.ar(\in.ar([0]), pan);
} ], [ 'panB', {arg pan1, pan2, chanNum;
	var  w, x, y, z, sig;
	sig = \in.ar([0]);
	#w, x, y, z = PanB.ar(sig, pan1, pan2);
	sig = DecodeB2.ar(4, w, x, y);
} ], [ 'split', {arg pan;
	var sig;
	sig = \in.ar([0]);
	sig = Out.ar(pan, sig);
} ] ]