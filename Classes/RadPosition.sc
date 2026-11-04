// Radicles: where a sound-file block plays (Document Model, step 2, PR 3;
// position option A, decided 3 Nov 2026). A block definition that plays a
// buffer through time adds one line before its PlayBuf, with the same
// arguments:
//   RadPosition.kr(\position, \buffer, rate, trigger, startPos, 1);
// Block.play replaces \position with the block's control bus (~position1 for
// block1), as it replaces \buffer, and the line writes the play position, in
// seconds of the file, to that bus. Block.position reads it (blkget 1
// position). It follows rate changes, modulated rate, retriggers and loops,
// and costs one Phasor per block. Blocks without the line report nothing.

RadPosition {
	// loop: 1 wraps at the end of the file, 0 stops there (like PlayBuf's
	// loop); a number or a control
	*kr {|bus, buffer, rate = 1, trigger = 1, startPos = 0, loop = 1|
		var frames = BufFrames.kr(buffer), start, phase, end, pos, looped;
		//PlayBuf reads startPos when it starts and on each trigger only
		start = Latch.ar(K2A.ar(startPos.clip(0, 1) * frames), K2A.ar(trigger) + Impulse.ar(0));
		looped = if(loop.isNumber, { loop > 0 }, { loop > 0 });
		end = if(looped.isKindOf(Boolean), { if(looped, { frames }, { 1e9 }) },
			{ Select.kr(looped, [1e9, frames]) });
		//phase from the start position, so it stays below the file's length
		//when looping (precision over long loops)
		phase = Phasor.ar(trigger, rate * BufRateScale.kr(buffer), 0, end, 0);
		pos = phase + start;
		pos = if(looped.isKindOf(Boolean), {
			if(looped, { pos.wrap(0, frames) }, { pos.clip(0, frames) })
		}, { Select.ar(looped, [pos.clip(0, frames), pos.wrap(0, frames)]) });
		^ReplaceOut.kr(bus, A2K.kr(pos) / BufSampleRate.kr(buffer));
	}
}
