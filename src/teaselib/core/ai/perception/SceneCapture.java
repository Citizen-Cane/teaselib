package teaselib.core.ai.perception;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import teaselib.core.jni.NativeObject;
import teaselib.core.jni.NativeObjectList;

public class SceneCapture extends NativeObject.Disposible {
    public static final int NoImage = -1;

    public static final long DEVICE_POLL_DURATION_MILLIS = 5000;

    public final String name;
    public final EnclosureLocation location;

    public enum EnclosureLocation {
        Front,
        Rear,
        External
    }

    public enum Rotation {
        None,
        Clockwise,
        CounterClockwise,
        UpsideDown
    }

    public static class DeviceLost extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public DeviceLost(String message) {
            super(message);
        }

    }

    public static SceneCapture getDevice() throws InterruptedException {
        return awaitDevice(0, TimeUnit.MILLISECONDS);
    }

    public static SceneCapture awaitDevice() throws InterruptedException {
        return awaitDevice(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public static SceneCapture awaitDevice(long timeout, TimeUnit unit) throws InterruptedException {
        long durationMillis = TimeUnit.MILLISECONDS.convert(timeout, unit);
        while (durationMillis >= 0) {
            try (NativeObjectList<SceneCapture> devices = devices()) {
                if (devices.isEmpty()) {
                    if (durationMillis > 0) {
                        long sleepMillis = Math.min(DEVICE_POLL_DURATION_MILLIS, durationMillis);
                        Thread.sleep(sleepMillis);
                        durationMillis -= sleepMillis;
                    } else {
                        break;
                    }
                } else {
                    Optional<SceneCapture> device = find(devices, EnclosureLocation.External);
                    if (device.isEmpty()) {
                        device = find(devices, EnclosureLocation.Front);
                    }
                    if (device.isPresent()) {
                        SceneCapture captureDevice = device.get();
                        devices.remove(captureDevice);
                        return captureDevice;
                    }
                }
            }
        }
        return null;
    }

    private static Optional<SceneCapture> find(List<SceneCapture> sceneCaptures, EnclosureLocation location) {
        return sceneCaptures.stream().filter(s -> s.location == location).findFirst();
    }

    public static native NativeObjectList<SceneCapture> devices();

    SceneCapture(long nativeObject, String name, EnclosureLocation location) {
        super(nativeObject);
        this.name = name;
        this.location = location;
    }

    public SceneCapture(String openCVImagePattern) {
        super(newNativeInstance(openCVImagePattern));
        this.name = openCVImagePattern;
        this.location = EnclosureLocation.External;
    }

    private static native long newNativeInstance(String openCVImagePattern);

    public native void start();

    public native boolean isStarted();

    public native void stop();

    public Rotation rotation() {
        // TODO get device orientation and replace prototype with production code
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        boolean portrait = screenSize.width < screenSize.height;
        // The native code only understands rotation != null and rotates clockwise
        return portrait ? Rotation.Clockwise : null;
    }

    @Override
    protected native void dispose();

    @Override
    public void close() {
        if (isStarted()) {
            stop();
        }
        super.close();
    }

}
