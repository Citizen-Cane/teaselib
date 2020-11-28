package teaselib.core.speechrecognition;

import static java.util.Collections.emptyList;
import static teaselib.core.speechrecognition.Confidence.High;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.AudioSync;
import teaselib.core.events.DelegateExecutor;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.Choices;
import teaselib.core.util.Environment;
import teaselib.core.util.ExceptionUtil;

public class SpeechRecognition {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognition.class);

    static final Rule TIMEOUT = new Rule("Timeout", "", Integer.MIN_VALUE, emptyList(), 0, 0, 1.0f, High);

    public final SpeechRecognitionEvents events;
    public final Locale locale;

    private final SpeechRecognitionTimeoutWatchdog timeoutWatchdog;
    private final DelegateExecutor delegateThread = new DelegateExecutor("Speech Recognition dispatch");
    public final SpeechRecognitionNativeImplementation implementation;

    private PreparedChoices preparedChoices;

    /**
     * Speech recognition has been started or resumed and is listening for voice input
     */
    private AtomicBoolean speechRecognitionActive = new AtomicBoolean(false);

    // Allow other threads to wait for speech recognition to complete
    private Event<SpeechRecognitionStartedEventArgs> lockSpeechRecognitionInProgress = args -> {
        if (isActiveCalledFromDelegateThread()) {
            lockSpeechRecognitionInProgressSyncObject();
        }
    };

    private Event<SpeechRecognizedEventArgs> unlockSpeechRecognitionInProgress = args -> unlockSpeechRecognitionInProgressSyncObject();

    private final Event<SpeechRecognizedEventArgs> handleMissingRecognitionStartedEvent;

    public final AudioSync audioSync;

    private void lockSpeechRecognitionInProgressSyncObject() {
        delegateThread.run(() -> {
            // RenetrantLock is ref-counted,
            // and startRecognition events can occur more than once
            if (!audioSync.speechRecognitionInProgress()) {
                logger.debug("Locking speech recognition sync object");
                audioSync.startSpeechRecognition();
            }
        });
    }

    private void unlockSpeechRecognitionInProgressSyncObject() {
        delegateThread.run(this::unlockSpeechRecognitionInProgressSyncObjectFromDelegateThread);
    }

    private void unlockSpeechRecognitionInProgressSyncObjectFromDelegateThread() {
        // Check because this is called as a completion event by the
        // event source, and might be called twice when the
        // hypothesis event handler generates a Completion event
        if (audioSync.speechRecognitionInProgress()) {
            logger.debug("Unlocking speech recognition sync object");
            audioSync.endSpeechRecognition();
        }
    }

    public SpeechRecognition(Locale locale, Class<? extends SpeechRecognitionNativeImplementation> srClass,
            AudioSync audioSync) {
        this.events = new SpeechRecognitionEvents(lockSpeechRecognitionInProgress, unlockSpeechRecognitionInProgress);
        this.locale = locale;
        this.audioSync = audioSync;

        if (locale == null) {
            implementation = Unsupported.Instance;
        } else if (srClass == Unsupported.class) {
            implementation = Unsupported.Instance;
        } else {
            implementation = delegateThread.call(() -> {
                try {
                    if (Environment.SYSTEM == Environment.Windows) {
                        SpeechRecognitionNativeImplementation instance = srClass
                                .getConstructor(Locale.class, SpeechRecognitionEvents.class)
                                .newInstance(locale, events);
                        instance.process(events);
                        return instance;
                    } else {
                        return Unsupported.Instance;
                    }
                } catch (UnsupportedLanguageException e) {
                    logger.warn(e.getMessage());
                    throw e;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Throwable t) {
                    throw ExceptionUtil.asRuntimeException(t);
                }
            });
        }

        // Add watchdog last, to receive speechRejected/completed from the hypothesis event handler
        this.timeoutWatchdog = new SpeechRecognitionTimeoutWatchdog(this::handleRecognitionTimeout);
        this.timeoutWatchdog.add(events);

        handleMissingRecognitionStartedEvent = e -> {
            if (!audioSync.speechRecognitionInProgress()) {
                events.recognitionStarted.fire(new SpeechRecognitionStartedEventArgs());
            }
        };

        this.events.speechDetected.add(handleMissingRecognitionStartedEvent);
    }

    public void close() {
        this.events.speechDetected.remove(handleMissingRecognitionStartedEvent);

        timeoutWatchdog.enable(false);
        timeoutWatchdog.remove(events);

        implementation.close();

        unlockSpeechRecognitionInProgressSyncObject();
        delegateThread.shutdown();
    }

    private void handleRecognitionTimeout() {
        if (audioSync.speechRecognitionInProgress()) {
            events.recognitionRejected.fire(new SpeechRecognizedEventArgs(TIMEOUT));
        }
    }

    public PreparedChoices prepare(Choices choices) {
        return implementation.prepare(choices);
    }

    public void apply(PreparedChoices preparedChoices) {
        this.preparedChoices = preparedChoices;
    }

    public Optional<Rule> hypothesis(List<Rule> rules, Rule currentHypothesis) {
        return preparedChoices.hypothesis(rules, currentHypothesis);
    }

    public IntUnaryOperator phraseToChoiceMapping() {
        return preparedChoices.mapper();
    }

    public void startRecognition() {
        if (implementation != null) {
            delegateThread.run(() -> {
                if (isActiveCalledFromDelegateThread()) {
                    logger.warn("Speech recognition already running");
                } else {
                    enableSR();
                    logger.info("Speech recognition started");
                }
            });
        } else {
            recognizerNotInitialized();
        }
    }

    public void pauseRecognition() {
        if (implementation != null) {
            delegateThread.run(() -> {
                if (isActiveCalledFromDelegateThread()) {
                    disableSR();
                    logger.info("Speech recognition paused");
                } else {
                    logger.warn("Speech recognition already stopped");
                }
            });
        } else {
            recognizerNotInitialized();
        }
    }

    public void resumeRecognition() {
        if (implementation != null) {
            delegateThread.run(() -> {
                if (isActiveCalledFromDelegateThread()) {
                    logger.warn("Speech recognition already running on resume attempt");
                } else if (preparedChoices == null) {
                    logger.warn("Speech recognition already stopped - not resumed");
                } else {
                    enableSR();
                    logger.info("Speech recognition resumed");
                }
            });
        } else {
            recognizerNotInitialized();
        }
    }

    public void restartRecognition() {
        if (implementation != null) {
            delegateThread.run(() -> {
                if (isActiveCalledFromDelegateThread()) {
                    implementation.stopRecognition();
                    enableSR();
                    logger.info("Speech recognition restarted");
                } else {
                    logger.warn("Speech recognition already stopped - restarting not allowed");
                }
            });
        } else {
            recognizerNotInitialized();
        }
    }

    public void endRecognition() {
        if (implementation != null) {
            delegateThread.run(() -> {
                preparedChoices = null;
                if (isActiveCalledFromDelegateThread()) {
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

    private void enableSR() {
        preparedChoices.accept(implementation);
        audioSync.whenSpeechCompleted(implementation::startRecognition);
        timeoutWatchdog.enable(true);
        speechRecognitionActive.set(true);
    }

    private void disableSR() {
        implementation.stopRecognition();
        speechRecognitionActive.set(false);
        timeoutWatchdog.enable(false);
        unlockSpeechRecognitionInProgressSyncObjectFromDelegateThread();
    }

    private static void recognizerNotInitialized() {
        throw new IllegalStateException("Recognizer not initialized");
    }

    public boolean isActive() {
        return delegateThread.call(this::isActiveCalledFromDelegateThread);
    }

    /**
     * Determine whether speech recognition listens to voice input
     * 
     * @return True if speech recognition is listening to voice input
     */
    private boolean isActiveCalledFromDelegateThread() {
        return speechRecognitionActive.get();
    }

    public void emulateRecogntion(String emulatedRecognitionResult) {
        if (implementation != null) {
            delegateThread.run(() -> {
                implementation.emulateRecognition(emulatedRecognitionResult);
                logger.info("Emulating recognition for '{}'", emulatedRecognitionResult);
            });
        } else {
            recognizerNotInitialized();
        }
    }

    @Override
    public String toString() {
        return locale.toString();
    }

}
