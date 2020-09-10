package teaselib.core.ai.perception;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import teaselib.Answer;
import teaselib.Answer.Meaning;
import teaselib.core.ai.perception.HumanPose.HeadGestures;
import teaselib.core.ui.AbstractInputMethod;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Result;

public class HeadGesturesV2InputMethod extends AbstractInputMethod {

    public final HumanPoseDeviceInteraction.Reaction trackGaze = new HumanPoseDeviceInteraction.Reaction(
            HumanPose.Interest.HeadGestures, pose -> {
                Optional<HeadGestures> aspect = pose.aspect(HeadGestures.class);
                if (aspect.isPresent()) {
                    //
                } else {
                    //
                }
            });

    public HeadGesturesV2InputMethod(ExecutorService executor) {
        super(executor);
    }

    @Override
    public Setup getSetup(Choices choices) {
        return new Setup() {
            @Override
            public void apply() {
                // nothing to do
            }
        };
    }

    @Override
    protected Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        Thread.sleep(Long.MAX_VALUE);
        return Result.DISMISSED;
    }

    @Override
    protected void handleDismiss(Prompt prompt) throws InterruptedException {
        // nothing to do
    }

    public static boolean distinctGestures(List<Answer> answers) {
        boolean singleYes = answers.stream().filter(answer -> answer.meaning == Meaning.YES).count() == 1;
        boolean singleNo = answers.stream().filter(answer -> answer.meaning == Meaning.NO).count() == 1;
        return singleYes || singleNo;
    }

}
