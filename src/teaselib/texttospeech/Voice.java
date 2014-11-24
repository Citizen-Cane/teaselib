package teaselib.texttospeech;

import teaselib.util.jni.NativeObject;

// refactor -> change class name in native code

/**
 * @author someone
 *
 */
/**
 * @author someone
 * 
 */
public class Voice extends NativeObject {
	/**
	 * A unique identifier for the voice. Only alphanumeric characters and dots are
	 * allowed. Avoid file system characters like '/', '\', ':'.
	 */
	public final String guid;
	public final String langID;
	public final String language;
	public final Gender gender;
	public final String name;
	public final String vendor;

	public enum Gender {
		Male, Female, Robot
	}

	public Voice(long nativeObject, String guid, String langID,
			String language, Gender gender, String name, String vendor) {
		super(nativeObject);
		this.guid = guid;
		this.gender = gender;
		this.langID = langID;
		this.language = language;
		this.name = name;
		this.vendor = vendor;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": guid=" + guid + " , gender= "
				+ gender + " , lang-id=" + langID + " , language=" + language
				+ " , name=" + name + " , vendor=" + vendor;
	}
}
