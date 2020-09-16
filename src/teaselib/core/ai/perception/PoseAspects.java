package teaselib.core.ai.perception;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import teaselib.core.ai.perception.HumanPose.HeadGestures;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.HumanPose.Status;

public class PoseAspects {
    public static final PoseAspects Unavailable = new PoseAspects();

    public final HumanPose.Estimation pose;

    private final Set<Interest> interests;
    private final Set<PoseAspect> aspects;

    PoseAspects() {
        this.pose = HumanPose.Estimation.NONE;
        this.interests = Collections.emptySet();
        this.aspects = Collections.singleton(Status.None);
    }

    PoseAspects(HumanPose.Estimation pose, Set<Interest> interests, PoseAspects previous) {
        this.pose = pose;
        this.interests = interests;
        this.aspects = new HashSet<>();
        aspects.add(Status.Available);

        if (interests.contains(Interest.Proximity)) {
            Optional<Proximity> previousProximity = previous.aspect(Proximity.class);
            Proximity proximity = pose.proximity();
            if (previousProximity.isPresent() && previousProximity.get().isCloserThan(proximity)) {
                aspects.add(pose.proximityWithFactor(1.2f));
            } else {
                aspects.add(proximity);
            }

            // TODO max-reliable distance for inference model to avoid drop-outs in the distance
            if (interests.contains(Interest.HeadGestures)) {
                if (proximity == Proximity.FACE2FACE) {
                    aspects.add(HeadGestures.Gaze);
                    aspects.add(Status.Stream);
                } else {
                    aspects.add(HeadGestures.None);
                }
            }
        }
    }

    public boolean is(Interest interest) {
        return interests.contains(interest);
    }

    public boolean is(PoseAspect... values) {
        for (PoseAspect value : values) {
            if (aspects.contains(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(Interest... values) {
        for (Interest value : values) {
            if (!interests.contains(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aspects == null) ? 0 : aspects.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PoseAspects other = (PoseAspects) obj;
        if (aspects == null) {
            if (other.aspects != null)
                return false;
        } else if (!aspects.equals(other.aspects))
            return false;
        return true;
    }

    public <T extends HumanPose.PoseAspect> Optional<T> aspect(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        Optional<T> element = (Optional<T>) aspects.stream().filter(a -> a.getClass() == clazz).findFirst();
        return element;
    }

    @Override
    public String toString() {
        return aspects.toString();
    }

}