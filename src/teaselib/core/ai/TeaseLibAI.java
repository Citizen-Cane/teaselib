package teaselib.core.ai;

import java.util.List;

import teaselib.core.ai.perception.SceneCapture;

public class TeaseLibAI {
    final boolean haveAccelleratedImageProcesing;

    public TeaseLibAI() throws UnsatisfiedLinkError {
        teaselib.core.jni.LibraryLoader.load("TeaseLibAI");
        haveAccelleratedImageProcesing = initOpenCL();
    }

    native boolean initOpenCL();

    native List<SceneCapture> sceneCaptures();

}
