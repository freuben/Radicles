[ [ 'pan2', {arg pan = 0 -> [-1,1];
	Pan2.ar(\in, pan) * 3.dbamp;
} ], [ 'bal2', {arg pan = 0 -> [-1,1];
	var source;
	source = \in;
	Balance2.ar(source[0], source[1], pan) * 3.dbamp;
} ], [ 'pan4', {arg panx = 0 -> [-1,1], pany = 0 -> [-1, 1];
	Pan4.ar(\in, panx, pany);
} ], [ 'panAz3', {arg pan = 0 -> [-1,1], width=2->[1,4], orientation=0.5->[0,3];
	PanAz.ar(3, \in, pan, 1, width, orientation);
} ], [ 'panAz5', {arg pan = 0 -> [-1,1], width=2->[1,6], orientation=0.5->[0,5];
	PanAz.ar(5, \in, pan, 1, width, orientation);
} ], [ 'panAz6', {arg pan = 0 -> [-1,1], width=2->[1,7], orientation=0.5->[0,6];
	PanAz.ar(6, \in, pan, 1, width, orientation);
} ], [ 'split', {arg pan=0 -> [0,8,{|val| val.round(1)}];
	var sig;
	sig = \in;
	sig = Out.ar(pan, sig);
} ], [ 'linpan2', {arg pan=0 -> [-1,1];
    LinPan2.ar(\in, pan);
} ], [ 'panB', {arg pan1=0 -> [-pi, pi], pan2=0 -> [-0.5pi,0.5pi];
	var  w, x, y, z, sig;
	sig = \in;
	#w, x, y, z = PanB.ar(sig, pan1, pan2);
	sig = DecodeB2.ar(\num, w, x, y);
} ], [ 'dir', {arg dirmul=1->[0.5,2];
	var  sig;
	sig = \in;
	sig*dirmul;
} ], [ 'panAz8', {arg pan = 0 -> [-1,1], width=2->[1,9], orientation=0.5->[0,8];
	PanAz.ar(8, \in, pan, 1, width, orientation);
} ], [ 'panAz7', {arg pan = 0 -> [-1,1], width=2->[1,8], orientation=0.5->[0,7];
	PanAz.ar(7, \in, pan, 1, width, orientation);
} ], [ 'panAz4', {arg pan = 0 -> [-1,1], width=2->[1,5], orientation=0.5->[0,4];
	PanAz.ar(4, \in, pan, 1, width, orientation);
} ], [ 'rymer1', {arg azimuth = 0 -> [-0.5pi,0.5pi], elevation = 0 -> [-0.5pi,0.5pi], rho = 1 -> [0,8];
	var w, x, y, z, rymerSpeakers;
	rymerSpeakers = [ [25, 0], [-25, 0], [-60, 0], [-110, 0], [-160, 0], [160, 0], [110, 0], [60, 0], [-40, 40], [-120, 40], [120, 40], [40, 40], [-25, -40], [-130, -40], [130, -40], [25, -40] ];
	#w, x, y, z = BFEncode1.ar(\in, azimuth, elevation, rho);
  BFDecode1.ar(w, x, y, z, rymerSpeakers.flop[0], rymerSpeakers.flop[1]);
} ], [ 'rymer2', {arg panx = 0 -> [-1,1], pany = 0 -> [-1,1], elevation = 0 -> [-0.5pi,0.5pi];
	var w, x, y, z, rymerSpeakers;
	rymerSpeakers = [ [25, 0], [-25, 0], [-60, 0], [-110, 0], [-160, 0], [160, 0], [110, 0], [60, 0], [-40, 40], [-120, 40], [120, 40], [40, 40], [-25, -40], [-130, -40], [130, -40], [25, -40] ];
	#w, x, y, z = BFEncode2.ar(\in, panx, pany, elevation);
  BFDecode1.ar(w, x, y, z, rymerSpeakers.flop[0], rymerSpeakers.flop[1]);
} ] ]