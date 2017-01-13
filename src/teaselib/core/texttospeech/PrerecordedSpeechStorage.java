package teaselib.core.texttospeech;

import java.io.IOException;
import java.io.InputStream;

import teaselib.Actor;

public interface PrerecordedSpeechStorage {

    String SpeechDirName = "speech";

    void createActorEntry(Actor actor, Voice voice, VoiceProperties properties)
            throws IOException;

    boolean haveMessage(Actor actor, Voice voice, String hash);

    long lastModified(Actor actor, Voice voice, String hash);

    // TODO MessageHash - name is wrong, it's a hashable message text
    String getMessageHash(Actor actor, Voice voice, String hash)
            throws IOException;

    void deleteMessage(Actor actor, Voice voice, String hash);

    void storeSpeechResource(Actor actor, Voice voice, String hash,
            InputStream inputStream, String name) throws IOException;

    void createNewEntry(Actor actor, Voice voice, String hash,
            String messageHash);

    void keepMessage(Actor actor, Voice voice, String hash) throws IOException;

    void close() throws IOException;

}