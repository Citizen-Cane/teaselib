package teaselib.core.texttospeech.implementation;

import java.util.List;

import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;

public class TeaseLibTTS extends TextToSpeechImplementation {
    private static TeaseLibTTS instance = null;

    public static synchronized TeaseLibTTS getInstance() {
        if (instance == null) {
            teaselib.core.jni.LibraryLoader.load("TeaseLibTTS");
            instance = new TeaseLibTTS(newNativeInstance());
        }
        return instance;
    }

    // TODO refactor TextToSpeechImplementation to interface in order to derive from NativeObject
    private long nativeObject;

    private TeaseLibTTS(long nativeObject) {
        this.nativeObject = nativeObject;
    }

    private static native long newNativeInstance();

    @Override
    public String sdkName() {
        return "sapi";
    }

    @Override
    public String phonemeAlphabetName() {
        return UPS;
    }

    @Override
    public native void addLexiconEntry(String locale, String word, int partOfSpeech, String pronunciation);

    @Override
    public native List<Voice> getVoices();

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
