package teaselib.core.media;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptRenderer;
import teaselib.core.TeaseLib;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.ui.InputMethods;

public abstract class RenderSpeech extends MediaRendererThread {
    private static final Logger logger = LoggerFactory.getLogger(RenderSpeech.class);

    public RenderSpeech(TeaseLib teaseLib) {
        super(teaseLib);
    }

    @Override
    public final void renderMedia() throws IOException, InterruptedException {
        logger.info("{} started", this);
        // Suspend speech recognition while speaking, to avoid wrong
        // recognitions - and the mistress speech isn't to be interrupted anyway

        ScriptRenderer scriptRenderer = teaseLib.globals.get(ScriptRenderer.class);
        scriptRenderer.audioSync.completeSpeechRecognition();
        startCompleted();

        InputMethods inputMethods = teaseLib.globals.get(InputMethods.class);
        SpeechRecognitionInputMethod inputMethod = inputMethods.get(SpeechRecognitionInputMethod.class);
        try (SpeechRecognitionInputMethod.ResumeRecognition closeable = inputMethod.pauseRecognition()) {
            renderSpeech();
            logger.info("{} completed", this);
        } finally {
            mandatoryCompleted();
        }
    }

    protected abstract void renderSpeech() throws IOException, InterruptedException;
}
