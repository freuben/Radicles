[ [ 'pan2', {arg pan = 0 ;
	Pan2.ar(\in, pan);
} ], [ 'panB', {arg pan1, pan2;
	var  w, x, y, z, sig;
	sig = \in;
	#w, x, y, z = PanB.ar(sig, pan1, pan2);
	sig = DecodeB2.ar(\num, w, x, y);
} ], [ 'split', {arg pan;
	var sig;
	sig = \in;
	sig = Out.ar(pan, sig);
} ], [ 'linpan2', {arg pan;
    LinPan2.ar(\in, pan);
} ] ]