package teaselib.core.speechrecognition;

import static java.lang.Class.*;
import static teaselib.core.speechrecognition.SpeechRecognitionNativeImplementation.*;

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
import teaselib.core.util.ReflectionUtils;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognizer implements Closeable {

    public final AudioSync audioSync;

    public enum Config {
        Default,
        Locale
    }

    private final Configuration config;
    private final Map<Locale, SpeechRecognition> speechRecognitionInstances = new HashMap<>();

    public SpeechRecognizer(Configuration config) {
        this(config, new AudioSync());
    }

    public SpeechRecognizer(Configuration config, AudioSync audioSync) {
        this.config = config;
        this.audioSync = audioSync;
    }

    private static Class<? extends SpeechRecognitionNativeImplementation> defaultImplemntation(Configuration config) {
        Class<? extends SpeechRecognitionNativeImplementation> defaultImplementation;
        if (Boolean.parseBoolean(config.get(teaselib.Config.InputMethod.SpeechRecognition))) {
            if (config.has(Config.Default)) {
                String className = config.get(Config.Default);
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends SpeechRecognitionNativeImplementation> implementationClass = //
                            (Class<? extends SpeechRecognitionNativeImplementation>) forName(className);
                    defaultImplementation = implementationClass;
                } catch (ClassNotFoundException e) {
                    throw new NoSuchElementException(Config.Default + ": " + className);
                }
            } else {
                defaultImplementation = TeaseLibSRGS.Relaxed.class;
            }
        } else {
            defaultImplementation = Unsupported.class;
        }
        return defaultImplementation;
    }

    public SpeechRecognition get(Locale locale) {
        synchronized (speechRecognitionInstances) {
            if (speechRecognitionInstances.containsKey(locale)) {
                return speechRecognitionInstances.get(locale);
            } else {
                Class<? extends SpeechRecognitionNativeImplementation> implementationClass;
                implementationClass = implementationClass(locale);
                SpeechRecognition instance = new SpeechRecognition(locale, implementationClass, audioSync);
                speechRecognitionInstances.put(locale, instance);
                return instance;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends SpeechRecognitionNativeImplementation> implementationClass(Locale locale) {
        Class<? extends SpeechRecognitionNativeImplementation> implementationClass;
        if (Boolean.parseBoolean(config.get(teaselib.Config.InputMethod.SpeechRecognition))) {
            String setting = ReflectionUtils.qualified(Config.Locale, languageCode(locale));
            if (!config.has(setting)) {
                setting = ReflectionUtils.qualified(Config.Locale, locale.getLanguage());
            }
            if (config.has(setting)) {
                String className = config.get(setting);
                try {
                    implementationClass = (Class<? extends SpeechRecognitionNativeImplementation>) forName(className);
                } catch (ClassNotFoundException e) {
                    implementationClass = defaultImplemntation(config);
                }
            } else {
                implementationClass = defaultImplemntation(config);
            }
        } else {
            implementationClass = defaultImplemntation(config);
        }
        return implementationClass;
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