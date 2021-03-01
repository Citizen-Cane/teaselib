package teaselib.core.ai;

import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import teaselib.core.Closeable;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.SceneCapture;
import teaselib.core.ai.perception.SceneCapture.EnclosureLocation;
import teaselib.core.concurrency.NamedExecutorService;

public class TeaseLibAI implements Closeable {
    private final boolean haveAccelleratedImageProcesing;
    private final NamedExecutorService cl;
    private final NamedExecutorService cpu;

    public TeaseLibAI() throws UnsatisfiedLinkError {
        teaselib.core.jni.LibraryLoader.load("TeaseLibAI");
        NamedExecutorService executor = NamedExecutorService
                .sameThread(TeaseLibAI.class.getSimpleName() + " OpenCL Inference");
        haveAccelleratedImageProcesing = initializeOpenCLInExecutorThread(executor);
        cl = haveAccelleratedImageProcesing ? executor : null;
        cpu = NamedExecutorService.sameThread(TeaseLibAI.class.getSimpleName() + " CPU inference");
    }

    private boolean initializeOpenCLInExecutorThread(ExecutorService executor) {
        try {
            return executor.submit(this::initOpenCL).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            throw asRuntimeException(e);
        }
    }

    private native boolean initOpenCL();

    public native List<SceneCapture> sceneCaptures();

    @Override
    public void close() {
        if (cl != null)
            cl.shutdown();
        if (cpu != null)
            cpu.shutdown();

        if (cl != null)
            awaitTermination(cl);
        if (cpu != null)
            awaitTermination(cpu);
    }

    private void awaitTermination(ExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static long pollDurationMillis = 5000;

    public SceneCapture awaitCaptureDevice() throws InterruptedException {
        return awaitCaptureDevice(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

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

    public enum ExecutionType {
        Accelerated,
        Cpu
    }

    public NamedExecutorService getExecutor(ExecutionType executionType) {
        if (executionType == ExecutionType.Accelerated && haveAccelleratedImageProcesing) {
            return cl;
        } else {
            return cpu;
        }
    }

}
