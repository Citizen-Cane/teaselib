package teaselib.core.devices.motiondetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.VideoRenderer;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.motiondetection.MotionDetectionResultImplementation.PresenceData;
import teaselib.core.javacv.Copy;
import teaselib.core.javacv.HeadGestureTracker;
import teaselib.core.javacv.Scale;
import teaselib.core.javacv.ScaleAndMirror;
import teaselib.core.javacv.Transformation;
import teaselib.core.javacv.util.Buffer;
import teaselib.core.javacv.util.FramesPerSecond;
import teaselib.core.javacv.util.Geom;
import teaselib.motiondetection.MotionDetector;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.motiondetection.ViewPoint;
import teaselib.video.VideoCaptureDevice;

class MotionDetectorCaptureThread extends Thread {
    static final Logger logger = LoggerFactory.getLogger(MotionDetectorJavaCV.class);

    private static final Size DesiredProcessingSize = new Size(640, 480);
    private static final boolean MIRROR = true;

    VideoCaptureDevice videoCaptureDevice;
    private Buffer<Mat> video = new Buffer<>(Mat::new, (int) MotionDetectorJavaCV.DesiredFps);
    private Transformation videoInputTransformation;

    MotionSensitivity motionSensitivity;
    ViewPoint viewPoint;

    final MotionSource motion = new MotionSource();
    final GestureSource gesture = new GestureSource(this::active);

    private final double desiredFps;
    private double fps;
    private FramesPerSecond fpsStatistics;
    private long desiredFrameTimeMillis;

    volatile double debugWindowTimeSpan = MotionDetector.PresenceRegionDefaultTimespan;
    private final AtomicBoolean active = new AtomicBoolean(false);

    MotionDetectorJavaCVDebugRenderer debugInfo = null;
    VideoRenderer videoRenderer = null;
    boolean logDetails = false;

    MotionDetectorCaptureThread(VideoCaptureDevice videoCaptureDevice, double desiredFps) {
        super();
        this.desiredFps = desiredFps;
        this.videoCaptureDevice = videoCaptureDevice;
    }

    private void openVideoCaptureDevice(VideoCaptureDevice videoCaptureDevice) {
        videoCaptureDevice.fps(fps);
        videoCaptureDevice.open();
        videoCaptureDevice.resolution(videoCaptureDevice.getResolutions().getMatchingOrSimilar(DesiredProcessingSize));
        this.fps = FramesPerSecond.getFps(videoCaptureDevice.fps(), desiredFps);
        this.fpsStatistics = new FramesPerSecond((int) fps);
        this.desiredFrameTimeMillis = (long) (1000.0 / fps);

        Size resolution = videoCaptureDevice.resolution();
        processingSize = getProcessingSize(resolution);
        this.videoInputTransformation = getVideoTransformation(resolution, processingSize);
        motion.setResulution(processingSize);
        motion.applySensitivity(resolution, processingSize, motionSensitivity);
        motion.applyPointOfView(viewPoint);
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
        if (MIRROR) {
            return new ScaleAndMirror(resolution, processingSize, video);
        } else if (resolution.equals(processingSize)) {
            // TODO Move buffers to video capture classes in order to save copies and scale/mirror on video hardware
            return new Copy(video);
        } else {
            return new Scale(resolution, processingSize, video);
        }
    }

    public void setSensitivity(MotionSensitivity motionSensitivity) {
        if (motionSensitivity == this.motionSensitivity)
            return;

        boolean isActive = active();
        if (isActive) {
            stopCapture();
        }

        this.motionSensitivity = motionSensitivity;
        if (isActive) {
            motion.applySensitivity(videoCaptureDevice.resolution(), processingSize, motionSensitivity);
        }

        if (isActive) {
            startCapture();
        }
    }

    public void setPointOfView(ViewPoint viewPoint) {
        if (viewPoint == this.viewPoint)
            return;

        boolean isActive = active();
        if (isActive) {
            stopCapture();
        }

        this.viewPoint = viewPoint;
        if (isActive) {
            motion.applyPointOfView(viewPoint);
        }

        if (isActive) {
            startCapture();
        }
    }

    @Override
    public void run() {
        try {
            // TODO Auto-adjust until frame rate is stable
            // - KNN/findContours uses less cpu without motion
            // -> adjust to < 50% processing time per frame
            while (!isInterrupted()) {
                synchronized (active) {
                    active.notifyAll();
                    while (!active.get()) {
                        active.wait();
                    }
                }

                DeviceCache.connect(videoCaptureDevice);
                openVideoCaptureDevice(videoCaptureDevice);
                fpsStatistics.start();
                try {
                    processVideoCaptureStream();
                } finally {
                    videoCaptureDevice.close();
                    if (videoRenderer != null) {
                        videoRenderer.close();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            shutdown(frameTasks);
            shutdown(perceptionTasks);
            shutdown(overlayRenderer);
        }
    }

    public void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private NamedExecutorService frameTasks = NamedExecutorService.newFixedThreadPool(2, "frame tasks", Long.MAX_VALUE,
            TimeUnit.MILLISECONDS);
    private NamedExecutorService perceptionTasks = NamedExecutorService.singleThreadedQueue("Motion and Presence",
            Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    private NamedExecutorService overlayRenderer = NamedExecutorService
            .singleThreadedQueue("Perception overlay renderer", Long.MAX_VALUE, TimeUnit.MILLISECONDS);

    private Future<?> motionAndPresence = null;
    private Mat motionImageCopy = new Mat();

    private Size processingSize;

    private void processVideoCaptureStream() throws InterruptedException, ExecutionException {
        provideFakeMotionAndPresenceData();

        boolean warmedUp = false;
        int warmupFrames = MotionProcessorJavaCV.WARMUP_FRAMES;

        List<Future<?>> frameTaskFutures = new ArrayList<>();
        for (Mat frame : videoCaptureDevice) {
            long timeStamp = System.currentTimeMillis();
            // TODO Retrieving buffer from video hidden in transformations, code duplicated
            Mat image = videoInputTransformation.update(frame);
            Buffer.Locked<Mat> lock = video.get(image);
            // Mat buffer = lock.get();

            complete(frameTaskFutures);
            frameTaskFutures.clear();
            // TODO move to after if-clause after resolving image mat copy issue
            // - because copying the mat and computing gestures can be done parallel
            frameTaskFutures.add(frameTasks.submit(() -> gesture.update(image, timeStamp)));

            if (motionAndPresence == null) {
                motionAndPresence = perceptionTasks.submit(() -> {
                    // TODO Save cpu-cycles by copying to tracker and KNN data directly
                    image.copyTo(motionImageCopy);
                    computeMotionAndPresence(motionImageCopy, timeStamp);
                    // TODO Increase semaphore count
                    // lock.release();
                });
            } else if (motionAndPresence.isCancelled()) {
                motionAndPresence = null;
            } else if (motionAndPresence.isDone()) {
                motionAndPresence = null;
                if (!warmedUp) {
                    if (warmupFrames > 0) {
                        --warmupFrames;
                    } else {
                        Set<Presence> presence = motion.presenceResult.getPresence();
                        warmedUp = presence.contains(Presence.NoCameraShake);
                        if (warmedUp) {
                            Set<Presence> history = presence;
                            motion.presenceResult.clear(history);
                        }
                    }
                }
                if (warmedUp) {
                    motion.updatePresenceResult();
                    updateGestureResult(image);
                }
            }
            long timeLeft = fpsStatistics.timeMillisLeft(desiredFrameTimeMillis, timeStamp);
            if (timeLeft > 0) {
                Thread.sleep(timeLeft);
            }
            // TODO review frame statistics class - may be wrong
            fpsStatistics.updateFrame(timeStamp + timeLeft);
            completeComputationAndRender(image, lock, new ArrayList<>(frameTaskFutures));

            if (!active.get() || Thread.currentThread().isInterrupted()) {
                break;
            }
        }
    }

    private void provideFakeMotionAndPresenceData() {
        motion.updateResult(defaultHeadRegion());
        gesture.updateResult(false, true, defaultHeadRegion());
    }

    private void updateGestureResult(Mat video) {
        Rect gestureRegion = Geom.intersect(
                HeadGestureTracker.enlargePresenceRegionToFaceSize(video,
                        motion.presenceResult.presenceData.presenceRegion),
                motion.presenceResult.presenceData.presenceIndicators.get(Presence.Present));
        gesture.updateResult(motion.presenceResult.presenceData.indicators.contains(Presence.CameraShake),
                motion.presenceResult.motionDetected, gestureRegion != null ? gestureRegion : defaultHeadRegion());
    }

    private Rect defaultHeadRegion() {
        return motion.defaultRegion();
    }

    private void computeMotionAndPresence(Mat video, long timeStamp) {
        motion.update(video, timeStamp);
    }

    private void completeComputationAndRender(Mat image, Buffer.Locked<Mat> lock, List<Future<?>> tasks) {
        overlayRenderer.submit(() -> {
            try {
                complete(tasks);
                render(image);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }
        });
    }

    private static void complete(List<Future<?>> tasks) throws InterruptedException, ExecutionException {
        for (Future<?> task : tasks) {
            task.get();
        }
    }

    private void render(Mat video) {
        if (videoRenderer != null) {
            renderOverlays(video);
            videoRenderer.update(video);
        }
    }

    private Set<Presence> renderOverlays(Mat image) {
        PresenceData presenceData = motion.presenceResult.presenceData;
        Set<Presence> indicators = presenceData.debugIndicators;
        debugInfo.render(image, motion.motionProcessor.motionContours, motion.motionProcessor.motionData, presenceData,
                gesture.gestureTracker, gesture.current.get(), fpsStatistics.value());

        if (logDetails) {
            logger.info("contourMotionDetected={}  trackerMotionDetected={} (distance={}), {}",
                    presenceData.contourMotionDetected, presenceData.trackerMotionDetected,
                    motion.motionProcessor.motionData.distance2, indicators);
        }

        return indicators;
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