[ [ 'perkysine', {arg freq=400, amp=1, out=0;
    var osc;
    osc = SinOsc.ar( [freq,freq+0.05.rand] ) * EnvGen.ar(
    Env.perc, doneAction: 2) * 0.2;
    Out.ar(out,osc*amp);
    } ] ]