package teaselib.core.texttospeech.implementation;

import static teaselib.core.jni.NativeLibraries.TEASELIB_FRAMEWORK;
import static teaselib.core.jni.NativeLibraries.TEASELIB_TTS;

import java.util.List;

import teaselib.core.jni.NativeLibraries;
import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;

public class TeaseLibTTS extends TextToSpeechImplementation {

    public static TextToSpeechImplementation newInstance() {
        try {
            NativeLibraries.require(TEASELIB_FRAMEWORK, TEASELIB_TTS);
            return new TeaseLibTTS(newNativeInstance());
        } catch (UnsatisfiedLinkError e) {
            return Unsupported.Instance;
        }
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
