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
			{where == \gui} {
				this.do{|item|
					item.docpostln(extraPost)
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

	radpost {arg type=\ln;
		var string, doc;
		string = this.asString.lineFormat(Radicles.lineSize);
		doc = Radicles.postDoc;
		if(string.includesString(10.asAscii), {
			type = \br
		});
		if(Radicles.logCodeTime, {
			string = ("//thisThread: " ++ thisThread.seconds ++ 10.asAscii ++ string);
		});
		if(doc.isNil, {
		string.postin(\ide, type);
		}, {
			string.postallin([\ide, \doc], type, doc);
		});
	}

	radpostcont {arg type=\ln, postDoc=false, postWin=true, win=\ide;
		var string, doc;
		string = this.asString;
		if(Radicles.reducePostControl, {
			string = string.replace(",", "(").replace(").set").replace(");").split($().copyToEnd(1);
		});
		if(postDoc, {
		doc = Radicles.postDoc;
		if(Radicles.logCodeTime, {
			string = ("//thisThread: " ++ thisThread.seconds ++ 10.asAscii ++ string);
		});
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
	}

}