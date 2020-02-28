package teaselib.core.speechrecognition;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.events.DelegateExecutor;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.hypothesis.SpeechDetectionEventHandler;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.speechrecognition.implementation.Unsupported;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.ui.Choices;
import teaselib.core.util.Environment;
import teaselib.core.util.ExceptionUtil;

public class SpeechRecognition {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognition.class);

    private static final Map<Confidence, Confidence> confidenceMapping = confidenceMapping();

    private static EnumMap<Confidence, Confidence> confidenceMapping() {
        EnumMap<Confidence, Confidence> enumMap = new EnumMap<>(Confidence.class);
        enumMap.put(Confidence.Noise, Confidence.Noise);
        enumMap.put(Confidence.Low, Confidence.Low);
        enumMap.put(Confidence.Normal, Confidence.Normal);
        enumMap.put(Confidence.High, Confidence.High);
        return enumMap;
    }

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
         * Be indulgent and let the user finish speaking before deciding about timeout. A recognized prompt will cancel
         * the timeout, even if time is up, and return the recognized choice instead of a timeout.
         */
        InDubioProDuriore("In Dubio Pro Duriore"),

        /**
         * Give the benefit of the doubt and stop the timeout on the first attempt to answer via speech recognition,
         * even if that recognition result will be rejected.
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

    public final SpeechRecognitionEvents events;
    private final Locale locale;

    private final SpeechDetectionEventHandler speechDetectionEventHandler;
    private final SpeechRecognitionTimeoutWatchdog timeoutWatchdog;
    private final DelegateExecutor delegateThread = new DelegateExecutor("Speech Recognition dispatch");

    public SpeechRecognitionImplementation sr = null;

    /**
     * Locked if a recognition is in progress, e.g. a start event has been fired, but the the recognition has neither
     * been rejected or completed
     */
    private static final ReentrantLock SpeechRecognitionInProgress = new ReentrantLock();

    @Override
    public String toString() {
        return locale.toString();
    }

    /**
     * Speech recognition has been started or resumed and is listening for voice input
     */
    private AtomicBoolean speechRecognitionActive = new AtomicBoolean(false);

    // Allow other threads to wait for speech recognition to complete
    private Event<SpeechRecognitionStartedEventArgs> lockSpeechRecognitionInProgress = args -> {
        if (isActive()) {
            lockSpeechRecognitionInProgressSyncObject();
        }
    };

    private Event<SpeechRecognizedEventArgs> unlockSpeechRecognitionInProgress = (
            args) -> unlockSpeechRecognitionInProgressSyncObject();

    private Confidence recognitionConfidence;

    private Choices choices;
    private byte[] srgs;
    IntUnaryOperator mapper;

    private void lockSpeechRecognitionInProgressSyncObject() {
        delegateThread.run(() -> {
            // RenetrantLock is ref-counted,
            // and startRecognition events can occur more than once
            if (!SpeechRecognitionInProgress.isLocked()) {
                logger.debug("Locking speech recognition sync object");
                SpeechRecognitionInProgress.lockInterruptibly();
            }
        });
    }

    private void unlockSpeechRecognitionInProgressSyncObject() {
        delegateThread.run(SpeechRecognition::unlockSpeechRecognitionInProgressSyncObjectFromDelegateThread);
    }

    private static void unlockSpeechRecognitionInProgressSyncObjectFromDelegateThread() {
        // Check because this is called as a completion event by the
        // event source, and might be called twice when the
        // hypothesis event handler generates a Completion event
        if (SpeechRecognitionInProgress.isLocked()) {
            logger.debug("Unlocking speech recognition sync object");
            SpeechRecognitionInProgress.unlock();
        }
    }

    SpeechRecognition() {
        this(null);
    }

    SpeechRecognition(Locale locale) {
        this(locale, TeaseLibSRGS.class);
    }

    SpeechRecognition(Locale locale, Class<? extends SpeechRecognitionImplementation> srClass) {
        // First add the progress events, because we don't want to get events
        // consumed before setting the in-progress state
        this.events = new SpeechRecognitionEvents(lockSpeechRecognitionInProgress, unlockSpeechRecognitionInProgress);
        speechDetectionEventHandler = new SpeechDetectionEventHandler(events);
        this.locale = locale;
        if (locale == null) {
            sr = Unsupported.Instance;
        } else {
            delegateThread.run(() -> {
                try {
                    if (Environment.SYSTEM == Environment.Windows) {
                        sr = srClass.getConstructor().newInstance();
                        sr.init(speechDetectionEventHandler.eventSink, SpeechRecognition.this.locale);
                    } else {
                        sr = Unsupported.Instance;
                    }
                } catch (RuntimeException e) {
                    // TODO Handle UnsupportedLanguageException and give instructions to
                    // download speech recognition pack for the selected language
                    throw e;
                } catch (Throwable t) {
                    throw ExceptionUtil.asRuntimeException(t);
                }
            });
        }

        // add the SpeechDetectionEventHandler listeners now to ensure
        // other listeners downstream receive only the correct event,
        // as the event handler may consume the
        // RecognitionRejected-event and fire an recognized event instead
        speechDetectionEventHandler.addEventListeners();
        // Add watchdog last, to receive speechRejected/completed from the hypothesis event handler
        this.timeoutWatchdog = new SpeechRecognitionTimeoutWatchdog(events, this::handleRecognitionTimeout);
        this.timeoutWatchdog.addEvents();
    }

    void close() {
        sr.close();
        delegateThread.shutdown();
        timeoutWatchdog.removeEvents();
        speechDetectionEventHandler.removeEventListeners();
        sr = null;
    }

    private void handleRecognitionTimeout() {
        Rule result = speechDetectionEventHandler.getHypothesis();
        if (result == null) {
            result = new Rule("", "", Integer.MIN_VALUE, Collections.emptySet(), 0, 0, 0.0f, Confidence.Noise);
        }
        events.recognitionRejected.run(new SpeechRecognizedEventArgs(result));
    }

    @Deprecated
    public void setChoices(Choices choices) {
        throw new UnsupportedOperationException("Provide test method to generate impl specific srgs and mapper");
    }

    public void setChoices(Choices choices, byte[] srgs, IntUnaryOperator mapper) {
        this.choices = choices;
        this.srgs = srgs;
        this.mapper = mapper;
    }

    // TODO Make class stateless by moving sr process into new class with final state - remember that class upstream
    // TODO Add a prepare step to allow setup in advance - all cpu computations should be done beforehand
    public void startRecognition(Confidence recognitionConfidence) {
        this.recognitionConfidence = recognitionConfidence;
        if (sr != null) {
            delegateThread.run(() -> {
                enableSR();
                logger.info("Speech recognition started");
            });
        } else {
            recognizerNotInitialized();
        }
    }

    public void pauseRecognition() {
        if (sr != null) {
            delegateThread.run(() -> {
                disableSR();
                logger.info("Speech recognition paused");
            });
        } else {
            recognizerNotInitialized();
        }
    }

    public void resumeRecognition() {
        if (sr != null) {
            delegateThread.run(() -> {
                if (srgs != null) {
                    enableSR();
                    logger.info("Speech recognition resumed");
                } else {
                    logger.info("Speech recognition already stopped");
                }
            });
        } else {
            recognizerNotInitialized();
        }
    }

    public void restartRecognition() {
        if (sr != null) {
            delegateThread.run(() -> {
                sr.stopRecognition();
                if (srgs != null) {
                    // TODO Parse to srgs once before start - not on each resume
                    // - needs grammar management since prompts may be paused and a different grammar loaded
                    // TODO Phrases field may conflict with Prompt stacking because SR is not supposed to remember this
                    // -> needs map of phrases->grammar hash to enable/disable the right grammar (and to remove it when
                    // dismissed)
                    enableSR();
                    logger.info("Speech recognition restarted");
                } else {
                    logger.warn("Speech recognition already stopped");
                }
            });
        } else {
            recognizerNotInitialized();
        }
    }

    public void endRecognition() {
        if (sr != null) {
            delegateThread.run(() -> {
                if (isActive()) {
                    disableSR();
                    srgs = null;
                    mapper = null;
                    choices = null;
                    logger.info("Speech recognition stopped");
                } else {
                    logger.warn("Speech recognition already stopped");
                }
            });
        } else {
            recognizerNotInitialized();
        }
    }

    private void enableSR() {
        if (sr instanceof SpeechRecognitionSRGS) {
            ((SpeechRecognitionSRGS) sr).setChoices(srgs);
        } else if (sr instanceof SpeechRecognitionChoices) {
            ((SpeechRecognitionChoices) sr).setChoices(choices.firstPhraseOfEach());
        } else {
            throw new UnsupportedOperationException(SpeechRecognitionChoices.class.getSimpleName());
        }

        // TODO must be the same phrases used to setup sr implementation
        speechDetectionEventHandler.setChoices(choices);
        speechDetectionEventHandler.setExpectedConfidence(SpeechRecognition.this.recognitionConfidence);
        speechDetectionEventHandler.enable(true);

        timeoutWatchdog.enable(true);

        synchronized (TextToSpeech.AudioOutput) {
            sr.startRecognition();
        }

        speechRecognitionActive.set(true);
    }

    private void disableSR() {
        sr.stopRecognition();
        SpeechRecognition.this.speechRecognitionActive.set(false);
        speechDetectionEventHandler.enable(false);
        timeoutWatchdog.enable(false);
        unlockSpeechRecognitionInProgressSyncObjectFromDelegateThread();
    }

    private static void recognizerNotInitialized() {
        throw new IllegalStateException("Recognizer not initialized");
    }

    public static boolean isSpeechRecognitionInProgress() {
        return SpeechRecognitionInProgress.isLocked();
    }

    // TODO Move to SpeechRecognizer and make it non-static
    public static void completeSpeechRecognitionInProgress() {
        if (isSpeechRecognitionInProgress()) {
            logger.info("Waiting for speech recognition to complete");
            try {
                SpeechRecognitionInProgress.lockInterruptibly();
                SpeechRecognitionInProgress.unlock();
                logger.info("Speech recognition in progress completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        }
    }

    /**
     * Determine whether speech recognition listens to voice input
     * 
     * @return True if speech recognition is listening to voice input
     */
    public boolean isActive() {
        return speechRecognitionActive.get();
    }

    public void emulateRecogntion(String emulatedRecognitionResult) {
        if (sr != null) {
            delegateThread.run(() -> {
                sr.emulateRecognition(emulatedRecognitionResult);
                logger.info("Emulating recognition for '{}'", emulatedRecognitionResult);
            });
        } else {
            recognizerNotInitialized();
        }
    }

    public Confidence userDefinedConfidence(Confidence expectedConfidence) {
        return confidenceMapping.getOrDefault(expectedConfidence, expectedConfidence);
    }

    public Integer mapPhraseToChoice(int index) {
        return mapper.applyAsInt(index);
    }
}
