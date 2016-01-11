/**
 * 
 */
package teaselib.core.speechrecognition;

import java.util.HashMap;
import java.util.Map;

/**
 * @author someone
 *
 */
public class SpeechRecognizer {

    public static SpeechRecognizer instance = new SpeechRecognizer();

    private final Map<String, SpeechRecognition> speechRecognitionInstances = new HashMap<String, SpeechRecognition>();

    private SpeechRecognizer() {
    }

    public SpeechRecognition get(String locale) {
        synchronized (instance) {
            if (speechRecognitionInstances.containsKey(locale)) {
                return speechRecognitionInstances.get(locale);
            } else {
                SpeechRecognition speechRecognition = new SpeechRecognition(
                        locale);
                speechRecognitionInstances.put(locale, speechRecognition);
                return speechRecognition;
            }
        }
    }

}
