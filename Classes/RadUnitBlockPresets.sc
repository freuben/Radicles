// Radicles: block presets (Document Model, step 2, PR 2). A block preset is
// a block unit saved in the same folders as effect presets, under kind block:
//   <RadiclesFiles>/Settings/Presets/units/block/<def>/<name>.scd
// (and in the block's library, as for effects). It carries the block's
// values, its modulators, and its sound file (by name and Play folder) or
// wavetable (by name); never its position or its block number.
//   blksave 1 warmLoop          block 1's unit -> your presets for its definition
//   blkload 1 warmLoop          block 1 to the preset (same definition only)
//   blkload 1 warmLoop nobuf    the preset's values, keeping block 1's file or table
//   blkload 1 default           the definition's defaults, no modulators
// Rules (decided 3 Nov 2026): a preset loads only into a block already
// playing its definition; recording blocks (rec, recpv, recloop, ...) have no
// presets; a recorded buffer (rec: N) is not a file, so a preset never holds
// one, and a block playing one keeps it. What a preset doesn't carry, the
// block keeps.
// Keeping a recording (decided 4 Nov 2026): blksave 1 warmLoop keep [file]
// writes the recording block 1 plays to SoundFiles/Play/recordings/<file>.wav
// (file: rec_<date>_<time> unless given), plays the block from that file
// (a crossfade), then saves the preset, which now holds the file like any
// other: (play: file, folder: 'recordings').

+ RadUnit {

	*blockTag {|block| ^("block" ++ block).asSymbol }

	// does this block definition record (RecordBuf) rather than play?
	*recordsDef {|def|
		var func = try { SynthFile.read(\block, def) } { nil };
		^func.respondsTo(\def) and: { func.def.sourceCode.asString.contains("RecordBuf") }
	}

	// the buffer forms of a unit (a vosc block has several)
	*prBufferForms {|buffer|
		^case
		{ buffer.isNil } { [] }
		{ buffer.isKindOf(Dictionary) } { [buffer] }
		{ buffer.asArray }
	}

	*prHasRecBuffer {|buffer| ^this.prBufferForms(buffer).any{|form| form.isKindOf(Dictionary) and: { form[\rec].notNil } } }

	// the block unit at block N for a preset, or nil with the reason posted
	*prPresetBlock {|block, verb = "save"|
		var tag = this.blockTag(block), unit = this.readBlock(tag);
		case
		{ unit.isNil } {
			("No block " ++ block ++ " to " ++ verb ++ " (blocks that are playing; patterns have no presets)").warn; ^nil }
		{ this.recordsDef(unit[\def]) } {
			("Block " ++ block ++ " records (" ++ unit[\def] ++ "); recording blocks have no presets").warn; ^nil };
		^unit
	}

	// what a block preset holds: the unit without a recorded buffer
	*blockPresetUnit {|unit|
		var copy = unit.deepCopy;
		if(this.prHasRecBuffer(copy[\buffer]), { copy.removeAt(\buffer) });
		^copy
	}

	// blksave N name [keep [file]]: asks before replacing one of your own
	// (refused where there is no window to ask in). keep first writes the
	// recording the block plays to a file and plays the block from it (see
	// keepRecording). Returns the unit saved (without keep), true (keep: the
	// save happens in a moment), or nil when refused.
	*saveBlockPreset {|block, name, action, keep = false, fileName|
		var unit = this.prPresetBlock(block), write, save, keepIt;
		if(unit.isNil, { ^nil });
		if(this.validPresetName(name).not, {
			("Preset names use letters, digits, - and _ only: " ++ name).warn; ^nil });
		if(name.asSymbol == \default, {
			("default is the block's own default values (blkload " ++ block ++ " default); choose another name").warn; ^nil });
		keepIt = keep and: { this.prHasRecBuffer(unit[\buffer]) };
		if(keep and: { keepIt.not }, {
			("Block " ++ block ++ " plays no recording: keep has nothing to keep; saving as usual").postln });
		if(keepIt and: { this.prKeepFileProblem(unit, fileName) !? {|problem| problem.warn; true } ? false }, { ^nil });
		write = {|unitNow|
			var path;
			unitNow = this.blockPresetUnit(unitNow);
			path = this.writePreset(unitNow, name);
			("Saved preset " ++ name ++ " for " ++ unitNow[\def] ++ ": " ++ path).postln;
			action.(path);
		};
		save = {
			if(keepIt, {
				{
					this.keepRecording(block, fileName) !? {|kept| write.(kept) };
				}.fork(AppClock);
			}, { write.(unit) });
		};
		if(File.exists(this.presetPath(\block, unit[\def], name)), {
			if(GUI.current.notNil, {
				{ Window.warnQuestion("Preset " ++ name ++ " for " ++ unit[\def] ++ " exists. Replace it?", save) }.defer;
			}, {
				("Preset " ++ name ++ " for " ++ unit[\def] ++ " exists; not replaced (no window to ask in)").warn;
			});
		}, save);
		^if(keepIt, { true }, { unit })
	}

	// ---- keeping a recording ----

	// recordings are kept in this Play folder (SoundFiles/Play/recordings)
	*recordingsFolder { ^\recordings }

	*recordingsPath { ^BStore.getDirPath(\audio, "Play" ++ Platform.pathSeparator, this.recordingsFolder) }

	// the file name keep uses when none is given: rec_<yymmdd>_<hhmmss>
	*recordingFileName { ^"rec_" ++ Date.getDate.stamp }

	// why the recording of unit cannot be kept under fileName, or nil
	*prKeepFileProblem {|unit, fileName|
		var form = this.prBufferForms(unit[\buffer]).detect{|f| f.isKindOf(Dictionary) and: { f[\rec].notNil } },
		info = form !? { (Block.recBufInfo ? [])[form[\rec] - 1] };
		^case
		{ form.isNil } { "No recording to keep" }
		{ info.isNil } { "Recording " ++ form[\rec] ++ " no longer exists" }
		{ info[2] != \audio } { "keep works with audio recordings (@); @" ++ form[\rec] ++ " is a pv recording" }
		{ fileName.notNil and: { this.validPresetName(fileName).not } } {
			"File names use letters, digits, - and _ only: " ++ fileName }
		{ fileName.notNil and: { File.exists(this.recordingsPath +/+ fileName ++ ".wav") } } {
			"recordings/" ++ fileName ++ ".wav exists; choose another name" }
		{ nil };
	}

	// write the recording block N plays to recordings/<fileName>.wav and play
	// the block from that file, with the same values and modulators. Call
	// inside a Routine. Returns the block's unit afterwards, or nil.
	*keepRecording {|block, fileName|
		var tag = this.blockTag(block), unit = this.readBlock(tag), form, buf, path, kept, problem;
		if(unit.isNil, { ("No block " ++ block ++ " playing").warn; ^nil });
		problem = this.prKeepFileProblem(unit, fileName);
		if(problem.notNil, { problem.warn; ^nil });
		fileName = (fileName ? this.recordingFileName).asString;
		if(File.exists(this.recordingsPath +/+ fileName ++ ".wav"), {
			("recordings/" ++ fileName ++ ".wav exists; choose another name").warn; ^nil });
		form = this.prBufferForms(unit[\buffer]).detect{|f| f.isKindOf(Dictionary) and: { f[\rec].notNil } };
		buf = BStore.buffByID(Block.recBuf(form[\rec]));
		if(buf.isNil, { ("Recording " ++ form[\rec] ++ " has no buffer").warn; ^nil });
		File.mkdir(this.recordingsPath);
		path = this.recordingsPath +/+ fileName ++ ".wav";
		buf.write(path, "wav", "float");
		Server.default.sync;
		if(File.exists(path).not, { ("Could not write " ++ path).warn; ^nil });
		("Kept recording " ++ form[\rec] ++ " as recordings/" ++ fileName ++ ".wav").postln;
		kept = unit.deepCopy;
		kept[\buffer] = (play: fileName.asSymbol, folder: this.recordingsFolder);
		if(this.applyBlock(block, kept).not, { ^nil });
		Server.default.sync;
		Radicles.aZ !? {|az| az.updateSourceWin(tag) };
		^this.readBlock(tag)
	}

	// the unit a preset gives block N (playing def), or nil with the reason
	// posted. The block keeps what the preset doesn't carry, and with nobuf
	// (or for default) its buffer and table.
	*blockPresetFor {|block, def, name, nobuf = false|
		var unit, current, others;
		if(this.presetExists(\block, def, name).not, {
			others = this.allPresets(\block).select{|pair| pair[1] == name.asSymbol }.collect(_[0]);
			if(others.notEmpty, {
				("block " ++ block ++ " plays " ++ def ++ "; " ++ name ++ " is a preset for " ++ others.join(", ")).warn;
			}, {
				("No preset " ++ name ++ " for " ++ def).warn;
			});
			^nil
		});
		unit = if(name.asSymbol == \default, { this.defaultUnit(\block, def) }, { this.readPreset(\block, def, name) });
		if(unit.isNil, { ^nil });
		current = this.readBlock(this.blockTag(block)) ? ();
		[\buffer, \data].do{|key|
			if(nobuf or: { name.asSymbol == \default } or: { unit[key].isNil }
				or: { key == \buffer and: { this.prHasRecBuffer(current[key]) } }, {
				if(current[key].notNil, { unit[key] = current[key] }, { unit.removeAt(key) });
			});
		};
		^unit
	}

	// bring block N to unit (same definition), like apply for effects: a
	// different sound file or table plays the block again (Block.play, a
	// crossfade) with the unit's values; then values and modulators are set
	// where they differ. Call inside a Routine. Returns true when applied.
	*applyBlock {|block, unit, setFunc|
		var tag = this.blockTag(block), current, pairs, cond, folder, before;
		current = this.readBlock(tag);
		if(current.isNil, { ("No block " ++ block ++ " playing").warn; ^false });
		if(current[\def] != unit[\def], {
			("block " ++ block ++ " plays " ++ current[\def] ++ "; the preset is for " ++ unit[\def]).warn; ^false });
		if(RadDoc.diff((data: current[\data], buffer: current[\buffer]),
			(data: unit[\data], buffer: unit[\buffer])).notEmpty, {
			pairs = [];
			(unit[\args] ? ()).keysValuesDo{|key, value|
				if(value.isKindOf(Dictionary).not, { pairs = pairs ++ [key, value] }) };
			//a sound file is found in its own Play folder, which may not be the
			//one in use: use it while the block starts, then put it back
			folder = this.prBufferForms(unit[\buffer]).detect{|form|
				form.isKindOf(Dictionary) and: { form[\folder].notNil } } !? {|form| form[\folder] };
			before = BStore.playFolder;
			if(folder.notNil, { BStore.playFolder = folder });
			cond = Condition(false);
			Block.play(block, unit[\def], Block.bufferArgOf(unit[\buffer]), pairs, unit[\data],
				action: { cond.test = true; cond.signal });
			cond.wait;
			BStore.playFolder = before;
			current = this.readBlock(tag);
			if(current.isNil, { ("Block " ++ block ++ " did not start again").warn; ^false });
		});
		this.prApplyArgsMods(Ndef(tag), current, unit, setFunc);
		^true
	}

	// blkload N name [nobuf]: loads a preset into the block, then rebuilds its
	// window if one is open
	*loadBlockPreset {|block, name, nobuf = false, action|
		var current = this.prPresetBlock(block, "load into"), unit;
		if(current.isNil, { ^false });
		unit = this.blockPresetFor(block, current[\def], name, nobuf);
		if(unit.isNil, { ^false });
		{
			if(this.applyBlock(block, unit), {
				Server.default.sync;
				Radicles.aZ !? {|az| az.updateSourceWin(this.blockTag(block)) };
			});
			action.();
		}.fork(AppClock);
		^true
	}
}
