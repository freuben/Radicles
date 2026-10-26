// Step 3: GUI actions that know their source. `view.srcAction = {...}` sets the
// action like `view.action = {...}`, but runs it with the view's source:
// \menu for PopUpMenus and lists (posted on radpost), \gui for sliders, knobs,
// buttons and fields (posted on radpostcont). See Design Principle 1.

+ View {

	radSourceKind {
		^if(this.isKindOf(PopUpMenu) or: { this.isKindOf(ListView) }, {\menu}, {\gui});
	}

	srcAction_ {arg func;
		this.action = Radicles.srcFunc(this.radSourceKind, func);
	}

	srcMouseDownAction_ {arg func;
		this.mouseDownAction = Radicles.srcFunc(this.radSourceKind, func);
	}
}
