package teaselib.util.jni;

import java.io.File;

public class LibraryLoader {

	public static void load(String name) {
		File folder = new File("lib");
		String extension;
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows")) {
			extension = "dll";
		}
		else
		{
			throw new UnsupportedOperationException("LoadLibrary() " + os + " not supported yet");
		}
		final String architecture;
		String arch = System.getProperty("os.arch");
		if (arch.equalsIgnoreCase("x86"))
		{
			architecture = "x86";
		}
		else if (arch.equalsIgnoreCase("amd64"))
		{
			architecture = "x64";
		}
		else
		{
			throw new UnsupportedOperationException("LoadLibrary() processor architecture " + arch + " not supported yet");
		}
		File file = new File(folder, name + "_" + architecture + "."
				+ extension);
		System.load(file.getAbsolutePath());
	}
}
