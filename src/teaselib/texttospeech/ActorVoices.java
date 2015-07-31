package teaselib.texttospeech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import teaselib.TeaseLib;
import teaselib.core.ResourceLoader;

public class ActorVoices extends VoiceProperties {

    public final static String VoicesFilename = "Actor Voices.properties";

    public ActorVoices(ResourceLoader resources) {
        InputStream recordedVoicesConfig = null;
        String path = VoicesFilename;
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
            TeaseLib.log("No actor voices configuration found in '"
                    + resources.getAssetPath(path) + "' - using defaults");
        }
    }

    public void store(File path) throws IOException {
        store(path, VoicesFilename);
    }
}
