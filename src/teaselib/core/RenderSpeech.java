package teaselib.core;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import teaselib.Actor;
import teaselib.Config;
import teaselib.TeaseLib;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognizer;

public abstract class RenderSpeech extends MediaRendererThread {
    protected final Actor actor;
    private final long pauseMillis;

    public RenderSpeech(Actor actor, long pauseMillis, TeaseLib teaseLib) {
        super(teaseLib);
        this.actor = actor;
        this.pauseMillis = pauseMillis;
    }

    @Override
    public final void renderMedia() {
        teaseLib.log.info(this.getClass().getSimpleName() + ": " + toString());
        // Suspend speech recognition while speaking,
        // to avoid wrong recognitions
        // - and the mistress speech isn't to be interrupted anyway
        // TODO Pause any recognition, not necessarily the one with the
        // actor's locale
        final SpeechRecognition speechRecognizer = SpeechRecognizer.instance
                .get(actor.locale);
        SpeechRecognition.completeSpeechRecognitionInProgress();
        boolean reactivateSpeechRecognition = speechRecognizer != null
                && speechRecognizer.isActive();
        if (reactivateSpeechRecognition) {
            if (speechRecognizer != null) {
                speechRecognizer.stopRecognition();
            }
        }
        startCompleted();
        try {
            renderSpeech();
            mandatoryCompleted();
            teaseLib.sleep(pauseMillis, TimeUnit.MILLISECONDS);
        } catch (ScriptInterruptedException e) {
            // Expected
        } catch (IOException e) {
            if (!teaseLib.getBoolean(Config.Namespace,
                    Config.Debug.IgnoreMissingResources)) {
            }
        } finally {
            allCompleted();
            // resume SR if necessary
            if (reactivateSpeechRecognition) {
                if (speechRecognizer != null) {
                    speechRecognizer.resumeRecognition();
                }
            }
        }
        String text = toString();
        teaseLib.log.info(
                this.getClass().getSimpleName() + ": " + text + " completed");
    }

    protected abstract void renderSpeech() throws IOException;
}
