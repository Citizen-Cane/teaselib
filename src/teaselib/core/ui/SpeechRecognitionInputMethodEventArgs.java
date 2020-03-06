package teaselib.core.ui;

import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

public class SpeechRecognitionInputMethodEventArgs extends InputMethodEventArgs {
    public final SpeechRecognizedEventArgs eventArgs;

    public SpeechRecognitionInputMethodEventArgs(InputMethod.Notification source, SpeechRecognizedEventArgs eventArgs) {
        super(source);
        this.eventArgs = eventArgs;
    }

}
