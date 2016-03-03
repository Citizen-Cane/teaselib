package teaselib.core;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import teaselib.Config;
import teaselib.TeaseLib;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognizer;

public abstract class RenderSpeech extends MediaRendererThread {
    private final long pauseMillis;

    public RenderSpeech(long pauseMillis, TeaseLib teaseLib) {
        super(teaseLib);
        this.pauseMillis = pauseMillis;
    }

    @Override
    public final void renderMedia() {
        teaseLib.log.info(this.getClass().getSimpleName() + ": " + toString());
        // Suspend speech recognition while speaking,
        // to avoid wrong recognitions
        // - and the mistress speech isn't to be interrupted anyway
        SpeechRecognition.completeSpeechRecognitionInProgress();
        Runnable resumeSpeechRecognition = SpeechRecognizer.instance
                .pauseRecognition();
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
            resumeSpeechRecognition.run();
        }
        String text = toString();
        teaseLib.log.info(
                this.getClass().getSimpleName() + ": " + text + " completed");
    }

    protected abstract void renderSpeech() throws IOException;
}
