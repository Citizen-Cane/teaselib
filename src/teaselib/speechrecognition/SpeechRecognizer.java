/**
 * 
 */
package teaselib.speechrecognition;

import java.util.HashMap;
import java.util.Map;

/**
 * @author someone
 *
 */
public class SpeechRecognizer {

    private final Map<String, SpeechRecognition> speechRecognitionInstances = new HashMap<String, SpeechRecognition>();

    public SpeechRecognizer() {

    }

    public SpeechRecognition get(String locale) {
        if (speechRecognitionInstances.containsKey(locale)) {
            return speechRecognitionInstances.get(locale);
        } else {
            SpeechRecognition speechRecognition = new SpeechRecognition(locale);
            speechRecognitionInstances.put(locale, speechRecognition);
            return speechRecognition;
        }
    }

}
