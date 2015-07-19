package teaselib.texttospeech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import teaselib.ResourceLoader;
import teaselib.TeaseLib;

public class ActorVoice extends VoiceProperties {
    public final static String ActorPropertiesFilename = "actor.properties";

    public ActorVoice(String actorName, String voiceGuid,
            ResourceLoader resources) {
        InputStream recordedVoicesConfig = null;
        String path = TextToSpeechRecorder.SpeechDirName + "/" + actorName
                + "/" + voiceGuid + "/" + ActorPropertiesFilename;
        try {
            try {
                recordedVoicesConfig = resources.getResource(path);
                properties.load(recordedVoicesConfig);
            } finally {
                if (recordedVoicesConfig != null) {
                    recordedVoicesConfig.close();
                }
            }
        } catch (IOException e) {
            TeaseLib.log(this, e);
        }
    }

    public void store(File path) throws IOException {
        store(path, ActorPropertiesFilename);
    }
}
