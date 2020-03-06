package teaselib.core.ui;

import teaselib.core.events.EventArgs;
import teaselib.core.ui.InputMethod.Notification;

public class InputMethodEventArgs extends EventArgs {
    public final InputMethod.Notification source;

    public InputMethodEventArgs(Notification source) {
        super();
        this.source = source;
    }

}
