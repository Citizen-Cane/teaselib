package teaselib.speechrecognition;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.TeaseLib;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.events.Delegate;
import teaselib.core.events.DelegateThread;
import teaselib.core.events.Event;
import teaselib.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.speechrecognition.implementation.TeaseLibSR;
import teaselib.texttospeech.TextToSpeech;

public class SpeechRecognition {
    private String locale;
    private SpeechRecognitionImplementation sr;
    private DelegateThread delegateThread = new DelegateThread();

    private static void recognizerNotInitialized() {
        throw new IllegalStateException("Recognizer not initialized");
    }

    public enum AudioSignalProblem {
        None, Noise, NoSignal, TooLoud, TooQuiet, TooFast, TooSlow
    }

    private final ReentrantLock SpeechRecognitionInProgress = new ReentrantLock();

    private boolean speechRecognitionActive = false;

    // Allow other threads to wait while speech recognition is action
    private Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> lockSpeechRecognitionInProgress = new Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs>() {
        @Override
        public void run(SpeechRecognitionImplementation sender,
                SpeechRecognitionStartedEventArgs args) {
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
            } catch (Throwable t) {
                TeaseLib.log(this, t);
            }
        }
    };
    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> unlockSpeechRecognitionInProgress = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
        @Override
        public void run(SpeechRecognitionImplementation sender,
                SpeechRecognizedEventArgs args) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    try {
                        SpeechRecognitionInProgress.unlock();
                    } catch (Throwable t) {
                        TeaseLib.log(this, t);
                    }
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (Throwable t) {
                TeaseLib.log(this, t);
            }
        }
    };

    public final SpeechRecognitionEvents<SpeechRecognitionImplementation> events = new SpeechRecognitionEvents<SpeechRecognitionImplementation>(
            lockSpeechRecognitionInProgress, unlockSpeechRecognitionInProgress);

    public SpeechRecognition(String locale) {
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
        } catch (Throwable t) {
            TeaseLib.log(this, t);
        }
    }

    /**
     * @return Whether SpeechRecognition is ready to render speech
     */
    public boolean isReady() {
        return sr != null;
    }

    public void startRecognition(final List<String> choices) {
        if (sr != null) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    sr.setChoices(choices);
                    // Don't try to recognize speech during speech synthesis or
                    // other speech related audio output
                    synchronized (TextToSpeech.AudioOutput) {
                        sr.startRecognition();
                        SpeechRecognition.this.speechRecognitionActive = true;
                    }
                }
            };
            try {
                delegateThread.run(delegate);
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
                    // Don't try to recognize speech during speech synthesis or
                    // other speech related audio output
                    synchronized (TextToSpeech.AudioOutput) {
                        sr.startRecognition();
                        SpeechRecognition.this.speechRecognitionActive = true;
                    }
                }
            };
            try {
                delegateThread.run(delegate);
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
                        SpeechRecognition.this.speechRecognitionActive = false;
                        // This unlock is optional
                        if (SpeechRecognitionInProgress.tryLock()) {
                            SpeechRecognitionInProgress.unlock();
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (Throwable t) {
                TeaseLib.log(this, t);
            }
        } else {
            recognizerNotInitialized();
        }
    }

    public void completeSpeechRecognitionInProgress() {
        if (sr != null) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    try {
                        SpeechRecognitionInProgress.lockInterruptibly();
                    } catch (InterruptedException e) {
                        throw new ScriptInterruptedException();
                    } finally {
                        SpeechRecognitionInProgress.unlock();
                    }
                }
            };
            try {
                delegateThread.run(delegate);
            } catch (Throwable t) {
                TeaseLib.log(this, t);
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
