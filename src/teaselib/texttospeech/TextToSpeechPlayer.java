package teaselib.texttospeech;

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

import teaselib.Actor;
import teaselib.ResourceLoader;
import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;
import teaselib.text.Message;

public class TextToSpeechPlayer {

    public final ResourceLoader resources;
    public final TextToSpeech textToSpeech;

    private final VoicesProperties actorVoices;

    private final Map<String, Voice> voices;
    private final Map<String, String> actor2PrerecordedVoice = new HashMap<>();
    private final Map<String, Voice> actor2TTSVoice = new HashMap<>();

    private final Set<String> usedVoices = new HashSet<>();

    public TextToSpeechPlayer(ResourceLoader resources,
            TextToSpeech textToSpeech) {
        super();
        this.resources = resources;
        this.textToSpeech = textToSpeech;
        // TTS might not be available
        if (textToSpeech.isReady()) {
            this.voices = textToSpeech.getVoices();
        } else {
            this.voices = new HashMap<>();
        }
        // Read pre-recorded voices config
        actorVoices = new VoicesProperties(resources);
        for (Object value : actorVoices.keySet()) {
            // Available as a pre-recorded voice?
            String actorName = value.toString();
            String voiceGuid = actorVoices.getVoice(actorName);
            ActorVoice actorVoice = new ActorVoice(actorName, voiceGuid,
                    resources);
            boolean prerecorded = !actorVoice.empty();
            if (prerecorded) {
                actor2PrerecordedVoice.put(actorName, voiceGuid);
                usedVoices.add(voiceGuid);
                TeaseLib.log("Actor " + actorName
                        + ": using prerecorded voice '" + voiceGuid + "'");
            } else {
                TeaseLib.log("Actor " + actorName
                        + ": prerecorded voice not available");
                Voice voice = voices.get(voiceGuid);
                if (voice != null) {
                    TeaseLib.log("Actor " + actorName + ": using TTS voice");
                    actor2TTSVoice.put(actorName, voice);
                    usedVoices.add(voiceGuid);
                } else {
                    TeaseLib.log("Actor " + actorName + ": voice not available");
                }
            }
        }
    }

    public boolean hasPrerecordedVoices() {
        return actor2PrerecordedVoice.size() > 0;
    }

    public String getAssignedVoiceFor(Actor actor) {
        return actor2PrerecordedVoice.get(actor.name);
    }

    public Voice getVoiceFor(Actor actor) {
        if (actor2PrerecordedVoice.containsKey(actor.name)) {
            throw new IllegalStateException("Prerecorded voice available");
        }
        if (actor2TTSVoice.containsKey(actor.name)) {
            return actor2TTSVoice.get(actor.name);
        } else {
            return getMatchingOrBestVoiceFor(actor);
        }
    }

    private Voice getMatchingOrBestVoiceFor(Actor actor) {
        Map<String, Voice> voices = textToSpeech.getVoices();
        Voice voice = null;
        String guid = getAssignedVoiceFor(actor);
        if (guid != null) {
            voice = voices.get(guid);
        }
        if (voice == null && voices.size() > 0) {
            voice = getMatchingVoiceFor(actor, voices);
            if (voice != null) {
                actor2TTSVoice.put(actor.name, voice);
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
                TeaseLib.log("Using voice '" + voice.guid + "' with locale '"
                        + voice.locale + "' for actor '" + actor.name + "'");
                return voice;
            }
        }
        // Partial match: language only
        for (Voice voice : voices.values()) {
            String voiceLanguage = voice.locale.substring(2);
            String actorLanguage = voice.locale.substring(2);
            if (voiceLanguage.compareToIgnoreCase(actorLanguage) == 0
                    && !usedVoices.contains(voice.guid)) {
                TeaseLib.log("Using voice '" + voice.guid + "' with locale '"
                        + voice.locale + "' for actor '" + actor.name + "'");
                return voice;
            }
        }
        // Reuse voice of first non-dominant actor
        for (String actorName : actor2TTSVoice.keySet()) {
            if (actorName.compareToIgnoreCase(Actor.Dominant) != 0) {
                Voice voice = actor2TTSVoice.get(actorName);
                TeaseLib.log("Reusing voice of actor '" + actorName + "': '"
                        + voice.guid + "' with locale '" + voice.locale
                        + "' for actor '" + actor.name + "'");
                return voice;
            }
        }
        // voice of Dominant actor
        if (actor2TTSVoice.containsKey(Actor.Dominant)) {
            Voice voice = actor2TTSVoice.get(Actor.Dominant);
            TeaseLib.log("Reusing voice of dominant actor ': " + voice.guid
                    + "' with locale '" + voice.locale + "' for actor '"
                    + actor.name + "'");
            return voice;
        }
        // No voice
        TeaseLib.log("No voice defined for actor '" + actor.name + "'");
        return null;
    }

    /**
     * Retrieve speech resources for the message, selected actor voice
     * 
     * @param message
     * @return
     * @throws IOException
     */
    private List<String> getSpeechResources(Message message) throws IOException {
        String actorName = message.actor.name;
        String voice = actor2PrerecordedVoice.get(actorName);
        if (voice == null) {
            return null;
        } else {
            String path = TextToSpeechRecorder.SpeechDirName + "/" + actorName
                    + "/" + voice + "/" + TextToSpeechRecorder.getHash(message)
                    + "/";
            BufferedReader reader = null;
            List<String> speechResources = new Vector<>();
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
    public Iterator<String> selectVoice(Message message) throws IOException {
        List<String> prerenderedSpeechFiles = getSpeechResources(message);
        final Iterator<String> prerenderedSpeech;
        if (prerenderedSpeechFiles != null) {
            prerenderedSpeech = prerenderedSpeechFiles.iterator();
            return prerenderedSpeech;
        } else {
            prerenderedSpeech = null;
        }
        // Use TTS voice
        if (textToSpeech.isReady()) {
            // No, setup voice
            Voice voice = getVoiceFor(message.actor);
            textToSpeech.setVoice(voice);
        }
        return null;
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
    public void speak(String prompt, Iterator<String> prerecorded,
            TeaseLib teaseLib) throws IOException {
        final String path;
        if (prerecorded != null) {
            path = prerecorded.hasNext() ? prerecorded.next() : null;
        } else {
            path = null;
        }
        if (path != null) {
            teaseLib.host.playSound(resources.getAssetsPath(path)
                    .getAbsolutePath(), teaseLib.resources.getResource(path));
        } else if (textToSpeech.isReady()) {
            try {
                textToSpeech.speak(prompt);
            } catch (ScriptInterruptedException e) {
                throw e;
            } catch (Throwable t) {
                TeaseLib.log(this, t);
                speakSilent(prompt, teaseLib);
            }
        } else {
            speakSilent(prompt, teaseLib);
        }
    }

    private void speakSilent(String prompt, TeaseLib teaseLib) {
        // Unable to speak, just display the estimated duration
        long duration = TextToSpeech.getEstimatedSpeechDuration(prompt);
        teaseLib.host.sleep(duration);
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }
}
