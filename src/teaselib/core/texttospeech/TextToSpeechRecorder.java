package teaselib.core.texttospeech;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Message;
import teaselib.MessagePart;
import teaselib.Mood;
import teaselib.core.AbstractMessage;
import teaselib.core.CommandLineHost;
import teaselib.core.Configuration;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLibConfigSetup;
import teaselib.core.util.ExceptionUtil;
import teaselib.util.TextVariables;

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
    private final long buildStart;
    private final TextVariables textVariables;

    static class Pass {
        class Symbol {
            final String key;
            final String value;

            public Symbol(String key, String value) {
                super();
                this.key = key;
                this.value = value;
            }

            @Override
            public String toString() {
                return key + "=" + value;
            }

        }

        final List<Symbol> symbols = new ArrayList<>();

        int newEntries = 0;
        int changedEntries = 0;
        int upToDateEntries = 0;
        int reusedDuplicates = 0;

        public Pass(String key, String value) {
            symbols.add(new Symbol(key, value));
        }

        public Pass(List<Symbol> symbols) {
            this.symbols.addAll(symbols);
        }

        static Pass sum(List<Pass> passes) {
            List<Symbol> symbols = new ArrayList<>();
            for (Pass p : passes) {
                symbols.addAll(p.symbols);
            }

            Pass sum = new Pass(symbols);
            for (Pass p : passes) {
                sum.newEntries += p.newEntries;
                sum.changedEntries += p.changedEntries;
                sum.upToDateEntries += p.upToDateEntries;
                sum.reusedDuplicates += p.reusedDuplicates;
            }
            return sum;
        }

        private void log() {
            StringBuilder processSymbols = new StringBuilder();
            boolean appendSeparator = false;
            for (Symbol symbol : symbols) {
                if (appendSeparator) {
                    processSymbols.append(" ");
                }
                processSymbols.append(symbol.toString());
                appendSeparator = true;
            }
            logger.info("{}: {} up to date, {} reused, {} changed, {} new  => {}", processSymbols, upToDateEntries,
                    reusedDuplicates, changedEntries, newEntries,
                    (upToDateEntries + reusedDuplicates + changedEntries + newEntries));
        }

    }

    List<Pass> passes = new ArrayList<>();
    Pass pass;
    Pass sum;

    public TextToSpeechRecorder(File path, String name, ResourceLoader resources, TextVariables textVariables)
            throws IOException {
        this(path, name, resources, textVariables, new Configuration(new TeaseLibConfigSetup(new CommandLineHost())));
    }

    public TextToSpeechRecorder(File path, String name, ResourceLoader resources, TextVariables textVariables,
            Configuration config) throws IOException {
        this.resources = resources;
        this.textVariables = textVariables;

        this.ttsPlayer = new TextToSpeechPlayer(config);
        this.ttsPlayer.load();
        this.buildStart = System.currentTimeMillis();
        this.voices = ttsPlayer.textToSpeech.getVoices();
        this.storage = new StorageSynchronizer(new PrerecordedSpeechZipStorage(path, resources.getRoot(), name));
        this.actorVoices = new ActorVoices(resources);
        logger.info("Build start: {} with {} encoding threads", new Date(buildStart), storage.getEncodingThreads());
        ttsPlayer.loadActorVoiceProperties(resources);
    }

    // TODO Add text variables to preparePass
    public void startPass(String key, String value) {
        pass = new Pass(key, value);
        passes.add(pass);

        logger.info("Pass {} for {}={}", passes.size(), key, value);
        logger.info("using text variables: '{}'", textVariables);
    }

    public void run(ScriptScanner scanner) throws IOException, InterruptedException, ExecutionException {
        logger.info("Scanning script '{}'", scanner.getScriptName());
        Set<String> created = new HashSet<>();
        for (Message message : scanner) {

            Actor actor = message.actor;
            Voice voice = getVoice(actor);
            if (!actors.contains(actor.key)) {
                createActorEntry(actor, voice);
            }
            String hash = processMessage(actor, voice, message);
            created.add(hash);
        }
    }

    private String processMessage(Actor actor, Voice voice, Message message)
            throws InterruptedException, ExecutionException {
        storage.checkForAsynchronousErrors();

        String hash = getHash(message);
        String newMessageHash = message.toPrerecordedSpeechHashString();
        if (!newMessageHash.isEmpty()) {
            if (storage.hasMessage(actor, voice, hash)) {
                long lastModified = storage.lastModified(actor, voice, hash);
                String oldMessageHash = storage.getMessageHash(actor, voice, hash);
                if (oldMessageHash.equals(newMessageHash)) {
                    keepOrReuseMessage(actor, voice, hash, lastModified);
                } else if (lastModified > buildStart) {
                    handleCollision(actor, voice, hash, oldMessageHash, newMessageHash);
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
            log(actor, voice, hash, "is reused");
            pass.reusedDuplicates++;
        } else {
            log(actor, voice, hash, "is up to date");
            storage.keepMessage(actor, voice, hash);
            pass.upToDateEntries++;
        }
    }

    private static void handleCollision(Actor actor, Voice voice, String hash, String oldMessageHash,
            String newMessageHash) {
        log(actor, voice, hash, "collision!");
        logger.info("Old: {}", oldMessageHash);
        logger.info("New: {}", newMessageHash);
        throw new IllegalStateException("Collision");
    }

    private void updateMessage(Actor actor, Voice voice, Message message, String hash, String newMessageHash)
            throws InterruptedException {
        log(actor, voice, hash, "has changed");
        storage.deleteMessage(actor, voice, hash);
        create(actor, voice, message, hash, newMessageHash);
        pass.changedEntries++;
    }

    private void createNewMessage(Actor actor, Voice voice, Message message, String hash) throws InterruptedException {
        log(actor, voice, hash, "is new");
        create(actor, voice, message, hash, message.toPrerecordedSpeechHashString());
        pass.newEntries++;
    }

    private static void log(Actor actor, final Voice voice, String hash, String message) {
        if (logger.isInfoEnabled()) {
            logger.info("{} {} {} {}", actor.key, voice.info().name, hash, message);
        }
    }

    private void createActorEntry(Actor actor, Voice voice) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("Creating actor entry for {}", voice.info().name);
        }
        actors.add(actor.key);
        actorVoices.putGuid(actor.key, voice);

        if (!haveActorVoicesFile()) {
            actorVoices.store(resources.getAssetPath(""));
        }

        PreRecordedVoice prerecordedVoice = new PreRecordedVoice(actor, voice);
        storage.createActorEntry(actor, voice, prerecordedVoice);
    }

    private boolean haveActorVoicesFile() throws IOException {
        try (InputStream is = resources.get(ActorVoices.VoicesFilename);) {
            return is != null;
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

    public void finish() throws IOException, InterruptedException {
        storage.close();

        if (passes.size() > 1) {
            logger.info("Generating speech files for all symbols generates a lot of reused entries");
        }

        for (Pass p : passes) {
            p.log();
        }

        if (logger.isInfoEnabled()) {
            long now = System.currentTimeMillis();
            long seconds = (now - buildStart) / 1000;
            String duration = String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
            logger.info("Finished - Build time : {} using {} of {} encoding threads", duration,
                    storage.getUsedEncodingThreads(), storage.getEncodingThreads());
        }

        sum = Pass.sum(passes);
        sum.log();
    }

    static String readMessage(InputStream inputStream) throws IOException {
        StringBuilder message = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (message.length() > 0) {
                    message.append("\n");
                }
                message.append(line);
            }
        }
        return message.toString();
    }

    public void create(Actor actor, Voice voice, Message message, String hash, String messageHash)
            throws InterruptedException {
        List<String> soundFiles = writeSpeechResources(actor, voice, message, hash, messageHash);
        writeMessageHash(actor, voice, hash, messageHash);
        writeInventory(actor, voice, hash, soundFiles);
    }

    private List<String> writeSpeechResources(Actor actor, Voice voice, Message message, String hash,
            String messageHash) throws InterruptedException {
        logger.info("Recording message:\n{}", messageHash);
        List<String> soundFiles = new ArrayList<>();
        String mood = Mood.Neutral;
        storage.createNewEntry(actor, voice, hash, messageHash);

        for (MessagePart part : ttsPlayer.speechMessage(message)) {
            if (part.type == Message.Type.Mood) {
                mood = part.value;
            } else if (part.type == Message.Type.Speech) {
                int index = soundFiles.size();
                String name = Integer.toString(index);
                String storageSoundFile = storageSoundFile(name);
                writeSpeechResource(actor, voice, hash, storageSoundFile, mood, part.value);
                soundFiles.add(storageSoundFile);
            }
        }

        return soundFiles;
    }

    private Future<String> writeSpeechResource(Actor actor, Voice voice, String hash, String storageSoundFile,
            String mood, String text) throws InterruptedException {
        String recordedSoundFile = generateMultithreadingIncomatibleTextToSpeechPlayer(actor, mood, text);

        return storage.encode(() -> {
            if (!recordedSoundFile.endsWith(SpeechResourceFileTypeExtension)) {
                try {
                    String encodedSoundFile = recordedSoundFile.replace(SpeechResourceFileUncompressedFormat,
                            SpeechResourceFileTypeExtension);
                    String[] argv = { recordedSoundFile, encodedSoundFile, "--preset", "standard" };
                    logger.info("Recording part {}", storageSoundFile);
                    mp3.Main mp3Encoder = new mp3.Main();
                    mp3Encoder.run(argv);
                    return storage.storeRecordedSoundFile(actor, voice, hash, encodedSoundFile, storageSoundFile).get();
                } finally {
                    Files.delete(Paths.get(recordedSoundFile));
                }
            } else {
                return storage.storeRecordedSoundFile(actor, voice, hash, recordedSoundFile, storageSoundFile).get();
            }
        });
    }

    private String generateMultithreadingIncomatibleTextToSpeechPlayer(Actor actor, String mood, String text)
            throws InterruptedException {
        return ttsPlayer.speak(actor, text, mood,
                createTempFileName(SpeechResourceTempFilePrefix + "_", SpeechResourceFileUncompressedFormat));
    }

    private static String storageSoundFile(String name) {
        return new File(name + SpeechResourceFileTypeExtension).getName();
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

    public static String getHash(AbstractMessage message) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
        byte[] string = null;
        try {
            string = message.toPrerecordedSpeechHashString().getBytes("UTF-16");
        } catch (UnsupportedEncodingException e) {
            throw ExceptionUtil.asRuntimeException(e);
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

    public File assetPath() {
        return storage.assetPath();
    }
}
