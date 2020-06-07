package teaselib.core.ai;

import java.util.List;

import teaselib.core.Closeable;
import teaselib.core.ai.perception.SceneCapture;

public class TeaseLibAI implements Closeable {
    final boolean haveAccelleratedImageProcesing;

    public TeaseLibAI() throws UnsatisfiedLinkError {
        teaselib.core.jni.LibraryLoader.load("TeaseLibAI");
        haveAccelleratedImageProcesing = initOpenCL();
    }

    private native boolean initOpenCL();

    public native List<SceneCapture> sceneCaptures();

    @Override
    public void close() {
        //
    }

}
