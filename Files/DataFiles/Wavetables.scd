[ [ 'sine1harm6', {|buf| buf.sine1(1.0/[1,2,3,4,5,6])} ], [ 'sine2harm2', {|buf| buf.sine2([1.0, 3], [1, 0.5]); } ], [ 'chebyEx1', {|buf| buf.cheby([1,0,1,1,0,1])} ], [ 'varsquared', {|buf, num, index|
			var arr;
			 num = (index+1)**2;
			 arr = Array.fill(num, { arg ind; ((num-ind)/num).squared.round(0.001) });
		  buf.sine1(arr);
} ] ]