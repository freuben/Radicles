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
 } ] ]