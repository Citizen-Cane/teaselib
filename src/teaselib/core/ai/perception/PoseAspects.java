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
    private static final float DISTANCE_RETREAT_FACTOR = 1.25f;

    public static final PoseAspects Unavailable = new PoseAspects();

    public final HumanPose.Estimation estimation;

    final long timestamp;
    final Set<Interest> interests;
    private final Set<PoseAspect> aspects;

    PoseAspects() {
        this.estimation = HumanPose.Estimation.NONE;
        this.timestamp = 0;
        this.interests = Collections.singleton(Interest.Status);
        this.aspects = Collections.singleton(Status.None);
    }

    PoseAspects(HumanPose.Estimation pose, long timestamp, Set<Interest> interests) {
        this(pose, timestamp, interests, Unavailable);
    }

    PoseAspects(HumanPose.Estimation pose, long timestamp, Set<Interest> interests, PoseAspects previous) {
        this.estimation = pose;
        this.timestamp = timestamp;
        this.interests = Collections.unmodifiableSet(interests);
        this.aspects = new HashSet<>();
        aspects.add(Status.Available);

        if (interests.contains(Interest.Proximity)) {
            Optional<Proximity> previousProximity = previous.aspect(Proximity.class);
            Proximity proximity = pose.proximity();
            if (proximity == null) {
                previousProximity.ifPresent(aspects::add);
            } else {
                aspects.add(previousProximity
                        .filter(p -> p.isCloserThan(proximity))
                        .map(p -> pose.proximity(DISTANCE_RETREAT_FACTOR))
                        .orElse(proximity));
            }

            if (interests.contains(Interest.HeadGestures)) {
                if (proximity == Proximity.FACE2FACE && pose.head.isPresent()) {
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

    /**
     * @param values
     *            Aspects to test. Multiple aspects might be passed for each aspect class.
     * @return {@code true} if there is a match for each aspect class.
     */
    public boolean is(PoseAspect... values) {
        for (PoseAspect value : values) {
            if (aspects.contains(value)) {
                // TODO match single aspect value for each aspect class
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(Set<Interest> values) {
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