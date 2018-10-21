[ [ 'pan2', {arg pan = 0 -> [-1,1];
	Pan2.ar(\in, pan);
} ], [ 'bal2', {arg pan = 0 -> [-1,1];
	var source;
	source = \in;
	Balance2.ar(source[0], source[1], pan);
} ] ]