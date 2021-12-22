package teaselib.core.ui;

import teaselib.core.events.EventArgs;
import teaselib.core.ui.InputMethod.Notification;

public class InputMethodEventArgs extends EventArgs {

    public static final InputMethodEventArgs None = new InputMethodEventArgs(null);

    public final InputMethod.Notification source;

    public InputMethodEventArgs(Notification source) {
        super();
        this.source = source;
    }

}
