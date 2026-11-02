// Assemblage: effect bypass (Document Model, step 1, PR 4). Bypassing an
// effect routes its strip around it, so the stage after it reads the stage
// before it, exactly as if the effect were removed, while the effect's Ndef
// stays with all its settings. The re-routing crossfades over the stage's
// fade time; once it has faded, the effect is paused (no CPU) and keeps its
// state, so a delay or reverb continues from where it was when resumed.
// Part of the Assemblage class: the class and its variables are in Assemblage.sc.

+ Assemblage {

	isBypassed {arg filterTag; ^(bypassed ? []).includes(filterTag) }

	// a strip's stages with the bypassed effects left out: the chain as it sounds
	activeStages {arg trackInfo;
		^trackInfo.reject{|item| this.isBypassed(item[0]) }
	}

	// the code that makes the stage info read from the Ndef destKey (as autoRoute does)
	prStageCode {arg info, destKey;
		var synth = info[1], synthFunc;
		if(synth.isFunction.not, { ^nil });
		if(synth.isArray, {
			synthFunc = synth[0].filterFunc([Ndef(destKey)] ++ synth.copyRange(1, synth.size-1));
		}, {
			synthFunc = synth.filterFunc(Ndef(destKey));
		});
		^this.ndefPrepare(Ndef(info[0]), synthFunc)
	}

	// route one stage of a strip from the stage before it in the active chain
	prRouteStage {arg trackInfo, key;
		var active = this.activeStages(trackInfo), index, code;
		index = active.flop[0].indexOf(key);
		if(index.notNil and: { index > 0 }, {
			code = this.prStageCode(active[index], active[index-1][0]);
			if(code.notNil, { code.radcode; server.sync });
		});
	}

	// bypass (state 1), un-bypass (0), or toggle (nil) the effect in a slot
	bypassFx {arg type=\track, num=1, slot=1, state, action;
		var filterTag, trackInfo, next;
		filterTag = this.findFilterTag(type, num, slot);
		if(filters.isNil or: { filters.flop[0].includes(filterTag).not }, {
			"No effect in this slot".warn; ^this });
		state = state ?? { if(this.isBypassed(filterTag), 0, 1) };
		if((state == 1) == this.isBypassed(filterTag), { action.(); ^this }); //already so
		trackInfo = this.getThisTrack(type, num);
		{
			if(state == 1, {
				bypassed = (bypassed ? []).add(filterTag);
				//the first stage after the effect that sounds now reads around it
				next = this.prNextActive(trackInfo, filterTag);
				if(next.notNil, {
					this.prRouteStage(trackInfo, next[0]);
					Ndef(next[0]).fadeTime.wait;
				});
				("Ndef(" ++ filterTag.cs ++ ").pause;").radcode;
			}, {
				bypassed = bypassed.reject{|tag| tag == filterTag };
				("Ndef(" ++ filterTag.cs ++ ").resume;").radcode;
				server.sync;
				//the effect reads the stage before it: re-routed only if that stage
				//changed while it was bypassed, since a new source would restart it
				//(a delay or reverb continues from where it was)
				if(this.prReadsFrom(filterTag, this.prPrevActive(trackInfo, filterTag)).not, {
					this.prRouteStage(trackInfo, filterTag);
				});
				//the next stage reads the effect
				next = this.prNextActive(trackInfo, filterTag);
				if(next.notNil, { this.prRouteStage(trackInfo, next[0]) });
			});
			RadUnit.sync(filterTag, this); //document (step 1)
			{ this.prUpdateBypassButton(filterTag) }.defer;
			action.();
		}.fork(AppClock);
	}

	// the key of the last stage before key that sounds, or nil
	prPrevActive {arg trackInfo, key;
		var index = trackInfo.flop[0].indexOf(key);
		^index !? { trackInfo.copyRange(0, index - 1).reverse
			.detect{|item| this.isBypassed(item[0]).not } !? {|item| item[0] } }
	}

	// does the Ndef key's source read the Ndef from?
	prReadsFrom {arg key, from;
		^from.notNil and: { Ndef(key).source.cs.find("Ndef.ar(" ++ from.cs ++ ",").notNil }
	}

	// the first stage after key that sounds (not bypassed), or nil
	prNextActive {arg trackInfo, key;
		var index = trackInfo.flop[0].indexOf(key);
		^index !? { trackInfo.copyToEnd(index + 1).detect{|item| this.isBypassed(item[0]).not } }
	}

	// forget a slot's bypass (its effect was removed or replaced by another),
	// resuming its Ndef: clear does not, and a new effect there would start
	// paused. Returns true if the slot was bypassed.
	prClearBypass {arg filterTag;
		if(this.isBypassed(filterTag).not, { ^false });
		bypassed = bypassed.reject{|tag| tag == filterTag };
		("Ndef(" ++ filterTag.cs ++ ").resume;").radcode;
		^true
	}

	// the BYPASS button of the effect's window, if it is open
	prUpdateBypassButton {arg filterTag;
		var win = (filtersWindow ? []).detect{|w| w.name.beginsWith(filterTag.asString ++ " ") };
		win !? {|w| w.canvas.children.do{|v|
			if(v.isKindOf(Button) and: { v.states.size == 2 }, {
				v.value = this.isBypassed(filterTag).binaryValue });
		} };
	}
}
