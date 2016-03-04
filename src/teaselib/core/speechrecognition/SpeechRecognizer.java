/**
 * 
 */
package teaselib.core.speechrecognition;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
        synchronized (speechRecognitionInstances) {
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

    /**
     * Disables all running speech recognition instances (if any).
     * 
     * @return A runnable for resuming speech recognition.
     */
    public Runnable pauseRecognition() {
        synchronized (speechRecognitionInstances) {
            final Collection<SpeechRecognition> stoppedInstances = new HashSet<SpeechRecognition>();
            for (SpeechRecognition sR : speechRecognitionInstances.values()) {
                if (sR.isActive()) {
                    sR.stopRecognition();
                    stoppedInstances.add(sR);
                }
            }
            return new Runnable() {
                @Override
                public void run() {
                    for (SpeechRecognition sR : stoppedInstances) {
                        sR.resumeRecognition();
                    }
                }
            };
        }
    }

}