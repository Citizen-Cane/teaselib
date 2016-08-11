package teaselib.motiondetection;

import org.junit.Test;

public class TestMotionInterface {

    @Test
    public void testMotionStartStop() {
        Movement movement = MotionDetection
                .movement(MotionDetection.Instance.getDefaultDevice());

        System.out.println("Move!");

        final double amount = 0.95;
        // md.setSensitivity(MotionSensitivity.High);

        while (!movement.startedWithin(1.0, 5.0)) {
            System.out.println("I said 'Move'!");
        }
        System.out.println("Keep moving!");

        while (true) {
            // Triggers after not moving for about one second
            if (movement.stoppedWithin(1.0, 10.0)) {
                System.out.println("I said 'Keep moving'!");
                // Besides leaving the tester some time to move again
                // the timeout greater than the trigger time span
                // clears the history to avoid being triggered again
                // and repeating the message too fast
                movement.startedWithin(1.0, 10.0);
            } else {
                break;
            }
        }

        System.out.println("Stop!");
        // md.setSensitivity(MotionSensitivity.Low);

        while (!movement.stoppedWithin(1.0, 5.0)) {
            System.out.println("I said 'Stop'!");
        }

        System.out.println("Now stay still.");
        // use the timeout to avoid repeating the message too fast
        while (true) {
            if (movement.startedWithin(10.0)) {
                System.out.println("I said 'Stay still'!");
                // Besides leaving the tester some time to stand still
                // the timeout greater than the trigger time span
                // clears the history to avoid being triggered again
                // and repeating the message too fast
                movement.stoppedWithin(10.0);
            } else {
                break;
            }
        }
        System.out.println("Very good.");
        movement.startedWithin(Double.MAX_VALUE);
    }
}
