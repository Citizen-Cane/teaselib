package teaselib.core.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ai.TeaseLibAI.ExecutionType;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.SceneCapture;
import teaselib.core.ai.perception.SceneCapture.Rotation;
import teaselib.core.jni.NativeObjectList;
import teaselib.core.util.ExceptionUtil;

public class TeaseLibAITest {
    private static final Logger logger = LoggerFactory.getLogger(TeaseLibAITest.class);

    @Test
    public void loadNativeLibrary() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                NativeObjectList<SceneCapture> devices = SceneCapture.devices()) {
            assertNotNull(devices);
            int n = 0;
            for (SceneCapture device : devices) {
                logger.info("Device {}: '{}' , enclosure location = {}", n++, device.name, device.location);
            }
        }
    }

    @Test
    public void accessCamera() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                NativeObjectList<SceneCapture> devices = SceneCapture.devices()) {
            assertNotNull(devices);
            assumeFalse("No Scene Capture devices found", devices.isEmpty());
            Runnable test = () -> {
                try (HumanPose humanPose = new HumanPose()) {
                    var sceneCapture = devices.get(0);
                    sceneCapture.start();
                    List<Estimation> poses = humanPose.poses(sceneCapture, Rotation.None);
                    assertFalse(poses.stream().anyMatch(pose -> humanPose.getTimestamp() == 0));
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    @Test
    public void testImageCapture() throws InterruptedException {
        String name = "images/p2_320x240_01.jpg";
        String pattern = "p2_320x240_%02d.jpg";
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                SceneCapture sceneCapture = new SceneCapture(getOpenCVImageSequence(name, pattern))) {
            sceneCapture.start();
            Runnable test = () -> {
                try (HumanPose humanPose = new HumanPose()) {
                    List<HumanPose.Estimation> poses = humanPose.poses(sceneCapture, Rotation.None);
                    assertEquals(2, poses.size());
                    assertFalse(poses.stream().anyMatch(pose -> humanPose.getTimestamp() == 0));
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    private Object runAccelerated(TeaseLibAI teaseLibAI, Runnable test) throws InterruptedException {
        try {
            return teaseLibAI.getExecutor(ExecutionType.Accelerated).submit(test).get();
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    @Test
    public void testSomeImages() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();) {
            Runnable test = () -> {
                try (HumanPose humnaPose = new HumanPose()) {
                    try {
                        pose1(humnaPose);
                        pose2(humnaPose);
                        pose3(humnaPose);
                    } catch (IOException e) {
                        throw ExceptionUtil.asRuntimeException(e);
                    }
                }
            };
            runAccelerated(teaseLibAI, test);
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

    @Test(expected = NullPointerException.class)
    public void testImageStabilityNullByteArray() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            Runnable test = () -> {
                try (HumanPose humnaPose = new HumanPose()) {
                    byte[] nullptr = null;
                    humnaPose.poses(nullptr);
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImageStabilityEmptyByteArray() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            Runnable test = () -> {
                try (HumanPose humnaPose = new HumanPose()) {
                    byte[] empty = new byte[0];
                    humnaPose.poses(empty);
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImageStabilityNotAnImage() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            Runnable test = () -> {
                try (HumanPose humnaPose = new HumanPose()) {
                    byte[] garbage = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
                    humnaPose.poses(garbage);
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    @Test
    public void testMultipleModelsSharedCapture() throws InterruptedException {
        String name = "images/hand1.jpg";
        String pattern = "hand%01d.jpg";

        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                SceneCapture sceneCapture = new SceneCapture(getOpenCVImageSequence(name, pattern));) {

            Runnable test = () -> {
                try (HumanPose humanPose1 = new HumanPose(); HumanPose humanPose2 = new HumanPose()) {
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
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    @Test
    public void testDistanceNear() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            Runnable test = () -> {
                try (HumanPose humnaPose = new HumanPose()) {
                    List<Estimation> poses;
                    try {
                        poses = humnaPose.poses(resource("images/p2_320x240_01.jpg"));
                        assertEquals(2, poses.size());
                        assertEquals(Proximity.NEAR, poses.get(0).proximity());
                        assertEquals(Proximity.NEAR, poses.get(1).proximity());
                    } catch (IOException e) {
                        throw ExceptionUtil.asRuntimeException(e);
                    }
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    private InputStream resource(String path) {
        return getClass().getResourceAsStream(path);
    }

    private static String getOpenCVImageSequence(String name, String pattern) {
        File folder = new File(TeaseLibAITest.class.getResource(name).getFile()).getParentFile();
        return new File(folder, pattern).getAbsolutePath();
    }

}
