package teaselib.core.ai.perception;

import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.TeaseLibAITest;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.test.TestScript;

public class PoseEstimationTaskTest {

    private static final int TIMEOUT_MILLIS = 15;

    private HumanPoseDeviceInteraction humanPoseDeviceInteraction(TestScript script, TeaseLibAI teaseLibAI, String image) throws InterruptedException {
        return humanPoseDeviceInteraction(script, teaseLibAI, "images/" + image + "_01.jpg", image + "_%02d.jpg");
    }

    private HumanPoseDeviceInteraction humanPoseDeviceInteraction(TestScript script, TeaseLibAI teaseLibAI, String name, String pattern)
            throws InterruptedException {
        var humanPoseDeviceInteraction = new HumanPoseDeviceInteraction(script.teaseLib, new PoseEstimationTask(teaseLibAI) {
            @Override
            SceneCapture getDevice() throws InterruptedException {
                try {
                    return new SceneCapture(TeaseLibAITest.getOpenCVImageSequence(name, pattern));
                } catch (FileNotFoundException e) {
                    throw new AssertionError(name + "' not found");
                }
            }
        });
        assertTrue(humanPoseDeviceInteraction.isActive(), "Scene capture device not active");
        return humanPoseDeviceInteraction;
    }

    @Test
    public void testGetPose() throws InterruptedException, IOException {
        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "p2_320x240")) {
            PoseAspects pose = humanPoseDeviceInteraction.getPose(Interest.Proximity);
            assertTrue(pose.is(Proximity.NEAR));
        }
    }

    @Test
    public void testAwaitPose() throws InterruptedException, IOException {
        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "p2_320x240")) {
            assertTrue(humanPoseDeviceInteraction.awaitPose(singleton(Interest.Proximity), TIMEOUT_MILLIS, SECONDS, Proximity.NEAR));
        }
    }

    @Test
    public void testFace2Face() throws InterruptedException, IOException {
        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "images/hand1.jpg", "hand%01d.jpg")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertTrue(pose.FaceToFace.await(5, SECONDS));
        }

        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "p2_320x240")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertFalse(pose.FaceToFace.await(5, SECONDS));
        }
    }

    @Test
    public void testNotFace2Face() throws InterruptedException, IOException {
        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "images/hand1.jpg", "hand%01d.jpg")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertFalse(pose.NotFaceToFace.await(5, SECONDS));
        }

        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "p2_320x240")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertTrue(pose.NotFaceToFace.await(5, SECONDS));
        }
    }

    @Test
    public void testNear() throws InterruptedException, IOException {
        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "p2_320x240")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertTrue(pose.Near.await(5, SECONDS));
        }

        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "images/hand1.jpg", "hand%01d.jpg")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertTrue(pose.Near.await(5, SECONDS));
        }
    }

    @Test
    public void testNotNear() throws InterruptedException, IOException {
        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "p2_320x240")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertFalse(pose.NotNear.await(5, SECONDS));
        }

        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "images/hand1.jpg", "hand%01d.jpg")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertFalse(pose.NotNear.await(5, SECONDS));
        }

        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "baseball")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertTrue(pose.NotNear.await(5, SECONDS));
        }
    }

    @Test
    public void testFar() throws InterruptedException, IOException {
        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "baseball")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertTrue(pose.Far.await(5, SECONDS));
        }
    }

    @Test
    public void testNotFar() throws InterruptedException, IOException {
        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "baseball")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertFalse(pose.NotFar.await(5, SECONDS));
        }

        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "p2_320x240")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertTrue(pose.NotFar.await(5, SECONDS));
        }

        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "baseball")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertFalse(pose.NotFar.await(5, SECONDS));
        }
    }

    @Test
    public void testAway() throws InterruptedException, IOException {
        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "baseball")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertTrue(pose.Far.await(5, SECONDS));
        }
    }

    @Test
    public void testNotAway() throws InterruptedException, IOException {
        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "p2_320x240")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertTrue(pose.NotAway.await(5, SECONDS));
        }

        try (var script = new TestScript();
                var teaseLibAI = new TeaseLibAI();
                var humanPoseDeviceInteraction = humanPoseDeviceInteraction(script, teaseLibAI, "baseball")) {
            var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
            assertTrue(pose.NotAway.await(5, SECONDS));
        }
    }

}
