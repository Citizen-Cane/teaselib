/**
 * 
 */
package teaselib.persistence;

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
public enum Clothing {
    Apron, Baby_Doll, Bathing_Suit, Belt, Bikini, Blouse, Boots, Boxers, Bra, Briefs, Catsuit, Cheerleader_Uniform, Corset, Diaper, Dress, Garterbelt, GString, Gloves, Handbag, High_Heels, Jacket, Jeans, Jewelry, Leotard, Maid_Attire, Make_up, Miniskirt, Monokini, Nightie, Nurse_Uniform, Pajamas, Panties, Pantyhose, Sandals, Scarf, Schoolgirl_Uniform, Shirt, Shoes, Shorts, Skirt, Socks, Stockings, Suit, Sweater, Sweatpants, Swimsuit, Tanktop, Thong, Tie, Trousers, TShirt, Vest;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
