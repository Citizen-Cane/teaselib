package teaselib.core.media;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.TeaseLib;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognizer;

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
        SpeechRecognition.completeSpeechRecognitionInProgress();
        Runnable resumeSpeechRecognition = teaseLib.globals.get(SpeechRecognizer.class).pauseRecognition();
        startCompleted();
        try {
            try {
                renderSpeech();
            } catch (IOException e) {
                handleIOException(e);
            } finally {
                mandatoryCompleted();
                resumeSpeechRecognition.run();
            }
        } finally {
            allCompleted();
        }

        logger.info("{} completed", this);
    }

    protected abstract void renderSpeech() throws IOException, InterruptedException;
}
