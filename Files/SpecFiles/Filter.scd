[ [ 'pch', [ [ 'pitchRatio', [ -12, 12 ], {|val| val.round(0.4).midiratio}
 ], [ 'pitchDispertion', [ 0.01, 2.01, 'exp' ], {|val| val-0.01} ], [ 'timeDispersion',
 [ 0.01, 2.01, 'exp' ], {|val| val-0.01} ], [ 'lag', [ 0.01, 15 ] ], [ 'winSize',
 [ 0.01, 3 ] ] ] ], [ 'pch2', [  ] ] ]