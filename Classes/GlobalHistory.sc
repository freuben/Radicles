GlobalHistory : MainImprov {
	var startTime, path, <>recordHistory;

	*start {arg fileName, replace;
		^super.new.initCallWindow(fileName, replace);
	}

	initCallWindow {arg fileName, replace=false;
		var file, date, writeHistoryFunc;

		fileName ?? {fileName = Date.getDate.asString};

		path = (mainPath ++ "Settings/GlobalHistory/" ++ fileName ++ ".scd");

		writeHistoryFunc = {
			file = File(path, "a+");
			file.close;
			recordHistory = true;

			SystemClock.sched(0.0, {arg time;
				startTime = time;
				nil;
			});
		};

		if(File.existsCaseSensitive(path), {
			if(replace.not, {
				Window.warnQuestion(("This global history already exist: " ++
					"Are you sure you want to replace it?"), {writeHistoryFunc.();});
			});
		}, {
			writeHistoryFunc.();
		});
	}

	stop {
		recordHistory = false;
	}

	resume {
		recordHistory = true;
	}

	writeHistory {arg time, arr;
		var file, now;
		now = time - startTime;
		file = File(path, "a+");
		file.write([now, arr].asCompileString);
		file.write(", ");
		file.close;
	}

	add {arg arr;
		if(recordHistory, {
			SystemClock.sched(0.0, {arg time;
				this.writeHistory(time, arr);
				nil;
			});
		});
	}
}