package teaselib.core.speechrecognition;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionTimeoutWatchdog {
    static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionTimeoutWatchdog.class);

    static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(2);

    private final SpeechRecognitionEvents events;
    private final Consumer<SpeechRecognitionEvents> timeoutAction;

    private boolean enabled = false;
    private boolean recognitionStarted = false;
    private Timer timer = null;

    public SpeechRecognitionTimeoutWatchdog(SpeechRecognitionEvents events,
            Consumer<SpeechRecognitionEvents> timeoutAction) {
        this.events = events;
        this.timeoutAction = timeoutAction;
    }

    private TimerTask timerTask(Consumer<SpeechRecognitionEvents> timeoutAction) {
        return new TimerTask() {
            @Override
            public void run() {
                if (enabled()) {
                    logger.info("Timeout after {}ms", TIMEOUT_MILLIS);
                    timeoutAction.accept(events);
                }
            }
        };
    }

    public void addEvents() {
        events.recognitionStarted.add(startWatching);
        events.speechDetected.add(resetTimer);
        events.recognitionRejected.add(stopWatching);
        events.recognitionCompleted.add(stopWatching);
    }

    public void removeEvents() {
        events.recognitionStarted.remove(startWatching);
        events.speechDetected.add(resetTimer);
        events.recognitionRejected.remove(stopWatching);
        events.recognitionCompleted.remove(stopWatching);
    }

    public boolean enabled() {
        return enabled;
    }

    public void enable(boolean isEnabled) {
        this.enabled = isEnabled;
        if (!isEnabled) {
            stopRecognitionTimeout();
        }
    }

    private Event<SpeechRecognitionStartedEventArgs> startWatching = args -> {
        if (enabled()) {
            stopRecognitionTimeout();
            recognitionStarted = true;
            startRecognitionTimeout();
            logger.info("Started");
        }
    };

    private Event<SpeechRecognizedEventArgs> resetTimer = args -> {
        if (enabled()) {
            ensureFireRecognitionStartedEvent();
            restartRecognitionTimout();
            logger.debug("Restarted");
        }
    };

    private void ensureFireRecognitionStartedEvent() {
        if (!recognitionStarted) {
            recognitionStarted = true;
            events.recognitionStarted.fire(new SpeechRecognitionStartedEventArgs());
        }
    }

    private Event<SpeechRecognizedEventArgs> stopWatching = args -> {
        if (enabled()) {
            recognitionStarted = false;
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
