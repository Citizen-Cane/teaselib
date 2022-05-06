package teaselib.util;

import java.util.concurrent.TimeUnit;

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
        // Tag interface
    }

    public static final Item NotFound = new Item() {
        @Override
        public boolean is(Object... attributes) {
            return false;
        }

        @Override
        public State.Options apply() {
            throw new UnsupportedOperationException("Cannot apply when item not found");
        }

        @Override
        public boolean canApply() {
            return false;
        }

        @Override
        public Item to(Object... additionalPeers) {
            return this;
        }

        @Override
        public State.Options applyTo(Object... peers) {
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
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeFrom(Object... peers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removed() {
            return true;
        }

        @Override
        public long removed(TimeUnit unit) {
            return Long.MAX_VALUE;
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

    boolean canApply();

    Item to(Object... additionalPeers);
}