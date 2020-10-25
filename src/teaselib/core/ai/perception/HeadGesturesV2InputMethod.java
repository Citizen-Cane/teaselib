package teaselib.core.ai.perception;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(HeadGesturesV2InputMethod.class);

    private final HumanPoseDeviceInteraction humanPoseInteraction;

    public HeadGesturesV2InputMethod(HumanPoseDeviceInteraction humanPoseInteraction, ExecutorService executor) {
        super(executor);
        this.humanPoseInteraction = humanPoseInteraction;
    }

    @Override
    public Setup getSetup(Choices choices) {
        return () -> { //
        };
    }

    public final HumanPoseDeviceInteraction.EventListener trackProximity = new HumanPoseDeviceInteraction.EventListener(
            HumanPose.Interest.Proximity) {
        @Override
        public void run(PoseEstimationEventArgs eventArgs) throws Exception {
            activePrompt.updateAndGet(prompt -> {
                if (prompt != null) {
                    Optional<Proximity> proximity = eventArgs.pose.aspect(Proximity.class);
                    boolean face2face;
                    if (proximity.isPresent()) {
                        if (proximity.get() == Proximity.FACE2FACE) {
                            Optional<Gaze> gaze = eventArgs.pose.estimation.gaze;
                            if (gaze.isPresent() && gaze.get().isFace2Face()) {
                                if (!humanPoseInteraction.containsEventListener(eventArgs.actor, trackGaze)) {
                                    clearTimeline(gaze.get());
                                    humanPoseInteraction.addEventListener(eventArgs.actor, trackGaze);
                                }
                                face2face = true;
                            } else {
                                face2face = false;
                            }
                        } else {
                            face2face = false;
                        }
                    } else {
                        face2face = false;
                    }

                    if (!face2face && humanPoseInteraction.containsEventListener(eventArgs.actor, trackGaze)) {
                        humanPoseInteraction.removeEventListener(eventArgs.actor, trackGaze);
                    }
                } else if (humanPoseInteraction.containsEventListener(eventArgs.actor, trackGaze)) {
                    humanPoseInteraction.removeEventListener(eventArgs.actor, trackGaze);
                }
                return prompt;
            });
        }

        private void clearTimeline(Gaze gaze) {
            detectedGesture.set(Gesture.None);
            movement.clear();
            movement.add(new DirectionValue(Direction.None, gaze.nod, 0.0f), System.currentTimeMillis(), MILLISECONDS);
        }
    };

    enum Gesture {
        Nod,
        Shake,
        None
    }

    interface Direction {
        Direction None = new Direction() {

            @Override
            public String toString() {
                return "None";
            }
        };

        enum Nod implements Direction {
            Up,
            Down,
        }

        enum Shake implements Direction {
            Left,
            Right
        }

        enum Rejected implements Direction {
            TooLong,
            TooSmall
        }

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

    private class DirectionValue {
        final Direction direction;
        final float x;
        final float dx;

        private DirectionValue(Direction direction, float x, float dx) {
            this.direction = direction;
            this.x = x;
            this.dx = dx;
        }

        @Override
        public String toString() {
            return direction + "=" + x + "(" + dx + ")";
        }

    }

    private final Lock gestureDetectionLock = new ReentrantLock();
    private final Condition gestureDetected = gestureDetectionLock.newCondition();
    private final AtomicReference<Gesture> detectedGesture = new AtomicReference<>(Gesture.None);
    private final DenseTimeLine<DirectionValue> movement = new DenseTimeLine<>(12, 3, TimeUnit.SECONDS);

    public final HumanPoseDeviceInteraction.EventListener trackGaze = new HumanPoseDeviceInteraction.EventListener(
            HumanPose.Interest.HeadGestures) {

        private final List<Direction> nodDown = Arrays.asList(Direction.Nod.Down, Direction.Nod.Up, Direction.Nod.Down,
                Direction.Nod.Up, Direction.Nod.Down, Direction.Nod.Up);
        private final List<Direction> nodUp = Arrays.asList(Direction.Nod.Up, Direction.Nod.Down, Direction.Nod.Up,
                Direction.Nod.Down, Direction.Nod.Up, Direction.Nod.Down);
        private final List<Direction> shakeRight = Arrays.asList(Direction.Shake.Right, Direction.Shake.Left,
                Direction.Shake.Right, Direction.Shake.Left, Direction.Shake.Right, Direction.Shake.Left);
        private final List<Direction> shakeLeft = Arrays.asList(Direction.Shake.Left, Direction.Shake.Right,
                Direction.Shake.Left, Direction.Shake.Right, Direction.Shake.Left, Direction.Shake.Right);

        private float lastNod = 0.0f;

        @Override
        public void run(PoseEstimationEventArgs eventArgs) throws Exception {
            if (eventArgs.pose.is(HeadGestures.Gaze)) {
                Gaze gaze = eventArgs.pose.estimation.gaze.orElseThrow();
                estimateGesture(eventArgs, gaze);
            }
        }

        private void estimateGesture(PoseEstimationEventArgs eventArgs, Gaze gaze) {
            float x = (float) (gaze.nod * 180.0f / Math.PI);
            if (!Float.isNaN(x)) {
                float dx = x - lastNod;
                if (dx != 0) {
                    Direction direction;
                    if (dx > 0)
                        direction = Direction.Nod.Up;
                    else /* if (dxNod < 0) */
                        direction = Direction.Nod.Down;

                    DirectionValue lastDirection = movement.last();
                    if (direction == lastDirection.direction) {
                        movement.removeLast();
                        movement.add(new DirectionValue(direction, x, lastDirection.dx + dx), eventArgs.timestamp,
                                MILLISECONDS);
                    } else {
                        movement.add(new DirectionValue(direction, x, dx), eventArgs.timestamp, MILLISECONDS);
                    }
                    lastNod = x;

                    // TODO filter insignificant values and join adjacent entries
                    DenseTimeLine<DirectionValue> tail = movement.last(nodDown.size());
                    logger.info("movement = {}", tail);
                    List<Direction> gesture = tail.last(3, TimeUnit.SECONDS).stream().map(this::direction)
                            .collect(toList());
                    logger.info("Gesture pattern = {}", gesture);
                    if (gesture.equals(nodDown) || gesture.equals(nodUp)) {
                        signal(Gesture.Nod);
                    } else if (gesture.equals(shakeRight) || gesture.equals(shakeLeft)) {
                        signal(Gesture.Shake);
                    }
                }
            }
        }

        private Direction direction(DenseTimeLine.TimeStamp<DirectionValue> element) {
            DirectionValue directionValue = element.element;
            if (Math.abs(directionValue.dx) < 15.0) {
                return Direction.Rejected.TooSmall;
            } else if (element.durationMillis > 750) {
                return Direction.Rejected.TooLong;
            } else {
                return directionValue.direction;
            }
        }

        private void signal(Gesture gesture) {
            gestureDetectionLock.lock();
            try {
                detectedGesture.set(gesture);
                gestureDetected.signalAll();
            } finally {
                gestureDetectionLock.unlock();
            }
        }

    };

    @Override
    protected Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        humanPoseInteraction.addEventListener(prompt.script.actor, trackProximity);
        while (!Thread.currentThread().isInterrupted()) {
            gestureDetectionLock.lockInterruptibly();
            try {
                gestureDetected.await();
                Meaning meaning = meaning(detectedGesture.get());
                Optional<Choice> choice = prompt.choices.stream().filter(c -> c.answer.meaning == meaning).findFirst();
                if (choice.isPresent()) {
                    return new Result(prompt.choices.indexOf(choice.get()));
                }
            } finally {
                gestureDetectionLock.unlock();
            }
        }
        return Result.UNDEFINED;
    }

    @Override
    protected void handleDismiss(Prompt prompt) throws InterruptedException {
        if (humanPoseInteraction.containsEventListener(prompt.script.actor, trackProximity)) {
            humanPoseInteraction.removeEventListener(prompt.script.actor, trackProximity);
        }
        if (humanPoseInteraction.containsEventListener(prompt.script.actor, trackGaze)) {
            humanPoseInteraction.removeEventListener(prompt.script.actor, trackGaze);
        }
    }

    public static boolean distinctGestures(List<Answer> answers) {
        boolean singleYes = answers.stream().filter(answer -> answer.meaning == Meaning.YES).count() == 1;
        boolean singleNo = answers.stream().filter(answer -> answer.meaning == Meaning.NO).count() == 1;
        return singleYes || singleNo;
    }

}
