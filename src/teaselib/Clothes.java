package teaselib;

import static teaselib.util.Select.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import teaselib.util.Item;
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
    Shirt,
    Skirt,
    Suit,
    Sweater,
    Tanktop,
    Trousers,
    Vest,

    Corset,

    /**
     * In the BDSM context, a hood is full head cover with optional holes for eyes, nose and mouth, otherwise a head
     * harness. A mask is similar but also different, as for disguising the wearers identity.
     */
    Hood,

    ;

    public enum UnderpantsType implements Item.Attribute {
        Boxers,
        Briefs,
        G_String,
        Panties,
        Thong,
    }

    public enum SkirtType implements Item.Attribute {
        MiniSkirt,
    }

    public enum TrouwsersType implements Item.Attribute {
        Jeans,
        Shorts,
        Sweatpants,
    }

    public enum SwimwearType implements Item.Attribute {
        Bikini, // bra/underpants
        Monokini, // body/leotard
        Swimsuit, // body/leotard
    }

    public enum Ouvert implements Item.Attribute {
        Anal,
        Breasts,
        Genital,
        Nipples
    }

    public enum Zipper implements Item.Attribute {
        Anal,
        Breasts,
        Genital,
        Nipples
    }

    @SuppressWarnings("hiding")
    public enum Category implements Item.Attribute {
        Lingerie,
        Underwear,
        Sleepwear,
        Swimwear,
        OuterGarment,
    }

    // Namespaces of wearers of the diverse clothing items

    // TODO change make these fields to domains (blocked because domain instances aren't static)
    public static final String Doll = "Doll";
    public static final String Partner = "Partner";

    public static final Select.Statement All = items(Clothes.values());

    /**
     * Feminine underwear
     */
    public static final Select.Statement Lingerie = new Select.Statement(
            items(Babydoll, Body, Bra, Catsuit, Garter_Belt, Leotard, Pantyhose, Socks, Stockings, Underpants)
                    .where(Items::matching, Category.Lingerie).and(Items::without, Sexuality.Gender.Masculine));

    /**
     * Masculine underwear
     */
    public static final Select.Statement Underwear = new Select.Statement(items(Socks, Underpants)
            .where(Items::matching, Category.Underwear).and(Items::without, Sexuality.Gender.Feminine));

    public static final Select.Statement Swimwear = new Select.Statement(
            items(Clothes.values()).where(Items::matching, Category.Swimwear));

    public static final Select.Statement Sleepwear = new Select.Statement(
            items(Clothes.values()).where(Items::matching, Category.Sleepwear));

    public static final Select.Statement Female = new Select.Statement(
            items(Blouse, Dress, Jacket, Shirt, Skirt, Sweater, Tanktop, Trousers, Vest).where(Items::without,
                    Sexuality.Gender.Masculine));

    public static final Select.Statement Male = new Select.Statement(
            items(Shirt, Suit, Sweater, Trousers, Vest).where(Items::without, Sexuality.Gender.Feminine));

    public static final Select.Statement Fetish = items(Corset, Hood);

    public static final List<Select.Statement> Categories = Collections
            .unmodifiableList(Arrays.asList(Underwear, Lingerie, Swimwear, Sleepwear, Female, Male, Fetish));

}
