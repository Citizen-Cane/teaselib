package teaselib.core;

import teaselib.core.events.EventSource;

public class ScriptEvents {
    public final EventSource<ScriptEventArgs> beforeChoices = new EventSource<>("Before Choices");
    public final EventSource<ScriptEventArgs> afterChoices = new EventSource<>("After Choices");
}