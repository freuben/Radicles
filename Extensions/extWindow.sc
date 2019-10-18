+ Window {

	*extraScreenBounds {arg offSet;
		var win, newBounds;
		offSet ?? {offSet = this.screenBounds.width};
		{
			win = this.new("", Rect(offSet, 0, 100, 100));
			0.1.yield;
			win.fullScreen;
			0.1.yield;
			newBounds = win.bounds;
			1.yield;
			win.endFullScreen;
			0.3.yield;
			win.close;
			newBounds.postln;
			}.fork(AppClock);
	}

	*win2Left {arg fullScreen, menuSpacer=40;
		var rect, newHeight;
		fullScreen ?? {fullScreen = this.screenBounds};
		newHeight = (fullScreen.height - menuSpacer);
		rect = [fullScreen.left, fullScreen.top, fullScreen.width/2, newHeight].asRect;
		^rect;
	}

	*win2Right {arg fullScreen, menuSpacer=40;
		var rect, newHeight;
		fullScreen ?? {fullScreen = this.screenBounds};
		newHeight = (fullScreen.height - menuSpacer);
		rect = [fullScreen.left+(fullScreen.width/2), fullScreen.top, fullScreen.width/2, newHeight].asRect;
		^rect;
	}

	*win4TopLeft {arg fullScreen, menuSpacer=40, winBorder=15;
		var rect, halfHeight;
		fullScreen ?? {fullScreen = this.screenBounds};
		halfHeight = (fullScreen.height - menuSpacer) / 2;
		rect = [fullScreen.left, halfHeight+winBorder, fullScreen.width/2, halfHeight-winBorder].asRect;
		^rect;
	}

	*win4BottomLeft {arg fullScreen, menuSpacer=40, winBorder=15;
		var rect, halfHeight;
		fullScreen ?? {fullScreen = this.screenBounds};
		halfHeight = (fullScreen.height - menuSpacer) / 2;
		rect = [fullScreen.left, fullScreen.top, fullScreen.width/2, halfHeight-winBorder].asRect;
		^rect;
	}

	*win4TopRight {arg fullScreen, menuSpacer=40, winBorder=15;
		var rect, halfHeight;
		fullScreen ?? {fullScreen = this.screenBounds};
		halfHeight = (fullScreen.height - menuSpacer) / 2;
		rect = [fullScreen.left+(fullScreen.width/2), halfHeight+winBorder, fullScreen.width/2, halfHeight-winBorder].asRect;
		^rect;
	}

		*win4BottomRight {arg fullScreen, menuSpacer=40, winBorder=15;
		var rect, halfHeight;
		fullScreen ?? {fullScreen = this.screenBounds};
		halfHeight = (fullScreen.height - menuSpacer) / 2;
		rect = [fullScreen.left+(fullScreen.width/2), fullScreen.top, fullScreen.width/2, halfHeight-winBorder].asRect;
		^rect;
	}

	*warnQuestion {arg string, yesfunc, nofunc;
		var win, text, button1, button2;
		win = Window.new("WARNING", Rect(0,0,300,100).center_( Window.availableBounds.center ));
		text = StaticText(win);
		text.string = string;
		text.align = \center;
		button1 = Button(win).states_([["Yes"]]).maxWidth_(75);
		button1.action = {yesfunc.value; win.close};
		button2 = Button(win).states_([["No"]]).maxWidth_(75);
		button2. action = {nofunc.value; win.close};
		win.layout = VLayout(text, HLayout(button1, button2));
		win.front;
	}

}

