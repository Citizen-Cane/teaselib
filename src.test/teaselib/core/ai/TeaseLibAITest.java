package teaselib.core.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.SceneCapture;

public class TeaseLibAITest {
    private static final Logger logger = LoggerFactory.getLogger(TeaseLibAITest.class);

    @Test
    public void loadNativeLibrary() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            List<SceneCapture> sceneCaptures = teaseLibAI.sceneCaptures();
            assertNotNull(sceneCaptures);

            int n = 0;
            for (SceneCapture device : sceneCaptures) {
                logger.info("Device {}: '{}' , enclosure location = {}", n++, device.name, device.location);
            }
        }
    }

    @Test
    public void accessCamera() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            List<SceneCapture> sceneCaptures = teaseLibAI.sceneCaptures();
            assertNotNull(sceneCaptures);
            assumeFalse("No Scene Capture devices found", sceneCaptures.isEmpty());

            try (HumanPose humanPose = new HumanPose(sceneCaptures.get(0))) {
                humanPose.device.start();
                humanPose.poses();
            }
        }
    }

    @Test
    public void testAwareness() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            assertNotNull(teaseLibAI.sceneCaptures());
        }
        String name = "images/p2_320x240_01.jpg";
        String pattern = "p2_320x240_%02d.jpg";

        try (SceneCapture scene = new SceneCapture(getOpenCVImageSequence(name, pattern));
                HumanPose humanPose = new HumanPose(scene)) {
            humanPose.device.start();
            List<HumanPose.EstimationResult> poses = humanPose.poses();
            assertEquals(2, poses.size());
        }
    }

    @Test
    public void testMultipleModelsSharedCapture() {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            assertNotNull(teaseLibAI.sceneCaptures());
        }
        String name = "images/hand1.jpg";
        String pattern = "hand%01d.jpg";

        try (SceneCapture scene = new SceneCapture(getOpenCVImageSequence(name, pattern));
                HumanPose humanPose1 = new HumanPose(scene);
                HumanPose humanPose2 = new HumanPose(scene)) {

            humanPose1.device.start();
            List<HumanPose.EstimationResult> poses1 = humanPose1.poses();
            assertEquals(1, poses1.size());
            assertEquals("Assertion based on 128x96 model", 0.78f, poses1.get(0).distance, 0.01f);
            assertEquals(Proximity.FACE2FACE, poses1.get(0).proximity());

            List<HumanPose.EstimationResult> poses2 = humanPose2.poses();
            assertEquals(1, poses2.size());
            assertEquals("Assertion based on 128x96 model", 0.96, poses2.get(0).distance, 0.1);
            assertEquals(Proximity.NEAR, poses2.get(0).proximity());

            try {
                humanPose2.poses();
            } catch (SceneCapture.DeviceLost exception) {
                return;
            }
            fail("Expected device lost since image sequence doesn't contain any more images");
        }
    }

    private String getOpenCVImageSequence(String name, String pattern) {
        File folder = new File(TeaseLibAITest.class.getResource(name).getFile()).getParentFile();
        return new File(folder, pattern).getAbsolutePath();
    }

}
