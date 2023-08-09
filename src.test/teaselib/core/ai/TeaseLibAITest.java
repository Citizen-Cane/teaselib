package teaselib.core.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ai.TeaseLibAI.ExecutionType;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.SceneCapture;
import teaselib.core.ai.perception.SceneCapture.DeviceLost;
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
                    humanPose.setInterests(Interest.Head);
                    var sceneCapture = devices.get(0);
                    sceneCapture.start();
                    var timestamp = System.currentTimeMillis();
                    List<Estimation> poses = humanPose.poses(sceneCapture, timestamp);
                    assertNotNull(poses);
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    @Test
    public void testImageCapture() throws InterruptedException, FileNotFoundException {
        String name = "images/p2_320x240_01.jpg";
        String pattern = "p2_320x240_%02d.jpg";
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                SceneCapture sceneCapture = new SceneCapture(getOpenCVImageSequence(name, pattern))) {
            sceneCapture.start();
            Runnable test = () -> {
                try (HumanPose humanPose = new HumanPose()) {
                    humanPose.setInterests(Interest.AllPersons);
                    var timestamp = System.currentTimeMillis();
                    var poses = humanPose.poses(sceneCapture, timestamp);
                    assertNotNull(poses);
                    assertEquals(2, poses.size());
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    @Test
    public void testImageCaptureRotated() throws InterruptedException, FileNotFoundException {
        String name = "images/handsup1_camera_rotated_clockwise_01.jpg";
        String pattern = "handsup1_camera_rotated_clockwise_%02d.jpg";
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                SceneCapture sceneCapture = new SceneCapture(getOpenCVImageSequence(name, pattern)) {
                    @Override
                    public Rotation rotation() {
                        return Rotation.Clockwise;
                    }
                }) {
            sceneCapture.start();
            Runnable test = () -> {
                try (HumanPose humanPose = new HumanPose()) {
                    humanPose.setInterests(Interest.Head);
                    var timestamp = System.currentTimeMillis();
                    List<HumanPose.Estimation> poses = humanPose.poses(sceneCapture, timestamp);
                    assertNotNull(poses);
                    assertEquals(1, poses.size());
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    @Test
    public void testReverseRotation() {
        assertEquals(Rotation.None, Rotation.None.reverse());
        assertEquals(Rotation.Clockwise, Rotation.CounterClockwise.reverse());
        assertEquals(Rotation.UpsideDown, Rotation.UpsideDown.reverse());
        assertEquals(Rotation.CounterClockwise, Rotation.Clockwise.reverse());
    }

    @Test
    public void testImageRotated() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            Runnable test = () -> {
                try (HumanPose humanPose = new HumanPose()) {
                    humanPose.setInterests(Interest.Head);
                    List<HumanPose.Estimation> poses = poses(humanPose,
                            "images/handsup1_camera_rotated_clockwise_01.jpg", Rotation.CounterClockwise);
                    assertNotNull(poses);
                    assertEquals(1, poses.size());
                } catch (IOException e) {
                    throw ExceptionUtil.asRuntimeException(e);
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    @Test
    public void testImageRotationChange() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            Runnable test = () -> {
                try (HumanPose humanPose = new HumanPose()) {
                    humanPose.setInterests(Interest.AllPersons);
                    List<HumanPose.Estimation> poses1_n = poses(humanPose,
                            "images/p2_320x240_01.jpg", Rotation.None);
                    assertNotNull(poses1_n);
                    assertEquals(2, poses1_n.size());
                    assertEquals(1.80, poses1_n.get(0).distance.get(), 0.01);
                    assertEquals(1.67, poses1_n.get(1).distance.get(), 0.01);

                    humanPose.setInterests(Interest.Pose);
                    List<HumanPose.Estimation> poses2_n = poses(humanPose,
                            "images/handsup1_camera_rotated_clockwise_01.jpg", Rotation.CounterClockwise);
                    assertNotNull(poses2_n);
                    assertEquals(1, poses2_n.size());
                    assertEquals(1.40, poses2_n.get(0).distance.get(), 0.01);

                    humanPose.setInterests(Interest.Pose);
                    List<HumanPose.Estimation> poses_2cc = poses(humanPose,
                            "images/handsup1.jpg", Rotation.None);
                    assertNotNull(poses_2cc);
                    assertEquals(1, poses_2cc.size());
                    assertEquals(1.40, poses_2cc.get(0).distance.get(), 0.01);
                } catch (IOException e) {
                    throw ExceptionUtil.asRuntimeException(e);
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    private static Object runAccelerated(TeaseLibAI teaseLibAI, Runnable test) throws InterruptedException {
        try {
            return teaseLibAI.getExecutor(ExecutionType.Accelerated).submit(test).get();
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    @Test
    public void testSomeImages() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();) {
            Runnable test = () -> {
                try (HumanPose humanPose = new HumanPose()) {
                    try {
                        pose1(humanPose);
                        pose2(humanPose);
                        pose3(humanPose);
                        pose4(humanPose);
                        absent(humanPose);
                    } catch (IOException e) {
                        throw ExceptionUtil.asRuntimeException(e);
                    }
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    private void pose1(HumanPose humanPose) throws IOException {
        humanPose.setInterests(Interest.Pose);
        var poses = poses(humanPose, "images/p2_320x240_01.jpg", Rotation.None);
        assertEquals(1, poses.size());

        humanPose.setInterests(Interest.AllPersons);
        poses = poses(humanPose, "images/p2_320x240_01.jpg", Rotation.None);
        assertEquals(2, poses.size());
        assertEquals(0.25, poses.get(0).head.orElseThrow().getX(), 0.02);
        assertEquals(0.09, poses.get(0).head.orElseThrow().getY(), 0.02);
        assertEquals(0.7, poses.get(1).head.orElseThrow().getX(), 0.01);
        assertEquals(0.10, poses.get(1).head.orElseThrow().getY(), 0.02);
    }

    private void pose2(HumanPose humanPose) throws IOException {
        humanPose.setInterests(Interest.Pose);
        List<Estimation> poses = poses(humanPose, "images/hand1.jpg", Rotation.None);
        assertEquals(1, poses.size());
        assertEquals(0.51, poses.get(0).head.orElseThrow().getX(), 0.02);
        assertEquals(0.42, poses.get(0).head.orElseThrow().getY(), 0.02);
    }

    private void pose3(HumanPose humanPose) throws IOException {
        humanPose.setInterests(Interest.Pose);
        List<Estimation> poses = poses(humanPose, "images/hand2.jpg", Rotation.None);
        assertEquals(1, poses.size());
        assertEquals(0.59, poses.get(0).head.orElseThrow().getX(), 0.02);
        assertEquals(0.40, poses.get(0).head.orElseThrow().getY(), 0.02);
    }

    private void pose4(HumanPose humanPose) throws IOException {
        humanPose.setInterests(Interest.Pose);
        List<Estimation> poses = poses(humanPose, "images/baseball_01.jpg", Rotation.None);
        assertEquals(1, poses.size());
        assertEquals(0.32, poses.get(0).head.orElseThrow().getX(), 0.02);
        assertEquals(0.34, poses.get(0).head.orElseThrow().getY(), 0.02);
    }

    private void absent(HumanPose humanPose) throws IOException {
        humanPose.setInterests(Interest.Pose);
        List<Estimation> poses = poses(humanPose, "images/absent_01.jpg", Rotation.None);
        assertEquals(0, poses.size());
    }

    public void testImageStabilityNullByteArray() {
        testIllegalArgument(null);
    }

    public void testImageStabilityEmptyByteArray() {
        testIllegalArgument(new byte[0]);
    }

    public void testImageStabilityNotAnImage() {
        testIllegalArgument(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
    }

    private static void testIllegalArgument(byte[] image) throws UnsatisfiedLinkError {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            Runnable test = () -> {
                try (HumanPose humnaPose = new HumanPose()) {
                    humnaPose.poses(image, Rotation.None);
                }
            };
            assertThrows(IllegalArgumentException.class, () -> runAccelerated(teaseLibAI, test));
        }
    }

    @Test
    public void testMultipleModelsSharedCapture() throws InterruptedException, FileNotFoundException {
        String name = "images/hand1.jpg";
        String pattern = "hand%01d.jpg";

        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                SceneCapture sceneCapture = new SceneCapture(getOpenCVImageSequence(name, pattern));) {

            Runnable test = () -> {
                try (HumanPose humanPose1 = new HumanPose(); HumanPose humanPose2 = new HumanPose()) {
                    humanPose1.setInterests(Interest.Head);
                    humanPose2.setInterests(Interest.Head);
                    sceneCapture.start();
                    var timestamp = System.currentTimeMillis();
                    List<HumanPose.Estimation> poses1 = humanPose1.poses(sceneCapture, timestamp);
                    assertEquals(1, poses1.size());
                    assertEquals("Assertion based on PoseEstimation::Resolution::Size320x240", 0.88f,
                            poses1.get(0).distance.orElseThrow(), 0.01f);
                    assertEquals(Proximity.FACE2FACE, poses1.get(0).proximity());

                    var timestamp2 = System.currentTimeMillis();
                    List<HumanPose.Estimation> poses2 = humanPose2.poses(sceneCapture, timestamp2);
                    assertEquals(1, poses2.size());
                    assertEquals("Assertion based on PoseEstimation::Resolution::Size320x240", 1.00f,
                            poses2.get(0).distance.orElseThrow(), 0.01f);
                    // FACE2FACE distance but looking aside
                    assertEquals(Proximity.NEAR, poses2.get(0).proximity());

                    var timestamp3 = System.currentTimeMillis();
                    assertThrows(
                            "Expected Device Lost since cacpture sequence doesn't contain any more images",
                            DeviceLost.class,
                            () -> humanPose2.poses(sceneCapture, timestamp3));
                }
            };
            runAccelerated(teaseLibAI, test);
        }

    }

    @Test
    public void testProximity() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            Runnable test = () -> {
                try (HumanPose humanPose = new HumanPose()) {
                    List<Estimation> poses;
                    try {
                        humanPose.setInterests(Interest.AllPersons);
                        poses = poses(humanPose, "images/p2_320x240_01.jpg", Rotation.None);
                        assertEquals(2, poses.size());
                        assertEquals(Proximity.NEAR, poses.get(0).proximity());
                        assertEquals("Assertion based on PoseEstimation::Resolution::Size320x240", 1.80,
                                poses.get(0).distance.orElseThrow(), 0.01f);
                        assertEquals(Proximity.NEAR, poses.get(1).proximity());
                        assertEquals("Assertion based on PoseEstimation::Resolution::Size320x240", 1.67,
                                poses.get(1).distance.orElseThrow(), 0.01f);

                        humanPose.setInterests(Interest.Pose);
                        poses = poses(humanPose, "images/hand1.jpg", Rotation.None);
                        assertEquals(1, poses.size());
                        assertEquals("Assertion based on PoseEstimation::Resolution::Size320x240", 0.88f,
                                poses.get(0).distance.orElseThrow(), 0.01f);
                        assertEquals(Proximity.FACE2FACE, poses.get(0).proximity());

                        humanPose.setInterests(Interest.Pose);
                        poses = poses(humanPose, "images/baseball_far_01.jpg", Rotation.None);
                        assertEquals(1, poses.size());
                        assertEquals("Assertion based on PoseEstimation::Resolution::Size320x240", 4.88f,
                                poses.get(0).distance.orElseThrow(), 0.01f);
                        assertEquals(Proximity.FAR, poses.get(0).proximity());
                    } catch (IOException e) {
                        throw ExceptionUtil.asRuntimeException(e);
                    }
                }
            };
            runAccelerated(teaseLibAI, test);
        }
    }

    public List<HumanPose.Estimation> poses(HumanPose humanPose, String path, Rotation rotation) throws IOException {
        try (InputStream image = getClass().getResourceAsStream(path);) {
            if (image == null) {
                throw new IllegalArgumentException(path);
            }
            return humanPose.poses(image.readAllBytes(), rotation);
        } finally {
            // image.close();
        }
    }

    public static String getOpenCVImageSequence(String name, String pattern) throws FileNotFoundException {
        URL resource = TeaseLibAITest.class.getResource(name);
        if (resource == null) throw new FileNotFoundException(name);
        File folder = new File(resource.getFile()).getParentFile();
        return new File(folder, pattern).getAbsolutePath();
    }

}
