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
    Enema_Bulb,
    Enema_Kit,
    Gag,
    Nipple_Clamps,
    Spanking_Implement,
    Spreader_Bar,
    Wrist_Restraints,

    Ball_Stretcher,
    Chastity_Device,
    Cockring,
    Humbler,

    Dildo,
    Pussy_Clamps,
    VaginalInsert,
    Vibrator,

    // Too BDSM like to be household items
    Chains,
    Rope,

    // These toys below aren't used directly by the player,
    // albeit they may have a huge influence on the play
    EStim_Device,
    Strap_On,

    Doll,
    Husband,
    Wife,

    ;

    public enum ChastityDevices implements Item.Attribute {
        Chastity_Belt,
        Chastity_Cage,
        Gates_of_Hell,
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

    public final static Toys Basic[] = { Ankle_Restraints, Gag, Blindfold, Buttplug, Chains, Collar, Dildo,
            Nipple_Clamps, Rope, Spanking_Implement, Wrist_Restraints };

    public final static Toys Advanced[] = { Ball_Stretcher, Chastity_Device, Cockring, Enema_Bulb, Enema_Kit,
            EStim_Device, Humbler, Pussy_Clamps, Spreader_Bar, VaginalInsert, Vibrator };

    public final static Toys Other[] = { Doll, Husband, Strap_On, Wife };

    public final static Toys[] Categories[] = { Basic, Advanced, Other };
}
