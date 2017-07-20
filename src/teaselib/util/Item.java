package teaselib.util;

import teaselib.Duration;
import teaselib.State;

public interface Item {

    public static final Item NotAvailable = new Item() {

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
        public <S extends Object> State.Options to(S... items) {
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
        public State remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void setAvailable(boolean isAvailable) {
            // ignore
        }

        @Override
        public String displayName() {
            return "Not available";
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

    State.Options apply();

    // TODO rename to applyTo()
    <S extends Object> State.Options to(S... items);

    boolean is(Object... attributes);

    boolean applied();

    boolean expired();

    Duration duration();

    State remove();
}