[ [ 'pan2', {arg inputArr, panArr, chanNum, vol=1;
	{var signal, sigArr;
	inputArr.do({|item, index|
		var sig;
		sig = (\in++index).asSymbol.ar([0]);
		sigArr = sigArr.add(Pan2.ar(sig, panArr[index]);)
	});
		signal = (sigArr.sum.flat * vol);}} ], [ 'panB', {arg inputArr, panArr, chanNum, vol=1;
	var signal, sigArr;
	inputArr.do({|item, index|
		var  w, x, y, z, sig;
		sig = (\in++index).asSymbol.ar([0]);
		#w, x, y, z = PanB.ar(sig, panArr[0][index], panArr[1][index]);
		sigArr = sigArr.add(DecodeB2.ar(chanNum, w, x, y);)
	});
	signal = (sigArr.sum * vol);} ], [ 'split', {arg inputArr, panArr, chanNum, vol=1;
		var signal, sigArr, sig;
		inputArr.do({|item, index|
			sig = (\in++index).asSymbol.ar([0]);
			sigArr = sigArr.add(Out.ar(panArr[index], sig));
		});
		signal = (sigArr.sum * vol);
} ] ]