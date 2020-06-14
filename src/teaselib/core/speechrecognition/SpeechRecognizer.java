package teaselib.core.speechrecognition;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import teaselib.core.AudioSync;
import teaselib.core.Closeable;
import teaselib.core.configuration.Configuration;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.speechrecognition.implementation.Unsupported;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognizer implements Closeable {
    private final Map<Locale, SpeechRecognition> speechRecognitionInstances = new HashMap<>();
    private final AudioSync audioSync;

    enum Config {
        SpeechRecognitionImplementation;
    }

    private Class<? extends SpeechRecognitionImplementation> srClass;

    public SpeechRecognizer(Configuration config) {
        this(config, new AudioSync());
    }

    @SuppressWarnings("unchecked")
    public SpeechRecognizer(Configuration config, AudioSync audioSync) {
        this.audioSync = audioSync;
        if (Boolean.parseBoolean(config.get(teaselib.Config.InputMethod.SpeechRecognition))) {
            if (config.has(Config.SpeechRecognitionImplementation)) {
                String className = config.get(Config.SpeechRecognitionImplementation);
                try {
                    this.srClass = (Class<? extends SpeechRecognitionImplementation>) Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new NoSuchElementException(Config.SpeechRecognitionImplementation + ": " + className);
                }
            } else {
                this.srClass = TeaseLibSRGS.class;
            }
        } else {
            this.srClass = Unsupported.class;
        }
    }

    public SpeechRecognition get(Locale locale) {
        synchronized (speechRecognitionInstances) {
            if (speechRecognitionInstances.containsKey(locale)) {
                SpeechRecognition speechRecognition = speechRecognitionInstances.get(locale);
                if (speechRecognition.implementation.getClass() != srClass) {
                    throw new UnsupportedOperationException("SR implementation already set for locale " + locale);
                }
                return speechRecognition;
            } else {
                SpeechRecognition speechRecognition = new SpeechRecognition(locale, srClass, audioSync);
                speechRecognitionInstances.put(locale, speechRecognition);
                return speechRecognition;
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
                    if (sR.isActive()) {
                        sR.resumeRecognition();
                    }
                }
            };
        }
    }

}