package teaselib.core;

import java.util.Collection;

import teaselib.Accessoires;
import teaselib.Body;
import teaselib.Bondage;
import teaselib.Clothes;
import teaselib.Features;
import teaselib.Gadgets;
import teaselib.Household;
import teaselib.Posture;
import teaselib.Shoes;
import teaselib.Toys;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.QualifiedStringMapping;

public class UserItemsLogic {

    public Enum<?>[] defaultPeers(QualifiedString item) {
        if (item.namespace().equalsIgnoreCase(Accessoires.class.getName())) {
            return getAccessoiresDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Bondage.class.getName())) {
            return getBondageDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Clothes.class.getName())) {
            return getClothesDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Gadgets.class.getName())) {
            return getGadgetsDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Household.class.getName())) {
            return getHouseholdDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Shoes.class.getName())) {
            return getShoesDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Toys.class.getName())) {
            return getToyDefaults(item);
        } else {
            throw new IllegalArgumentException("Undefined defaults for " + item);
        }
    }

    private Enum<?>[] getAccessoiresDefaults(QualifiedString item) {
        if (item.is(Accessoires.Breast_Forms)) {
            return peers(Body.OnNipples);
        } else {
            return None;
        }
    }

    private Enum<?>[] getBondageDefaults(QualifiedString item) {
        if (item.is(Bondage.Chains)) {
            return None;
        } else if (item.is(Bondage.Rope)) {
            return None;
        } else if (item.is(Bondage.Spreader_Bar)) {
            return None;
        } else if (item.is(Bondage.Anklets)) {
            return peers(Body.AnklesCuffed);
        } else if (item.is(Bondage.Wristlets)) {
            return peers(Body.WristsCuffed);
        } else {
            return None;
        }
    }

    private Enum<?>[] getClothesDefaults(@SuppressWarnings("unused") QualifiedString item) {
        return None;
    }

    private Enum<?>[] getGadgetsDefaults(@SuppressWarnings("unused") QualifiedString item) {
        return None;
    }

    private Enum<?>[] getHouseholdDefaults(@SuppressWarnings("unused") QualifiedString item) {
        return None;
    }

    private Enum<?>[] getShoesDefaults(@SuppressWarnings("unused") QualifiedString item) {
        return None;
    }

    /**
     * Get system default peers for items. The defaults depend on the meaning of the item. Item have defaults if they
     * have a clear application or are applied to a distinct spot, like putting a collar around one's neck. Same for
     * ankle restraints.
     * <p>
     * On the other hand wrist restraints don't have a Posture default, since wrists can be tied before and behind the
     * body.
     * 
     * @param item
     *            The item to get defaults for.
     * @return The defaults for the item. An item may not have defaults, in this case the returned array is empty.
     */
    private Enum<?>[] getToyDefaults(QualifiedString item) {
        if (item.is(Toys.Buttplug)) {
            return peers(Body.InButt);
        } else if (item.is(Toys.Ankle_Restraints)) {
            return peers(Body.AnklesCuffed, Body.AnklesTied, Posture.AnklesTiedTogether);
        } else if (item.is(Toys.Wrist_Restraints)) {
            return peers(Body.WristsCuffed, Body.WristsTied);
        } else if (item.is(Toys.Gag)) {
            return peers(Body.InMouth);
        } else if (item.is(Toys.Spanking_Implement)) {
            return None;
        } else if (item.is(Toys.Collar)) {
            return peers(Body.AroundNeck);
        } else if (item.is(Toys.Nipple_Clamps)) {
            return peers(Body.OnNipples);
        } else if (item.is(Toys.Chastity_Device)) {
            return peers( //
                    Body.AroundCockBase, // Not for chastity belts with crotch band
                    Body.OnPenis, //
                    Body.CantJerkOff);
        } else if (item.is(Toys.Dildo)) {
            return None;
        } else if (item.is(Toys.VaginalInsert)) {
            return peers(Body.InVagina);
        } else if (item.is(Toys.Vibrator)) {
            return None;
        } else if (item.is(Toys.Ball_Stretcher)) {
            return peers(Body.OnBalls);
        } else if (item.is(Toys.Blindfold)) {
            return peers(Body.Blindfolded);
        } else if (item.is(Toys.Cock_Ring)) {
            return peers(Body.AroundCockBase);
        } else if (item.is(Toys.Anal_Douche)) {
            return peers(Body.InButt);
        } else if (item.is(Toys.Enema_Bulb)) {
            return peers(Body.InButt);
        } else if (item.is(Toys.Enema_Kit)) {
            return peers(Body.InButt);
        } else if (item.is(Toys.Glans_Ring)) {
            return peers(Body.OnPenis);
        } else if (item.is(Toys.Humbler)) {
            return peers(Body.OnBalls, Posture.CantStand, Posture.CantSitOnChair);
        } else if (item.is(Toys.Masturbator)) {
            return None;
        } else if (item.is(Toys.Pussy_Clamps)) {
            return peers(Body.OnLabia, Body.OnBalls);
        } else if (item.is(Toys.Clit_Clamp)) {
            return peers(Body.OnClit, Body.OnPenis);
        } else if (item.is(Toys.EStim_Device)) {
            return None;
        } else if (item.is(Toys.Doll)) {
            return None;
        } else if (item.is(Toys.Spouse)) {
            return None;
        } else {
            throw new IllegalArgumentException("Defaults not defined for " + item);
        }
    }

    private static final Enum<?>[] None = new Enum<?>[] {};

    private static Enum<?>[] peers(Enum<?>... state) {
        return state;
    }

    Enum<?>[] physicallyBlockingPeers(QualifiedString state) {
        if (state.is(Toys.Buttplug)) {
            return blockedBy(Body.CrotchRoped);
        } else if (state.is(Toys.Cock_Ring)) {
            return blockedBy(Body.OnBalls, Body.OnPenis);
        } //
        else if (state.is(Clothes.Babydoll)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Blouse)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Body)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Bra)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Catsuit)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether, Body.WristsTied, Posture.WristsTiedBehindBack,
                    Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Dress)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Jacket)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Leotard)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Nightie)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Pajamas)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether, Body.WristsTied, Posture.WristsTiedBehindBack,
                    Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Pantyhose)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether);
        } else if (state.is(Clothes.Shirt)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Stockings)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether);
        } else if (state.is(Clothes.Suit)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Tanktop)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Trousers)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether);
        } else if (state.is(Clothes.Vest)) {
            return blockedBy(Body.WristsTied, Posture.WristsTiedBehindBack, Posture.WristsTiedInFront);
        } else if (state.is(Clothes.Underpants)) {
            return blockedBy(Body.AnklesTied, Posture.AnklesTiedTogether);
        } //
        else if (state.is(Shoes.class)) {
            return blockedBy(Clothes.Socks, Clothes.Pantyhose, Clothes.Stockings);
        } else {
            return NotBlocked;
        }
    }

    Enum<?>[] logicallyBlockingPeers(QualifiedString state) {
        if (state.is(Clothes.Underpants)) {
            // TODO allow panties/catsuit/pantyhose Ouvert
            return blockedBy(Clothes.Catsuit, Clothes.Pantyhose, Clothes.Trousers);
        } //
        else if (state.is(Shoes.class)) {
            return blockedBy(Clothes.Socks, Clothes.Pantyhose, Clothes.Stockings);
        } else if (state.is(Clothes.Socks) || state.is(Clothes.Pantyhose) || state.is(Clothes.Stockings)) {
            return blockedBy(Shoes.values());
        } else {
            return NotBlocked;
        }
    }

    void applyPeerRules(QualifiedString kind, Collection<QualifiedString> defaultPeers,
            Collection<QualifiedString> itemAttributes, Collection<QualifiedString> itemBlockers) {
        for (QualifiedString peer : defaultPeers) {
            itemBlockers.addAll(QualifiedStringMapping.of(peerBlockers(peer)));
        }
    }

    private Enum<?>[] peerBlockers(QualifiedString peer) {
        if (peer.is(Body.InButt) || peer.is(Body.InVagina)) {
            return blockedBy(Body.CrotchRoped);
        } else if (peer.is(Body.AroundCockBase)) {
            return blockedBy(Body.OnBalls);
        } else {
            return NotBlocked;
        }
    }

    private final Enum<?>[] NotBlocked = new Enum<?>[] {};

    private static Enum<?>[] blockedBy(Enum<?>... state) {
        return state;
    }

    void relaxBlockingRules(QualifiedString kind, Collection<QualifiedString> defaultPeers,
            Collection<QualifiedString> itemAttributes, Collection<QualifiedString> itemBlockers) {
        if (kind.is(Toys.Cock_Ring)) {
            if (itemAttributes.contains(QualifiedString.of(Features.Detachable))) {
                itemBlockers.remove(QualifiedString.of(Body.OnBalls));
            }
        }
    }

}
