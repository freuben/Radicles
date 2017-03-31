[ [ 'pch', {arg pitchRatio=1.0, pitchDispertion=0.0, timeDispersion=0.0001, lag=5.0, winSize=0.02;
		PitchShift.ar(\in, winSize, pitchRatio.lag(lag), pitchDispertion, timeDispersion); } ], [ 'pch2', {arg centerpitch=440,bus=0, pitchDispertion=0.0, timeDispersion=0.0, lag=0.1, winSize=0.1; 
	var freq, hasFreq; 
	# freq, hasFreq = Pitch.kr(SoundIn.ar(bus.round(1)), ampThreshold: 0.0, median: 7);
	PitchShift.ar(\in, winSize, (freq/centerpitch).lag(lag), pitchDispertion, timeDispersion) } ], [ 'pch3', {arg centerpitch=440, bus=0, adjvol=4, pitchDispertion=0.0, timeDispersion=0.0001, 
	lag=0.1, winSize=0.02; 
	var freq, hasFreq, input, amp;
	input = SoundIn.ar(bus.round(1));
	amp = Amplitude.kr(input, 0.05, 0.05);
	# freq, hasFreq = Pitch.kr(input, ampThreshold: 0.0, median: 7);
	PitchShift.ar(\in, winSize, (freq/centerpitch).lag(lag), pitchDispertion, 
		timeDispersion)*(amp*adjvol) } ] ]