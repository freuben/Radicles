[ [ 'sine', {arg freq=440 , phase=0 , mul=0.4 ; SinOsc.ar(freq,phase,mul)} ], [ 'play', {arg rate=1 , trigger=1 , startPos=0 , mul=1 ;
	PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), 0)*mul;} ], [ 'loop', {arg rate=1 , trigger=1 , startPos=0 , mul=1 ;
	PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), 1)*mul;} ], [ 'cue', {arg loop=1 , mul=1 ;
	DiskIn.ar(\buffer.numChannels, \buffer, loop)*mul;} ], [ 'playpv', {arg rate=1 , offset=0 , numFrames=0.6 , winType=0 , mul=1 ;
	var localbuf, chain;
	localbuf = LocalBuf.new(numFrames.linexp(0,1,32,32768), \buffer.numChannels);
		chain = PV_PlayBuf(localbuf, \buffer.bufnum, rate, offset.linlin(0,1,0, \buffer.numFrames),
 0);
	IFFT(chain, winType)*mul} ], [ 'looppv', {arg rate=1 , offset=0 , numFrames=0.6 , winType=0 , mul=1 ;
	var localbuf, chain;
	localbuf = LocalBuf.new(numFrames.linexp(0,1,32,32768), \buffer.numChannels);
		chain = PV_PlayBuf(localbuf, \buffer.bufnum, rate, offset.linlin(0,1,0, \buffer.numFrames),
 1);
	IFFT(chain, winType)*mul} ], [ 'playbuf', {arg rate=1 , trigger=1 , startPos=0 , mul=1 , loop=1 ;
	PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), loop)*mul;} ], [ 'rec', {arg recLevel=1 , preLevel=0 ;
	var sig;
	sig = \in;
	RecordBuf.ar(sig, \buffer.bufnum, 0, recLevel, preLevel, loop: 0); } ], [ 'recpv', {arg numChannels=1 , numFrames=0.6 , hop=0.5 , winType=0 , offset=0 , run=1 , loop=0 , mul=1 ;
	var sig, localbuf, chain;
	sig = \in;
	localbuf = LocalBuf(numFrames.linexp(0,1,32,32768), numChannels);
	chain = FFT(localbuf, sig, hop/2, winType);
	chain = PV_RecordBuf(chain, \buffer.bufnum, offset.linlin(0,1,0, \buffer.numFrames), run, loop, hop, winType)*mul} ], [ 'recloop', {arg recLevel=1 , preLevel=0 ;
	var sig;
	sig = \in;
	RecordBuf.ar(sig, \buffer.bufnum, 0, recLevel, preLevel, loop: 1); } ], [ 'looptr', {arg rate=1 , triggerRate=1 , startPos=0 , mul=1 ;
	PlayBuf.ar(\buffer.numChannels, \buffer, rate, Impulse.kr(triggerRate),
 startPos.linlin(0,1,0, \buffer.numFrames), 1)*mul;} ], [ 'osc1', {arg freq=400 , phase=0 , mul=0.5 ; Osc.ar(\buffer, freq, phase, mul)} ], [ 'shaperSine', {arg freq=440 , phase=0 , mul=0.5 ;
	Shaper.ar(\buffer, SinOsc.ar(freq, phase, mul), 0.5)} ], [ 'vosc', {arg bufoffset=0 , freq=440 , phase=0 , mul=0.5 ;
		var buffer, bufnum;
	buffer = \buffer;
	bufnum = buffer[0].bufnum;
	VOsc.ar(bufoffset+MouseX.kr(bufnum, bufnum+buffer.size-1), freq, phase, mul)} ], [ 'test2', {arg freq = 400 , amp = 0.4 , mul = 0.4 , harm = 3;
	Saw.ar(freq, amp);} ], [ 'test1', {arg freq = 400 , amp = 0.4 , mul = 0.4
 , harm = 3;
SinOsc.ar(freq, 0, amp);
} ], [ 'loopAll', {arg rate=1 , trigger=1 , startPos=0 , mul=1 ;
	var buffer, play;
	buffer = \buffer;
	play = buffer.collect{|item|
		PlayBuf.ar(item.numChannels, item, rate, trigger, startPos.linlin(0,1,0, item.numFrames), 1)*mul;};
	play.sum/play.size;
} ] ]