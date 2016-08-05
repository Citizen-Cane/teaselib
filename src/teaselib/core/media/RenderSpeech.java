package teaselib.core.media;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Config;
import teaselib.TeaseLib;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognizer;

public abstract class RenderSpeech extends MediaRendererThread {
    private static final Logger logger = LoggerFactory
            .getLogger(RenderSpeech.class);

    private final long pauseMillis;

    public RenderSpeech(long pauseMillis, TeaseLib teaseLib) {
        super(teaseLib);
        this.pauseMillis = pauseMillis;
    }

    @Override
    public final void renderMedia() {
        logger.info(this.getClass().getSimpleName() + ": " + toString());
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
        logger.info(
                this.getClass().getSimpleName() + ": " + text + " completed");
    }

    protected abstract void renderSpeech() throws IOException;
}
