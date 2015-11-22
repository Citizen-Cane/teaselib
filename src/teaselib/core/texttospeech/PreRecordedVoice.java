package teaselib.core.texttospeech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import teaselib.TeaseLib;
import teaselib.core.ResourceLoader;

public class PreRecordedVoice extends VoiceProperties {
    public final static String ActorPropertiesFilename = "voice.properties";

    public PreRecordedVoice(String actorName, String voiceGuid,
            ResourceLoader resources) {
        InputStream prerecordedVoiceConfig = null;
        String path = TextToSpeechRecorder.SpeechDirName + "/" + actorName
                + "/" + voiceGuid + "/" + ActorPropertiesFilename;
        try {
            try {
                prerecordedVoiceConfig = resources.getResource(path);
                properties.load(prerecordedVoiceConfig);
            } finally {
                if (prerecordedVoiceConfig != null) {
                    prerecordedVoiceConfig.close();
                }
            }
        } catch (IOException e) {
            // This is expected, as the script may not have pre-recorded voices
            TeaseLib.instance().log
                    .debug("Prerecorded voice configuration file '" + path
                            + "' not found");
        }
    }

    public boolean available() {
        return !empty();
    }

    public void store(File path) throws IOException {
        store(path, ActorPropertiesFilename);
    }
}
