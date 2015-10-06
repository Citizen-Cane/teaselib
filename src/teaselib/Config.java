/**
 * 
 */
package teaselib;

/**
 * This class lists up all configuration properties.
 * 
 * The default value is always {@code false}, and chosen to maximize the user
 * experience.
 * <P>
 * Values are written via the TeaseLib {@code getXXX(...)/set(...)} methods, as a result the TeaseLib settings are maintained by the host.
 * <P>
 * Examples:
 * <P>
 * {@code boolean ignoreMissing = teaseLib.getBoolean(Config.Namespace, Config.Debug.IgnoreMissingResources);}
 * <P>
 * {@code teaseLib.set(Config.Namespace, Config.Debug.IgnoreMissingResources, ignoreMissing);}
 */
public enum Config {
    ;
    /**
     * The namespace for all TeaseLib configuration properties.
     */
    public static final String Namespace = "TeaseLib.Config";

    public enum Debug {
        /**
         * TeaseLib.Config.Debug.IgnoreMissingResources=false
         */
        IgnoreMissingResources,

        /**
         * TeaseLib.Config.Debug.LogDetails=false
         */
        LogDetails;
    }
}
