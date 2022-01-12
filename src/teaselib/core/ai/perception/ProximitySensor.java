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
        var stream = stream(eventArgs);

        var speechProximity = presence && proximity == Proximity.FACE2FACE;
        var focusLevel = stream ? (presence || proximity == Proximity.CLOSE ? 1.0f : 0.0f) : 1.0f;

        logger.info("User Presence: {}", presence);
        logger.info("User Proximity: {}", proximity);

        // TODO update during actor speech - disabled to reduce power consumption on older hardware
        // TODO faster updates for immediate response - disabled to reduce power consumption on older hardware
        // -> detect older hardware? Require fast CPU or decent GPU

        // TODO proximity and blur visualization is out-of-sync with script-rendering
        // - when presence is false because pose estimation has not started or is just starting up
        // -> may result in blurred actor images before prompt
        teaseLib.host.setActorProximity(proximity);
        teaseLib.host.setFocusLevel(focusLevel);
        teaseLib.globals.get(Shower.class).updateUI(new InputMethod.UiEvent(speechProximity));

        // TODO zoomed image rendered with a delay because
        // - image rendering in section renderer is not synchronized with proximity blurring
        // Showing the prompt renders the first showAll image and
        // proximity blur/zoom at the same time but completely unsynchronized
        teaseLib.host.show();
    }

    private static boolean stream(PoseEstimationEventArgs eventArgs) {
        Optional<HumanPose.Status> aspect = eventArgs.pose.aspect(HumanPose.Status.class);
        HumanPose.Status stream;
        if (aspect.isPresent()) {
            stream = aspect.get();
        } else {
            stream = HumanPose.Status.None;
        }
        return stream == HumanPose.Status.Stream;
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
            if (previous == Proximity.FACE2FACE) {
                // TODO handle left and right (up and down) to visualize
                // when the user is about to leave the field of view
                proximity = Proximity.NEAR;
                // TODO timeout missing presence to Proximity.AWAY
            } else if (previous == Proximity.CLOSE || previous == Proximity.NEAR || previous == Proximity.FAR) {
                proximity = previous;
            } else {
                proximity = Proximity.AWAY;
            }
        }
        return proximity;
    }
}