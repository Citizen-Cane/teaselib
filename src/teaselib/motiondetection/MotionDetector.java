package teaselib.motiondetection;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window.Type;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import teaselib.TeaseLib;

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

    public static final double InitialAreaTreshold = 5;
    public static final int InitialPixelTreshold = 16;

    private static final int PollingInterval = 100;
    private final static int MotionInertia = 4; // frames
    private final static int MaximumNumberOfPastFrames = 400;

    private final MotionHistory mi = new MotionHistory(
            MaximumNumberOfPastFrames);

    private static MotionDetector instance = null;
    private DetectionEvents detectionEvents = null;
    private Webcam webcam = null;

    private Dimension ViewSize = WebcamResolution.VGA.getSize();

    private double areaTreshold = InitialAreaTreshold;
    private int pixelTreshold = InitialPixelTreshold;

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
        // If we already have a webcam, we can init the properties now,
        // otherwise we have to update them when a new webcam has been
        // discovered
        if (webcam != null) {
            setAreaTreshold(InitialAreaTreshold);
            setPixelTreshold(InitialPixelTreshold);
        }
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
        detectionEvents = new DetectionEvents();
        // Update properties
        detectionEvents.setAreaTreshold(areaTreshold);
        detectionEvents.setPixelTreshold(pixelTreshold);
        detectionEvents.start();
    }

    private void detachWebcam(Webcam oldWebcam) {
        TeaseLib.log(webcam.getName() + " disconnected");
        detectionEvents.interrupt();
        hideWebcamWindow();
        while (detectionEvents.isAlive()) {
            try {
                detectionEvents.join();
            } catch (InterruptedException e) {
            }
        }
        detectionEvents = null;
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

    private class DetectionEvents extends Thread {
        final Lock motionStartLock = new ReentrantLock();
        final Condition motionStart = motionStartLock.newCondition();
        final Lock motionEndLock = new ReentrantLock();
        final Condition motionEnd = motionEndLock.newCondition();

        private final WebcamMotionDetector detector;

        DetectionEvents() {
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
            detector.start();
            int motionDetectedCounter = -1;
            while (!isInterrupted()) {
                // The webcam may freeze (running in VMWare, then opening
                // DirectX game on host), but we can reanimate
                final boolean isDead = isDead(webcam);
                if (isDead) {
                    reanimate();
                }
                boolean motionDetected = detector.isMotion();
                // Build motion and direction frames history
                final double motionArea;
                if (motionDetected) {
                    motionArea = detector.getMotionArea();
                } else {
                    motionArea = 0.0;
                }
                synchronized (mi) {
                    mi.add(motionArea);
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
            TeaseLib.logDetail("MotionArea=" + detector.getMotionArea());
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
        final int frames = (int) seconds * (1000 / PollingInterval);
        return Math.max(1, Math.min(frames, MaximumNumberOfPastFrames));
    }

    /**
     * Percentage of the image that has to change in order to recognize changes
     * it as motion
     * 
     * @param areaTreshold
     */
    public void setAreaTreshold(double areaTreshold) {
        this.areaTreshold = areaTreshold;
        detectionEvents.setAreaTreshold(areaTreshold);
    }

    /**
     * Pixels are considered to have changed when they're above the pixel
     * threshold.
     * 
     * @param pixelTreshold
     */
    public void setPixelTreshold(int pixelTreshold) {
        this.pixelTreshold = pixelTreshold;
        detectionEvents.setPixelTreshold(pixelTreshold);
    }

    public void clearMotionHistory() {
        synchronized (mi) {
            mi.clear();
        }
    }

    public boolean isMotionDetected(double pastSeconds) {
        return isMotionDetected(frames(pastSeconds));
    }

    private boolean isMotionDetected(int pastFrames) {
        synchronized (mi) {
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
        detectionEvents.motionStartLock.lock();
        try {
            boolean motionDetected = isMotionDetected(MotionInertia);
            if (!motionDetected) {
                motionDetected = detectionEvents.motionStart.await(
                        (long) timeoutSeconds * 1000, TimeUnit.MILLISECONDS);
            }
            return motionDetected;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            detectionEvents.motionStartLock.unlock();
        }
    }

    /**
     * @param timeoutSeconds
     * @return True if motion stopped within the time period. False if motion is
     *         still detected at the end of the time period.
     */
    public boolean awaitMotionEnd(double timeoutSeconds) {
        detectionEvents.motionEndLock.lock();
        try {
            boolean motionStopped = !isMotionDetected(MotionInertia);
            if (!motionStopped) {
                motionStopped = detectionEvents.motionEnd.await(
                        (long) timeoutSeconds * 1000, TimeUnit.MILLISECONDS);
            }
            return motionStopped;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            detectionEvents.motionEndLock.unlock();
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