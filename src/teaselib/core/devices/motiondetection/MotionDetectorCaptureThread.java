package teaselib.core.devices.motiondetection;

import java.util.ArrayList;
import java.util.EnumMap;
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

import teaselib.core.VideoRenderer;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.concurrency.Signal;
import teaselib.core.devices.DeviceCache;
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
import teaselib.motiondetection.ViewPoint;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.video.VideoCaptureDevice;

class MotionDetectorCaptureThread extends Thread {
    private static final Size DesiredProcessingSize = new Size(640, 480);
    private static final boolean mirror = true;

    private static final Map<MotionSensitivity, Integer> motionSensitivities = initStructuringElementSizes();

    VideoCaptureDevice videoCaptureDevice;

    private Buffer<Mat> video = new Buffer<>(Mat::new, (int) MotionDetectorJavaCV.DesiredFps);

    private Transformation videoInputTransformation;
    MotionSensitivity motionSensitivity;
    ViewPoint viewPoint;
    private MotionProcessorJavaCV motionProcessor;
    HeadGestureTracker gestureTracker = new HeadGestureTracker(Color.Cyan);
    protected Gesture gesture = Gesture.None;

    // TODO Atomic reference
    MotionDetectionResultImplementation presenceResult;

    private final double desiredFps;
    private double fps;
    private FramesPerSecond fpsStatistics;
    private long desiredFrameTimeMillis;

    volatile double debugWindowTimeSpan = MotionDetectorJavaCV.PresenceRegionDefaultTimespan;
    private final AtomicBoolean active = new AtomicBoolean(false);
    final Signal presenceChanged = new Signal();
    final Signal gestureChanged = new Signal();

    MotionDetectorJavaCVDebugRenderer debugInfo = null;
    VideoRenderer videoRenderer = null;
    boolean logDetails = false;

    MotionDetectorCaptureThread(VideoCaptureDevice videoCaptureDevice, double desiredFps) {
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
            MotionDetectorJavaCV.logger.error(e.getMessage(), e);
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
            MotionDetectorJavaCV.logger.error(e.getMessage(), e);
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
            MotionDetectorJavaCV.logger.info("contourMotionDetected=" + presenceResult.presenceData.contourMotionDetected
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