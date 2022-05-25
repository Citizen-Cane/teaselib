package teaselib.util;

public interface Inventory {

    boolean noneAvailable();

    /**
     * Determine whether the query result would contain at least one available item.
     * <p>
     * 
     * @return Whether all items are applied.
     */
    boolean anyAvailable();

    /**
     * Determine whether the query result would contain at least one available item of each element of its value set.
     * <p>
     * This does not necessarily mean that all items are available, but the value set of
     * {@link Items.Query#getAvailable} would be equal to this item query' {@link Items#valueSet}.
     * 
     * @return Whether all items are applied.
     */
    boolean allAvailable();

    boolean noneApplicable();

    /**
     * Determine whether the query result would contain at least one applicable item.
     * <p>
     * 
     * @return Whether all items are applied.
     */
    boolean anyApplicable();

    /**
     * Determine whether the query result would contain at least one applicable item of each element of its value set.
     * <p>
     * This does not necessarily mean that all items are applicable, but the value set of
     * {@link Items.Query#getApplicable} would be equal to this item query' {@link Items#valueSet}.
     * 
     * @return Whether all items are applied.
     */
    boolean allApplicable();

    boolean noneApplied();

    /**
     * Determine whether the query result would contain at least one applied item.
     * <p>
     * 
     * @return Whether all items are applied.
     */
    boolean anyApplied();

    /**
     * Determine whether the query result would contain at least one applied item of each element of its value set.
     * <p>
     * This does not necessarily mean that all items are applied, but the value set of {@link Items.Query#getApplied}
     * would be equal to this item query' {@link Items#valueSet}.
     * 
     * @return Whether all items are applied.
     */
    boolean allApplied();
}