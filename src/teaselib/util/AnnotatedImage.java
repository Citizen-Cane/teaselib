package teaselib.util;

import teaselib.Message;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;

public class AnnotatedImage {

    public static final AnnotatedImage NoImage = new AnnotatedImage(Message.NoImage, new byte[] {});
    
    final String resource;
    final public byte[] bytes;
    final public HumanPose.Estimation pose;

    public AnnotatedImage(String resource, byte[] image) {
        this(resource, image, null);
    }

    public AnnotatedImage(String resource, byte[] bytes, Estimation pose) {
        super();
        this.resource = resource;
        this.bytes = bytes;
        this.pose = pose;
    }

}
