package teaselib.core.ai;

import static teaselib.core.jni.NativeLibraries.TEASELIB_AI;
import static teaselib.core.jni.NativeLibraries.TEASELIB;
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
import teaselib.core.jni.NativeLibraries;
import teaselib.core.jni.NativeObjectList;

public class TeaseLibAI implements Closeable {

    public static final long CAPTURE_DEVICE_POLL_DURATION_MILLIS = 5000;

    private final boolean haveAccelleratedImageProcessing;
    private final NamedExecutorService cl;
    private final NamedExecutorService cpu;

    public TeaseLibAI() throws UnsatisfiedLinkError {
        NativeLibraries.require(TEASELIB, TEASELIB_AI);
        NamedExecutorService executor = NamedExecutorService
                .sameThread(TeaseLibAI.class.getSimpleName() + " OpenCL Inference");
        haveAccelleratedImageProcessing = initializeOpenCLInExecutorThread(executor);
        if (haveAccelleratedImageProcessing) {
            cl = executor;
        } else {
            cl = null;
            executor.shutdown();
        }
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

    public native NativeObjectList<SceneCapture> sceneCaptures();

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

    private static void awaitTermination(ExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public SceneCapture getCaptureDevice() throws InterruptedException {
        return awaitCaptureDevice(0, TimeUnit.MILLISECONDS);
    }

    public SceneCapture awaitCaptureDevice() throws InterruptedException {
        return awaitCaptureDevice(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public SceneCapture awaitCaptureDevice(long timeout, TimeUnit unit) throws InterruptedException {
        long durationMillis = TimeUnit.MILLISECONDS.convert(timeout, unit);
        while (durationMillis >= 0) {
            try (NativeObjectList<SceneCapture> devices = sceneCaptures()) {
                if (devices.isEmpty()) {
                    if (durationMillis > 0) {
                        long sleepMillis = Math.min(CAPTURE_DEVICE_POLL_DURATION_MILLIS, durationMillis);
                        Thread.sleep(sleepMillis);
                        durationMillis -= sleepMillis;
                    } else {
                        break;
                    }
                } else {
                    Optional<SceneCapture> device = find(devices, EnclosureLocation.External);
                    if (device.isEmpty()) {
                        device = find(devices, EnclosureLocation.Front);
                    }
                    if (device.isPresent()) {
                        SceneCapture captureDevice = device.get();
                        devices.remove(captureDevice);
                        return captureDevice;
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
        if (executionType == ExecutionType.Accelerated && haveAccelleratedImageProcessing) {
            return cl;
        } else {
            return cpu;
        }
    }

}
