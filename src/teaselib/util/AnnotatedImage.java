package teaselib.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import teaselib.Message;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;

public class AnnotatedImage {

    public static final AnnotatedImage NoImage = new AnnotatedImage(Message.NoImage, new byte[] {});

    public interface Annotation {
        // Tag interface

        public enum Person implements Annotation {
            Actor,
            Model
        }
    }

    public final String resource;
    public final byte[] bytes;
    public final HumanPose.Estimation pose;
    public final Set<Annotation> annotations;

    public AnnotatedImage(String resource, byte[] image) {
        this(resource, image, HumanPose.Estimation.NONE);
    }

    public AnnotatedImage(String resource, byte[] bytes, Estimation pose, Annotation... annotations) {
        super();
        this.resource = resource;
        this.bytes = bytes;
        this.pose = pose;
        this.annotations = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(annotations)));
    }

}
