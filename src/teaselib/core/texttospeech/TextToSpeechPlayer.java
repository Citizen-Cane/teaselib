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
import java.util.concurrent.TimeUnit;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.TeaseLib;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognizer;

public class TextToSpeechPlayer {
    public final TextToSpeech textToSpeech;
    private final TeaseLib teaseLib;
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
     * Actor key to TTS voice
     */
    private final Map<String, Voice> actorKey2TTSVoice = new HashMap<String, Voice>();

    /**
     * voice guid
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
        super();
        this.teaseLib = TeaseLib.instance();
        this.textToSpeech = new TextToSpeech();
        // TTS might not be available
        if (textToSpeech.isReady()) {
            voices = textToSpeech.getVoices();
        } else {
            voices = new HashMap<String, Voice>();
        }
        // Write list of installed voices to log file in order to provide data
        // for the Actor to Voices mapping properties file
        final InstalledVoices installedVoices = new InstalledVoices(voices);
        teaseLib.log.info("Installed voices:");
        for (String key : installedVoices.keySet()) {
            teaseLib.log.info(key + ".guid=" + installedVoices.getGuid(key));
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
    public void loadActorVoices(ResourceLoader resources) {
        // Have we read any voice-related configuration files from this resource
        // loader yet?
        if (!processedVoiceActorVoices.contains(resources)) {
            processedVoiceActorVoices.add(resources);
            // Get the list of actor to voice assignments
            ActorVoices actorVoices = new ActorVoices(resources);
            for (String actorKey : actorVoices.keySet()) {
                // Available as a pre-recorded voice?
                String voiceGuid = actorVoices.getGuid(actorKey);
                PreRecordedVoice preRecordedVoice = new PreRecordedVoice(
                        actorKey, voiceGuid, resources);
                // Only if actor isn't assigned yet - when called from other
                // script
                if (!actorKey2TTSVoice.containsKey(actorKey)
                        && preRecordedVoice.available()) {
                    actorKey2PrerecordedVoiceGuid.put(actorKey, voiceGuid);
                    usedVoices.add(voiceGuid);
                    teaseLib.log.info("Actor " + actorKey
                            + ": using prerecorded voice '" + voiceGuid + "'");
                } else {
                    teaseLib.log.info("Actor " + actorKey
                            + ": prerecorded voice not available");
                    Voice voice = voices.get(voiceGuid);
                    if (voice != null) {
                        teaseLib.log.info(
                                "Actor " + actorKey + ": using TTS voice");
                        actorKey2TTSVoice.put(actorKey, voice);
                        usedVoices.add(voiceGuid);
                    } else {
                        teaseLib.log.info(
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
            String locale = voice.locale;
            if (locale.compareToIgnoreCase(actor.locale) == 0
                    && !usedVoices.contains(voice.guid)) {
                teaseLib.log.info("Using voice '" + voice.guid
                        + "' with locale '" + voice.locale + "' for actor '"
                        + actor.key + "'");
                return voice;
            }
        }
        // Partial match: language only
        for (Voice voice : genderFilteredVoices) {
            String voiceLanguage = voice.locale.substring(0, 2);
            String actorLanguage = actor.locale.substring(0, 2);
            if (voiceLanguage.compareToIgnoreCase(actorLanguage) == 0
                    && !usedVoices.contains(voice.guid)) {
                teaseLib.log.info("Using voice '" + voice.guid
                        + "' with locale '" + voice.locale + "' for actor '"
                        + actor.key + "'");
                return voice;
            }
        }
        // Reuse voice of first non-dominant actor
        for (String actorName : actorKey2TTSVoice.keySet()) {
            if (actorName.compareToIgnoreCase(Actor.Dominant) != 0
                    && actorKey2TTSVoice
                            .get(actorName).gender == actor.gender) {
                Voice voice = actorKey2TTSVoice.get(actorName);
                teaseLib.log.info("Reusing voice of actor '" + actorName
                        + "': '" + voice.guid + "' with locale '" + voice.locale
                        + "' for actor '" + actor.key + "'");
                return voice;
            }
        }
        // voice of default dominant actor
        for (String actorName : actorKey2TTSVoice.keySet()) {
            if (actorName.compareToIgnoreCase(Actor.Dominant) == 0
                    && actorKey2TTSVoice
                            .get(actorName).gender == actor.gender) {
                Voice voice = actorKey2TTSVoice.get(actorName);
                teaseLib.log.info("Reusing voice of actor '" + actorName
                        + "': '" + voice.guid + "' with locale '" + voice.locale
                        + "' for actor '" + actor.key + "'");
                return voice;
            }
        }
        // No voice
        teaseLib.log.info("No voice available for actor '" + actor.key + "'");
        return null;
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
    public void speak(Actor actor, String prompt, String mood) {
        boolean useTTS = textToSpeech.isReady();
        final boolean reactivateSpeechRecognition;
        final SpeechRecognition speechRecognizer = SpeechRecognizer.instance
                .get(actor.locale);
        // Suspend speech recognition while speaking,
        // to avoid accidental recognitions
        // - and the mistress speech isn't to be interrupted anyway
        if (useTTS) {
            SpeechRecognition.completeSpeechRecognitionInProgress();
            reactivateSpeechRecognition = speechRecognizer.isActive();
        } else {
            reactivateSpeechRecognition = false;
        }
        try {
            if (reactivateSpeechRecognition && speechRecognizer != null) {
                speechRecognizer.stopRecognition();
            }
            if (useTTS) {
                Voice voice = getVoiceFor(actor);
                // Synchronize speaking, only one actor can speak at a time
                textToSpeech.setVoice(voice);
                textToSpeech.setHint(mood);
                try {
                    textToSpeech.speak(prompt);
                } catch (ScriptInterruptedException e) {
                    throw e;
                } catch (Throwable t) {
                    teaseLib.log.error(this, t);
                    speakSilent(prompt);
                }
            } else {
                speakSilent(prompt);
            }
        } finally {
            // resume SR if necessary
            if (reactivateSpeechRecognition && speechRecognizer != null) {
                speechRecognizer.resumeRecognition();
            }
        }
    }

    public void stop() {
        textToSpeech.stop();
    }

    private void speakSilent(String prompt) {
        // Unable to speak, just display the estimated duration
        long duration = TextToSpeech.getEstimatedSpeechDuration(prompt);
        teaseLib.sleep(duration, TimeUnit.MILLISECONDS);
    }

    public boolean prerenderedSpeechAvailable(Actor actor) {
        return actorKey2PrerecordedVoiceGuid.containsKey(actor.key);
    }

    public Message getPrerenderedMessage(Message message,
            ResourceLoader resources) throws IOException {
        // Do we have a pre-recorded voice?
        Iterator<String> prerenderedSpeechFiles = getSpeechResources(message,
                resources).iterator();
        Message preRenderedSpeechMessage = new Message(message.actor);
        for (Part part : message.getParts()) {
            if (part.type == Message.Type.Text) {
                preRenderedSpeechMessage.add(part);
                preRenderedSpeechMessage.add(Message.Type.Speech,
                        prerenderedSpeechFiles.next());
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
    private List<String> getSpeechResources(Message message,
            ResourceLoader resources) throws IOException {
        String key = message.actor.key;
        String voice = actorKey2PrerecordedVoiceGuid.get(key);
        if (voice == null) {
            return null;
        } else {
            String path = TextToSpeechRecorder.SpeechDirName + "/" + key + "/"
                    + voice + "/" + TextToSpeechRecorder.getHash(message) + "/";
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
