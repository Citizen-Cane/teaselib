package teaselib;

import static teaselib.util.Select.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import teaselib.util.Items;
import teaselib.util.Select;

/**
 * @author Citizen-Cane
 *
 */
public enum Shoes {

    Boots,
    High_Heels,
    Pumps,
    Sandals,
    Sneakers

    ;

    public static final Select.Statement All = items(values());

    public static final Select.Statement Masculine = new Select.Statement(
            items(Shoes.values()).where(Items::without, Sexuality.Gender.Feminine));

    public static final Select.Statement Feminine = new Select.Statement(
            items(Shoes.values()).where(Items::without, Sexuality.Gender.Masculine));

    public static final List<Select.Statement> Categories = Collections.unmodifiableList(Arrays.asList( //
            Masculine, Feminine));

}
