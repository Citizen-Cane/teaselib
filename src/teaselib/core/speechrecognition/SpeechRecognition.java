package teaselib.core.speechrecognition;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.events.Delegate;
import teaselib.core.events.DelegateThread;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.implementation.TeaseLibSR;
import teaselib.core.texttospeech.TextToSpeech;

public class SpeechRecognition {
    private static final Logger logger = LoggerFactory
            .getLogger(SpeechRecognition.class);

    static final String EnableSpeechHypothesisHandlerGlobally = SpeechRecognition.class
            .getPackage().getName() + ".Enable"
            + SpeechDetectionEventHandler.class.getSimpleName() + "Globally";

    /**
     * How to handle speech recognition and timeout in script functions.
     *
     */
    public enum TimeoutBehavior {
        /**
         * Cut the slave short: Ignore speech recognition and just finish.
         */
        InDubioContraReum("In Dubio Contra Reum"),

        /**
         * Be indulgent and let the user finish speaking before deciding about
         * timeout. A recognized prompt will cancel the timeout, even if time is
         * up, and return the recognized choice instead of a timeout.
         */
        InDubioProDuriore("In Dubio Pro Duriore"),

        /**
         * Give the benefit of the doubt and stop the timeout on the first
         * attempt to answer via speech recognition, even if that recognition
         * result will be rejected.
         * 
         * The prompt has of course still to be answered.
         */
        InDubioMitius("In Dubio Mitius");

        private final String displayName;

        TimeoutBehavior(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum AudioSignalProblem {
        None,
        Noise,
        NoSignal,
        TooLoud,
        TooQuiet,
        TooFast,
        TooSlow
    }

    public final SpeechRecognitionEvents<SpeechRecognitionImplementation> events;

    private final Locale locale;
    private final SpeechDetectionEventHandler hypothesisEventHandler;
    private final DelegateThread delegateThread = new DelegateThread(
            "Text-To-Speech dispatcher thread");

    private SpeechRecognitionImplementation sr;

    /**
     * Locked if a recognition is in progress, e.g. a start event has been
     * fired, but the the recognition has neither been rejected or completed
     */
    private static final ReentrantLock SpeechRecognitionInProgress = new ReentrantLock();

    /**
     * Speech recognition has been started or resumed and is listening for voice
     * input
     */
    private boolean speechRecognitionActive = false;

    // Allow other threads to wait for speech recognition to complete
    private Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> lockSpeechRecognitionInProgress = new Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs>() {
        @Override
        public void run(SpeechRecognitionImplementation sender,
                SpeechRecognitionStartedEventArgs args) {
            lockSpeechRecognitionInProgressSyncObject();
        }
    };

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> unlockSpeechRecognitionInProgress = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
        @Override
        public void run(SpeechRecognitionImplementation sender,
                SpeechRecognizedEventArgs args) {
            unlockSpeechRecognitionInProgressSyncObject();
        }
    };

    private Confidence recognitionConfidence;

    private List<String> choices;

    private void lockSpeechRecognitionInProgressSyncObject() {
        Delegate delegate = new Delegate() {
            @Override
            public void run() {
                try {
                    logger.debug("Locking speech recognition sync object");
                    SpeechRecognitionInProgress.lock();
                } catch (Throwable t) {
                    logger.error(t.getMessage(), t);
                }
            }
        };
        try {
            delegateThread.run(delegate);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    private void unlockSpeechRecognitionInProgressSyncObject() {
        Delegate delegate = new Delegate() {
            @Override
            public void run() {
                try {
                    // Check because this is called as a completion event by the
                    // event source, and might be called twice because the
                    // hypothesis
                    // event handler may generate a Completion event
                    if (SpeechRecognitionInProgress.isHeldByCurrentThread()) {
                        logger.debug(
                                "Unlocking speech recognition sync object");
                        SpeechRecognitionInProgress.unlock();
                    }
                } catch (Throwable t) {
                    logger.error(t.getMessage(), t);
                }
            }
        };
        try {
            delegateThread.run(delegate);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    public SpeechRecognition(Locale locale) {
        // First add the progress events, because we don't want to get events
        // consumed before setting the in-progress state
        this.events = new SpeechRecognitionEvents<SpeechRecognitionImplementation>(
                lockSpeechRecognitionInProgress,
                unlockSpeechRecognitionInProgress);
        this.locale = locale;
        try {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    try {
                        sr = new TeaseLibSR();
                        sr.init(events, SpeechRecognition.this.locale);
                    } catch (UnsatisfiedLinkError e) {
                        logger.error(e.getMessage(), e);
                        sr = null;
                    } catch (Throwable t) {
                        logger.error(t.getMessage(), t);
                        sr = null;
                    }
                }
            };
            delegateThread.run(delegate);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        // add the hypothesis handler now, as it may consume the
        // RecognitionRejected-event and fire an recognized event instead,
        // and subsequent listeners should react to these events
        hypothesisEventHandler = new SpeechDetectionEventHandler(this);
        hypothesisEventHandler.addEventListeners();
    }

    private static boolean enableSpeechHypothesisHandlerGlobally() {
        return Boolean.toString(true).compareToIgnoreCase(System.getProperty(
                EnableSpeechHypothesisHandlerGlobally, "false")) == 0;
    }

    /**
     * @return Whether SpeechRecognition is ready to recognize speech
     */
    public boolean isReady() {
        return sr != null;
    }

    public void startRecognition(List<String> choices,
            Confidence recognitionConfidence) {
        this.choices = choices;
        this.recognitionConfidence = recognitionConfidence;
        if (sr != null) {
            Delegate startRecognition = new Delegate() {
                @Override
                public void run() {
                    setupAndStartSR(SpeechRecognition.this.choices);
                    logger.info("Speech recognition started");
                }

            };
            try {
                delegateThread.run(startRecognition);
            } catch (InterruptedException e) {
                hypothesisEventHandler.enable(false);
                SpeechRecognition.this.speechRecognitionActive = false;
                throw new ScriptInterruptedException();
            } catch (Throwable t) {
                hypothesisEventHandler.enable(false);
                SpeechRecognition.this.speechRecognitionActive = false;
                logger.error(t.getMessage(), t);
            }
        } else {
            recognizerNotInitialized();
        }
    }

    public void resumeRecognition() {
        if (sr != null) {
            Delegate resumeRecognition = new Delegate() {
                @Override
                public void run() {
                    setupAndStartSR(choices);
                    logger.info("Speech recognition resumed");
                }
            };
            try {
                delegateThread.run(resumeRecognition);
            } catch (InterruptedException e) {
                hypothesisEventHandler.enable(false);
                SpeechRecognition.this.speechRecognitionActive = false;
                throw new ScriptInterruptedException();
            } catch (Throwable t) {
                hypothesisEventHandler.enable(false);
                SpeechRecognition.this.speechRecognitionActive = false;
                logger.error(t.getMessage(), t);
            }
        } else {
            recognizerNotInitialized();
        }
    }

    public void stopRecognition() {
        if (sr != null) {
            Delegate stopRecognition = new Delegate() {
                @Override
                public void run() {
                    try {
                        hypothesisEventHandler.enable(false);
                        sr.stopRecognition();
                    } finally {
                        SpeechRecognition.this.speechRecognitionActive = false;
                        logger.info("Speech recognition stopped");
                    }
                }
            };
            try {
                delegateThread.run(stopRecognition);
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            } finally {
                // Unlock explicitly since after stopping we won't receive
                // events anymore
                unlockSpeechRecognitionInProgressSyncObject();
            }
        } else {
            recognizerNotInitialized();
        }
    }

    private void setupAndStartSR(final List<String> choices) {
        if (enableSpeechHypothesisHandlerGlobally()
                || SpeechRecognition.this.recognitionConfidence == Confidence.Low) {
            hypothesisEventHandler.setChoices(choices);
            hypothesisEventHandler.setConfidence(
                    SpeechRecognition.this.recognitionConfidence);
            hypothesisEventHandler.enable(true);
        } else {
            hypothesisEventHandler.enable(false);
        }
        sr.setChoices(SpeechRecognition.this.choices);
        SpeechRecognition.this.speechRecognitionActive = true;
        // Don't try to recognize speech during speech synthesis or
        // other speech related audio output
        synchronized (TextToSpeech.AudioOutput) {
            sr.startRecognition();
        }
    }

    private static void recognizerNotInitialized() {
        throw new IllegalStateException("Recognizer not initialized");
    }

    public static boolean isSpeechRecognitionInProgress() {
        return SpeechRecognitionInProgress.isLocked();
    }

    public static void completeSpeechRecognitionInProgress() {
        if (SpeechRecognitionInProgress.isLocked()) {
            logger.info("Waiting for speech recognition to complete");
            try {
                SpeechRecognitionInProgress.lockInterruptibly();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            } finally {
                if (SpeechRecognitionInProgress.isHeldByCurrentThread()) {
                    SpeechRecognitionInProgress.unlock();
                }
            }
        } else {
            logger.debug("Speech recognition sync object not locked");
        }
    }

    /**
     * Determine whether speech recognition listens to voice input
     * 
     * @return True if speech recognition is listening to voice input
     */
    public boolean isActive() {
        return speechRecognitionActive && isReady();
    }
}
