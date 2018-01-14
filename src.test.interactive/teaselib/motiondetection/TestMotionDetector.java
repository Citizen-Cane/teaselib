package teaselib.motiondetection;

import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.devices.Devices;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.test.DebugSetup;

public class TestMotionDetector {

    @Test
    public void testMotionStartStop() {
        // String devicePath = VideoCaptureDeviceFactory.Instance
        // .createDevicePath(VideoCaptureDeviceCV.DeviceClassName, "0");
        // VideoCaptureDevice videoCaptureDevice =
        // VideoCaptureDeviceFactory.Instance
        // .getDevice(devicePath);
        // MotionDetector md = new MotionDetectorJavaCV(videoCaptureDevice);

        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        MotionDetector md = devices.get(MotionDetector.class).getDefaultDevice();

        // using an amount < 1.0 ignores small pauses while moving
        // respectively ignores slight motions while standing still
        // Setting amount to 1.0 would make it very difficult to pass the test
        final double amount = 0.95;
        md.setSensitivity(MotionSensitivity.High);

        System.out.println("Move!");

        while (!md.await(amount, Presence.Motion, 1.0, 5.0)) {
            System.out.println("I said 'Move'!");
        }

        System.out.println("Keep moving!");
        while (true) {
            // Triggers after not moving for about one second
            if (md.await(amount, Presence.NoMotion, 1.0, 10.0)) {
                System.out.println("I said 'Keep moving'!");
                // Besides leaving the tester some time to move again
                // the timeout greater than the trigger time span
                // clears the history to avoid being triggered again
                // and repeating the message too fast
                md.await(amount, Presence.Motion, 1.0, 10.0);
            } else {
                break;
            }
        }

        System.out.println("Stop!");
        md.setSensitivity(MotionSensitivity.Low);
        while (!md.await(amount, Presence.NoMotion, 1.0, 5.0)) {
            System.out.println("I said 'Stop'!");
        }

        System.out.println("Now stay still.");
        // use the timeout to avoid repeating the message too fast
        while (true) {
            if (md.await(amount, Presence.Motion, 0.5, 10.0)) {
                System.out.println("I said 'Stay still'!");
                // Besides leaving the tester some time to stand still
                // the timeout greater than the trigger time span
                // clears the history to avoid being triggered again
                // and repeating the message too fast
                md.await(amount, Presence.NoMotion, 1.0, 10.0);
            } else {
                break;
            }
        }
        System.out.println("Very good.");
        md.await(amount, Presence.Motion, 1.0, Double.MAX_VALUE);
    }
}
