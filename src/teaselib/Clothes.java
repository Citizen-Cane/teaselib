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
    Nightie,
    Underpants,
    Pantyhose,
    Pajamas,
    Socks,
    Stockings,
    Swimwear,

    // Attire
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
    Vest,

    // Fetish
    Corset,
    Harness,

    /**
     * In the BDSM context, a hood is full head cover with optional holes for eyes, nose and mouth, otherwise a head
     * harness. A mask is similar but also different, as for disguising the wearers identity.
     */
    Hood,

    ;

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
        Bikini,
        Monokini,
        Swimsuit,
    }

    public enum GenitalAccessType {
        Ouvert,
        Zipper,
    }

    public enum AnalAccessType {
        Ouvert,
        Zipper,
    }

    public enum BreastAccessType {
        Ouvert,
        Zipper,
    }

    // Namespaces of wearers of the diverse clothing items

    public static final String Doll = "Doll";
    public static final String Partner = "Partner";

    public static final Clothes[] Underwear = { Babydoll, Body, Bra, Catsuit, Garter_Belt, Leotard, Nightie, Underpants,
            Pantyhose, Socks, Stockings, Swimwear };

    public static final Clothes[] Clothing = { Shirt, Socks, Suit, Sweater, Trousers, TShirt };

    public static final Clothes[] MaleUnderwear = { Underpants, Socks, Pajamas };
    public static final Clothes[] MaleClothing = { Socks, Shirt, TShirt, Sweater, Trousers, Jacket, Suit, Vest, Shoes };

    public static final Clothes[] FemaleUnderwear = { Underpants, Garter_Belt, Stockings, Pantyhose, Socks, Body, Bra,
            Babydoll, Nightie, Pajamas, Leotard, Catsuit, Swimwear };
    public static final Clothes[] FemaleClothing = { Blouse, Dress, Jacket, Shirt, Skirt, Sweater, Tanktop, Trousers,
            Vest, Shoes };

    public static final Clothes[] Fetish = { Corset, Harness, Hood };

    public static final Clothes[][] Categories = { MaleUnderwear, MaleClothing, FemaleUnderwear, FemaleClothing,
            Fetish };

    @Override
    public String toString() {
        return super.toString();
    }
}
