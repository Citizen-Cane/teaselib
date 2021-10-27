package teaselib.core.ai;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.SceneCapture;
import teaselib.core.ai.perception.SceneCapture.Rotation;
import teaselib.core.jni.NativeObjectList;

public class TeaseLibAITest {
    private static final Logger logger = LoggerFactory.getLogger(TeaseLibAITest.class);

    @Test
    public void loadNativeLibrary() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                NativeObjectList<SceneCapture> sceneCaptures = teaseLibAI.sceneCaptures()) {
            assertNotNull(sceneCaptures);
            int n = 0;
            for (SceneCapture device : sceneCaptures) {
                logger.info("Device {}: '{}' , enclosure location = {}", n++, device.name, device.location);
            }
        }
    }

    @Test
    public void accessCamera() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                NativeObjectList<SceneCapture> sceneCaptures = teaseLibAI.sceneCaptures();
                HumanPose humanPose = new HumanPose()) {
            assertNotNull(sceneCaptures);
            assumeFalse("No Scene Capture devices found", sceneCaptures.isEmpty());
            var sceneCapture = sceneCaptures.get(0);
            sceneCapture.start();
            List<Estimation> poses = humanPose.poses(sceneCapture, Rotation.None);
            assertFalse(poses.stream().anyMatch(pose -> humanPose.getTimestamp() == 0));
        }
    }

    @Test
    public void testImageCapture() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            String name = "images/p2_320x240_01.jpg";
            String pattern = "p2_320x240_%02d.jpg";
            try (SceneCapture sceneCapture = new SceneCapture(getOpenCVImageSequence(name, pattern));
                    HumanPose humanPose = new HumanPose()) {
                sceneCapture.start();
                List<HumanPose.Estimation> poses = humanPose.poses(sceneCapture, Rotation.None);
                assertEquals(2, poses.size());
                assertFalse(poses.stream().anyMatch(pose -> humanPose.getTimestamp() == 0));
            }
        }
    }

    @Test
    public void testImage() throws IOException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI(); HumanPose humnaPose = new HumanPose()) {
            pose1(humnaPose);
            pose2(humnaPose);
            pose3(humnaPose);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testImageStabilityNullByteArray() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI(); HumanPose humnaPose = new HumanPose()) {
            byte[] nullptr = null;
            humnaPose.poses(nullptr);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImageStabilityEmptyByteArray() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI(); HumanPose humnaPose = new HumanPose()) {
            byte[] empty = new byte[0];
            humnaPose.poses(empty);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImageStabilityNotAnImage() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI(); HumanPose humnaPose = new HumanPose()) {
            byte[] garbage = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            humnaPose.poses(garbage);
        }
    }

    private void pose1(HumanPose humnaPose) throws IOException {
        List<Estimation> poses = humnaPose.poses(resource("images/p2_320x240_01.jpg"));
        assertEquals(2, poses.size());
        assertEquals(0.25, poses.get(1).head.orElseThrow().getX(), 0.02);
        assertEquals(0.09, poses.get(1).head.orElseThrow().getY(), 0.02);
        assertEquals(0.7, poses.get(0).head.orElseThrow().getX(), 0.01);
        assertEquals(0.10, poses.get(0).head.orElseThrow().getY(), 0.02);
    }

    private void pose2(HumanPose humnaPose) throws IOException {
        List<Estimation> poses = humnaPose.poses(resource("images/hand1.jpg"));
        assertEquals(1, poses.size());
        assertEquals(0.51, poses.get(0).head.orElseThrow().getX(), 0.02);
        assertEquals(0.42, poses.get(0).head.orElseThrow().getY(), 0.02);
    }

    private void pose3(HumanPose humnaPose) throws IOException {
        List<Estimation> poses = humnaPose.poses(resource("images/hand2.jpg"));
        assertEquals(1, poses.size());
        assertEquals(0.58, poses.get(0).head.orElseThrow().getX(), 0.02);
        assertEquals(0.41, poses.get(0).head.orElseThrow().getY(), 0.02);
    }

    private InputStream resource(String path) {
        return getClass().getResourceAsStream(path);
    }

    @Test
    public void testMultipleModelsSharedCapture() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            assertNotNull(teaseLibAI.sceneCaptures());
        }
        String name = "images/hand1.jpg";
        String pattern = "hand%01d.jpg";

        try (SceneCapture sceneCapture = new SceneCapture(getOpenCVImageSequence(name, pattern));
                HumanPose humanPose1 = new HumanPose();
                HumanPose humanPose2 = new HumanPose()) {

            sceneCapture.start();
            List<HumanPose.Estimation> poses1 = humanPose1.poses(sceneCapture, Rotation.None);
            assertEquals(1, poses1.size());
            assertEquals("Assertion based on PoseEstimation::Resolution::Size320x240", 0.71f,
                    poses1.get(0).distance.orElseThrow(), 0.01f);
            assertEquals(Proximity.FACE2FACE, poses1.get(0).proximity());

            List<HumanPose.Estimation> poses2 = humanPose2.poses(sceneCapture, Rotation.None);
            assertEquals(1, poses2.size());
            assertEquals("Assertion based on PoseEstimation::Resolution::Size320x240", 0.92,
                    poses2.get(0).distance.orElseThrow(), 0.1);
            assertEquals(Proximity.FACE2FACE, poses2.get(0).proximity());

            try {
                humanPose2.poses(sceneCapture, Rotation.None);
                fail("Expected device lost since image sequence doesn't contain any more images");
            } catch (SceneCapture.DeviceLost exception) {
                return;
            }
        }
    }

    @Test
    public void testDistanceNear() throws IOException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI(); HumanPose humnaPose = new HumanPose()) {
            List<Estimation> poses = humnaPose.poses(resource("images/p2_320x240_01.jpg"));
            assertEquals(2, poses.size());
            assertEquals(Proximity.NEAR, poses.get(0).proximity());
            assertEquals(Proximity.NEAR, poses.get(1).proximity());
        }
    }

    private static String getOpenCVImageSequence(String name, String pattern) {
        File folder = new File(TeaseLibAITest.class.getResource(name).getFile()).getParentFile();
        return new File(folder, pattern).getAbsolutePath();
    }

}
