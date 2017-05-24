/**
 * 
 */
package teaselib;

/**
 * Clothes are generalized, which means their names have been chosen not be too
 * specific.
 * 
 * So for now, just the generalized version of something is added.
 * 
 * The list has been inspired by CyberMistress and SexScripts.
 * 
 * @author someone
 *
 */
public enum Clothes {
    Apron,
    Baby_Doll,
    Belt,
    Bikini,
    Blouse,
    Body,
    Boots,
    Boxers,
    Bra,
    Breast_Forms,
    Briefs,
    Catsuit,
    Corset,
    Diaper,
    Dress,
    Garterbelt,
    GString,
    Gloves,
    Handbag,
    Harness,
    High_Heels,
    Hood,
    Jacket,
    Jeans,
    Jewelry,
    Leotard,
    Make_up,
    Miniskirt,
    Monokini,
    Nightie,
    Panties,
    Pantyhose,
    Princess_Tiara,
    Sandals,
    Scarf,
    Shirt,
    Shoes,
    Shorts,
    Skirt,
    Socks,
    Stockings,
    Suit,
    Sweater,
    Sweatpants,
    Swimsuit,
    Tanktop,
    Thong,
    Tie,
    Trousers,
    TShirt,
    Vest,
    Wig;

    // Namespaces of wearers of the diverse clothing items

    public static final String Male = "Male";
    public static final String Female = "Female";
    public static final String Maid = "Maid";
    public static final String Doll = "Doll";
    public static final String Wife = "wife";
    public static final String Husband = "Husband";

    public static final Clothes[] Underwear = { Boxers, Briefs, GString, Panties, Thong };

    public static final Clothes[] MaleClothes = { Boots, Boxers, Briefs, Jacket, Jeans, Sandals, Shirt, Shoes, Shorts,
            Socks, Suit, Sweater, Sweatpants, Trousers, TShirt, Tie };

    public static final Clothes[] FemaleClothes = { Baby_Doll, Bikini, Blouse, Body, Boots, Bra, Catsuit, Corset, Dress,
            Garterbelt, GString, High_Heels, Jacket, Jeans, Leotard, Miniskirt, Monokini, Nightie, Panties, Pantyhose,
            Sandals, Shirt, Shoes, Shorts, Skirt, Socks, Stockings, Sweater, Sweatpants, Swimsuit, Tanktop, Thong,
            Trousers, TShirt, Vest };

    public static final Clothes[] MaidClothes = { Blouse, Body, Boots, Bra, Catsuit, Corset, Dress, Garterbelt, GString,
            High_Heels, Leotard, Miniskirt, Panties, Pantyhose, Sandals, Shirt, Shoes, Skirt, Socks, Stockings, Thong };

    public static final Clothes[] FemaleAccesoires = { Belt, Gloves, Handbag, Jewelry, Make_up, Princess_Tiara, Scarf };
    public static final Clothes[] FetishAccesoires = { Corset, Harness, Hood };
    public static final Clothes[] MaidAccesoires = { Apron, Belt, Gloves };
    public static final Clothes[] MaleAccesoires = { Belt, Gloves, Tie };
    public static final Clothes[] TrannyAccesoires = { Breast_Forms, Diaper, Wig };

    @Override
    public String toString() {
        return super.toString();
    }
}
