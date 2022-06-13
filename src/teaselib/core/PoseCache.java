package teaselib.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.FileUtilities;
import teaselib.util.AnnotatedImage;

public class PoseCache {

    private static final String PROPERTIES_EXTENSION = ".properties";

    private final Path path;
    private final ResourceLoader loader;
    private final BiFunction<String, byte[], Estimation> computePose;

    private final Map<String, HumanPose.Estimation> poses = new HashMap<>();
    private final Map<String, WeakReference<AnnotatedImage>> annotatedImages = new HashMap<>();

    public PoseCache(Path poseCache, ResourceLoader loader, BiFunction<String, byte[], Estimation> computePose) {
        this.path = poseCache;
        this.loader = loader;
        this.computePose = computePose;
    }

    public static boolean isPropertyFile(String resource) {
        return resource.endsWith(PROPERTIES_EXTENSION);
    }

    public boolean loadPose(String resource) {
        if (!poses.containsKey(resource)) {
            InputStream in = tryLoadPersistedPose(propertyFilename(resource));
            if (in != null) {
                var pose = loadPose(in);
                poses.put(resource, pose);
            }
        }
        return poses.containsKey(resource);
    }

    private InputStream tryLoadPersistedPose(String name) {
        InputStream in;
        try {
            in = loader.get(name);
        } catch (IOException e1) {
            Path file = Paths.get(path.toString(), name);
            if (Files.exists(file)) {
                try {
                    in = Files.newInputStream(file);
                } catch (IOException e2) {
                    in = null;
                }
            } else {
                in = null;
            }
        }
        return in;
    }

    static Estimation loadPose(InputStream in) {
        PersistedPoseEstimation persisted;
        try {
            persisted = new PersistedPoseEstimation(in);
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
        return persisted.toPose();
    }

    AnnotatedImage annotatedImage(String resource, byte[] image) {
        WeakReference<AnnotatedImage> reference = annotatedImages.computeIfAbsent(resource, key -> {
            return new WeakReference<>(createAnnotatedImage(key, image));
        });
        AnnotatedImage annotatedImage = reference.get();
        if (annotatedImage != null) {
            return annotatedImage;
        } else {
            annotatedImage = createAnnotatedImage(resource, image);
            annotatedImages.put(resource, new WeakReference<>(annotatedImage));
            return annotatedImage;
        }
    }

    private AnnotatedImage createAnnotatedImage(String resource, byte[] image) {
        return new AnnotatedImage(resource, image, getPose(resource, image));
    }

    private HumanPose.Estimation getPose(String resource, byte[] image) {
        HumanPose.Estimation estimation = poses.computeIfAbsent(resource, key -> {
            Estimation pose = computePose.apply(resource, image);
            Path file = Paths.get(path.toString(), propertyFilename(resource));
            try {
                Files.createDirectories(file.getParent());
                new PersistedPoseEstimation(pose).store(Files.newOutputStream(file));
            } catch (IOException e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
            return pose;
        });
        return estimation;
    }

    private static String propertyFilename(String resource) {
        return FileUtilities.removeExtension(resource) + PROPERTIES_EXTENSION;
    }

    @Override
    public String toString() {
        return poses.toString();
    }

}
