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
    Boots,
    Boxers,
    Bra,
    Breast_Forms,
    Briefs,
    Catsuit,
    Cheerleader_Uniform,
    Corset,
    Diaper,
    Dress,
    Garterbelt,
    GString,
    Gloves,
    Handbag,
    High_Heels,
    Jacket,
    Jeans,
    Jewelry,
    Leotard,
    Maid_Attire,
    Make_up,
    Miniskirt,
    Monokini,
    Nightie,
    Nurse_Uniform,
    Panties,
    Pantyhose,
    Princess_Tiara,
    Sandals,
    Scarf,
    Schoolgirl_Uniform,
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

    // Wearers of the diverse clothing items

    public static final String Male = "male";
    public static final String Female = "female";
    public static final String Tranny = "tranny";
    public static final String Maid = "maid";
    public static final String Schoolgirl = "schoolgirl";
    public static final String Doll = "doll";
    public static final String Wife = "wife";
    public static final String Husband = "husband";

    public static final Clothes[] MaleClothes = { Boots, Boxers, Briefs, Jacket,
            Jeans, Sandals, Shirt, Shoes, Shorts, Socks, Suit, Sweater,
            Sweatpants, Trousers, TShirt, Tie };

    public static final Clothes[] FemaleClothes = { Baby_Doll, Bikini, Blouse,
            Boots, Bra, Catsuit, Cheerleader_Uniform, Corset, Dress, Garterbelt,
            GString, Handbag, High_Heels, Jacket, Jeans, Leotard, Miniskirt,
            Monokini, Nightie, Panties, Pantyhose, Sandals, Shirt, Shoes,
            Shorts, Skirt, Socks, Stockings, Sweater, Sweatpants, Swimsuit,
            Tanktop, Thong, Trousers, TShirt, Vest };

    public static final Clothes[] Accesoires = { Apron, Belt, Gloves, Jewelry,
            Make_up, Princess_Tiara, Scarf, Tie };

    public static final Clothes[] TrannyItems = { Breast_Forms, Diaper, Wig };

    public static final Clothes[] Costumes = { Maid_Attire, Nurse_Uniform,
            Schoolgirl_Uniform, Cheerleader_Uniform };

    @Override
    public String toString() {
        return super.toString();
    }
}
