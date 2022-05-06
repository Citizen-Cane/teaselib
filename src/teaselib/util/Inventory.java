package teaselib.util;

public interface Inventory {

    boolean noneAvailable();

    boolean anyAvailable();

    /**
     * Determine whether the query result would contain at least one available item of each element of its value set.
     * <p>
     * This does not necessarily mean that all items in the query are available, but the value set of
     * {@link Items.Query#getAvailable} would be equal to this item query' {@link Items#valueSet}.
     * 
     * @return Whether all items are applied.
     */
    boolean allAvailable();

    boolean noneApplicable();

    boolean anyApplicable();

    /**
     * Determine whether the query result would contain at least one applicable item of each element of its value set.
     * <p>
     * This does not necessarily mean that all items in the query are available, but the value set of
     * {@link Items.Query#getApplicable} would be equal to this item query' {@link Items#valueSet}.
     * 
     * @return Whether all items are applied.
     */
    boolean allApplicable();

    boolean noneApplied();

    boolean anyApplied();

    /**
     * Determine whether the query result would contain at least one applied item of each element of its value set.
     * <p>
     * This does not necessarily mean that all items in the query are applied, but the value set of
     * {@link Items.Query#getApplied} would be equal to this item query' {@link Items#valueSet}.
     * 
     * @return Whether all items are applied.
     */
    boolean allApplied();
}