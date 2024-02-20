package teaselib.core.ai.perception;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.TeaseLib;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Shower;

public final class ProximitySensor extends HumanPoseDeviceInteraction.EventListener {

    static final Logger logger = LoggerFactory.getLogger(ProximitySensor.class);

    private final TeaseLib teaseLib;

    private PoseAspects pose = PoseAspects.Unavailable;
    private Proximity previous = Proximity.FACE2FACE;

    public static final Map<Proximity, Double> zoom = new HashMap<>() {
        private static final long serialVersionUID = 1L;

        {
            put(Proximity.CLOSE, 1.728);
            put(Proximity.FACE2FACE, 1.2);
            put(Proximity.NEAR, 1.0);
            put(Proximity.FAR, 1.0);
            put(Proximity.AWAY, 1.0);
        }
    };

    ProximitySensor(TeaseLib teaseLib, Set<Interest> interests) {
        super(interests);
        this.teaseLib = teaseLib;
    }

    @Override
    public void run(PoseEstimationEventArgs eventArgs) {
        pose = eventArgs.pose;

        var presence = presence(pose);
        var proximity = proximity();
        var stream = stream(pose);

        var speechProximity = presence && (proximity == Proximity.FACE2FACE || proximity == Proximity.CLOSE);
        var focusLevel = stream ? (presence ? 1.0f : 0.0f) : 1.0f;

        logger.info("User Presence: {}\t User Proximity: {}, Stream: {}", presence, proximity, stream);

        // TODO proximity and blur visualization is out-of-sync with script-rendering
        // - when presence is false because pose estimation has not started or is just starting up
        // -> may result in blurred actor images before prompt
        teaseLib.host.setActorZoom(zoom.get(proximity));
        teaseLib.host.setFocusLevel(focusLevel);
        teaseLib.globals.get(Shower.class).updateUI(new InputMethod.UiEvent(speechProximity));
        teaseLib.host.show();
    }

    private static boolean stream(PoseAspects pose) {
        Optional<HumanPose.Status> aspect = pose.aspect(HumanPose.Status.class);
        HumanPose.Status stream;
        if (aspect.isPresent()) {
            stream = aspect.get();
        } else {
            stream = HumanPose.Status.None;
        }
        return stream == HumanPose.Status.Stream;
    }

    private static boolean presence(PoseAspects pose) {
        Optional<HumanPose.Status> aspect = pose.aspect(HumanPose.Status.class);
        HumanPose.Status presence;
        if (aspect.isPresent()) {
            presence = aspect.get();
        } else {
            presence = HumanPose.Status.None;
        }
        return presence != HumanPose.Status.None;
    }

    private Proximity proximity() {
        Optional<Proximity> aspect = pose.aspect(Proximity.class);
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

    public PoseAspects pose() {
        return pose;
    }
}
