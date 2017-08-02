package teaselib.core.speechrecognition;

import java.util.Arrays;
import java.util.Locale;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

public class SpeechRecognitionTests {
    private static final Logger logger = LoggerFactory
            .getLogger(SpeechRecognitionTests.class);

    private final Object sync = new Object();
    private Confidence confidence = Confidence.High;

    private boolean enableSpeechHypothesisHandler = false;

    @Test
    public void test() throws Exception {
        if (enableSpeechHypothesisHandler) {
            System.setProperty(SpeechRecognition.EnableSpeechHypothesisHandlerGlobally,
                    Boolean.toString(true));
        }
        SpeechRecognition sr = new SpeechRecognition(new Locale("en-us"));

        Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;
        recognitionCompleted = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs) {
                for (SpeechRecognitionResult result : eventArgs.result) {
                    if (confidenceIsHighEnough(result, confidence)) {
                        logger.info("Recognized prompt: " + result.text);
                        synchronized (sync) {
                            sync.notify();
                        }
                    } else {
                        logger.info("Dropping result '" + result.toString()
                                + "' due to lack of confidence (Confidence="
                                + confidence + " expected)");
                    }
                }
            }

            private boolean confidenceIsHighEnough(
                    SpeechRecognitionResult result, Confidence confidence) {
                return result.confidence.probability >= confidence.probability;
            }
        };

        sr.events.recognitionCompleted.add(recognitionCompleted);
        sr.startRecognition(Arrays.asList("I have a dream"), confidence);
        synchronized (sync) {
            sync.wait();
        }
        sr.stopRecognition();
    }
}
