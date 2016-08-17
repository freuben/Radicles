[ [ 'pan2', {arg pan, chanNum;
	Pan2.ar(\in.ar([0]), pan);
} ], [ 'panB', {arg pan, chanNum;
	var  w, x, y, z, sig;
	sig = \in.ar([0]);
	#w, x, y, z = PanB.ar(sig, pan[0], pan[1]);
	sig = DecodeB2.ar(chanNum, w, x, y);
} ], [ 'split', {arg inputArr, panArr, chanNum, vol=1;
	{var signal, sigArr, sig;
		inputArr.do({|item, index|
			sig = (\in++index).asSymbol.ar([0]);
			sigArr = sigArr.add(Out.ar(panArr[index], sig));
		});
		signal = (sigArr.sum * vol);
}} ] ]