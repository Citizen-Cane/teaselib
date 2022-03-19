package teaselib.core.ai.perception;

import teaselib.Actor;
import teaselib.core.events.EventArgs;

public class PoseEstimationEventArgs extends EventArgs {

    public final Actor actor;
    public final PoseAspects pose;

    public PoseEstimationEventArgs(Actor actor, PoseAspects pose) {
        this.actor = actor;
        this.pose = pose;
    }

    @Override
    public String toString() {
        return "[" + actor.key + ", " + pose + "]";
    }

}
