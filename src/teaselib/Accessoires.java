package teaselib;

import static teaselib.util.Select.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import teaselib.util.Item;
import teaselib.util.Select;

public enum Accessoires {
    Apron,
    Belt,
    Bonnet,
    Gloves,
    Handbag,
    Makeup,
    Jewelry,
    Princess_Tiara,
    Scarf,
    Tie,

    // Crossdressing
    Breast_Forms,
    Wig,
    Bells,

    ;

    public enum JewelryType implements Item.Attribute {
        Bracelet,
        Necklace,
    }

    public enum GloveType implements Item.Attribute {
        Household,
        Elbow,
    }

    public static final Select.Statement All = items(Accessoires.values());

    public static final Select.Statement Male = items(Tie, Gloves);
    public static final Select.Statement Female = items(Belt, Gloves, Handbag, Jewelry, Makeup, Princess_Tiara, Scarf);

    public static final Select.Statement Maid = items(Apron, Bonnet, Bells);
    public static final Select.Statement Crossdressing = items(Breast_Forms, Wig);

    public static final List<Select.Statement> Categories = Collections.unmodifiableList(Arrays.asList( //
            Male, Female, Maid, Crossdressing));

}
