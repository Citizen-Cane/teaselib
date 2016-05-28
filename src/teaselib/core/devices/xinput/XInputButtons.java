package teaselib.core.devices.xinput;

/**
 * Contains the states of all XInput buttons.
 * 
 * @author Ivan "StrikerX3" Oliveira
 */
public class XInputButtons {
    public boolean a, b, x, y;
    public boolean back, start;
    public boolean lShoulder, rShoulder;
    public boolean lThumb, rThumb;
    public boolean up, down, left, right;

    protected XInputButtons() {
        reset();
    }

    /**
     * Resets the state of all buttons.
     */
    protected void reset() {
        a = b = x = y = false;
        back = start = false;
        lShoulder = rShoulder = false;
        lThumb = rThumb = false;
        up = down = left = right = false;
    }

    /**
     * Copies the state of all buttons from the specified state.
     * 
     * @param buttons
     *            the state to copy from
     */
    protected void copy(final XInputButtons buttons) {
        a = buttons.a;
        b = buttons.b;
        x = buttons.x;
        y = buttons.y;

        back = buttons.back;
        start = buttons.start;

        lShoulder = buttons.lShoulder;
        rShoulder = buttons.rShoulder;

        lThumb = buttons.lThumb;
        rThumb = buttons.rThumb;

        up = buttons.up;
        down = buttons.down;
        left = buttons.left;
        right = buttons.right;
    }

    @Override
    public String toString() {
        return "a=" + a + ", b=" + b + ", x=" + x + ", y=" + y + ", back="
                + back + ", start=" + start + ", lShoulder=" + lShoulder
                + ", rShoulder=" + rShoulder + ", lThumb=" + lThumb
                + ", rThumb=" + rThumb + ", up=" + up + ", down=" + down
                + ", left=" + left + ", right=" + right;
    }
}
