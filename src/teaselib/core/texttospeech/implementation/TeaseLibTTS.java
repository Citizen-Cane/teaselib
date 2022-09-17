package teaselib.core.texttospeech.implementation;

import static teaselib.core.jni.NativeLibraries.*;

import java.util.List;

import teaselib.core.jni.NativeLibraries;
import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.ExceptionUtil;

public abstract class TeaseLibTTS extends TextToSpeechImplementation {

    // TODO refactor TextToSpeechImplementation to interface in order to derive from NativeObject
    private long nativeObject;

    private TeaseLibTTS(long nativeObject) {
        this.nativeObject = nativeObject;
    }

    private static native long newNativeInstance(String sdk);

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

    public static class Loquendo extends TeaseLibTTS {

        private static final String LTTS7 = "LTTS7";

        public static TextToSpeechImplementation newInstance() {
            try {
                NativeLibraries.require(TEASELIB, TEASELIB_TTS);
                return new TeaseLibTTS.Loquendo(newNativeInstance(LTTS7));
            } catch (UnsupportedOperationException e) {
                return Unsupported.Instance;
            }
        }

        public Loquendo(long nativeObject) {
            super(nativeObject);
        }

        @Override
        public String sdkName() {
            return LTTS7;
        }

        @Override
        public String phonemeAlphabetName() {
            return IPA;
        }

    }

    public static class Microsoft extends TeaseLibTTS {

        private static final String SAPI = "SAPI";

        public static TextToSpeechImplementation newInstance() {
            try {
                NativeLibraries.require(TEASELIB, TEASELIB_TTS);
                return new TeaseLibTTS.Microsoft(newNativeInstance(SAPI));
            } catch (UnsatisfiedLinkError e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
        }

        public Microsoft(long nativeObject) {
            super(nativeObject);
        }

        @Override
        public String sdkName() {
            return SAPI;
        }

        @Override
        public String phonemeAlphabetName() {
            return UPS;
        }

    }

}
