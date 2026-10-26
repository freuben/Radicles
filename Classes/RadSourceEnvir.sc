// The environment Radicles.withSource runs code in. It holds only
// ~radSource; every other ~variable is read from and written to the
// environment around it (`outer`), so code behaves exactly as without it.
// Routines started inside it inherit it, so a command's later steps
// (waiting for the server, for example) keep their source.
//
// It is deliberately not an Environment subclass: ~x = value is a primitive
// that writes straight into an Environment, while for any other object it
// falls back to currentEnvironment.put, which forwards here.

RadSourceEnvir {
	var <outer, <source;

	*run {arg source, func;
		^this.new.init(currentEnvironment, source).use(func);
	}

	init {arg outerEnvir, newSource;
		outer = outerEnvir;
		source = newSource;
	}

	use {arg function;
		var result, saveEnvir;
		saveEnvir = currentEnvironment;
		currentEnvironment = this;
		protect {
			result = function.value(this);
		} {
			currentEnvironment = saveEnvir;
		};
		^result
	}

	at {arg key;
		^if(key == \radSource, { source }, { outer.at(key) });
	}

	put {arg key, value;
		if(key == \radSource, { source = value }, { outer.put(key, value) });
	}

	removeAt {arg key;
		^if(key == \radSource, { source = nil }, { outer.removeAt(key) });
	}

	// anything else (keys, includesKey, ...) is answered by the outer environment
	doesNotUnderstand {arg selector ... args;
		^outer.performList(selector, args);
	}
}
