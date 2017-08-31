package teaselib.hosts;

import teaselib.Body;
import teaselib.Features;
import teaselib.Household;
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
 *         A set of pre-defined items that models almost the toys available in SexScripts. This means most of the toys
 *         are mapped to their SexScripts-equivalent.
 *         <li>Items are a fixed set, e.g. they're not enumerated from host settings.
 *         <li>All toys specialization enumerations are created and mapped. As a result, these items can also be made
 *         available by a script.
 */
public class PreDefinedItems extends AbstractUserItems {
    @Override
    protected Item[] createUserItems(TeaseLib teaseLib, String domain, QualifiedItem<?> item) {
        if (item.equals(Toys.Buttplug)) {
            Item standardButtplug = item(teaseLib, item, "buttplug", "A buttplug", Features.Anal, Toys.Anal.Plug);
            Item vibratingButtplug = item(teaseLib, item, "vibrating_buttplug", "A vibrating buttplug",
                    Features.Vibrating, Features.Anal, Toys.Anal.Plug);
            Item inflatableButtplug = item(teaseLib, item, "inflatable_buttplug", "An inflatable buttplug",
                    Features.Inflatable, Features.Anal, Toys.Anal.Plug);
            Item analBeads = item(teaseLib, item, "anal_beads", "Anal Beads", Features.Anal, Toys.Anal.Beads);

            return new Item[] { standardButtplug, vibratingButtplug, inflatableButtplug, analBeads };

        } else if (item.equals(Toys.Ankle_Restraints)) {
            Item ankleCuffs = item(teaseLib, item, "ankle_cuffs", "Metal Ankle cuffs", Material.Metal);
            Item leatherAnkleCuffs = item(teaseLib, item, "leather_ankle_cuffs", "Leather ankle cuffs",
                    Material.Leather);

            return new Item[] { ankleCuffs, leatherAnkleCuffs };

        } else if (item.equals(Toys.Wrist_Restraints)) {
            Item handCuffs = item(teaseLib, item, "handcuffs", "Metal Hand cuffs", Material.Metal);
            Item leatherWristCuffs = item(teaseLib, item, "leather_wrist_cuffs", "Leather wrist cuffs",
                    Material.Leather, Features.Detachable);

            return new Item[] { handCuffs, leatherWristCuffs };

        } else if (item.equals(Toys.Gag)) {
            Item ballGag = item(teaseLib, item, "ball_gag", "A ball gag", Toys.Gags.Ball_Gag, Material.Rubber);
            Item bitGag = item(teaseLib, item, "bit_gag", "A bit gag", Toys.Gags.Bit_Gag, Material.Rubber);
            Item muzzleGag = item(teaseLib, item, "muzzle_gag", "A muzzle gag", Toys.Gags.Muzzle_Gag, Material.Leather);
            Item penisGag = item(teaseLib, item, "penis_gag", "A penis gag", Toys.Gags.Penis_Gag, Material.Rubber);
            Item ringGag = item(teaseLib, item, "ring_gag", "A ring gag", Toys.Gags.Ring_Gag, Material.Metal,
                    Material.Leather);

            return new Item[] { ballGag, bitGag, muzzleGag, penisGag, ringGag };

        } else if (item.equals(Toys.Spanking_Implement)) {
            Item crop = item(teaseLib, item, "crop", "A crop", Toys.Spanking_Implements.Crop, Material.Leather,
                    Toys.Spanking_Implements.Crop);
            Item paddle = item(teaseLib, item, "paddle", "A paddle", Toys.Spanking_Implements.Paddle, Material.Leather);
            Item cane = item(teaseLib, item, "cane", "A cane", Toys.Spanking_Implements.Cane, Material.Wood);
            Item whip = item(teaseLib, item, "whip", "A whip", Toys.Spanking_Implements.Whip, Material.Leather);
            Item flogger = item(teaseLib, item, "flogger", "A flogger", Toys.Spanking_Implements.Flogger,
                    Material.Rubber);

            Item hairbrush = item(teaseLib, new QualifiedEnum(Household.Hairbrush), "hairbrush", "A hairbrush",
                    Material.Wood);
            Item woodenSpoon = item(teaseLib, new QualifiedEnum(Household.Wooden_Spoon), "wooden_spoon",
                    "A wooden spoon", Material.Wood);

            return new Item[] { crop, paddle, cane, whip, flogger, hairbrush, woodenSpoon };

        } else if (item.equals(Toys.Collar)) {
            Item dogCollar = item(teaseLib, item, "dog_collar", "A dog-collar", Toys.Collars.Dog_Collar,
                    Material.Leather);
            Item maidCollar = item(teaseLib, item, "maid_collar", "A maid-collar", Toys.Collars.Maid_Collar,
                    Material.Leather);
            Item postureCollar = item(teaseLib, item, "posture_collar", "Posture collar", Toys.Collars.Posture_Collar,
                    Material.Leather, Size.Large);
            Item slaveCollar = item(teaseLib, item, "collar", "A slave collar", Toys.Collars.Slave_Collar,
                    Material.Leather);

            return new Item[] { dogCollar, postureCollar, maidCollar, slaveCollar };

        } else if (item.equals(Toys.Nipple_Clamps)) {
            Item nippleClamps = item(teaseLib, item, "nipple_clamps", "Nipple clamps", Material.Metal);

            return new Item[] { nippleClamps };

        } else if (item.equals(Toys.Chastity_Device)) {
            Item chastityCage = item(teaseLib, item, "chastity_cage", "A chastity cage",
                    defaults(item, Body.AroundCockBase), Toys.ChastityDevices.Chastity_Cage, Features.Lockable);
            Item chastityBelt = item(teaseLib, item, "chastity_belt", "A Chastity belt", defaults(item, Body.OnBalls),
                    Toys.ChastityDevices.Chastity_Belt, Material.Metal, Features.Lockable);
            Item gatesOfHell = item(teaseLib, item, "gates_of_hell", "Gates of Hell",
                    new Enum<?>[] { Body.AroundCockBase, Body.OnPenis }, Toys.ChastityDevices.Gates_of_Hell,
                    Material.Leather, Material.Metal);

            return new Item[] { chastityCage, chastityBelt, gatesOfHell };

        } else if (item.equals(Toys.Dildo)) {
            Item analDildo = item(teaseLib, item, "anal_dildo", "Anal dildo", Material.Rubber, Features.Anal,
                    Features.SuctionCup);
            Item dildo = item(teaseLib, item, "dildo", "A dildo", Material.Rubber, Features.Vaginal,
                    Features.SuctionCup);
            Item vibratingDildo = item(teaseLib, item, "vibrating_dildo", "A vibrating dildo", Material.Rubber,
                    Features.Vaginal, Features.Vibrating);

            return new Item[] { dildo, vibratingDildo, analDildo };

        } else if (item.equals(Toys.VaginalInsert)) {
            Item vibratingEgg = item(teaseLib, item, "vibrating_egg", "A vibrating egg", Toys.Vaginal.VibratingEgg,
                    Features.Vibrating);
            Item benWaBalls = item(teaseLib, item, "ben_wa_balls", "Ben Wa Balls", Toys.Vaginal.Ben_Wa_Balls);

            return new Item[] { vibratingEgg, benWaBalls };

        } else if (item.equals(Toys.Vibrator)) {
            Item butterfly = item(teaseLib, item, "vibrator", "A butterfly vibrator", Toys.Vibrators.HandsFree,
                    Features.Vibrating);
            Item massager = item(teaseLib, item, "vibrator_massager", "A massager", Toys.Vibrators.Manual,
                    Features.Vibrating);

            return new Item[] { butterfly, massager };

        } else if (item.equals(Toys.EStim_Device)) {
            Item estimDevice = item(teaseLib, item, "estim", "An EStim device", Features.Vibrating);
            return new Item[] { estimDevice };

        } else if (item.equals(Toys.Ball_Stretcher)) {
            return new Item[] { item(teaseLib, item, "ball_stretcher", "A ball stretcher") };

        } else if (item.equals(Toys.Blindfold)) {
            return new Item[] { item(teaseLib, item, "blindfold", "A blindfold") };

        } else if (item.equals(Toys.Chains)) {
            return new Item[] { item(teaseLib, item, "chains", "Some chains") };

        } else if (item.equals(Toys.Rope)) {
            return new Item[] { item(teaseLib, item, "rope", "Plenty of rope") };

        } else if (item.equals(Toys.Cockring)) {
            return new Item[] { item(teaseLib, item, "cockring", "A tight cock ring"),
                    item(teaseLib, item, "cockring_with_two_rings", "A tight cock ring with an additional ring",
                            defaults(item, Body.OnPenis)) };

        } else if (item.equals(Toys.Enema_Kit)) {
            return new Item[] { item(teaseLib, item, "enema_kit", "An enema kit") };

        } else if (item.equals(Toys.Enema_Bulb)) {
            return new Item[] { item(teaseLib, item, "enema_bulb", "An enema bulb") };

        } else if (item.equals(Toys.Humbler)) {
            return new Item[] { item(teaseLib, item, "humbler", "A humbler") };

        } else if (item.equals(Toys.Pussy_Clamps)) {
            return new Item[] { item(teaseLib, item, "pussy_clamps", "Pussy clamps") };

        } else if (item.equals(Toys.Spreader_Bar)) {
            return new Item[] { item(teaseLib, item, "spreader_bar", "A spreader bar") };

        } else if (item.equals(Toys.Strap_On)) {
            return new Item[] { item(teaseLib, item, "strap_on", "A strap-on") };

        } else if (item.equals(Toys.Doll)) {
            return new Item[] { item(teaseLib, item, "doll", "A doll") };

        } else if (item.equals(Toys.Husband)) {
            return new Item[] { item(teaseLib, item, "husband", "Husband") };

        } else if (item.equals(Toys.Wife)) {
            return new Item[] { item(teaseLib, item, "wife", "Wife") };

        } else if (item.equals(Household.Leash)) {
            return new Item[] { item(teaseLib, item, "leash", "A leash"),
                    item(teaseLib, item, "retractable_leash", "A retractable leash", defaults(item, Body.Tethered)) };

        } else {
            return onlyTheOriginalItem(teaseLib, domain, item);
        }
    }

    protected Enum<?>[] defaults(QualifiedItem<?> item, Enum<?> more) {
        return array(defaults(item), more);
    }
}
