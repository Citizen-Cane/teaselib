package teaselib.core.texttospeech;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.core.Configuration;
import teaselib.core.ResourceLoader;

public class TextToSpeechPlayer {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechPlayer.class);

    TextToSpeech textToSpeech;
    private final Set<ResourceLoader> loadedActorVoiceProperties = new HashSet<>();
    private final PronunciationDictionary pronounciationDictionary;

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

    private final Configuration config;
    private final PreferredVoices preferredVoices;

    public TextToSpeechPlayer(Configuration config) {
        this.config = config;
        if (config.has(Settings.Pronunciation)) {
            this.pronounciationDictionary = new PronunciationDictionary(new File(config.get(Settings.Pronunciation)));
        } else {
            this.pronounciationDictionary = PronunciationDictionary.empty();
        }
        preferredVoices = new PreferredVoices();
    }

    TextToSpeechPlayer(Configuration config, TextToSpeechImplementation textToSpeechImplementation) {
        this(config);
        initTextToSpeech(new TextToSpeech(textToSpeechImplementation));
    }

    private void lazyInitTTS() {
        if (this.textToSpeech == null) {
            initTextToSpeech(new TextToSpeech());
        }
    }

    void reload() {
        preferredVoices.clear();
        actorKey2TTSVoice.clear();
        voices.clear();
        usedVoices.clear();

        initTextToSpeech();
    }

    public void initTextToSpeech(TextToSpeech textToSpeech) {
        this.textToSpeech = textToSpeech;
        initTextToSpeech();
    }

    void initTextToSpeech() {
        try {
            readPreferredVoices();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }

        if (this.textToSpeech.isReady()) {
            Map<String, String> preferredVoiceGuids = preferredVoices.getPreferredVoiceGuids();
            Set<String> ignoredVoiceGuids = preferredVoices.getDisabledVoiceGuids();

            try {
                this.textToSpeech.initPhoneticDictionary(pronounciationDictionary);
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }

            Map<String, Voice> allVoices = this.textToSpeech.getVoices();
            voices.putAll(sortedVoicesAccordingToSettings(allVoices, preferredVoiceGuids, ignoredVoiceGuids));

            if (logger.isInfoEnabled()) {
                logPreferredVoices(voices, preferredVoiceGuids);
                logOtherVoices(voices, preferredVoiceGuids, ignoredVoiceGuids);
                logIgnoredVoices(allVoices, ignoredVoiceGuids);
            }
        }
    }

    public void readPreferredVoices() throws IOException {
        if (config.has(Settings.Voices)) {
            preferredVoices.load(new File(config.get(Settings.Voices)));
        }
    }

    private static void logPreferredVoices(Map<String, Voice> voices, Map<String, String> preferredVoiceGuids) {
        for (Entry<String, String> entry : preferredVoiceGuids.entrySet()) {
            logger.info("Preferring voice " + voices.get(entry.getKey()) + "=" + entry.getValue());
        }
    }

    public static void logOtherVoices(Map<String, Voice> voices, Map<String, String> preferredVoiceGuids,
            Set<String> ignoredVoiceGuids) {
        for (String key : voices.keySet()) {
            if (!preferredVoiceGuids.containsKey(key) && !ignoredVoiceGuids.contains(key)) {
                logger.info("Using voice " + voices.get(key).toString());
            }
        }
    }

    private static void logIgnoredVoices(Map<String, Voice> voices, Set<String> ignoredVoiceGuids) {
        for (String guid : ignoredVoiceGuids) {
            logger.info("Ignoring voice " + voices.get(guid));
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
            // Get the list of actor to voice assignments
            ActorVoices actorVoices = new ActorVoices(resources);
            for (String actorKey : actorVoices.keySet()) {
                // Available as a pre-recorded voice?
                String voiceGuid = actorVoices.getGuid(actorKey);
                PreRecordedVoice preRecordedVoice;
                try {
                    preRecordedVoice = new PreRecordedVoice(
                            resources.getResource(PreRecordedVoice.getResourcePath(actorKey, voiceGuid)));
                } catch (IOException e) {
                    preRecordedVoice = null;
                }
                if (!actorKey2TTSVoice.containsKey(actorKey) && preRecordedVoice != null) {
                    usePrerecordedVoice(actorKey, voiceGuid);
                } else {
                    logger.info("Actor key=" + actorKey + ": prerecorded voice '" + voiceGuid + "' not available");
                    reserveVoice(actorKey, voiceGuid);
                }
            }
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
        logger.info("Actor " + actorKey + ": using prerecorded voice '" + voiceGuid + "'");
    }

    private void useTTSVoice(String actorKey, String voiceGuid) {
        Voice voice = voices.get(voiceGuid);
        if (voice != null) {
            logger.info("Actor key=" + actorKey + ": using TTS voice '" + voiceGuid + "'");
            actorKey2TTSVoice.put(actorKey, voice);
            usedVoices.add(voiceGuid);
        } else {
            logger.info("Actor key=" + actorKey + ": assigned voice '" + voiceGuid + "' not available");
        }
    }

    Voice getVoiceFor(Actor actor) {
        lazyInitTTS();

        if (actorKey2PrerecordedVoiceGuid.containsKey(actor.key)) {
            throw new IllegalStateException("Prerecorded voice available");
        }
        if (actorKey2TTSVoice.containsKey(actor.key)) {
            return actorKey2TTSVoice.get(actor.key);
        } else if (actorKey2ReservedVoiceGuid.containsKey(actor.key)) {
            return getReservedVoice(actor);
        } else {
            return getMatchingOrBestVoiceFor(actor);
        }
    }

    private Voice getReservedVoice(Actor actor) {
        String voiceGuid = actorKey2ReservedVoiceGuid.get(actor.key);
        if (voiceGuid != null) {
            useTTSVoice(actor.key, voiceGuid);
            return getVoiceFor(actor);
        } else {
            return getMatchingOrBestVoiceFor(actor);
        }
    }

    String getAssignedVoiceFor(Actor actor) {
        return actorKey2PrerecordedVoiceGuid.get(actor.key);
    }

    private Voice getMatchingOrBestVoiceFor(Actor actor) {
        Voice voice = null;
        String guid = getAssignedVoiceFor(actor);
        if (guid != null) {
            voice = voices.get(guid);
        }
        if (voice == null && voices.size() > 0) {
            voice = getMatchingVoiceFor(actor);
        }

        if (voice != null) {
            actorKey2TTSVoice.put(actor.key, voice);
            usedVoices.add(voice.guid);
        }

        return voice;
    }

    private Voice getMatchingVoiceFor(Actor actor) {
        // Filter the actor's gender
        List<Voice> genderFilteredVoices = new ArrayList<>();
        for (Entry<String, Voice> voice : voices.entrySet()) {
            if (actor.gender == voice.getValue().gender) {
                genderFilteredVoices.add(voice.getValue());
            }
        }
        // Full match: language and region
        for (Voice voice : genderFilteredVoices) {
            if (voice.matches(actor.getLocale()) && !voiceInUse(voice)) {
                useVoice(actor, voice);
                return voice;
            }
        }
        // Partial match: language only
        for (Voice voice : genderFilteredVoices) {
            String voiceLanguage = voice.locale.substring(0, 2);
            String actorLanguage = actor.getLocale().getLanguage();
            if (voiceLanguage.equals(actorLanguage) && !voiceInUse(voice)) {
                useVoice(actor, voice);
                return voice;
            }
        }
        // Reuse voice of first non-dominant actor
        for (String actorName : actorKey2TTSVoice.keySet()) {
            if (!isDominantActor(actorName) && actorKey2TTSVoice.get(actorName).gender == actor.gender) {
                return reuseVoice(actor, actorName);
            }
        }
        // voice of default dominant actor
        for (String actorKey : actorKey2TTSVoice.keySet()) {
            if (isDominantActor(actorKey) && actorKey2TTSVoice.get(actorKey).gender == actor.gender) {
                return reuseVoice(actor, actorKey);
            }
        }
        // No voice
        logger.warn("No voice available for '" + actor.toString() + "'");
        return null;
    }

    private boolean voiceInUse(Voice voice) {
        return usedVoices.contains(voice.guid);
    }

    private static void useVoice(Actor actor, Voice voice) {
        logger.info("Actor " + actor.toString() + " uses voice " + voice.guid);
    }

    private Voice reuseVoice(Actor actor, String reusedActorKey) {
        Voice voice = actorKey2TTSVoice.get(reusedActorKey);
        actorKey2TTSVoice.put(actor.key, voice);
        logger.warn("Actor " + actor.toString() + " re-uses voice " + voice.guid + " of actor " + reusedActorKey);
        return voice;
    }

    private static boolean isDominantActor(String actorName) {
        return actorName.compareToIgnoreCase(Actor.Key.DominantFemale) == 0
                || actorName.compareToIgnoreCase(Actor.Key.DominantMale) == 0;
    }

    public void acquireVoice(Actor actor, ResourceLoader resources) {
        loadActorVoiceProperties(resources);
        if (!prerenderedSpeechAvailable(actor)) {
            reserveVoice(actor.key, null);
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
        lazyInitTTS();

        boolean useTTS = textToSpeech.isReady();
        if (useTTS) {
            Voice voice = getVoiceFor(actor);
            try {
                textToSpeech.speak(voice, pronounciationDictionary.correct(voice, prompt), new String[] { mood });
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

    public void stop(Actor actor) {
        if (textToSpeech != null) {
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

    public boolean prerenderedSpeechAvailable(Actor actor) {
        return actorKey2PrerecordedVoiceGuid.containsKey(actor.key);
    }

    public Message createPrerenderedSpeechMessage(Message message, ResourceLoader resources) {
        if (message.toPrerecordedSpeechHashString().isEmpty()) {
            return message;
        } else {
            try {
                Iterator<String> prerenderedSpeechFiles = getSpeechResources(message, resources).iterator();
                // Render pre-recorded speech as sound
                Message preRenderedSpeechMessage = new Message(message.actor);
                for (Part part : message.getParts()) {
                    if (part.type == Message.Type.Text) {
                        preRenderedSpeechMessage.add(Message.Type.Speech, prerenderedSpeechFiles.next());
                        preRenderedSpeechMessage.add(part);
                    } else {
                        preRenderedSpeechMessage.add(part);
                    }
                }
                return preRenderedSpeechMessage;
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                // Render missing pre-recorded speech as delay
                Message preRenderedSpeechMessage = new Message(message.actor);
                for (Part part : message.getParts()) {
                    if (part.type == Message.Type.Text) {
                        preRenderedSpeechMessage.add(part);
                        int durationSeconds = Math
                                .toIntExact(TextToSpeech.getEstimatedSpeechDuration(part.value) / 1000);
                        preRenderedSpeechMessage.add(Message.Type.Delay, Integer.toString(durationSeconds));
                    } else {
                        preRenderedSpeechMessage.add(part);
                    }
                }
                return preRenderedSpeechMessage;
            }
        }
    }

    /**
     * Retrieve speech resources for the message, selected actor voice
     * 
     * @param message
     * @return
     * @throws IOException
     */
    private List<String> getSpeechResources(Message message, ResourceLoader resources) throws IOException {
        String key = message.actor.key;
        String voice = actorKey2PrerecordedVoiceGuid.get(key);
        if (voice == null) {
            return null;
        } else {
            String path = actorKey2SpeechResourcesLocation.get(key) + TextToSpeechRecorder.getHash(message) + "/";
            BufferedReader reader = null;
            List<String> speechResources = new ArrayList<>();
            try {
                reader = new BufferedReader(
                        new InputStreamReader(resources.getResource(path + TextToSpeechRecorder.ResourcesFilename)));
                String soundFile = null;
                while ((soundFile = reader.readLine()) != null) {
                    speechResources.add(path + soundFile);
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
            return speechResources;
        }
    }
}
