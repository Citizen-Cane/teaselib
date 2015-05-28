/**
 * 
 */
package teaselib.persistence;

/**
 * Toys are generalized, which means their names have been chosen not be too
 * specific.
 * 
 * I guess that because toys can be grouped (Gags, spanking implements, plugs,
 * clamps, restraints), there has to be a more advanced approach to organize
 * toys.
 * 
 * So for now, just the generalized version of something is added.
 * 
 * The list has been inspired by CyberMistress and SexScripts.
 * 
 * @author someone
 *
 */
public enum Toys {
    Ankle_Cuffs, Anal_Dildo, Anal_Beads, Ass_Hook, Baby_Oil, Ball_Gag, Ball_Stretcher, Bells, Belt, Ben_Wa_Balls, Bit_Gag, Blindfold, Breast_Forms, Buttplug, Camera, Candle, Cane, Chains, Chastity_Belt, Chastity_Cage, Cigarette, Clothespins, Cockring, Collar, Condoms, Crop, Diaper, Dildo, Dog_Collar, Duct_Tape, Doll, Enema_Bulb, Enema_Kit, Estim, Flogger, Gates_Of_Hell, Hairbrush, Handcuffs, Heat_Rub, Hood, Humbler, Husband, Ice_Tray, Inflatable_Buttplug, Inflatable_Dildo, Leash, Leather_Harness, Leather_Ankle_Cuffs, Leather_Wrist_Cuffs, Lube, Muzzle_Gag, Nipple_clamps, Paddle, Padlocks, Penis_Gag, Penis_Pump, Plants, Posture_Collar, Princess_Crown, Pussy_Clamps, Ring_Gag, Rope, Rubber_bands, Ruler, Shoe_Lace, Shrinkwrap, Spreader_Bar, Strap_On, Tampon, Vibrating_Buttplug, Vibrating_Dildo, Vibrating_Egg, Vibrator, Wax, Weights, Whip, Wife, Wooden_Spoon;

    public final static Toys AnalToys[] = { Toys.Anal_Beads, Toys.Ass_Hook,
            Toys.Anal_Dildo, Toys.Buttplug, Toys.Inflatable_Buttplug,
            Toys.Vibrating_Buttplug };

    public final static Toys VaginalToys[] = { Toys.Ben_Wa_Balls,
            Toys.Anal_Dildo, Toys.Dildo, Toys.Inflatable_Dildo,
            Toys.Vibrating_Dildo, Toys.Vibrating_Egg, Toys.Vibrator };

    public final static Toys Buttplugs[] = { Toys.Buttplug,
            Toys.Inflatable_Buttplug, Toys.Vibrating_Buttplug };

    public final static Toys Dildos[] = { Toys.Anal_Dildo, Toys.Dildo,
            Toys.Inflatable_Dildo, Toys.Vibrating_Dildo };

    public final static Toys Gags[] = { Toys.Ball_Gag, Toys.Bit_Gag,
            Toys.Penis_Gag, Toys.Ring_Gag };

    public final static Toys Chastity_Devices[] = { Toys.Chastity_Belt,
            Toys.Chastity_Cage };

    public final static Toys Collars[] = { Toys.Dog_Collar, Toys.Collar,
            Toys.Posture_Collar };

    public final static Toys Vibrators[] = { Toys.Vibrator, Toys.Vibrating_Egg,
            Toys.Vibrating_Dildo };

    public final static Toys Spanking_Implements[] = { Toys.Belt, Toys.Cane,
            Toys.Crop, Toys.Flogger, Toys.Paddle, Toys.Ruler, Toys.Hairbrush,
            Toys.Wooden_Spoon, Toys.Whip };

    public final static Toys Ankle_Restraints[] = { Toys.Ankle_Cuffs,
            Toys.Leather_Ankle_Cuffs };

    public final static Toys Wrist_Restraints[] = { Toys.Handcuffs,
            Toys.Leather_Wrist_Cuffs };

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
