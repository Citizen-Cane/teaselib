package teaselib.core.texttospeech;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import teaselib.Actor;
import teaselib.core.util.Stream;

public class PrerecordedSpeechZipStorage implements PrerecordedSpeechStorage {
    // private static final Logger logger = LoggerFactory
    // .getLogger(PrerecordedSpeechZipStorage.class);
    private static final String Zip = ".zip";
    private static final String Updated = " updated";

    private final ZipFile current;
    private final ZipOutputStream updated;

    private final Map<String, Long> processed = new HashMap<String, Long>();
    private final Map<String, String> messageHashes = new HashMap<String, String>();
    private File zipFileUpdated;
    private static File zipFileCurrent;

    public PrerecordedSpeechZipStorage(File assetsDir) throws IOException {
        zipFileCurrent = new File(assetsDir, SpeechDirName + Zip)
                .getAbsoluteFile();
        zipFileUpdated = new File(assetsDir, SpeechDirName + Updated + Zip)
                .getAbsoluteFile();
        current = getCurrent();
        updated = new ZipOutputStream(new FileOutputStream(zipFileUpdated));
    }

    private static ZipFile getCurrent() throws ZipException, IOException {
        try {
            return new ZipFile(zipFileCurrent);
        } catch (FileNotFoundException e) {
            FileOutputStream fos = new FileOutputStream(zipFileCurrent);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zos.flush();
            zos.close();
            fos.close();
            return new ZipFile(zipFileCurrent);
        }
    }

    @Override
    public void createActorEntry(Actor actor, Voice voice,
            VoiceProperties properties) throws IOException {
        String voicePath = PreRecordedVoice.getResourcePath(actor, voice);
        ZipEntry actorEntry = new ZipEntry(voicePath);
        updated.putNextEntry(actorEntry);
        properties.store(updated, "");
        updated.closeEntry();
    }

    private static String getPath(Actor actor, Voice voice, String hash,
            String name) {
        return SpeechDirName + "/" + actor.key + "/" + voice.guid + "/" + hash
                + "/" + name;
    }

    @Override
    public boolean haveMessage(Actor actor, Voice voice, String hash) {
        if (processed.containsKey(processHash(actor, voice, hash))) {
            return true;
        }
        ZipEntry entry = current.getEntry(getPath(actor, voice, hash,
                TextToSpeechRecorder.ResourcesFilename));
        return entry != null;
    }

    private static String processHash(Actor actor, Voice voice, String hash) {
        return getPath(actor, voice, hash, "");
    }

    @Override
    public long lastModified(Actor actor, Voice voice, String hash) {
        Long lastModified = processed.get(processHash(actor, voice, hash));
        if (lastModified != null) {
            return lastModified;
        }
        ZipEntry entry = current.getEntry(getPath(actor, voice, hash,
                TextToSpeechRecorder.ResourcesFilename));
        return entry.getTime();
    }

    @Override
    public String getMessageHash(Actor actor, Voice voice, String hash)
            throws IOException {
        String messageHash = messageHashes.get(processHash(actor, voice, hash));
        if (messageHash == null) {
            ZipEntry entry = current.getEntry(getPath(actor, voice, hash,
                    TextToSpeechRecorder.MessageFilename));
            InputStream inputStream = current.getInputStream(entry);
            try {
                messageHash = TextToSpeechRecorder.readMessage(inputStream);
                messageHashes.put(processHash(actor, voice, hash), messageHash);
            } finally {
                inputStream.close();
            }
        }
        return messageHash;
    }

    @Override
    public void deleteMessage(Actor actor, Voice voice, String hash) {
        String key = processHash(actor, voice, hash);
        processed.remove(key);
        messageHashes.remove(key);
    }

    @Override
    public void storeSpeechResource(Actor actor, Voice voice, String hash,
            InputStream inputStream, String name) throws IOException {
        ZipEntry resourceEntry = new ZipEntry(
                getPath(actor, voice, hash, name));
        updated.putNextEntry(resourceEntry);
        Stream.copy(inputStream, updated);
        updated.flush();
        updated.closeEntry();
    }

    @Override
    public void createNewEntry(Actor actor, Voice voice, String hash,
            String messageHash) {
        String key = processHash(actor, voice, hash);
        processed.put(key, System.currentTimeMillis());
        messageHashes.put(key, messageHash);
    }

    @Override
    public void keepMessage(Actor actor, Voice voice, String hash)
            throws IOException {
        String messageHash = getStringResource(actor, voice, hash,
                TextToSpeechRecorder.MessageFilename);
        messageHashes.put(processHash(actor, voice, hash), messageHash);
        writeStringResource(actor, voice, hash,
                TextToSpeechRecorder.MessageFilename, messageHash);

        String inventory = getStringResource(actor, voice, hash,
                TextToSpeechRecorder.ResourcesFilename);
        writeStringResource(actor, voice, hash,
                TextToSpeechRecorder.ResourcesFilename, inventory);

        for (String speechResource : inventory.split("\n")) {
            copyEntry(actor, voice, hash, speechResource);
        }

        long lastModified = System.currentTimeMillis();
        processed.put(processHash(actor, voice, hash), lastModified);
    }

    private void copyEntry(Actor actor, Voice voice, String hash, String name)
            throws IOException {
        ZipEntry entry = current.getEntry(getPath(actor, voice, hash, name));
        InputStream inputStream = current.getInputStream(entry);
        storeSpeechResource(actor, voice, hash, inputStream, name);
        inputStream.close();
        updated.closeEntry();
    }

    private String getStringResource(Actor actor, Voice voice, String hash,
            String name) throws IOException {
        ZipEntry entry = current.getEntry(getPath(actor, voice, hash, name));
        InputStream inputStream = current.getInputStream(entry);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Stream.copy(inputStream, bos);
        inputStream.close();
        bos.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private void writeStringResource(Actor actor, Voice voice, String hash,
            String name, String value) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(
                value.toString().getBytes(StandardCharsets.UTF_8));
        storeSpeechResource(actor, voice, hash, inputStream, name);
    }

    @Override
    public void close() throws IOException {
        current.close();
        updated.flush();
        updated.close();
        zipFileCurrent.delete();
        zipFileUpdated.renameTo(zipFileCurrent);
    }

}
