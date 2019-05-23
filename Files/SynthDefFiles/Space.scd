[ [ 'pan2', {arg pan = 0 -> [-1,1];
	Pan2.ar(\in, pan) * 3.dbamp;
} ], [ 'bal2', {arg pan = 0 -> [-1,1];
	var source;
	source = \in;
	Balance2.ar(source[0], source[1], pan) * 3.dbamp;
} ], [ 'pan4', {arg panx = 0 -> [-1,1], pany = 0 -> [-1, 1];
	Pan4.ar(\in, panx, pany);
} ], [ 'panAz3', {arg pan = 0 -> [-1,1];
	PanAz.ar(3, \in, pan);
} ], [ 'panAz5', {arg pan = 0 -> [-1,1];
	PanAz.ar(5, \in, pan);
} ], [ 'panAz6', {arg pan = 0 -> [-1,1];
	PanAz.ar(6, \in, pan);
} ] ]