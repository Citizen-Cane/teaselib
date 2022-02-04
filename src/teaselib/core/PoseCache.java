package teaselib.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Status;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.FileUtilities;
import teaselib.util.AnnotatedImage;

public class PoseCache {

    private static final String PROPERTIES_EXTENSION = ".properties";

    private final Path path;
    private final Script script;
    private final HumanPoseScriptInteraction interaction;

    private final Map<String, HumanPose.Estimation> poses = new HashMap<>();
    private final Map<String, AnnotatedImage> annotatedImages = new HashMap<>();

    public PoseCache(Path poseCache, Script script) {
        this.path = poseCache;
        this.script = script;
        this.interaction = script.interaction(HumanPoseScriptInteraction.class);
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
            in = script.resources.get(name);
        } catch (IOException e1) {
            Path file = path.resolve(name);
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
        return annotatedImages.computeIfAbsent(resource, key -> {
            return new AnnotatedImage(key, image, getPose(resource, image));
        });
    }

    private HumanPose.Estimation getPose(String resource, byte[] image) {
        HumanPose.Estimation estimation = poses.computeIfAbsent(resource, key -> {
            // TODO add status display to host since this is not about actors and script conversation
            script.teaseLib.host.showInterTitle("Pose estimation pre-caching: \n\n" + key);
            script.teaseLib.host.show();
            Estimation pose = computePose(image);
            Path file = path.resolve(propertyFilename(resource));
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
        return teaselib.core.util.ReflectionUtils
                .relativePath(FileUtilities.removeExtension(resource) + PROPERTIES_EXTENSION);
    }

    private Estimation computePose(byte[] image) {
        // TODO implement HumanPose.Interest.Pose to get better results
        var pose = interaction.getPose(Interest.Head, image);
        return pose.is(Status.Available) ? pose.estimation : HumanPose.Estimation.NONE;
    }

    @Override
    public String toString() {
        return poses.toString();
    }

}
