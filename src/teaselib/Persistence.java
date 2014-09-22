package teaselib;

public interface Persistence {

	String read(String name);
	void write(String name, String value);

	/**
	 * Create a mapping from a script internal to a host external name
	 * @param internal The name used internally by the script
	 * @param external The name of the setting as stored by the implementing class 
	 */
	void setMapping(String internal, String external);
}
