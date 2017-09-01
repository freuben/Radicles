[ [ 'sine', {arg freq=440, phase=0, mul=0.4; SinOsc.ar(freq,phase,mul)} ], [ 'play', {arg buffer; {arg rate=1, trigger=1, startPos=0, mul=1;
	PlayBuf.ar(buffer.numChannels, buffer, rate, trigger, startPos, 0)*mul;} } ], [ 'loop', {arg buffer; {arg rate=1, trigger=1, startPos=0, mul=1;
	PlayBuf.ar(buffer.numChannels, buffer, rate, trigger, startPos, 1)*mul;} } ], [ 'pseq1', {arg key, instruments, notes, durs, quant=0;
	Pbind(\instrument, instruments, \note, notes, \dur, durs);} ], [ 'cue', {arg buffer; {arg loop=1, mul=1;
	DiskIn.ar(buffer.numChannels, buffer, loop)*mul;} } ], [ 'playpv', {arg buffer; {arg rate=1, offset=0, numFrames=2048, winType=1, mul=1;
	var localbuf, chain;
	localbuf = LocalBuf.new(numFrames, 1);
	chain = PV_PlayBuf(localbuf, buffer.bufnum, rate, offset, 0);
	IFFT(chain, winType)*mul}; } ], [ 'looppv', {arg buffer; {arg rate=1, offset=0, numFrames=2048, winType=1, mul=1;
	var localbuf, chain;
	localbuf = LocalBuf.new(numFrames, 1);
	chain = PV_PlayBuf(localbuf, buffer.bufnum, rate, offset, 1);
	IFFT(chain, winType)*mul}; } ] ]