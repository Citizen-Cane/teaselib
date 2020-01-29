package teaselib;

/**
 *
 * @author Citizen-Cane
 *
 */
public enum Posture {
    AnklesTiedTogether, // Duplicate of Body.AnklesTied -> Remove
    WristsTiedInFront, // Redundant - default of Body.WristsTied -> Remove
    WristsTiedBehindBack, // Correct, changes pose

    CantCrawl,
    CantKneel,
    CantProne,
    CantReachCrotch,
    CantSitOnChair,
    CantStand, // TODO if this means kneeling UpRight, then there's no difference to CantProne
    CantTypeOrUseMouse,
}
