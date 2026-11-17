// Radicles: libraries for chain and track presets (Document Model, step 4).
// A chain or track preset can hold effects, blocks and wavetables from
// several libraries (an effect from Main, another from RAVE). csload,
// csmorph, tload and tmorph first make sure every library they need is
// loaded, adding the missing ones to those already selected (as imp [Main,
// RAVE] would) and posting it, so everything in the preset loads. A library
// is never unloaded by a preset. Which library: the one holding the effect,
// block or wavetable, found from the library files; when several hold the
// same name, one already selected is used, else Main, else the first.
// Modulators need nothing: they are read from every library already.

+ RadUnit {

	// a library's Files folder: Main's in Radicles, the others in RadiclesLibs
	*libraryFilesPath {|lib|
		^if(lib == "Main", { Radicles.mainPath +/+ "Files" }, { Radicles.libPath +/+ lib +/+ "Files" })
	}

	// the libraries selected now (imp). Library names are Strings: compared
	// with includesEqual, never includes (identity)
	*selectedLibraries {
		var excluded = Radicles.excludeLibs ? [];
		^Radicles.allLibs.reject{|lib| excluded.includesEqual(lib) }
	}

	// the names in one kind of library file, for every library, Main first:
	// [[lib, Set[names]], ...]. folder: "SynthFiles" or "DataFiles"
	*prLibraryNames {|folder, file|
		^(["Main"] ++ this.libraries).collect{|lib|
			var path = this.libraryFilesPath(lib) +/+ folder +/+ file, names = Set[];
			if(File.exists(path), {
				(try { path.load } { nil }) !? {|arr|
					if(arr.isArray, { arr.do{|pair| if(pair.isArray and: { pair.notEmpty }, { names.add(pair[0].asSymbol) }) } }) };
			});
			[lib, names]
		}
	}

	// the libraries holding these effects, blocks and wavetables
	*librariesFor {|filters, blocks, tables|
		var selected = this.selectedLibraries, need = [];
		[["SynthFiles", "Filter.scd", filters], ["SynthFiles", "Block.scd", blocks],
			["DataFiles", "Wavetables.scd", tables]].do{|item|
			var index;
			if((item[2] ? []).notEmpty, {
				index = this.prLibraryNames(item[0], item[1]);
				item[2].do{|name|
					var libs = index.select{|pair| pair[1].includes(name.asSymbol) }.collect(_[0]);
					(libs.detect{|lib| selected.includesEqual(lib) } ? libs.first) !? {|lib| need = need.add(lib) };
				};
			});
		};
		^need.asSet.asArray
	}

	// the libraries a chain needs
	*chainLibraries {|chain|
		^this.librariesFor((chain[\fx] ? ()).values.collect(_[\def]).reject(_.isNil))
	}

	// the libraries a track preset needs (without the parts left out)
	*trackLibraries {|track, nosrc = false, nofx = false|
		var source = if(nosrc, { [] }, { track[\source] ? [] });
		^this.librariesFor(
			if(nofx, { [] }, { ((track[\chain] ? ())[\fx] ? ()).values.collect(_[\def]).reject(_.isNil) }),
			source.collect(_[\def]).reject(_.isNil),
			source.collect(_[\data]).select(_.isKindOf(Symbol)))
	}

	// load the libraries in libs not selected yet, keeping those selected (as
	// imp would, posted as code). Returns the libraries loaded.
	*useLibraries {|libs, who = "preset"|
		var selected = this.selectedLibraries, missing = libs.reject{|lib| selected.includesEqual(lib) }, all;
		if(missing.isEmpty, { ^[] });
		all = (["Main"] ++ Radicles.allLibs.reject{|lib| lib == "Main" }).select{|lib|
			selected.includesEqual(lib) or: { missing.includesEqual(lib) } };
		("Radicles.selLibs(" ++ all.cs ++ ");").radcode;
		(who ++ ": loaded " ++ if(missing.size == 1, { "library " }, { "libraries " }) ++ missing.join(", ") ++
			" (imp [" ++ all.join(", ") ++ "])").postln;
		^missing
	}
}
