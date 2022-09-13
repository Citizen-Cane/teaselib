package teaselib;

import static teaselib.util.Select.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import teaselib.util.Item;
import teaselib.util.Select;

/**
 * Toys are generalized, which means their names have been chosen not be too specific.
 * <p>
 * Toys are grouped by category, to make it possible to query a whole category.
 * <p>
 * I guess that because toys can be grouped (Gags, spanking implements, plugs, clamps, restraints), there has to be a
 * more advanced approach to organize toys.
 * <p>
 * So for now, just the generalized version of something is added.
 * <p>
 * The list has been inspired by CyberMistress and SexScripts.
 * <p>
 *
 */
public enum Toys {
    Blindfold,
    Buttplug,
    Collar,
    Gag,
    Nipple_Clamps,
    Spanking_Implement,

    Ball_Stretcher,
    Chastity_Device,
    Cock_Ring,
    Glans_Ring,
    Humbler,
    Masturbator,

    Dildo,
    Pussy_Clamps,
    Clit_Clamp,
    VaginalInsert,

    Anal_Douche,
    Enema_Kit,
    Enema_Bulb,

    Vibrator,
    EStim_Device,

    Doll,
    Spouse,

    ;

    public enum ClampStyle implements Item.Attribute {
        Alligator,
        Clover,
        Tweezer
    }

    public enum Masturbators implements Item.Attribute {
        Pussy,
        Bumhole,
        Feet,
        Breasts,
        Mouth,
    }

    public enum Chastity_Devices implements Item.Attribute {
        Belt,
        Cage,
        Cone_of_Shame,
        Crimper,
        Gates_of_Hell,

        ;

        public enum Features implements Item.Attribute {
            Dilator,
            AccessibleGlans,
            HingedRing,
            MultipleRingSizes
        }
    }

    public enum CollarType implements Item.Attribute {
        Dog,
        Posture,
    }

    public enum Gags implements Item.Attribute {
        Ball_Gag,
        Bit_Gag,
        Muzzle_Gag,
        Penis_Gag,
        Ring_Gag,
    }

    public enum Spanking_Implements implements Item.Attribute {
        Cane,
        Crop,
        Flogger,
        Paddle,
        Whip
    }

    public enum Anal implements Item.Attribute {
        Beads,
        Plug,
        Ball,
        Hook
    }

    public enum Vaginal implements Item.Attribute {
        Ben_Wa_Balls,
        Vibrating_Egg,
    }

    public static final Select.Statement All = items(values());

    public static final Select.Statement Essential = items(Blindfold, Collar, Gag, Nipple_Clamps, Spanking_Implement);

    public static final Select.Statement Backdoor = items(Buttplug, Dildo, Anal_Douche, Enema_Bulb, Enema_Kit);
    public static final Select.Statement Female = items(Chastity_Device, Clit_Clamp, Pussy_Clamps, VaginalInsert);
    public static final Select.Statement Male = items(Ball_Stretcher, Chastity_Device, Glans_Ring, Cock_Ring, Humbler,
            Masturbator);
    public static final Select.Statement Stimulation = items(Vibrator, EStim_Device);
    public static final Select.Statement Partner = items(Doll, Spouse);

    public static final List<Select.Statement> Categories = Collections.unmodifiableList(Arrays.asList( //
            Essential, Backdoor, Female, Male, Stimulation, Partner));

}
