package teaselib.core.texttospeech;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.events.Delegate;
import teaselib.core.events.DelegateThread;
import teaselib.core.texttospeech.implementation.TeaseLibTTS;

public class TextToSpeech {
    private static final Logger logger = LoggerFactory
            .getLogger(TextToSpeech.class);

    private TextToSpeechImplementation tts;

    private DelegateThread delegateThread = new DelegateThread(
            "Speech Recognition dispatcher thread");

    private String[] NoHints = null;

    private static void ttsEngineNotInitialized() {
        throw new IllegalStateException("TTS engine not initialized");
    }

    public static final Lock AudioOutput = new ReentrantLock();

    public TextToSpeech() {
        Set<String> names = getImplementations();
        if (!names.isEmpty()) {
            // Just use the first (as there will be usually just one per
            // platform)
            setImplementation(names.iterator().next());
        }
    }

    static private Set<String> getImplementations() {
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
                    TextToSpeechImplementation currentTTS = tts;
                    TextToSpeechImplementation newTTS = null;
                    try {
                        Class<? extends Object> ttsClass = getClass()
                                .getClassLoader().loadClass(className);
                        newTTS = (TextToSpeechImplementation) ttsClass
                                .newInstance();
                        // tts = new TeaseLibTTS();
                    } catch (Throwable t) {
                        setError(t);
                    } finally {
                        tts = newTTS;
                        if (currentTTS != null) {
                            currentTTS.dispose();
                            currentTTS = null;
                        }
                    }
                }
            };
            delegateThread.run(delegate);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
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
        final Map<String, Voice> voices = new HashMap<String, Voice>();
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
                throw new ScriptInterruptedException();
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        } else {
            ttsEngineNotInitialized();
        }
        return voices;
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
                throw new ScriptInterruptedException();
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        } else {
            ttsEngineNotInitialized();
        }
    }

    /**
     * Set hints for the next call to speak(...). Hints are cleared after each
     * call to speak.
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
                throw new ScriptInterruptedException();
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
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
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        } else {
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
                        String actualPath = tts.speak(prompt,
                                file.getAbsolutePath());
                        soundFilePath.append(actualPath);
                    } finally {
                        tts.setHints(NoHints);
                    }
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
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
     * Estimate the duration for displaying the text when not spoken by speech
     * synthesis.
     * 
     * @param text
     *            Text to estimate the time needed to speak for
     * @return duration, in milliseconds
     */
    public static long getEstimatedSpeechDuration(String text) {
        long millisecondsPerLetter = 70;
        long pauseAfterParagraph = 1 * 1000;
        return text.length() * millisecondsPerLetter + pauseAfterParagraph;
    }

}
