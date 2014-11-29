package teaselib.texttospeech;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class AvailableVoicesProperties extends VoiceProperties {
	public final static String AvailableVoicesFilename = "installed.properties";

	public AvailableVoicesProperties(Map<String, Voice> voices) {
		// List available voices by guid and language
		for (String key : voices.keySet()) {
			put(key, voices.get(key));
		}
	}

	public void store(File path) throws IOException {
		store(path, AvailableVoicesFilename);
	}
}
