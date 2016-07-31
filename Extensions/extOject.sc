+ Object {

	postin {arg where=\ide, type=\post, extraPost, time=0.1, dev=0.01;
		case
		{type == \fork} {
			if((where == \ide).or(where == \both), {
				{
				this.asString.do{|item| item.post; rrand(time-dev, time+dev).yield;
					};
					}.fork;
			});
			if((where == \gui).or(where == \both), {extraPost.addToFork(this, time, dev);});
		}
		{type == \forkln} {
			if((where == \ide).or(where == \both), {
				{
					(this.asString ++ 13.asAscii).do{|item| item.post; rrand(time-dev, time+dev).yield;
					};
					}.fork;
			});
			if((where == \gui).or(where == \both), {extraPost.addLineToFork(this, time, dev);});
		}
		{type == \ln} {
			if((where == \ide).or(where == \both), {this.postln});
			if((where == \gui).or(where == \both), {extraPost.addLine(this);});
		}
		{type == \post} {
			if((where == \ide).or(where == \both), {this.post});
			if((where == \gui).or(where == \both), {extraPost.addPost(this);});
		}
		{type == \doln} {
			if((where == \ide).or(where == \both), {
				this.do{|item|
					item.postln;
				};
			});
			if((where == \gui).or(where == \both), {extraPost.doAddLine(this);});
		};
	}

}