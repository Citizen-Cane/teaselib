package teaselib;

import teaselib.util.Item;

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

    Rope,
    Ankle_Restraints,
    Wrist_Restraints,
    Chains,
    Spreader_Bar,

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
    Husband,
    Wife,

    ;

    public enum ClampType implements Item.Attribute {
        Clover,
    }

    public enum Masturbators {
        Pussy,
        Bumhole,
        Feet,
        Breasts,
        Mouth,
    }

    public enum Chastity_Devices implements Item.Attribute {
        Belt,
        Cage,
        Gates_of_Hell,
        Cone_of_Shame,
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
        Plug
    }

    public enum Vaginal implements Item.Attribute {
        Ben_Wa_Balls,
        Vibrating_Egg,
    }

    public static final Toys Essential[] = { Gag, Blindfold, Collar, Nipple_Clamps, Spanking_Implement };
    public static final Toys Bondage[] = { Ankle_Restraints, Wrist_Restraints, Rope, Chains, Spreader_Bar };
    public static final Toys Backdoor[] = { Buttplug, Dildo, Anal_Douche, Enema_Bulb, Enema_Kit };
    public static final Toys Female[] = { Clit_Clamp, Pussy_Clamps, VaginalInsert };
    public static final Toys Male[] = { Ball_Stretcher, Chastity_Device, Cock_Ring, Glans_Ring, Humbler, Masturbator };
    public static final Toys Stimulation[] = { Vibrator, EStim_Device };
    public static final Toys Partner[] = { Doll, Husband, Wife };

    public static final Toys[] Categories[] = { Essential, Bondage, Backdoor, Female, Male, Stimulation, Partner };
}
