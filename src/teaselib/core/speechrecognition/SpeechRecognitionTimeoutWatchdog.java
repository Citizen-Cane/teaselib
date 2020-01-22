package teaselib.core.speechrecognition;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    final SpeechRecognitionEvents events;
    final AtomicBoolean active = new AtomicBoolean(false);
    final Timer timer = new Timer(getClass().getSimpleName());

    TimerTask task = null;

    final Runnable timeoutAction;

    public SpeechRecognitionTimeoutWatchdog(SpeechRecognitionEvents events, Runnable timeoutAction) {
        this.events = events;
        this.timeoutAction = timeoutAction;
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
        return active.get();
    }

    public void enable(boolean enabled) {
        active.set(enabled);
        TimerTask t = task;
        if (!enabled && t != null) {
            stopTimerTask();
        }
    }

    private Event<SpeechRecognitionStartedEventArgs> startWatching = args -> {
        if (enabled()) {
            startTimerTask();
        }
    };

    private Event<AudioSignalProblemOccuredEventArgs> updateAudioProblemStatus = args -> {
        if (enabled() && task != null) {
            updateStatus();
        }
    };

    private Event<SpeechRecognizedEventArgs> updateSpeechDetectionStatus = args -> {
        if (enabled() && task != null) {
            updateStatus();
        }
    };

    private Event<SpeechRecognizedEventArgs> stopWatching = args -> {
        if (enabled() && task != null) {
            stopTimerTask();
            timer.purge();
            logger.info("Timeout watchdog stopped");
        }
    };

    private void startTimerTask() {
        if (task == null) {
            logger.info("Starting timeout watchdog");
        } else {
            logger.info("Restarting timeout watchdog");
        }

        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                if (enabled() && task != null) {
                    logger.info("Timeout after {}ms", TIMEOUT_MILLIS);
                    timeoutAction.run();
                }
            }
        };
        this.task = t;
        timer.schedule(t, TIMEOUT_MILLIS);
    }

    private void updateStatus() {
        stopTimerTask();
        startTimerTask();
    }

    private void stopTimerTask() {
        task.cancel();
        task = null;
    }

}
