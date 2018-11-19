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

    public static final Accessoires[] Maid = { Apron, Bonnet };
    public static final Accessoires[] Crossdressing = { Breast_Forms, Wig };

    public static final Accessoires[][] Categories = { Male, Female, Maid, Crossdressing };

}
