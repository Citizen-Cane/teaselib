package teaselib.motiondetection;

import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.motiondetection.MotionDetector.Presence;

public class testMotionDetectionNew {

    @Test
    public void testMotionStartStop() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());

        // String devicePath = VideoCaptureDeviceFactory.Instance
        // .createDevicePath(VideoCaptureDeviceCV.DeviceClassName, "0");
        // VideoCaptureDevice videoCaptureDevice =
        // VideoCaptureDeviceFactory.Instance
        // .getDevice(devicePath);
        // MotionDetector md = new MotionDetectorJavaCV(videoCaptureDevice);

        MotionDetector md = MotionDetectorFactory.Instance.getDefaultDevice();
        md.setSensitivity(MotionSensitivity.Normal);

        System.out.println("Move!");
        final double amount = 1.0;
        while (!md.awaitChange(amount, Presence.Motion, 1.0, 5.0)) {
            System.out.println("I said 'Move'!");
        }

        System.out.println("Keep moving!");
        while (true) {
            // Triggers after not moving for one second
            if (md.awaitChange(amount, Presence.NoMotion, 1.0, 10.0)) {
                System.out.println("I said 'Keep moving'!");
                // use the timeout to avoid repeating the message too fast
                md.awaitChange(amount, Presence.Motion, 1.0, 5.0);
            } else {
                break;
            }
        }

        System.out.println("Stop!");
        while (!md.awaitChange(amount, Presence.NoMotion, 1.0, 5.0)) {
            System.out.println("I said 'Stop'!");
        }

        System.out.println("Now stay still.");
        // use the timeout to avoid repeating the message too fast
        while (true) {
            if (md.awaitChange(amount, Presence.Motion, 1.0, 5.0)) {
                System.out.println("I said 'Stay still'!");
                md.awaitChange(amount, Presence.NoMotion, 1.0, 2.0);
            } else {
                break;
            }
        }
        System.out.println("Very good.");
        md.awaitChange(amount, Presence.Motion, 1.0, Double.MAX_VALUE);
    }
}
