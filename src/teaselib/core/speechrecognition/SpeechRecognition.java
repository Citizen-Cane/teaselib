package teaselib.core.speechrecognition;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static teaselib.core.speechrecognition.Confidence.High;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.AudioSync;
import teaselib.core.events.DelegateExecutor;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.speechrecognition.implementation.Unsupported;
import teaselib.core.speechrecognition.implementation.UnsupportedLanguageException;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.util.Environment;
import teaselib.core.util.ExceptionUtil;

public class SpeechRecognition {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognition.class);

    static final Rule TIMEOUT = new Rule("Timeout", "", Integer.MIN_VALUE, emptySet(), 0, 0, 1.0f, High);

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

    public final SpeechRecognitionEvents events;
    public final Locale locale;

    private final SpeechRecognitionTimeoutWatchdog timeoutWatchdog;
    private final DelegateExecutor delegateThread = new DelegateExecutor("Speech Recognition dispatch");
    public final SpeechRecognitionImplementation implementation;

    private SpeechRecognitionParameters parameters;

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

    SpeechRecognition() {
        this(null, null);
    }

    SpeechRecognition(Locale locale, AudioSync audioSync) {
        this(locale, TeaseLibSRGS.class, audioSync);
    }

    SpeechRecognition(Locale locale, Class<? extends SpeechRecognitionImplementation> srClass, AudioSync audioSync) {
        this.events = new SpeechRecognitionEvents(lockSpeechRecognitionInProgress, unlockSpeechRecognitionInProgress);
        this.locale = locale;
        this.audioSync = audioSync;

        if (locale == null) {
            implementation = Unsupported.Instance;
        } else {
            implementation = delegateThread.call(() -> {
                try {
                    if (Environment.SYSTEM == Environment.Windows) {
                        SpeechRecognitionImplementation instance = srClass.getConstructor().newInstance();
                        instance.init(events, locale);
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

    void close() {
        this.events.speechDetected.remove(handleMissingRecognitionStartedEvent);

        timeoutWatchdog.enable(false);
        timeoutWatchdog.remove(events);

        implementation.close();

        unlockSpeechRecognitionInProgressSyncObjectFromDelegateThread();
        delegateThread.shutdown();
    }

    private void handleRecognitionTimeout() {
        if (audioSync.speechRecognitionInProgress()) {
            events.recognitionRejected.fire(new SpeechRecognizedEventArgs(TIMEOUT));
        }
    }

    public void setChoices(SpeechRecognitionParameters parameters) {
        this.parameters = parameters;
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
                    logger.warn("Speech recognition already running on restart attempt");
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
                if (isActiveCalledFromDelegateThread()) {
                    disableSR();
                    parameters = null;
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
        if (implementation instanceof SpeechRecognitionSRGS) {
            ((SpeechRecognitionSRGS) implementation).setChoices(parameters.srgs);
        } else if (implementation instanceof SpeechRecognitionChoices) {
            ((SpeechRecognitionChoices) implementation).setChoices(firstPhraseOfEach(parameters.choices));
        } else if (implementation instanceof Unsupported) {
            return;
        } else {
            throw new UnsupportedOperationException(SpeechRecognitionChoices.class.getSimpleName());
        }

        audioSync.whenSpeechCompleted(implementation::startRecognition);

        timeoutWatchdog.enable(true);
        speechRecognitionActive.set(true);
    }

    public List<String> firstPhraseOfEach(Choices choices) {
        return choices.stream().map(SpeechRecognition::firstPhrase).map(SpeechRecognition::withoutPunctation)
                .collect(toList());
    }

    private static String firstPhrase(Choice choice) {
        return choice.phrases.get(0);
    }

    static String withoutPunctation(String text) {
        return Arrays.stream(PhraseString.words(text)).collect(joining(" "));
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
