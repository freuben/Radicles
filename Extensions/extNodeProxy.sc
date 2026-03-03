+ NodeProxy {

	resetDefaultVals {
		this.controlKeys.do{|item|
			this.set(item, this.getDefaultVal(item));
		};
	}

}