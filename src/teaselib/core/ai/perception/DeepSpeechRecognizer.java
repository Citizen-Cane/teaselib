package teaselib.core.ai.perception;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionNativeImplementation;
import teaselib.core.ui.Choices;

public class DeepSpeechRecognizer extends SpeechRecognitionNativeImplementation {

    public DeepSpeechRecognizer(Locale locale, SpeechRecognitionEvents events) {
        super(init(languageCode(locale)), events);
    }

    protected static native long init(String langugeCode);

    @Override
    protected native void process(SpeechRecognitionEvents events, CountDownLatch signalInitialized);

    @Override
    public native String languageCode();

    @Override
    public PreparedChoices prepare(Choices choices) {
        throw new UnsupportedOperationException();
    }

    @Override
    public native void startRecognition();

    @Override
    public native void emulateRecognition(String emulatedRecognitionResult);

    @Override
    public native void stopRecognition();

    @Override
    public native void dispose();

}
