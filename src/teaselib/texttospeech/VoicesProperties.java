package teaselib.texttospeech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import teaselib.ResourceLoader;
import teaselib.TeaseLib;

public class VoicesProperties extends VoiceProperties {

    public final static String VoicesFilename = "voices.properties";

    public VoicesProperties(ResourceLoader resources) {
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
        } catch (IOException e1) {
            TeaseLib.log("No assigned voices config: " + path);
        }
    }

    public void store(File path) throws IOException {
        store(path, VoicesFilename);
    }
}
