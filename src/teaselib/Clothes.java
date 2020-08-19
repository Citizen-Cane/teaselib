package teaselib;

import static teaselib.util.Select.items;

import teaselib.util.Items;
import teaselib.util.Select;

/**
 * Clothes are generalized, which means their names have been chosen not be too specific.
 * 
 * So for now, just the generalized version of something is added.
 * 
 * The list has been inspired by CyberMistress and SexScripts.
 * 
 * @author Citizen-Cane
 *
 */
public enum Clothes {
    // Underwear
    Babydoll,
    Body,
    Bra,
    Catsuit,
    Garter_Belt,
    Leotard,
    Underpants,
    Pantyhose,
    Socks,
    Stockings,

    // Sleepwear
    Nightie,
    Pajamas,

    // outer garments
    Blouse,
    Dress,
    Jacket,
    Skirt,
    Shirt,
    Shoes,
    Suit,
    Sweater,
    Tanktop,
    Trousers,
    TShirt,
    Vest,;

    public enum UnderpantsType {
        Boxers,
        Briefs,
        G_String,
        Panties,
        Thong,
    }

    public enum Footwear {
        Boots,
        High_Heels,
        Pumps,
        Sandals,
        Sneakers
    }

    public enum SkirtType {
        MiniSkirt,
        Skirt
    }

    public enum TrouwsersType {
        Jeans,
        Shorts,
        Sweatpants,
        Trousers
    }

    public enum SwimwearType {
        Bikini, // bra/underpants
        Monokini, // body/leotard
        Swimsuit, // body/leotard
    }

    public enum GenitalAccessType {
        Ouvert,
        Zipper,
        None
    }

    public enum AnalAccessType {
        Ouvert,
        Zipper,
        None
    }

    public enum BreastAccessType {
        Ouvert,
        Zipper,
        None
    }

    public enum NippleAccessType {
        Ouvert,
        Zipper,
        None
    }

    /**
     * TODO classify clothing items by category to define catsuits as lingerie or outer garments
     * <p>
     * TODO also classify by {@link teaselib.Sexuality#Gender}
     */
    public enum Category {
        Lingerie,
        Underwear,
        Sleepwear,
        Swimwear,
        OuterGarment,
    }

    // Namespaces of wearers of the diverse clothing items

    public static final String Doll = "Doll";
    public static final String Partner = "Partner";

    public static final Select.Statement all = items(Clothes.values());

    /**
     * Feminine underwear
     */
    public static final Select.Statement lingerie = items(Babydoll, Body, Bra, Catsuit, Garter_Belt, Leotard, Pantyhose,
            Socks, Stockings, Underpants).where(Items::matching, Category.Lingerie).and(Items::without,
                    Sexuality.Gender.Masculine);

    /**
     * Masculine underwear
     */
    public static final Select.Statement underwear = items(Underpants, Socks).where(Items::matching, Category.Underwear)
            .and(Items::without, Sexuality.Gender.Masculine).and(Items::without, Category.Swimwear);

    public static final Select.Statement swimwear = items(Clothes.values()).where(Items::matching, Category.Swimwear);

    public static final Select.Statement sleepwear = items(Clothes.values()).where(Items::matching, Category.Sleepwear);

    public static final Select.Statement female = items(Blouse, Dress, Jacket, Shirt, Skirt, Sweater, Tanktop, Trousers,
            Vest, Shoes).where(Items::without, Sexuality.Gender.Masculine);

    public static final Select.Statement male = items(Blouse, Dress, Jacket, Shirt, Skirt, Sweater, Tanktop, Trousers,
            Vest, Shoes).where(Items::without, Sexuality.Gender.Feminine);

    @Deprecated // TODO Remove since it's all lingerie
    public static final Clothes[] Underwear = { Babydoll, Body, Bra, Catsuit, Garter_Belt, Leotard, Pantyhose, Socks,
            Stockings, Underpants };

    @Deprecated // TODO Remove since it's all male clothing
    public static final Clothes[] Clothing = { Shirt, Socks, Suit, Sweater, Trousers, TShirt };

    @Deprecated // TODO Remove
    public static final Clothes[] MaleUnderwear = { Underpants, Socks };
    @Deprecated // TODO Remove
    public static final Clothes[] MaleClothing = { Socks, Shirt, TShirt, Sweater, Trousers, Jacket, Suit, Vest, Shoes };

    @Deprecated // TODO Remove
    public static final Clothes[] Lingerie = { Babydoll, Body, Bra, Catsuit, Garter_Belt, Leotard, Pantyhose, Socks,
            Stockings, Underpants };
    @Deprecated // TODO Remove
    public static final Clothes[] FemaleClothing = { Blouse, Dress, Jacket, Shirt, Skirt, Sweater, Tanktop, Trousers,
            Vest, Shoes };

    @Deprecated // TODO Remove
    public static final Clothes[] Sleepwear = { Nightie, Pajamas };

    @Deprecated // TODO Remove
    public static final Clothes[][] Categories = { MaleUnderwear, MaleClothing, Lingerie, FemaleClothing, Sleepwear };

    @Override
    public String toString() {
        return super.toString();
    }

}
