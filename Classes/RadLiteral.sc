// Radicles: plain data as readable SuperCollider code, and back.
// RadLiteral.write(obj) turns Events, Arrays, numbers, Symbols, Strings,
// Chars, nil, true and false into code that a person can read and edit.
// RadLiteral.read(string) turns such code back into data with its own small
// parser: the text is never compiled or run, so a file can only ever give
// data (no method calls, functions, environment variables or classes).
// Used by RadDoc for documents, presets and sessions.

RadLiteral {
	classvar <>keyOrder, <>lineWidth = 80, <>step = "  ";
	var string, pos;

	*initClass {
		// keys written first, in this order; others follow alphabetically
		keyOrder = [\format, \type, \spec, \kind, \def, \name, \args, \mods, \data, \buffer,
			\morphShape, \morphTime, \bypass, \position, \unit, \strips, \blocks];
	}

	// ---- writing ----

	*write {|obj| ^this.prWrite(obj, 0) }

	*isIdentifier {|str|
		^str.size > 0 and: { str[0].isLower } and: {
			str.every{|c| c.isAlphaNum or: { c == $_ } } } and: {
			#["nil", "true", "false", "inf", "nan", "pi", "var", "arg", "this",
				"super", "thisProcess", "thisThread", "thisMethod", "thisFunction",
				"thisFunctionDef", "currentEnvironment", "topEnvironment"]
			.includesEqual(str).not }
	}

	*isSimple {|obj| ^(obj.isKindOf(Dictionary) or: {
		obj.isKindOf(ArrayedCollection) and: { obj.isString.not } }).not
	}

	// how deeply collections nest: 0 for a value, 1 for (a: 1), 2 for (a: (b: 1))
	*height {|obj|
		var values;
		if(this.isSimple(obj), { ^0 });
		values = if(obj.isKindOf(Dictionary), { obj.values }, { obj });
		^1 + (values.collect{|v| this.height(v) }.maxItem ? 0)
	}

	*sortedKeys {|dict|
		var keys = dict.keys.asArray;
		// strips in mixer order (tracks, buses, master), then named keys
		var group = {|k| var s = k.asString;
			case
			{ "^track[0-9]+$".matchRegexp(s) } { 0 }
			{ "^bus[0-9]+$".matchRegexp(s) } { 1 }
			{ s == "master" } { 2 }
			{ 3 } };
		var rank = {|k| keyOrder.indexOf(k) ? inf };
		// natural order: track2 before track10
		var split = {|k| var s = k.asString, i = s.size;
			while({ i > 0 and: { s[i - 1].isDecDigit } }, { i = i - 1 });
			[s.keep(i), if(i < s.size, { s.drop(i).asInteger }, { -1 })] };
		^keys.sort{|a, b|
			var sa, sb;
			case
			{ a.isNumber and: { b.isNumber } } { a <= b }
			{ a.isNumber } { true }
			{ b.isNumber } { false }
			{ group.(a) != group.(b) } { group.(a) < group.(b) }
			{ rank.(a) != rank.(b) } { rank.(a) < rank.(b) }
			{ sa = split.(a); sb = split.(b); sa[0] != sb[0] } { sa[0] <= sb[0] }
			{ sa[1] <= sb[1] }
		}
	}

	// the shortest text that reads back as exactly the same Float
	*floatString {|x|
		var str;
		if(x.isNaN, { ^"nan" });
		if(x == inf, { ^"inf" });
		if(x == -inf, { ^"-inf" });
		(6..17).do{|prec|
			str = x.asStringPrec(prec);
			if(str.asFloat == x, {
				if(str.any{|c| (c == $.) or: { c == $e } }.not, { str = str ++ ".0" });
				^str
			});
		};
		^x.asStringPrec(17)
	}

	*keyString {|key|
		^case
		{ key.isKindOf(Integer) } { key.asString }
		{ key.isKindOf(Symbol) and: { this.isIdentifier(key.asString) } } { key.asString }
		{ key.isKindOf(Symbol) or: { key.isString } } { key.asSymbol.asCompileString }
		{ Error("RadLiteral: cannot write the key " ++ key.cs).throw }
	}

	*prWrite {|obj, depth|
		var pad, inner, parts, oneLine;
		^case
		{ obj.isNil or: { obj == true } or: { obj == false } } { obj.asString }
		{ obj.isKindOf(Integer) } { obj.asString }
		{ obj.isKindOf(Float) } { this.floatString(obj) }
		{ obj.isKindOf(Symbol) } {
			if(obj.asString.size > 0 and: { obj.asString[0].isAlpha } and: {
				obj.asString.every{|c| c.isAlphaNum or: { c == $_ } } }, {
				"\\" ++ obj.asString }, { obj.asCompileString })
		}
		{ obj.isString } { obj.asCompileString.replace("\n", "\\n").replace("\t", "\\t") }
		{ obj.isKindOf(Char) } { obj.asCompileString }
		{ obj.isKindOf(Dictionary) } {
			if(obj.isEmpty, { "()" }, {
				parts = this.sortedKeys(obj).collect{|k|
					this.keyString(k) ++ ": " ++ this.prWrite(obj[k], depth + 1) };
				oneLine = "(" ++ parts.join(", ") ++ ")";
				if((this.height(obj) <= 2) and: {
					(oneLine.size + (depth * step.size)) <= lineWidth }, { oneLine }, {
					pad = step.dup(depth).join; inner = pad ++ step;
					"(\n" ++ parts.collect{|p| inner ++ p }.join(",\n") ++ "\n" ++ pad ++ ")"
				})
			})
		}
		{ obj.isKindOf(ArrayedCollection) } {
			if(obj.isEmpty, { "[]" }, {
				parts = obj.asArray.collect{|v| this.prWrite(v, depth + 1) };
				oneLine = "[" ++ parts.join(", ") ++ "]";
				if((this.height(obj) <= 2) and: {
					(oneLine.size + (depth * step.size)) <= lineWidth }, { oneLine }, {
					pad = step.dup(depth).join; inner = pad ++ step;
					"[\n" ++ parts.collect{|p| inner ++ p }.join(",\n") ++ "\n" ++ pad ++ "]"
				})
			})
		}
		{ Error("RadLiteral: cannot write a " ++ obj.class.name ++
			" (only Events, Arrays, numbers, Symbols, Strings, Chars, nil, true, false)").throw }
	}

	// ---- reading ----

	// returns the data; throws an Error naming the line if the text is not plain data
	*read {|aString| ^super.new.prRead(aString) }

	prRead {|aString|
		var result;
		string = aString.asString; pos = 0;
		this.prSkip;
		if(pos >= string.size, { this.prFail("no data") });
		result = this.prValue;
		this.prSkip;
		if(this.prPeek == $;, { pos = pos + 1; this.prSkip });
		if(pos < string.size, { this.prFail("unexpected " ++ this.prShow ++ " after the data") });
		^result
	}

	prPeek {|offset = 0| ^string[pos + offset] }

	prShow {
		var c = this.prPeek;
		^if(c.isNil, { "end of text" }, { "'" ++ string.copyRange(pos, (pos + 11).min(string.size - 1)) ++ "'" })
	}

	prFail {|message|
		var line = 1;
		string.copyRange(0, (pos - 1).max(0)).do{|c| if(c == $\n, { line = line + 1 }) };
		Error("RadLiteral: line " ++ line ++ ": " ++ message).throw;
	}

	prSkip {
		var c, depth;
		while({ pos < string.size }, {
			c = string[pos];
			case
			{ c.isSpace } { pos = pos + 1 }
			{ c == $/ and: { this.prPeek(1) == $/ } } {
				while({ pos < string.size and: { string[pos] != $\n } }, { pos = pos + 1 });
			}
			{ c == $/ and: { this.prPeek(1) == $* } } {
				depth = 1; pos = pos + 2;
				while({ depth > 0 }, {
					if(pos >= string.size, { this.prFail("unclosed comment") });
					case
					{ string[pos] == $/ and: { this.prPeek(1) == $* } } { depth = depth + 1; pos = pos + 2 }
					{ string[pos] == $* and: { this.prPeek(1) == $/ } } { depth = depth - 1; pos = pos + 2 }
					{ pos = pos + 1 };
				});
			}
			{ ^this };
		});
	}

	prExpect {|char|
		this.prSkip;
		if(this.prPeek != char, { this.prFail("expected '" ++ char ++ "' but found " ++ this.prShow) });
		pos = pos + 1;
	}

	prValue {
		var c;
		this.prSkip;
		c = this.prPeek;
		^case
		{ c.isNil } { this.prFail("unexpected end of text") }
		{ c == $( } { this.prEvent }
		{ c == $[ } { this.prArray }
		{ c == $# and: { this.prPeek(1) == $[ } } { pos = pos + 1; this.prArray }
		{ c == $" } { this.prQuoted($") }
		{ c == $' } { this.prQuoted($').asSymbol }
		{ c == $\\ } { pos = pos + 1; this.prWord(true).asSymbol }
		{ c == $$ } { this.prChar }
		{ c.isDecDigit or: { c == $- } or: { c == $+ } } { this.prNumber }
		{ c.isAlpha } { this.prKeyword }
		{ this.prFail("unexpected " ++ this.prShow ++
			" (only Events, Arrays, numbers, Symbols, Strings, Chars, nil, true, false)") }
	}

	prEvent {
		var event = (), key, c;
		pos = pos + 1;
		this.prSkip;
		if(this.prPeek == $), { pos = pos + 1; ^event });
		loop {
			key = this.prKey;
			this.prExpect($:);
			event.put(key, this.prValue);
			this.prSkip;
			c = this.prPeek;
			case
			{ c == $, } {
				pos = pos + 1; this.prSkip;
				if(this.prPeek == $), { pos = pos + 1; ^event });
			}
			{ c == $) } { pos = pos + 1; ^event }
			{ this.prFail("expected ',' or ')' in an Event but found " ++ this.prShow) };
		}
	}

	prKey {
		var c;
		this.prSkip;
		c = this.prPeek;
		^case
		{ c.notNil and: { c.isAlpha } } { this.prWord(false).asSymbol }
		{ c == $' } { this.prQuoted($').asSymbol }
		{ c == $\\ } { pos = pos + 1; this.prWord(true).asSymbol }
		{ c == $" } { this.prQuoted($").asSymbol }
		{ c.notNil and: { c.isDecDigit or: { c == $- } } } {
			c = this.prNumber;
			if(c.isKindOf(Integer).not, { this.prFail("an Event key must be a name or a whole number") });
			c
		}
		{ this.prFail("expected an Event key but found " ++ this.prShow) }
	}

	prArray {
		var array = [], c;
		pos = pos + 1;
		this.prSkip;
		if(this.prPeek == $], { pos = pos + 1; ^array });
		loop {
			array = array.add(this.prValue);
			this.prSkip;
			c = this.prPeek;
			case
			{ c == $, } {
				pos = pos + 1; this.prSkip;
				if(this.prPeek == $], { pos = pos + 1; ^array });
			}
			{ c == $] } { pos = pos + 1; ^array }
			{ this.prFail("expected ',' or ']' in an Array but found " ++ this.prShow) };
		}
	}

	prWord {|allowDigitFirst|
		var start = pos;
		while({ pos < string.size and: { string[pos].isAlphaNum or: { string[pos] == $_ } } }, {
			pos = pos + 1 });
		if(pos == start, { this.prFail("expected a name but found " ++ this.prShow) });
		if(allowDigitFirst.not and: { string[start].isAlpha.not }, {
			pos = start; this.prFail("expected a name but found " ++ this.prShow) });
		^string.copyRange(start, pos - 1)
	}

	prKeyword {
		var start = pos, word = this.prWord(false);
		^switch(word,
			"nil", { nil },
			"true", { true },
			"false", { false },
			"inf", { inf },
			"nan", { 0/0 },
			{ pos = start; this.prFail("unexpected name '" ++ word ++ "'" ++
				" (names are only allowed as Event keys, or as Symbols written \\name)") }
		)
	}

	prQuoted {|quote|
		var out = List.new, c;
		pos = pos + 1;
		loop {
			if(pos >= string.size, { this.prFail("unclosed " ++
				if(quote == $", { "String" }, { "Symbol" })) });
			c = string[pos];
			case
			{ c == quote } { pos = pos + 1; ^String.newFrom(out) }
			{ c == $\\ } {
				pos = pos + 1;
				if(pos >= string.size, { this.prFail("unclosed String") });
				c = string[pos];
				out.add(switch(c, $n, { $\n }, $t, { $\t }, $r, { $\r }, $f, { $\f }, $v, { $\v }, { c }));
				pos = pos + 1;
			}
			{ out.add(c); pos = pos + 1 };
		}
	}

	prChar {
		var c;
		pos = pos + 1;
		c = this.prPeek;
		if(c.isNil, { this.prFail("unexpected end of text") });
		if(c == $\\, {
			pos = pos + 1; c = this.prPeek;
			if(c.isNil, { this.prFail("unexpected end of text") });
			c = switch(c, $n, { $\n }, $t, { $\t }, $r, { $\r }, $f, { $\f }, $v, { $\v }, { c });
		});
		pos = pos + 1;
		^c
	}

	prNumber {
		var start = pos, sign = 1, text, isFloat = false, digits = {
			var s = pos;
			while({ pos < string.size and: { string[pos].isDecDigit } }, { pos = pos + 1 });
			pos - s
		};
		if(this.prPeek == $- or: { this.prPeek == $+ }, {
			if(this.prPeek == $-, { sign = -1 }); pos = pos + 1 });
		if(this.prPeek.notNil and: { this.prPeek.isAlpha }, {
			if(this.prWord(false) == "inf", { ^inf * sign });
			pos = start; this.prFail("expected a number but found " ++ this.prShow);
		});
		if(digits.value == 0, { pos = start; this.prFail("expected a number but found " ++ this.prShow) });
		if(this.prPeek == $. and: { this.prPeek(1).notNil and: { this.prPeek(1).isDecDigit } }, {
			pos = pos + 1; digits.value; isFloat = true });
		if(this.prPeek == $e and: { this.prPeek(1).notNil and: {
			this.prPeek(1).isDecDigit or: { (this.prPeek(1) == $-) or: { this.prPeek(1) == $+ } } } }, {
			pos = pos + 1;
			if(this.prPeek == $- or: { this.prPeek == $+ }, { pos = pos + 1 });
			if(digits.value == 0, { pos = start; this.prFail("bad number " ++ this.prShow) });
			isFloat = true;
		});
		if(this.prPeek.notNil and: { this.prPeek.isAlpha or: { this.prPeek == $_ } }, {
			pos = start; this.prFail("bad number " ++ this.prShow) });
		text = string.copyRange(start, pos - 1);
		^if(isFloat, { text.asFloat }, { text.asInteger })
	}
}
