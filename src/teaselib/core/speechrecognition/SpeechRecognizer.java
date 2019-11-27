package teaselib.core.speechrecognition;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import teaselib.Config;
import teaselib.core.Closeable;
import teaselib.core.configuration.Configuration;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognizer implements Closeable {
    private final Map<Locale, SpeechRecognition> speechRecognitionInstances = new HashMap<>();

    private final Configuration config;

    public SpeechRecognizer(Configuration config) {
        this.config = config;
    }

    public SpeechRecognition get(Locale locale) {
        synchronized (speechRecognitionInstances) {
            if (speechRecognitionInstances.containsKey(locale)) {
                return speechRecognitionInstances.get(locale);
            } else {
                if (Boolean.parseBoolean(config.get(Config.InputMethod.SpeechRecognition))) {
                    SpeechRecognition speechRecognition = new SpeechRecognition(locale);
                    speechRecognitionInstances.put(locale, speechRecognition);
                    return speechRecognition;
                } else {
                    SpeechRecognition none = new SpeechRecognition();
                    speechRecognitionInstances.put(locale, none);
                    return none;
                }
            }
        }
    }

    @Override
    public void close() {
        for (Entry<Locale, SpeechRecognition> entry : speechRecognitionInstances.entrySet()) {
            entry.getValue().close();
        }
        speechRecognitionInstances.clear();
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
                    sR.pauseRecognition();
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