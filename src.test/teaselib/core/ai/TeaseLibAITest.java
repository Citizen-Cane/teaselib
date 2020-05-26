package teaselib.core.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.Test;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.SceneCapture;
import teaselib.test.TestScript;

public class TeaseLibAITest {

    @Test
    public void loadNativeLibrary() {
        TeaseLibAI teaseLibAI = new TeaseLibAI();
        List<SceneCapture> sceneCaptures = teaseLibAI.sceneCaptures();
        assertNotNull(sceneCaptures);
    }

    @Test
    public void accessViaScript() {
        TestScript script = TestScript.getOne();
        TeaseLibAI teaseLibAI = script.teaseLib.globals.get(TeaseLibAI.class);
        List<SceneCapture> sceneCaptures = teaseLibAI.sceneCaptures();
        assertNotNull(sceneCaptures);
    }

    @Test
    public void testAwareness() {
        // TODO Move teaseLibAI.sceneCaptures() to SceneCapture class and remove TeaseLibAI
        TeaseLibAI teaseLibAI = new TeaseLibAI();
        assertNotNull(teaseLibAI.sceneCaptures());

        String name = "images/p2_320x240_01.jpg";
        String pattern = "p2_320x240_%2d.jpg";
        SceneCapture scene = new SceneCapture("Test", getOpenCVImageSequence(name, pattern));

        try (HumanPose humanPose = new HumanPose(scene)) {
            int n = humanPose.estimate();
            assertEquals(2, n);
        }
    }

    private String getOpenCVImageSequence(String name, String pattern) {
        File folder = new File(TeaseLibAITest.class.getResource(name).getFile()).getParentFile();
        return new File(folder, pattern).getAbsolutePath();
    }

}
