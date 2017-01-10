package teaselib.core.texttospeech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ResourceLoader;

public class PreRecordedVoice extends VoiceProperties {
    private static final Logger logger = LoggerFactory
            .getLogger(PreRecordedVoice.class);

    public final static String ActorPropertiesFilename = "voice.properties";

    public PreRecordedVoice(String actorName, String voiceGuid,
            ResourceLoader resources) {
        InputStream prerecordedVoiceConfig = null;
        String actorProperties = TextToSpeechRecorderFileStorage.SpeechDirName
                + "/" + actorName + "/" + voiceGuid + "/"
                + ActorPropertiesFilename;
        try {
            try {
                prerecordedVoiceConfig = resources.getResource(actorProperties);
                properties.load(prerecordedVoiceConfig);
            } finally {
                if (prerecordedVoiceConfig != null) {
                    prerecordedVoiceConfig.close();
                }
            }
        } catch (IOException e) {
            // This is expected, as the script may not have pre-recorded voices
            logger.debug("Prerecorded voice configuration file '"
                    + actorProperties + "' not found");
        }
    }

    public boolean available() {
        return !empty();
    }

    public void store(File path) throws IOException {
        store(path, ActorPropertiesFilename);
    }
}
