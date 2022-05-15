package teaselib;

import static teaselib.util.Select.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import teaselib.util.Item;
import teaselib.util.Select;

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

    enum PadlockType implements Item.Attribute {
        Key,
        Numeric,
    }

    enum LubeType implements Item.Attribute {
        Baby_Oil,
        Silicone_Based,
        Vaseline,
        Water_Based,
    }

    public static final Select.Statement All = items(values());

    public static final Select.Statement Discipline = items(Clothes_Pegs, Weight, Hairbrush, Ruler, Wooden_Spoon,
            Leash);
    public static final Select.Statement Bondage = items(Padlock, Shoe_Lace, Shrinkwrap, Duct_Tape);
    public static final Select.Statement Commodities = items(Balloons, Candle, Cigarettes, Condoms, Diaper, Heat_Rub,
            Lube, Rubber_Bands, Tampons);
    public static final Select.Statement Aids = items(Dust_Pan, Sewing_Box, Vacuum_Cleaner);
    public static final Select.Statement Kitchen = items(Ice_Tray, Icecubes);

    public static final List<Select.Statement> Categories = Collections.unmodifiableList(Arrays.asList( //
            Discipline, Bondage, Commodities, Kitchen, Aids));

}
