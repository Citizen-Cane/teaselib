package teaselib.core.ai.perception;

import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import teaselib.core.Closeable;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.TeaseLibAITest;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.test.TestScript;

public class PoseEstimationTaskTest {

    private static final int AWAIT_DURATION = 5;

    static class TestPerson implements Closeable {

        private final TestScript script;
        private final TeaseLibAI teaseLibAI;
        private final Proximity proximity;

        TestPerson() throws IOException {
            this(Proximity.AWAY);
        }

        TestPerson(Proximity startupProximity) throws IOException {
            this(new TestScript(), new TeaseLibAI(), startupProximity);
        }

        public TestPerson(TestScript script, TeaseLibAI teaseLibAI, Proximity startupProximity) {
            this.script = script;
            this.teaseLibAI = teaseLibAI;
            this.proximity = startupProximity;
        }

        public TestPerson another(Proximity startupProximity) {
            return new TestPerson(script, teaseLibAI, startupProximity);
        }

        private HumanPoseDeviceInteraction closeProximity() throws InterruptedException {
            return humanPoseDeviceInteraction("images/close1.jpg", "close%01d.jpg");
        }

        private HumanPoseDeviceInteraction face2face() throws InterruptedException {
            return humanPoseDeviceInteraction("images/hand1.jpg", "hand%01d.jpg");
        }

        private HumanPoseDeviceInteraction near() throws InterruptedException {
            return humanPoseDeviceInteraction("p2_320x240");
        }

        private HumanPoseDeviceInteraction far() throws InterruptedException {
            return humanPoseDeviceInteraction("baseball_far");
        }

        private HumanPoseDeviceInteraction away() throws InterruptedException {
            return humanPoseDeviceInteraction("baseball");
        }

        public HumanPoseDeviceInteraction absent() throws InterruptedException {
            return humanPoseDeviceInteraction("absent");
        }

        private HumanPoseDeviceInteraction humanPoseDeviceInteraction(String image)
                throws InterruptedException {
            return humanPoseDeviceInteraction("images/" + image + "_01.jpg", image + "_%02d.jpg");
        }

        private HumanPoseDeviceInteraction humanPoseDeviceInteraction(String name, String pattern)
                throws InterruptedException {
            var poseAspects = new PoseAspects(Estimation.NONE, 0L, singleton(Interest.Proximity), this.proximity);
            var humanPoseDeviceInteraction = new HumanPoseDeviceInteraction(
                    script.teaseLib, new PoseEstimationTask(teaseLibAI, poseAspects) {

                        CountDownLatch sceneCaptureCompleted = new CountDownLatch(1);

                        @Override
                        SceneCapture getDevice() throws InterruptedException {
                            try {
                                return new SceneCapture(TeaseLibAITest.getOpenCVImageSequence(name, pattern)) {

                                    @Override
                                    public void close() {
                                        super.close();
                                        try {
                                            sceneCaptureCompleted.await();
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    }

                                };
                            } catch (FileNotFoundException e) {
                                throw new AssertionError(name + "' not found");
                            }
                        }

                        @Override
                        public void close() {
                            super.close();
                            sceneCaptureCompleted.countDown();
                        }

                    });
            assertTrue(humanPoseDeviceInteraction.isActive(), "Scene capture device not active");
            return humanPoseDeviceInteraction;
        }

        interface Pose {
            HumanPoseDeviceInteraction get() throws InterruptedException;
        }

        public void getProximity(Pose humanPoseDeviceInteraction, Proximity startupProximity) throws InterruptedException {
            try (var pose = humanPoseDeviceInteraction.get()) {
                getProximity(pose, startupProximity);
            }
        }

        void getProximity(HumanPoseDeviceInteraction humanPoseDeviceInteraction, Proximity startupProximity) {
            PoseAspects pose = humanPoseDeviceInteraction.getPose(Interest.Proximity);
            assertTrue(pose.is(startupProximity));
        }

        public void awaitProximity(Pose humanPoseDeviceInteraction, Proximity startupProximity) throws InterruptedException {
            try (var pose = humanPoseDeviceInteraction.get()) {
                awaitProximity(pose, startupProximity);
            }
        }

        void awaitProximity(HumanPoseDeviceInteraction humanPoseDeviceInteraction, Proximity startupProximity) {
            assertTrue(humanPoseDeviceInteraction.await(singleton(Interest.Proximity), AWAIT_DURATION, SECONDS, startupProximity));
        }

        @Override
        public void close() {
            teaseLibAI.close();
            script.close();
        }

    }

    @Test
    public void testGetPose() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson()) {
            testPerson.getProximity(testPerson::face2face, Proximity.FACE2FACE);
            testPerson.getProximity(testPerson::near, Proximity.NEAR);
            testPerson.getProximity(testPerson::far, Proximity.FAR);
            testPerson.getProximity(testPerson::away, Proximity.AWAY);
        }
    }

    @Test
    public void testAwaitProximity() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson()) {
            testPerson.awaitProximity(testPerson.near(), Proximity.NEAR);
        }
    }

    @Test
    public void testPresence() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson()) {
            try (var humanPoseDeviceInteraction = testPerson.face2face()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.Presence.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.near()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertFalse(pose.FaceToFace.await(AWAIT_DURATION, SECONDS));
            }
        }
    }

    @Test
    public void testFace2Face() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson()) {
            try (var humanPoseDeviceInteraction = testPerson.face2face()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.FaceToFace.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.near()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertFalse(pose.FaceToFace.await(AWAIT_DURATION, SECONDS));
            }
        }
    }

    @Test
    public void testNotFace2Face() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson(Proximity.CLOSE)) {
            try (var hysteresisSafeEstimation = testPerson.closeProximity()) {
                var pose = new HumanPoseScriptInteraction(hysteresisSafeEstimation);
                assertFalse(pose.NotFaceToFace.await(AWAIT_DURATION, SECONDS));
            }

            try (var hysteresisSafeEstimation = testPerson.far()) {
                var pose = new HumanPoseScriptInteraction(hysteresisSafeEstimation);
                assertTrue(pose.Near.await(AWAIT_DURATION, SECONDS));
            }
        }
    }

    @Test
    public void testNear() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson(Proximity.FAR)) {
            try (var humanPoseDeviceInteraction = testPerson.near()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.Near.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.face2face()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.Near.await(AWAIT_DURATION, SECONDS));
            }
        }
    }

    @Test
    public void testNotNear() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson(Proximity.NEAR)) {
            try (var humanPoseDeviceInteraction = testPerson.far()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.NotNear.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.near()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertFalse(pose.NotNear.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.far()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.NotNear.await(AWAIT_DURATION, SECONDS));
            }
        }
    }

    @Test
    public void testFar() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson(Proximity.AWAY)) {
            try (var humanPoseDeviceInteraction = testPerson.far()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.Far.await(AWAIT_DURATION, SECONDS));
            }
        }
    }

    @Test
    public void testNotFar() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson(Proximity.NEAR)) {
            try (var humanPoseDeviceInteraction = testPerson.far()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertFalse(pose.NotFar.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.away()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.NotFar.await(AWAIT_DURATION, SECONDS));
            }
        }
    }

    @Test
    public void testAway() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson(Proximity.NEAR)) {
            try (var humanPoseDeviceInteraction = testPerson.near()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                // TODO fails because scene capture ends and pose is set back to PoseAspects.Unavailable
                // - workaround: sleep before signaling in PoseEstimationTask::call
                assertFalse(pose.Absence.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.far()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertFalse(pose.Absence.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.away()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.Absence.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.absent()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.Absence.await(AWAIT_DURATION, SECONDS));
            }

        }
    }

    @Test
    public void testNotAway() throws InterruptedException, IOException {
        try (var testPerson = new TestPerson()) {
            try (var humanPoseDeviceInteraction = testPerson.absent()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertFalse(pose.Presence.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.away()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertFalse(pose.Presence.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.near()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.Presence.await(AWAIT_DURATION, SECONDS));
            }

            try (var humanPoseDeviceInteraction = testPerson.far()) {
                var pose = new HumanPoseScriptInteraction(humanPoseDeviceInteraction);
                assertTrue(pose.Presence.await(AWAIT_DURATION, SECONDS));
            }
        }
    }

}
