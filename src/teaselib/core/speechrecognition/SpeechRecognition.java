package teaselib.core.speechrecognition;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

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
import teaselib.core.speechrecognition.srgs.SRGSPhraseBuilder;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.ui.Choices;
import teaselib.core.util.Environment;
import teaselib.core.util.ExceptionUtil;

public class SpeechRecognition {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognition.class);

    static final String EnableSpeechHypothesisHandlerGlobally = SpeechRecognition.class.getPackage().getName()
            + ".Enable" + SpeechDetectionEventHandler.class.getSimpleName() + "Globally";

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

    private final SpeechDetectionEventHandler hypothesisEventHandler;
    private final SpeechRecognitionTimeoutWatchdog timeoutWatchdog;
    private final DelegateExecutor delegateThread = new DelegateExecutor("Speech Recognition dispatch");

    private SpeechRecognitionImplementation sr = null;

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
        this.locale = locale;
        if (locale == null) {
            sr = Unsupported.Instance;
        } else {
            delegateThread.run(() -> {
                try {
                    if (Environment.SYSTEM == Environment.Windows) {
                        sr = srClass.getConstructor().newInstance();
                        sr.init(events, SpeechRecognition.this.locale);
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
        hypothesisEventHandler = new SpeechDetectionEventHandler(this);
        hypothesisEventHandler.addEventListeners();
        // Add watchdog last, to receive speechRejected/completed from the hypothesis event handler
        this.timeoutWatchdog = new SpeechRecognitionTimeoutWatchdog(events, this::handleReecognitionTimeout);
        this.timeoutWatchdog.addEvents();
    }

    void close() {
        sr.close();
        delegateThread.shutdown();
        timeoutWatchdog.removeEvents();
        hypothesisEventHandler.removeEventListeners();
        sr = null;
    }

    private void handleReecognitionTimeout() {
        Rule result = hypothesisEventHandler.getHypothesis();
        if (result == null) {
            result = new Rule("", "", Integer.MIN_VALUE, Collections.emptySet(), 0, 0, 0.0f, Confidence.Noise);
        }
        events.recognitionRejected.run(new SpeechRecognizedEventArgs(result));
    }

    private static boolean enableSpeechHypothesisHandlerGlobally() {
        return Boolean.toString(true)
                .compareToIgnoreCase(System.getProperty(EnableSpeechHypothesisHandlerGlobally, "true")) == 0;
    }

    // TODO Make class stateless by moving sr process into new class with final state - remember that class upstream
    // TODO Add a prepare step to allow setup in advance - all cpu computations should be done beforehand
    public void startRecognition(Choices choices, Confidence recognitionConfidence) {
        this.choices = choices;
        this.srgs = srgs(choices);
        this.recognitionConfidence = recognitionConfidence;
        if (sr != null) {
            delegateThread.run(() -> {
                enableSR(SpeechRecognition.this.srgs);
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
                    enableSR(srgs);
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
                    enableSR(srgs);
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
                if (srgs != null) {
                    srgs = null;
                    disableSR();
                    logger.info("Speech recognition stopped");
                } else {
                    logger.warn("Speech recognition already stopped");
                }
            });
        } else {
            recognizerNotInitialized();
        }
    }

    private void enableSR(byte[] srgs) {
        setChoices(srgs);

        if (enableSpeechHypothesisHandlerGlobally() || SpeechRecognition.this.recognitionConfidence == Confidence.Low) {
            hypothesisEventHandler.setChoices(firstPhraseOfEachChoice());
            hypothesisEventHandler.setExpectedConfidence(SpeechRecognition.this.recognitionConfidence);
            hypothesisEventHandler.enable(true);
        } else {
            hypothesisEventHandler.enable(false);
        }
        timeoutWatchdog.enable(true);

        synchronized (TextToSpeech.AudioOutput) {
            sr.startRecognition();
        }

        speechRecognitionActive.set(true);
    }

    private void disableSR() {
        SpeechRecognition.this.speechRecognitionActive.set(false);
        hypothesisEventHandler.enable(false);
        timeoutWatchdog.enable(false);
        unlockSpeechRecognitionInProgressSyncObjectFromDelegateThread();
        sr.stopRecognition();
    }

    private void setChoices(byte[] srgs) {
        if (sr instanceof SpeechRecognitionSRGS) {
            ((SpeechRecognitionSRGS) sr).setChoices(srgs);
        } else if (sr instanceof SpeechRecognitionChoices) {
            List<String> firstPhraseOfEachChoice = firstPhraseOfEachChoice();
            ((SpeechRecognitionChoices) sr).setChoices(firstPhraseOfEachChoice);
        } else {
            throw new UnsupportedOperationException(SpeechRecognitionChoices.class.getSimpleName());
        }
    }

    private List<String> firstPhraseOfEachChoice() {
        return choices.stream().map(choice -> choice.phrases.get(0)).collect(toList());
    }

    byte[] srgs(Choices choices) {
        try {
            SRGSPhraseBuilder builder = new SRGSPhraseBuilder(choices, sr.languageCode);
            if (logger.isInfoEnabled()) {
                logger.info("{}", builder.toXML());
            }
            return builder.toBytes();
        } catch (ParserConfigurationException | TransformerException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
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
}
