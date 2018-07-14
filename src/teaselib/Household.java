package teaselib;

public enum Household {
    Bell,
    Balloons,
    Candle,
    Cigarettes,
    Clothes_Pegs,
    Condoms,
    Diaper,
    Duct_Tape,
    Dust_Pan,
    Heat_Rub,
    Hairbrush,
    Ice_Tray,
    Icecubes,
    Leash,
    Lube,
    Padlocks,
    Rubber_Bands,
    Ruler,
    Sewing_Box,
    Shoe_Lace,
    Shrinkwrap,
    Tampon,
    Vacuum_Cleaner,
    Weight,
    Wooden_Spoon,

    ;

    enum LubeType {
        Baby_Oil,
        Silicone_Based,
        Vaseline,
        Water_Based,
    }

    public static final Household[] Discipline = { Hairbrush, Ruler, Wooden_Spoon, Clothes_Pegs };
    public static final Household[] Bondage = { Padlocks, Shoe_Lace, Shrinkwrap, Duct_Tape, Leash };
    public static final Household[] Utilities = { Heat_Rub, Lube, Balloons, Bell, Condoms, Diaper, Rubber_Bands, Tampon,
            Weight };
    public static final Household[] Tools = { Dust_Pan, Sewing_Box, Vacuum_Cleaner };
    public static final Household[] Kitchen = { Ice_Tray, Icecubes, Candle, Cigarettes };

    public static final Household[] Categories[] = { Discipline, Bondage, Utilities, Kitchen, Tools };
}
