/**
 * 
 */
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
    Mask,

    ;

    enum UnderpantsType {
        Boxers,
        Briefs,
        G_String,
        Panties,
        Thong,
    }

    enum Footwear {
        Boots,
        High_Heels,
        Pumps,
        Sandals,
        Sneakers
    }

    enum SkirtType {
        MiniSkirt,
        Skirt
    }

    enum TrouwsersType {
        Jeans,
        Shorts,
        Sweatpants,
        Trousers
    }

    enum SwimwearType {
        Bikini,
        Monokini,
        Swimsuit,
    }
    // Namespaces of wearers of the diverse clothing items

    public static final String Male = "Male";
    public static final String Female = "Female";
    public static final String Doll = "Doll";
    public static final String Wife = "Wife";
    public static final String Husband = "Husband";

    public static final Clothes[] Underwear = { Babydoll, Body, Bra, Catsuit, Garter_Belt, Leotard, Nightie, Underpants,
            Pantyhose, Socks, Stockings, Swimwear };

    public static final Clothes[] Clothing = { Shirt, Socks, Suit, Sweater, Trousers, TShirt };

    public static final Clothes[] MaleUnderwear = { Underpants, Socks, Pajamas };
    public static final Clothes[] MaleClothing = { Socks, Shirt, TShirt, Sweater, Trousers, Jacket, Suit, Vest, Shoes };

    public static final Clothes[] FemaleUnderwear = { Underpants, Socks, Babydoll, Nightie, Pantyhose, Body, Bra,
            Catsuit, Garter_Belt, Nightie, Underpants, Pajamas, Leotard, Swimwear };
    public static final Clothes[] FemaleClothing = { Blouse, Corset, Dress, Jacket, Shirt, Skirt, Stockings, Sweater,
            Tanktop, Trousers, Vest, Shoes };

    public static final Clothes[] Fetish = { Corset, Harness, Mask };

    public static final Clothes[][] Categories = { MaleUnderwear, MaleClothing, FemaleUnderwear, FemaleClothing,
            Fetish };

    @Override
    public String toString() {
        return super.toString();
    }
}
