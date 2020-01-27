[ [ 'pch', {arg pitchRatio=1.0 -> [ -12, 12, {|val| val.round(0.4).midiratio }], pitchDispertion=0.0 -> [ 0.01, 2.01, \exp, {|val| (val-0.01)} ], timeDispersion=0.0001
 -> [ 0.01, 2.01, \exp, {|val| (val-0.01)} ], winSize=0.02
 -> [ 0.01, 3 ];
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
} ], [ 'ctp', {arg rate=1 -> [(1/12),10], pitchDispersion=0.0 -> [0.01,2.01, \exp, {|val| (val-0.01)}], timeDispersion=0.0001 -> [0.01,2.01, \exp, {|val| (val-0.01)}], lag=0.01 -> [0.01,15],
	windowSize=0.02 -> [0.01,3];
	var freq, hasFreq, latch1, latch2, latch3, input, maxDelay=12;
	input = \in;
	#freq, hasFreq = Pitch.kr(input);
	latch1 = Latch.ar(freq, Impulse.kr(rate));
	latch2 = DelayL.kr(Latch.ar(freq, Impulse.kr(rate)), maxDelay, 1/rate);
	latch3 = (latch1.cpsmidi - (latch1.cpsmidi - latch2.cpsmidi)).midicps.min(20000).max(20);
	PitchShift.ar(input, windowSize, (latch1/latch3)[0].lag(lag), pitchDispersion,
 timeDispersion);
} ], [ 'cbg', {arg decayTime=0.5 -> [0.0,10], freq=10 -> [1.0,1000, \exp], lag=5.0 -> [0.01,15];
	CombL.ar(\in * (LFNoise0.ar(freq.lag(lag)) > 0), 0.2, 0.2, decayTime);} ], [ 'lpf', {arg freq=20000 -> [10,20000, \exp], lag=0.1 -> [0,30]; LPF.ar(\in, freq.lag(lag));} ], [ 'convrev1', {arg mix=1.0 -> [ 0, 1];
	var input, output;
	input = \in * Line.kr(0,1,0.1);
	output = \convrev;
	(output*mix.max(0).min(1)) + (input*mix.linlin(0,1,1,0));
} ], [ 'gdy1', {arg mix=0 -> [0,1], maxMul=1 -> [1, 8], ampdist=2 -> [0, 6, {|val| val.round(1)}], durDist=3 -> [0.0001, 8, \exp], adParam=1 -> [0.0001, 1, \exp], ddParam=1 -> [0.0001, 1, \exp], ampScale=0.5 -> [0,1], durScale=0.0 -> [0,1], kNum=40 -> [2, 50], transp=1 -> [0.1, 4]; 
	var freq, hasFreq, amp, minFreq, sig, in;
	in = \in;
#freq, hasFreq = Pitch.kr(in, minFreq: 20, ampThreshold: 0.0, median: 7);
amp = Amplitude.kr(in, 0.05, 0.05).lag(0.1);
minFreq = ((freq[0] * transp).fold2(1000)).abs.max(20);
sig = Gendy1.ar(ampdist.max(0).min(6), durDist, adParam, ddParam, minFreq,(minFreq*maxMul).min(1000),ampScale,durScale,50,kNum)*amp;
((sig*mix.linlin(0,1,0,1))+(in*mix.linlin(0,1,1,0)))
} ], [ 'cct1', {arg mix=0 -> [0,1], match=0.3 -> [0,0.5], lffreq=0.1 -> [0.01,1.0], sinadd = 300;
var concat, control;
control= SinOsc.ar(0.5)*Saw.ar(SinOsc.kr(LFNoise0.kr(lffreq,3,4.5),0,50,sinadd));
concat= Concat2.ar(control,\in,4.0,4.0,4.0,match,0,0.5,1.0,0.5,0.0);
((concat*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'fds1', {arg mix=0 -> [0,1], alpha= -0.001 -> [-0.1,0.1, {|val| val.round(0.001)}], adjVol=0 -> [-20,20]; 
var kernel, kernel2, signal, signal2, signal3, freq, hasFreq;
signal = \in;
#freq, hasFreq = Pitch.kr(signal);
kernel= WeaklyNonlinear.ar(signal,0,1,1,freq*2,0,0,alpha,3,0,0);
kernel2= WeaklyNonlinear2.ar(signal,0,1,1,freq*2,0,0,alpha,3,0,0);
signal2 = Convolution.ar(kernel+signal,kernel2, 2048, 0.0005) * EnvGen.kr(Env.asr(0.01, 1.0, 0.01), Amplitude.kr(signal));
signal3 = Limiter.ar(signal2);
	adjVol * ((signal3*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'fds2', {arg mix=0 -> [0,1], num=64 -> [64,1], adjVol=0-> [-20,20];
var kernel, signal, signal2, signal3, in;
in = \in;
kernel = LPCError.ar(in, in.linlin(-1,1,num,1));
signal = Convolution.ar(in,kernel, 2048);
signal2 = LPCError.ar(signal, in.linlin(-1,1,1,num));
signal3 = ((signal2+(in.clip2(0.05)*0.05))).clip2(1.0);
adjVol.dbamp * ((signal3*mix.linlin(0,1,0,1))+(in*mix.linlin(0,1,1,0)));} ], [ 'mga', {arg thresh=0->[0.01,50.01,\exp, {|val| val-0.01}], adjVol=1->[0,4];
var chain, frameSize=2048, input;
	input = \in;
chain = FFT(LocalBuf(frameSize, input.numChannels), input);
chain = PV_MagAbove(chain, thresh); 
(adjVol * IFFT(chain));} ], [ 'mrk1', {arg mix=0 -> [0,1], rec=1, table=2, time=2;
	var sig;
	sig = MarkovSynth.ar(\in, rec, time, table);
	((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))} ], [ 'chr1', {arg mix=0 -> [0,1], depth=0.1 -> [0,2], predelay=0.08 -> [0,2], speed=0.05 -> [0.01, 10], ph_diff=0.5 -> [0, 6.2831], lagTime=0.1 -> [0,10];
var input, sig, modulators, numDelays = 12;
input = \in * numDelays.reciprocal;
modulators = Array.fill(numDelays, {arg i;
LFPar.kr(speed * rrand(0.94, 1.06), ph_diff * i, depth.lag(lagTime), predelay.lag(lagTime));}); 
sig = DelayC.ar(input, 2.0, modulators);
sig = sig.sum;
sig = sig * 3;
((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'dl2', {arg mul=1->[0,12], freq=0.25->[0.25,20.0, \exp], lag=0.1->[0.01,15], decayTime=0->[0,24], lag2=1->[0.01,15]; CombL.ar(\in,12,LFNoise1.kr(freq.lag(lag), 1*(mul.lag(lag2)/2), 0+(mul.lag(lag2)/2)).lag(lag),decayTime)} ], [ 'sclip', {arg vol=0 -> [-inf, 6, \db, 0, -inf, " dB" ];
	(\in*vol.dbamp).softclip;
 } ], [ 'pch5', {arg pitchRatio=1.0 -> [ -6, 6, {|val| val.round(0.4).midiratio }], pitchDispertion=0.0 -> [ 0.01, 2.01, \exp, {|val| (val-0.01)} ], timeDispersion=0.0001
 -> [ 0.01, 1.01, \exp, {|val| (val-0.01)} ], winSize=0.02
 -> [ 0.01, 2 ];
	PitchShift.ar(\in, winSize, pitchRatio, pitchDispertion, timeDispersion)*SinOsc.ar(440);
} ], [ 'hpf', {arg freq=20000 -> [10,20000, \exp], lag=0.1 -> [0,30]; HPF.ar(\in, freq.lag(lag));} ], [ 'eq8', {arg freq1=30 -> [20,20000,\exp], freq2=80 -> [20,20000,\exp], freq3=200 -> [20,20000,\exp], freq4=500 -> [20,20000,\exp], freq5=1200 -> [20,20000,\exp], freq6=3500 -> [20,20000,\exp], freq7=10000 -> [20,20000,\exp], freq8=17000 -> [20,20000,\exp], gain1=0 -> [-24,24], gain2=0 -> [-24,24], gain3=0 -> [-24,24], gain4=0 -> [-24,24], gain5=0 -> [-24,24], gain6=0 -> [-24,24], gain7=0 -> [-24,24], gain8=0 -> [-24,24], q1=0.71 -> [0.1, 10, \exp], q2=1.10 -> [0.1, 2, \exp], q3=0.98 -> [0.1, 100, \exp], q4=0.71 -> [0.1, 100, \exp], q5=0.71 -> [0.1, 100, \exp], q6=0.71 -> [0.1, 100, \exp], q7=0.71 -> [0.1, 2, \exp], q8=0.71 -> [0.1, 10, \exp], on1=0 -> [0,1, \lin, 1, 0], on2=1 -> [0,1, \lin, 1, 0], on3=1 -> [0,1, \lin, 1, 0], on4=1 -> [0,1, \lin, 1, 0], on5=1 -> [0,1, \lin, 1, 0], on6=1 -> [0,1, \lin, 1, 0], on7=1 -> [0,1, \lin, 1, 0], on8=0 -> [0,1, \lin, 1, 0], lag=0.5-> [0,10], masterGain=0 -> [ -inf, 6, 'db', 0, -inf, " dB" ], lagOn=0.1 -> [0,10];
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
} ], [ 'ds1', {arg drive=1->[1,200], adjVol=1->[0.2,2.0, {|val| val.round(0.01)-0.01}], lag=0.1->[0.01,15]; var lagdr; lagdr = drive.lag(lag);((\in*lagdr).distort * (lagdr.sqrt/lagdr))*adjVol} ], [ 'ds2', {arg drive=1->[1,200], adjVol=1->[0.2,2.0, {|val| val.round(0.01)-0.01}], lag=0.1->[0.01,15]; var lagdr; lagdr = drive.lag(lag);((\in*lagdr).softclip * (lagdr.sqrt/lagdr))*adjVol} ], [ 'ds3', {arg drive=1->[1,200], adjVol=1->[0.2,2.0, {|val| val.round(0.01)-0.01}], lag=0.1->[0.01,15]; var lagdr; lagdr = drive.lag(lag);((\in*lagdr).clip2 * (lagdr.sqrt/lagdr))*adjVol} ], [ 'ds4', {arg drive=1->[1,200], adjVol=1->[0.2,2.0, {|val| val.round(0.01)-0.01}], lag=0.1->[0.01,15]; var lagdr; lagdr = drive.lag(lag);((\in*lagdr).fold2(1) * (lagdr.sqrt/lagdr))*adjVol} ], [ 'ds5', {arg drive=1->[1,200], adjVol=1->[0.2,2.0, {|val| val.round(0.01)-0.01}], lag=0.1->[0.01,15]; var lagdr; lagdr = drive.lag(lag);((\in*lagdr).wrap2(1) * (lagdr.sqrt/lagdr))*adjVol} ], [ 'fvrb', {arg mix=0->[0,1], room=0->[0,1], damp=0.0->[0,1], lag=0.0->[0,15], adjVol=1->[0.2,2.0,{|val| val.round(0.01)-0.01}]; 
	FreeVerb.ar(\in, mix.lag(lag), room.lag(lag), damp.lag(lag))*adjVol} ], [ 'fvrb2', {arg mix=0->[0,1], room=0->[0,1], damp=0.0->[0,1], lag=0.0->[0,15], adjVol=1->[0.2,2.0,{|val| val.round(0.01)-0.01}]; 
	var sig;
	sig = \in;
	FreeVerb2.ar(sig[0], sig[1], mix.lag(lag), room.lag(lag), damp.lag(lag))*adjVol} ], [ 'gvrb1', {arg mix=0.5->[0,1], adjVol=1->[0.2,2.0,{|val| val.round(0.01)-0.01}]; 
	var drysig, wetsig;
	drysig = \in;
	wetsig= GVerb.ar(\in, 5, 0.6, 0.62, 0.48, 15, -6, -11, -13,300,-40.dbamp);
	(((drysig/2) * mix.linlin(0,1,1,0)) + (wetsig * mix))*0.95*adjVol} ], [ 'gvrb2', {arg mix=0.5->[0,1], adjVol=1->[0.2,2.0,{|val| val.round(0.01)-0.01}]; 
	var drysig, wetsig;
	drysig = \in;
	wetsig= GVerb.ar(\in, 16, 1.24, 0.1, 0.95, 15, -3, -15, -17, 300,-40.dbamp);
	(((drysig/2) * mix.linlin(0,1,1,0)) + (wetsig * mix))*0.95*adjVol} ], [ 'gvrb3', {arg mix=0.5->[0,1], adjVol=1->[0.2,2.0,{|val| val.round(0.01)-0.01}]; 
	var drysig, wetsig;
	drysig = \in;
	wetsig= GVerb.ar(\in, 80, 4.85, 0.41, 0.19, 15, -3, -9, -11, 300,-40.dbamp);
	(((drysig/2) * mix.linlin(0,1,1,0)) + (wetsig * mix))*0.95*adjVol} ], [ 'gvrb4', {arg mix=0.5->[0,1], adjVol=1->[0.2,2.0,{|val| val.round(0.01)-0.01}]; 
	var drysig, wetsig;
	drysig = \in;
	wetsig= GVerb.ar(\in, 243, 1, 0.1, 0.19, 15, -3, -9, -11, 300,-40.dbamp);
	(((drysig/2) * mix.linlin(0,1,1,0)) + (wetsig * mix))*0.95*adjVol} ], [ 'gvrb5', {arg mix=0.5->[0,1], adjVol=1->[0.2,2.0,{|val| val.round(0.01)-0.01}]; 
	var drysig, wetsig;
	drysig = \in;
	wetsig= GVerb.ar(\in, 300, 103, 0.43, 0.51, 15, -5, -26, -20, 300,-40.dbamp);
	(((drysig/2) * mix.linlin(0,1,1,0)) + (wetsig * mix))*0.95*adjVol} ], [ 'amp', {arg bus=1->[1,8,{|val| val.round(1)}], adjVol=1->[1,30, {|val| val.round(0.01)-0.01}], lag=0.1->[0,10]; (\in*Amplitude.kr(SoundIn.ar((bus-1).round(1))).lag(lag))*adjVol
} ], [ 'dl1', {arg delayTime=0->[0,12], decayTime=1->[0,24]; CombL.ar(\in, 12, delayTime, decayTime)} ], [ 'cvt1', {arg mix=0->[0,1], track=1->[1,8,{|val| val.round(1)}], frameSize=1024 -> [512,8192,{|val| val.round(512) }], adjVol=1->[0,5,{|val| val.round(0.01)-0.01}]; 
	var signal, input;
	signal = Select.ar(track-1, [Ndef(\track1).ar, Ndef(\track2).ar, Ndef(\track3).ar, Ndef(\track4).ar, Ndef(\track5).ar, Ndef(\track6).ar, Ndef(\track7).ar, Ndef(\track8).ar]);
	input = \in;
	(Convolution.ar(input,signal, frameSize,adjVol*0.15)*mix)
		+(input*mix.linlin(0,1,1,0))} ], [ 'cvt2', {arg mix=0->[0,1], track=1->[1,8,{|val| val.round(1)}], frameSize=1024 -> [512,8192,{|val| val.round(512) }], adjVol=1->[0,5,{|val| val.round(0.01)-0.01}]; 
	var signal, input;
	signal = Select.ar(track-1, [Ndef(\track1).ar, Ndef(\track2).ar, Ndef(\track3).ar, Ndef(\track4).ar, Ndef(\track5).ar, Ndef(\track6).ar, Ndef(\track7).ar, Ndef(\track8).ar]);
	input = \in;
	(Convolution.ar(signal,input, frameSize,adjVol*0.15)*mix)
		+(input*mix.linlin(0,1,1,0))} ], [ 'cvi1', {arg mix=0->[0,1], soundin=1->[1,8,{|val| val.round(1)}], frameSize=1024 -> [512,8192,{|val| val.round(512) }], adjVol=1->[1,30,{|val| val.round(0.01)-0.01}]; 
	var signal, input;
	signal = SoundIn.ar((soundin+1).round(1));
	input = \in;
	(Convolution.ar(input,signal,frameSize,adjVol*0.15)*mix)
	+(input*mix.linlin(0,1,1,0))} ], [ 'cvi2', {arg mix=0->[0,1], soundin=1->[1,8,{|val| val.round(1)}], frameSize=1024 -> [512,8192,{|val| val.round(512) }], adjVol=1->[1,30,{|val| val.round(0.01)-0.01}]; 
	var signal, input;
	signal = SoundIn.ar((soundin+1).round(1));
	input = \in;
	(Convolution.ar(signal, input, frameSize,adjVol*0.15)*mix)
	+(input*mix.linlin(0,1,1,0))} ], [ 'mmi', {arg mix=0->[0,1], soundin=1->[1,8, {|val| val.round(1)}], adjVol=1->[1,4]; 
var input, inA, chainA, chainB, chain, frameSize=2048;
	input = \in;
	inA = SoundIn.ar((soundin-1).round(1));
chainA = FFT(LocalBuf(frameSize), input);
chainB = FFT(LocalBuf(frameSize), inA);
chain = PV_MagMul(chainA, chainB);
Mix.new(adjVol * ((IFFT(chain)*mix.linlin(0,1,0,1))+(input*mix.linlin(0,1,1,0))));
} ], [ 'mmt', {arg mix=0->[0,1], track=1->[1,8, {|val| val.round(1)}], adjVol=1->[0,5,{|val| val.round(0.01)-0.01}]; 
var input, inA, chainA, chainB, chain, frameSize=2048;
	input = \in;
	inA = Select.ar(track-1, [Ndef(\track1).ar, Ndef(\track2).ar, Ndef(\track3).ar, Ndef(\track4).ar, Ndef(\track5).ar, Ndef(\track6).ar, Ndef(\track7).ar, Ndef(\track8).ar]);
chainA = FFT(LocalBuf(frameSize, input.numChannels), input);
chainB = FFT(LocalBuf(frameSize, inA.numChannels), inA);
chain = PV_MagMul(chainA, chainB);
(adjVol * ((IFFT(chain)*mix.linlin(0,1,0,1))+(input*mix.linlin(0,1,1,0))));
} ], [ 'frz1', {arg freeze=0->[0,1,{|val| val.round(1)}], adjVol=1->[1,4];
var chain, input, frameSize=2048;
	input = \in;
chain = FFT(LocalBuf(frameSize, input.numChannels), input);
chain = PV_MagFreeze(chain, freeze); 
	(adjVol * IFFT(chain));} ], [ 'frz2', {arg thresh=1->[0,2], adjVol=1->[1,4], lagTime=0.1->[0,0.4];
var freeze, chain, input, frameSize=2048;
	input = \in;
	chain = FFT(LocalBuf(frameSize, input.numChannels), input);
	freeze = Onsets.kr(chain, thresh).lag(lagTime);
	chain = PV_MagFreeze(chain, freeze); 
	(adjVol * IFFT(chain));} ], [ 'srt', {arg mix=0->[0,1], adjVol=1->[0,4];
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
} ], [ 'sdf', {arg mix=0->[0,1], adjVol=1->[0,4];
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
} ], [ 'srv', {arg mix=0->[0,1], adjVol=1->[0,4];
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
} ], [ 'ssq', {arg mix=0->[0,1], adjVol=1->[0,4];
	var chain, magsphases, input;
	input = \in;
	chain = FFT(LocalBuf(1024, input.numChannels), input);
	magsphases = UnpackFFT(chain, 1024);
	magsphases = magsphases.collect(_.sqrt);
	chain = PackFFT(chain, 1024, magsphases);
	(adjVol * ((IFFT(chain)*mix.linlin(0,1,0,1))+(input*mix.linlin(0,1,1,0))));
} ], [ 'gate', {arg gates=1->[0,1,{|val| val.round}], adjVol=1->[0,10]; 
	Gate.ar(\in, gates.round(1))*adjVol} ], [ 'gain', {arg gain=0->[ -inf, 24, \db, 0, -inf,  "dB" ]; 
	(\in*gain.dbamp)} ], [ 'lmt', {arg mix = 1 -> [0,1], thresh= -1 -> [0,-53], ratio=10 -> [10, 100, \exp, {|val| val.round(1)}], attack=0.002 -> [0,0.1], release=0.002-> [0.002, 2], gain=0 -> [-10,40], outLevel=0 -> [-10, 1]; 
	var sig;
sig = Compander.ar(\in*gain.dbamp, thresh: thresh.dbamp, slopeBelow: 1, slopeAbove: 1/ratio, clampTime: attack, relaxTime: release)*outLevel.dbamp;
	((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'cmp', {arg mix = 1 -> [0,1], thresh= -12 -> [0,-53], ratio= 2 -> [1, 30, \exp, {|val| val.round(0.1)}], attack=0.001 -> [0,0.2], release=0.048-> [0.005, 5], preLevel=0 -> [-10,40], gain=4 -> [-20,50]; 
var sig;
sig = Compander.ar(\in*preLevel.dbamp, thresh: thresh.dbamp, slopeBelow: 1, slopeAbove: 1/ratio, clampTime: attack, relaxTime: release)*gain.dbamp;
((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'lpc1', {arg mix=0 -> [0,1], num=64 -> [64,1], adjVol=0 -> [-20,20];
var signal;
signal = LPCError.ar(\in, num);
adjVol.dbamp * ((signal*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'rm1', {arg mix=0 -> [0,1], freq=493.8833 -> [20,20000, \exp], sinamp=0.5 -> [0,1], lagTime=10 -> [0,10], adjVol=0-> [-20,20]; 
var signal;
	signal = \in*SinOsc.ar(freq.lag(lagTime),0,sinamp);
adjVol.dbamp * ((signal*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'rm2', {arg mix=0 -> [0,1], sinamp=0.5 -> [0,1], lagTime=0.1 -> [0,10], adjVol=0-> [-20,20]; 
	var signal, freq, hasFreq; 
# freq, hasFreq = Pitch.kr(\in, ampThreshold: 0.0, median: 7);
signal = \in*SinOsc.ar(freq.lag(lagTime),0,sinamp);
adjVol.dbamp * ((signal*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'rm3', {arg mix=0 -> [0,1], sinamp=0.5 -> [0,1], lagTime=0.1 -> [0,10], adjVol=0-> [-20,20];
var signal, freq, hasFreq, amp; 
# freq, hasFreq = Pitch.kr(\in, ampThreshold: 0.0, median: 7);
amp = Amplitude.kr(\in, 0.05, 0.05);
signal = \in*SinOsc.ar(freq.lag(lagTime),0,sinamp*amp);
adjVol.dbamp * ((signal*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'bpf', {arg freq=600 -> [20,20000,\exp], q=30 -> [0.01,30.0], vol=0 -> [-53, 0], mix=1 -> [0,1], lagTime=40 -> [0,10]; 
var signal; 
signal = BPF.ar(\in, freq, q.lag(lagTime))*q.lag(lagTime).linlin(0.01,30,10,1);
vol.dbamp * ((signal*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)));} ], [ 'chr2', {arg mix=0 -> [0,1], depth=0.1 -> [0,2], predelay=0.08 -> [0,2], speed=0.05 -> [0.01, 10], ph_diff=0.5 -> [0, 6.2831], lagTime=0.1 -> [0,10];
var input, sig, modulators, numDelays = 24;
input = \in * numDelays.reciprocal;
modulators = Array.fill(numDelays, {arg i;
LFPar.kr(speed * rrand(0.94, 1.06), ph_diff * i, depth.lag(lagTime), predelay.lag(lagTime));}); 
sig = DelayC.ar(input, 7.0, modulators);
sig = sig.sum;
sig = sig * 3;
((sig*mix.linlin(0,1,0,1))+(\in*mix.linlin(0,1,1,0)))
} ], [ 'cut1', {arg freq=3 -> [0.01, 100, \exp], decayTime=0.025 -> [0.0001,0.1, \exp]; 
	var trig;
	trig = Decay.kr(Impulse.kr(freq), decayTime);
	Gate.ar(\in, trig)} ], [ 'cut2', {arg freq=3 -> [0.01, 100, \exp], decayTime=0.025 -> [0.0001,0.1, \exp]; 
	var trig;
	trig = Decay.kr(Dust.kr(freq), decayTime);
	Gate.ar(\in, trig)} ], [ 'cut3', {arg freq=3 -> [0.01, 100, \exp], decayTime=0.025 -> [0.0001,0.1, \exp],
	minFreq=0.1 -> [0.01, 10, \exp], maxFreq=10 -> [10.01, 50, \exp]; 
	var trig;
	trig = Decay.kr(Impulse.kr(LFNoise1.kr(freq).linlin(-1,1,minFreq,maxFreq)), decayTime);
	Gate.ar(\in, trig)} ] ]