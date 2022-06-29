package teaselib.core.ai;

import static teaselib.core.jni.NativeLibraries.*;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import teaselib.core.Closeable;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.jni.NativeLibraries;

public class TeaseLibAI implements Closeable {

    private final NamedExecutorService cl;
    private final NamedExecutorService cpu;

    public TeaseLibAI() throws UnsatisfiedLinkError {
        NativeLibraries.require(TEASELIB);
        // NativeLibraries.require(TEASELIB_TENSORFLOW_SHARED);
        // NativeLibraries.require(TEASELIB_DEEPSPEECH_SHARED);
        NativeLibraries.require(TEASELIB_AI);
        var executor = new ComputeService();
        if (executor.context.accelerated) {
            cl = executor;
        } else {
            cl = null;
            executor.shutdown();
        }
        cpu = NamedExecutorService.sameThread(TeaseLibAI.class.getSimpleName() + " CPU inference");
    }

    @Override
    public void close() {
        // TOOD replace simplified caching with executor->List<Model> map
        if (cachedModel != null) {
            if (cl != null) {
                cl.submit(cachedModel::close);
            } else {
                cpu.submit(cachedModel::close);
            }
        }

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
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private HumanPose cachedModel = null;

    public HumanPose getModel(Set<Interest> interests) {
        if (!Interest.supported.containsAll(interests)) {
            throw new UnsupportedOperationException("No pose estimation model available to match " + interests);
        }
        if (cachedModel == null) {
            cachedModel = new HumanPose();
        }
        return cachedModel;
    }

    public enum ExecutionType {
        Accelerated,
        Cpu
    }

    public NamedExecutorService getExecutor(ExecutionType executionType) {
        if (executionType == ExecutionType.Accelerated && cl != null) {
            return cl;
        } else {
            return cpu;
        }
    }

}
