package teaselib.core.texttospeech;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.events.Delegate;
import teaselib.core.events.DelegateThread;
import teaselib.core.texttospeech.implementation.TeaseLibTTS;
import teaselib.core.texttospeech.implementation.TextToSpeechImplementationDebugProxy;
import teaselib.core.util.ExceptionUtil;

public class TextToSpeech {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeech.class);

    private TextToSpeechImplementation tts;

    private DelegateThread delegateThread = new DelegateThread("Speech Recognition dispatcher thread");

    private String[] NoHints = null;

    private static void ttsEngineNotInitialized() {
        throw new IllegalStateException("TTS engine not initialized");
    }

    public static final Lock AudioOutput = new ReentrantLock();

    public static final Set<String> BlackList = new HashSet<String>(Arrays.asList("LTTS7Ludoviko", "MSMary", "MSMike"));

    public TextToSpeech() {
        Set<String> names = getImplementations();
        if (!names.isEmpty()) {
            setImplementation(names.iterator().next());
        }
    }

    public TextToSpeech(TextToSpeechImplementation ttsImpl) {
        this.tts = ttsImpl;
    }

    private static Set<String> getImplementations() {
        Set<String> names = new HashSet<String>();
        names.add(TeaseLibTTS.class.getName());
        return names;
    }

    private void setImplementation(final String className) {
        // Create class here
        if (tts != null && tts.getClass().getName().equals(className)) {
            return;
        }
        try {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    try {
                        Class<? extends Object> ttsClass = getClass().getClassLoader().loadClass(className);
                        Method singleton = ttsClass.getDeclaredMethod("getInstance");

                        TextToSpeechImplementation newTTS = null;
                        if (singleton != null) {
                            newTTS = (TextToSpeechImplementation) singleton.invoke(this);
                        } else {
                            newTTS = (TextToSpeechImplementation) ttsClass.newInstance();
                        }

                        if (tts != null) {
                            tts.dispose();
                        }

                        tts = new TextToSpeechImplementationDebugProxy(newTTS);
                    } catch (Throwable t) {
                        setError(t);
                    }
                }
            };
            delegateThread.run(delegate);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    /**
     * @return Whether TextToSpeech is ready to render speech
     */
    public boolean isReady() {
        return tts != null;
    }

    public Map<String, Voice> getVoices() {
        final Map<String, Voice> voices = new LinkedHashMap<String, Voice>();

        if (tts != null) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    tts.getVoices(voices);
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        } else {
            ttsEngineNotInitialized();
        }

        removeBlackListed(voices);
        return voices;
    }

    /**
     * @param voices
     */
    private static void removeBlackListed(Map<String, Voice> voices) {
        for (String blackListed : BlackList) {
            voices.remove(blackListed);
        }
    }

    /**
     * Set a specific voice, or null for system default voice
     * 
     * @param voice
     */
    public void setVoice(final Voice voice) {
        if (tts != null) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    tts.setVoice(voice);
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        } else {
            ttsEngineNotInitialized();
        }
    }

    /**
     * Set hints for the next call to speak(...). Hints are cleared after each call to speak.
     * 
     * @param hints
     *            The hints to consider for the next call to speak(..)
     */
    public void setHint(final String... hints) {
        if (tts != null) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    tts.setHints(hints);
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        } else {
            ttsEngineNotInitialized();
        }
    }

    public void speak(final String prompt) throws InterruptedException {
        if (tts != null) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    synchronized (AudioOutput) {
                        try {
                            tts.speak(prompt);
                        } finally {
                            tts.setHints(NoHints);
                        }
                    }
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
            }
        } else

        {
            ttsEngineNotInitialized();
        }
    }

    public String speak(final String prompt, final File file) {
        final StringBuilder soundFilePath = new StringBuilder();
        if (tts != null) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    try {
                        String actualPath = tts.speak(prompt, file.getAbsolutePath());
                        soundFilePath.append(actualPath);
                    } finally {
                        tts.setHints(NoHints);
                    }
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            } catch (Exception e) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
            }
        } else {
            ttsEngineNotInitialized();
        }
        return soundFilePath.toString();
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        } else {
            ttsEngineNotInitialized();
        }
    }

    /**
     * Estimate the duration for displaying the text when not spoken by speech synthesis.
     * 
     * @param text
     *            Text to estimate the time needed to speak for
     * @return duration, in milliseconds
     */
    public static long getEstimatedSpeechDuration(String text) {
        long millisecondsPerLetter = 70;
        long pauseAfterParagraph = 1 * 1000L;
        return text.length() * millisecondsPerLetter + pauseAfterParagraph;
    }

    public void initPhoneticDictionary(PronunciationDictionary pronunciationDictionary) throws IOException {
        Map<String, Map<String, String>> phonemes = pronunciationDictionary.pronunciations(tts.sdkName(),
                tts.phonemeAlphabetName());
        for (Entry<String, Map<String, String>> entry : phonemes.entrySet()) {
            String locale = entry.getKey();
            Map<String, String> locale2Dictionary = entry.getValue();
            for (Entry<String, String> dictionary : locale2Dictionary.entrySet()) {
                String word = dictionary.getKey();
                String pronunciation = dictionary.getValue();
                // TODO Define part of speech in phoneme dictionary instead of setting all flags
                int partOfSpeech = TextToSpeechImplementation.SPPS_Noun | TextToSpeechImplementation.SPPS_Verb
                        | TextToSpeechImplementation.SPPS_Modifier | TextToSpeechImplementation.SPPS_Function
                        | TextToSpeechImplementation.SPPS_Interjection;
                tts.addLexiconEntry(locale, word, partOfSpeech, pronunciation);
            }
        }
    }
}
