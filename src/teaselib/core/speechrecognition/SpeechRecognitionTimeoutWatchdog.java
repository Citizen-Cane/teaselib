package teaselib.core.speechrecognition;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionTimeoutWatchdog {
    static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionTimeoutWatchdog.class);
    static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(2);

    private final AtomicReference<Timer> timer = new AtomicReference<>(null);
    private final SpeechRecognitionEvents events;
    private final Runnable timeoutAction;

    public SpeechRecognitionTimeoutWatchdog(SpeechRecognitionEvents events, Runnable timeoutAction) {
        this.events = events;
        this.timeoutAction = timeoutAction;
    }

    private TimerTask timerTask(Runnable timeoutAction) {
        return new TimerTask() {
            @Override
            public void run() {
                if (enabled()) {
                    logger.info("Timeout after {}ms", TIMEOUT_MILLIS);
                    timeoutAction.run();
                }
            }
        };
    }

    public void addEvents() {
        this.events.recognitionStarted.add(startWatching);
        this.events.audioSignalProblemOccured.add(updateAudioProblemStatus);
        this.events.speechDetected.add(updateSpeechDetectionStatus);
        this.events.recognitionRejected.add(stopWatching);
        this.events.recognitionCompleted.add(stopWatching);
    }

    public void removeEvents() {
        this.events.recognitionStarted.remove(startWatching);
        this.events.audioSignalProblemOccured.remove(updateAudioProblemStatus);
        this.events.speechDetected.add(updateSpeechDetectionStatus);
        this.events.recognitionRejected.remove(stopWatching);
        this.events.recognitionCompleted.remove(stopWatching);
    }

    public boolean enabled() {
        return timer.get() != null;
    }

    public void enable(boolean enabled) {
        if (enabled) {
            timer.set(newTimer());
        } else {
            stopTimerTask();
            timer.set(null);
        }
    }

    private Event<SpeechRecognitionStartedEventArgs> startWatching = args -> {
        if (enabled()) {
            startTimerTask();
        }
    };

    private Event<AudioSignalProblemOccuredEventArgs> updateAudioProblemStatus = args -> {
        if (enabled()) {
            updateStatus();
        }
    };

    private Event<SpeechRecognizedEventArgs> updateSpeechDetectionStatus = args -> {
        if (enabled()) {
            updateStatus();
        }
    };

    private Event<SpeechRecognizedEventArgs> stopWatching = args -> {
        if (enabled()) {
            stopTimerTask();
            logger.info("Timeout watchdog stopped");
        }
    };

    private void startTimerTask() {
        if (enabled()) {
            logger.info("Restarting timeout watchdog");
        } else {
            logger.info("Starting timeout watchdog");
        }

        Timer newTimer = newTimer();
        timer.set(newTimer);
        newTimer.schedule(timerTask(timeoutAction), TIMEOUT_MILLIS);
    }

    private Timer newTimer() {
        return new Timer(getClass().getSimpleName());
    }

    private void updateStatus() {
        stopTimerTask();
        startTimerTask();
    }

    private void stopTimerTask() {
        Timer t = timer.get();
        if (t != null) {
            t.cancel();
        }
    }

}
