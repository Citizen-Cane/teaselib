package teaselib.core.ai.perception;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import teaselib.Answer;
import teaselib.Answer.Meaning;
import teaselib.core.ai.perception.HumanPose.HeadGestures;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ui.AbstractInputMethod;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Result;

public class HeadGesturesV2InputMethod extends AbstractInputMethod {
    private final HumanPoseDeviceInteraction humanPoseInteraction;

    public final HumanPoseDeviceInteraction.EventListener trackProximity = new HumanPoseDeviceInteraction.EventListener(
            HumanPose.Interest.Proximity) {

        @Override
        public void run(PoseEstimationEventArgs eventArgs) throws Exception {
            // TODO gaze tracking doesn't end after dismissing input method
            if (!humanPoseInteraction.containsEventListener(eventArgs.actor, trackProximity)) {
                if (humanPoseInteraction.containsEventListener(eventArgs.actor, trackGaze)) {
                    humanPoseInteraction.removeEventListener(eventArgs.actor, trackGaze);
                }
            } else {
                Optional<Proximity> aspect = eventArgs.pose.aspect(Proximity.class);
                boolean face2face;
                if (aspect.isPresent()) {
                    Proximity proximity = aspect.get();
                    face2face = proximity == Proximity.FACE2FACE;
                } else {
                    face2face = false;
                }

                if (face2face && !humanPoseInteraction.containsEventListener(eventArgs.actor, trackGaze)) {
                    humanPoseInteraction.addEventListener(eventArgs.actor, trackGaze);
                } else if (!face2face && humanPoseInteraction.containsEventListener(eventArgs.actor, trackGaze)) {
                    humanPoseInteraction.removeEventListener(eventArgs.actor, trackGaze);
                }
            }
        }
    };

    public final HumanPoseDeviceInteraction.EventListener trackGaze = new HumanPoseDeviceInteraction.EventListener(
            HumanPose.Interest.HeadGestures) {

        @Override
        public void run(PoseEstimationEventArgs eventArgs) throws Exception {
            Optional<HeadGestures> aspect = eventArgs.pose.aspect(HeadGestures.class);
            if (aspect.isPresent()) {
                //
            } else {
                //
            }
        }
    };

    public HeadGesturesV2InputMethod(HumanPoseDeviceInteraction humanPoseInteraction, ExecutorService executor) {
        super(executor);
        this.humanPoseInteraction = humanPoseInteraction;
    }

    @Override
    public Setup getSetup(Choices choices) {
        return () -> { // nothing to do
        };
    }

    @Override
    protected Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        humanPoseInteraction.addEventListener(prompt.script.actor, trackProximity);
        Thread.sleep(Long.MAX_VALUE);
        return Result.DISMISSED;
    }

    @Override
    protected void handleDismiss(Prompt prompt) throws InterruptedException {
        if (humanPoseInteraction.containsEventListener(prompt.script.actor, trackProximity)) {
            humanPoseInteraction.removeEventListener(prompt.script.actor, trackProximity);
        }
        // TODO gaze tracking doesn't end after dismissing input method
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
