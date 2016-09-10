package teaselib.core.texttospeech;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Actor.FormOfAddress;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.core.ResourceLoader;

public class TextToSpeechPlayer {
    private static final Logger logger = LoggerFactory
            .getLogger(TextToSpeechPlayer.class);

    public final TextToSpeech textToSpeech;
    private final Set<ResourceLoader> processedVoiceActorVoices = new HashSet<ResourceLoader>();

    /**
     * voice guid to voice
     */
    private final Map<String, Voice> voices;

    /**
     * Actor key to prerecorded voice guid
     */
    private final Map<String, String> actorKey2PrerecordedVoiceGuid = new HashMap<String, String>();

    /**
     * Actor key to speech resources location
     */
    private final Map<String, String> actorKey2SpeechResourcesLocation = new HashMap<String, String>();

    /**
     * Actor key to TTS voice
     */
    private final Map<String, Voice> actorKey2TTSVoice = new HashMap<String, Voice>();

    /**
     * guids of used voices
     */
    private final Set<String> usedVoices = new HashSet<String>();

    private static TextToSpeechPlayer instance = null;

    public static TextToSpeechPlayer instance() {
        synchronized (TextToSpeechPlayer.class) {
            if (instance == null) {
                instance = new TextToSpeechPlayer();
            }
            return instance;
        }
    }

    private TextToSpeechPlayer() {
        this.textToSpeech = new TextToSpeech();
        // TTS might not be available
        if (textToSpeech.isReady()) {
            voices = textToSpeech.getVoices();
        } else {
            voices = new HashMap<String, Voice>();
        }
        // Write list of installed voices to log file in order to provide data
        // for the Actor to Voices mapping properties file
        logger.info("Installed voices:");
        InstalledVoices installedVoices = new InstalledVoices(voices);
        for (String key : installedVoices.keySet()) {
            logger.info(key + ".guid=" + installedVoices.getGuid(key));
        }
    }

    /**
     * Actors may have preferred voices or pre-recorded voices. These are stored
     * in the resources section of each project.
     * 
     * As a result, the configuration has to be loaded for each resource loader
     * instance.
     * 
     * @param resources
     *            The resource object that contains the assignments and/or
     *            pre-recorded speech.
     */
    public void loadActorVoices(ResourceLoader resources,
            String speechResourcesLocation) {
        // Have we read any voice-related configuration files from this resource
        // loader yet?
        if (!processedVoiceActorVoices.contains(resources)) {
            processedVoiceActorVoices.add(resources);
            // Get the list of actor to voice assignments
            ActorVoices actorVoices = new ActorVoices(resources,
                    speechResourcesLocation);
            for (String actorKey : actorVoices.keySet()) {
                // Available as a pre-recorded voice?
                String voiceGuid = actorVoices.getGuid(actorKey);
                PreRecordedVoice preRecordedVoice = new PreRecordedVoice(
                        actorKey, voiceGuid, resources,
                        speechResourcesLocation);
                // Only if actor isn't assigned yet - when called from other
                // script
                if (!actorKey2TTSVoice.containsKey(actorKey)
                        && preRecordedVoice.available()) {
                    actorKey2PrerecordedVoiceGuid.put(actorKey, voiceGuid);
                    usedVoices.add(voiceGuid);
                    String speechResources = speechResourcesLocation + "/"
                            + TextToSpeechRecorder.SpeechDirName + "/"
                            + actorKey + "/" + voiceGuid + "/";
                    actorKey2SpeechResourcesLocation.put(actorKey,
                            speechResources);
                    logger.info("Actor " + actorKey
                            + ": using prerecorded voice '" + voiceGuid + "'");
                } else {
                    logger.info("Actor " + actorKey
                            + ": prerecorded voice not available");
                    Voice voice = voices.get(voiceGuid);
                    if (voice != null) {
                        logger.info("Actor " + actorKey + ": using TTS voice");
                        actorKey2TTSVoice.put(actorKey, voice);
                        usedVoices.add(voiceGuid);
                    } else {
                        logger.info(
                                "Actor " + actorKey + ": voice not available");
                    }
                }
            }
        }
    }

    Voice getVoiceFor(Actor actor) {
        if (actorKey2PrerecordedVoiceGuid.containsKey(actor.key)) {
            throw new IllegalStateException("Prerecorded voice available");
        }
        if (actorKey2TTSVoice.containsKey(actor.key)) {
            return actorKey2TTSVoice.get(actor.key);
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
            if (voice != null) {
                actorKey2TTSVoice.put(actor.key, voice);
                usedVoices.add(voice.guid);
            }
        }
        return voice;
    }

    private Voice getMatchingVoiceFor(Actor actor) {
        // Filter the actor's gender
        Set<Voice> genderFilteredVoices = new LinkedHashSet<Voice>();
        for (Voice voice : voices.values()) {
            if (actor.gender == voice.gender) {
                genderFilteredVoices.add(voice);
            }
        }
        // Full match: language and region
        for (Voice voice : genderFilteredVoices) {
            if (voice.matches(actor.getLocale())
                    && !usedVoices.contains(voice.guid)) {
                useVoice(actor, voice);
                return voice;
            }
        }
        // Partial match: language only
        for (Voice voice : genderFilteredVoices) {
            String voiceLanguage = voice.locale.substring(0, 2);
            String actorLanguage = actor.getLocale().getLanguage();
            if (voiceLanguage.equals(actorLanguage)
                    && !usedVoices.contains(voice.guid)) {
                useVoice(actor, voice);
                return voice;
            }
        }
        // Reuse voice of first non-dominant actor
        for (String actorName : actorKey2TTSVoice.keySet()) {
            if (!isDominantActor(actorName) && actorKey2TTSVoice
                    .get(actorName).gender == actor.gender) {
                return reuseVoice(actor, actorName);
            }
        }
        // voice of default dominant actor
        for (String actorName : actorKey2TTSVoice.keySet()) {
            if (isDominantActor(actorName) && actorKey2TTSVoice
                    .get(actorName).gender == actor.gender) {
                return reuseVoice(actor, actorName);
            }
        }
        // No voice
        logger.warn("No voice available for '" + actor.key + "'");
        return null;
    }

    private static void useVoice(Actor actor, Voice voice) {
        logger.info("Actor '" + actor.get(FormOfAddress.FullName) + "("
                + actor.getLocale() + ") ' uses voice " + voice.guid);
    }

    private Voice reuseVoice(Actor actor, String actorName) {
        Voice voice = actorKey2TTSVoice.get(actorName);
        logger.warn("Actor '" + actor.get(FormOfAddress.FullName) + "("
                + actor.getLocale() + ") ' re-uses voice " + voice.guid
                + " of actor " + actorName);
        return voice;
    }

    private static boolean isDominantActor(String actorName) {
        return actorName.compareToIgnoreCase(Actor.Key.DominantFemale) == 0
                || actorName.compareToIgnoreCase(Actor.Key.DominantMale) == 0;
    }

    public Voice acquireVoice(Actor actor) {
        if (prerenderedSpeechAvailable(actor)) {
            return null;
        } else {
            return getVoiceFor(actor);
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
    public void speak(Actor actor, String prompt, String mood)
            throws InterruptedException {
        boolean useTTS = textToSpeech.isReady();
        if (useTTS) {
            Voice voice = getVoiceFor(actor);
            // Synchronize speaking, only one actor can speak at a time
            textToSpeech.setVoice(voice);
            textToSpeech.setHint(mood);
            try {
                textToSpeech.speak(prompt);
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
                waitEstimatedSpeechDuration(prompt);
            }
        } else {
            waitEstimatedSpeechDuration(prompt);
        }
    }

    public void stop() {
        textToSpeech.stop();
    }

    /**
     * Unable to speak, just display the estimated duration.
     * 
     * @param prompt
     */
    private static void waitEstimatedSpeechDuration(String prompt)
            throws InterruptedException {
        Thread.sleep(TextToSpeech.getEstimatedSpeechDuration(prompt));
    }

    public boolean prerenderedSpeechAvailable(Actor actor) {
        return actorKey2PrerecordedVoiceGuid.containsKey(actor.key);
    }

    public Message getPrerenderedMessage(Message message,
            ResourceLoader resources) {
        // Do we have a pre-recorded voice?
        try {
            Iterator<String> prerenderedSpeechFiles = getSpeechResources(
                    message, resources).iterator();
            // Render pre-recorded speech as sound
            Message preRenderedSpeechMessage = new Message(message.actor);
            for (Part part : message.getParts()) {
                if (part.type == Message.Type.Text) {
                    preRenderedSpeechMessage.add(Message.Type.Speech,
                            prerenderedSpeechFiles.next());
                    preRenderedSpeechMessage.add(part);
                } else {
                    preRenderedSpeechMessage.add(part);
                }
            }
            return preRenderedSpeechMessage;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            // Render pre-recorded speech as delay
            Message preRenderedSpeechMessage = new Message(message.actor);
            for (Part part : message.getParts()) {
                if (part.type == Message.Type.Text) {
                    preRenderedSpeechMessage.add(part);
                    int durationSeconds = Math.toIntExact(
                            TextToSpeech.getEstimatedSpeechDuration(part.value)
                                    / 1000);
                    preRenderedSpeechMessage.add(Message.Type.Delay,
                            Integer.toString(durationSeconds));
                } else {
                    preRenderedSpeechMessage.add(part);
                }
            }
            return preRenderedSpeechMessage;
        }
    }

    /**
     * Retrieve speech resources for the message, selected actor voice
     * 
     * @param message
     * @return
     * @throws IOException
     */
    private List<String> getSpeechResources(Message message,
            ResourceLoader resources) throws IOException {
        String key = message.actor.key;
        String voice = actorKey2PrerecordedVoiceGuid.get(key);
        if (voice == null) {
            return null;
        } else {
            // TODO main script name missing for loading resources
            // getAssetPath doesn't do the trick because
            // it just denotes the parent directory for resource jars/zips
            String path = actorKey2SpeechResourcesLocation.get(key) + "/"
                    + TextToSpeechRecorder.getHash(message) + "/";
            BufferedReader reader = null;
            List<String> speechResources = new Vector<String>();
            try {
                reader = new BufferedReader(
                        new InputStreamReader(resources.getResource(path
                                + TextToSpeechRecorder.ResourcesFilename)));
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
