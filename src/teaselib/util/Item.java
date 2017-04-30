package teaselib.util;

import teaselib.State;

public interface Item {

    public static final Item NotAvailable = new Item() {

        @Override
        public <S> boolean is(S... attributes) {
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
        public <S extends Enum<?>> State.Options to(S... items) {
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

    <S> boolean is(S... attributes);

    <S extends Enum<?>> State.Options to(S... items);

    State.Options apply();

    State remove();

    boolean canApply();
}