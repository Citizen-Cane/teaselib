package teaselib.core.texttospeech;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.Mood;
import teaselib.core.Configuration;
import teaselib.core.ResourceLoader;
import teaselib.test.DebugSetup;
import teaselib.util.TextVariables;

// TODO error handling of asynchronous futures
public class TextToSpeechRecorder {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechRecorder.class);

    public static final String MessageFilename = "message.txt";
    public static final String ResourcesFilename = "inventory.txt";

    private static final String SpeechResourceTempFilePrefix = "TeaseLib_SpeechRecorder";

    private static final String SpeechResourceFileUncompressedFormat = ".wav";
    private static final String SpeechResourceFileTypeExtension = ".mp3";

    private final ResourceLoader resources;
    private final StorageSynchronizer storage;
    private final Map<String, Voice> voices;
    private final Set<String> actors = new HashSet<>();
    private final ActorVoices actorVoices;
    private final TextToSpeechPlayer ttsPlayer;
    private final long buildStart = System.currentTimeMillis();
    private final TextVariables textVariables;

    int newEntries = 0;
    int changedEntries = 0;
    int upToDateEntries = 0;
    int reusedDuplicates = 0;

    int numberOfPasses = 0;

    public TextToSpeechRecorder(File path, String resourcesRoot, ResourceLoader resources, TextVariables textVariables)
            throws IOException {
        this.resources = resources;
        this.textVariables = textVariables;
        this.ttsPlayer = new TextToSpeechPlayer(new Configuration(new DebugSetup()));
        this.ttsPlayer.load();
        this.voices = ttsPlayer.textToSpeech.getVoices();
        this.storage = new StorageSynchronizer(new PrerecordedSpeechZipStorage(path, resourcesRoot));
        this.actorVoices = new ActorVoices(resources);
        logger.info("Build start: " + new Date(buildStart).toString());
        ttsPlayer.loadActorVoiceProperties(resources);
    }

    public void preparePass(Entry<String, String> entry) {
        this.numberOfPasses++;
        logger.info("Pass " + numberOfPasses + " for symbol " + entry.getKey() + "=" + entry.getValue());
        logger.info("using text variables: '" + textVariables.toString() + "'");
    }

    public void create(ScriptScanner scanner) throws IOException, InterruptedException, ExecutionException {
        logger.info("Scanning script '" + scanner.getScriptName() + "'");
        Set<String> created = new HashSet<>();
        for (Message message : scanner) {
            Actor actor = message.actor;
            Voice voice = getVoice(actor);
            logger.info("Voice: " + voice.info().name);
            if (!actors.contains(actor.key)) {
                createActorEntry(actor, voice);
            }
            String hash = processMessage(actor, voice, message);
            created.add(hash);
        }
    }

    private String processMessage(Actor actor, Voice voice, Message message)
            throws InterruptedException, ExecutionException {
        String hash = getHash(message);
        String newMessageHash = message.toPrerecordedSpeechHashString();
        if (!newMessageHash.isEmpty()) {
            if (storage.hasMessage(actor, voice, hash)) {
                long lastModified = storage.lastModified(actor, voice, hash);
                String oldMessageHash = storage.getMessageHash(actor, voice, hash);
                if (oldMessageHash.equals(newMessageHash)) {
                    keepOrReuseMessage(actor, voice, hash, lastModified);
                } else if (lastModified > buildStart) {
                    handleCollision(hash, oldMessageHash, newMessageHash);
                } else {
                    updateMessage(actor, voice, message, hash, newMessageHash);
                }
            } else {
                createNewMessage(actor, voice, message, hash);
            }
        }
        return hash;
    }

    private void keepOrReuseMessage(Actor actor, final Voice voice, String hash, long lastModified) {
        if (lastModified > buildStart) {
            logger.info(hash + " is reused");
            reusedDuplicates++;
        } else {
            logger.info(hash + " is up to date as of " + new Date(lastModified));
            storage.keepMessage(actor, voice, hash);
            upToDateEntries++;
        }
    }

    private static void handleCollision(String hash, String oldMessageHash, String newMessageHash) {
        logger.info(hash + " collision!");
        logger.info("Old:");
        logger.info(oldMessageHash);
        logger.info("New:");
        logger.info(newMessageHash);
        throw new IllegalStateException("Collision");
    }

    private void updateMessage(Actor actor, Voice voice, Message message, String hash, String newMessageHash)
            throws InterruptedException {
        logger.info(hash + " has changed");
        storage.deleteMessage(actor, voice, hash);
        create(actor, voice, message, hash, newMessageHash);
        changedEntries++;
    }

    private void createNewMessage(Actor actor, Voice voice, Message message, String hash) throws InterruptedException {
        logger.info(hash + " is new");
        create(actor, voice, message, hash, message.toPrerecordedSpeechHashString());
        newEntries++;
    }

    private void createActorEntry(Actor actor, Voice voice) throws IOException {
        actors.add(actor.key);
        actorVoices.putGuid(actor.key, voice);

        if (!haveActorVoicesFile()) {
            actorVoices.store(resources.getAssetPath(""));
        }

        PreRecordedVoice prerecordedVoice = new PreRecordedVoice(actor, voice);
        storage.createActorEntry(actor, voice, prerecordedVoice);
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
        String voiceGuid = ttsPlayer.getAssignedVoiceFor(actor);

        final Voice voice;
        if (voiceGuid == null) {
            voice = ttsPlayer.getVoiceFor(actor);
        } else {
            voice = voices.get(voiceGuid);
        }

        if (voice == null) {
            throw new IllegalArgumentException(
                    "Voice for actor '" + actor + "' not found in " + ActorVoices.VoicesFilename);
        }
        return voice;
    }

    public void finish() throws IOException, InterruptedException, ExecutionException {
        storage.close();

        if (numberOfPasses > 1) {
            logger.info("Generating speech files for all symbols generates a lot of reused entries");
        }

        long now = System.currentTimeMillis();
        long seconds = (now - buildStart) / 1000;
        String duration = String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
        logger.info("Finished - Build time : " + duration);

        logger.info(upToDateEntries + " up to date, " + reusedDuplicates + " reused, " + changedEntries + " changed, "
                + newEntries + " new");
    }

    static String readMessage(InputStream inputStream) throws IOException {
        StringBuilder message = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
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

    public void create(Actor actor, Voice voice, Message message, String hash, String messageHash)
            throws InterruptedException {
        writeMessageHash(actor, voice, hash, messageHash);

        List<Future<String>> soundFileFutures = writeSpeechResources(actor, voice, message, hash, messageHash);
        List<String> soundFiles = soundFileFutures.stream().map(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.<String> toList());

        writeInventory(actor, voice, hash, soundFiles);
    }

    private List<Future<String>> writeSpeechResources(Actor actor, Voice voice, Message message, String hash,
            String messageHash) throws InterruptedException {
        logger.info("Recording message:\n" + messageHash);
        List<Future<String>> soundFiles = new ArrayList<>();
        String mood = Mood.Neutral;
        storage.createNewEntry(actor, voice, hash, messageHash);
        for (Part part : ttsPlayer.speechMessage(message).getParts()) {
            if (part.type == Message.Type.Mood) {
                mood = part.value;
            } else if (part.type == Message.Type.Speech) {
                int index = soundFiles.size();
                Future<String> soundFileName = writeSpeechResource(actor, voice, hash, index, mood, part.value);
                soundFiles.add(soundFileName);
            }
        }
        return soundFiles;
    }

    private Future<String> writeSpeechResource(Actor actor, Voice voice, String hash, int index, String mood,
            String text) throws InterruptedException {
        String soundFileName = Integer.toString(index);
        File soundFile = createTempFileName(SpeechResourceTempFilePrefix + "_" + soundFileName + "_",
                SpeechResourceFileUncompressedFormat);
        TextToSpeech textToSpeech = ttsPlayer.textToSpeech;
        String recordedSoundFile = textToSpeech.speak(voice, text, soundFile, new String[] { mood });
        String storedSoundFileName = new File(soundFileName + SpeechResourceFileTypeExtension).getName();
        if (!recordedSoundFile.endsWith(SpeechResourceFileTypeExtension)) {
            return storage.encode(() -> {
                String encodedSoundFile = recordedSoundFile.replace(SpeechResourceFileUncompressedFormat,
                        SpeechResourceFileTypeExtension);
                String[] argv = { recordedSoundFile, encodedSoundFile, "--preset", "standard" };
                try {
                    logger.info("Recording part " + index);
                    mp3.Main mp3Encoder = new mp3.Main();
                    mp3Encoder.run(argv);
                } finally {
                    if (!new File(recordedSoundFile).delete()) {
                        throw new IllegalStateException("Can't delete temporary speech file " + recordedSoundFile);
                    }
                }
                storage.storeRecordedSoundFile(actor, voice, hash, soundFileName, encodedSoundFile);
                return storedSoundFileName;
            });
        } else {
            return storage.storeRecordedSoundFile(actor, voice, hash, storedSoundFileName, recordedSoundFile);
        }
    }

    private void writeInventory(Actor actor, Voice voice, String hash, List<String> soundFiles) {
        StringBuilder inventory = new StringBuilder();
        for (String soundFile : soundFiles) {
            inventory.append(soundFile);
            inventory.append("\n");
        }
        storage.writeStringResource(actor, voice, hash, TextToSpeechRecorder.ResourcesFilename, inventory.toString());
    }

    private void writeMessageHash(Actor actor, Voice voice, String hash, String messageHash) {
        storage.writeStringResource(actor, voice, hash, TextToSpeechRecorder.MessageFilename, messageHash);
    }

    private static File createTempFileName(String prefix, String suffix) {
        return new File(System.getProperty("java.io.tmpdir"), prefix + UUID.randomUUID() + suffix);
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
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
