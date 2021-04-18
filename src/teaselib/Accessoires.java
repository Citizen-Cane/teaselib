package teaselib;

import static teaselib.util.Select.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    // Fetish
    Corset,
    Harness,

    /**
     * In the BDSM context, a hood is full head cover with optional holes for eyes, nose and mouth, otherwise a head
     * harness. A mask is similar but also different, as for disguising the wearers identity.
     */
    Hood,

    ;

    public enum JewelryType {
        Bracelet,
        Necklace,
    }

    public enum GloveType {
        Household,
        Elbow,
    }

    public static final Select.Statement All = items(Accessoires.values());

    public static final Select.Statement Male = items(Tie, Gloves);
    public static final Select.Statement Female = items(Belt, Gloves, Handbag, Jewelry, Makeup, Princess_Tiara, Scarf);

    public static final Select.Statement Maid = items(Apron, Bonnet, Bells);
    public static final Select.Statement Crossdressing = items(Breast_Forms, Wig);

    public static final Select.Statement Fetish = items(Corset, Harness, Hood);

    public static final List<Select.Statement> Categories = Collections.unmodifiableList(Arrays.asList( //
            Male, Female, Maid, Crossdressing, Fetish));

}
