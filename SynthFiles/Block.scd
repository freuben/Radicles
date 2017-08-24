[ [ 'sine', {arg freq=440, phase=0, mul=0.4; SinOsc.ar(freq,phase,mul)} ], [ 'play', {arg buffer; {arg mul=1,rate=1, trigger=1, startPos=1, loop=0;
	PlayBuf.ar(buffer.numChannels, buffer, rate, trigger, startPos, 0)*mul;} } ], [ 'loop', {arg buffer; {arg mul=1,rate=1, trigger=1, startPos=1, loop=0;
	PlayBuf.ar(buffer.numChannels, buffer, rate, trigger, startPos, 1)*mul;} } ], [ 'pseq1', {arg key, instruments, notes, durs, quant=0;
	Pbind(\instrument, instruments, \note, notes, \dur, durs );
} ], [ 'playbuf', {arg buffer; {arg mul=1,rate=1, trigger=1, startPos=1, loop=0;
	PlayBuf.ar(buffer.numChannels, buffer, rate, trigger, startPos, loop)*mul;} } ] ]