package teaselib.core.ai.perception;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.TeaseLib;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Shower;

final class ProximitySensor extends HumanPoseDeviceInteraction.EventListener {

    static final Logger logger = LoggerFactory.getLogger(ProximitySensor.class);

    private final TeaseLib teaseLib;

    private Proximity previous = Proximity.FACE2FACE;

    ProximitySensor(TeaseLib teaseLib, Set<Interest> interests) {
        super(interests);
        this.teaseLib = teaseLib;
    }

    @Override
    public void run(PoseEstimationEventArgs eventArgs) throws InterruptedException {
        var presence = presence(eventArgs);
        var proximity = proximity(eventArgs);
        boolean speechProximity = presence && proximity == Proximity.FACE2FACE;

        logger.info("User Presence: {}", presence);
        logger.info("User Proximity: {}", proximity);
        teaseLib.host.setActorProximity(proximity);
        teaseLib.globals.get(Shower.class).updateUI(new InputMethod.UiEvent(speechProximity));
        teaseLib.host.show();
    }

    private static boolean presence(PoseEstimationEventArgs eventArgs) {
        Optional<HumanPose.Status> aspect = eventArgs.pose.aspect(HumanPose.Status.class);
        HumanPose.Status presence;
        if (aspect.isPresent()) {
            presence = aspect.get();
        } else {
            presence = HumanPose.Status.Available;
        }
        return presence != HumanPose.Status.None;
    }

    private Proximity proximity(PoseEstimationEventArgs eventArgs) {
        Optional<Proximity> aspect = eventArgs.pose.aspect(Proximity.class);
        Proximity proximity;
        if (aspect.isPresent()) {
            proximity = aspect.get();
            previous = proximity;
        } else {
            if (previous == Proximity.CLOSE || previous == Proximity.FACE2FACE) {
                proximity = Proximity.NEAR;
            } else {
                proximity = previous;
            }
        }
        return proximity;
    }
}