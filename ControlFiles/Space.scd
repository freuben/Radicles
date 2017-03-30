[ [ 'pan2', {arg pan;
	Pan2.ar(\in, pan);
} ], [ 'linpan2', {arg pan;
	LinPan2.ar(\in, pan);
} ], [ 'panB', {arg arr; var a,b;
a = Array.interpolation(arr.size,-0.5,0.5).clump(arr.size/2);
a.do({|item| b = b.add(item.reverse) });
[Array.panDis(arr.size, arr.size)*pi, b.flat * pi].flop;
} ], [ 'split', {arg pan;
	var sig;
	sig = \in;
	sig = Out.ar(pan, sig);
} ] ]