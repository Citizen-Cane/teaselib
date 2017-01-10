package teaselib.core.texttospeech;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.util.Stream;

public class TextToSpeechRecorderFileStorage {
    private static final Logger logger = LoggerFactory
            .getLogger(TextToSpeechRecorderFileStorage.class);

    public final static String SpeechDirName = "speech";

    private final File assetsDir;
    private final File speechDir;

    public TextToSpeechRecorderFileStorage(File assetsDir) {
        this.assetsDir = assetsDir;
        this.speechDir = createSubDir(assetsDir, SpeechDirName);
    }

    public void createActorFile(Actor actor, Voice voice) {
        File characterDir = createSubDir(speechDir, actor.key);
        File voiceDir = createSubDir(characterDir, voice.guid);
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

    public OutputStream getActorVoiceStream(Actor actor, Voice voice)
            throws IOException {
        File file = new File(getVoiceDir(actor, voice),
                PreRecordedVoice.ActorPropertiesFilename);
        return new FileOutputStream(file);
    }

    private File getVoiceDir(Actor actor, Voice voice) {
        return new File(new File(speechDir, actor.key), voice.guid);
    }

    public boolean haveMessage(Actor actor, Voice voice, String hash) {
        File messageDir = getMessageDir(actor, voice, hash);
        File messageFile = new File(messageDir,
                TextToSpeechRecorder.MessageFilename);
        return messageDir.exists() && messageFile.exists();
    }

    private File getMessageDir(Actor actor, Voice voice, String hash) {
        return new File(getVoiceDir(actor, voice), hash);
    }

    public long lastModified(Actor actor, Voice voice, String hash) {
        return getMessageDir(actor, voice, hash).lastModified();
    }

    public InputStream getMessageStream(Actor actor, Voice voice, String hash)
            throws IOException {
        return new FileInputStream(new File(getMessageDir(actor, voice, hash),
                TextToSpeechRecorder.MessageFilename));
    }

    public void deleteMessage(Actor actor, Voice voice, String hash) {
        File messageDir = getMessageDir(actor, voice, hash);
        for (File file : messageDir.listFiles()) {
            file.delete();
        }
        messageDir.delete();
    }

    public void storeSpeechResource(Actor actor, Voice voice, String hash,
            InputStream inputStream, String name) throws IOException {
        FileOutputStream os = new FileOutputStream(
                new File(getMessageDir(actor, voice, hash), name));
        Stream.copy(inputStream, os);
        os.close();
    }

    public OutputStream getInventoryOutputStream(Actor actor, Voice voice,
            String messageHash) throws IOException {
        File inventory = new File(getMessageDir(actor, voice, messageHash),
                TextToSpeechRecorder.ResourcesFilename);
        return new FileOutputStream(inventory);
    }

    public OutputStream getMessageOutputStream(Actor actor, Voice voice,
            String messageHash) throws IOException {
        File message = new File(getMessageDir(actor, voice, messageHash),
                TextToSpeechRecorder.MessageFilename);
        return new FileOutputStream(message);
    }

    public void createNewEntry(Actor actor, Voice voice, String hash) {
        getMessageDir(actor, voice, hash).mkdir();
    }
}
