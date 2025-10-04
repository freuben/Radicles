[ [ 'test2', {arg freq = 400 -> [200.0,1000, \exp, {|val| val.round}], amp
 = 0.4 -> [0,1], mul = 0.4 -> [0,0.5], harm = 3-> [1,10, {|val| val.round;}];
	Saw.ar(freq, amp);} ], [ 'test1', {arg freq = 400 -> [200.0,1000, \exp, {|val|
 val.round}], amp = 0.4 -> [0,1], mul = 0.4 -> [0,0.5], harm = 3-> [1,10,
 {|val|
 val.round;}];
SinOsc.ar(freq, 0, amp);
} ], [ 'sine', {arg freq=440 -> [20, 20000, \exp], phase=0 -> [-2pi, 2pi], mul=0.4 -> [0,1]; SinOsc.ar(freq,phase,mul)} ], [ 'play', {arg rate=1 -> [0.1,10], trigger=1 -> [0,1, {|val| val.round(1)}], startPos=0 -> [0,1], mul=1 -> [0,2];
	PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), 0)*mul;} ], [ 'loop', {arg rate=1 -> [0.1,10], trigger=1 -> [0,1, {|val| val.round(1)}], startPos=0 -> [0,1], mul=1 -> [0,2];
	PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), 1)*mul;} ], [ 'cue', {arg loop=1 -> [0,1, {|val| val.round(1)}], mul=1 -> [0,2];
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
	PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), loop)*mul;} ], [ 'rec', {arg recLevel=1 -> [0,2], preLevel=0 -> [0,2], fade=0.01 -> [0,0.1];
	var sig;
	sig = \in;
		sig = sig * Env.linen(fade,  (\duration-(fade*2)), fade, 1, 'sin').kr;
	RecordBuf.ar(sig, \buffer.bufnum, 0, recLevel, preLevel, loop: 0); } ], [ 'recpv', {arg numChannels=1 -> [1,10, {|val| val.round(1)}], numFrames=0.6 -> [0,1,\lin, 0.1], hop=0.5 -> [0,1], winType=0 -> [-1,1, {|val| val.round(1)}], offset=0 -> [0,1], run=1 -> [0,1, {|val| val.round(1)}], loop=0 -> [0,1, {|val| val.round(1)}], mul=1 -> [0,2], fade=0.01 -> [0,0.1];
	var sig, localbuf, chain;
	sig = \in;
	sig = sig * Env.linen(fade,  (\duration-(fade*2)), fade, 1, 'sin').kr;
	localbuf = LocalBuf(numFrames.linexp(0,1,32,32768), numChannels);
	chain = FFT(localbuf, sig, hop/2, winType);
	chain = PV_RecordBuf(chain, \buffer.bufnum, offset.linlin(0,1,0, \buffer.numFrames), run, loop, hop, winType)*mul} ], [ 'recloop', {arg recLevel=1 -> [0,2], preLevel=0 -> [0,2], fade=0.01 -> [0,0.1];
	var sig;
	sig = \in;
	sig = sig * Env.asr(fade, 1, fade, 'sin').kr;
	RecordBuf.ar(sig, \buffer.bufnum, 0, recLevel, preLevel, loop: 1); } ], [ 'looptr', {arg rate=1 -> [0.1,10], triggerRate=1 -> [0.1,10], startPos=0 -> [0,1], mul=1 -> [0,2];
	PlayBuf.ar(\buffer.numChannels, \buffer, rate, Impulse.kr(triggerRate),
 startPos.linlin(0,1,0, \buffer.numFrames), 1)*mul;} ], [ 'osc1', {arg freq=400 -> [20,20000, \exp], phase=0 -> [-2pi, 2pi], mul=0.5 -> [0,2]; Osc.ar(\buffer, freq, phase, mul)} ], [ 'shaperSine', {arg freq=440 -> [20,20000, \exp], phase=0 -> [-2pi, 2pi], mul=0.5 -> [0,2];
	Shaper.ar(\buffer, SinOsc.ar(freq, phase, mul), 0.5)} ], [ 'vosc', {arg bufoffset=0 -> [0,10], freq=440 -> [20,20000, \exp], phase=0 -> [-2pi, 2pi], mul=0.5 -> [0,2];
		var buffer, bufnum;
	buffer = \buffer;
	bufnum = buffer[0].bufnum;
	VOsc.ar(bufoffset+MouseX.kr(bufnum, bufnum+buffer.size-1), freq, phase, mul)} ], [ 'loopAll', {arg rate=1 -> [0.1,10], trigger=1 -> [0,1, {|val| val.round(1)}], startPos=0 -> [0,1], mul=1 -> [0,2];
	var buffer, play;
	buffer = \buffer;
	play = buffer.collect{|item|
		PlayBuf.ar(item.numChannels, item, rate, trigger, startPos.linlin(0,1,0, item.numFrames), 1)*mul;};
	play.sum/play.size;
} ], [ 'audioin', {arg bus=1 -> [1,8], mul= 1 -> [0,4];
	SoundIn.ar((bus.round(1)-1).max(0), mul);} ], [ 'grainbuf', {arg rate=1 -> [0.1,10], freq= 1 -> [0.5,50], dur = 0.1 -> [0.0001, 0.1, {|val|val.round(0.0001)}], startPos=0 -> [0,1], interp=1 -> [0,4, {|val| val.round(1)}], pan= 0 -> [-1,1];
	var buffer;
	buffer = \buffer;
	BufGrain.ar(Impulse.kr(freq), dur, \buffer, rate, startPos, interp); 	
	/*GrainBuf.ar(buffer.numChannels, Impulse.kr(freq), dur, buffer[0], rate, startPos, interp, pan); 	*/
} ], [ 'grainfm', {arg freq= 1 -> [0.5,50], dur = 0.1 -> [0.0001, 0.1, {|val| val.round(0.0001)}], carfreq= 440 -> [20,20000, \exp], modFreq=200 -> [1,1000, \exp], index= 1 -> [0.1,10], pan = 0 -> [-1,1];
	GrainFM.ar(1, Impulse.kr(freq), dur, carfreq, modFreq, index, pan); 	
} ], [ 'formletfun', {arg my= 1000 -> [400,3200];
	var amp;
    amp = LFPulse.kr(0.5,0,0.5);
        Formlet.ar(Dust.ar(12,[0.05,0.05]) * amp, my * exprand(0.5,2.0), 0.005, 0.1);
} ], [ 'sinenv', {arg trig = 1 -> [0,1,{|val| val.round(1)}], freq=440 -> [20, 20000, \exp], phase=0 -> [-2pi, 2pi], mul=0.4 -> [0,2], attack=0.01 -> [0.0001,0.5], decay = 0.3 -> [0.001,0.4], sustain= 0.5 -> [0.1,1], release = 1 -> [0.01,3], octMul=1 -> [0.25,4];
	var cps;
	cps = Latch.ar(freq*octMul, trig);
	SinOsc.ar(cps,phase,mul) * EnvGen.kr(Env.adsr(attack, decay, sustain, release), trig);
} ], [ 'blipenv', {arg trig = 1 -> [0,1,{|val| val.round(1)}], freq=440 -> [20, 20000, \exp], numharm=0 -> [0, 200], mul=0.4 -> [0,1], attack=0.01 -> [0.0001,0.5], decay = 0.3 -> [0.001,0.4], sustain= 0.5 -> [0.1,1], release = 1 -> [0.01,3]; 
	Blip.ar(freq,numharm,mul) * EnvGen.kr(Env.adsr(attack, decay, sustain, release), trig);
} ], [ 'blipdissenv', {arg trig = 1 -> [0,1,{|val| val.round(1)}], freq=440 -> [20, 20000, \exp], diss=0 -> [0, 1], mul=0.4 -> [0,1], attack=0.01 -> [0.0001,0.5], decay = 0.3 -> [0.001,0.4], sustain= 0.5 -> [0.1,1], release = 1 -> [0.01,3]; 
	Blip.ar(freq,(diss.sqrt)*200,mul) * EnvGen.kr(Env.adsr(attack, decay, sustain, release), trig);
} ], [ 'sinekey', {arg key=0 -> [0, 23, {|val| val.round(1)}], oct=1 -> [0.25,4], phase=0 -> [-2pi, 2pi], mul=0.4 -> [0,1], lag = 0.01 => [0,0.1]; 
	var chord;
	chord = Select.kr(key, [
		([60, 64, 67]+0).midicps*oct,
		([60, 64, 67]+1).midicps*oct,
		([60, 64, 67]+2).midicps*oct,
		([60, 64, 67]+3).midicps*oct,
		([60, 64, 67]+4).midicps*oct,
		([60, 64, 67]+5).midicps*oct,
		([60, 64, 67]+6).midicps*oct,
		([60, 64, 67]+7).midicps*oct,
		([60, 64, 67]+8).midicps*oct,
		([60, 64, 67]+9).midicps*oct,
		([60, 64, 67]+10).midicps*oct,
		([60, 64, 67]+11).midicps*oct,
		([60, 63, 67]+0).midicps*oct,
		([60, 63, 67]+1).midicps*oct,
		([60, 63, 67]+2).midicps*oct,
		([60, 63, 67]+3).midicps*oct,
		([60, 63, 67]+4).midicps*oct,
		([60, 63, 67]+5).midicps*oct,
		([60, 63, 67]+6).midicps*oct,
		([60, 63, 67]+7).midicps*oct,
		([60, 63, 67]+8).midicps*oct,
		([60, 63, 67]+9).midicps*oct,
		([60, 63, 67]+10).midicps*oct,
		([60, 63, 67]+11).midicps*oct,
	]);	
	Mix.ar(SinOsc.ar([chord[0].lag(lag), chord[1].lag(lag), chord[2].(lag)],phase,mul/3));
	} ], [ 'sinekey2', {arg key=0 -> [0, 23, {|val| val.round(1)}], oct=1 -> [0.25,4], phase=0 -> [-2pi, 2pi], mul=0.4 -> [0,1], lag = 0.01 => [0,0.1], inv1=0 -> [-24, 24, {|val| val.round(12)}], inv2=0 -> [-24, 24, {|val| val.round(12)}], inv3=0 -> [-24, 24, {|val| val.round(12)}]; 
	var chord;
	chord = Select.kr(key, [
		([60+inv1, 64+inv2, 67+inv3]+0).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+1).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+2).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+3).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+4).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+5).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+6).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+7).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+8).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+9).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+10).midicps*oct,
		([60+inv1, 64+inv2, 67+inv3]+11).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+0).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+1).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+2).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+3).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+4).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+5).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+6).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+7).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+8).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+9).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+10).midicps*oct,
		([60+inv1, 63+inv2, 67+inv3]+11).midicps*oct,
	]);	
	Mix.ar(SinOsc.ar([chord[0].lag(lag), chord[1].lag(lag), chord[2].(lag)],phase,mul/3));
	} ], [ 'karplus', {arg trig = 1 -> [0,1,{|val| val.round(1)}], freq=440 -> [20, 20000, \exp], decayTime = 2.0 -> [0.2,4], att = 0 -> [0,0.01], dec = 0.001 -> [0,0.1];
    var burstEnv, signalOut, delayTime;
    delayTime = freq.min(2000).reciprocal;
	delayTime = Latch.ar(delayTime, trig);
    burstEnv = EnvGen.kr(Env.perc(att, dec), trig);
	signalOut = CombL.ar(PinkNoise.ar(burstEnv), 0.1, delayTime, decayTime)} ], [ 'sinenv2', {arg trig = 1 -> [0,1,{|val| val.round(1)}], freq=440 -> [20, 20000, \exp], phase=0 -> [-2pi, 2pi], mul=0.4 -> [0,2], attack=0.01 -> [0.0001,0.5], decay = 0.3 -> [0.001,0.4], sustain= 0.5 -> [0.1,1], release = 1 -> [0.01,3]; 
	var cps;
	cps = Latch.ar(freq, trig);
	SinOsc.ar(cps,phase,mul) * EnvGen.kr(Env.adsr(attack, decay, sustain, release), trig);
} ], [ 'sawenv', {arg trig = 1 -> [0,1,{|val| val.round(1)}], freq=440 -> [20, 20000, \exp], mul=0.4 -> [0,2], attack=0.01 -> [0.0001,0.5], decay = 0.3 -> [0.001,0.4], sustain= 0.5 -> [0.1,1], release = 1 -> [0.01,3], octMul=1 -> [0.25,4]; 
	var cps;
	cps = Latch.ar(freq*octMul, trig);
	Saw.ar(cps,mul) * EnvGen.kr(Env.adsr(attack, decay, sustain, release), trig);
} ], [ 'squarenv', {arg trig = 1 -> [0,1,{|val| val.round(1)}], freq=440 -> [20, 20000, \exp], width=0.5 -> [0, 1], mul=0.4 -> [0,2], attack=0.01 -> [0.0001,0.5], decay = 0.3 -> [0.001,0.4], sustain= 0.5 -> [0.1,1], release = 1 -> [0.01,3], octMul=1 -> [0.25,4];
	var cps;
	cps = Latch.ar(freq*octMul, trig);
	Pulse.ar(cps,width,mul) * EnvGen.kr(Env.adsr(attack, decay, sustain, release), trig);
} ], [ 'noise', {arg mul=0.5->[0,1];
	WhiteNoise.ar(mul);
} ], [ 'blip', {arg freq=10 -> [0.1,30], mul=0.5->[0,1], freq2 = 1200 -> [200,2000,\exp];
	Decay.ar(Impulse.ar(freq), 0.25, LFCub.ar(freq2, 0, 0.1)) *mul;
} ], [ 'ravein', {arg amp = 1->[0,1], prior=1->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'ravein2', {arg amp = 1->[0,1], prior=1->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'ravest', {arg amp = 1->[0,1], prior=1->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts", 16,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts", 16,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts",
			z)*amp;
} ], [ 'ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts", 16,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts",
			z)*amp; 
} ], [ 'ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts",
            z,
        ) *4 *amp;
} ], [ 'pravein', {arg amp = 1->[0,1], prior=1->[0,1,{|val| val.round}], temp=1->[0,1,{|val| val.round}], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'pravein2', {arg amp = 1->[0,1], prior=1->[0,1,{|val| val.round}], temp=1->[0,1,{|val| val.round}], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'pravest', {arg amp = 1->[0,1], prior=1->[0,1,{|val| val.round}], temp=1->[0,1,{|val| val.round}], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'pravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts", 4,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'pravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts", 4,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts",
			z)*amp;
} ], [ 'pravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts", 4,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts",
			z)*amp;
} ], [ 'pravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/percussion.ts",
            z,
        ) *4 *amp;
} ], [ 'vravein', {arg amp = 1->[0,1], prior=1->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'vravein2', {arg amp = 1->[0,1], prior=1->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'vravest', {arg amp = 1->[0,1], prior=1->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'vravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts", 8,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'vravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts", 8,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts",
			z)*amp;
} ], [ 'vravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts", 8,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts",
			z)*amp;
} ], [ 'vravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts",
            z,
        ) *4 *amp;
} ], [ 'travein', {arg amp = 1->[0,1], prior=1->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}], model=0->[0,8,{|val| val.round(1).asInteger}];
	var strArr, string, index;
	strArr = ["/Users/fr650/Documents/SuperCollider/RAVE tests/rave_sax_march15.ts", "/Users/fr650/Documents/SuperCollider/RAVE tests/VCTK.ts"];
	index = model.source.values.last;
	string = strArr[index];
	string.cs.postln;
	RAVE.new(
		string,
		SoundIn.ar(bus-1), // input for timbre transfer
		prior, // 0 for timbre transfer, 1 for generation
		temp // temperature of generation
	) * amp;
} ], [ 'i1ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i1ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i1ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i1ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts", 16,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i1ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts", 16,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i1ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts", 16,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i1ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_motherbird_b2048_r48000_z16.ts",
            z,
        ) *4 *amp;
} ], [ 'i2ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i2ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i2ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i2ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts", 12,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i2ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts", 12,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts",
			z)*amp;
} ], [ 'i2ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts", 12,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts",
			z)*amp;
} ], [ 'i2ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/birds_pluma_b2048_r48000_z12.ts",
            z,
        ) *4 *amp;
} ], [ 'i3ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i3ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i3ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i3ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts", 16,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i3ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts", 16,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i3ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts", 16,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i3ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/guitar_iil_b2048_r48000_z16.ts",
            z,
        ) *4 *amp;
} ], [ 'i4ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i4ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i4ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i4ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts", 20,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i4ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts", 20,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts",
			z)*amp;
} ], [ 'i4ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts", 20,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts",
			z)*amp;
} ], [ 'i4ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/humpbacks_pondbrain_b2048_r48000_z20.ts",
            z,
        ) *4 *amp;
} ], [ 'i5ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i5ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i5ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i5ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts", 8,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i5ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts", 8,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts",
			z)*amp;
} ], [ 'i5ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts", 8,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts",
			z)*amp;
} ], [ 'i5ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/magnets_b2048_r48000_z8.ts",
            z,
        ) *4 *amp;
} ], [ 'i6ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i6ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i6ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i6ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts", 20,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i6ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts", 20,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts",
			z)*amp;
} ], [ 'i6ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts", 20,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts",
			z)*amp;
} ], [ 'i6ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/marinemammals_pondbrain_b2048_r48000_z20.ts",
            z,
        ) *4 *amp;
} ], [ 'i7ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i7ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i7ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i7ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts", 16,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i7ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts", 16,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts",
			z)*amp;
} ], [ 'i7ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts", 16,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts",
			z)*amp;
} ], [ 'i7ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/mrp_strengjavera_b2048_r44100_z16.ts",
            z,
        ) *4 *amp;
} ], [ 'i8ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i8ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i8ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i8ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts", 16,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i8ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts", 16,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i8ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts", 16,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i8ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_archive_b2048_r48000_z16.ts",
            z,
        ) *4 *amp;
} ], [ 'i9ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i9ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i9ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i9ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts", 16,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i9ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts", 16,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts",
			z)*amp;
} ], [ 'i9ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts", 16,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts",
			z)*amp;
} ], [ 'i9ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/organ_bach_b2048_sr48000_z16.ts",
            z,
        ) *4 *amp;
} ], [ 'i10ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i10ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i10ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i10ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts", 16,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i10ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts", 16,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i10ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts", 16,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i10ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_hifitts_b2048_r48000_z16.ts",
            z,
        ) *4 *amp;
} ], [ 'i11ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i11ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i11ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i11ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts", 16,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i11ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts", 16,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts",
			z)*amp;
} ], [ 'i11ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts", 16,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts",
			z)*amp;
} ], [ 'i11ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_jvs_b2048_r44100_z16.ts",
            z,
        ) *4 *amp;
} ], [ 'i12ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i12ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i12ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i12ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts", 22,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i12ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts", 22,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts",
			z)*amp;
} ], [ 'i12ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts", 22,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts",
			z)*amp;
} ], [ 'i12ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vctk_b2048_r44100_z22.ts",
            z,
        ) *4 *amp;
} ], [ 'i13ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i13ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i13ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i13ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts", 16,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i13ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts", 16,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i13ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts", 16,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i13ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/voice_vocalset_b2048_r48000_z16.ts",
            z,
        ) *4 *amp;
} ], [ 'i14ravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'i14ravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'i14ravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'i14ravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts", 16,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'i14ravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts", 16,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i14ravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts", 16,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts",
			z)*amp;
} ], [ 'i14ravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/victor/water_pondbrain_b2048_r48000_z16.ts",
            z,
        ) *4 *amp;
} ], [ 'mhravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'mhravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'mhravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'mhravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts", 8,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'mhravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts", 8,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts",
			z)*amp;
} ], [ 'mhravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts", 8,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts",
			z)*amp;
} ], [ 'mhravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/mark/mark.ts",
            z,
        ) *4 *amp;
} ], [ 'dravein', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit",
            SoundIn.ar(bus-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        ) * amp;
} ], [ 'dravein2', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus=1->[1,8,{|val| val.round(1)}];
RAVE.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit",
		Mix.new([SoundIn.ar(bus-1),SoundIn.ar(bus)]), // input for timbre transfer
        prior, // 0 for timbre transfer, 1 for generation
        temp // temperature of generation
        ) * amp;
} ], [ 'dravest', {arg amp = 1->[0,1], prior=0->[0,1,{|val| val.round}], temp=1->[0,2], bus1=1->[1,8,{|val| val.round(1)}], bus2=2->[1,8,{|val| val.round(1)}];
	var left, right;
	left = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit",
            SoundIn.ar(bus1-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	right = RAVE.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit",
            SoundIn.ar(bus2-1), // input for timbre transfer
            prior, // 0 for timbre transfer, 1 for generation
            temp // temperature of generation
        );
	[left, right] * amp;
} ], [ 'dravevan', {arg amp = 1->[0,1], bus=1->[1,8,{|val| val.round(1)}];
	var z;
	z = RAVEEncoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit", 8,
		SoundIn.ar(bus-1),// input for latent embedding
	);
	RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit",
		[z[0]]++(1-z[1..])
	)*amp;
} ], [ 'dravenoise', {arg amp = 1->[0,1], freq=0.5->[0,2], lo=1.0->[0,2], hi=1.5->[0.1,2.1];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit", 8,
		LFNoise1.kr(freq).range(lo,hi),
	);
        RAVEDecoder.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit",
			z)*amp;
} ], [ 'dravedc', {arg amp = 1->[0,1], temp=1->[0,2];
	var z;
	 z = RAVEPrior.new(
		"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit", 8,
		DC.kr(temp), // prior temperature
	);
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit",
			z)*amp;
} ], [ 'dravenoise2', {arg amp = 1->[0,1], lo=0.1->[0,2], hi=10->[0.1,2.1];
	var z;
	z = 16.collect{ |i| {LFNoise1.kr(Rand(lo,hi) )*1 }};
        RAVEDecoder.new(
			"/Users/fr650/Documents/SuperCollider/RAVE tests/drumkit",
            z,
        ) *4 *amp;
} ], [ 'audioins', {arg bus=1 -> [1,8], mul= 1 -> [0,4];
	[SoundIn.ar((bus.round(1)-1).max(0), mul), 
	SoundIn.ar((bus.round(1)).max(0), mul)]
	;} ], [ 'audioin2', {arg bus=1 -> [1,8], mul= 1 -> [0,4], pan=0 -> [-1,1], lagTime=0.1 -> [0,1];
	Pan2.ar(SoundIn.ar((bus.round(1)-1).max(0)), pan.lag(lagTime))*mul;
} ], [ 'play2', {arg rate=1 -> [0.1,10], trigger=1 -> [0,1, {|val| val.round(1)}], startPos=0 -> [0,1], mul=1 -> [0,2], pan=0 -> [-1,1], lagTime=0.1 -> [0,1];
	Pan2.ar(PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), 0)*mul, pan.lag(lagTime));} ], [ 'loop2', {arg rate=1 -> [0.1,10], trigger=1 -> [0,1, {|val| val.round(1)}], startPos=0 -> [0,1], mul=1 -> [0,2], pan=0 -> [-1,1], lagTime=0.1 -> [0,1];
	Pan2.ar(PlayBuf.ar(\buffer.numChannels, \buffer, rate, trigger, startPos.linlin(0,1,0, \buffer.numFrames), 1)*mul, pan.lag(lagTime));} ] ]