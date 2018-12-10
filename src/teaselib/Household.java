package teaselib;

public enum Household {
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
    Padlock,
    Rubber_Bands,
    Ruler,
    Sewing_Box,
    Shoe_Lace,
    Shrinkwrap,
    Tampons,
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

    public static final Household[] Discipline = { Clothes_Pegs, Weight, Hairbrush, Ruler, Wooden_Spoon };
    public static final Household[] Bondage = { Padlock, Shoe_Lace, Shrinkwrap, Duct_Tape, Leash };
    public static final Household[] Commodities = { Balloons, Candle, Cigarettes, Condoms, Diaper, Heat_Rub, Lube,
            Rubber_Bands, Tampons };
    public static final Household[] Aids = { Dust_Pan, Sewing_Box, Vacuum_Cleaner };
    public static final Household[] Kitchen = { Ice_Tray, Icecubes };

    public static final Household[] Categories[] = { Discipline, Bondage, Commodities, Kitchen, Aids };
}
