[ [ 'test2', [ [ 'freq', [ 200.0, 1000, 'exp' ], {|val| val.round} ], [ 'amp', [ 0, 1 ] ], [ 'mul', [ 0, 0.5 ] ], [ 'harm', [ 1, 10 ], {|val| val.round;} ] ] ], [ 'test1', [ [ 'freq', [ 200.0, 1000, 'exp' ], {|val| val.round} ], [ 'amp', [ 0, 1 ] ], [ 'mul', [ 0, 0.5 ] ], [ 'harm', [ 1, 10 ], {|val| val.round;} ] ] ], [ 'sine', [ [ 'freq', [ 20, 20000, 'exp' ] ], [ 'phase', [ -6.2831853071796, 6.2831853071796 ] ], [ 'mul', [ 0, 2 ] ] ] ], [ 'play', [ [ 'rate', [ 0.1, 10 ] ], [ 'trigger', [ 0, 1 ], {|val| val.round(1)} ], [ 'startPos', [ 0, 1 ] ], [ 'mul', [ 0, 2 ] ] ] ], [ 'loop', [ [ 'rate', [ 0.1, 10 ] ], [ 'trigger', [ 0, 1 ], {|val| val.round(1)} ], [ 'startPos', [ 0, 1 ] ], [ 'mul', [ 0, 2 ] ] ] ], [ 'cue', [ [ 'loop', [ 0, 1 ], {|val| val.round(1)} ], [ 'mul', [ 0, 2 ] ] ] ], [ 'playpv', [ [ 'rate', [ 0.1, 10 ] ], [ 'offset', [ 0, 1 ] ], [ 'numFrames', [ 0, 1, 'lin', 0.1 ] ], [ 'winType', [ -1, 1 ], {|val| val.round(1)} ], [ 'mul', [ 0, 2 ] ] ] ], [ 'looppv', [ [ 'rate', [ 0.1, 10 ] ], [ 'offset', [ 0, 1 ] ], [ 'numFrames', [ 0, 1, 'lin', 0.1 ] ], [ 'winType', [ -1, 1 ], {|val| val.round(1)} ], [ 'mul', [ 0, 2 ] ] ] ], [ 'playbuf', [ [ 'rate', [ 0.1, 10 ] ], [ 'trigger', [ 0, 1 ], {|val| val.round(1)} ], [ 'startPos', [ 0, 1 ] ], [ 'mul', [ 0, 2 ] ], [ 'loop', [ 0, 1 ], {|val| val.round(1)} ] ] ], [ 'rec', [ [ 'recLevel', [ 0, 2 ] ], [ 'preLevel', [ 0, 2 ] ] ] ], [ 'recpv', [ [ 'numChannels', [ 1, 10 ], {|val| val.round(1)} ], [ 'numFrames', [ 0, 1, 'lin', 0.1 ] ], [ 'hop', [ 0, 1 ] ], [ 'winType', [ -1, 1 ], {|val| val.round(1)} ], [ 'offset', [ 0, 1 ] ], [ 'run', [ 0, 1 ], {|val| val.round(1)} ], [ 'loop', [ 0, 1 ], {|val| val.round(1)} ], [ 'mul', [ 0, 2 ] ] ] ], [ 'recloop', [ [ 'recLevel', [ 0, 2 ] ], [ 'preLevel', [ 0, 2 ] ] ] ], [ 'looptr', [ [ 'rate', [ 0.1, 10 ] ], [ 'triggerRate', [ 0.1, 10 ] ], [ 'startPos', [ 0, 1 ] ], [ 'mul', [ 0, 2 ] ] ] ], [ 'osc1', [ [ 'freq', [ 20, 20000, 'exp' ] ], [ 'phase', [ -6.2831853071796, 6.2831853071796 ] ], [ 'mul', [ 0, 2 ] ] ] ], [ 'shaperSine', [ [ 'freq', [ 20, 20000, 'exp' ] ], [ 'phase', [ -6.2831853071796, 6.2831853071796 ] ], [ 'mul', [ 0, 2 ] ] ] ], [ 'vosc', [ [ 'bufoffset', [ 0, 10 ] ], [ 'freq', [ 20, 20000, 'exp' ] ], [ 'phase', [ -6.2831853071796, 6.2831853071796 ] ], [ 'mul', [ 0, 2 ] ] ] ], [ 'loopAll', [ [ 'rate', [ 0.1, 10 ] ], [ 'trigger', [ 0, 1 ], {|val| val.round(1)} ], [ 'startPos', [ 0, 1 ] ], [ 'mul', [ 0, 2 ] ] ] ] ]