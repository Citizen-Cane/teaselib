package teaselib.texttospeech;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import teaselib.ResourceLoader;
import teaselib.TeaseLib;
import teaselib.text.Message;

public class TextToSpeechPlayer {

	public final ResourceLoader resources;
	public final TextToSpeech textToSpeech;

	/**
	 * Character -> voice guid
	 */
	private final Map<String, String> characterToRecordedVoice = new HashMap<>();

	public TextToSpeechPlayer(ResourceLoader resources,
			TextToSpeech textToSpeech) {
		super();
		this.resources = resources;
		this.textToSpeech = textToSpeech;
		// Read pre-recorded voices config
		InputStream recordedVoicesConfig;
		String path = TextToSpeechRecorder.RecordedVoicesFilename;
		try {
			recordedVoicesConfig = resources.getResource(path);
		} catch (IOException e1) {
			recordedVoicesConfig = null;
			TeaseLib.logDetail("No prerecorded voices config: " + path);
		}
		if (recordedVoicesConfig != null) {
			Properties record = new Properties();
			try {
				record.load(recordedVoicesConfig);
			} catch (Exception e) {
				// No file or no voices defined, ok
			}
			for (Object value : record.keySet()) {
				String guid = record.get(value.toString()).toString();
				characterToRecordedVoice.put(value.toString(), guid);
			}
		}
	}

	public boolean hasPrerecordedVoices() {
		return characterToRecordedVoice.size() > 0;
	}

	public String getVoiceFor(String character) {
		return characterToRecordedVoice.get(character);
	}

	public Voice getMatchingOrBestVoiceFor(String character) {
		// TODO Select best voice based on language and region, assign different
		// voices to different characters
		// - Create common property file format
		// - add language and region to property files
		String guid = getVoiceFor(character);
		Map<String, Voice> voices = textToSpeech.getVoices();
		Voice voice = voices.get(guid);
		if (voice == null && voices.size() > 0) {
			voice = getMatchingVoiceFor(character, voices);
		}
		return voice;
	}

	private Voice getMatchingVoiceFor(String character,
			Map<String, Voice> voices) {
		Voice voice;
		voice = voices.values().iterator().next();
		TeaseLib.log("No voice defined for character '" + character
				+ "'. Defaulting to voice " + voice.guid);
		return voice;
	}

	public List<String> getSpeechResources(Message message) throws IOException {
		String character = message.getCharacter();
		String voice = characterToRecordedVoice.get(character);
		if (voice == null) {
			TeaseLib.log("Character " + character
					+ ": prerecorded voice not found");
			return null;
		} else {
			String path = TextToSpeechRecorder.SpeechDirName + "/" + character
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
	 * Speak or wait the estimated duration it takes to speak the prompt
	 * 
	 * @param prompt
	 *            What to speak
	 * @param completionSync
	 *            Object to wait for during pauses
	 * @param teaseLib
	 *            instance to call sleep on
	 */
	public void speak(String prompt, String prerecorded, Object completionSync,
			TeaseLib teaseLib) throws IOException {
		if (prerecorded != null) {
			teaseLib.host.playSound(resources.getAssetsPath(prerecorded)
					.getAbsolutePath(), teaseLib.resources
					.getResource(prerecorded));
		} else if (textToSpeech.isReady()) {
			// TTS
			try {
				textToSpeech.speak(prompt);
			} catch (Throwable t) {
				TeaseLib.log(this, t);
				speakSilent(prompt, completionSync, teaseLib);
			}
		} else {
			speakSilent(prompt, completionSync, teaseLib);
		}
	}

	private void speakSilent(String prompt, Object completionSync,
			TeaseLib teaseLib) {
		// Unable to speak, just display the estimated duration
		long duration = TextToSpeech.getEstimatedSpeechDuration(prompt);
		synchronized (completionSync) {
			teaseLib.host.sleep(duration);
		}
	}
}
