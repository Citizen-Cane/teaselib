package teaselib.core.javacv;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;

import teaselib.core.VideoRenderer;
import teaselib.core.concurrency.NamedExecutorService;

public abstract class VideoRendererJavaCV implements VideoRenderer {
    private NamedExecutorService executor;
    private SynchronousQueue<Runnable> handShake = new SynchronousQueue<>();

    private final Type type;
    private final String name;
    private int x = Integer.MAX_VALUE;
    private int y = Integer.MAX_VALUE;

    public VideoRendererJavaCV(Type type) {
        this.type = type;
        this.name = type.toString();

        executor = NamedExecutorService.singleThreadedQueue(getClass().getSimpleName() + ":" + name, 1,
                TimeUnit.SECONDS);

        executor.submit(this::processFrames);
    }

    private void processFrames() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                handShake.take().run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void update(Mat video) {
        try {
            handShake.put(() -> renderFrame(video));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void renderFrame(Mat video) {
        // Must show window before setting the position,
        // to ensure that the position is updated when the position has changed
        // - looks like OpenCV caches the position values,
        // but doesn't apply them when the window is not visible
        org.bytedeco.javacpp.opencv_highgui.imshow(name, video);
        try (Point position = getPosition(type, video.cols(), video.rows());) {
            if (position.x() != x || position.y() != y) {
                x = position.x();
                y = position.y();
                org.bytedeco.javacpp.opencv_highgui.moveWindow(name, x, y);
            }
        }
        pumpMessages();
    }

    private static void pumpMessages() {
        org.bytedeco.javacpp.opencv_highgui.waitKey(1);
    }

    @Override
    public void close() {
        try {
            handShake.put(() -> org.bytedeco.javacpp.opencv_highgui.destroyWindow(name));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected abstract Point getPosition(Type type, int width, int height);
}
