package teaselib.core.texttospeech;

import java.util.Map;

import teaselib.TeaseLib;

public abstract class TextToSpeechImplementation {
    public abstract void getVoices(Map<String, Voice> voices);

    public abstract void setVoice(Voice voice);

    public abstract void speak(String prompt);

    public abstract String speak(String prompt, String wav);

    /**
     * Stop current speech (if any)
     */
    public abstract void stop();

    public abstract void dispose();

    protected String[] hints = null;

    public void setHints(String... hints) {
        this.hints = hints;
    }

    public String[] getHints() {
        return hints;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            dispose();
        } catch (Throwable t) {
            TeaseLib.log(this, t);
        }
        super.finalize();
    }
}