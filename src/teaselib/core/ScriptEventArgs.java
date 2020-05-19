package teaselib.core;

import teaselib.Actor;
import teaselib.core.events.EventArgs;

public class ScriptEventArgs extends EventArgs {

    public static class ActorChanged extends ScriptEventArgs {
        public final Actor actor;

        public ActorChanged(Actor actor) {
            this.actor = actor;
        }
    }

}
