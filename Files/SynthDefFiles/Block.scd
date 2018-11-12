[ [ 'test2', {arg freq = 400 -> [200.0,1000, \exp, {|val| val.round}], amp
 = 0.4 -> [0,1], mul = 0.4 -> [0,0.5], harm = 3-> [1,10, {|val| val.round;}];
	Saw.ar(freq, amp);} ], [ 'test1', {arg freq = 400 -> [200.0,1000, \exp, {|val|
 val.round}], amp = 0.4 -> [0,1], mul = 0.4 -> [0,0.5], harm = 3-> [1,10,
 {|val|
 val.round;}];
SinOsc.ar(freq, 0, amp);
} ], [ 'sine', {arg freq=440 -> [20, 20000, \exp], phase=0 -> [-2pi, 2pi], mul=0.4 -> [0,2]; SinOsc.ar(freq,phase,mul)} ], [ 'play', {arg rate=1 -> [0.1,10], trigger=1 -> [0,1, {|val| val.round(1)}], startPos=0 -> [0,1], mul=1 -> [0,2];
	Mix(PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), 0)*mul);} ], [ 'loop', {arg rate=1 -> [0.1,10], trigger=1 -> [0,1, {|val| val.round(1)}], startPos=0 -> [0,1], mul=1 -> [0,2];
	Mix(PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), 1)*mul);} ], [ 'cue', {arg loop=1 -> [0,1, {|val| val.round(1)}], mul=1 -> [0,2];
	DiskIn.ar(\buffer.numChannels, \buffer, loop)*mul;} ], [ 'playpv', {arg rate=1 -> [0.1,10], offset=0 -> [0,1], numFrames=0.6 -> [0,1,\lin, 0.1], winType=0 -> [-1,1, {|val| val.round(1)}], mul=1 -> [0,2];
	var localbuf, chain;
	localbuf = LocalBuf.new(numFrames.linexp(0,1,32,32768), \buffer.numChannels);
		chain = PV_PlayBuf(localbuf, \buffer.bufnum, rate, offset.linlin(0,1,0, \buffer.numFrames),
 0);
	IFFT(chain, winType)*mul} ], [ 'looppv', {arg rate=1 -> [0.1,10], offset=0 -> [0,1], numFrames=0.6 -> [0,1,\lin, 0.1], winType=0 -> [-1,1, {|val| val.round(1)}], mul=1 -> [0,2];
	var localbuf, chain;
	localbuf = LocalBuf.new(numFrames.linexp(0,1,32,32768), \buffer.numChannels);
		chain = PV_PlayBuf(localbuf, \buffer.bufnum, rate, offset.linlin(0,1,0, \buffer.numFrames),
 1);
	IFFT(chain, winType)*mul} ], [ 'playbuf', {arg rate=1 -> [0.1,10], trigger=1 -> [0,1, {|val| val.round(1)}], startPos=0 -> [0,1], mul=1 -> [0,2], loop=1 -> [0,1, {|val| val.round(1)}];
	PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), loop)*mul;} ], [ 'rec', {arg recLevel=1 -> [0,2], preLevel=0 -> [0,2];
	var sig;
	sig = \in;
	RecordBuf.ar(sig, \buffer, 0, recLevel, preLevel, loop: 0); } ], [ 'recpv', {arg numChannels=1 -> [1,10, {|val| val.round(1)}], numFrames=0.6 -> [0,1,\lin, 0.1], hop=0.5 -> [0,1], winType=0 -> [-1,1, {|val| val.round(1)}], offset=0 -> [0,1], run=1 -> [0,1, {|val| val.round(1)}], loop=0 -> [0,1, {|val| val.round(1)}], mul=1 -> [0,2];
	var sig, localbuf, chain;
	sig = \in;
	localbuf = LocalBuf(numFrames.linexp(0,1,32,32768), numChannels);
	chain = FFT(localbuf, sig, hop/2, winType);
	chain = PV_RecordBuf(chain, \buffer.bufnum, offset.linlin(0,1,0, \buffer.numFrames), run, loop, hop, winType)*mul} ], [ 'recloop', {arg recLevel=1 -> [0,2],
	preLevel=0 -> [0,2];
	var sig;
	sig = \in;
	RecordBuf.ar(sig, \buffer, 0, recLevel, preLevel, loop: 1); } ], [ 'looptr', {arg rate=1 -> [0.1,10], triggerRate=1 -> [0.1,10], startPos=0 -> [0,1], mul=1 -> [0,2];
	PlayBuf.ar(\buffer.numChannels, \buffer, rate, Impulse.kr(triggerRate),
 startPos.linlin(0,1,0, \buffer.numFrames), 1)*mul;} ], [ 'osc1', {arg freq=400 -> [20,20000, \exp], phase=0 -> [-2pi, 2pi], mul=0.5 -> [0,2]; Osc.ar(\buffer, freq, phase, mul)} ], [ 'shaperSine', {arg freq=440 -> [20,20000, \exp], phase=0 -> [-2pi, 2pi], mul=0.5 -> [0,2];
	Shaper.ar(\buffer, SinOsc.ar(freq, phase, mul), 0.5)} ], [ 'vosc', {arg bufoffset=0 -> [0,10], freq=440 -> [20,20000, \exp], phase=0 -> [-2pi, 2pi], mul=0.5 -> [0,2];
		var bufnum;
		bufnum = \buffer.bufnum;
	VOsc.ar(bufoffset+MouseX.kr(bufnum, bufnum+\buffer.size-1), freq, phase, mul)} ] ]