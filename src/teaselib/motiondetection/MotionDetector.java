package teaselib.motiondetection;

import java.awt.Dimension;
import java.awt.Point;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;

import teaselib.motiondetection.DirectionIndicator.Direction;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamMotionDetector;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class MotionDetector {

    public enum HorizontalMotion {
        None, Left, Right
    }

    public enum VerticalMotion {
        None, Up, Down
    }

    public enum Amount {
        JustABit(0.1), SomeWhat(0.25), ALot(0.5);

        public final double ScreenPercentage;

        Amount(double screenPercentage) {
            this.ScreenPercentage = screenPercentage;
        }
    }

    HorizontalMotion currentHorizontalMotion = HorizontalMotion.None;
    VerticalMotion currentVerticalMotion = VerticalMotion.None;

    // todo threshold could be a parameter of getDirection
    private static final int MotionThreshold = 20;
    private static final int MotionSeconds = 1;

    private static final int PollingInterval = 100;
    private static final double AreaThresholdPercent = 10.0;
    private static final int PixelThreshold = 16;

    final private DirectionIndicator xi = new DirectionIndicator();
    final DirectionIndicator yi = new DirectionIndicator();

    final static int MotionInertia = 4; // frames

    private Dimension size = WebcamResolution.VGA.getSize();
    private Point motionCog = null;
    private double motionArea = 0.0;

    private final Lock motionStartLock = new ReentrantLock();
    private final Condition motionStart = motionStartLock.newCondition();
    private final Lock motionEndLock = new ReentrantLock();
    private final Condition motionEnd = motionEndLock.newCondition();

    public MotionDetector() {
        this(Webcam.getDefault());
    }

    public MotionDetector(final Webcam webcam) {
        if (webcam == null) {
            System.out.println("No webcam detected");
            throw new IllegalArgumentException();
        }
        print(webcam.getName());

        webcam.setViewSize(size);

        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setFPSDisplayed(true);
        panel.setDisplayDebugInfo(true);
        panel.setImageSizeDisplayed(true);
        panel.setMirrored(true);
        panel.setPreferredSize(new Dimension(32, 64));

        JFrame window = new JFrame("Test webcam panel");
        window.add(panel);
        window.setResizable(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.pack();
        window.setVisible(true);

        Thread t = new Thread("motion-detector") {
            private final WebcamMotionDetector detector;
            {
                detector = new WebcamMotionDetector(webcam);
            }

            @Override
            public void run() {
                // This value should be high?
                detector.setInterval(PollingInterval); // one check per 100 ms
                // Longer values join short motions into a single motion, but
                // also increase response time
                // detector.setInertia(500); // keep "motion" state for 100ms
                // seconds
                // Manage inertia ourselves, to avoid wrong isMotion reports
                // with the image center as the image c.o.g.
                detector.clearInertia();
                // How much of the image must move in order to recognize image
                // changes it as motion
                detector.setAreaThreshold(AreaThresholdPercent);
                // Pixels are considered to be changed when they're above the
                // pixel threshold
                detector.setPixelThreshold(PixelThreshold);
                detector.start();
                int motion = -1;
                while (true) {
                    if (detector.isMotion()) {
                        motionCog = detector.getMotionCog();
                        xi.add(motionCog.x);
                        yi.add(motionCog.y);
                        motionArea = detector.getMotionArea();
                    } else {
                        motionCog = null;
                        motionArea = 0.0;
                    }
                    // Compute current direction
                    int frames = frames(MotionSeconds);
                    int threshold = 1; // MotionThreshold;
                    synchronized (MotionDetector.this) {
                        currentHorizontalMotion = getHorizontalMotion(xi
                                .getDirection(frames, threshold));
                        currentVerticalMotion = getVerticalMotion(yi
                                .getDirection(frames, threshold));
                    }
                    // After setting current state, send any notifications
                    if (detector.isMotion()) {
                        if (motion < 0) {
                            printDebug();
                            signalMotionStart();
                        } else {
                            printDebug();
                            signalMotionStart();
                        }
                        motion = MotionInertia;
                    } else {
                        if (motion == 0) {
                            printDebug();
                            signalMotionEnd();
                            motion = -1;
                        } else if (motion > 0) {
                            // inertia
                            motion--;
                            signalMotionStart();
                        }
                    }

                    try {
                        Thread.sleep(PollingInterval - 1); // must be smaller
                                                           // than interval
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }

            public void signalMotionStart() {
                motionStartLock.lock();
                try {
                    motionStart.signalAll();
                } finally {
                    motionStartLock.unlock();
                }
            }

            public void signalMotionEnd() {
                motionEndLock.lock();
                try {
                    motionEnd.signalAll();
                } finally {
                    motionEndLock.unlock();
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private static void print(String text) {
        System.out.println(text);
    }

    private void printDebug() {
        long now = System.currentTimeMillis();
        String debug = currentHorizontalMotion + " " + currentVerticalMotion
                + " MotionArea=" + motionArea;
        if (motionCog != null) {
            debug += " MotionCog=(" + motionCog.x + ", " + motionCog.y + ")";
        }
        System.out.println(now + " " + debug);
    }

    private static HorizontalMotion getHorizontalMotion(Direction dx) {
        if (dx == Direction.Positive) {
            return HorizontalMotion.Right;
        } else if (dx == Direction.Negative) {
            return HorizontalMotion.Left;
        } else {
            return HorizontalMotion.None;
        }
    }

    private static VerticalMotion getVerticalMotion(Direction dy) {
        if (dy == Direction.Positive) {
            return VerticalMotion.Down;
        } else if (dy == Direction.Negative) {
            return VerticalMotion.Up;
        } else {
            return VerticalMotion.None;
        }
    }

    private static int frames(double seconds) {
        if (seconds > 7200.0) {
            return Integer.MAX_VALUE;
        } else {
            return Math.max(1, (int) seconds * (1000 / PollingInterval));
        }
    }

    public void clearDirections() {
        xi.clear();
        yi.clear();
        currentHorizontalMotion = HorizontalMotion.None;
        currentVerticalMotion = VerticalMotion.None;
    }

    public HorizontalMotion getCurrentHorizontalMotion() {
        return currentHorizontalMotion;
    }

    public VerticalMotion getCurrentVerticalMotion() {
        return currentVerticalMotion;
    }

    public synchronized HorizontalMotion getRecentHorizontalMotion(
            double pastSeconds) {
        HorizontalMotion horizontalMotion = getHorizontalMotion(xi
                .getDirection(frames(pastSeconds), MotionThreshold));
        print("getRecentHorizontalMotion(" + pastSeconds + ") returned "
                + horizontalMotion.toString());
        return horizontalMotion;
    }

    public Amount getAmountOfMotion(double pastSeconds) {
        int dx = Math.abs(xi.distance(frames(pastSeconds)));
        int dy = Math.abs(yi.distance(frames(pastSeconds)));
        Amount[] values = Amount.values();
        for (Amount amount : values) {
            if (dx < amount.ScreenPercentage * size.width
                    && dy < amount.ScreenPercentage * size.height) {
                return amount;
            }
        }
        return Amount.ALot;
    }

    public synchronized VerticalMotion getRecentVerticalMotion(
            double pastSeconds) {
        VerticalMotion verticalMotion = getVerticalMotion(yi.getDirection(
                frames(pastSeconds), MotionThreshold));
        print("getRecentVerticalMotion(" + pastSeconds + ") returned "
                + verticalMotion.toString());
        return verticalMotion;
    }

    // todo set length of motion
    public boolean awaitMotionStart(double timeoutSeconds) {
        boolean motionDetected = false;
        motionStartLock.lock();
        try {
            motionDetected = motionStart.await((long) timeoutSeconds * 1000,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            motionStartLock.unlock();
        }
        return motionDetected;
    }

    public boolean awaitMotionEnd(double timeoutSeconds) {
        boolean motionStopped = false;
        motionEndLock.lock();
        try {
            motionStopped = motionEnd.await((long) timeoutSeconds * 1000,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            motionEndLock.unlock();
        }
        return motionStopped;
    }
}