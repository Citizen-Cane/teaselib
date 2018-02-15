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
    Ankle_Restraints,
    Blindfold,
    Buttplug,
    Collar,
    Gag,
    Nipple_Clamps,
    Spanking_Implement,
    Spreader_Bar,
    Wrist_Restraints,

    Chains,
    Rope,

    Ball_Stretcher,
    Chastity_Device,
    Cockring,
    GlansRing,
    Humbler,
    Masturbator,

    Dildo,
    Pussy_Clamps,
    Clit_Clamp,
    VaginalInsert,
    Vibrator,
    Strap_On,

    Anal_Douche,
    Enema_Kit,
    Enema_Bulb,

    EStim_Device,

    Doll,
    Husband,
    Wife,

    ;

    public enum Masturbators {
        Pussy,
        Bumhole,
        Feet,
        Breasts,
        Face,
    }

    public enum ChastityDevices implements Item.Attribute {
        Chastity_Belt,
        Chastity_Cage,
        Cone_of_Shame,
        Gates_of_Hell
    }

    public enum Collars implements Item.Attribute {
        Dog_Collar,
        Maid_Collar,
        Posture_Collar,
        Slave_Collar,
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
        VibratingEgg,
    }

    public enum Vibrators implements Item.Attribute {
        HandsFree,
        Manual,
    }

    public static final Toys Essential[] = { Gag, Blindfold, Collar, Nipple_Clamps, Spanking_Implement };
    public static final Toys Bondage[] = { Ankle_Restraints, Wrist_Restraints, Rope, Chains, Spreader_Bar };
    public static final Toys Backdoor[] = { Buttplug, Dildo, Anal_Douche, Enema_Bulb, Enema_Kit };
    public static final Toys Female[] = { Clit_Clamp, Pussy_Clamps, VaginalInsert };
    public static final Toys Male[] = { Ball_Stretcher, Cockring, GlansRing, Humbler, Masturbator };
    public static final Toys Stimulation[] = { Vibrator, EStim_Device };
    public static final Toys Partner[] = { Doll, Husband, Wife, Chastity_Device, Strap_On };

    public static final Toys[] Categories[] = { Essential, Bondage, Backdoor, Female, Male, Stimulation, Partner };
}
