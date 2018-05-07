package teaselib.core.texttospeech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.LoggerFactory;

import teaselib.core.ResourceLoader;

public class ActorVoices extends VoiceProperties {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ActorVoices.class);

    public static final String VoicesFilename = "Actor Voices.properties";

    public ActorVoices(ResourceLoader resources) {
        try {
            try (InputStream recordedVoicesConfig = resources.get(VoicesFilename);) {
                properties.load(recordedVoicesConfig);
            }
        } catch (IOException e) {
            logger.info("No actor voices configuration found in '{}' - using defaults", VoicesFilename);
        }
    }

    public void store(File path) throws IOException {
        store(path, VoicesFilename);
    }
}
