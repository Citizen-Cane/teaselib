package teaselib.util;

import teaselib.Message;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;

public class AnnotatedImage {

    public static final AnnotatedImage NoImage = new AnnotatedImage(Message.NoImage, new byte[] {});

    public final String resource;
    public final byte[] bytes;
    public final HumanPose.Estimation pose;

    public AnnotatedImage(String resource, byte[] image) {
        this(resource, image, HumanPose.Estimation.NONE);
    }

    public AnnotatedImage(String resource, byte[] bytes, Estimation pose) {
        super();
        this.resource = resource;
        this.bytes = bytes;
        this.pose = pose;
    }

}
