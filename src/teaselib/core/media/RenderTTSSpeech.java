package teaselib.core.media;

import teaselib.Actor;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public class RenderTTSSpeech extends RenderSpeech {
    private final TextToSpeechPlayer ttsPlayer;

    protected final Actor actor;
    private final String prompt;
    private final String mood;

    public RenderTTSSpeech(TextToSpeechPlayer ttsPlayer, Actor actor, String prompt, String mood, long pauseMillis,
            TeaseLib teaseLib) {
        super(pauseMillis, teaseLib);
        this.ttsPlayer = ttsPlayer;
        this.actor = actor;
        this.prompt = prompt;
        this.mood = mood;
    }

    @Override
    protected void renderSpeech() throws InterruptedException {
        ttsPlayer.speak(actor, prompt, mood);
    }

    @Override
    public String toString() {
        return "\"" + (prompt.length() > 20 ? prompt.substring(0, 20) + "..." : prompt) + "\"";
    }

    @Override
    public void interrupt() {
        if (ttsPlayer != null) {
            ttsPlayer.stop(actor);
        }
        super.interrupt();
    }
}
