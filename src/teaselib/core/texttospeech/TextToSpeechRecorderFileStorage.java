package teaselib.core.texttospeech;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.util.Stream;

public class TextToSpeechRecorderFileStorage implements PrerecordedSpeechStorage {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechRecorderFileStorage.class);

    private final File speechDir;

    public TextToSpeechRecorderFileStorage(File assetsDir) {
        this.speechDir = createSubDir(assetsDir, SpeechDirName);
    }

    private void createActorDirectories(Actor actor, Voice voice) {
        File actorDir = createSubDir(speechDir, actor.key);
        createSubDir(actorDir, voice.guid());
    }

    private static File createSubDir(File dir, String name) {
        File subDir = new File(dir, name);
        if (subDir.exists() == false) {
            logger.info("Creating directory " + subDir.getAbsolutePath());
            subDir.mkdirs();
        } else {
            logger.info("Using directory " + subDir.getAbsolutePath());
        }
        return subDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.texttospeech.PrerecordedSpeechStorage#getActorVoiceStream( teaselib.Actor,
     * teaselib.core.texttospeech.Voice)
     */
    @Override
    public void createActorEntry(Actor actor, Voice voice, VoiceProperties properties) throws IOException {
        createActorDirectories(actor, voice);
        File file = new File(getVoiceDir(actor, voice), PreRecordedVoice.ActorPropertiesFilename);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            properties.store(fileOutputStream, "");
        } finally {
            fileOutputStream.close();
        }
    }

    private File getVoiceDir(Actor actor, Voice voice) {
        return new File(new File(speechDir, actor.key), voice.guid());
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.texttospeech.PrerecordedSpeechStorage#haveMessage(teaselib. Actor,
     * teaselib.core.texttospeech.Voice, java.lang.String)
     */
    @Override
    public boolean hasMessage(Actor actor, Voice voice, String hash) {
        File messageDir = getMessageDir(actor, voice, hash);
        File messageFile = new File(messageDir, TextToSpeechRecorder.MessageFilename);
        return messageDir.exists() && messageFile.exists();
    }

    private File getMessageDir(Actor actor, Voice voice, String hash) {
        return new File(getVoiceDir(actor, voice), hash);
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.texttospeech.PrerecordedSpeechStorage#lastModified(teaselib .Actor,
     * teaselib.core.texttospeech.Voice, java.lang.String)
     */
    @Override
    public long lastModified(Actor actor, Voice voice, String hash) {
        return getMessageDir(actor, voice, hash).lastModified();
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.texttospeech.PrerecordedSpeechStorage#getMessageStream( teaselib.Actor,
     * teaselib.core.texttospeech.Voice, java.lang.String)
     */
    @Override
    public String getMessageHash(Actor actor, Voice voice, String hash) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(
                new File(getMessageDir(actor, voice, hash), TextToSpeechRecorder.MessageFilename));
        String messageHash;
        try {
            messageHash = TextToSpeechRecorder.readMessage(fileInputStream);
        } finally {
            fileInputStream.close();
        }
        return messageHash;
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.texttospeech.PrerecordedSpeechStorage#deleteMessage( teaselib.Actor,
     * teaselib.core.texttospeech.Voice, java.lang.String)
     */
    @Override
    public void deleteMessage(Actor actor, Voice voice, String hash) {
        File messageDir = getMessageDir(actor, voice, hash);
        for (File file : messageDir.listFiles()) {
            file.delete();
        }
        messageDir.delete();
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.texttospeech.PrerecordedSpeechStorage#storeSpeechResource( teaselib.Actor,
     * teaselib.core.texttospeech.Voice, java.lang.String, java.io.InputStream, java.lang.String)
     */
    @Override
    public void storeSpeechResource(Actor actor, Voice voice, String hash, InputStream inputStream, String name)
            throws IOException {
        FileOutputStream os = new FileOutputStream(new File(getMessageDir(actor, voice, hash), name));
        Stream.copy(inputStream, os);
        os.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.texttospeech.PrerecordedSpeechStorage#createNewEntry( teaselib.Actor,
     * teaselib.core.texttospeech.Voice, java.lang.String)
     */
    @Override
    public void createNewEntry(Actor actor, Voice voice, String hash, String messageHash) {
        getMessageDir(actor, voice, hash).mkdir();
    }

    @Override
    public void keepMessage(Actor actor, Voice voice, String hash) {
        // nothing to do - already on the file system
    }

    @Override
    public void close() {
        // nothing to do - already on the file system

        // messages that have been changed in the scripts anymore
        // will not be deleted
        // because the old hash is not known
        // TODO build hash set of messages,
        // delete directories that are not present in the map
    }

    @Override
    public void writeStringResource(Actor actor, Voice voice, String hash, String name, String value)
            throws IOException {
        InputStream inputStream = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        try {
            storeSpeechResource(actor, voice, hash, inputStream, name);
        } finally {
            inputStream.close();
        }
    }
}
