package teaselib.texttospeech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import teaselib.ResourceLoader;
import teaselib.TeaseLib;

public class VoicesProperties extends VoiceProperties {

	public final static String VoicesFilename = "voices.properties";

	public VoicesProperties(ResourceLoader resources) {
		InputStream recordedVoicesConfig;
		String path = VoicesFilename;
		try {
			recordedVoicesConfig = resources.getResource(path);
		} catch (IOException e1) {
			recordedVoicesConfig = null;
			TeaseLib.logDetail("No assigned voices config: " + path);
		}
		try {
			properties.load(recordedVoicesConfig);
		} catch (Exception e) {
			// No file or no voices defined, ok
		}
	}

	public void store(File path) throws IOException {
		store(path, VoicesFilename);
	}
}
