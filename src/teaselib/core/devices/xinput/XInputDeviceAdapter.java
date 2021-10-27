package teaselib.core.devices.xinput;

/**
 * Provides empty implementations of all {@link XInputDeviceListener} methods for easier subclassing.
 *
 * @author Ivan "StrikerX3" Oliveira
 */
public class XInputDeviceAdapter implements XInputDeviceListener {

    @Override
    public void connected() { //
    }

    @Override
    public void disconnected() { //
    }

    @Override
    public void buttonChanged(XInputButton button, boolean pressed) { //
    }

    @Override
    public void axisChanged(XInputAxis axis, float value, float delta) { //
    }

}
