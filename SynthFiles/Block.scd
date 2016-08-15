[
	['sine', {arg freq=440, phase=0, mul=0.4; SinOsc.ar(freq,phase,mul)} ],
	['play', {arg buffer; {arg rate=1; PlayBuf.ar(buffer.numChannels, buffer, rate)} }],
	['loop', {arg buffer; {arg rate=1; PlayBuf.ar(buffer.numChannels, buffer, rate, loop: 1)} }],
];