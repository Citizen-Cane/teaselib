package teaselib.hosts;

import teaselib.Body;
import teaselib.Features;
import teaselib.HouseHold;
import teaselib.Material;
import teaselib.Size;
import teaselib.Toys;
import teaselib.core.AbstractUserItems;
import teaselib.core.TeaseLib;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;

/**
 * @author Citizen-Cane
 *
 *         A set of pre-defined items that models almost the toys available in
 *         SexScripts. This means most of the toys are mapped to their
 *         SexScripts-equivalent.
 *         <li>Items are a fixed set, e.g. they're not enumerated from host
 *         settings.
 *         <li>All toys specialization enumerations are created and mapped. As a
 *         result, these items can also be made available by a script.
 */
public class PreDefinedItems extends AbstractUserItems {
    @Override
    protected Item[] createUserItems(TeaseLib teaseLib, String domain, QualifiedItem<?> item) {
        if (item.equals(Toys.Buttplug)) {
            Item standardButtplug = item(teaseLib, item, "Toys", "buttplug", "A buttplug", Features.Anal,
                    Toys.Anal.Plug);
            Item vibratingButtplug = item(teaseLib, item, "Toys", "vibrating_buttplug", "A vibrating buttplug",
                    Features.Vibrating, Features.Anal, Toys.Anal.Plug);
            Item inflatableButtplug = item(teaseLib, item, "Toys", "inflatable_buttplug", "An inflatable buttplug",
                    Features.Inflatable, Features.Anal, Toys.Anal.Plug);
            Item analBeads = item(teaseLib, item, "Toys", "anal_beads", "Anal Beads", Features.Anal, Toys.Anal.Beads);

            return new Item[] { standardButtplug, vibratingButtplug, inflatableButtplug, analBeads };

        } else if (item.equals(Toys.Ankle_Restraints)) {
            Item ankleCuffs = item(teaseLib, item, "Toys", "ankle_cuffs", "Metal Ankle cuffs", Material.Metal);
            Item leatherAnkleCuffs = item(teaseLib, item, "Toys", "leather_ankle_cuffs", "Leather ankle cuffs",
                    Material.Leather);

            return new Item[] { ankleCuffs, leatherAnkleCuffs };

        } else if (item.equals(Toys.Wrist_Restraints)) {
            Item handCuffs = item(teaseLib, item, "Toys", "handcuffs", "Metal Hand cuffs", Material.Metal);
            Item leatherWristCuffs = item(teaseLib, item, "Toys", "leather_wrist_cuffs", "Leather wrist cuffs",
                    Material.Leather, Features.Detachable);

            return new Item[] { handCuffs, leatherWristCuffs };

        } else if (item.equals(Toys.Gag)) {
            Item ballGag = item(teaseLib, item, "Toys", "ball_gag", "A ball gag", Toys.Gags.Ball_Gag, Material.Rubber);
            Item bitGag = item(teaseLib, item, "Toys", "bit_gag", "A bit gag", Toys.Gags.Bit_Gag, Material.Rubber);
            Item muzzleGag = item(teaseLib, item, "Toys", "muzzle_gag", "A muzzle gag", Toys.Gags.Muzzle_Gag,
                    Material.Leather);
            Item penisGag = item(teaseLib, item, "Toys", "penis_gag", "A penis gag", Toys.Gags.Penis_Gag,
                    Material.Rubber);
            Item ringGag = item(teaseLib, item, "Toys", "ring_gag", "A ring gag", Toys.Gags.Ring_Gag, Material.Metal,
                    Material.Leather);

            return new Item[] { ballGag, bitGag, muzzleGag, penisGag, ringGag };

        } else if (item.equals(Toys.Spanking_Implement)) {
            Item crop = item(teaseLib, item, "Toys", "crop", "A crop", Toys.Spanking_Implements.Crop, Material.Leather,
                    Toys.Spanking_Implements.Crop);
            Item paddle = item(teaseLib, item, "Toys", "paddle", "A paddle", Toys.Spanking_Implements.Paddle,
                    Material.Leather);
            Item cane = item(teaseLib, item, "Toys", "cane", "A cane", Toys.Spanking_Implements.Cane, Material.Wood);
            Item whip = item(teaseLib, item, "Toys", "whip", "A whip", Toys.Spanking_Implements.Whip, Material.Leather);
            Item flogger = item(teaseLib, item, "Toys", "flogger", "A flogger", Toys.Spanking_Implements.Flogger,
                    Material.Rubber);

            Item hairbrush = item(teaseLib, new QualifiedEnum(HouseHold.Hairbrush), "Toys", "hairbrush", "A hairbrush",
                    Material.Wood);
            Item woodenSpoon = item(teaseLib, new QualifiedEnum(HouseHold.Wooden_Spoon), "Toys", "wooden_spoon",
                    "A wooden spoon", Material.Wood);

            return new Item[] { crop, paddle, cane, whip, flogger, hairbrush, woodenSpoon };

        } else if (item.equals(Toys.Collar)) {
            Item dogCollar = item(teaseLib, item, "Toys", "dog_collar", "A dog-collar", Toys.Collars.Dog_Collar,
                    Material.Leather);
            Item maidCollar = item(teaseLib, item, "Toys", "maid_collar", "A maid-collar", Toys.Collars.Maid_Collar,
                    Material.Leather);
            Item postureCollar = item(teaseLib, item, "Toys", "posture_collar", "Posture collar",
                    Toys.Collars.Posture_Collar, Material.Leather, Size.Large);
            Item slaveCollar = item(teaseLib, item, "Toys", "collar", "A slave collar", Toys.Collars.Slave_Collar,
                    Material.Leather);

            return new Item[] { dogCollar, postureCollar, maidCollar, slaveCollar };

        } else if (item.equals(Toys.Nipple_clamps)) {
            Item nippleClamps = item(teaseLib, item, "Toys", "nipple_clamps", "Nipple clamps", Material.Metal);

            return new Item[] { nippleClamps };

        } else if (item.equals(Toys.Chastity_Device)) {
            Item chastityCage = item(teaseLib, item, "Toys", "chastity_cage", "Chastity cage", Material.Metal,
                    Features.Lockable);
            Item chastityBelt = item(teaseLib, item, "Toys", "chastity_belt", "Chastity belt",
                    array(defaults(item), Body.SomethingOnBalls), Material.Metal, Features.Lockable);
            Item gatesOfHell = item(teaseLib, item, "Toys", "gates_of_hell", "Gates of Hell", Material.Leather,
                    Material.Metal);

            return new Item[] { chastityCage, chastityBelt, gatesOfHell };

        } else if (item.equals(Toys.Dildo)) {
            Item analDildo = item(teaseLib, item, "Toys", "anal_dildo", "Anal dildo", Material.Rubber, Features.Anal,
                    Features.SuctionCup);
            Item dildo = item(teaseLib, item, "Toys", "dildo", "A dildo", Material.Rubber, Features.Vaginal,
                    Features.SuctionCup);
            Item vibratingDildo = item(teaseLib, item, "Toys", "vibrating_dildo", "A vibrating dildo", Material.Rubber,
                    Features.Vaginal, Features.Vibrating);

            return new Item[] { dildo, vibratingDildo, analDildo };

        } else if (item.equals(Toys.VaginalInsert)) {
            Item vibratingEgg = item(teaseLib, item, "Toys", "vibrating_egg", "Vibrating egg",
                    Toys.Vaginal.VibratingEgg, Features.Vibrating);
            Item benWaBalls = item(teaseLib, item, "Toys", "ben_wa_balls", "Ben Wa Balls", Toys.Vaginal.Ben_Wa_Balls);

            return new Item[] { vibratingEgg, benWaBalls };

        } else if (item.equals(Toys.Vibrator)) {
            Item butterfly = item(teaseLib, item, "Toys", "vibrator", "Butterfly vibrator", Toys.Vibrators.HandsFree,
                    Features.Vibrating);
            Item massager = item(teaseLib, item, "Toys", "vibrator_massager", "Massager", Toys.Vibrators.Manual,
                    Features.Vibrating);

            return new Item[] { butterfly, massager };

        } else if (item.equals(Toys.EStim_Device)) {
            Item estimDevice = item(teaseLib, item, "Toys", "estim", "EStim device", Features.Vibrating);
            return new Item[] { estimDevice };

        } else {
            return onlyTheOriginalItem(teaseLib, domain, item);
        }
    }
}
