+ Collection {

	rejectSame {
		this.do({|item|
			var array;
			array = this.indicesOfEqual(item);
			if(array.size > 1, {array.copyRange(1, array.size-1).do({|item,index| this.removeAt(item-index);});});
		});
	}


}

+ ArrayedCollection {

	removeAtAll {arg indexArr;
		var count = 0, removeItem;
		indexArr.do{|item|
			removeItem = item - count;
			this.removeAt(removeItem);
			count = count + 1;
		};
	}

}

