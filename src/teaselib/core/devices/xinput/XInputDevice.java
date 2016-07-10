package teaselib.core.devices.xinput;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceCache.Factory;
import teaselib.core.jni.LibraryLoader;

/**
 * Represents all XInput devices registered in the system.
 *
 * @author Citizen-Cane, Based on work by Ivan "StrikerX3" Oliveira
 * @see XInputComponents
 * @see XInputComponentsDelta
 */
public class XInputDevice implements Device {
    private static final String DeviceClassName = "XInput360GameController";

    static final DeviceCache.Factory<XInputDevice> Factory = new Factory<XInputDevice>() {
        @Override
        public String getDeviceClass() {
            return XInputDevice.DeviceClassName;
        }

        @Override
        public List<String> getDevices() {
            List<String> deviceNames = new ArrayList<String>();
            deviceNames.addAll(XInputDevice.getDevicePaths());
            return deviceNames;
        }

        @Override
        public XInputDevice getDevice(String path) {
            return XInputDevice.getDeviceFor(
                    Integer.parseInt(DeviceCache.getDeviceName(path)));
        }
    };

    private final int playerNum;
    private final ByteBuffer buffer; // Contains the XINPUT_STATE struct
    private final XInputComponents lastComponents;
    private final XInputComponents components;
    private final XInputComponentsDelta delta;

    private boolean lastConnected;
    private boolean connected;

    private final List<XInputDeviceListener> listeners;

    public static final int MAX_PLAYERS = 4;
    public static final int VIBRATION_MIN_VALUE = 0;
    public static final int VIBRATION_MAX_VALUE = 65535;

    private static boolean libLoaded = false;
    private static final XInputDevice[] DEVICES = new XInputDevice[MAX_PLAYERS];

    /**
     * @return All connected devices
     */
    public static List<String> getDevicePaths() {
        List<String> devicePaths = new ArrayList<String>(4);
        for (int i = 0; i < XInputDevice.MAX_PLAYERS; i++) {
            if (XInputDevice.getDeviceFor(i).active()) {
                devicePaths.add(DeviceCache.createDevicePath(
                        XInputDevice.DeviceClassName, Integer.toString(i)));
            }
        }
        return devicePaths;
    }

    /**
     * Returns the XInput device for the specified player.
     *
     * @param playerNum
     *            the player number
     * @return the XInput device for the specified player
     */
    static XInputDevice getDeviceFor(int playerNum) {
        if (playerNum < 0 || playerNum >= MAX_PLAYERS) {
            throw new IllegalArgumentException(
                    "Invalid player number: " + playerNum
                            + ". Must be between 0 and " + (MAX_PLAYERS - 1));
        }
        if (!libLoaded) {
            LibraryLoader.load("TeaseLibx360c");
            for (int i = 0; i < MAX_PLAYERS; i++) {
                DEVICES[i] = new XInputDevice(i);
            }
        }
        return DEVICES[playerNum];
    }

    private XInputDevice(final int playerNum) {
        this.playerNum = playerNum;
        buffer = ByteBuffer.allocateDirect(16); // sizeof(XINPUT_STATE)
        buffer.order(ByteOrder.nativeOrder());

        lastComponents = new XInputComponents();
        components = new XInputComponents();
        delta = new XInputComponentsDelta(lastComponents, components);
        listeners = new LinkedList<XInputDeviceListener>();
        poll();
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                Integer.toString(playerNum));
    }

    @Override
    public String getName() {
        return "XInput Game Controller #" + getPlayerNum();
    }

    /**
     * Returns a boolean indicating whether this device is connected.
     *
     * @return <code>true</code> if the device is connected, <code>false</code>
     *         otherwise
     */
    @Override
    public boolean active() {
        return connected;
    }

    @Override
    public void release() {
    }

    /**
     * Adds an event listener that will react to changes in the input.
     *
     * @param listener
     *            the listener
     */
    public void addListener(final XInputDeviceListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a registered event listener
     *
     * @param listener
     *            the listener
     */
    public void removeListener(final XInputDeviceListener listener) {
        listeners.remove(listener);
    }

    /**
     * Reads input from the device and updates components.
     *
     * @return <code>false</code> if the device was not connected
     */
    public boolean poll() {
        final int ret = pollDevice(playerNum, buffer);
        if (ret == ERROR_DEVICE_NOT_CONNECTED) {
            setConnected(false);
            return false;
        }
        if (ret != ERROR_SUCCESS) {
            setConnected(false);
            throw new Error("Could not read controller state: " + ret);
        }
        setConnected(true);

        // typedef struct _XINPUT_STATE
        // {
        // DWORD dwPacketNumber;
        // XINPUT_GAMEPAD Gamepad;
        // } XINPUT_STATE, *PXINPUT_STATE;

        // typedef struct _XINPUT_GAMEPAD
        // {
        // WORD wButtons;
        // BYTE bLeftTrigger;
        // BYTE bRightTrigger;
        // SHORT sThumbLX;
        // SHORT sThumbLY;
        // SHORT sThumbRX;
        // SHORT sThumbRY;
        // } XINPUT_GAMEPAD, *PXINPUT_GAMEPAD;

        /* int packetNumber = */buffer.getInt(); // can be safely ignored
        final short btns = buffer.getShort();
        final byte leftTrigger = buffer.get();
        final byte rightTrigger = buffer.get();
        final short thumbLX = buffer.getShort();
        final short thumbLY = buffer.getShort();
        final short thumbRX = buffer.getShort();
        final short thumbRY = buffer.getShort();
        buffer.flip();

        lastComponents.copy(components);

        final boolean up = (btns & XINPUT_GAMEPAD_DPAD_UP) != 0;
        final boolean down = (btns & XINPUT_GAMEPAD_DPAD_DOWN) != 0;
        final boolean left = (btns & XINPUT_GAMEPAD_DPAD_LEFT) != 0;
        final boolean right = (btns & XINPUT_GAMEPAD_DPAD_RIGHT) != 0;

        final XInputAxes axes = components.getAxes();
        axes.lx = thumbLX / 32768f;
        axes.ly = thumbLY / 32768f;
        axes.rx = thumbRX / 32768f;
        axes.ry = thumbRY / 32768f;
        axes.lt = (leftTrigger & 0xff) / 255f;
        axes.rt = (rightTrigger & 0xff) / 255f;
        axes.dpad = XInputAxes.dpadFromButtons(up, down, left, right);

        final XInputButtons buttons = components.getButtons();
        buttons.a = (btns & XINPUT_GAMEPAD_A) != 0;
        buttons.b = (btns & XINPUT_GAMEPAD_B) != 0;
        buttons.x = (btns & XINPUT_GAMEPAD_X) != 0;
        buttons.y = (btns & XINPUT_GAMEPAD_Y) != 0;
        buttons.back = (btns & XINPUT_GAMEPAD_BACK) != 0;
        buttons.start = (btns & XINPUT_GAMEPAD_START) != 0;
        buttons.guide = (btns & XINPUT_GAMEPAD_GUIDE) != 0;
        buttons.lShoulder = (btns & XINPUT_GAMEPAD_LEFT_SHOULDER) != 0;
        buttons.rShoulder = (btns & XINPUT_GAMEPAD_RIGHT_SHOULDER) != 0;
        buttons.lThumb = (btns & XINPUT_GAMEPAD_LEFT_THUMB) != 0;
        buttons.rThumb = (btns & XINPUT_GAMEPAD_RIGHT_THUMB) != 0;
        buttons.up = up;
        buttons.down = down;
        buttons.left = left;
        buttons.right = right;

        processDelta();
        return true;
    }

    private void setConnected(final boolean state) {
        lastConnected = connected;
        connected = state;
        for (final XInputDeviceListener listener : listeners) {
            if (connected && !lastConnected) {
                listener.connected();
            } else if (!connected && lastConnected) {
                listener.disconnected();
            }
        }
    }

    private void processDelta() {
        final XInputButtonsDelta buttons = delta.getButtons();
        final XInputAxesDelta axes = delta.getAxes();
        for (final XInputDeviceListener listener : listeners) {
            for (final XInputButton button : XInputButton.values()) {
                if (buttons.isPressed(button)) {
                    listener.buttonChanged(button, true);
                } else if (buttons.isReleased(button)) {
                    listener.buttonChanged(button, false);
                }
            }
            for (final XInputAxis axis : XInputAxis.values()) {
                final float delta = axes.getDelta(axis);
                if (delta != 0f) {
                    listener.axisChanged(axis, components.getAxes().get(axis),
                            delta);
                }
            }
        }
    }

    /**
     * Sets the vibration of the controller. Returns <code>false</code> if the
     * device was not connected.
     *
     * @param leftMotor
     *            the left motor speed
     * @param rightMotor
     *            the right motor speed
     * @return <code>false</code> if the device was not connected
     */
    public boolean setVibration(int leftMotor, int rightMotor) {
        return setVibration(playerNum, clampVibrationValue(leftMotor),
                clampVibrationValue(rightMotor)) != ERROR_DEVICE_NOT_CONNECTED;
    }

    private static short clampVibrationValue(int value) {
        if (value < VIBRATION_MIN_VALUE)
            return (short) VIBRATION_MIN_VALUE;
        else if (value > VIBRATION_MAX_VALUE)
            return (short) VIBRATION_MAX_VALUE;
        else
            return (short) value;
    }

    /**
     * Shutdown the device completely. First turn off vibration, then shutdown
     * wireless device to save battery power.
     * 
     * @return Whether shutting down the device was successful.
     */
    public boolean shutdown() {
        return shutdownDevice(playerNum);
    }

    /**
     * Returns the state of the Xbox 360 controller components before the last
     * poll.
     *
     * @return the state of the Xbox 360 controller components before the last
     *         poll.
     */
    public XInputComponents getLastComponents() {
        return lastComponents;
    }

    /**
     * Returns the state of the Xbox 360 controller components at the last poll.
     *
     * @return the state of the Xbox 360 controller components at the last poll.
     */
    public XInputComponents getComponents() {
        return components;
    }

    /**
     * Returns the difference between the last two states of the Xbox 360
     * controller components.
     *
     * @return the difference between the last two states of the Xbox 360
     *         controller components.
     */
    public XInputComponentsDelta getDelta() {
        return delta;
    }

    /**
     * Returns the player number that this device represents.
     *
     * @return the player number that this device represents.
     */
    public int getPlayerNum() {
        return playerNum;
    }

    private static native int pollDevice(int playerNum, ByteBuffer data);

    private static native int setVibration(int playerNum, int leftMotor,
            int rightMotor);

    private static native boolean shutdownDevice(int playerNum);

    // Xbox 360 controller button masks
    private static final short XINPUT_GAMEPAD_DPAD_UP = 0x0001;
    private static final short XINPUT_GAMEPAD_DPAD_DOWN = 0x0002;
    private static final short XINPUT_GAMEPAD_DPAD_LEFT = 0x0004;
    private static final short XINPUT_GAMEPAD_DPAD_RIGHT = 0x0008;
    private static final short XINPUT_GAMEPAD_START = 0x0010;
    private static final short XINPUT_GAMEPAD_BACK = 0x0020;
    private static final short XINPUT_GAMEPAD_LEFT_THUMB = 0x0040;
    private static final short XINPUT_GAMEPAD_RIGHT_THUMB = 0x0080;
    private static final short XINPUT_GAMEPAD_LEFT_SHOULDER = 0x0100;
    private static final short XINPUT_GAMEPAD_RIGHT_SHOULDER = 0x0200;
    private static final short XINPUT_GAMEPAD_GUIDE = 0x0400;
    private static final short XINPUT_GAMEPAD_A = 0x1000;
    private static final short XINPUT_GAMEPAD_B = 0x2000;
    private static final short XINPUT_GAMEPAD_X = 0x4000;
    private static final short XINPUT_GAMEPAD_Y = (short) 0x8000;
    // Windows error codes
    private static final int ERROR_SUCCESS = 0;
    private static final int ERROR_DEVICE_NOT_CONNECTED = 1167;
}
