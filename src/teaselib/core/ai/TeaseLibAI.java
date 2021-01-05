package teaselib.core.ai;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import teaselib.core.Closeable;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.SceneCapture;
import teaselib.core.ai.perception.SceneCapture.EnclosureLocation;

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

    private static long pollDurationMillis = 5000;

    public SceneCapture awaitCaptureDevice(long timeout, TimeUnit unit) throws InterruptedException {
        long durationMillis = TimeUnit.MILLISECONDS.convert(timeout, unit);
        while (durationMillis > 0) {
            List<SceneCapture> devices = sceneCaptures();
            if (devices.isEmpty()) {
                Thread.sleep(Math.min(pollDurationMillis, durationMillis));
                durationMillis -= pollDurationMillis;
            } else {
                Optional<SceneCapture> device = find(devices, EnclosureLocation.External);
                if (device.isPresent()) {
                    return device.get();
                } else {
                    device = find(devices, EnclosureLocation.Front);
                    if (device.isPresent()) {
                        return device.get();
                    }
                }
            }
        }
        return null;
    }

    private static Optional<SceneCapture> find(List<SceneCapture> sceneCaptures, EnclosureLocation location) {
        return sceneCaptures.stream().filter(s -> s.location == location).findFirst();
    }

    public HumanPose getModel(Interest interest) {
        if (interest != Interest.Status) {
            throw new UnsupportedOperationException("TODO Match interests with pose estiamtion model");
        }
        return new HumanPose();
    }

}
