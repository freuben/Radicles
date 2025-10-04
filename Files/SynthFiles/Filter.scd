[ [ 'pch', {arg pitchRatio=1.0 , pitchDispertion=0.0 , timeDispersion=0.0001
 , winSize=0.02
 ;
		PitchShift.ar(\in, winSize, pitchRatio, pitchDispertion, timeDispersion);
 } ], [ 'pch2', {arg centerpitch=440, pitchDispertion=0.0, timeDispersion=0.0,
		lag=0.1, winSize=0.1;
	var freq, hasFreq;
		# freq, hasFreq = Pitch.kr(Ndef.ar(\bus1), ampThreshold: 0.0, median: 7);
	PitchShift.ar(\in, winSize, (freq[0]/centerpitch).lag(lag), pitchDispertion,
 timeDispersion) } ], [ 'pch3', {arg centerpitch=440, bus=0, adjvol=4, pitchDispertion=0.0,
 timeDispersion=0.0001,
	lag=0.1, winSize=0.02;
	var freq, hasFreq, input, amp;
	input = SoundIn.ar(bus.round(1));
	amp = Amplitude.kr(input, 0.05, 0.05);
	# freq, hasFreq = Pitch.kr(input, ampThreshold: 0.0, median: 7);
	PitchShift.ar(\in, winSize, (freq/centerpitch).lag(lag), pitchDispertion,

		timeDispersion)*(amp*adjvol) } ], [ 'pch4', {arg pitchRatio=1.0, amp=1,
 pitchDispertion=0.0, timeDispersion=0.0001, winSize=0.02, transp=1;
	PitchShift.ar(\in, winSize, (pitchRatio*transp), pitchDispertion,
			timeDispersion)*amp} ], [ 'harm', {arg pitchRatio1=1.0, pitchRatio2=1.0,
 pitchRatio3=1.0,
 pitchRatio4=1.0, vol1=1.0, vol2=1.0,
	vol3=1.0, vol4=1.0, pitchDispertion1=0.0, pitchDispertion2=0.0, pitchDispertion3=0.0,
	pitchDispertion4=0.0, timeDispersion1=0.0001, timeDispersion2=0.0001,
	timeDispersion3=0.0001, timeDispersion4=0.0001, lag1=0.1, lag2=0.1, lag3=0.1,
 lag4=0.1,
	winSize1=0.02, winSize2=0.02, winSize3=0.02, winSize4=0.02;
	var signal, pitch1, pitch2, pitch3, pitch4, input;
	input = \in;
	pitch1 = PitchShift.ar(input, winSize1, (pitchRatio1).lag(lag1), pitchDispertion1,
		timeDispersion1) * vol1.lag(lag1);
	pitch2 = PitchShift.ar(input, winSize2, (pitchRatio2).lag(lag2), pitchDispertion2,
		timeDispersion2) * vol2.lag(lag2);
	pitch3 = PitchShift.ar(input, winSize3, (pitchRatio3).lag(lag3), pitchDispertion3,
		timeDispersion3)	* vol3.lag(lag3);
	pitch4 = PitchShift.ar(input, winSize4, (pitchRatio4).lag(lag4), pitchDispertion4,
		timeDispersion4) * vol4.lag(lag4);
	signal = pitch1 + pitch2 + pitch3 + pitch4;
	signal = signal/4;
	signal;
} ], [ 'harm2', {arg lag=5.0, pitchDispertion=0.0, timeDispersion=0.0001,
 winSize=0.02, pitchRatio1=1.0,
	pitchRatio2=1.0, pitchRatio3=1.0, pitchRatio4=1.0, vol1=1.0, vol2=1.0, vol3=1.0,
 vol4=1.0;
	var signal, pitch1, pitch2, pitch3, pitch4, input;
	input = \in;
	pitch1 = PitchShift.ar(input, winSize, pitchRatio1.lag(lag), pitchDispertion,
 timeDispersion)
	* vol1.lag(lag);
	pitch2 = PitchShift.ar(input, winSize, pitchRatio2.lag(lag), pitchDispertion,
 timeDispersion)
	* vol2.lag(lag);
	pitch3 = PitchShift.ar(input, winSize, pitchRatio3.lag(lag), pitchDispertion,
 timeDispersion)
	* vol3.lag(lag);
	pitch4 = PitchShift.ar(input, winSize, pitchRatio4.lag(lag), pitchDispertion,
 timeDispersion)
	* vol4.lag(lag);
	signal = pitch1 + pitch2 + pitch3 + pitch4;
	signal = signal/4;
	signal;
} ], [ 'ctp', {arg rate=1 , pitchDispersion=0.0 , timeDispersion=0.0001 , lag=0.01 ,
	windowSize=0.02 ;
	var freq, hasFreq, latch1, latch2, latch3, input, maxDelay=12;
	input = \in;
	#freq, hasFreq = Pitch.kr(input);
	latch1 = Latch.ar(freq, Impulse.kr(rate));
	latch2 = DelayL.kr(Latch.ar(freq, Impulse.kr(rate)), maxDelay, 1/rate);
	latch3 = (latch1.cpsmidi - (latch1.cpsmidi - latch2.cpsmidi)).midicps.min(20000).max(20);
	PitchShift.ar(input, windowSize, (latch1/latch3)[0].lag(lag), pitchDispersion,
 timeDispersion);
} ], [ 'cbg', {arg decayTime=0.5 , freq=10 , lag=5.0 ;
	CombL.ar(\in * (LFNoise0.ar(freq.lag(lag)) > 0), 0.2, 0.2, decayTime);} ], [ 'lpf', {arg freq=20000 , lag=0.1 ; LPF.ar(\in, freq.lag(lag));} ], [ 'convrev1', {arg mix=1.0 ;
	var input, output;
	input = \in * Line.kr(0,1,0.1);
	output = \convrev;
	(output*mix.max(0).min(1)) + (input*mix.linlin(0,1,1,0));
} ], [ 'frz2', {arg thresh=1, adjVol=1, lagTime=0.1;
var freeze, chain, input, frameSize=2048;
	input = \in;
	chain = FFT(LocalBuf(frameSize, input.numChannels), input);
	freeze = Onsets.kr(chain, thresh).lag(lagTime);
	chain = PV_MagFreeze(chain, freeze); 
	(adjVol * IFFT(chain));} ], [ 'gdy1', {arg mix=0 , maxMul=1 , ampdist=2 , durDist=3 , adParam=1 , ddParam=1 , ampScale=0.5 , durScale=0.0 , kNum=40 , transp=1 ; 
	var freq, hasFreq, amp, minFreq, sig, in;
	in = \in;
#freq, hasFreq = Pitch.kr(in, minFreq: 20, ampThreshold: 0.0, median: 7);
amp = Amplitude.kr(in, 0.05, 0.05).lag(0.1);
minFreq = ((freq[0] * transp).fold2(1000)).abs.max(20);
sig = Gendy1.ar(ampdist.max(0).min(6), durDist, adParam, ddParam, minFreq,(minFreq*maxMul).min(1000),ampScale,durScale,50,kNum)*amp;
((sig*mix.linlin(0,1,0,1))+(in*mix.linlin(0,1,1,0)))
} ], [ 'cct1', {arg mix=0 , match=0.3 , lffreq=0.1 , sinadd = 300;
var concat, control;
control= SinOsc.ar(0.5)*Saw.ar(SinOsc.kr(LFNoise0.kr(lffreq,3,4.5),0,50,sinadd));
concat= Concat2.ar(control,\in,4.0,4.0,4.0,match,0,0.5,1.0,0.5,0.0);
((concat*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'fds1', {arg mix=0 , alpha= -0.001 , adjVol=0 ; 
var kernel, kernel2, signal, signal2, signal3, freq, hasFreq;
signal = \in;
#freq, hasFreq = Pitch.kr(signal);
kernel= WeaklyNonlinear.ar(signal,0,1,1,freq*2,0,0,alpha,3,0,0);
kernel2= WeaklyNonlinear2.ar(signal,0,1,1,freq*2,0,0,alpha,3,0,0);
signal2 = Convolution.ar(kernel+signal,kernel2, 2048, 0.0005) * EnvGen.kr(Env.asr(0.01, 1.0, 0.01), Amplitude.kr(signal));
signal3 = Limiter.ar(signal2);
	adjVol * ((signal3*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'fds2', {arg mix=0 , num=64 , adjVol=0;
var kernel, signal, signal2, signal3, in;
in = \in;
kernel = LPCError.ar(in, in.linlin(-1,1,num,1));
signal = Convolution.ar(in,kernel, 2048);
signal2 = LPCError.ar(signal, in.linlin(-1,1,1,num));
signal3 = ((signal2+(in.clip2(0.05)*0.05))).clip2(1.0);
adjVol.dbamp * ((signal3*mix.linlin(0,1,0,1))+(in*mix.linlin(0,1,1,0)));} ], [ 'mga', {arg thresh=0, adjVol=1;
var chain, frameSize=2048, input;
	input = \in;
chain = FFT(LocalBuf(frameSize, input.numChannels), input);
chain = PV_MagAbove(chain, thresh); 
(adjVol * IFFT(chain));} ], [ 'mrk1', {arg mix=0 , rec=1 , time=2 ;
	var sig, table;
	table=2;
	sig = MarkovSynth.ar(\in, rec, time, table);
	((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))} ], [ 'chr1', {arg mix=0 , depth=0.1 , predelay=0.08 , speed=0.05 , ph_diff=0.5 , lagTime=0.1 ;
var input, sig, modulators, numDelays;
numDelays = 12;
input = \in * numDelays.reciprocal;
modulators = Array.fill(numDelays, {arg i;
LFPar.kr(speed * rrand(0.94, 1.06), ph_diff * i, depth.lag(lagTime), predelay.lag(lagTime));}); 
sig = DelayC.ar(input, 2.0, modulators);
sig = sig.sum;
sig = sig * 3;
((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'dl2', {arg mul=1, freq=0.25, lag=0.1, decayTime=0, lag2=1; CombL.ar(\in,12,LFNoise1.kr(freq.lag(lag), 1*(mul.lag(lag2)/2), 0+(mul.lag(lag2)/2)).lag(lag),decayTime)} ], [ 'sclip', {arg vol=0 ;
	(\in*vol.dbamp).softclip;
 } ], [ 'pch5', {arg pitchRatio=1.0 , pitchDispertion=0.0 , timeDispersion=0.0001
 , winSize=0.02
 ;
	PitchShift.ar(\in, winSize, pitchRatio, pitchDispertion, timeDispersion)*SinOsc.ar(440);
} ], [ 'hpf', {arg freq=20000 , lag=0.1 ; HPF.ar(\in, freq.lag(lag));} ], [ 'eq8', {arg freq1=30 , freq2=80 , freq3=200 , freq4=500 , freq5=1200 , freq6=3500 , freq7=10000 , freq8=17000 , gain1=0 , gain2=0 , gain3=0 , gain4=0 , gain5=0 , gain6=0 , gain7=0 , gain8=0 , q1=0.71 , q2=1.10 , q3=0.98 , q4=0.71 , q5=0.71 , q6=0.71 , q7=0.71 , q8=0.71 , on1=0 , on2=1 , on3=1 , on4=1 , on5=1 , on6=1 , on7=1 , on8=0 , lag=0.5, masterGain=0 , lagOn=0.1 ;
	var signal;
	signal = (on1.linlin(0,1,1,0).lag(lagOn)*\in) + (on1.lag(lagOn)*BHiPass.ar(\in,freq1.lag(lag),1/q1.lag(lag), gain1.dbamp.lag(lag);));
	signal = (on2.linlin(0,1,1,0).lag(lagOn)*signal) + (on2.lag(lagOn)*BLowShelf.ar(signal,freq2.lag(lag),1/(q2).lag(lag),gain2.dbamp.lag(lag)));
	signal = (on3.linlin(0,1,1,0).lag(lagOn)*signal) + (on3.lag(lagOn)*BPeakEQ.ar(signal,freq3.lag(lag),1/(q3).lag(lag),gain3.dbamp.lag(lag)));
	signal = (on4.linlin(0,1,1,0).lag(lagOn)*signal) + (on4.lag(lagOn)*BPeakEQ.ar(signal,freq4.lag(lag),1/(q4).lag(lag),gain4.dbamp.lag(lag)));
	signal = (on5.linlin(0,1,1,0).lag(lagOn)*signal) + (on5.lag(lagOn)*BPeakEQ.ar(signal,freq5.lag(lag),1/(q5).lag(lag),gain5.dbamp.lag(lag)));
	signal = (on6.linlin(0,1,1,0).lag(lagOn)*signal) + (on6.lag(lagOn)*BPeakEQ.ar(signal,freq6.lag(lag),1/(q6).lag(lag),gain6.dbamp.lag(lag)));
	signal = (on7.linlin(0,1,1,0).lag(lagOn)*signal) + (on7.lag(lagOn)*BHiShelf.ar(signal,freq7.lag(lag),1/(q7).lag(lag),gain7.dbamp.lag(lag)));
	signal = (on8.linlin(0,1,1,0).lag(lagOn)*signal) + (on8.lag(lagOn)*BLowPass.ar(signal,freq8.lag(lag),1/q8.lag(lag), gain8.dbamp.lag(lag)));
	signal * masterGain.dbamp;
} ], [ 'ds1', {arg drive=1, adjVol=1, lag=0.1; var lagdr; lagdr = drive.lag(lag);((\in*lagdr).distort * (lagdr.sqrt/lagdr))*adjVol} ], [ 'ds2', {arg drive=1, adjVol=1, lag=0.1; var lagdr; lagdr = drive.lag(lag);((\in*lagdr).softclip * (lagdr.sqrt/lagdr))*adjVol} ], [ 'ds3', {arg drive=1, adjVol=1, lag=0.1; var lagdr; lagdr = drive.lag(lag);((\in*lagdr).clip2 * (lagdr.sqrt/lagdr))*adjVol} ], [ 'ds4', {arg drive=1, adjVol=1, lag=0.1; var lagdr; lagdr = drive.lag(lag);((\in*lagdr).fold2(1) * (lagdr.sqrt/lagdr))*adjVol} ], [ 'ds5', {arg drive=1, adjVol=1, lag=0.1; var lagdr; lagdr = drive.lag(lag);((\in*lagdr).wrap2(1) * (lagdr.sqrt/lagdr))*adjVol} ], [ 'fvrb', {arg mix=0, room=0, damp=0.0, lag=0.0, adjVol=1; 
	FreeVerb.ar(\in, mix.lag(lag), room.lag(lag), damp.lag(lag))*adjVol} ], [ 'fvrb2', {arg mix=0, room=0, damp=0.0, lag=0.0, adjVol=1; 
	var sig;
	sig = \in;
	FreeVerb2.ar(sig[0], sig[1], mix.lag(lag), room.lag(lag), damp.lag(lag))*adjVol} ], [ 'gvrb1', {arg mix=0.5, adjVol=1; 
	var drysig, wetsig;
	drysig = \in;
	wetsig= GVerb.ar(\in, 5, 0.6, 0.62, 0.48, 15, -6, -11, -13,300,-40.dbamp);
	(((drysig/2) * mix.linlin(0,1,1,0)) + (wetsig * mix))*0.95*adjVol} ], [ 'gvrb2', {arg mix=0.5, adjVol=1; 
	var drysig, wetsig;
	drysig = \in;
	wetsig= GVerb.ar(\in, 16, 1.24, 0.1, 0.95, 15, -3, -15, -17, 300,-40.dbamp);
	(((drysig/2) * mix.linlin(0,1,1,0)) + (wetsig * mix))*0.95*adjVol} ], [ 'gvrb3', {arg mix=0.5, adjVol=1; 
	var drysig, wetsig;
	drysig = \in;
	wetsig= GVerb.ar(\in, 80, 4.85, 0.41, 0.19, 15, -3, -9, -11, 300,-40.dbamp);
	(((drysig/2) * mix.linlin(0,1,1,0)) + (wetsig * mix))*0.95*adjVol} ], [ 'gvrb4', {arg mix=0.5, adjVol=1; 
	var drysig, wetsig;
	drysig = \in;
	wetsig= GVerb.ar(\in, 243, 1, 0.1, 0.19, 15, -3, -9, -11, 300,-40.dbamp);
	(((drysig/2) * mix.linlin(0,1,1,0)) + (wetsig * mix))*0.95*adjVol} ], [ 'gvrb5', {arg mix=0.5, adjVol=1; 
	var drysig, wetsig;
	drysig = \in;
	wetsig= GVerb.ar(\in, 300, 103, 0.43, 0.51, 15, -5, -26, -20, 300,-40.dbamp);
	(((drysig/2) * mix.linlin(0,1,1,0)) + (wetsig * mix))*0.95*adjVol} ], [ 'amp', {arg bus=1, adjVol=1, lag=0.1; (\in*Amplitude.kr(SoundIn.ar((bus-1).round(1))).lag(lag))*adjVol
} ], [ 'dl1', {arg delayTime=0, decayTime=1; CombL.ar(\in, 12, delayTime, decayTime)} ], [ 'cvt1', {arg mix=0, track=1, frameSize=1024 , adjVol=1; 
	var signal, input;
	signal = Select.ar(track-1, [Ndef(\track1).ar, Ndef(\track2).ar, Ndef(\track3).ar, Ndef(\track4).ar, Ndef(\track5).ar, Ndef(\track6).ar, Ndef(\track7).ar, Ndef(\track8).ar]);
	input = \in;
	(Convolution.ar(input,signal, frameSize,adjVol*0.15)*mix)
		+(input*mix.linlin(0,1,1,0))} ], [ 'cvt2', {arg mix=0, track=1, frameSize=1024 , adjVol=1; 
	var signal, input;
	signal = Select.ar(track-1, [Ndef(\track1).ar, Ndef(\track2).ar, Ndef(\track3).ar, Ndef(\track4).ar, Ndef(\track5).ar, Ndef(\track6).ar, Ndef(\track7).ar, Ndef(\track8).ar]);
	input = \in;
	(Convolution.ar(signal,input, frameSize,adjVol*0.15)*mix)
		+(input*mix.linlin(0,1,1,0))} ], [ 'cvi1', {arg mix=0, soundin=1, frameSize=1024 , adjVol=1; 
	var signal, input;
	signal = SoundIn.ar((soundin+1).round(1));
	input = \in;
	(Convolution.ar(input,signal,frameSize,adjVol*0.15)*mix)
	+(input*mix.linlin(0,1,1,0))} ], [ 'cvi2', {arg mix=0, soundin=1, frameSize=1024 , adjVol=1; 
	var signal, input;
	signal = SoundIn.ar((soundin+1).round(1));
	input = \in;
	(Convolution.ar(signal, input, frameSize,adjVol*0.15)*mix)
	+(input*mix.linlin(0,1,1,0))} ], [ 'mmi', {arg mix=0, soundin=1, adjVol=1; 
var input, inA, chainA, chainB, chain, frameSize=2048;
	input = \in;
	inA = SoundIn.ar((soundin-1).round(1));
chainA = FFT(LocalBuf(frameSize), input);
chainB = FFT(LocalBuf(frameSize), inA);
chain = PV_MagMul(chainA, chainB);
Mix.new(adjVol * ((IFFT(chain)*mix.linlin(0,1,0,1))+(input*mix.linlin(0,1,1,0))));
} ], [ 'mmt', {arg mix=0, track=1, adjVol=1; 
var input, inA, chainA, chainB, chain, frameSize=2048;
	input = \in;
	inA = Select.ar(track-1, [Ndef(\track1).ar, Ndef(\track2).ar, Ndef(\track3).ar, Ndef(\track4).ar, Ndef(\track5).ar, Ndef(\track6).ar, Ndef(\track7).ar, Ndef(\track8).ar]);
chainA = FFT(LocalBuf(frameSize, input.numChannels), input);
chainB = FFT(LocalBuf(frameSize, inA.numChannels), inA);
chain = PV_MagMul(chainA, chainB);
(adjVol * ((IFFT(chain)*mix.linlin(0,1,0,1))+(input*mix.linlin(0,1,1,0))));
} ], [ 'frz1', {arg freeze=0, adjVol=1;
var chain, input, frameSize=2048;
	input = \in;
chain = FFT(LocalBuf(frameSize, input.numChannels), input);
chain = PV_MagFreeze(chain, freeze); 
	(adjVol * IFFT(chain));} ], [ 'srt', {arg mix=0, adjVol=1;
	var chain, origmagsphases, magsphases, input;
	input = \in;
	chain = FFT(LocalBuf(1024, input.numChannels), input);
	origmagsphases = UnpackFFT(chain, 1024, 0, 250).clump(2).flop;
	magsphases = [origmagsphases[0][30..] ++ origmagsphases[0][..30], origmagsphases[1][30..] ++ origmagsphases[1][..30]];
	magsphases = magsphases.size.switch(
		1, {magsphases ++ origmagsphases[1]},
		2, {magsphases},
		{[magsphases, origmagsphases[1]]}
	);
	magsphases = magsphases.flop.flatten;
	chain = PackFFT(chain, 1024, magsphases, 0, 250, 0);
	(adjVol * ((IFFT(chain)*mix.linlin(0,1,0,1))+(input*mix.linlin(0,1,1,0))));
} ], [ 'sdf', {arg mix=0, adjVol=1;
	var chain, origmagsphases, magsphases, input;
	input = \in;
	chain = FFT(LocalBuf(1024, input.numChannels), input);
	origmagsphases = UnpackFFT(chain, 1024, 0, 250).clump(2).flop;
	magsphases = [origmagsphases[0].differentiate, origmagsphases[1].differentiate];
	magsphases = magsphases.size.switch(
		1, {magsphases ++ origmagsphases[1]},
		2, {magsphases},
		{[magsphases, origmagsphases[1]]}
	);
	magsphases = magsphases.flop.flatten;
	chain = PackFFT(chain, 1024, magsphases, 0, 250, 0);
	(adjVol * ((IFFT(chain)*mix.linlin(0,1,0,1))+(input*mix.linlin(0,1,1,0))));
} ], [ 'srv', {arg mix=0, adjVol=1;
	var chain, origmagsphases, magsphases, input;
	input = \in;
	chain = FFT(LocalBuf(1024, input.numChannels), input);
	origmagsphases = UnpackFFT(chain, 1024, 0, 250).clump(2).flop;
	magsphases = [origmagsphases[0].reverse, origmagsphases[1].reverse];
	magsphases = magsphases.size.switch(
		1, {magsphases ++ origmagsphases[1]},
		2, {magsphases},
		{[magsphases, origmagsphases[1]]}
	);
	magsphases = magsphases.flop.flatten;
	chain = PackFFT(chain, 1024, magsphases, 0, 250, 0);
	(adjVol * ((IFFT(chain)*mix.linlin(0,1,0,1))+(input*mix.linlin(0,1,1,0))));
} ], [ 'ssq', {arg mix=0, adjVol=1;
	var chain, magsphases, input;
	input = \in;
	chain = FFT(LocalBuf(1024, input.numChannels), input);
	magsphases = UnpackFFT(chain, 1024);
	magsphases = magsphases.collect(_.sqrt);
	chain = PackFFT(chain, 1024, magsphases);
	(adjVol * ((IFFT(chain)*mix.linlin(0,1,0,1))+(input*mix.linlin(0,1,1,0))));
} ], [ 'gate', {arg gates=1, adjVol=1; 
	Gate.ar(\in, gates.round(1))*adjVol} ], [ 'gain', {arg gain=0; 
	(\in*gain.dbamp)} ], [ 'lmt', {arg mix = 1 , thresh= -1 , ratio=10 , attack=0.002 , release=0.002, gain=0 , outLevel=0 ; 
	var sig;
sig = Compander.ar(\in*gain.dbamp, thresh: thresh.dbamp, slopeBelow: 1, slopeAbove: 1/ratio, clampTime: attack, relaxTime: release)*outLevel.dbamp;
	((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'cmp', {arg mix = 1 , thresh= -12 , ratio= 2 , attack=0.001 , release=0.048, preLevel=0 , gain=4 ; 
var sig;
sig = Compander.ar(\in*preLevel.dbamp, thresh: thresh.dbamp, slopeBelow: 1, slopeAbove: 1/ratio, clampTime: attack, relaxTime: release)*gain.dbamp;
((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'lpc1', {arg mix=0 , num=64 , adjVol=0 ;
var signal;
signal = LPCError.ar(\in, num);
adjVol.dbamp * ((signal*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'rm1', {arg mix=0 , freq=493.8833 , sinamp=0.5 , lagTime=10 , adjVol=0; 
var signal;
	signal = \in*SinOsc.ar(freq.lag(lagTime),0,sinamp);
adjVol.dbamp * ((signal*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'rm2', {arg mix=0 , sinamp=0.5 , lagTime=0.1 , adjVol=0; 
	var signal, freq, hasFreq; 
# freq, hasFreq = Pitch.kr(\in, ampThreshold: 0.0, median: 7);
signal = \in*SinOsc.ar(freq.lag(lagTime),0,sinamp);
adjVol.dbamp * ((signal*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'rm3', {arg mix=0 , sinamp=0.5 , lagTime=0.1 , adjVol=0;
var signal, freq, hasFreq, amp; 
# freq, hasFreq = Pitch.kr(\in, ampThreshold: 0.0, median: 7);
amp = Amplitude.kr(\in, 0.05, 0.05);
signal = \in*SinOsc.ar(freq.lag(lagTime),0,sinamp*amp);
adjVol.dbamp * ((signal*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'bpf', {arg freq=600 , q=30 , vol=0 , mix=1 , lagTime=40 ; 
var signal; 
signal = BPF.ar(\in, freq, q.lag(lagTime))*q.lag(lagTime).linlin(0.01,30,10,1);
vol.dbamp * ((signal*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'chr2', {arg mix=0 , depth=0.1 , predelay=0.08 , speed=0.05 , ph_diff=0.5 , lagTime=0.1 ;
var input, sig, modulators, numDelays;
numDelays = 24;
input = \in * numDelays.reciprocal;
modulators = Array.fill(numDelays, {arg i;
LFPar.kr(speed * rrand(0.94, 1.06), ph_diff * i, depth.lag(lagTime), predelay.lag(lagTime));}); 
sig = DelayC.ar(input, 7.0, modulators);
sig = sig.sum;
sig = sig * 3;
((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'cut1', {arg freq=3 , decayTime=0.025 ; 
	var trig;
	trig = Decay.kr(Impulse.kr(freq), decayTime);
	Gate.ar(\in, trig)} ], [ 'cut2', {arg freq=3 , decayTime=0.025 ; 
	var trig;
	trig = Decay.kr(Dust.kr(freq), decayTime);
	Gate.ar(\in, trig)} ], [ 'cut3', {arg freq=3 , decayTime=0.025 ,
	minFreq=0.1 , maxFreq=10 ; 
	var trig;
	trig = Decay.kr(Impulse.kr(LFNoise1.kr(freq).linlin(-1,1,minFreq,maxFreq)), decayTime);
	Gate.ar(\in, trig)} ], [ 'gdy2', {arg mix=0 , maxMul=1 , ampdist=2 , durDist=3 , adParam=1 , ddParam=1 , ampScale=0.5 , durScale=0.0 , kNum=40 , transp=1 , rate=1 ; 
	var freq, hasFreq, amp, minFreq, sig, latch1, latch2, latch3, maxDelay, in;
	maxDelay=12;
	in = \in;
#freq, hasFreq = Pitch.kr(in, minFreq: 20, ampThreshold: 0.0, median: 7);
latch1 = Latch.ar(freq, Impulse.kr(rate));
latch2 = DelayL.kr(Latch.ar(freq, Impulse.kr(rate)), maxDelay, 1/rate);
latch3 = (latch1.cpsmidi - (latch1.cpsmidi - latch2.cpsmidi)).midicps.min(20000).max(20);
amp = Amplitude.kr(in, 0.05, 0.05).lag(0.1);
minFreq = ((latch3 * transp).fold2(1000)).abs.max(20);
sig = Gendy1.ar(ampdist.max(0).min(6), durDist, adParam, ddParam, minFreq,(minFreq*maxMul).min(1000),ampScale,durScale,50,kNum)*amp;
((sig*mix.linlin(0,1,0,1))+(in*mix.linlin(0,1,1,0)))
} ], [ 'cct2', {arg match=0.05 , gendur=0.01 , freeze=0 , zcr=1 , genfreq=400 , genamp= 0.02 ; 
var concat, control, amp;
control= Mix(Gendy3.ar(3,5,1.0,1.0,(Array.fill(5,{LFNoise0.kr(1.3.rand,1,2)})*genfreq), genamp, gendur, 5, mul:0.1));
amp=Amplitude.ar(control);
concat= Concat2.ar(control,\in,1.0,1.0,1.0,match,freeze,zcr,0.0,1.0,1.0);
10*concat*amp} ], [ 'cct3', {arg match=0.5, zcr=0.5 , lffreq=0.1 , sinadd=50 ;
var concat, control;
control= Saw.ar(SinOsc.kr(LFNoise0.kr(lffreq,3,4.5),0,10,sinadd));
concat= Concat2.ar(control,\in,2.0,2.0,2.0,match,0,zcr,1.0,0.5);
concat;
} ], [ 'mrk2', {arg mix=0 , rec=1 , time=2 ;
	var sig, table;
	table=10;
	sig = MarkovSynth.ar(\in, rec, time, table);
	((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))} ], [ 'mrk3', {arg mix=0 , rec=1 , time=5 ;
	var sig, table;
	table=3;
	sig = MarkovSynth.ar(\in, rec, time, table);
	((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))} ], [ 'gdy3', {arg mix=0 , maxMul=1 , ampdist=2 , durDist=3 , adParam=1 , ddParam=1 , ampScale=0.5 , durScale=0.0 , kNum=40 , transp=1 , rate=1 ; 
var freq, hasFreq, amp, minFreq, sig, latch1, latch2, latch3, maxDelay, in;
in = \in;
maxDelay=12;
#freq, hasFreq = Pitch.kr(in, minFreq: 20, ampThreshold: 0.0, median: 7);
latch1 = Latch.ar(freq, Impulse.kr(rate));
latch2 = DelayL.kr(Latch.ar(freq, Impulse.kr(rate)), maxDelay, 1/rate);
latch3 = (latch1.cpsmidi - (latch1.cpsmidi - latch2.cpsmidi)).midicps.min(20000).max(20);
amp = Amplitude.kr(in, 0.05, 0.05).lag(0.1);
minFreq = ((latch3 * transp).fold2(1000)).abs.max(20);
sig = Gendy1.ar(ampdist.max(0).min(6), durDist, adParam, ddParam, minFreq,(minFreq*maxMul).min(1000),ampScale,durScale,50,kNum)*amp;
((sig*mix.linlin(0,1,0,1))+(in*mix.linlin(0,1,1,0))) + (in)
} ], [ 'limiter', {arg mix = 1 , level= 1 ;
	var sig;
	sig = Limiter.ar(\in, level);
	((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));
} ], [ 'limiterst', {arg mix = 1 , level= 1 ;
	var sig;
	sig = Limiter.ar(\in, level);
	sig = ((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));
} ], [ 'volgate', {arg thresh = 0.006, 
	time = 1.152, amp = 1;
	var input, trig;
	input = \in;
	trig = (input.abs > thresh);
	SelectX.ar(trig.lag(time), [DC.ar(0), input]) * amp;
} ], [ 'noisegate', {arg thresh = 0.006, 
	atk = 0.01, rel = 0.1 , amp = 1;
	var input;
	input = \in;
	Compander.ar(input, input, thresh, 10, 1, atk, rel) * amp;
} ], [ 'echo', {arg delayTime=0.08, mix=0.5, 
	adjVol=1; 
	var drysig, wetsig;
	drysig = \in;
	wetsig = CombL.ar(\in, 12, delayTime, 0);
	((drysig * mix.linlin(0,1,1,0)) + (wetsig * mix))*adjVol;
} ], [ 'convrevbfenc2_10in', {arg mix=1.0 ,
	pan1_1=0 , pan2_1=0 , elev1=0 ,
	pan1_2=0 , pan2_2=0 , elev2=0 ,
	pan1_3=0 , pan2_3=0 , elev3=0 ,
	pan1_4=0 , pan2_4=0 , elev4=0 ,
	pan1_5=0 , pan2_5=0 , elev5=0 ,
	pan1_6=0 , pan2_6=0 , elev6=0 ,
	pan1_7=0 , pan2_7=0 , elev7=0 ,
	pan1_8=0 , pan2_8=0 , elev8=0 ,
	pan1_9=0 , pan2_9=0 , elev9=0 ,
	pan1_10=0 , pan2_10=0 , elev10=0 ,
	wComp=0 ;
	var input, output, dryoutput, elevation, spksDeg, sig;
	var w1,x1,y1,z1,w2,x2,y2,z2,w3,x3,y3,z3,w4,x4,y4,z4,w5,x5,y5,z5;
	var w6,x6,y6,z6,w7,x7,y7,z7,w8,x8,y8,z8,w9,x9,y9,z9,w10,x10,y10,z10;
	var lag=0.1;
	sig = \in * Line.kr(0,1,0.1);
	#w1,x1,y1,z1 = BFEncode2.ar(sig[0], pan1_1.lag(lag),
		pan2_1.lag(lag), elev1.degrad.lag(lag));
	#w2,x2,y2,z2 = BFEncode2.ar(sig[1], pan1_2.lag(lag), pan2_2.lag(lag),
		elev2.degrad.lag(lag));
	#w3,x3,y3,z3 = BFEncode2.ar(sig[2], pan1_3.lag(lag), pan2_3.lag(lag),
		elev3.degrad.lag(lag));
	#w4,x4,y4,z4 = BFEncode2.ar(sig[3], pan1_4.lag(lag), pan2_4.lag(lag),
		elev4.degrad.lag(lag));
	#w5,x5,y5,z5 = BFEncode2.ar(sig[4], pan1_5.lag(lag), pan2_5.lag(lag),
		elev5.degrad.lag(lag));
	#w6,x6,y6,z6 = BFEncode2.ar(sig[5], pan1_6.lag(lag), pan2_6.lag(lag),
		elev6.degrad.lag(lag));
	#w7,x7,y7,z7 = BFEncode2.ar(sig[6], pan1_7.lag(lag), pan2_7.lag(lag),
		elev7.degrad.lag(lag));
	#w8,x8,y8,z8 = BFEncode2.ar(sig[7], pan1_8.lag(lag), pan2_8.lag(lag),
		elev8.degrad.lag(lag));
	#w9,x9,y9,z9 = BFEncode2.ar(sig[8], pan1_8.lag(lag), pan2_8.lag(lag),
		elev8.degrad.lag(lag));
	#w10,x10,y10,z10 = BFEncode2.ar(sig[9], pan1_8.lag(lag), pan2_8.lag(lag),
		elev8.degrad.lag(lag));
input = [(w1+w2+w3+w4+w5+w6+w7+w8+w9+w10), (x1+x2+x3+x4+x5+x6+x7+x8+x9+x10),
		(y1+y2+y3+y4+y5+y6+y7+y8+y9+y10), (z1+z2+z3+z4+z5+z6+z7+z8+z9+z10)];
output = \convrev;
spksDeg = ~speakDeg.degrad;
elevation = ~elev;
	dryoutput = BFDecode1.ar(input[0], input[1], input[2], input[3], spksDeg, elevation.degrad);
	output = BFDecode1.ar(output[0], output[1], output[2], output[3], spksDeg, elevation.degrad);
	(output*mix.max(0).min(1)) + (dryoutput*mix.linlin(0,1,1,0));
} ], [ 'convrevbfenc1_10in', {arg mix=1.0 ,
	azi1=0 , elev1=0 , rho1=0 ,
	azi2=0 , elev2=0 , rho2=0 ,
	azi3=0 , elev3=0 , rho3=0 ,
	azi4=0 , elev4=0 , rho4=0 ,
	azi5=0 , elev5=0 , rho5=0 ,
	azi6=0 , elev6=0 , rho6=0 ,
	azi7=0 , elev7=0 , rho7=0 ,
	azi8=0 , elev8=0 , rho8=0 ,
	azi9=0 , elev9=0 , rho9=0 ,
	azi10=0 , elev10=0 , rho10=0 ;
	var input, output, dryoutput, elevation, spksDeg, sig;
	var w1,x1,y1,z1,w2,x2,y2,z2,w3,x3,y3,z3,w4,x4,y4,z4,w5,x5,y5,z5;
	var w6,x6,y6,z6,w7,x7,y7,z7,w8,x8,y8,z8,w9,x9,y9,z9,w10,x10,y10,z10;
	var lag=0.1;
	sig = \in * Line.kr(0,1,0.1);
	#w1,x1,y1,z1 = BFEncode1.ar(sig[0], azi1.degrad.lag(lag), elev1.degrad.lag(lag),
		rho1.lag(lag));
	#w2,x2,y2,z2 = BFEncode1.ar(sig[1], azi2.degrad.lag(lag), elev2.degrad.lag(lag),
		rho2.lag(lag));
	#w3,x3,y3,z3 = BFEncode1.ar(sig[2], azi3.degrad.lag(lag), elev3.degrad.lag(lag),
		rho3.lag(lag));
	#w4,x4,y4,z4 = BFEncode1.ar(sig[3], azi4.degrad.lag(lag), elev4.degrad.lag(lag),
		rho4.lag(lag));
	#w5,x5,y5,z5 = BFEncode1.ar(sig[4], azi5.degrad.lag(lag), elev5.degrad.lag(lag),
		rho5.lag(lag));
	#w6,x6,y6,z6 = BFEncode1.ar(sig[5], azi6.degrad.lag(lag), elev6.degrad.lag(lag),
		rho6.lag(lag));
	#w7,x7,y7,z7 = BFEncode1.ar(sig[6], azi7.degrad.lag(lag), elev7.degrad.lag(lag),
		rho7.lag(lag));
	#w8,x8,y8,z8 = BFEncode1.ar(sig[7], azi8.degrad.lag(lag), elev8.degrad.lag(lag),
		rho8.lag(lag));
	#w9,x9,y9,z9 = BFEncode1.ar(sig[8], azi9.degrad.lag(lag), elev9.degrad.lag(lag),
		rho9.lag(lag));
	#w10,x10,y10,z10 = BFEncode1.ar(sig[9], azi10.degrad.lag(lag), elev10.degrad.lag(lag),
		rho10.lag(lag));
input = [(w1+w2+w3+w4+w5+w6+w7+w8+w9+w10), (x1+x2+x3+x4+x5+x6+x7+x8+x9+x10),
		(y1+y2+y3+y4+y5+y6+y7+y8+y9+y10), (z1+z2+z3+z4+z5+z6+z7+z8+z9+z10)];
output = \convrev;
spksDeg = ~speakDeg.degrad;
elevation = ~elev;
	dryoutput = BFDecode1.ar(input[0], input[1], input[2], input[3], spksDeg, elevation.degrad);
	output = BFDecode1.ar(output[0], output[1], output[2], output[3], spksDeg, elevation.degrad);
	(output*mix.max(0).min(1)) + (dryoutput*mix.linlin(0,1,1,0));
} ] ]