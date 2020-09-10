package teaselib.core;

import teaselib.Actor;
import teaselib.core.events.EventArgs;

public class ScriptEventArgs extends EventArgs {

    public static class ActorChanged extends ScriptEventArgs {
        public final Actor actor;

        public ActorChanged(Actor actor) {
            this.actor = actor;
        }

        @Override
        public String toString() {
            return actor.toString();
        }

    }

    public static class BeforeNewMessage extends ScriptEventArgs {
        public final OutlineType type;

        public enum OutlineType {
            NewSection,
            AppendParagraph,
            ReplaceParagraph
        }

        public BeforeNewMessage(OutlineType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "[" + super.toString() + ", " + type.name() + "]";
        }
    }

}
