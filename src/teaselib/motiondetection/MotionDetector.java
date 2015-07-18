package teaselib.motiondetection;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;

import teaselib.TeaseLib;
import teaselib.motiondetection.DirectionIndicator.Direction;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamMotionDetector;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class MotionDetector {

    public enum HorizontalMotion {
        None,
        Left,
        Right
    }

    public enum VerticalMotion {
        None,
        Up,
        Down
    }

    public enum Amount {
        None(0),
        JustABit(0.1),
        SomeWhat(0.25),
        ALot(0.5);

        public final double ScreenPercentage;

        Amount(double screenPercentage) {
            this.ScreenPercentage = screenPercentage;
        }
    }

    // todo threshold could be a parameter of getDirection
    private static final int MotionThreshold = 20;
    private static final int MotionSeconds = 1;

    private static final int PollingInterval = 100;
    private static final double AreaThresholdPercent = 10.0;
    private static final int PixelThreshold = 16;

    private final static int MotionInertia = 4; // frames

    private final Lock motionStartLock = new ReentrantLock();
    private final Condition motionStart = motionStartLock.newCondition();
    private final Lock motionEndLock = new ReentrantLock();
    private final Condition motionEnd = motionEndLock.newCondition();

    private final DirectionIndicator xi = new DirectionIndicator();
    private final DirectionIndicator yi = new DirectionIndicator();

    private HorizontalMotion currentHorizontalMotion = HorizontalMotion.None;
    private VerticalMotion currentVerticalMotion = VerticalMotion.None;

    Webcam webcam = null;
    WebCamThread t = null;
    private Dimension ViewSize = WebcamResolution.VGA.getSize();

    private static MotionDetector instance = null;

    JFrame window = null;

    private class DiscoveryListener implements WebcamDiscoveryListener {
        @Override
        public void webcamFound(WebcamDiscoveryEvent event) {
            attachWebcam(event.getWebcam());
        }

        @Override
        public void webcamGone(WebcamDiscoveryEvent event) {
            detachWebcam(event.getWebcam());
        }
    }

    public static synchronized MotionDetector getDefault() {
        if (instance == null) {
            instance = new MotionDetector();
        }
        return instance;
    }

    private MotionDetector() {
        this(Webcam.getDefault());
        Webcam.addDiscoveryListener(new DiscoveryListener());
    }

    public MotionDetector(Webcam webcam) {
        this.webcam = webcam;
        if (webcam == null) {
            TeaseLib.log("No webcam detected");
        } else {
            attachWebcam(webcam);
        }
    }

    private void attachWebcam(Webcam newWebcam) {
        TeaseLib.log(newWebcam.getName() + " connected");
        newWebcam.setViewSize(ViewSize);
        newWebcam.open();
        showWebcamWindow(newWebcam);
        webcam = newWebcam;
        t = new WebCamThread();
        t.start();
    }

    private void detachWebcam(Webcam oldWebcam) {
        TeaseLib.log(webcam.getName() + " disconnected");
        t.interrupt();
        hideWebcamWindow();
        while (t.isAlive()) {
            try {
                t.join();
            } catch (InterruptedException e) {
            }
        }
        t = null;
        oldWebcam.close();
        webcam = null;
    }

    private void showWebcamWindow(Webcam webcam) {
        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setFPSDisplayed(true);
        panel.setDisplayDebugInfo(true);
        panel.setImageSizeDisplayed(true);
        panel.setMirrored(true);
        panel.setPreferredSize(new Dimension(320, 240));
        window = new JFrame("Test webcam panel");
        window.add(panel);
        window.setResizable(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.pack();
        Rectangle r = window.getBounds();
        r.x += 16;
        r.y += 32;
        window.setBounds(r);
        window.setVisible(true);
    }

    private void hideWebcamWindow() {
        window.setVisible(false);
        window.dispose();
    }

    private class WebCamThread extends Thread {
        private final WebcamMotionDetector detector;

        private Point motionCog = null;
        private double motionArea = 0.0;

        WebCamThread() {
            this.detector = new WebcamMotionDetector(webcam);
            setName("motion-detector");
            setDaemon(true);
        }

        @Override
        public void run() {
            // Longer values join short motions into a single motion, but
            // also increase response time
            detector.setInterval(PollingInterval); // one check per 100 ms
            // Manage inertia manually, to avoid wrong isMotion reports
            // with the image center as the image c.o.g.
            detector.clearInertia();
            // Percentage of the image that has to change in order to recognize
            // changes it as motion
            detector.setAreaThreshold(AreaThresholdPercent);
            // Pixels are considered to be changed when they're above the
            // pixel threshold
            detector.setPixelThreshold(PixelThreshold);
            detector.start();
            int motionDetectedCounter = -1;
            while (!isInterrupted()) {
                boolean motionDetected = detector.isMotion();
                synchronized (MotionDetector.this) {
                    if (motionDetected) {
                        motionCog = detector.getMotionCog();
                        xi.add(motionCog.x);
                        yi.add(motionCog.y);
                        motionArea = detector.getMotionArea();
                    } else {
                        motionCog = null;
                        motionArea = 0.0;
                        xi.addLastValueAgain();
                        yi.addLastValueAgain();
                    }
                    // Compute current direction
                    int frames = frames(MotionSeconds);
                    int threshold = 1; // MotionThreshold;
                    currentHorizontalMotion = getHorizontalMotion(xi.direct(
                            frames, threshold));
                    currentVerticalMotion = getVerticalMotion(yi.direct(frames,
                            threshold));
                }
                // After setting current state, send notifications
                if (motionDetected) {
                    if (motionDetectedCounter < 0) {
                        // Motion just started
                        signalMotionStart();
                        printDebug();
                    } else {
                        printDebug();
                    }
                    motionDetectedCounter = MotionInertia;
                } else {
                    if (motionDetectedCounter == 0) {
                        printDebug();
                        signalMotionEnd();
                        motionDetectedCounter = -1;
                    } else if (motionDetectedCounter > 0) {
                        // inertia
                        motionDetectedCounter--;
                    }
                }
                try {
                    // sleep time must be smaller than interval
                    Thread.sleep(PollingInterval - 1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        private void printDebug() {
            String debug = currentHorizontalMotion + " "
                    + currentVerticalMotion + " MotionArea=" + motionArea;
            if (motionCog != null) {
                debug += " MotionCog=(" + motionCog.x + ", " + motionCog.y
                        + ")";
            }
            TeaseLib.logDetail(debug);
        }

        public void signalMotionStart() {
            motionStartLock.lock();
            try {
                TeaseLib.log("Motion started");
                motionStart.signalAll();
            } finally {
                motionStartLock.unlock();
            }
        }

        public void signalMotionEnd() {
            motionEndLock.lock();
            try {
                TeaseLib.log("Motion ended");
                motionEnd.signalAll();
            } finally {
                motionEndLock.unlock();
            }
        }
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
        synchronized (this) {
            xi.clear();
            yi.clear();
            currentHorizontalMotion = HorizontalMotion.None;
            currentVerticalMotion = VerticalMotion.None;
        }
    }

    public HorizontalMotion getCurrentHorizontalMotion() {
        synchronized (this) {
            return currentHorizontalMotion;
        }
    }

    public VerticalMotion getCurrentVerticalMotion() {
        synchronized (this) {
            return currentVerticalMotion;
        }
    }

    public HorizontalMotion getRecentHorizontalMotion(double pastSeconds) {
        synchronized (this) {
            HorizontalMotion horizontalMotion = getHorizontalMotion(xi.direct(
                    frames(pastSeconds), MotionThreshold));
            TeaseLib.log("getRecentHorizontalMotion(" + pastSeconds
                    + ") returned " + horizontalMotion.toString());
            return horizontalMotion;
        }
    }

    public Amount getAmountOfMotion(double pastSeconds) {
        return getAmountOfMotion(frames(pastSeconds));
    }

    public Amount getAmountOfMotion(int pastFrames) {
        synchronized (this) {
            int dx = Math.abs(xi.distance(pastFrames));
            int dy = Math.abs(yi.distance(pastFrames));
            Amount[] values = Amount.values();
            for (Amount amount : values) {
                if (dx <= amount.ScreenPercentage * ViewSize.width
                        && dy <= amount.ScreenPercentage * ViewSize.height) {
                    return amount;
                }
            }
            return Amount.None;
        }
    }

    public VerticalMotion getRecentVerticalMotion(double pastSeconds) {
        synchronized (this) {
            VerticalMotion verticalMotion = getVerticalMotion(yi.direct(
                    frames(pastSeconds), MotionThreshold));
            TeaseLib.log("getRecentVerticalMotion(" + pastSeconds
                    + ") returned " + verticalMotion.toString());
            return verticalMotion;
        }
    }

    // todo set length of motion
    /**
     * Waits the specified period for motion.
     * 
     * @param timeoutSeconds
     * @return True if motion started within the time period. False if no motion
     *         was detected during the time period.
     */
    public boolean awaitMotionStart(double timeoutSeconds) {
        motionStartLock.lock();
        try {
            boolean motionDetected = getAmountOfMotion(MotionInertia) != MotionDetector.Amount.None;
            if (!motionDetected) {
                motionDetected = motionStart.await(
                        (long) timeoutSeconds * 1000, TimeUnit.MILLISECONDS);
            }
            return motionDetected;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            motionStartLock.unlock();
        }
    }

    /**
     * @param timeoutSeconds
     * @return True if motion stopped within the time period. False if motion is
     *         still detected at the end of the time period.
     */
    // todo must return immediately when there isn't any motion
    public boolean awaitMotionEnd(double timeoutSeconds) {
        motionEndLock.lock();
        try {
            boolean motionStopped = getAmountOfMotion(MotionInertia) == MotionDetector.Amount.None;
            if (!motionStopped) {
                motionStopped = motionEnd.await((long) timeoutSeconds * 1000,
                        TimeUnit.MILLISECONDS);
            }
            return motionStopped;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            motionEndLock.unlock();
        }
    }

    /**
     * Webcam connected and processing is enabled
     * 
     * @return
     */
    public boolean active() {
        // todo listen to connection/disconnection events
        return webcam != null;
    }
}