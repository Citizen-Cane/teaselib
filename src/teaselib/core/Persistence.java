package teaselib.core;

public interface Persistence {

    boolean has(String name);

    /**
     * @param name
     *            The name of the property
     * @return The value of the property or null if not found
     */
    String get(String name);

    void set(String name, String value);

    boolean getBoolean(String name);

    void set(String name, boolean value);

    void clear(String name);

    /**
     * Host-defined text variables. These are also auto-replaced in messages.
     * The format is #name (just as in the good old CyberMistress. If the
     * variable cannot be resolved, the default for that variable is used, which
     * gives us reasonable values if the script contains english language.
     */
    public enum TextVariable {
        Dom("Dom", null),
        Dominant("Dominant", null),

        /**
         * Short name of the default mistress
         */
        Miss("Miss", Dom),

        /**
         * Long name of the default mistress
         */
        Mistress("Mistress", Miss),

        /**
         * Long name of the default master
         */
        Master("Master", Dom),

        /**
         * Name of the slave
         */
        Slave("slave", null);

        public String value;
        public TextVariable fallback;

        TextVariable(String value, TextVariable fallback) {
            this.value = value;
            this.fallback = fallback;
        }
    }

    /**
     * Get a user defined value defined by the host, for instance #mistress or
     * #slave (CyberMistress used the '#' to tag variables, followed by an
     * meaningful name).
     * 
     * The scheme is kept so that teaselib can fallback to if the variable is
     * not defined by the host, teaselib can just return the variable name
     * 
     * @param name
     *            The name of the variable
     * @return The value of the variable or null
     */
    String get(TextVariable name, String locale);

    void set(TextVariable name, String locale, String value);
}
