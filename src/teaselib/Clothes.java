package teaselib;

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
    enum Category {
        Lingerie,
        Sleepwear,
        Swimwear,
        OuterGarments
    }

    // Namespaces of wearers of the diverse clothing items

    public static final String Doll = "Doll";
    public static final String Partner = "Partner";

    // Categories

    public static final Clothes[] Underwear = { Babydoll, Body, Bra, Catsuit, Garter_Belt, Leotard, Nightie, Underpants,
            Pantyhose, Socks, Stockings };

    public static final Clothes[] Clothing = { Shirt, Socks, Suit, Sweater, Trousers, TShirt };

    public static final Clothes[] MaleUnderwear = { Underpants, Socks, Pajamas };
    public static final Clothes[] MaleClothing = { Socks, Shirt, TShirt, Sweater, Trousers, Jacket, Suit, Vest, Shoes };

    public static final Clothes[] Lingerie = { Underpants, Garter_Belt, Stockings, Pantyhose, Socks, Body, Bra,
            Babydoll, Leotard, Catsuit };
    public static final Clothes[] Sleepwear = { Nightie, Pajamas };
    public static final Clothes[] FemaleClothing = { Blouse, Dress, Jacket, Shirt, Skirt, Sweater, Tanktop, Trousers,
            Vest, Shoes };

    public static final Clothes[][] Categories = { MaleUnderwear, MaleClothing, Lingerie, Sleepwear, FemaleClothing };

    @Override
    public String toString() {
        return super.toString();
    }

}
