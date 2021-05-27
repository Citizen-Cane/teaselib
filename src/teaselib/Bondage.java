package teaselib;

import static teaselib.util.Select.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import teaselib.util.Item;
import teaselib.util.Select;

public enum Bondage {

    Harness,

    Wristlets,
    Anklets,

    Chains,
    Rope,
    Tape,

    Spreader_Bar,

    ;

    public enum HarnessType implements Item.Attribute {
        Torso,
        Crotch,

        Strap_On,
    }

    public static final Select.Statement All = items(Bondage.values());

    public static final Select.Statement Cuffs = items(Wristlets, Anklets);
    public static final Select.Statement Restraints = items(Chains, Rope, Tape);
    public static final Select.Statement FetishItems = items(Harness, Spreader_Bar);

    public static final List<Select.Statement> Categories = Collections.unmodifiableList(Arrays.asList( //
            Cuffs, Restraints, FetishItems));

}
