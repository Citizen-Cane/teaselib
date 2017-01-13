package teaselib.core.texttospeech;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.Mood;
import teaselib.core.ResourceLoader;
import teaselib.util.TextVariables;

public class TextToSpeechRecorder {
    private static final Logger logger = LoggerFactory
            .getLogger(TextToSpeechRecorder.class);

    public final static String MessageFilename = "message.txt";
    public final static String ResourcesFilename = "inventory.txt";
    private static final String SpeechResourceFileTypeExtension = ".mp3";

    private final ResourceLoader resources;
    private final PrerecordedSpeechStorage storage;
    private final Map<String, Voice> voices;
    private final Set<String> actors = new HashSet<String>();
    private final ActorVoices actorVoices;
    private final TextToSpeechPlayer ttsPlayer;
    private final mp3.Main mp3Encoder;
    private final long buildStart = System.currentTimeMillis();
    private final TextVariables textVariables;

    int newEntries = 0;
    int changedEntries = 0;
    int upToDateEntries = 0;
    int reusedDuplicates = 0;

    int numberOfPasses = 0;

    public TextToSpeechRecorder(File path, String resourcesRoot,
            ResourceLoader resources, TextVariables textVariables)
            throws IOException {
        this.resources = resources;
        this.textVariables = textVariables;
        this.ttsPlayer = TextToSpeechPlayer.instance();
        this.voices = ttsPlayer.textToSpeech.getVoices();
        // storage = new TextToSpeechRecorderFileStorage(
        // resources.getAssetPath(""));
        storage = new PrerecordedSpeechZipStorage(path, resourcesRoot);
        actorVoices = new ActorVoices(resources);
        logger.info("Build start: " + new Date(buildStart).toString());
        ttsPlayer.loadActorVoices(resources);
        mp3Encoder = new mp3.Main();
    }

    public void preparePass(Entry<String, String> entry) {
        this.numberOfPasses++;
        logger.info("Pass " + numberOfPasses + " for symbol " + entry.getKey()
                + "=" + entry.getValue());
        logger.info("using text variables: '" + textVariables.toString() + "'");
    }

    public void create(ScriptScanner scanner) throws IOException {
        logger.info("Scanning script '" + scanner.getScriptName() + "'");
        Set<String> created = new HashSet<String>();
        for (Message message : scanner) {
            Actor actor = message.actor;
            Voice voice = getVoice(actor);
            logger.info("Voice: " + voice.name);
            if (!actors.contains(actor.key)) {
                createActorEntry(actor, voice);
            }
            String hash = recordMessage(actor, voice, message);
            created.add(hash);
        }
    }

    private String recordMessage(Actor actor, final Voice voice,
            Message message) throws IOException {
        String hash = getHash(message);
        if (storage.haveMessage(actor, voice, hash)) {
            long lastModified = storage.lastModified(actor, voice, hash);
            String oldMessageHash = storage.getMessageHash(actor, voice, hash);
            String messageHash = message.toPrerecordedSpeechHashString();
            if (messageHash.equals(oldMessageHash)) {
                if (lastModified > buildStart) {
                    logger.info(hash + " is reused");
                    reusedDuplicates++;
                } else {
                    logger.info(hash + " is up to date");
                    storage.keepMessage(actor, voice, hash);
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
            // scan -> collision
            // - if created before the current build, then change
        } else {
            logger.info(hash + " is new");
            create(actor, voice, message, hash,
                    message.toPrerecordedSpeechHashString());
            newEntries++;
        }
        return hash;
    }

    private void createActorEntry(Actor actor, final Voice voice)
            throws IOException {
        // Create a tag file containing the actor voice properties,
        // for information and because
        // the resource loader can just load files, but not check for
        // directories in the resource paths
        actors.add(actor.key);
        PreRecordedVoice info = new PreRecordedVoice(actor, voice);
        storage.createActorEntry(actor, voice, info);
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

    public void finish() throws IOException {
        storage.close();

        if (numberOfPasses > 1) {
            logger.info(
                    "Generating speech files for all symbols generates a lot of reused entries");
        }

        logger.info("Finished: " + upToDateEntries + " up to date, "
                + reusedDuplicates + " reused, " + changedEntries + " changed, "
                + newEntries + " new");
    }

    static String readMessage(InputStream inputStream) throws IOException {
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
        storage.createNewEntry(actor, voice, hash, messageHash);
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
        StringBuilder inventory = new StringBuilder();
        for (String soundFile : soundFiles) {
            inventory.append(soundFile);
            inventory.append("\n");
        }
        storage.writeStringResource(actor, voice, hash,
                TextToSpeechRecorder.ResourcesFilename, inventory.toString());
    }

    private void writeMessageHash(Actor actor, Voice voice, String hash,
            String messageHash) throws IOException {
        storage.writeStringResource(actor, voice, hash,
                TextToSpeechRecorder.MessageFilename, messageHash);
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
