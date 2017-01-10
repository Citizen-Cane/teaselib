package teaselib.core.texttospeech;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.Mood;
import teaselib.core.ResourceLoader;

public class TextToSpeechRecorder {
    private static final Logger logger = LoggerFactory
            .getLogger(TextToSpeechRecorder.class);

    public final static String MessageFilename = "message.txt";
    public final static String ResourcesFilename = "inventory.txt";
    private static final String SpeechResourceFileTypeExtension = ".mp3";

    private final ResourceLoader resources;
    private final TextToSpeechRecorderFileStorage storage;
    private final Map<String, Voice> voices;
    private final Set<String> actors = new HashSet<String>();
    private final ActorVoices actorVoices;
    private final TextToSpeechPlayer ttsPlayer;
    private final mp3.Main mp3Encoder;
    private final long buildStart = System.currentTimeMillis();

    int newEntries = 0;
    int changedEntries = 0;
    int upToDateEntries = 0;
    int reusedDuplicates = 0;

    public TextToSpeechRecorder(ResourceLoader resources) {
        this.resources = resources;
        this.ttsPlayer = TextToSpeechPlayer.instance();
        this.voices = ttsPlayer.textToSpeech.getVoices();
        storage = new TextToSpeechRecorderFileStorage(
                resources.getAssetPath(""));
        actorVoices = new ActorVoices(resources);
        logger.info("Build start: " + new Date(buildStart).toString());
        ttsPlayer.loadActorVoices(resources);
        mp3Encoder = new mp3.Main();
    }

    public void create(ScriptScanner scanner) throws IOException {
        logger.info("Scanning script '" + scanner.getScriptName() + "'");
        Set<String> created = new HashSet<String>();
        for (Message message : scanner) {
            Actor actor = message.actor;
            // Process voices for each character
            final Voice voice;
            voice = getVoice(actor);
            logger.info("Voice: " + voice.name);
            ttsPlayer.textToSpeech.setVoice(voice);

            storage.createActorFile(actor, voice);
            if (!actors.contains(actor.key)) {
                createActorFile(actor, voice);
            }
            String hash = recordMessage(actor, voice, message);
            // Unused directories will remain because an update changes the
            // checksum of the message
            // TODO clean up by checking unused
            created.add(hash);
        }
    }

    private String recordMessage(Actor actor, final Voice voice,
            Message message) throws IOException {
        String hash = getHash(message);
        if (storage.haveMessage(actor, voice, hash)) {
            long lastModified = storage.lastModified(actor, voice, hash);
            String oldMessageHash = readMessage(
                    storage.getMessageStream(actor, voice, hash));
            String messageHash = message.toPrerecordedSpeechHashString();
            if (messageHash.equals(oldMessageHash)) {
                if (lastModified > buildStart) {
                    logger.info(hash + " is reused");
                    reusedDuplicates++;
                } else {
                    logger.info(hash + " is up to date");
                    upToDateEntries++;
                }
            } else if (lastModified > buildStart) {
                // Collision
                logger.info(hash + " collision!");
                logger.info("Old:");
                logger.info(oldMessageHash);
                logger.info("New:");
                logger.info(messageHash);
                throw new IllegalStateException("Collision");
            } else {
                logger.info(hash + " has changed");
                storage.deleteMessage(actor, voice, hash);
                create(actor, voice, message, hash, messageHash);
                changedEntries++;
            }
            // - check whether we have created this during the current
            // scan
            // -> collision
            // - if created before the current build, then change
        } else {
            logger.info(hash + " is new");
            create(actor, voice, message, hash,
                    message.toPrerecordedSpeechHashString());
            newEntries++;
        }
        return hash;
    }

    private void createActorFile(Actor actor, final Voice voice)
            throws IOException {
        // Create a tag file containing the actor voice properties,
        // for information and because
        // the resource loader can just load files, but not check for
        // directories in the resource paths
        actors.add(actor.key);
        PreRecordedVoice actorVoice = new PreRecordedVoice(actor.key,
                voice.guid, resources);
        actorVoice.clear();
        actorVoice.put(actor.key, voice);

        OutputStream actorVoiceStream = storage.getActorVoiceStream(actor,
                voice);
        try {
            actorVoice.store(actorVoiceStream);
        } finally {
            actorVoiceStream.close();
        }
        actorVoices.putGuid(actor.key, voice);

        if (!haveActorVoicesFile()) {
            actorVoices.store(resources.getAssetPath(""));
        }
    }

    private boolean haveActorVoicesFile() throws IOException {
        InputStream is = null;
        try {
            is = resources.getResource(ActorVoices.VoicesFilename);
            return is != null;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private Voice getVoice(Actor actor) {
        final Voice voice;
        String voiceGuid = ttsPlayer.getAssignedVoiceFor(actor);
        if (voiceGuid == null) {
            voice = ttsPlayer.getVoiceFor(actor);
        } else {
            voice = voices.get(voiceGuid);
        }
        if (voice == null) {
            throw new IllegalArgumentException("Voice for actor '" + actor
                    + "' not found in " + ActorVoices.VoicesFilename);
        }
        return voice;
    }

    public void finish() {
        logger.info("Finished: " + upToDateEntries + " up to date, "
                + reusedDuplicates + " reused duplicates, " + changedEntries
                + " changed, " + newEntries + " new");
    }

    private static String readMessage(InputStream inputStream)
            throws IOException {
        StringBuilder message = new StringBuilder();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream));
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                if (message.length() > 0) {
                    message.append("\n");
                }
                message.append(line);
            }
        } finally {
            reader.close();
            inputStream.close();
        }
        return message.toString();
    }

    public void create(Actor actor, Voice voice, Message message, String hash,
            String messageHash) throws IOException {
        List<String> soundFiles = writeSpeechResources(actor, voice, message,
                hash, messageHash);
        writeInventory(actor, voice, hash, soundFiles);
        writeMessageHash(actor, voice, hash, messageHash);
    }

    private List<String> writeSpeechResources(Actor actor, Voice voice,
            Message message, String hash, String messageHash)
            throws IOException, FileNotFoundException {
        logger.info("Recording message:\n" + messageHash);
        List<String> soundFiles = new Vector<String>();
        String mood = Mood.Neutral;
        storage.createNewEntry(actor, voice, hash);
        for (Part part : message.getParts()) {
            if (part.type == Message.Type.Mood) {
                mood = part.value;
            } else if (part.type == Message.Type.Text) {
                int index = soundFiles.size();
                logger.info("Recording part " + index);
                String soundFileName = writeSpeechResource(actor, voice, hash,
                        index, mood, part.value);
                soundFiles.add(soundFileName);
            }
        }
        return soundFiles;
    }

    private String writeSpeechResource(Actor actor, Voice voice, String hash,
            int index, String mood, String text)
            throws IOException, FileNotFoundException {
        String soundFileName = Integer.toString(index);
        File soundFile = createTempFile("Part_" + soundFileName + "_", "wav");
        TextToSpeech textToSpeech = ttsPlayer.textToSpeech;
        textToSpeech.setVoice(voice);
        textToSpeech.setHint(mood);
        String recordedSoundFile = textToSpeech.speak(text,
                soundFile.getAbsolutePath());
        if (!recordedSoundFile.endsWith(SpeechResourceFileTypeExtension)) {
            String encodedSoundFile = recordedSoundFile.replace(".wav",
                    SpeechResourceFileTypeExtension);
            // sampling frequency is read from the wav audio file
            String[] argv = { recordedSoundFile, encodedSoundFile, "--preset",
                    "standard" };
            mp3Encoder.run(argv);
            new File(recordedSoundFile).delete();
            recordedSoundFile = encodedSoundFile;
        }
        FileInputStream inputStream = new FileInputStream(recordedSoundFile);
        try {
            storage.storeSpeechResource(actor, voice, hash, inputStream,
                    soundFileName + SpeechResourceFileTypeExtension);
        } finally {
            inputStream.close();
        }
        return new File(soundFileName + SpeechResourceFileTypeExtension)
                .getName();
    }

    private void writeInventory(Actor actor, Voice voice, String hash,
            List<String> soundFiles) throws IOException {
        // Write sound inventory file
        OutputStreamWriter outputStreamWriter = null;
        OutputStream inventoryOutputStream = null;
        BufferedWriter bufferedWriter = null;
        try {
            inventoryOutputStream = storage.getInventoryOutputStream(actor,
                    voice, hash);
            outputStreamWriter = new OutputStreamWriter(inventoryOutputStream);
            bufferedWriter = new BufferedWriter(outputStreamWriter);
            for (String soundFile : soundFiles) {
                bufferedWriter.write(soundFile);
                bufferedWriter.newLine();
            }
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }
            if (inventoryOutputStream != null) {
                inventoryOutputStream.close();
            }
        }
    }

    private void writeMessageHash(Actor actor, Voice voice, String hash,
            String messageHash) throws IOException {
        // Write message file to indicate the folder is complete
        OutputStreamWriter outputStreamWriter = null;
        try {
            outputStreamWriter = new OutputStreamWriter(
                    storage.getMessageOutputStream(actor, voice, hash));
            outputStreamWriter.write(messageHash);
        } finally {
            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }
        }
    }

    private static File createTempFile(String prefix, String suffix)
            throws IOException {
        return File.createTempFile(prefix, suffix);
    }

    public static String getHash(Message message) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] string = null;
        try {
            string = message.toPrerecordedSpeechHashString().getBytes("UTF-16");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = digest.digest(string);
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
