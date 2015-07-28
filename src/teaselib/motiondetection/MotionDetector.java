package teaselib.motiondetection;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window.Type;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import teaselib.TeaseLib;
import teaselib.motiondetection.DirectionHistory.Direction;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamMotionDetector;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

/**
 * Uses webcams to detect motion.
 * 
 * In order to achieve a high frame rate, the camera live image resolution is
 * set to VGA resolution.
 * 
 * However, in poor lighting conditions, the frame rate may drop because the
 * camera automatically increases its exposure time in order to compensate the
 * poor lighting. This typically results in a frame rate drop to below 10
 * frames. The overall brightness of the live image may also alternate between
 * dark and bright, if the camera can't decide between dark and bright. In this
 * case, motion detection will fail.
 * 
 * As a result, good lighting is a must to be successful with motion detection.
 * 
 * 
 * @author someone
 *
 */
public class MotionDetector {

    public static final double InitialAreaTreshold = 10;
    public static final int InitialPixelThreshold = 16;

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

    public enum DirectionAmount {
        None(0),
        JustABit(0.1),
        SomeWhat(0.25),
        ALot(0.5);

        public final double ScreenPercentage;

        DirectionAmount(double screenPercentage) {
            this.ScreenPercentage = screenPercentage;
        }
    }

    private static final int MotionThreshold = 20;
    private static final int MotionSeconds = 1;

    private static final int PollingInterval = 100;
    private final static int MotionInertia = 4; // frames
    private final static int NumberOfPastFrames = 400;

    private final Lock motionStartLock = new ReentrantLock();
    private final Condition motionStart = motionStartLock.newCondition();
    private final Lock motionEndLock = new ReentrantLock();
    private final Condition motionEnd = motionEndLock.newCondition();

    private final DirectionHistory xi = new DirectionHistory(NumberOfPastFrames);
    private final DirectionHistory yi = new DirectionHistory(NumberOfPastFrames);
    private final MotionHistory mi = new MotionHistory(NumberOfPastFrames);

    private HorizontalMotion currentHorizontalMotion = HorizontalMotion.None;
    private VerticalMotion currentVerticalMotion = VerticalMotion.None;

    private Webcam webcam = null;
    private WebCamThread t = null;
    private Dimension ViewSize = WebcamResolution.VGA.getSize();

    private static MotionDetector instance = null;

    JFrame window = null;

    private class DiscoveryListener implements WebcamDiscoveryListener {
        @Override
        public void webcamFound(WebcamDiscoveryEvent event) {
            try {
                attachWebcam(event.getWebcam());
            } catch (Exception e) {
                TeaseLib.log(this, e);
            }
        }

        @Override
        public void webcamGone(WebcamDiscoveryEvent event) {
            try {
                detachWebcam(event.getWebcam());
            } catch (Exception e) {
                TeaseLib.log(this, e);
            }
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
        panel.setPreferredSize(new Dimension(160, 120));
        window = new JFrame("Test webcam panel");
        window.add(panel);
        window.setResizable(true);
        window.setType(Type.POPUP);
        window.setAlwaysOnTop(true);
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.pack();
        Rectangle screen = window.getGraphicsConfiguration().getBounds();
        Rectangle r = window.getBounds();
        r.x = screen.x + screen.width - r.width - 16;
        r.y = screen.y + 32;
        window.setBounds(r);
        window.setVisible(true);
    }

    private void hideWebcamWindow() {
        window.setVisible(false);
        window.dispose();
        window = null;
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

        public void setAreaTreshold(double areaTreshold) {
            synchronized (MotionDetector.this) {
                detector.setAreaThreshold(areaTreshold);
            }
        }

        public void setPixelTreshold(int pixelTreshold) {
            synchronized (MotionDetector.this) {
                detector.setPixelThreshold(pixelTreshold);
            }
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
            detector.setAreaThreshold(InitialAreaTreshold);
            // Pixels are considered to have changed when they're above the
            // pixel threshold
            detector.setPixelThreshold(InitialPixelThreshold);
            detector.start();
            int motionDetectedCounter = -1;
            while (!isInterrupted()) {
                // The webcam may freeze (running in VMWare, then opening
                // DirectX game on host), but we can reanimate
                final boolean isDead = isDead(webcam);
                if (isDead) {
                    reanimate();
                }
                synchronized (MotionDetector.this) {
                    boolean motionDetected = detector.isMotion();
                    // Build motion and direction frames history
                    if (motionDetected) {
                        motionCog = detector.getMotionCog();
                        xi.add(motionCog.x);
                        yi.add(motionCog.y);
                        motionArea = detector.getMotionArea();
                    } else {
                        motionCog = null;
                        xi.addLastValueAgain();
                        yi.addLastValueAgain();
                        motionArea = 0.0;
                    }
                    mi.add(motionArea);
                    // Compute current direction
                    int frames = frames(MotionSeconds);
                    int threshold = 1; // MotionThreshold;
                    currentHorizontalMotion = getHorizontalMotion(xi.direction(
                            frames, threshold));
                    currentVerticalMotion = getVerticalMotion(yi.direction(
                            frames, threshold));
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
                }
                try {
                    // sleep time must be smaller than interval
                    Thread.sleep(PollingInterval - 1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        private boolean isDead(Webcam webcam) {
            // This condition is weak, because the webcam has some averaging, so
            // it takes a long time to detect a dead cam
            // TODO FInd out faster detection for dead webcam
            final double fps = webcam.getFPS();
            final boolean isDead = fps > 0.0 && fps < 5.0;
            return isDead;
        }

        private void reanimate() {
            // This doesn't seem to trigger the detachWebcam event
            webcam.close();
            hideWebcamWindow();
            Webcam.resetDriver();
            // Just reopening doesn't do the trick, because either the
            // fps member isn't reset or the webcam object just won't
            // work anymore
            webcam = Webcam.getDefault();
            // This triggers the attachWebcam event, so we don't have to
            // reopen the window manually
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

    private static int frames(double seconds) {
        if (seconds > 7200.0) {
            return Integer.MAX_VALUE;
        } else {
            return Math.max(1, (int) seconds * (1000 / PollingInterval));
        }
    }

    public void clearDirectionHistory() {
        synchronized (this) {
            xi.clear();
            yi.clear();
            currentHorizontalMotion = HorizontalMotion.None;
            currentVerticalMotion = VerticalMotion.None;
        }
    }

    public void setAreaTreshold(double areaTreshold) {
        t.setAreaTreshold(areaTreshold);
    }

    public void setPixelTreshold(int pixelTreshold) {
        t.setPixelTreshold(pixelTreshold);
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
            HorizontalMotion horizontalMotion = getHorizontalMotion(xi
                    .direction(frames(pastSeconds), MotionThreshold));
            TeaseLib.log("getRecentHorizontalMotion(" + pastSeconds
                    + ") returned " + horizontalMotion.toString());
            return horizontalMotion;
        }
    }

    public DirectionAmount getAmountOfDirection(double pastSeconds) {
        return getAmountOfDirection(frames(pastSeconds));
    }

    public DirectionAmount getAmountOfDirection(int pastFrames) {
        synchronized (this) {
            int dx = Math.abs(xi.distance(pastFrames));
            int dy = Math.abs(yi.distance(pastFrames));
            DirectionAmount[] values = DirectionAmount.values();
            for (DirectionAmount amount : values) {
                if (dx <= amount.ScreenPercentage * ViewSize.width
                        && dy <= amount.ScreenPercentage * ViewSize.height) {
                    return amount;
                }
            }
            return DirectionAmount.None;
        }
    }

    public VerticalMotion getRecentVerticalMotion(double pastSeconds) {
        synchronized (this) {
            VerticalMotion verticalMotion = getVerticalMotion(yi.direction(
                    frames(pastSeconds), MotionThreshold));
            TeaseLib.log("getRecentVerticalMotion(" + pastSeconds
                    + ") returned " + verticalMotion.toString());
            return verticalMotion;
        }
    }

    public void clearMotionHistory() {
        synchronized (this) {
            mi.clear();
        }
    }

    public boolean isMotionDetected(double pastSeconds, DirectionAmount amount) {
        return isMotionDetected(frames(pastSeconds), amount);
    }

    public boolean isMotionDetected(int pastFrames, double areaTreshold) {
        synchronized (this) {
            double dm = mi.getMotion(pastFrames);
            return dm > areaTreshold;
        }
    }

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
            t.setAreaTreshold(InitialAreaTreshold);
            boolean motionDetected = isMotionDetected(MotionInertia,
                    InitialAreaTreshold);
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
    public boolean awaitMotionEnd(double timeoutSeconds) {
        motionEndLock.lock();
        try {
            t.setAreaTreshold(InitialAreaTreshold);
            boolean motionStopped = !isMotionDetected(MotionInertia,
                    InitialAreaTreshold);
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
        return webcam != null;
    }
}