// Assemblage: recording and master output mapping.
// Part of the Assemblage class: the class and its variables are in Assemblage.sc.

+ Assemblage {
	prepareRecording {arg headerFormat = "wav", sampleFormat = "int16";
		var recPath, timestamp, recTracks, dash;
		dash = "/";
		timestamp = Date.localtime.format("%d_%m_%Y_%H_%M_%S");
		Platform.case(
			\windows,   {dash = "\\"; }
		);
		recPath = Radicles.soundFilePath ++ dash ++  "Record" ++ dash;
		if(recStates.isNil, { recStates = 0!(mixTrackNames.size-1) ++ [1] });
		recStates.do({|item, index|
			if(item == 1, {
				recTracks = recTracks.add(("space" ++
					mixTrackNames[index].asString.capitalise).asSymbol);
			});
		});
		recBStoreArr = [];
		if(recInputArr.isNil, {
			recInputArr = [ "Inputs 1-2" ];
		});
		recTracks = recTracks ++ recInputArr;
		recTracks.do{|item|
			var recNumChans;
			if(item.asString.find("Inputs").isNil, {
				recNumChans = Ndef(item).numChannels;
			}, {
				recNumChans = 2;
			});
			recBStoreArr = 	recBStoreArr.add([\alloc, ("alloc" ++ BStore.allocCount).asSymbol,
				[server.sampleRate.nextPowerOfTwo, recNumChans]];);
			BStore.allocCount = BStore.allocCount + 1;
		};
		"//prepare recording".radpost;
		BStore.addAll(recBStoreArr, {|buf|
			recBStoreArr.do{|it, in|
				(BStore.buffStrByID(it) ++ ".write(" ++
					(recPath ++ timestamp ++ " " ++ in ++ " " ++ recTracks[in].asString
						++ ".wav").cs ++ ", " ++ headerFormat.cs ++ ", " ++ sampleFormat.cs
					++ ", 0, 0, true);").radcode;
			};
		});
	}

	startRecording {var recTracks;
		recStates.do({|item, index|
			if(item == 1, {
				recTracks = recTracks.add(("space" ++
					mixTrackNames[index].asString.capitalise).asSymbol);
			});
		});
		if(recInputArr.isNil, {
			recInputArr = [ "Inputs 1-2" ];
		});
		recTracks = recTracks ++ recInputArr;
		recTracks.do{|item, index|
			var recBus, busArr;
			if(item.asString.find("Inputs").isNil, {
				recBus = ("Ndef.ar(" ++ item.cs ++ ",  " ++ Ndef(item).numChannels ++ ")");
			}, {
				busArr = item.split($ )[1].split($-).asString.interpret;
				recBus = ("SoundIn.ar(" ++ (busArr-1) ++ ")");
			});
			("//recording: " ++ recTracks[index]).radpost;
			("Ndef(" ++ ("record_" ++ index).asSymbol.cs ++ ", {\n\tDiskOut.ar("
				++ BStore.buffStrByID(recBStoreArr[index]) ++ ", " ++ recBus ++ " ) }); ")
			.radcode;
		};
	}

	stopRecording {
		{
			"//stop recording".radpost;
			recBStoreArr.do{|item, index|
				("Ndef(" ++ ("record_" ++ index).asSymbol.cs ++ ").free;").radcode;
				server.sync;
				(BStore.buffStrByID(item) ++ ".close;").radcode;
				BStore.removeID(item);
			};
		}.fork;
	}

	mastOutSynth	{
		var arr2, arr3, selArr;
		if(mastOutArr.notNil, {
			if(mastOutArr.flat.includes(1), {
				arr2 = mastOutArr.flop.collect({|item| item.collect({|it, in|
					if(it != 0, {it = "out[" ++ in ++ "]" }, {it =  it}); }); });
				arr2 = arr2.collect{|item| item.select({|it| it != 0 }) };
				arr2.do{|item, index| if(item.isEmpty.not, {selArr = selArr.add(index)}) };
				arr2 = arr2.copyRange(0, selArr.last);
				arr3 = arr2.collect{|item|
					if(item.size == 1, {
						item = item[0].asString;
					}, {
						if(item.isEmpty, {
							item = "DC.ar(0)";
						}, {
							item =  (item.asString ++ ".sum";)
						});
				}); };
			}, {
				arr3 = "DC.ar(0)"!Ndef(\master).numChannels ;
			});
			("Ndef('masterOut', {var out; \nout = Ndef.ar('spaceMaster', " ++
				Ndef(\spaceMaster).numChannels ++ ");\n\t" ++	arr3.asString ++ ";\n\t});")
			.radcode;
		});
	}

	mapOutFunc {var mastOutArr2, funcSource;
		{mastOutArr2 = {0!oiOuts} ! oiIns;
			oiIns.collect{|item| item }.do{|it, ind| mastOutArr2[ind][it] = 1 };
			if(Ndef(\masterOut).source.isNil, {
				if(mastOutArr != mastOutArr2, {
					funcSource = Ndef(\spaceMaster).source;
					"Ndef('spaceMaster').clear;".radcode;
					Ndef(\spaceMaster).fadeTime.yield;
					server.sync;
					("Ndef('spaceMaster', " ++ funcSource.cs ++ ");").radcode;
					/*"Ndef('spaceMaster')[1] = \\filter -> {arg in; (in).clip2};".radcode;*/
					"Ndef('masterOut').play;".radcode;
					"Ndef('masterOut').reshaping = 'elastic';".radcode;
					this.mastOutSynth;
				});
			}, {
				this.mastOutSynth;
			});
		}.fork(AppClock);
	}

	mapOuts {arg outArr = [1,2];
		var thisOutArr, outs;
		outs = outArr - 1;
		thisOutArr = {0!oiOuts} ! oiIns;
		thisOutArr.do{|item, index| item[outs[index] ] = 1 };
		mastOutArr = thisOutArr;
		this.mapOutFunc;
	}

	setDirInRec {arg stereoInIndices = [0];
		var inputs;
		(1..server.options.numInputBusChannels).pairsDo{|it1, it2|
			inputs = inputs.add(("Inputs " ++ it1 ++ "-" ++ it2))};
		recInputArr = inputs.atAll(stereoInIndices);
		^recInputArr;
	}

	setRecording {
		var noUIFunc;
		if(recordingValBut.isNil, {recordingValBut = 1});
		case
		{recordingValBut == 1} {this.prepareRecording}
		{recordingValBut == 2} {this.startRecording}
		{recordingValBut == 0} {this.stopRecording};
		if(mixerWin.notNil, {
			if(mixerWin.visible.notNil, {
				recordingButton.value = recordingValBut;
			});
		});
		recordingValBut = (recordingValBut+1)%3;
	}
}
