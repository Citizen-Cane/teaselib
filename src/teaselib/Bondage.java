package teaselib;

import static teaselib.util.Select.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import teaselib.util.Item;
import teaselib.util.Items;
import teaselib.util.Select;

public enum Bondage {

    Ankle_Restraints,
    Wrist_Restraints,

    Chains,
    Rope,
    Tape,

    Harness,
    Spreader_Bar,

    ;

    public enum HarnessType implements Item.Attribute {
        Torso,
        Crotch,
        Strap_On,
    }

    public static final Select.Statement All = items(Bondage.values());

    public static final Select.Statement Anklets = new Select.Statement(
            items(Ankle_Restraints).where(Items::matching, Features.Detachable));
    public static final Select.Statement Wristlets = new Select.Statement(
            items(Wrist_Restraints).where(Items::matching, Features.Detachable));
    public static final Select.Statement[] Cufflets = { Anklets, Wristlets };

    public static final Select.Statement Ankle_Cuffs = new Select.Statement(
            items(Ankle_Restraints).where(Items::without, Features.Detachable));
    public static final Select.Statement Wrist_Cuffs = new Select.Statement(
            items(Wrist_Restraints).where(Items::without, Features.Detachable));
    public static final Select.Statement Cuffs[] = { Ankle_Cuffs, Wrist_Cuffs };

    public static final Select.Statement Restraints = items(Ankle_Restraints, Wrist_Restraints);
    public static final Select.Statement Rig = items(Chains, Rope, Tape);
    public static final Select.Statement Fetish = items(Harness, Spreader_Bar);

    public static final List<Select.Statement> Categories = Collections.unmodifiableList(Arrays.asList( //
            Restraints, Rig, Fetish));

}
