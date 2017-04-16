package teaselib.util;

public interface Item {

    public static final Item NotAvailable = new Item() {

        @Override
        public <S> boolean is(S... attributes) {
            return false;
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
}