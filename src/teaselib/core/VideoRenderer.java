package teaselib.core;

import org.bytedeco.javacpp.opencv_core.Mat;

public interface VideoRenderer {

    enum Type {
        CameraFeedback,
        ImageAndText,
        FullScreen
    }

    Type getType();

    void update(Mat video);

    void close();
}
