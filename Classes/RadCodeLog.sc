// Research code log (Step 3, PR 4). Off by default.
//
//   Radicles.codeLog = true;    // or type: codelog on
//   Radicles.codeLog = false;   // or type: codelog off
//
// While it is on, every piece of code Radicles generates is appended to a
// file in RadiclesFiles/Logs (one file per session, named by date and time),
// with the seconds since the log started and its source:
//   typed, menu, button, hidcmd   posted on radpost (main document)
//   gui, hid, osc                 posted on radpostcont (post window)
//   code                          called directly from SuperCollider code
//   ide                           run by hand in the IDE (plain SuperCollider)
// The file stays on this computer; nothing is sent anywhere. Each entry is
// appended and the file closed straight away, so a crash loses nothing.
// The log is a valid .scd file: entries are comment headers followed by code.

RadCodeLog {
	classvar <path, startTime, ideHook, prevCodeDump;

	*isOn { ^path.notNil }

	*on {arg ide = true;
		var dir, header, rev;
		if(path.notNil, { ("Radicles code log is already on: " ++ path).postln; ^path });
		dir = (Radicles.filesPath ?? { Platform.userExtensionDir +/+ "RadiclesFiles" }) +/+ "Logs";
		File.mkdir(dir);
		path = dir +/+ ("radicles_" ++ Date.localtime.format("%Y-%m-%d_%H-%M-%S") ++ ".scd");
		startTime = Main.elapsedTime;
		rev = try {
			("cd " ++ Quark("Radicles").localPath.shellQuote ++
				" && git rev-parse --short HEAD 2>/dev/null").unixCmdGetStdOut.stripWhiteSpace
		} { "" };
		header = [
			"// Radicles code log",
			"// started: " ++ Date.localtime.format("%Y-%m-%d %H:%M:%S"),
			"// SuperCollider " ++ Main.version ++ ", Radicles " ++ (rev.size > 0).if({rev}, {"(version unknown)"}),
			"// Each entry: //@ <seconds since start> <source> <channel>, then the code.",
			"// Sources: typed, menu, button, hidcmd (radpost); gui, hid, osc (radpostcont);",
			"//          code (called from SuperCollider code); ide (run by hand in the IDE).",
			""
		].join("\n") ++ "\n";
		if(this.prWrite(header, "w").not, {
			("Radicles code log: could not write to " ++ path).warn;
			path = nil;
			^nil
		});
		if(ide, {
			// add to the interpreter's code hook rather than replacing it
			prevCodeDump = thisProcess.interpreter.codeDump;
			ideHook = {|code ... rest|
				this.add(code, \ide, \ide);
				prevCodeDump.value(code, *rest);
			};
			thisProcess.interpreter.codeDump = ideHook;
		});
		("Radicles code log ON, writing to: " ++ path).postln;
		^path
	}

	*off {
		var oldPath;
		if(path.isNil, { ^nil });
		if(ideHook.notNil and: { thisProcess.interpreter.codeDump === ideHook }, {
			thisProcess.interpreter.codeDump = prevCodeDump;
		});
		ideHook = nil;
		prevCodeDump = nil;
		this.prWrite("// stopped: " ++ Date.localtime.format("%Y-%m-%d %H:%M:%S") ++ "\n");
		oldPath = path;
		path = nil;
		("Radicles code log OFF: " ++ oldPath).postln;
		^oldPath
	}

	*add {arg code, channel, source;
		if(path.isNil, { ^this });
		this.prWrite("//@ " ++ (Main.elapsedTime - startTime).round(0.001) ++ " "
			++ (source ? \code) ++ " " ++ channel ++ "\n" ++ code.asString ++ "\n");
	}

	*prWrite {arg string, mode = "a";
		var file = File(path, mode);
		if(file.isOpen.not, { ^false });
		file.write(string);
		file.close;
		^true
	}
}
