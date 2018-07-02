package teaselib.hosts;

import teaselib.core.AbstractUserItems;
import teaselib.core.TeaseLib;
import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;

/**
 * @author Citizen-Cane
 *
 *         A set of pre-defined items that models almost the toys available in SexScripts. This means most of the toys
 *         are mapped to their SexScripts-equivalent.
 *         <li>Items are a fixed set, e.g. they're not enumerated from host settings.
 *         <li>All toys specialization enumerations are created and mapped. As a result, these items can also be made
 *         available by a script.
 */
public class PreDefinedItems extends AbstractUserItems {
    public PreDefinedItems(TeaseLib teaseLib) {
        super(teaseLib);
    }

    // TODO Create version for SexScripts with exact inventory match and exact save names for Mine Demo
    // TODO Create TeaseLib version with extended inventory and item guids for custom item creation

    @Override
    protected Item[] createDefaultItems(String domain, QualifiedItem item) {

        // TODO hairbrush and wooden spoon are listed as Toys.Spanking_Implement although they're of class Household
        // TODO Decide about posture collar (from CM), dog collar, maid collar etc. -> fetish style
        // TODO Add nipple clamp type (clover, etc.)

        return getDefaultItem(domain, item);
    }

    protected Enum<?>[] defaults(QualifiedItem item, Enum<?>... more) {
        return array(defaults(item), more);
    }
}
