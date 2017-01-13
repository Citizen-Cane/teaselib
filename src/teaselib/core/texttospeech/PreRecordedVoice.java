package teaselib.core.texttospeech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;

public class PreRecordedVoice extends VoiceProperties {
    private static final Logger logger = LoggerFactory
            .getLogger(PreRecordedVoice.class);

    public final static String ActorPropertiesFilename = "voice.properties";

    public PreRecordedVoice(Actor actor, Voice voice) {
        put(actor.key, voice);
    }

    public PreRecordedVoice(InputStream inputStream) throws IOException {
        try {
            properties.load(inputStream);
        } finally {
            inputStream.close();
        }
    }

    public static String getResourcePath(Actor actor, Voice voice) {
        return getResourcePath(actor.key, voice.guid);
    }

    public static String getResourcePath(String actorKey, String voiceGuid) {
        return PrerecordedSpeechStorage.SpeechDirName + "/" + actorKey + "/"
                + voiceGuid + "/" + ActorPropertiesFilename;
    }

    public boolean available() {
        return !empty();
    }

    public void store(File path) throws IOException {
        store(path, ActorPropertiesFilename);
    }
}
