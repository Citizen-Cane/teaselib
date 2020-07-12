package teaselib;

import teaselib.util.Item;

public enum Features implements Item.Attribute {
    Inflatable,
    Vibrating,
    Detachable,
    Coupled,
    SuctionCup,
    Lockable,
    HandsFree,
    EStim,
    Spikes,
    AutoRelease,

    ;

    public enum Channels {
        Single,
        Dual,
    }

    public enum Electrode {
        Single,
        Dual,
    }

}
