package teaselib.hosts;

import teaselib.Body;
import teaselib.Features;
import teaselib.HouseHold;
import teaselib.Material;
import teaselib.Size;
import teaselib.Toys;
import teaselib.core.AbstractUserItems;
import teaselib.core.TeaseLib;
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
    protected Item[] createUserItems(TeaseLib teaseLib, String domain, Object item) {
        if (item == Toys.Buttplug) {
            Body[] peers = { Body.SomethingInButt };

            Item standardButtplug = item(teaseLib, item, "Toys", "buttplug", "A buttplug", peers,
                    Features.Anal, Toys.Anal.Plug);
            Item vibratingButtplug = item(teaseLib, item, "Toys", "vibrating_buttplug",
                    "A vibrating buttplug", peers, Features.Vibrating, Features.Anal,
                    Toys.Anal.Plug);
            Item inflatableButtplug = item(teaseLib, item, "Toys", "inflatable_buttplug",
                    "An inflatable buttplug", peers, Features.Inflatable, Features.Anal,
                    Toys.Anal.Plug);
            Item analBeads = item(teaseLib, item, "Toys", "anal_beads", "Anal Beads", peers,
                    Features.Anal, Toys.Anal.Beads);

            return new Item[] { standardButtplug, vibratingButtplug, inflatableButtplug,
                    analBeads };

        } else if (item == Toys.Ankle_Restraints) {
            Body[] peers = { Body.AnklesTied };

            Item ankleCuffs = item(teaseLib, item, "Toys", "ankle_cuffs", "Metal Ankle cuffs",
                    peers, Material.Metal);
            Item leatherAnkleCuffs = item(teaseLib, item, "Toys", "leather_ankle_cuffs",
                    "Leather ankle cuffs", peers, Material.Leather);
            return new Item[] { ankleCuffs, leatherAnkleCuffs };
        } else if (item == Toys.Wrist_Restraints) {
            Body[] peers = { Body.WristsTied };

            Item handCuffs = item(teaseLib, item, "Toys", "handcuffs", "Metal Hand cuffs", peers,
                    Material.Metal);
            Item leatherWristCuffs = item(teaseLib, item, "Toys", "leather_wrist_cuffs",
                    "Leather wrist cuffs", peers, Material.Leather, Features.Detachable);

            return new Item[] { handCuffs, leatherWristCuffs };

        } else if (item == Toys.Gag) {
            Body[] peers = { Body.SomethingInMouth };

            Item ballGag = item(teaseLib, item, "Toys", "ball_gag", "A ball gag", peers,
                    Toys.Gags.Ball_Gag, Material.Rubber);
            Item bitGag = item(teaseLib, item, "Toys", "bit_gag", "A bit gag", peers,
                    Toys.Gags.Bit_Gag, Material.Rubber);
            Item muzzleGag = item(teaseLib, item, "Toys", "muzzle_gag", "A muzzle gag", peers,
                    Toys.Gags.Muzzle_Gag, Material.Leather);
            Item penisGag = item(teaseLib, item, "Toys", "penis_gag", "A penis gag", peers,
                    Toys.Gags.Penis_Gag, Material.Rubber);
            Item ringGag = item(teaseLib, item, "Toys", "ring_gag", "A ring gag", peers,
                    Toys.Gags.Ring_Gag, Material.Metal, Material.Leather);

            return new Item[] { ballGag, bitGag, muzzleGag, penisGag, ringGag };

        } else if (item == Toys.Spanking_Implement) {
            Body[] peers = {};

            Item crop = item(teaseLib, item, "Toys", "crop", "A crop", peers,
                    Toys.Spanking_Implements.Crop, Material.Leather, Toys.Spanking_Implements.Crop);
            Item paddle = item(teaseLib, item, "Toys", "paddle", "A paddle", peers,
                    Toys.Spanking_Implements.Paddle, Material.Leather);
            Item cane = item(teaseLib, item, "Toys", "cane", "A cane", peers,
                    Toys.Spanking_Implements.Cane, Material.Wood);
            Item whip = item(teaseLib, item, "Toys", "whip", "A whip", peers,
                    Toys.Spanking_Implements.Whip, Material.Leather);
            Item flogger = item(teaseLib, item, "Toys", "flogger", "A flogger", peers,
                    Toys.Spanking_Implements.Flogger, Material.Rubber);

            Item hairbrush = item(teaseLib, HouseHold.Hairbrush, "Toys", "hairbrush", "A hairbrush",
                    peers, Material.Wood);
            Item woodenSpoon = item(teaseLib, HouseHold.Wooden_Spoon, "Toys", "wooden_spoon",
                    "A wooden spoon", peers, Material.Wood);

            return new Item[] { crop, paddle, cane, whip, flogger, hairbrush, woodenSpoon };

        } else if (item == Toys.Collar) {
            Body[] peers = { Body.Collared };

            Item dogCollar = item(teaseLib, item, "Toys", "dog_collar", "A dog-collar", peers,
                    Toys.Collars.Maid_Collar, Material.Leather);
            Item maidCollar = item(teaseLib, item, "Toys", "maid_collar", "A maid-collar", peers,
                    Toys.Collars.Maid_Collar, Material.Leather);
            Item postureCollar = item(teaseLib, item, "Toys", "posture_collar", "Posture collar",
                    peers, Toys.Collars.Posture_Collar, Material.Leather, Size.Large);
            Item slaveCollar = item(teaseLib, item, "Toys", "collar", "A slave collar", peers,
                    Toys.Collars.Slave_Collar, Material.Leather);

            return new Item[] { dogCollar, postureCollar, maidCollar, slaveCollar };

        } else if (item == Toys.Chastity_Device) {
            Body[] peers = { Body.SomethingOnPenis, Body.CannotJerkOff };

            Item chastityCage = item(teaseLib, item, "Toys", "chastity_cage", "Chastity cage",
                    peers, Material.Metal, Features.Lockable);
            Item chastityBelt = item(teaseLib, item, "Toys", "chastity_belt", "Chastity belt",
                    peers, Material.Metal, Features.Lockable);
            Item gatesOfHell = item(teaseLib, item, "Toys", "gates_of_hell", "Gates of Hell", peers,
                    Material.Leather, Material.Metal);

            return new Item[] { chastityCage, chastityBelt, gatesOfHell };

        } else if (item == Toys.Dildo) {
            Body[] peers = {};

            Item analDildo = item(teaseLib, item, "Toys", "anal_dildo", "Anal dildo", peers,
                    Material.Rubber, Features.Anal, Features.SuctionCup);
            Item dildo = item(teaseLib, item, "Toys", "dildo", "A dildo", peers, Material.Rubber,
                    Features.Vaginal, Features.SuctionCup);
            Item vibratingDildo = item(teaseLib, item, "Toys", "vibrating_dildo",
                    "A vibrating dildo", peers, Material.Rubber, Features.Vaginal,
                    Features.Vibrating);

            return new Item[] { dildo, vibratingDildo, analDildo };

        } else if (item == Toys.VaginalInsert) {
            Body[] peers = { Body.SomethingInVagina };

            Item vibratingEgg = item(teaseLib, item, "Toys", "vibrating_egg", "Vibrating egg",
                    peers, Toys.Vaginal.VibratingEgg, Features.Vibrating);
            Item benWaBalls = item(teaseLib, item, "Toys", "ben_wa_balls", "Ben Wa Balls", peers,
                    Toys.Vaginal.Ben_Wa_Balls);

            return new Item[] { vibratingEgg, benWaBalls };

        } else if (item == Toys.Vibrator) {
            Body[] peers = { Body.SomethingOnClit };

            Item butterfly = item(teaseLib, item, "Toys", "vibrator", "Butterfly vibrator", peers,
                    Toys.Vibrators.HandsFree, Features.Vibrating);
            Item massager = item(teaseLib, item, "Toys", "vibrator_massager", "Massager", peers,
                    Toys.Vibrators.Manual, Features.Vibrating);

            return new Item[] { butterfly, massager };

        } else if (item == Toys.EStim_Device) {
            Body[] peers = {};

            Item estimDevice = item(teaseLib, item, "Toys", "estim", "EStim device", peers,
                    Features.Vibrating);
            return new Item[] { estimDevice };

        } else {
            return onlyTheOriginalItem(teaseLib, domain, item);
        }
    }
}
