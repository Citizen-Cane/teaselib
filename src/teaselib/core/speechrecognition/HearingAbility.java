package teaselib.core.speechrecognition;

import java.util.EnumMap;
import java.util.Map;

import teaselib.core.ui.Intention;

public class HearingAbility {

    public static final HearingAbility Good = new HearingAbility( //
            Confidence.Low.probability, //
            Confidence.Normal.probability, //
            Confidence.High.probability);

    public static final HearingAbility Impaired = new HearingAbility( //
            Confidence.Low.probability, //
            Confidence.Low.slightlyRaisedProbability(), //
            Confidence.Normal.reducedProbability());

    private final Map<Intention, Float> map = new EnumMap<>(Intention.class);

    public HearingAbility(float chat, float confirm, float decide) {
        map.put(Intention.Chat, chat);
        map.put(Intention.Confirm, confirm);
        map.put(Intention.Decide, decide);
    }

    public float confidence(Intention intention) {
        return map.get(intention);
    }

}
