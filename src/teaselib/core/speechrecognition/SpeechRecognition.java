package teaselib.core.speechrecognition;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public final SpeechRecognitionEvents events;

    private final DelegateExecutor delegateThread = new DelegateExecutor("Speech Recognition dispatch");
    private final Locale locale;
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
                        SpeechRecognitionNativeImplementation instance = srClass.getConstructor(Locale.class)
                                .newInstance(locale);
                        instance.startEventLoop(events);
                        instance.setMaxAlternates(SpeechRecognitionImplementation.MAX_ALTERNATES_DEFAULT);
                        return instance;
                    } else {
                        return Unsupported.Instance;
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Throwable t) {
                    throw ExceptionUtil.asRuntimeException(t);
                }
            });
        }
    }

    public void close() {
        implementation.close();
        unlockSpeechRecognitionInProgressSyncObject();
        delegateThread.shutdown();
    }

    public PreparedChoices prepare(Choices choices) {
        return implementation.prepare(choices);
    }

    public void apply(PreparedChoices preparedChoices) {
        this.preparedChoices = preparedChoices;
    }

    public PreparedChoices preparedChoices() {
        return preparedChoices;
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
        Optional<Throwable> exception = implementation.getException();
        if (exception.isPresent())
            throw ExceptionUtil.asRuntimeException(exception.get());

        preparedChoices.accept(implementation);
        audioSync.whenSpeechCompleted(implementation::startRecognition);
        speechRecognitionActive.set(true);
    }

    private void disableSR() {
        implementation.stopRecognition();
        speechRecognitionActive.set(false);
        unlockSpeechRecognitionInProgressSyncObjectFromDelegateThread();

        Optional<Throwable> exception = implementation.getException();
        if (exception.isPresent())
            throw ExceptionUtil.asRuntimeException(exception.get());
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
