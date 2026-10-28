+ Object {

	docpost {arg doc;
		var textSize, string;
		textSize = doc.text.size;
		doc.selectRange(textSize, 0);
		string = this;
		doc.string_(string, doc.selectionStart+string.size);
	}

	docpostln {arg doc;
		var string;
		string = this ++ 10.asAscii;
		string.docpost(doc);
	}

	docpostbr {arg doc;
		var string;
		string = this;
		string = "(" ++ 10.asAscii ++ string ++ 10.asAscii ++ ")";
		string.docpostln(doc);
		doc.selectRange(doc.text.size-1, 0);
	}

	docfork {arg doc, time=0.1, dev=0.01, action;
		var string;
		string = this;
		{
			string.do{|item|
				item.asString.docpost(doc);
				rrand(time-dev, time+dev).yield;
			};
			action.();
		}.fork(AppClock);
	}

	docforkln {arg doc, time=0.1, dev=0.01, action;
		var string;
		string = this ++ 10.asAscii;
		string.docfork(doc, time, dev, action);
	}

	docforkbr {arg doc, time=0.1, dev=0.01;
		var string;
		string = this;
		string = "(" ++ 10.asAscii ++ string ++ 10.asAscii ++ ")";
		string.docforkln(doc, time, dev, {
			doc.selectRange(doc.text.size-1, 0);
		});
	}

	postin {arg where=\ide, type=\post, extraPost, time=0.1, dev=0.01;
		var brackets;
		case
		{type == \fork} {
			case
			{where == \ide} {
				{
					this.asString.do{|item| item.post; rrand(time-dev, time+dev).yield;
					};
				}.fork(AppClock);
			}
			{where == \gui} {extraPost.addToFork(this, time, dev);}
			{where == \doc} { this.docfork(extraPost, time, dev); };
		}
		{type == \forkln} {
			case
			{where == \ide} {
				{
					(this.asString ++ 13.asAscii).do{|item| item.post;
						rrand(time-dev, time+dev).yield;
					};
				}.fork(AppClock);
			}
			{where == \gui} {extraPost.addLineToFork(this, time, dev);}
			{where == \doc} { this.docforkln(extraPost, time, dev); };
		}
		{type == \ln} {
			case
			{where == \ide} {this.postln}
			{where == \gui} {extraPost.addLine(this);}
			{where == \doc} { this.docpostln(extraPost) };
		}
		{type == \post} {
			case
			{where == \ide} {this.post}
			{where == \gui} {extraPost.addPost(this);}
			{where == \doc} { this.docpost(extraPost); };
		}
		{type == \doln} {
			case
			{where == \ide} {
				this.do{|item|
					item.postln;
				};
			}
			{where == \gui} {extraPost.doAddLine(this);}
			{where == \doc} {
				this.do{|item|
					item.asString.docpostln(extraPost)
				};
			};
		}
		{type == \br} {
			brackets =	("(" ++ 10.asAscii ++ this ++ 10.asAscii ++ ")";);
			case
			{where == \ide} {brackets.postln}
			{where == \gui} {extraPost.addLine(brackets);}
			{where == \doc} {this.docpostbr(extraPost);};
		}
		{type == \forkbr} {
			case
			{where == \ide} {this.postln}
			{where == \gui} {extraPost.addLine(this);}
			{where == \doc} {this.docforkbr(extraPost, time, dev);};
		}
	}

	postallin {arg where=[\ide, \doc], type=\post, extraPost, time=0.1, dev=0.01;
		where.do{|item|
			this.postin(item, type, extraPost, time, dev);
		}
	}

	dopostln {
		this.postin(\ide, \doln);
	}

	isSymbol {var bool;
		bool = this.class.name == 'Symbol';
		^bool;
	}

	radpost {arg type=\ln, oscId="/code";
		//optional listener (tests): gets the code as generated
		Radicles.postCapture !? {Radicles.postCapture.value(this.asString, \radpost, Radicles.source)};
		//research code log (off by default)
		if(RadCodeLog.isOn, {RadCodeLog.add(this.asString, \radpost, Radicles.source)});
		if(Radicles.ignorePost.not, {
			if(type == \doln and: { this.isString.not } and: { this.isKindOf(Collection) }, {
				// a list: each item on its own line (not the list as one string)
				this.do{|item| item.asString.prRadpostShow(\ln, oscId) };
			}, {
				this.prRadpostShow(type, oscId);
			});
		});
	}

	// display part of radpost: main document, post window, GUI post window, OSC
	prRadpostShow {arg type=\ln, oscId="/code";
		var string, doc, postUI, radImprov;
		doc = Radicles.postDoc;
		postUI = Radicles.postWindow;
		radImprov = Radicles.myImprov;
		string = this;
		if(postUI.notNil, {
			string = string.replace(9.asAscii, " ");
		});
		string = string.asString.lineFormat(Radicles.lineSize);
		if(string.includesString(10.asAscii), {
			type = \br
		});
		if(Radicles.oscCode.notNil, {
			Radicles.oscCode.sendMsg(oscId, string);
		});
		if(radImprov.notNil, {
			if(type == \post, {
				radImprov.postCount = radImprov.postCount + 1;

				if((radImprov.postCount%radImprov.postModulo) == 1, {
					{string.postIn(radImprov.extraPost2, \post);
						radImprov.extraPost2.clearFrom(
							radImprov.extraPost2.calcMaxChar + radImprov.maxPostChars;
							);
					}.defer;
				});

				"".postln;
			}, {
				{string.postIn(radImprov.extraDoc, \ln);}.defer;
			});
		});
		if(doc.isNil, {
			string.postin(\ide, type);
		}, {
			string.postallin([\ide, \doc], type, doc);
		});
		if(postUI.notNil, {
			string.postin(\gui, type, postUI);
		});
	}

	radpostcont {arg type=\ln, postDoc=false, postWin=true, win=\ide, oscId="/codecont";
		var string, doc, postUI;
		//optional listener (tests): continuous control channel
		Radicles.postCapture !? {Radicles.postCapture.value(this.asString, \radpostcont, Radicles.source)};
		if(RadCodeLog.isOn, {RadCodeLog.add(this.asString, \radpostcont, Radicles.source)});
		if(Radicles.ignorePostcont.not, {
			string = this.asString;
			//share on OSC like radpost, on its own address; Radicles.oscCodeCont = false
			//sends only /code
			if(Radicles.oscCode.notNil and: { Radicles.oscCodeCont }, {
				Radicles.oscCode.sendMsg(oscId, string);
			});
			//optional GUI window for the continuous stream (any PostWindow)
			postUI = Radicles.postWindowCont;
			if(Radicles.reducePostControl, {
				string = string.replace(",", "(").replace(").set").replace(");").split($().copyToEnd(1);
			});
			if(postDoc, {
				doc = Radicles.postDoc;
				if(doc.isNil, {
					string.postin(\ide, type);
				}, {
					string.postallin([\ide, \doc], type, doc);
				});
			}, {
				if(postWin, {
					string.postin(win, type);
				});
			});
			if(postUI.notNil, {
				string.postin(\gui, type, postUI);
			});
		});
	}

	// Step 3: post generated code on the channel its source calls for, then run
	// it. `default` is the channel used when there is no source (code called
	// directly from SuperCollider), so direct calls post where they always did.
	radcode {arg default=\radpost, type=\ln;
		if(Radicles.channelFor(Radicles.source, default) == \radpost, {
			this.radpost(type);
		}, {
			this.radpostcont(type);
		});
		^this.interpret;
	}

	// Log and run code without displaying it: for code a method deliberately
	// keeps out of the document (it still reaches the research code log).
	radlog {
		if(RadCodeLog.isOn, {RadCodeLog.add(this.asString, \nopost, Radicles.source)});
		^this.interpret;
	}

	// Display code on the channel its source calls for, without running it:
	// for code whose action Radicles carries out directly (a buffer allocated
	// through BufferSystem, a function put into an Ndef).
	radshow {arg default=\radpost, type=\ln;
		if(Radicles.channelFor(Radicles.source, default) == \radpost, {
			this.radpost(type);
		}, {
			this.radpostcont(type);
		});
	}

	radpostwarn {arg type=\ln;
		var string, postUI;
		postUI = Radicles.postWindow;
		string = this.asString.lineFormat(Radicles.lineSize);
		string.postin(\ide, type);
		if(postUI.notNil, {
			string.postin(\gui, type, postUI);
		});

	}

}