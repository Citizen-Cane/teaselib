package teaselib;

import teaselib.util.Item;

public enum Features implements Item.Attribute {
    Inflatable,
    Vibrating,
    Detachable,
    SuctionCup,
    Lockable,
    HandsFree,
    EStim,
    Spikes,

    ;

    public enum Electrode {
        Single,
        Dual,
    }
}
