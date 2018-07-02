package teaselib;

public enum Household {
    Bell,
    Baloons,
    Candle,
    Cigarettes,
    Clothes_Pegs,
    Condoms,
    Diaper,
    Duct_Tape,
    Heat_Rub,
    Hairbrush,
    Ice_Tray,
    Icecubes,
    Leash,
    Lube,
    Padlocks,
    Rubber_Bands,
    Ruler,
    Shoe_Lace,
    Shrinkwrap,
    Tampon,
    Weight,
    Wooden_Spoon,

    ;

    enum LubeType {
        Baby_Oil,
        Silicone_Based,
        Vaseline,
        Water_Based,
    }

    public static final Household[] Spanking = { Hairbrush, Ruler, Wooden_Spoon, Clothes_Pegs };
    public static final Household[] Bondage = { Padlocks, Shoe_Lace, Shrinkwrap, Duct_Tape, Leash };
    public static final Household[] Tools = { Heat_Rub, Lube, Baloons, Bell, Condoms, Diaper, Rubber_Bands, Tampon,
            Weight };
    public static final Household[] Kitchen = { Ice_Tray, Icecubes, Candle, Cigarettes };

    public static final Household[] Categories[] = { Spanking, Bondage, Tools, Kitchen };
}
