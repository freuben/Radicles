[ [ 'perkysine', {arg freq=400, amp=1;
    var osc;
    osc = SinOsc.ar( [freq,freq+0.05.rand] ) * EnvGen.ar(
    Env.perc, doneAction: 2) * 0.2;
    osc*amp;
    } ] ]