package teaselib.core.ai;

import java.util.List;

import teaselib.core.ai.perception.SceneCapture;

public class TeaseLibAI {

    public TeaseLibAI() throws UnsatisfiedLinkError {
        teaselib.core.jni.LibraryLoader.load("TeaseLibAI");
    }

    native List<SceneCapture> sceneCaptures();

}
