package teaselib.core.speechrecognition;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import teaselib.core.Configuration;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognizer {
    private final Map<Locale, SpeechRecognition> speechRecognitionInstances = new HashMap<>();
    @SuppressWarnings("unused")
    private final Configuration config;

    public SpeechRecognizer(Configuration config) {
        this.config = config;
    }

    public SpeechRecognition get(Locale locale) {
        synchronized (speechRecognitionInstances) {
            if (speechRecognitionInstances.containsKey(locale)) {
                return speechRecognitionInstances.get(locale);
            } else {
                SpeechRecognition speechRecognition = new SpeechRecognition(locale);
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
            final Collection<SpeechRecognition> stoppedInstances = new HashSet<>();
            for (SpeechRecognition sR : speechRecognitionInstances.values()) {
                if (sR.isActive()) {
                    sR.stopRecognition();
                    stoppedInstances.add(sR);
                }
            }
            return () -> {
                for (SpeechRecognition sR : stoppedInstances) {
                    sR.resumeRecognition();
                }
            };
        }
    }

}