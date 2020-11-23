package teaselib.core.speechrecognition;

import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.InputMethodEventArgs;

public class SpeechRecognitionInputMethodEventArgs extends InputMethodEventArgs {
    public final SpeechRecognizedEventArgs eventArgs;

    public SpeechRecognitionInputMethodEventArgs(InputMethod.Notification source, SpeechRecognizedEventArgs eventArgs) {
        super(source);
        this.eventArgs = eventArgs;
    }

}
