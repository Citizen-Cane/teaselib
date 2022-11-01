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
            throw new UnsupportedOperationException("Item.NotFound: only available items can be applied");
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
            return apply();
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
            throw new IllegalStateException("Item.NotFound: only available and applied items can be removed");
        }

        @Override
        public void removeFrom(Object... peers) {
            remove();
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

    /**
     * Apply the item and its attributes to its default peers.
     * 
     * @return Duration-{@link Options} for this item.
     */
    @Override
    Options apply();

    /**
     * Test whether the item is applied, either as a singleton item (no default peers using {@link Item#apply}), or to
     * at least one of its default peers (using {@link Item#applyTo}.
     */
    @Override
    boolean applied();

    /**
     * Determine whether the item can be applied, e.g. a call to apply would succeed and change the configuration of the
     * item.
     * <p>
     * Items are applicable until all of their default peers are applied. Either by the item itself, or by other items.
     * Items can be partially applied without all or some of their default peers using {@link Item#applyTo}.
     *
     * @return True if the item can be applied, or the application of the item can be completed by calling
     *         {@link Item#apply}.
     */
    boolean canApply();

    /**
     * Apply the item and its attributes to custom peers.
     * 
     * @return Duration-{@link Options} for this item.
     */
    @Override
    Options applyTo(Object... peers);

    boolean isAvailable();

    void setAvailable(boolean isAvailable);

    String displayName();

    /**
     * Produce a similar item with additional default peers. The similar item will extend the original item with
     * additional default peers, e.g. when it's applied, the original item will be applied to its original default
     * peers, plus the additional peers.
     * 
     * @param additionalPeers
     *            Additional default peers for the similar items.
     * @return An extension of the original item, with additional default peers to be applied. The similar item is the
     *         same entity as the original items, e.g. when applied, the original item will also change its state to
     *         applied.
     */
    Item to(Object... additionalPeers);
}