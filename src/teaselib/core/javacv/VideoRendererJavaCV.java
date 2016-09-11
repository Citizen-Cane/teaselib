package teaselib.core.javacv;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;

import teaselib.core.VideoRenderer;

public abstract class VideoRendererJavaCV implements VideoRenderer {

    private final Type type;
    private final String name;
    private int x = Integer.MAX_VALUE;
    private int y = Integer.MAX_VALUE;

    public VideoRendererJavaCV(Type type) {
        super();
        this.type = type;
        this.name = type.toString();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void update(Mat video) {
        Point position = getPosition(type, video.cols(), video.rows());
        if (position.x() != x || position.y() != y) {
            x = position.x();
            y = position.y();
            org.bytedeco.javacpp.opencv_highgui.moveWindow(name, x, y);
        }
        position.close();
        org.bytedeco.javacpp.opencv_highgui.imshow(name, video);
        pumpMessages();
    }

    private static void pumpMessages() {
        org.bytedeco.javacpp.opencv_highgui.waitKey(1);
    }

    @Override
    public void close() {
        org.bytedeco.javacpp.opencv_highgui.destroyWindow(name);
    }

    protected abstract Point getPosition(Type type, int width, int height);
}
