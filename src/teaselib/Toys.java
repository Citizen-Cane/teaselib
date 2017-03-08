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
    Ankle_Cuffs,
    Anal_Dildo,
    Anal_Beads,
    Ass_Hook,
    Baby_Oil,
    Ball_Gag,
    Ball_Stretcher,
    Bell,
    Belt,
    Ben_Wa_Balls,
    Bit_Gag,
    Blindfold,
    Buttplug,
    Candle,
    Cane,
    Chains,
    Chastity_Belt,
    Chastity_Cage,
    Chastity_Device_Lock,
    Cigarette,
    Clothespins,
    Cockring,
    Collar,
    Condoms,
    Crop,
    Diaper,
    Dildo,
    Dog_Collar,
    Duct_Tape,
    Doll,
    Enema_Bulb,
    Enema_Kit,
    EStim_Device,
    EStim_Computer_Controlled_Device,
    Flogger,
    Gates_Of_Hell,
    Hairbrush,
    Handcuffs,
    Harness,
    Heat_Rub,
    Hood,
    Humbler,
    Husband,
    Ice_Tray,
    Inflatable_Buttplug,
    Inflatable_Dildo,
    Leash,
    Leather_Ankle_Cuffs,
    Leather_Wrist_Cuffs,
    Lube,
    Muzzle_Gag,
    Nipple_clamps,
    Paddle,
    Padlocks,
    Penis_Gag,
    Penis_Pump,
    Plants,
    Posture_Collar,
    Pussy_Clamps,
    Ring_Gag,
    Rope,
    Rubber_bands,
    Ruler,
    SelfBondage_TimeLock,
    SelfBondage_TimeLock_Computer_Controlled,
    Shoe_Lace,
    Shrinkwrap,
    Spreader_Bar,
    Strap_On,
    Tampon,
    Vibrating_Buttplug,
    Vibrating_Dildo,
    Vibrating_Egg,
    Vibrating_Computer_Controlled_Device,
    Vibrator,
    Webcam,
    Weights,
    Whip,
    Wife,
    Wooden_Spoon,;

    /**
     * The basic toy set, without specializations.
     */
    public final static Toys Basic[] = { Ankle_Cuffs, Anal_Dildo, Ball_Gag,
            Bit_Gag, Blindfold, Buttplug, Cane, Chains, Chastity_Cage, Collar,
            Dildo, Handcuffs, Humbler, Nipple_clamps, Paddle, Pussy_Clamps,
            Ring_Gag, Rope, Vibrator };

    public final static Toys Advanced[] = { Anal_Beads, Ball_Stretcher,
            Ben_Wa_Balls, Cockring, Crop, Doll, Enema_Bulb, Enema_Kit,
            EStim_Device, Gates_Of_Hell, Harness, Hood, Inflatable_Buttplug,
            Inflatable_Dildo, Leather_Ankle_Cuffs, Leather_Wrist_Cuffs,
            Muzzle_Gag, Penis_Gag, Whip };

    public final static Toys Other[] = { Ass_Hook, Chastity_Belt, Diaper,
            Flogger, Penis_Pump, Posture_Collar, Spreader_Bar,
            SelfBondage_TimeLock, Strap_On, Vibrating_Buttplug, Vibrating_Dildo,
            Vibrating_Egg };

    public final static Toys ComputerDevices[] = {
            EStim_Computer_Controlled_Device,
            SelfBondage_TimeLock_Computer_Controlled,
            Vibrating_Computer_Controlled_Device, Webcam };

    public final static Toys HouseHoldItems[] = { Baby_Oil, Bell, Belt, Candle,
            Cigarette, Clothespins, Condoms, Dog_Collar, Duct_Tape, Hairbrush,
            Heat_Rub, Husband, Ice_Tray, Leash, Lube, Padlocks, Plants,
            Rubber_bands, Ruler, Shoe_Lace, Shrinkwrap, Tampon, Weights, Wife,
            Wooden_Spoon };

    public final static Toys[] Categories[] = { Basic, Advanced, Other,
            ComputerDevices, HouseHoldItems };

    // More categories

    public final static Toys AnalToys[] = { Toys.Anal_Beads, Toys.Ass_Hook,
            Toys.Anal_Dildo, Toys.Buttplug, Toys.Inflatable_Buttplug,
            Toys.Vibrating_Buttplug };

    public final static Toys VaginalToys[] = { Toys.Ben_Wa_Balls, Toys.Dildo,
            Toys.Inflatable_Dildo, Toys.Vibrating_Dildo, Toys.Vibrating_Egg,
            Toys.Vibrator };

    public final static Toys Buttplugs[] = { Toys.Buttplug,
            Toys.Inflatable_Buttplug, Toys.Vibrating_Buttplug };

    public final static Toys Dildos[] = { Toys.Anal_Dildo, Toys.Dildo,
            Toys.Inflatable_Dildo, Toys.Vibrating_Dildo };

    public final static Toys EStim_Devices[] = { Toys.EStim_Device,
            Toys.EStim_Computer_Controlled_Device };

    public final static Toys Gags[] = { Toys.Ball_Gag, Toys.Bit_Gag,
            Toys.Muzzle_Gag, Toys.Penis_Gag, Toys.Ring_Gag };

    public final static Toys Chastity_Devices[] = { Toys.Chastity_Belt,
            Toys.Chastity_Cage, Toys.Gates_Of_Hell };

    public final static Toys Collars[] = { Toys.Collar, Toys.Dog_Collar,
            Toys.Posture_Collar };

    public final static Toys Vibrators[] = { Toys.Vibrator, Toys.Vibrating_Egg,
            Toys.Vibrating_Dildo, Toys.Vibrating_Computer_Controlled_Device };

    public final static Toys Spanking_Implements[] = { Toys.Belt, Toys.Cane,
            Toys.Crop, Toys.Flogger, Toys.Paddle, Toys.Ruler, Toys.Hairbrush,
            Toys.Wooden_Spoon, Toys.Whip };

    public final static Toys Ankle_Restraints[] = { Toys.Ankle_Cuffs,
            Toys.Leather_Ankle_Cuffs };

    public final static Toys Wrist_Restraints[] = { Toys.Handcuffs,
            Toys.Leather_Wrist_Cuffs };
}
