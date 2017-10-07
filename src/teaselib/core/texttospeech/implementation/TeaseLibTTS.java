package teaselib.core.texttospeech.implementation;

import java.util.Map;

import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;

public class TeaseLibTTS extends TextToSpeechImplementation {
    private static TeaseLibTTS instance = null;

    public static synchronized TeaseLibTTS getInstance() {
        if (instance == null) {
            instance = new TeaseLibTTS();
        }
        return instance;
    }

    private long nativeObject;

    public TeaseLibTTS() throws UnsatisfiedLinkError {
        teaselib.core.jni.LibraryLoader.load("TeaseLibTTS");
    }

    @Override
    public String sdkName() {
        return "sapi";
    }

    @Override
    public String phonemeAlphabetName() {
        return "ups";
    }

    @Override
    public native void addLexiconEntry(String locale, String word, String pronunciation);

    @Override
    public native void getVoices(Map<String, Voice> voices);

    @Override
    public native void setVoice(Voice voice);

    @Override
    public native void speak(String prompt);

    @Override
    public native String speak(String prompt, String wav);

    @Override
    public native void stop();

    @Override
    public native void dispose();
}
