package teaselib.core.ai.perception;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static teaselib.core.ai.perception.HeadGesturesV2InputMethod.Nod.Down;
import static teaselib.core.ai.perception.HeadGesturesV2InputMethod.Nod.Up;
import static teaselib.core.ai.perception.HeadGesturesV2InputMethod.Shake.Left;
import static teaselib.core.ai.perception.HeadGesturesV2InputMethod.Shake.Right;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.Actor;
import teaselib.Answer;
import teaselib.Answer.Meaning;
import teaselib.core.ai.perception.HumanPose.Estimation.Gaze;
import teaselib.core.ai.perception.HumanPose.HeadGestures;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ui.AbstractInputMethod;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Result;

public class HeadGesturesV2InputMethod extends AbstractInputMethod {
    private final HumanPoseDeviceInteraction humanPoseInteraction;

    private static final float rad2deg = (float) (180.0d / Math.PI);

    enum Gesture {
        Nod,
        Shake,
        None
    }

    enum Nod implements FunctionPattern.Direction {
        Up,
        Down,
    }

    enum Shake implements FunctionPattern.Direction {
        Left,
        Right
    }

    private final List<FunctionPattern.Direction> nodDown = asList(Down, Up, Down, Up, Down, Up);
    private final List<FunctionPattern.Direction> nodUp = asList(Up, Down, Up, Down, Up, Down);
    private final FunctionPattern nodding = new FunctionPattern(12, 3, SECONDS, Up, Down, nodDown, nodUp);

    private final List<FunctionPattern.Direction> shakeRight = asList(Right, Left, Right, Left, Right, Left);
    private final List<FunctionPattern.Direction> shakeLeft = asList(Left, Right, Left, Right, Left, Right);
    private final FunctionPattern shaking = new FunctionPattern(12, 3, SECONDS, Right, Left, shakeRight, shakeLeft);

    private final Lock gestureDetectionLock = new ReentrantLock();
    private final Condition gestureDetected = gestureDetectionLock.newCondition();
    private final AtomicReference<Gesture> detectedGesture = new AtomicReference<>(Gesture.None);

    public HeadGesturesV2InputMethod(HumanPoseDeviceInteraction humanPoseInteraction, ExecutorService executor) {
        super(executor);
        this.humanPoseInteraction = humanPoseInteraction;
    }

    public final HumanPoseDeviceInteraction.EventListener trackProximity = new HumanPoseDeviceInteraction.EventListener(
            HumanPose.Interest.Proximity) {

        long face2faceLast = 0;

        @Override
        public void run(PoseEstimationEventArgs eventArgs) throws Exception {
            activePrompt.updateAndGet(prompt -> {
                var actor = eventArgs.actor;
                if (prompt == null) {
                    stopGazeTracking(actor);
                } else {
                    Optional<Proximity> proximity = eventArgs.pose.aspect(Proximity.class);
                    boolean face2face;
                    if (proximity.isPresent()) {
                        if (proximity.get() == Proximity.FACE2FACE) {
                            Optional<Gaze> gaze = eventArgs.pose.estimation.gaze;
                            if (gaze.isPresent()) {
                                if (gaze.get().isFace2Face()) {
                                    if (!humanPoseInteraction.containsEventListener(actor, trackGaze)) {
                                        clearTimeline(gaze.get());
                                        startGazeTracking(actor);
                                    }
                                    face2face = true;
                                    face2faceLast = eventArgs.timestamp;
                                } else {
                                    long duration = eventArgs.timestamp - face2faceLast;
                                    face2face = duration <= shaking.maxDuration(MILLISECONDS);
                                }
                            } else {
                                face2face = false;
                            }
                        } else {
                            face2face = false;
                        }
                    } else {
                        face2face = false;
                    }

                    if (!face2face) {
                        stopGazeTracking(actor);
                    }
                }
                return prompt;
            });
        }

        private void clearTimeline(Gaze gaze) {
            detectedGesture.set(Gesture.None);
            long timestamp = System.currentTimeMillis();
            nodding.clear(gaze.nod * rad2deg, timestamp, MILLISECONDS);
            shaking.clear(gaze.shake * rad2deg, timestamp, MILLISECONDS);
        }

        private void startGazeTracking(Actor actor) {
            humanPoseInteraction.addEventListener(actor, trackGaze);
        }
    };

    public final HumanPoseDeviceInteraction.EventListener trackGaze = new HumanPoseDeviceInteraction.EventListener(
            HumanPose.Interest.HeadGestures) {

        @Override
        public void run(PoseEstimationEventArgs eventArgs) throws Exception {
            if (eventArgs.pose.is(HeadGestures.Gaze)) {
                var gaze = eventArgs.pose.estimation.gaze.orElseThrow();
                boolean nod = nodding.update(eventArgs.timestamp, gaze.nod * rad2deg);
                boolean shake = shaking.update(eventArgs.timestamp, gaze.shake * rad2deg);
                if (nod && !shake) {
                    signal(eventArgs, Gesture.Nod);
                } else if (shake && !nod) {
                    signal(eventArgs, Gesture.Shake);
                }
            }
        }

        private void signal(PoseEstimationEventArgs eventArgs, Gesture gesture) {
            gestureDetectionLock.lock();
            try {
                detectedGesture.set(gesture);
                gestureDetected.signalAll();
                humanPoseInteraction.removeEventListener(eventArgs.actor, this);
            } finally {
                gestureDetectionLock.unlock();
            }
        }

    };

    private boolean trackGestures = false;

    @Override
    public Setup getSetup(Choices choices) {
        return () -> trackGestures = distinctGestures(choices);
    }

    @Override
    protected Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        if (trackGestures) {
            updateUI(prompt.initialState.get());
            while (!Thread.currentThread().isInterrupted()) {
                gestureDetectionLock.lockInterruptibly();
                try {
                    gestureDetected.await();
                    var meaning = meaning(detectedGesture.get());
                    Optional<Choice> choice = prompt.choices.stream().filter(c -> c.answer.meaning == meaning)
                            .findFirst();
                    if (choice.isPresent()) {
                        return new Result(prompt.choices.indexOf(choice.get()));
                    }
                } finally {
                    gestureDetectionLock.unlock();
                }
            }
        } else {
            Thread.sleep(Long.MAX_VALUE);
        }
        return Result.UNDEFINED;
    }

    private static Meaning meaning(Gesture gesture) {
        if (gesture == Gesture.Nod) {
            return Meaning.YES;
        } else if (gesture == Gesture.Shake) {
            return Meaning.NO;
        } else {
            return null;
        }
    }

    @Override
    public void updateUI(UiEvent event) {
        var actor = activePrompt.get().script.actor;
        if (event.enabled) {
            // TODO just track gaze since proximity handled by UiEvent
            startProximityTracking(actor);
        } else {
            stopProximityTracking(actor);
            stopGazeTracking(actor);
        }
    }

    @Override
    protected void handleDismiss(Prompt prompt) throws InterruptedException {
        var actor = prompt.script.actor;
        stopProximityTracking(actor);
        stopGazeTracking(actor);
    }

    private void startProximityTracking(Actor actor) {
        humanPoseInteraction.addEventListener(actor, trackProximity);
    }

    private void stopProximityTracking(Actor actor) {
        if (humanPoseInteraction.containsEventListener(actor, trackProximity)) {
            humanPoseInteraction.removeEventListener(actor, trackProximity);
        }
    }

    private void stopGazeTracking(Actor actor) {
        if (humanPoseInteraction.containsEventListener(actor, trackGaze)) {
            humanPoseInteraction.removeEventListener(actor, trackGaze);
        }
    }

    public static boolean distinctGestures(Choices choices) {
        boolean singleYes = choices.stream().map(choice -> choice.answer)
                .filter(answer -> answer.meaning == Meaning.YES).count() == 1;
        boolean singleNo = choices.stream().map(choice -> choice.answer).filter(answer -> answer.meaning == Meaning.NO)
                .count() == 1;
        return singleYes || singleNo;
    }

    public static boolean distinctGestures(List<Answer> answers) {
        boolean singleYes = answers.stream().filter(answer -> answer.meaning == Meaning.YES).count() == 1;
        boolean singleNo = answers.stream().filter(answer -> answer.meaning == Meaning.NO).count() == 1;
        return singleYes || singleNo;
    }

}
