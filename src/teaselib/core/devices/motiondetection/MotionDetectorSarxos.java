package teaselib.core.devices.motiondetection;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window.Type;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamMotionDetector;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

import teaselib.TeaseLib;
import teaselib.core.devices.DeviceCache;
import teaselib.motiondetection.MotionDetector;

/**
 * Uses webcam to detect motion.
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
 */
public class MotionDetectorSarxos extends BasicMotionDetector {
    public static final String DeviceClassName = "MotionDetectorSarxos";

    public static final EnumSet<Feature> Features = EnumSet.of(Feature.Motion);

    static final double InitialAreaTreshold = 5;
    static final int InitialPixelTreshold = 16;
    protected static final int PollingInterval = 100;

    protected double areaTreshold = InitialAreaTreshold;
    protected int pixelTreshold = InitialPixelTreshold;

    protected final MotionAreaHistory mi = new MotionAreaHistory(
            MaximumNumberOfPastFrames);

    private Dimension ViewSize = WebcamResolution.VGA.getSize();
    JFrame window = null;

    public static Collection<String> getDevices() {
        Collection<String> devices = new Vector<>();
        try {
            List<Webcam> webcams = Webcam.getWebcams();
            for (Webcam webcam : webcams) {
                devices.add(DeviceCache.createDevicePath(DeviceClassName,
                        webcam.getName()));
            }
        } catch (Exception e) {
            TeaseLib.instance().log.error(Webcam.class, e);
        }
        return devices;
    }

    private class DiscoveryListener implements WebcamDiscoveryListener {
        @Override
        public void webcamFound(WebcamDiscoveryEvent event) {
            try {
                attachWebcam(event.getWebcam());
            } catch (Exception e) {
                TeaseLib.instance().log.error(this, e);
            }
        }

        @Override
        public void webcamGone(WebcamDiscoveryEvent event) {
            try {
                detachWebcam(event.getWebcam());
            } catch (Exception e) {
                TeaseLib.instance().log.error(this, e);
            }
        }
    }

    public static synchronized MotionDetector getDefault() {
        return new MotionDetectorSarxos();
    }

    private MotionDetectorSarxos() {
        this(getDefaultWebcam());
        Webcam.addDiscoveryListener(new DiscoveryListener());
    }

    private static Webcam getDefaultWebcam() {
        try {
            return Webcam.getDefault();
        } catch (WebcamException e) {
            TeaseLib.instance().log.error(Webcam.class, e);
            return null;
        }
    }

    public MotionDetectorSarxos(String id) {
        this(findWebcam(DeviceCache.getDeviceName(id)));
    }

    private static Webcam findWebcam(String name) {
        try {
            List<Webcam> webcams = Webcam.getWebcams();
            for (Webcam webcam : webcams) {
                if (webcam.getName().equals(name)) {
                    return webcam;
                }
            }
        } catch (Exception e) {
            TeaseLib.instance().log.error(Webcam.class, e);
            return null;
        }
        TeaseLib.instance().log.info("No webcam detected");
        return null;
    }

    public MotionDetectorSarxos(Webcam webcam) {
        super();
        attachWebcam(webcam);
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                ((DetectionEventsSarxos) detectionEvents).webcam.getName());
    }

    private void attachWebcam(Webcam newWebcam) {
        TeaseLib.instance().log.info(newWebcam.getName() + " connected");
        newWebcam.setViewSize(ViewSize);
        newWebcam.open();
        showWebcamWindow(newWebcam);
        detectionEvents = new DetectionEventsSarxos(newWebcam);
        detectionEvents.setName("Motion detector events");
        detectionEvents.setDaemon(true);
        // Update properties
        ((DetectionEventsSarxos) detectionEvents).setAreaTreshold(areaTreshold);
        ((DetectionEventsSarxos) detectionEvents)
                .setPixelTreshold(pixelTreshold);
        detectionEvents.start();
    }

    private void detachWebcam(Webcam oldWebcam) {
        TeaseLib.instance().log.info(oldWebcam.getName() + " disconnected");
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

    private class DetectionEventsSarxos extends DetectionEvents {
        final Webcam webcam;
        final WebcamMotionDetector detector;

        DetectionEventsSarxos(Webcam webcam) {
            this.webcam = webcam;
            this.detector = new WebcamMotionDetector(webcam);
        }

        public void setAreaTreshold(double areaTreshold) {
            synchronized (MotionDetectorSarxos.this) {
                detector.setAreaThreshold(areaTreshold);
            }
        }

        public void setPixelTreshold(int pixelTreshold) {
            synchronized (MotionDetectorSarxos.this) {
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
                    cleanupResources();
                    // The discovery listener's webcamGone() isn't called
                    hideWebcamWindow();
                    // Just reopening doesn't do the trick, because either the
                    // fps member isn't reset or the webcam object just won't
                    // work anymore
                    Webcam.resetDriver();
                    // Instead of re-initializing, a new thread is created
                    // once the discovery listener has found a new webcam
                    return;
                    // Re-initialize
                    // webcam = Webcam.getDefault();
                    // detector = new WebcamMotionDetector(webcam);
                    // attachWebcam event is triggered, resulting in a new
                    // webcam window
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
                    // TODO Check usage on motion history in basic motion
                    // detector - inertia may add up this way,
                    // and the javacv implementation will just rely on the basic
                    // implementation
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
                    lockStartStop.lockInterruptibly();
                    lockStartStop.unlock();
                } catch (InterruptedException e) {
                    cleanupResources();
                    return;
                }
            }
        }

        private void cleanupResources() {
            detector.stop();
            webcam.close();
        }

        private boolean isDead(Webcam webcam) {
            // This condition is weak, because the webcam has some averaging, so
            // it takes a long time to detect a dead cam
            // TODO FInd out faster detection for dead webcam
            final double fps = webcam.getFPS();
            final boolean isDead = fps > 0.0 && fps < 5.0;
            return isDead;
        }

        private void printDebug() {
            TeaseLib.instance().log
                    .debug("MotionArea=" + detector.getMotionArea());
        }
    }

    @Override
    public void setSensitivity(MotionSensitivity motionSensivity) {
        this.areaTreshold = InitialAreaTreshold;
        ((DetectionEventsSarxos) detectionEvents).setAreaTreshold(areaTreshold);
        this.pixelTreshold = InitialPixelTreshold;
        ((DetectionEventsSarxos) detectionEvents)
                .setPixelTreshold(pixelTreshold);
    }

    @Override
    protected boolean isMotionDetected(int pastFrames) {
        synchronized (mi) {
            double dm = mi.getMotion(pastFrames);
            return dm > areaTreshold;
        }
    }

    @Override
    public EnumSet<Feature> getFeatures() {
        return Features;
    }

    @Override
    public EnumSet<Presence> getPresence() {
        return EnumSet.noneOf(Presence.class);
    }

    @Override
    protected int fps() {
        return 1000 / PollingInterval;
    }

    @Override
    public void clearMotionHistory() {
        synchronized (mi) {
            mi.clear();
        }
    }
}
