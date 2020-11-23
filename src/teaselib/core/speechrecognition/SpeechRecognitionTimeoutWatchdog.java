package teaselib.core.speechrecognition;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

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
class SpeechRecognitionTimeoutWatchdog {
    static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionTimeoutWatchdog.class);

    static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(2);

    private final Runnable timeoutAction;

    private boolean enabled = false;
    private Timer timer = null;

    public SpeechRecognitionTimeoutWatchdog(Runnable timeoutAction) {
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

    public void add(SpeechRecognitionEvents events) {
        events.recognitionStarted.add(startWatching);
        events.audioSignalProblemOccured.add(updateAudioProblemStatus);
        events.speechDetected.add(updateSpeechDetectionStatus);
        events.recognitionRejected.add(stopWatching);
        events.recognitionCompleted.add(stopWatching);
    }

    public void remove(SpeechRecognitionEvents events) {
        events.recognitionStarted.remove(startWatching);
        events.audioSignalProblemOccured.remove(updateAudioProblemStatus);
        events.speechDetected.add(updateSpeechDetectionStatus);
        events.recognitionRejected.remove(stopWatching);
        events.recognitionCompleted.remove(stopWatching);
    }

    public boolean enabled() {
        return enabled;
    }

    public void enable(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            stopRecognitionTimeout();
        }
    }

    private Event<SpeechRecognitionStartedEventArgs> startWatching = args -> {
        if (enabled()) {
            stopRecognitionTimeout();
            startRecognitionTimeout();
            logger.info("Started");
        }
    };

    private Event<AudioSignalProblemOccuredEventArgs> updateAudioProblemStatus = args -> {
        if (enabled()) {
            // Don't restart timer to avoid long delay after last speechDetected event
        }
    };

    private Event<SpeechRecognizedEventArgs> updateSpeechDetectionStatus = args -> {
        if (enabled()) {
            restartRecognitionTimout();
            logger.debug("Restarted");
        }
    };

    private Event<SpeechRecognizedEventArgs> stopWatching = args -> {
        if (enabled()) {
            stopRecognitionTimeout();
            logger.info("Stopped");
        }
    };

    private void startRecognitionTimeout() {
        timer = new Timer(getClass().getSimpleName());
        timer.schedule(timerTask(timeoutAction), TIMEOUT_MILLIS);
    }

    private void restartRecognitionTimout() {
        stopRecognitionTimeout();
        startRecognitionTimeout();
    }

    private void stopRecognitionTimeout() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

}
