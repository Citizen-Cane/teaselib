package teaselib.core.texttospeech;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Config.Debug;
import teaselib.Message;
import teaselib.MessagePart;
import teaselib.core.AbstractMessage;
import teaselib.core.AudioSync;
import teaselib.core.Closeable;
import teaselib.core.ResourceLoader;
import teaselib.core.configuration.Configuration;
import teaselib.core.util.ExceptionUtil;

public class TextToSpeechPlayer implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechPlayer.class);

    private static final String SIMULATED_SPEECH_TAG = "SimulatedSpeech=";

    private static final String ACQUIRE_VOICE_ON_FIRST_USE = "ACQUIRE_VOICE_ON_FIRST_USE";

    private final Configuration config;
    private final TextToSpeech textToSpeech;
    private final PronunciationDictionary pronunciationDictionary;

    private final PreferredVoices preferredVoices = new PreferredVoices();
    private final Set<ResourceLoader> loadedActorVoiceProperties = new HashSet<>();

    public final AudioSync audioSync;

    /**
     * voice guid to voice
     */
    private final Map<String, Voice> voices = new LinkedHashMap<>();

    /**
     * Actor key to prerecorded voice guid
     */
    private final Map<String, String> actorKey2PrerecordedVoiceGuid = new HashMap<>();

    /**
     * Actor key to speech resources location
     */
    private final Map<String, String> actorKey2SpeechResourcesLocation = new HashMap<>();

    /**
     * Actor key to TTS voice
     */
    private final Map<String, Voice> actorKey2TTSVoice = new HashMap<>();

    /**
     * guids of reserved voices
     */
    private final Map<String, String> actorKey2ReservedVoiceGuid = new LinkedHashMap<>();

    /**
     * guids of used voices
     */
    private final Set<String> usedVoices = new HashSet<>();

    public enum Settings {
        Voices,
        Pronunciation;
    }

    public TextToSpeechPlayer(Configuration config) {
        this(config, Boolean.parseBoolean(config.get(Config.Render.Speech)) ? TextToSpeech.allSystemVoices()
                : TextToSpeech.none());
    }

    TextToSpeechPlayer(Configuration config, TextToSpeechImplementation textToSpeechImplementation) {
        this(config, new TextToSpeech(textToSpeechImplementation));
    }

    public TextToSpeechPlayer(Configuration config, TextToSpeech textToSpeech) {
        this(config, textToSpeech, loadPronunciationDictionary(config));
    }

    public TextToSpeechPlayer(Configuration config, TextToSpeech textToSpeech,
            PronunciationDictionary pronunciationDictionary) {
        this.config = config;
        this.textToSpeech = textToSpeech;
        this.pronunciationDictionary = pronunciationDictionary;
        this.audioSync = textToSpeech.audioSync;

        if (Boolean.parseBoolean(config.get(Config.Render.Speech))) {
            loadVoices();
        }
    }

    private static PronunciationDictionary loadPronunciationDictionary(Configuration config) {
        PronunciationDictionary pronounciationDictionary;
        if (config.has(Settings.Pronunciation)) {
            try {
                pronounciationDictionary = new PronunciationDictionary(new File(config.get(Settings.Pronunciation)));
            } catch (IOException e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
        } else {
            pronounciationDictionary = PronunciationDictionary.empty();
        }
        return pronounciationDictionary;
    }

    public void reload() {
        preferredVoices.clear();
        actorKey2TTSVoice.clear();
        voices.clear();
        usedVoices.clear();

        loadVoices();
    }

    private void loadVoices() {
        try {
            readPreferredVoices();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }

        Map<String, String> preferredVoiceGuids = preferredVoices.getPreferredVoiceGuids();
        Set<String> ignoredVoiceGuids = preferredVoices.getDisabledVoiceGuids();

        try {
            textToSpeech.initPhoneticDictionary(pronunciationDictionary);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }

        Map<String, Voice> allVoices = textToSpeech.getVoices();
        voices.putAll(sortedVoicesAccordingToSettings(allVoices, preferredVoiceGuids, ignoredVoiceGuids));

        if (logger.isInfoEnabled()) {
            logPreferredVoices(voices, preferredVoiceGuids);
            logOtherVoices(voices, preferredVoiceGuids, ignoredVoiceGuids);
            logIgnoredVoices(allVoices, ignoredVoiceGuids);
        }
    }

    public void readPreferredVoices() throws IOException {
        if (config.has(Settings.Voices)) {
            preferredVoices.load(new File(config.get(Settings.Voices)));
        }
    }

    private static void logPreferredVoices(Map<String, Voice> voices, Map<String, String> preferredVoiceGuids) {
        for (Entry<String, String> entry : preferredVoiceGuids.entrySet()) {
            logger.info("Preferring voice {}={}", voices.get(entry.getKey()), entry.getValue());
        }
    }

    public static void logOtherVoices(Map<String, Voice> voices, Map<String, String> preferredVoiceGuids,
            Set<String> ignoredVoiceGuids) {
        for (Entry<String, Voice> entry : voices.entrySet()) {
            String key = entry.getKey();
            if (!preferredVoiceGuids.containsKey(key) && !ignoredVoiceGuids.contains(key)) {
                logger.info("Using voice {}", voices.get(key));
            }
        }
    }

    private static void logIgnoredVoices(Map<String, Voice> voices, Set<String> ignoredVoiceGuids) {
        for (String guid : ignoredVoiceGuids) {
            logger.info("Ignoring voice {}", voices.get(guid));
        }
    }

    public Map<String, Voice> sortedVoicesAccordingToSettings(Map<String, Voice> voices,
            Map<String, String> preferredVoiceGuids, Set<String> ignoredVoiceGuids) {
        Map<String, Voice> sorted = new LinkedHashMap<>();

        for (Entry<String, String> preferred : preferredVoiceGuids.entrySet()) {
            Voice voice = voices.get(preferred.getKey());
            if (voice != null) {
                sorted.put(preferred.getKey(), voice);
            }
        }

        for (Entry<String, Voice> entry : voices.entrySet()) {
            String guid = entry.getKey();
            if (!preferredVoiceGuids.containsKey(guid) && !ignoredVoiceGuids.contains(guid)) {
                sorted.put(guid, entry.getValue());
            }
        }

        return sorted;
    }

    /**
     * Actors may have assigned or pre-recorded voices. These are stored in the resources section of each project.
     * 
     * The configuration has to be loaded for each resource loader instance.
     * 
     * @param resources
     *            The resource object that contains the assignments and/or pre-recorded speech.
     */
    public void loadActorVoiceProperties(ResourceLoader resources) {
        if (!loadedActorVoiceProperties.contains(resources)) {
            loadedActorVoiceProperties.add(resources);
            var voiceAssignments = new ActorVoices(resources);
            for (String actorKey : voiceAssignments.keySet()) {
                String voiceGuid = voiceAssignments.getGuid(actorKey);
                Optional<PreRecordedVoice> preRecordedVoice = getPrerecordedVoice(resources, actorKey, voiceGuid);
                if (!actorKey2TTSVoice.containsKey(actorKey) && preRecordedVoice.isPresent()) {
                    usePrerecordedVoice(actorKey, voiceGuid);
                } else {
                    logger.info("Actor key={}: prerecorded voice '{}' not available", actorKey, voiceGuid);
                    reserveVoice(actorKey, voiceGuid);
                }
            }
        }
    }

    private static Optional<PreRecordedVoice> getPrerecordedVoice(ResourceLoader resources, String actorKey,
            String voiceGuid) {
        try {
            PreRecordedVoice preRecordedVoice = new PreRecordedVoice(
                    resources.get(PreRecordedVoice.getResourcePath(actorKey, voiceGuid)));
            return Optional.of(preRecordedVoice);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void reserveVoice(String actorKey, String voiceGuid) {
        if (actorKey2PrerecordedVoiceGuid.containsKey(actorKey))
            return;
        if (actorKey2TTSVoice.containsKey(actorKey))
            return;
        if (actorKey2ReservedVoiceGuid.containsKey(actorKey))
            return;

        actorKey2ReservedVoiceGuid.put(actorKey, voiceGuid);
    }

    private void usePrerecordedVoice(String actorKey, String voiceGuid) {
        actorKey2PrerecordedVoiceGuid.put(actorKey, voiceGuid);
        usedVoices.add(voiceGuid);
        String speechResources = PrerecordedSpeechStorage.SpeechDirName + "/" + actorKey + "/" + voiceGuid + "/";
        actorKey2SpeechResourcesLocation.put(actorKey, speechResources);
        logger.info("Actor {}: using prerecorded voice '{}'", actorKey, voiceGuid);
    }

    public void useTTSVoice(String actorKey, String voiceGuid) {
        var voice = voices.get(voiceGuid);
        if (voice != null) {
            logger.info("Actor key={}: using TTS voice '{}'", actorKey, voiceGuid);
            actorKey2TTSVoice.put(actorKey, voice);
            usedVoices.add(voiceGuid);
        } else {
            logger.info("Actor key={}: assigned voice '{}' not available", actorKey, voiceGuid);
            throw new IllegalArgumentException(actorKey + "->" + voiceGuid);
        }
    }

    Voice getVoiceFor(Actor actor) {
        if (hasTTSVoice(actor)) {
            return actorKey2TTSVoice.get(actor.key);
        } else if (hasReservedVoice(actor)) {
            return getReservedVoice(actor);
        } else {
            return getMatchingOrBestVoiceFor(actor);
        }
    }

    public boolean hasTTSVoice(Actor actor) {
        return actorKey2TTSVoice.containsKey(actor.key);
    }

    public boolean hasReservedVoice(Actor actor) {
        return actorKey2ReservedVoiceGuid.containsKey(actor.key);
    }

    public Voice getReservedVoice(Actor actor) {
        String voiceGuid = actorKey2ReservedVoiceGuid.get(actor.key);

        ensureNotPrerecordedVoice(actor);
        ensureVoiceNotAcquired(voiceGuid);

        if (voiceGuid != null && !ACQUIRE_VOICE_ON_FIRST_USE.equals(voiceGuid)) {
            useTTSVoice(actor.key, voiceGuid);
            return getVoiceFor(actor);
        } else {
            return getMatchingOrBestVoiceFor(actor);
        }
    }

    String getPrerecordedVoiceGuidFor(Actor actor) {
        return actorKey2PrerecordedVoiceGuid.get(actor.key);
    }

    private Voice getMatchingOrBestVoiceFor(Actor actor) {
        Voice voice = null;
        String guid = getPrerecordedVoiceGuidFor(actor);
        if (guid != null) {
            voice = voices.get(guid);
        }

        if (voice == null && voices.size() > 0) {
            voice = getMatchingVoiceFor(actor);
        }

        if (voice != null) {
            actorKey2TTSVoice.put(actor.key, voice);
            usedVoices.add(voice.guid());
        }

        return voice;
    }

    private Voice getMatchingVoiceFor(Actor actor) {
        // Filter the actor's gender
        List<Voice> genderFilteredVoices = new ArrayList<>();
        for (Entry<String, Voice> voice : voices.entrySet()) {
            if (actor.gender == voice.getValue().gender()) {
                genderFilteredVoices.add(voice.getValue());
            }
        }
        // Full match: language and region
        for (Voice voice : genderFilteredVoices) {
            if (matches(voice, actor.locale()) && !used(voice)) {
                useVoice(actor, voice);
                return voice;
            }
        }
        // Partial match: language only
        for (Voice voice : genderFilteredVoices) {
            var voiceLanguage = voice.locale().substring(0, 2);
            var actorLanguage = actor.locale().getLanguage();
            if (voiceLanguage.equals(actorLanguage) && !used(voice)) {
                useVoice(actor, voice);
                return voice;
            }
        }
        // Reuse voice of first non-dominant actor
        for (Entry<String, Voice> entry : actorKey2TTSVoice.entrySet()) {
            String key = entry.getKey();
            if (!isDominantActor(key) && actorKey2TTSVoice.get(key).gender() == actor.gender) {
                return reuseVoice(actor, key);
            }
        }
        // voice of default dominant actor
        for (Entry<String, Voice> entry : actorKey2TTSVoice.entrySet()) {
            String key = entry.getKey();
            if (isDominantActor(key) && actorKey2TTSVoice.get(key).gender() == actor.gender) {
                return reuseVoice(actor, key);
            }
        }
        // No voice
        logger.warn("No voice available for '{}'", actor);
        return null;
    }

    public boolean matches(Voice voice, Locale locale) {
        return voice.locale().replace("-", "_").equalsIgnoreCase(locale.toString());
    }

    private boolean used(Voice voice) {
        return usedVoices.contains(voice.guid());
    }

    private static void useVoice(Actor actor, Voice voice) {
        logger.info("Actor {} uses voice {}", actor, voice);
    }

    private Voice reuseVoice(Actor actor, String reusedActorKey) {
        Voice voice = actorKey2TTSVoice.get(reusedActorKey);
        actorKey2TTSVoice.put(actor.key, voice);
        logger.warn("Actor {} re-uses voice {} of actor {}", actor, voice, reusedActorKey);
        return voice;
    }

    private static boolean isDominantActor(String actorName) {
        return actorName.compareToIgnoreCase(Actor.Key.DominantFemale) == 0
                || actorName.compareToIgnoreCase(Actor.Key.DominantMale) == 0;
    }

    public void acquireVoice(Actor actor, ResourceLoader resources) {
        loadActorVoiceProperties(resources);
        if (!prerenderedSpeechAvailable(actor)) {
            reserveVoice(actor.key, ACQUIRE_VOICE_ON_FIRST_USE);
        }
    }

    /**
     * Speak or wait the estimated duration it takes to speak the prompt
     * 
     * @param prompt
     *            What to speak
     * @param completionSync
     *            Object to wait for during pauses
     * @param teaseLib
     *            instance to call sleep on
     */
    public void speak(Actor actor, String prompt, String mood) throws InterruptedException {
        ensureNotPrerecordedVoice(actor);

        var voice = getVoiceFor(actor);
        if (voice != TextToSpeech.None) {
            try {
                // TODO consuming exception renders TextToSpeechPlayerTest.testTextTOSpeechPlayerPhonemeDictionarySetup
                // useless
                textToSpeech.speak(voice, pronunciationDictionary.correct(voice, prompt), new String[] { mood });
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
                waitEstimatedSpeechDuration(prompt);
            }
        } else {
            waitEstimatedSpeechDuration(prompt);
        }
    }

    public String speak(Actor actor, String prompt, String mood, File file) {
        var voice = getVoiceFor(actor);
        if (voice != TextToSpeech.None) {
            try {
                return textToSpeech.speak(voice, pronunciationDictionary.correct(voice, prompt), file,
                        new String[] { mood });
            } catch (IOException e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException(actor.toString());
        }
    }

    public void stop(Actor actor) {
        if (textToSpeech != null) {
            ensureNotPrerecordedVoice(actor);

            textToSpeech.stop(getVoiceFor(actor));
        }
    }

    /**
     * Unable to speak, just display the estimated duration.
     * 
     * @param prompt
     */
    private static void waitEstimatedSpeechDuration(String prompt) throws InterruptedException {
        Thread.sleep(TextToSpeech.getEstimatedSpeechDuration(prompt));
    }

    public AbstractMessage createSpeechMessage(Actor actor, AbstractMessage message, ResourceLoader resources) {
        if (Boolean.parseBoolean(config.get(Config.Render.Speech))) {
            if (prerenderedSpeechAvailable(actor)) {
                return prerenderedSpeechMessage(actor, message, resources);
            } else {
                return speechMessage(message);
            }
        } else {
            return simulatedSpeechMessage(message);
        }
    }

    private static AbstractMessage simulatedSpeechMessage(AbstractMessage message) {
        var speechMessage = new AbstractMessage();
        for (MessagePart part : message) {
            if (part.type == Message.Type.Text) {
                speechMessage.add(part);
                speechMessage.add(Message.Type.Speech, simulatedSpeech(part.value));
            } else {
                speechMessage.add(part);
            }
        }
        return speechMessage;
    }

    private static String simulatedSpeech(String prompt) {
        return SIMULATED_SPEECH_TAG + prompt;
    }

    public static boolean isSimulatedSpeech(String value) {
        return value.startsWith(SIMULATED_SPEECH_TAG);
    }

    public static String getSimulatedSpeechText(String prompt) {
        if (!prompt.startsWith(SIMULATED_SPEECH_TAG)) {
            throw new IllegalArgumentException(prompt);
        }
        return prompt.substring(SIMULATED_SPEECH_TAG.length());
    }

    AbstractMessage speechMessage(AbstractMessage message) {
        var speechMessage = new AbstractMessage();
        for (MessagePart part : message) {
            if (part.type == Message.Type.Text) {
                speechMessage.add(part);
                speechMessage.add(Message.Type.Speech, part.value);
            } else {
                speechMessage.add(part);
            }
        }
        return speechMessage;
    }

    public boolean prerenderedSpeechAvailable(Actor actor) {
        return actorKey2PrerecordedVoiceGuid.containsKey(actor.key);
    }

    public AbstractMessage prerenderedSpeechMessage(Actor actor, AbstractMessage message, ResourceLoader resources) {
        if (message.toPrerecordedSpeechHashString().isEmpty()) {
            return message;
        } else {
            try {
                return injectPrerecordedSpeechParts(actor, message, resources);
            } catch (IOException e) {
                if (Boolean.parseBoolean(config.get(Debug.StopOnAssetNotFound))) {
                    throw ExceptionUtil.asRuntimeException(e, message.buildString(" ", false));
                } else {
                    logger.error(e.getMessage(), e);
                    return renderMissingPrerecordedSpeechAsDelay(message);
                }
            }
        }
    }

    private Message injectPrerecordedSpeechParts(Actor actor, AbstractMessage message, ResourceLoader resources)
            throws IOException {
        Iterator<String> prerenderedSpeechFiles = getSpeechResources(actor, message, resources).iterator();
        Message preRenderedSpeechMessage = new Message(actor);
        for (MessagePart part : message) {
            if (part.type == Message.Type.Text) {
                preRenderedSpeechMessage.add(part);
                preRenderedSpeechMessage.add(Message.Type.Speech, prerenderedSpeechFiles.next());
            } else {
                preRenderedSpeechMessage.add(part);
            }
        }
        return preRenderedSpeechMessage;
    }

    private static AbstractMessage renderMissingPrerecordedSpeechAsDelay(AbstractMessage message) {
        AbstractMessage preRenderedSpeechMessage = new AbstractMessage();
        for (MessagePart part : message) {
            if (part.type == Message.Type.Text) {
                preRenderedSpeechMessage.add(part);
                int durationSeconds = Math.toIntExact(TextToSpeech.getEstimatedSpeechDuration(part.value) / 1000);
                preRenderedSpeechMessage.add(Message.Type.Delay, Integer.toString(durationSeconds));
            } else {
                preRenderedSpeechMessage.add(part);
            }
        }
        return preRenderedSpeechMessage;
    }

    /**
     * Retrieve speech resources for the message, selected actor voice
     * 
     * @param message
     * @return
     * @throws IOException
     */
    private List<String> getSpeechResources(Actor actor, AbstractMessage message, ResourceLoader resources)
            throws IOException {
        String voice = actorKey2PrerecordedVoiceGuid.get(actor.key);
        if (voice == null) {
            return Collections.emptyList();
        } else {
            String path = actorKey2SpeechResourcesLocation.get(actor.key) + TextToSpeechRecorder.getHash(message) + "/";
            List<String> speechResources = new ArrayList<>();
            try (var reader = new BufferedReader(
                    new InputStreamReader(resources.get(path + TextToSpeechRecorder.ResourcesFilename)));) {
                String soundFile = null;
                while ((soundFile = reader.readLine()) != null) {
                    speechResources.add(path + soundFile);
                }
            }
            return speechResources;
        }
    }

    private void ensureVoiceNotAcquired(String voiceGuid) {
        if (actorKey2TTSVoice.containsKey(voiceGuid)) {
            throw new IllegalStateException(voiceGuid);
        }
    }

    private void ensureNotPrerecordedVoice(Actor actor) {
        if (actorKey2PrerecordedVoiceGuid.containsKey(actor.key)) {
            throw new IllegalStateException("Prerecorded voice available");
        }
    }

    @Override
    public void close() {
        textToSpeech.close();
    }

}
