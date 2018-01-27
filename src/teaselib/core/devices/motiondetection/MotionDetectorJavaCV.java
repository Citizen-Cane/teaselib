package teaselib.core.devices.motiondetection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.VideoRenderer;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.concurrency.Signal;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.Devices;
import teaselib.core.javacv.Color;
import teaselib.core.javacv.Copy;
import teaselib.core.javacv.HeadGestureTracker;
import teaselib.core.javacv.Scale;
import teaselib.core.javacv.ScaleAndMirror;
import teaselib.core.javacv.Transformation;
import teaselib.core.javacv.util.Buffer;
import teaselib.core.javacv.util.FramesPerSecond;
import teaselib.core.javacv.util.Geom;
import teaselib.core.util.ExceptionUtil;
import teaselib.motiondetection.Gesture;
import teaselib.motiondetection.MotionDetector;
import teaselib.motiondetection.ViewPoint;
import teaselib.video.VideoCaptureDevice;

/**
 * @author Citizen-Cane
 * 
 *         Bullet-Proof motion detector:
 *         <p>
 *         Motion is detected via motion contours from background subtraction. Sensitivity is implemented by applying a
 *         structuring element to the motion contours.
 *         <p>
 *         Absence of motion is measured by measuring the distance of tracking points from positions set when motion
 *         last stopped. Motion is detected when the tracking points move too far away from their start points.
 *         <p>
 *         Blinking eyes are detected by removing small circular motion contours before calculating the motion region.
 *         <p>
 *         The camera should be placed on top of the monitor with no tilt.
 *         <p>
 *         The detector is stable, but still can be cheated/misled. Av oid:
 *         <p>
 *         pets.
 *         <p>
 *         audience within the field of view-
 *         <p>
 *         light sources from moving cars or traffic lights.
 * 
 */
public class MotionDetectorJavaCV extends MotionDetector /* extends WiredDevice */ {
    private static final Logger logger = LoggerFactory.getLogger(MotionDetectorJavaCV.class);

    private static final String DeviceClassName = "MotionDetectorJavaCV";

    private static final class MyDeviceFactory extends DeviceFactory<MotionDetectorJavaCV> {
        private MyDeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
            super(deviceClass, devices, configuration);
        }

        @Override
        public List<String> enumerateDevicePaths(Map<String, MotionDetectorJavaCV> deviceCache) {
            List<String> deviceNames = new ArrayList<>();
            Set<String> videoCaptureDevicePaths = devices.get(VideoCaptureDevice.class).getDevicePaths();
            for (String videoCaptureDevicePath : videoCaptureDevicePaths) {
                deviceNames.add(DeviceCache.createDevicePath(DeviceClassName, videoCaptureDevicePath));
            }
            return deviceNames;
        }

        @Override
        public MotionDetectorJavaCV createDevice(String deviceName) {
            DeviceCache<VideoCaptureDevice> videoCaptureDevices = devices.get(VideoCaptureDevice.class);
            return new MotionDetectorJavaCV(videoCaptureDevices.getDevice(deviceName));
        }
    }

    public static MyDeviceFactory getDeviceFactory(Devices devices, Configuration configuration) {
        return new MyDeviceFactory(DeviceClassName, devices, configuration);
    }

    public static final Set<Feature> Features = EnumSet.of(Feature.Motion, Feature.Presence);
    private static final double DesiredFps = 30;

    private final CaptureThread eventThread;

    public MotionDetectorJavaCV(VideoCaptureDevice videoCaptureDevice) {
        super();
        eventThread = new CaptureThread(videoCaptureDevice, DesiredFps);
        setSensitivity(MotionSensitivity.Normal);
        setViewPoint(ViewPoint.EyeLevel);
        Thread detectionEventsShutdownHook = new Thread() {
            @Override
            public void run() {
                close();
            }
        };
        Runtime.getRuntime().addShutdownHook(detectionEventsShutdownHook);
        eventThread.setDaemon(false);
        eventThread.setName(getDevicePath());
        eventThread.start();
    }

    static class CaptureThread extends Thread {
        private static final Size DesiredProcessingSize = new Size(640, 480);
        private static final boolean mirror = true;

        private static final Map<MotionSensitivity, Integer> motionSensitivities = initStructuringElementSizes();

        private VideoCaptureDevice videoCaptureDevice;

        private Buffer<Mat> video = new Buffer<>(Mat::new, (int) DesiredFps);

        private Transformation videoInputTransformation;
        private MotionSensitivity motionSensitivity;
        private ViewPoint viewPoint;
        private MotionProcessorJavaCV motionProcessor;
        HeadGestureTracker gestureTracker = new HeadGestureTracker(Color.Cyan);
        protected Gesture gesture = Gesture.None;

        // TODO Atomic reference
        private MotionDetectionResultImplementation presenceResult;

        private final double desiredFps;
        private double fps;
        private FramesPerSecond fpsStatistics;
        private long desiredFrameTimeMillis;

        volatile double debugWindowTimeSpan = PresenceRegionDefaultTimespan;
        private final AtomicBoolean active = new AtomicBoolean(false);
        private final Signal presenceChanged = new Signal();
        private final Signal gestureChanged = new Signal();

        MotionDetectorJavaCVDebugRenderer debugInfo = null;
        VideoRenderer videoRenderer = null;
        boolean logDetails = false;

        CaptureThread(VideoCaptureDevice videoCaptureDevice, double desiredFps) {
            super();
            this.desiredFps = desiredFps;
            this.videoCaptureDevice = videoCaptureDevice;
            gestureTracker.restart();
        }

        private void openVideoCaptureDevice(VideoCaptureDevice videoCaptureDevice) {
            videoCaptureDevice.fps(fps);
            videoCaptureDevice.open();
            videoCaptureDevice
                    .resolution(videoCaptureDevice.getResolutions().getMatchingOrSimilar(DesiredProcessingSize));
            this.fps = FramesPerSecond.getFps(videoCaptureDevice.fps(), desiredFps);
            this.fpsStatistics = new FramesPerSecond((int) fps);
            this.desiredFrameTimeMillis = (long) (1000.0 / fps);
            @SuppressWarnings("resource")
            Size resolution = videoCaptureDevice.resolution();
            @SuppressWarnings("resource")
            Size processingSize = getProcessingSize(resolution);
            this.videoInputTransformation = getVideoTransformation(resolution, processingSize);
            motionProcessor = new MotionProcessorJavaCV(resolution, processingSize);
            presenceResult = new MotionDetectionResultImplementation(processingSize);
            applySensitivity(motionSensitivity);
            applyPointOfView(viewPoint);
            debugInfo = new MotionDetectorJavaCVDebugRenderer(processingSize);
        }

        private static Size getProcessingSize(Size resolution) {
            final Size processingSize;
            if (resolution.equals(DesiredProcessingSize)) {
                processingSize = new Size(resolution);
            } else {
                double factor = Scale.factor(resolution, DesiredProcessingSize);
                processingSize = new Size((int) (resolution.width() * factor), (int) (resolution.height() * factor));
            }
            return processingSize;
        }

        private Transformation getVideoTransformation(Size resolution, Size processingSize) {
            if (mirror) {
                return new ScaleAndMirror(resolution, processingSize, video);
            } else if (resolution.equals(processingSize)) {
                // TODO Move buffers to video capture classes in order to save copies and scale/mirror on video hardware
                return new Copy(video);
            } else {
                return new Scale(resolution, processingSize, video);
            }
        }

        private static EnumMap<MotionSensitivity, Integer> initStructuringElementSizes() {
            EnumMap<MotionSensitivity, Integer> map = new EnumMap<>(MotionSensitivity.class);
            map.put(MotionSensitivity.High, 12);
            map.put(MotionSensitivity.Normal, 24);
            map.put(MotionSensitivity.Low, 36);
            return map;
        }

        public void setSensitivity(MotionSensitivity motionSensitivity) {
            if (motionSensitivity == this.motionSensitivity)
                return;

            boolean isActive = active();
            if (isActive) {
                stopCapture();
            }

            this.motionSensitivity = motionSensitivity;

            if (motionProcessor != null) {
                applySensitivity(motionSensitivity);
            }

            if (isActive) {
                startCapture();
            }
        }

        private void applySensitivity(MotionSensitivity motionSensitivity) {
            motionProcessor.setStructuringElementSize(motionSensitivities.get(motionSensitivity));
        }

        public void setPointOfView(ViewPoint viewPoint) {
            if (viewPoint == this.viewPoint)
                return;

            boolean isActive = active();
            if (isActive) {
                stopCapture();
            }

            this.viewPoint = viewPoint;
            if (presenceResult != null) {
                applyPointOfView(viewPoint);
            }

            if (isActive) {
                startCapture();
            }
        }

        private void applyPointOfView(ViewPoint viewPoint) {
            presenceResult.setViewPoint(viewPoint);
        }

        @Override
        public void run() {
            try {
                // TODO Auto-adjust until frame rate is stable
                // - KNN/findContours uses less cpu without motion
                // -> adjust to < 50% processing time per frame
                while (!isInterrupted()) {
                    // poll the device until its reconnected
                    DeviceCache.connect(videoCaptureDevice);
                    openVideoCaptureDevice(videoCaptureDevice);
                    fpsStatistics.start();
                    processVideoCaptureStream();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        NamedExecutorService frameTasks = NamedExecutorService.newUnlimitedThreadPool("frames", Long.MAX_VALUE,
                TimeUnit.MILLISECONDS);
        NamedExecutorService perceptionTasks = NamedExecutorService.singleThreadedQueue("Motion and Presence",
                Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        Future<?> motionAndPresence = null;
        List<Future<?>> frameTaskFutures = new ArrayList<>();

        private HeadGestureTracker.Parameters gestureResult = new HeadGestureTracker.Parameters();
        Mat motionImageCopy = new Mat();

        private void processVideoCaptureStream() throws InterruptedException {
            synchronized (active) {
                active.notifyAll();
                while (!active.get()) {
                    active.wait();
                }
            }

            try {
                for (Mat frame : videoCaptureDevice) {
                    long timeStamp = System.currentTimeMillis();

                    @SuppressWarnings("resource")
                    Mat image = videoInputTransformation.update(frame);

                    // TODO Better make a pool, take and give buffers back
                    Buffer.Locked<Mat> lock = video.get(image);
                    Mat buffer = lock.get();

                    // TODO Extra thread, independent of motion
                    frameTaskFutures.add(frameTasks.submit(() -> computeGestures(image, timeStamp)));

                    // TODO Extra thread, lower fps, handle distance tracker
                    if (motionAndPresence == null) {
                        motionAndPresence = perceptionTasks.submit(() -> {
                            // TODO Save cpu-cycles by copying to tracker and KNN data directly
                            image.copyTo(motionImageCopy);
                            computeMotionAndPresence(motionImageCopy, timeStamp);
                            // TODO Increase semaphore count
                            // lock.release();
                        });
                        // motionAndPresence.get();
                    } else if (motionAndPresence.isCancelled()) {
                        motionAndPresence = null;
                    } else if (motionAndPresence.isDone()) {
                        motionAndPresence = null;
                        updatePresenceResult(buffer);
                    }

                    long timeLeft = fpsStatistics.timeMillisLeft(desiredFrameTimeMillis, timeStamp);
                    if (timeLeft > 0) {
                        Thread.sleep(timeLeft);
                    }
                    // TODO review frame statistics class - may be wrong
                    fpsStatistics.updateFrame(timeStamp + timeLeft);

                    completeComputationAndRender(image, lock);
                    // TODO resolve OpenCV lack of rendering to its windows in a different thread
                    // frameTasks.submit(() -> {
                    // completeComputationAndRender(image, lock);
                    // }).get();

                    if (active.get() == false) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                videoCaptureDevice.close();
                if (videoRenderer != null) {
                    videoRenderer.close();
                }
                if (debugInfo != null)
                    debugInfo.close();
            }
        }

        private void updatePresenceResult(Mat video) {
            motionProcessor.updateRenderData();
            presenceResult.updateRenderData(1.0, debugWindowTimeSpan);

            gestureResult.cameraShake = presenceResult.presenceData.indicators.contains(Presence.CameraShake);
            gestureResult.motionDetected = presenceResult.motionDetected;
            Rect presence = presenceResult.presenceData.presenceRegion;

            // Enlarge gesture region based on the observation that when beginning the first nod the presence region
            // starts with a horizontally wide but vertically narrow area around the eyes
            if (presence.width() < video.rows() / 10 && presence.height() * 4 < presence.width()) {
                presence = new Rect(presence.x(), presence.y() - presence.width() / 2, presence.width(),
                        presence.width());
            }

            gestureResult.gestureRegion = Geom.intersect(presence,
                    presenceResult.presenceData.presenceIndicators.get(Presence.Present));
        }

        private void computeMotionAndPresence(Mat video, long timeStamp) {
            motionProcessor.update(video);
            motionProcessor.updateTrackerData(video);

            presenceChanged.doLocked(() -> {
                boolean hasChanged = presenceResult.updateMotionState(video, motionProcessor, timeStamp);
                if (hasChanged) {
                    presenceChanged.signal();
                }
            });
        }

        private void computeGestures(Mat video, long timeStamp) {
            if (gestureResult.cameraShake) {
                gestureTracker.restart();
            } else {
                gestureTracker.update(video, gestureResult.motionDetected, gestureResult.gestureRegion, timeStamp);
            }
            Gesture newGesture = gestureTracker.getGesture();
            boolean changed = gesture != newGesture;
            gesture = newGesture;
            if (changed) {
                gestureChanged.signal();
            }
        }

        private void completeComputationAndRender(Mat image, Buffer.Locked<Mat> lock) {
            try {
                for (Future<?> task : frameTaskFutures) {
                    try {
                        task.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        throw ExceptionUtil.asRuntimeException(e);
                    }
                }

                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    return;
                }

                render(image);
            } finally {
                lock.release();
            }
        }

        private void render(Mat video) {
            if (videoRenderer != null) {
                renderOverlays(video);
                videoRenderer.update(video);
            }
        }

        private Set<Presence> renderOverlays(Mat image) {
            Set<Presence> indicators = presenceResult.presenceData.debugIndicators;
            debugInfo.render(image, motionProcessor.motionData, presenceResult.presenceData, gestureTracker, gesture,
                    fpsStatistics.value());

            if (logDetails) {
                // Log state
                logger.info("contourMotionDetected=" + presenceResult.presenceData.contourMotionDetected
                        + "  trackerMotionDetected=" + presenceResult.presenceData.trackerMotionDetected + "(distance="
                        + motionProcessor.motionData.distance2 + "), " + indicators.toString());
            }

            return indicators;
        }

        public void clearMotionHistory() {
            presenceChanged.doLocked(() -> presenceResult.clear());
        }

        public double fps() {
            return fps;
        }

        public boolean active() {
            return active.get();
        }

        public void stopCapture() {
            synchronized (active) {
                active.set(false);
                while (active.get()) {
                    try {
                        active.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        public void startCapture() {
            synchronized (active) {
                boolean isActive = active.getAndSet(true);
                if (!isActive) {
                    active.notifyAll();
                }
            }
        }
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName, eventThread.videoCaptureDevice.getDevicePath());
    }

    @Override
    public String getName() {
        return "Motion Detector";
    }

    @Override
    public MotionSensitivity getSensitivity() {
        return eventThread.motionSensitivity;
    }

    @Override
    public void setSensitivity(MotionSensitivity motionSensitivity) {
        eventThread.setSensitivity(motionSensitivity);
    }

    @Override
    public ViewPoint getViewPoint() {
        return eventThread.viewPoint;
    }

    @Override
    public void setViewPoint(ViewPoint pointOfView) {
        eventThread.setPointOfView(pointOfView);
    }

    @Override
    public void setVideoRenderer(VideoRenderer videoRenderer) {
        eventThread.videoRenderer = videoRenderer;
    }

    @Override
    public Set<Feature> getFeatures() {
        return Features;
    }

    @Override
    public boolean await(double amount, Presence change, double timeSpanSeconds, double timeoutSeconds) {
        if (!active()) {
            awaitTimeout(timeoutSeconds);
            return false;
        }
        eventThread.debugWindowTimeSpan = timeSpanSeconds;
        try {
            return eventThread.presenceResult.await(eventThread.presenceChanged, amount, change, timeSpanSeconds,
                    timeoutSeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } finally {
            eventThread.debugWindowTimeSpan = MotionRegionDefaultTimespan;
        }
    }

    @Override
    public Gesture await(List<Gesture> expected, double timeoutSeconds) {
        if (!active()) {
            throw new IllegalStateException(getClass().getName() + " not active");
        }

        // TODO resolve duplicated code
        if (expected.contains(eventThread.gesture)) {
            return eventThread.gesture;
        }
        try {
            eventThread.gestureChanged.await(timeoutSeconds, (new Signal.HasChangedPredicate() {
                @Override
                public Boolean call() throws Exception {
                    return expected.contains(eventThread.gesture);
                }
            }));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return eventThread.gesture;
    }

    private static void awaitTimeout(double timeoutSeconds) {
        try {
            Thread.sleep((long) (timeoutSeconds * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    @Override
    public boolean isWireless() {
        return eventThread.videoCaptureDevice.isWireless();
    }

    @Override
    public BatteryLevel batteryLevel() {
        return eventThread.videoCaptureDevice.batteryLevel();
    }

    protected double fps() {
        return eventThread.fps();
    }

    @Override
    public void clearMotionHistory() {
        eventThread.clearMotionHistory();
    }

    @Override
    public boolean connected() {
        return eventThread.videoCaptureDevice.connected();
    }

    @Override
    public boolean active() {
        return eventThread.active();
    }

    @Override
    public void stop() {
        eventThread.stopCapture();
    }

    @Override
    public void start() {
        eventThread.startCapture();
    }

    @Override
    public void close() {
        eventThread.interrupt();
        try {
            eventThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
