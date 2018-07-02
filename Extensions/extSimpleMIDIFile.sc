+ SimpleMIDIFile {

	pArr { |inst, amp = 0.2, useTempo = true| // amp: amp when velo == 127
		var thisFile, resultArr;
		inst = ( inst ? 'default' ).asCollection;

		if( useTempo ) {
			if( timeMode == 'seconds' )
				{ thisFile = this }
				{ thisFile = this.copy.timeMode_( 'seconds' ); };
		} {
			if( timeMode == 'ticks' )
				{ thisFile = this }
				{ thisFile = this.copy.timeMode_( 'ticks' ); };
		};

			({ |tr|
				var sustainEvents, deltaTimes;
				sustainEvents = thisFile.noteSustainEvents( nil, tr );
				if( sustainEvents.size > 0 )
					{
					sustainEvents = sustainEvents.flop;
					if( useTempo ) {
						deltaTimes = sustainEvents[1].differentiate;
					} {
						// always use 120BPM
						deltaTimes = (sustainEvents[1] / (division*2)).differentiate;
						sustainEvents[6] = sustainEvents[6] / (division*2);
					};
					resultArr = resultArr.add([
						inst.wrapAt( tr + 1 ), deltaTimes ++ [0], [0] ++ sustainEvents[3],
						[\rest] ++ sustainEvents[4], [0] ++ ( sustainEvents[5] / 127 ) * amp, [0] ++ sustainEvents[6]
					]);
					}
					{ nil }
				}!this.tracks).select({ |item| item.notNil });
			^resultArr;
		}

	noteTracks {
		var arr;
		this.tracks.do{|index|
	var events;
	events = this.noteOnEvents( nil, index )[0];
	if(events.notNil, {
		arr = arr.add(events[0]);
	});
};
		^arr;
	}

	pTrack {arg track=1, inst, amp = 0.2;
		var midArr, resultArr;
		midArr = this.pArr(inst, amp);
		if(track.isArray.not, { track = track.asArray});
		resultArr = midArr.atAll(track);
		if(inst.notNil, {
		if(inst.isArray, {
			inst.do{|item, index|
				resultArr[index][0] = item;
			};
		});
		});
		^resultArr;
	}

}

+ Collection {

		arrToPpar {arg repeats=1, extraArgs;
		var pbinds, pbindCont;
		this.do{|item, index|
			pbindCont = [
				instrument: item[0],
				dur: Pseq( item[1], repeats ),
				chan: Pseq( item[2], repeats ),
				midinote: Pseq( item[3], repeats ),
				amp: Pseq( item[4], repeats ),
				sustain: Pseq( item[5], repeats )
			];
			if(extraArgs.notNil, {
			if(extraArgs.rank == 1, {
			pbindCont = pbindCont ++ extraArgs;
				}, {
			pbindCont =	pbindCont ++	extraArgs[index];
				});
			});
			pbinds = pbinds.add(Pbind(*pbindCont);
				);
		};
		^Ppar(pbinds);
	}

}