package teaselib.texttospeech.implementation;

import java.util.Map;

import teaselib.texttospeech.TextToSpeechImplementation;
import teaselib.texttospeech.Voice;

public class TeaseLibTTS extends TextToSpeechImplementation {

    private long nativeObject;

    public TeaseLibTTS() throws UnsatisfiedLinkError {
        teaselib.util.jni.LibraryLoader.load("TeaseLibTTS");
    }

    public static native void getInstalledVoices(Map<String, Voice> voices);

    @Override
    public void getVoices(Map<String, Voice> voices) {
        TeaseLibTTS.getInstalledVoices(voices);
    }

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
