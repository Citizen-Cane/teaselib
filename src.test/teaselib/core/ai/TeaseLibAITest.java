package teaselib.core.ai;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

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

}
