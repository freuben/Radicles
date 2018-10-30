[ [ 'test2', {arg freq = 400 -> [200.0,1000, \exp, {|val| val.round}], amp
 = 0.4 -> [0,1], mul = 0.4 -> [0,0.5], harm = 3-> [1,10, {|val| val.round;}];
	Saw.ar(freq, amp);} ], [ 'test1', {arg freq = 400 -> [200.0,1000, \exp, {|val|
 val.round}], amp = 0.4 -> [0,1], mul = 0.4 -> [0,0.5], harm = 3-> [1,10,
 {|val|
 val.round;}];
SinOsc.ar(freq, 0, amp);
} ] ]