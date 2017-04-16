package teaselib;

/**
 * Toys are generalized, which means their names have been chosen not be too
 * specific.
 * <p>
 * Toys are grouped by category, to make it possible to query a whole category.
 * <p>
 * I guess that because toys can be grouped (Gags, spanking implements, plugs,
 * clamps, restraints), there has to be a more advanced approach to organize
 * toys.
 * <p>
 * So for now, just the generalized version of something is added.
 * <p>
 * The list has been inspired by CyberMistress and SexScripts.
 * <p>
 *
 */
public enum Toys {
    Ankle_Restraints,
    Buttplug,
    Chastity_Device,
    Collar,
    Dildo,
    Gag,
    Spanking_Implement,
    Wrist_Restraints,

    Ball_Stretcher,
    Blindfold,
    Chains,
    Cockring,
    Doll,
    Enema_Bulb,
    Enema_Kit,

    Harness,
    Hood,
    Humbler,
    Leash,
    Nipple_clamps,
    Pussy_Clamps,
    Rope,
    Spreader_Bar,
    Strap_On,

    Husband,
    Wife,

    EStim_Device,

    VaginalInsert,;

    public enum ChastityDevices {
        Chastity_Belt,
        Chastity_Cage,
        Gates_of_Hell,
    }

    public enum Collars {
        Dog_Collar,
        Maid_Collar,
        Posture_Collar,
        Slave_Collar,
    }

    public enum Gags {
        Ball_Gag,
        Bit_Gag,
        Muzzle_Gag,
        Penis_Gag,
        Ring_Gag,
    }

    public enum Spanking_Implements {
        Cane,
        Crop,
        Flogger,
        Paddle,
        Hairbrush,
        Wooden_Spoon,
        Whip
    }

    public enum Anal {
        Beads,
        Plug
    }

    public enum Vaginal {
        Ben_Wa_Balls,
        VibratingEgg,
    }

    public final static Toys Basic[] = { Ankle_Restraints, Gag, Blindfold, Buttplug, Chains,
            Chastity_Device, Collar, Dildo, Nipple_clamps, Rope, Spanking_Implement, VaginalInsert,
            Wrist_Restraints };

    public final static Toys Advanced[] = { Ball_Stretcher, Cockring, Enema_Bulb, Enema_Kit,
            EStim_Device, Harness, Hood, Humbler, Leash, Pussy_Clamps };

    public final static Toys Other[] = { Doll, Husband, Spreader_Bar, Strap_On, Wife };

    public final static Toys[] Categories[] = { Basic, Advanced, Other };
}
