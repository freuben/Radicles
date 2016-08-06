PostWindow : MainImprov {var text, <>megaString, <>time=0.1, <>deviation=0.01, win;

	*new {arg window, bounds, font, qpalette;
		^super.new.initBasicClass(window, bounds, font, qpalette);
	}

	initBasicClass {arg window, bounds, font, qpalette;

		bounds ?? {bounds = Window.win4TopLeft};
		font ?? {font=Font("Monaco", 12)};
		qpalette ?? {qpalette = QPalette.dark};

		if(window.isNil, {
			"You must provide a window as first argument, otherwise use the .window class method".warn;
		}, {
			text = TextView(window.asView, bounds).focus(true);
			text.font_(font).palette_(qpalette);
			megaString = "";
			this.fork;
		});
	}

	*window {arg name="Window into your code", bounds, font, qpalette;
		var window;

		bounds ?? {bounds = Window.win4TopLeft};

		window = Window.new(name, bounds.asRect).front;
		^this.new(window, Rect(0,0,bounds.width, bounds.height), font, qpalette);
	}

	fork {
		{
			inf.do{
				if(megaString.size != 0, {
					megaString.do{|item, index|
						text.addString(item.asString);
						text.select(text.selectionStart, 0);
						megaString = megaString.drop(1);
						rrand(time-deviation, time+deviation).yield;
					};
				});
				0.1.yield;
			}
		}.fork(AppClock);
	}

	addToFork {arg thisObject, changeTime, changeDev;
		megaString = megaString ++ (thisObject.asString ++ " ");
		if(changeTime.notNil, {
			time = changeTime;
			deviation = changeDev;
		});
	}

	addLineToFork {arg thisObject, changeTime, changeDev;
		megaString = megaString ++ (thisObject.asString ++ 13.asAscii);
		if(changeTime.notNil, {
			time = changeTime;
			deviation = changeDev;
		});
	}

	addLine {arg thisObject;
		var string, task;
		task = {
			inf.do{
				if(megaString.size == 0, {
					string = thisObject.asString ++ 13.asAscii;
					text.addString(string.asString);
					text.select(text.selectionStart, 0);
					task.stop;
				});
				0.1.yield;
			}
		}.fork(AppClock);
	}

	addPost {arg thisObject;
		var string, task;
		task = {
			inf.do{
				if(megaString.size == 0, {
					string = thisObject.asString ++ " ";
					text.addString(string.asString);
					text.select(text.selectionStart, 0);
					task.stop;
				});
				0.1.yield;
			}
		}.fork(AppClock);
	}

	doAddLine {arg array;
		{
			array.do{|item|
				this.addLine(item);
			};
			this.addLine("");
		}.fork(AppClock);
	}

	doAddPost {arg array;
		{
			array.do{|item|
				this.addPost(item);
			};
			this.addLine("");
		}.fork(AppClock);
	}

	clearFrom {arg max=10000;
		if((text.selectionStart > max), {
			text.select(0,max);
			text.string_("");
		});
	}

	clear {
		this.clearFrom(0);
	}

}
