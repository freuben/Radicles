// Radicles: the document (Document Model, step 1).
// Plain data (Events, Arrays, Symbols, Strings, numbers) describing the
// session, addressed by paths such as track1.fx.2.args.pitchRatio.
// put and removeAt record when an address changed and notify dependants
// (changed(\put, path, value) / changed(\removeAt, path)).
// Files are written as readable SuperCollider code and read back with
// RadLiteral, which only accepts data and never runs the file.

RadDoc {
	classvar current, <formatVersion = 1, <>converters, <>stripPattern = "^(track|bus)[0-9]+$";
	var <data, times, <>clockFunc;

	*initClass { converters = IdentityDictionary.new }

	*new {|data| ^super.new.init(data) }

	init {|argData|
		data = argData ?? { (format: formatVersion, strips: ()) };
		times = Dictionary.new;
		clockFunc = { Main.elapsedTime };
	}

	// the document of the running session
	*current { ^current ?? { current = this.new } }
	*current_ {|doc| current = doc }

	// ---- addresses ----

	*isStripName {|name|
		^name.isKindOf(Symbol) and: { name == \master or: { stripPattern.matchRegexp(name.asString) } }
	}

	// an address (Symbol or String with dots, or an Array) as an Array path;
	// strip names imply strips: track1.fx.2 -> [\strips, \track1, \fx, 2]
	*path {|address|
		var token = {|x|
			if(x.isKindOf(String) or: { x.isKindOf(Symbol) }, {
				x = x.asString;
				if(x.size > 0 and: { x.every(_.isDecDigit) }, { x.asInteger }, { x.asSymbol })
			}, { x })
		};
		var path = case
		{ address.isNil } { [] }
		{ address.isKindOf(Symbol) or: { address.isString } } {
			address.asString.split($.).reject(_.isEmpty).collect(token) }
		{ address.isKindOf(SequenceableCollection) } { address.asArray.collect(token) }
		{ [address] };
		if(path.notEmpty and: { this.isStripName(path[0]) }, { path = [\strips] ++ path });
		^path
	}

	// a path as a readable address: [\strips, \track1, \fx, 2] -> "track1.fx.2"
	*addressString {|path|
		path = this.path(path);
		if(path.size > 1 and: { path[0] == \strips } and: { this.isStripName(path[1]) }, {
			path = path.drop(1) });
		^path.collect(_.asString).join(".")
	}

	*walk {|obj, path|
		path.do{|key|
			obj = case
			{ obj.isKindOf(Dictionary) } { obj.at(key) }
			{ obj.isKindOf(Array) and: { key.isKindOf(Integer) } } { obj[key] }
			{ ^nil };
		};
		^obj
	}

	// ---- get and set ----

	at {|address| ^this.class.walk(data, this.class.path(address)) }

	put {|address, value|
		var path = this.class.path(address), parent;
		if(path.isEmpty, { Error("RadDoc: put needs an address").throw });
		parent = this.prContainer(path.drop(-1));
		if(parent.isKindOf(Array) and: { path.last.isKindOf(Integer).not or: {
			path.last >= parent.size } }, {
			Error("RadDoc: " ++ this.class.addressString(path) ++ " is outside an Array").throw });
		parent.put(path.last, value);
		this.prTouch(path);
		this.changed(\put, path, value);
	}

	removeAt {|address|
		var path = this.class.path(address), parent, old;
		if(path.isEmpty, { ^nil });
		parent = this.class.walk(data, path.drop(-1));
		if(parent.isKindOf(Dictionary).not or: { parent.includesKey(path.last).not }, { ^nil });
		old = parent.removeAt(path.last);
		this.prTouch(path);
		this.changed(\removeAt, path);
		^old
	}

	// when an address (or anything under it) last changed, or nil
	changedAt {|address| ^times[this.class.path(address)] }

	prContainer {|path|
		var obj = data, next;
		path.do{|key, i|
			next = this.class.walk(obj, [key]);
			if(next.isNil, {
				if(obj.isKindOf(Dictionary).not, {
					Error("RadDoc: " ++ this.class.addressString(path.keep(i + 1)) ++
						" cannot be created inside an Array").throw });
				next = (); obj.put(key, next);
			});
			if(next.isKindOf(Dictionary).not and: { next.isKindOf(Array).not }, {
				Error("RadDoc: " ++ this.class.addressString(path.keep(i + 1)) ++
					" holds a value, not an Event or Array").throw });
			obj = next;
		};
		^obj
	}

	prTouch {|path|
		var now = clockFunc.value;
		(path.size + 1).do{|i| times[path.keep(i)] = now };
	}

	// ---- copies, files, comparison ----

	copy { ^this.class.new(data.deepCopy).prTimes(times.deepCopy).clockFunc_(clockFunc) }

	prTimes {|aDictionary| times = aDictionary }

	asLiteral { ^RadLiteral.write(data) }

	header {|kind = "document"|
		^"// Radicles " ++ kind ++ " · format " ++ (data[\format] ? formatVersion) ++
		" · saved " ++ Date.getDate.format("%Y-%m-%d %H:%M:%S")
	}

	write {|path, kind = "document"|
		var text = this.header(kind) ++ "\n" ++ this.asLiteral ++ "\n";
		File.mkdir(path.dirname);
		File.use(path, "w", {|file| file.write(text) });
	}

	// read a document file; posts a warning and returns nil if it cannot be read
	*read {|path|
		var text;
		if(File.exists(path).not, { ("RadDoc: no file " ++ path).warn; ^nil });
		text = File.readAllString(path);
		^this.fromString(text, path)
	}

	*fromString {|text, name = "text"|
		var obj = this.readData(text, name);
		^if(obj.isNil, { nil }, { this.new(obj) })
	}

	// the data in a file's text, checked and converted to the current format,
	// or nil with a warning; also for presets and sessions
	*readData {|text, name = "text"|
		var obj, error;
		try { obj = RadLiteral.read(text) } {|err| error = err.errorString };
		if(error.isNil and: { obj.isKindOf(Dictionary).not }, {
			error = "the file does not hold an Event" });
		if(error.isNil, {
			try { obj = this.convert(obj) } {|err| error = err.errorString };
		});
		if(error.notNil, {
			("RadDoc: " ++ name ++ " not read: " ++ error.replace("ERROR: ", "")).warn;
			^nil
		});
		^obj
	}

	// bring data from an older format up to date with the converters,
	// one format at a time (converters[1] turns format 1 into format 2)
	*convert {|obj, to|
		var version = obj[\format], converter;
		to = to ? formatVersion;
		if(version.isKindOf(Integer).not, { Error("no format number").throw });
		if(version > to, {
			("RadDoc: made with a newer Radicles (format " ++ version ++
				", this one reads " ++ to ++ "); reading what it can").warn;
			^obj
		});
		while({ version < to }, {
			converter = converters[version];
			if(converter.isNil, { Error("no converter from format " ++ version).throw });
			obj = converter.value(obj);
			version = version + 1;
			obj[\format] = version;
		});
		^obj
	}

	// the differences between two documents (or Events): an Array of Events
	// (change: \added | \removed | \changed, path, address, old, new)
	*diff {|a, b, tolerance = 0|
		var result = List.new;
		if(a.isKindOf(RadDoc), { a = a.data });
		if(b.isKindOf(RadDoc), { b = b.data });
		this.prDiff(a, b, [], result, tolerance);
		^result.asArray
	}

	*prDiff {|a, b, path, result, tolerance|
		var union = (), entry = {|change, old, new|
			result.add((change: change, path: path, address: this.addressString(path),
				old: old, new: new)) };
		case
		{ a.isKindOf(Dictionary) and: { b.isKindOf(Dictionary) } } {
			a.keysDo{|k| union[k] = true };
			b.keysDo{|k| union[k] = true };
			RadLiteral.sortedKeys(union).do{|k|
				case
				{ a.includesKey(k).not } {
					result.add((change: \added, path: path ++ [k],
						address: this.addressString(path ++ [k]), new: b[k])) }
				{ b.includesKey(k).not } {
					result.add((change: \removed, path: path ++ [k],
						address: this.addressString(path ++ [k]), old: a[k])) }
				{ this.prDiff(a[k], b[k], path ++ [k], result, tolerance) };
			};
		}
		{ a.isKindOf(Array) and: { b.isKindOf(Array) } and: { a.size == b.size } } {
			a.do{|x, i| this.prDiff(x, b[i], path ++ [i], result, tolerance) };
		}
		{ this.prSame(a, b, tolerance).not } { entry.(\changed, a, b) };
	}

	*prSame {|a, b, tolerance|
		^case
		{ a.isKindOf(Float) and: { b.isKindOf(Float) } and: { a.isNaN and: { b.isNaN } } } { true }
		{ a.isNumber and: { b.isNumber } } {
			(a == b) or: { tolerance > 0 and: { absdif(a, b) <= tolerance } } }
		{ a.class != b.class and: { (a.isString and: { b.isString }).not } } { false }
		{ a == b }
	}
}
