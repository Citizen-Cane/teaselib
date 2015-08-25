package teaselib.core.speechrecognition;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.TeaseLib;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.events.Delegate;
import teaselib.core.events.DelegateThread;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.implementation.TeaseLibSR;
import teaselib.core.texttospeech.TextToSpeech;

public class SpeechRecognition {

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

    private final String locale;
    private SpeechRecognitionImplementation sr;
    private final DelegateThread delegateThread = new DelegateThread(
            "Text-To-Speech dispatcher thread");

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

    private void lockSpeechRecognitionInProgressSyncObject() {
        Delegate delegate = new Delegate() {
            @Override
            public void run() {
                try {
                    SpeechRecognitionInProgress.lock();
                } catch (Throwable t) {
                    TeaseLib.log(this, t);
                }
            }
        };
        try {
            delegateThread.run(delegate);
        } catch (ScriptInterruptedException e) {
            throw e;
        } catch (Throwable t) {
            TeaseLib.log(this, t);
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
                        SpeechRecognitionInProgress.unlock();
                    }
                } catch (ScriptInterruptedException e) {
                    throw e;
                } catch (Throwable t) {
                    TeaseLib.log(this, t);
                }
            }
        };
        try {
            delegateThread.run(delegate);
        } catch (ScriptInterruptedException e) {
            throw e;
        } catch (Throwable t) {
            TeaseLib.log(this, t);
        }
    }

    private final SpeechRecognitionHypothesisEventHandler hypothesisEventHandler;

    public SpeechRecognition(String locale) {
        // First add the progress events, because we don't want to get events
        // consumed before setting the in-progress state
        this.events = new SpeechRecognitionEvents<SpeechRecognitionImplementation>(
                lockSpeechRecognitionInProgress,
                unlockSpeechRecognitionInProgress);
        this.locale = locale.toLowerCase();
        try {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    try {
                        sr = new TeaseLibSR();
                        sr.init(events, SpeechRecognition.this.locale);
                    } catch (UnsatisfiedLinkError e) {
                        TeaseLib.log(this, e);
                        sr = null;
                    } catch (Throwable t) {
                        TeaseLib.log(this, t);
                        sr = null;
                    }
                }
            };
            delegateThread.run(delegate);
        } catch (ScriptInterruptedException e) {
            throw e;
        } catch (Throwable t) {
            TeaseLib.log(this, t);
        }
        // Last add the hypothesis handler, as it may consume the
        // RecognitionRejected-event
        this.hypothesisEventHandler = new SpeechRecognitionHypothesisEventHandler(
                this);
    }

    /**
     * @return Whether SpeechRecognition is ready to render speech
     */
    public boolean isReady() {
        return sr != null;
    }

    public void startRecognition(final List<String> choices) {
        hypothesisEventHandler.setChoices(choices);
        if (sr != null) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    SpeechRecognition.this.speechRecognitionActive = true;
                    sr.setChoices(choices);
                    // Don't try to recognize speech during speech synthesis or
                    // other speech related audio output
                    synchronized (TextToSpeech.AudioOutput) {
                        sr.startRecognition();
                    }
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (ScriptInterruptedException e) {
                throw e;
            } catch (Throwable t) {
                SpeechRecognition.this.speechRecognitionActive = false;
                TeaseLib.log(this, t);
            }
        } else {
            recognizerNotInitialized();
        }
    }

    public void resumeRecognition() {
        if (sr != null) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    SpeechRecognition.this.speechRecognitionActive = true;
                    // Don't try to recognize speech during speech synthesis or
                    // other speech related audio output
                    synchronized (TextToSpeech.AudioOutput) {
                        sr.startRecognition();
                    }
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (ScriptInterruptedException e) {
                throw e;
            } catch (Throwable t) {
                SpeechRecognition.this.speechRecognitionActive = false;
                TeaseLib.log(this, t);
            }
        } else {
            recognizerNotInitialized();
        }
    }

    public void stopRecognition() {
        if (sr != null) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    try {
                        sr.stopRecognition();
                    } finally {
                        if (SpeechRecognition.this.speechRecognitionActive) {
                            SpeechRecognition.this.speechRecognitionActive = false;
                        }
                    }
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (ScriptInterruptedException e) {
                throw e;
            } catch (Throwable t) {
                TeaseLib.log(this, t);
            } finally {
                // Unlock explicitly since after stopping we won't receive
                // events anymore
                unlockSpeechRecognitionInProgressSyncObject();
            }
        } else {
            recognizerNotInitialized();
        }
    }

    private static void recognizerNotInitialized() {
        throw new IllegalStateException("Recognizer not initialized");
    }

    public static void completeSpeechRecognitionInProgress() {
        if (SpeechRecognitionInProgress.isLocked()) {
            TeaseLib.log("Waiting for speech recognition to complete");
            try {
                SpeechRecognitionInProgress.lockInterruptibly();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            } finally {
                if (SpeechRecognitionInProgress.isHeldByCurrentThread()) {
                    SpeechRecognitionInProgress.unlock();
                }
            }
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
