package teaselib.core.texttospeech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.LoggerFactory;

import teaselib.core.ResourceLoader;

public class ActorVoices extends VoiceProperties {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ActorVoices.class);

    public final static String VoicesFilename = "Actor Voices.properties";

    public ActorVoices(ResourceLoader resources) {
        try {
            try (InputStream recordedVoicesConfig = resources.getResource(VoicesFilename);) {
                properties.load(recordedVoicesConfig);
            }
        } catch (IOException e) {
            logger.info("No actor voices configuration found in '" + VoicesFilename + "' - using defaults");
        }
    }

    public void store(File path) throws IOException {
        store(path, VoicesFilename);
    }
}
