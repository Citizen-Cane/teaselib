package teaselib.core;

import java.io.IOException;

import teaselib.Actor;
import teaselib.TeaseLib;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public class RenderTTSSpeech extends RenderSpeech {
    private final TextToSpeechPlayer ttsPlayer;
    private final String prompt;
    private final String mood;

    public RenderTTSSpeech(TextToSpeechPlayer ttsPlayer, Actor actor,
            String prompt, String mood, long pauseMillis, TeaseLib teaseLib) {
        super(actor, pauseMillis, teaseLib);
        this.ttsPlayer = ttsPlayer;
        this.prompt = prompt;
        this.mood = mood;
    }

    @Override
    protected void renderSpeech() throws IOException {
        ttsPlayer.speak(actor, prompt, mood);
    }

    @Override
    public String toString() {
        String text = "\"" + (prompt.length() > 20
                ? prompt.substring(0, 20) + "..." : prompt) + "\"";
        return text;
    }

    @Override
    public void interrupt() {
        if (ttsPlayer != null) {
            ttsPlayer.stop();
        }
        super.interrupt();
    }
}
