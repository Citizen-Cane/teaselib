package teaselib.core.events;

public abstract class EventArgs {
    /**
     * Set to true if the event should be consumed. If a event is consumed,
     * additional listeners are not notified about this event. This is necessary
     * to turn hypotized recognitions into a proper completion event.
     * 
     * Not very cooperative, so this should be used with care.
     */
    public boolean consumed = false;
}
