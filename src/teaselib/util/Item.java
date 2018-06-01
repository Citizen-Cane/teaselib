package teaselib.util;

import teaselib.Duration;
import teaselib.State;

public interface Item extends State {

    /**
     * Tag interface to distinguish peers and attributes
     * 
     * @author Citizen-Cane
     *
     */
    interface Attribute {
    }

    public static final Item NotFound = new Item() {
        @Override
        public boolean is(Object... attributes) {
            return false;
        }

        @Override
        public State.Options apply() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canApply() {
            return false;
        }

        @Override
        @SafeVarargs
        public final <S extends Object> State.Options applyTo(S... items) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean applied() {
            return false;
        }

        @Override
        public boolean expired() {
            return true;
        }

        @Override
        public Duration duration() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Persistence remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        @SafeVarargs
        public final <S extends Object> Persistence removeFrom(S... peer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void setAvailable(boolean isAvailable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String displayName() {
            return "Not found";
        }

        @Override
        public String toString() {
            return displayName();
        }
    };

    boolean isAvailable();

    void setAvailable(boolean isAvailable);

    String displayName();

    // TODO change meaning of canApply() to available & applicable - that's checked in the code very often
    // - would be closer to the meaning of "can"since unavailable stuff can't be applied anyway
    // - and canApply although not available can be checked by checking whether all default peers are free
    boolean canApply();
}