package teaselib;

public enum Accessoires {
    Apron,
    Belt,
    Gloves,
    Handbag,
    Makeup,
    Jewelry,
    Princess_Tiara,
    Scarf,
    Tie,

    // Crossdressing
    Breast_Forms,
    Strap_On,
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

    public static final Accessoires[] Fetish = { Strap_On };
    public static final Accessoires[] Maid = { Apron }; // TODO Bonnet
    public static final Accessoires[] Crossdressing = { Breast_Forms, Wig };

    public static final Accessoires[][] Categories = { Male, Female, Fetish, Maid, Crossdressing };

}
