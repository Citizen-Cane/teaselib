package teaselib.core.texttospeech;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import teaselib.Actor;
import teaselib.Message;
import teaselib.TeaseLib;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognizer;

public class TextToSpeechPlayer {

    public final TextToSpeech textToSpeech;

    private final TeaseLib teaseLib;
    private final Set<ResourceLoader> processedVoiceActorVoices = new HashSet<ResourceLoader>();
    private final Map<String, Voice> voices;
    private final Map<String, String> actor2PrerecordedVoice = new HashMap<String, String>();
    private final Map<String, Voice> actor2TTSVoice = new HashMap<String, Voice>();
    private final Set<String> usedVoices = new HashSet<String>();

    private Voice voice = null;

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
            this.voices = textToSpeech.getVoices();
        } else {
            this.voices = new HashMap<String, Voice>();
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
     * in the resources of the script of the actor.
     * 
     * As a result, this has to be called for each resource instance.
     * 
     * @param resources
     *            The resource object that contains the assignments and/or
     *            pre-recorded speech.
     */
    private void getActorVoices(ResourceLoader resources) {
        // Have we read any voice-related configuration files from this resource
        // loader yet?
        if (!processedVoiceActorVoices.contains(resources)) {
            processedVoiceActorVoices.add(resources);
            // Get the list of actor to voice assignments
            ActorVoices actorVoices = new ActorVoices(resources);
            for (String actorName : actorVoices.keySet()) {
                // Available as a pre-recorded voice?
                String voiceGuid = actorVoices.getGuid(actorName);
                PreRecordedVoice preRecordedVoice = new PreRecordedVoice(
                        actorName, voiceGuid, resources);
                if (preRecordedVoice.available()) {
                    actor2PrerecordedVoice.put(actorName, voiceGuid);
                    usedVoices.add(voiceGuid);
                    teaseLib.log.info("Actor " + actorName
                            + ": using prerecorded voice '" + voiceGuid + "'");
                } else {
                    teaseLib.log.info("Actor " + actorName
                            + ": prerecorded voice not available");
                    Voice voice = voices.get(voiceGuid);
                    if (voice != null) {
                        teaseLib.log.info("Actor " + actorName
                                + ": using TTS voice");
                        actor2TTSVoice.put(actorName, voice);
                        usedVoices.add(voiceGuid);
                    } else {
                        teaseLib.log.info("Actor " + actorName
                                + ": voice not available");
                    }
                }
            }
        }
    }

    Voice getVoiceFor(Actor actor, ResourceLoader resources) {
        getActorVoices(resources);
        if (actor2PrerecordedVoice.containsKey(actor.key)) {
            throw new IllegalStateException("Prerecorded voice available");
        }
        if (actor2TTSVoice.containsKey(actor.key)) {
            return actor2TTSVoice.get(actor.key);
        } else {
            return getMatchingOrBestVoiceFor(actor, resources);
        }
    }

    String getAssignedVoiceFor(Actor actor, ResourceLoader resources) {
        getActorVoices(resources);
        return actor2PrerecordedVoice.get(actor.key);
    }

    private Voice getMatchingOrBestVoiceFor(Actor actor,
            ResourceLoader resources) {
        Map<String, Voice> voices = textToSpeech.getVoices();
        Voice voice = null;
        String guid = getAssignedVoiceFor(actor, resources);
        if (guid != null) {
            voice = voices.get(guid);
        }
        if (voice == null && voices.size() > 0) {
            voice = getMatchingVoiceFor(actor, voices);
            if (voice != null) {
                actor2TTSVoice.put(actor.key, voice);
                usedVoices.add(voice.guid);
            }
        }
        return voice;
    }

    private Voice getMatchingVoiceFor(Actor actor, Map<String, Voice> voices) {
        // Full match: language and region
        for (Voice voice : voices.values()) {
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
        for (Voice voice : voices.values()) {
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
        for (String actorName : actor2TTSVoice.keySet()) {
            if (actorName.compareToIgnoreCase(Actor.Dominant) != 0) {
                Voice voice = actor2TTSVoice.get(actorName);
                teaseLib.log.info("Reusing voice of actor '" + actorName
                        + "': '" + voice.guid + "' with locale '"
                        + voice.locale + "' for actor '" + actor.key + "'");
                return voice;
            }
        }
        // voice of Dominant actor
        if (actor2TTSVoice.containsKey(Actor.Dominant)) {
            Voice voice = actor2TTSVoice.get(Actor.Dominant);
            teaseLib.log.info("Reusing voice of dominant actor ': " + voice.guid
                    + "' with locale '" + voice.locale + "' for actor '"
                    + actor.key + "'");
            return voice;
        }
        // No voice
        teaseLib.log.info("No voice defined for actor '" + actor.key + "'");
        return null;
    }

    /**
     * Retrieve speech resources for the message, selected actor voice
     * 
     * @param message
     * @return
     * @throws IOException
     */
    private List<String> getSpeechResources(ResourceLoader resources,
            Message message) throws IOException {
        String actorKey = message.actor.key;
        String voice = actor2PrerecordedVoice.get(actorKey);
        if (voice == null) {
            return null;
        } else {
            String path = TextToSpeechRecorder.SpeechDirName + "/" + actorKey
                    + "/" + voice + "/" + TextToSpeechRecorder.getHash(message)
                    + "/";
            BufferedReader reader = null;
            List<String> speechResources = new Vector<String>();
            try {
                reader = new BufferedReader(new InputStreamReader(
                        resources.getResource(path
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

    /**
     * Select the voice for speaking the message, either prerecorded or TTS
     * 
     * @param message
     * @return
     * @throws IOException
     */
    public Iterator<String> selectVoice(ResourceLoader resources,
            Message message) throws IOException {
        getActorVoices(resources);
        // Do we have a pre-recorded voice?
        List<String> prerenderedSpeechFiles = getSpeechResources(resources,
                message);
        final Iterator<String> prerenderedSpeech;
        if (prerenderedSpeechFiles != null) {
            prerenderedSpeech = prerenderedSpeechFiles.iterator();
            voice = null;
        } else {
            prerenderedSpeech = null;
            // Use TTS voice
            if (textToSpeech.isReady()) {
                voice = getVoiceFor(message.actor, resources);
                textToSpeech.setVoice(voice);
            }
        }
        return prerenderedSpeech;
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
        boolean useTTS = textToSpeech.isReady() && voice != null;
        final boolean reactivateSpeechRecognition;
        final SpeechRecognition speechRecognizer = SpeechRecognizer.instance
                .get(actor.locale);
        // Suspend speech recognition while speaking,
        // to avoid wrong recognitions
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

    public void play(ResourceLoader resources, Actor actor, String prompt,
            Iterator<String> prerecorded) throws IOException {
        final String path = prerecorded.hasNext() ? prerecorded.next() : null;
        boolean usePrerecorded = path != null;
        final boolean reactivateSpeechRecognition;
        final SpeechRecognition speechRecognizer = SpeechRecognizer.instance
                .get(actor.locale);
        // Suspend speech recognition while speaking,
        // to avoid wrong recognitions
        // - and the mistress speech isn't to be interrupted anyway
        if (usePrerecorded) {
            SpeechRecognition.completeSpeechRecognitionInProgress();
            reactivateSpeechRecognition = speechRecognizer.isActive();
        } else {
            reactivateSpeechRecognition = false;
        }
        try {
            if (reactivateSpeechRecognition && speechRecognizer != null) {
                speechRecognizer.stopRecognition();
            }
            if (usePrerecorded) {
                teaseLib.host.playSound(resources, path);
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

    private void speakSilent(String prompt) {
        // Unable to speak, just display the estimated duration
        long duration = TextToSpeech.getEstimatedSpeechDuration(prompt);
        teaseLib.sleep(duration, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (textToSpeech != null && voice != null) {
            textToSpeech.stop();
        }
    }
}
