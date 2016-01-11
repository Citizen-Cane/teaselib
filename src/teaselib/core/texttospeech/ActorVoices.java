package teaselib.core.texttospeech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import teaselib.TeaseLib;
import teaselib.core.ResourceLoader;

public class ActorVoices extends VoiceProperties {

    public final static String VoicesFilename = "Actor Voices.properties";

    public ActorVoices(ResourceLoader resources) {
        String path = VoicesFilename;
        try {
            InputStream recordedVoicesConfig = null;
            try {
                recordedVoicesConfig = resources.getResource(path);
                properties.load(recordedVoicesConfig);
            } finally {
                if (recordedVoicesConfig != null) {
                    recordedVoicesConfig.close();
                }
            }
        } catch (IOException e) {
            TeaseLib.instance().log
                    .info("No actor voices configuration found in '"
                            + resources.getAssetPath(path)
                            + "' - using defaults");
        }
    }

    public void store(File path) throws IOException {
        store(path, VoicesFilename);
    }
}
