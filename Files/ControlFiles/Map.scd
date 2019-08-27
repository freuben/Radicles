[ [ 'sin', {arg freq=0.1, phase=0, mul=1, add=0; 
	SinOsc.ar(freq, phase, mul).map + add} ], [ 'saw', {arg freq=0.1, mul=1, add=0; 
	Saw.ar(freq, mul).map + add } ], [ 'lfn2', {arg freq=0.1, mul=1, add=0; 
	LFNoise2.ar(freq).map + add } ], [ 'lfn1', {arg freq=0.1, mul=1, add=0; 
	LFNoise1.ar(freq).map + add } ], [ 'lfn0', {arg freq=0.1, mul=1, add=0; 
	LFNoise0.ar(freq).map + add } ], [ 'cspl', {arg freq=0.1, a=1, b=1.99, xi=0, mul=1, add=0; 
	CuspN.ar(freq,a,b,xi, mul).map + add} ], [ 'cspn', {arg freq=0.1, a=1, b=1.99, xi=0; CuspN.ar(freq,a,b,xi).map } ], [ 'henn', {arg freq=0.1, a=1.4, b=0.3, x0=0, x1=0, mul=1, add=0; HenonN.ar(freq,a,b,x0,x1,mul).map + add } ], [ 'henl', {arg freq=0.1, a=1.4, b=0.3, x0=0, x1=0, mul=1, add=0; HenonL.ar(freq,a,b,x0,x1,mul).map + add } ], [ 'henc', {arg freq=0.1, a=1.4, b=0.3, x0=0, x1=0, mul=1, add=0; HenonC.ar(freq,a,b,x0,x1,mul).map + add } ], [ 'lfp', {arg freq=0.1, iphase=0, width=0.5, mul=1, add=0; 
	LFPulse.ar(freq, iphase, width, mul).map + add } ], [ 'lfs', {arg freq=0.1, iphase=0; LFSaw.ar(freq, iphase).map } ], [ 'lfc', {arg freq=0.1, iphase=0, mul=1, add=0; 
	LFCub.ar(freq, iphase, mul).map + add } ], [ 'lft', {arg freq=0.1, iphase=0, mul=1, add=0; 
	LFTri.ar(freq, iphase, mul).map + add } ] ]