package teaselib.core.media;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.TeaseLib;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.util.ExceptionUtil;

public abstract class RenderSpeech extends MediaRendererThread {
    private static final Logger logger = LoggerFactory.getLogger(RenderSpeech.class);

    private final long pauseMillis;

    public RenderSpeech(long pauseMillis, TeaseLib teaseLib) {
        super(teaseLib);
        this.pauseMillis = pauseMillis;

        TextToSpeechPlayer textToSpeech = teaseLib.globals.get(TextToSpeechPlayer.class);
        textToSpeech.load();
    }

    @Override
    public final void renderMedia() throws IOException, InterruptedException {
        logger.info(this + " started");
        // Suspend speech recognition while speaking, to avoid wrong
        // recognitions - and the mistress speech isn't to be interrupted anyway
        SpeechRecognition.completeSpeechRecognitionInProgress();
        Runnable resumeSpeechRecognition = SpeechRecognizer.instance.pauseRecognition();
        startCompleted();
        try {
            try {
                renderSpeech();
            } catch (IOException e) {
                handleIOException(ExceptionUtil.reduce(e));
            } finally {
                mandatoryCompleted();
                resumeSpeechRecognition.run();
            }
            teaseLib.sleep(pauseMillis, TimeUnit.MILLISECONDS);
        } finally {
            allCompleted();
        }

        logger.info(this + " completed");
    }

    protected abstract void renderSpeech() throws IOException, InterruptedException;
}
