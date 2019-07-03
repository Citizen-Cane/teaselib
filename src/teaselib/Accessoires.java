package teaselib;

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

    public static final Accessoires[] Male = { Tie, Gloves };
    public static final Accessoires[] Female = { Belt, Gloves, Handbag, Jewelry, Makeup, Princess_Tiara, Scarf };

    public static final Accessoires[] Maid = { Apron, Bonnet, Bells };
    public static final Accessoires[] Crossdressing = { Breast_Forms, Wig };

    public static final Accessoires[] Fetish = { Corset, Harness, Hood };

    public static final Accessoires[][] Categories = { Male, Female, Maid, Crossdressing, Fetish };

}
