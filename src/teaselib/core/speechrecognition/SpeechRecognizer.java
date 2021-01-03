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
import teaselib.core.speechrecognition.sapi.TeaseLibSRGS;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognizer implements Closeable {
    private final Map<Locale, SpeechRecognition> speechRecognitionInstances = new HashMap<>();
    public final AudioSync audioSync;

    public enum Config {
        Implementation;
    }

    private Class<? extends SpeechRecognitionNativeImplementation> srClass;

    public SpeechRecognizer(Configuration config) {
        this(config, new AudioSync());
    }

    public SpeechRecognizer(Configuration config, AudioSync audioSync) {
        this.audioSync = audioSync;
        if (Boolean.parseBoolean(config.get(teaselib.Config.InputMethod.SpeechRecognition))) {
            if (config.has(Config.Implementation)) {
                String className = config.get(Config.Implementation);
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends SpeechRecognitionNativeImplementation> implementationClass = //
                            (Class<? extends SpeechRecognitionNativeImplementation>) Class.forName(className);
                    this.srClass = implementationClass;
                } catch (ClassNotFoundException e) {
                    throw new NoSuchElementException(Config.Implementation + ": " + className);
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
            final Collection<SpeechRecognition> pausedInstances = new HashSet<>();
            for (SpeechRecognition sR : speechRecognitionInstances.values()) {
                if (sR.isActive()) {
                    sR.pauseRecognition();
                    pausedInstances.add(sR);
                }
            }
            return () -> {
                for (SpeechRecognition sR : pausedInstances) {
                    sR.resumeRecognition();
                }
            };
        }
    }

}