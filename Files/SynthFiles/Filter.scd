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
} ], [ 'ctp', {arg rate=1, pitchDispersion=0.0, timeDispersion=0.0001, lag=0.01,
 windowSize=0.02;
	var freq, hasFreq, latch1, latch2, latch3, input, maxDelay=12;
	input = \in;
	#freq, hasFreq = Pitch.kr(input);
	latch1 = Latch.ar(freq, Impulse.kr(rate));
	latch2 = DelayL.kr(Latch.ar(freq, Impulse.kr(rate)), maxDelay, 1/rate);
	latch3 = (latch1.cpsmidi - (latch1.cpsmidi - latch2.cpsmidi)).midicps.min(20000).max(20);
	PitchShift.ar(input, windowSize, (latch1/latch3)[0].lag(lag), pitchDispersion,
 timeDispersion);
} ], [ 'cbg', {arg decayTime=0.5, freq=10, lag=5.0;
	CombL.ar(\in * (LFNoise0.ar(freq.lag(lag)) > 0), 0.2, 0.2, decayTime);} ], [ 'lpf', {arg freq=20000, lag=0.1; LPF.ar(\in, freq.lag(lag));} ], [ 'convrev1', {arg mix=1.0 ;
	var input, output;
	input = \in * Line.kr(0,1,0.1);
	output = \convrev;
	(output*mix.max(0).min(1)) + (input*mix.linlin(0,1,1,0));
} ], [ 'pch_test', {arg pitchRatio=1.0 , pitchDispertion=0.0 , timeDispersion=0.0001
 , winSize=0.02
 ;
	PitchShift.ar(\in, winSize, pitchRatio, pitchDispertion, timeDispersion)*SinOsc.ar();
 } ], [ 'frz2', {arg thresh=0 , adjVol=1, lagTime=0.1,pan=0;
var freeze, chain, frameSize=2048, sig;
chain = FFT(LocalBuf(frameSize), Mix.new(\in));
freeze = Onsets.kr(chain, thresh).lag(lagTime);
chain = PV_MagFreeze(chain, freeze);
	Pan2.ar(adjVol * IFFT(chain);, pan);} ], [ 'gdy1', {arg mix=0 , maxMul=1, ampdist=2, durDist=3, adParam=1, ddParam=1, ampScale=0.5, durScale=0.0, kNum=40, transp=1;
	var freq, hasFreq, amp, minFreq, sig;
#freq, hasFreq = Pitch.kr(\in, minFreq: 20, ampThreshold: 0.0, median: 7);
amp = Amplitude.kr(\in, 0.05, 0.05).lag(0.1);
minFreq = ((freq[0] * transp).fold2(1000)).abs.max(20);
sig = Gendy1.ar(ampdist.max(0).min(6), durDist, adParam, ddParam, minFreq,(minFreq*maxMul).min(1000),ampScale,durScale,50,kNum)*amp;
((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'cct1', {arg mix=0 , match=0.3 , lffreq=0.1 , sinadd = 300;
var concat, control;
control= SinOsc.ar(0.5)*Saw.ar(SinOsc.kr(LFNoise0.kr(lffreq,3,4.5),0,50,sinadd));
concat= Concat2.ar(control,\in,4.0,4.0,4.0,match,0,0.5,1.0,0.5,0.0);
((concat*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'fds1', {arg mix=0 , alpha= -0.001 , adjVol=1;
var kernel, kernel2, signal, signal2, signal3, freq, hasFreq;
signal = \in;
#freq, hasFreq = Pitch.kr(signal);
kernel= WeaklyNonlinear.ar(signal,0,1,1,freq*2,0,0,alpha,3,0,0);
kernel2= WeaklyNonlinear2.ar(signal,0,1,1,freq*2,0,0,alpha,3,0,0);
signal2 = Convolution.ar(kernel+signal,kernel2, 2048, 0.0005) * EnvGen.kr(Env.asr(0.01, 1.0, 0.01), Amplitude.kr(signal));
signal3 = Limiter.ar(signal2*adjVol);
	adjVol * ((signal3*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'fds2', {arg mix=0 , num=64 , adjVol=1;
var kernel, signal, signal2, signal3;
kernel = LPCError.ar(\in, \in.linlin(-1,1,num,1));
signal = Convolution.ar(\in,kernel, 2048);
signal2 = LPCError.ar(signal, \in.linlin(-1,1,1,num));
signal3 = ((signal2+(\in.clip2(0.05)*0.05))).clip2(1.0);
	adjVol * ((signal3*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'mga', {arg thresh=0 , adjVol=1, pan=0;
var chain, frameSize=2048;
chain = FFT(LocalBuf(frameSize), Mix.new(\in));
chain = PV_MagAbove(chain, thresh);
	Pan2.ar(adjVol * IFFT(chain), pan);} ], [ 'mrk1', {arg mix=0 , rec=1, table=2, time=2;
	var sig;
	sig = MarkovSynth.ar(\in, rec, time, table);
	((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))} ], [ 'chr1', {arg mix=0, depth=0.1, predelay=0.08, speed=0.05, ph_diff=0.5, lagTime=0.1;
var input, sig, modulators, numDelays = 12;
input = \in * numDelays.reciprocal;
modulators = Array.fill(numDelays, {arg i;
LFPar.kr(speed * rrand(0.94, 1.06), ph_diff * i, depth.lag(lagTime), predelay.lag(lagTime));});
sig = DelayC.ar(input, 2.0, modulators);
sig = sig.sum;
sig = sig * 3;
((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'dl2', {arg mix=0 , mul=1, freq=0.25, lag=0.1, decayTime=0, lag2=1; var sig;
	sig = CombL.ar(\in,12,LFNoise1.kr(freq.lag(lag), 1*(mul.lag(lag2)/2), 0+(mul.lag(lag2)/2)).lag(lag),decayTime);
((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'sclip', {arg vol=0 ;
	(\in*vol.dbamp).softclip;
 } ], [ 'pch5', {arg pitchRatio=1.0 , pitchDispertion=0.0 , timeDispersion=0.0001
 , winSize=0.02
 ;
	PitchShift.ar(\in, winSize, pitchRatio, pitchDispertion, timeDispersion)*SinOsc.ar(440);
} ], [ 'pchmod', {arg pitchRatio=1.0 , pitchDispertion=0.0 , timeDispersion=0.0001
 , winSize=0.02
	, modFreq=100 , shape=0 ;
	PitchShift.ar(\in, winSize, pitchRatio, pitchDispertion, timeDispersion)*
	Select.ar(shape, [
	SinOsc.ar(modFreq),
	Saw.ar(modFreq),
		Pulse.ar(modFreq)]);
} ] ]