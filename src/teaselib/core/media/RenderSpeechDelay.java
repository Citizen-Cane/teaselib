package teaselib.core.media;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.TextToSpeech;

final class RenderSpeechDelay extends RenderSpeech {
    private final String prompt;

    RenderSpeechDelay(long pauseMillis, TeaseLib teaseLib, String prompt) {
        super(pauseMillis, teaseLib);
        this.prompt = prompt;
    }

    @Override
    protected void renderSpeech() throws IOException, InterruptedException {
        teaseLib.sleep(TextToSpeech.getEstimatedSpeechDuration(prompt), TimeUnit.MILLISECONDS);
    }
}