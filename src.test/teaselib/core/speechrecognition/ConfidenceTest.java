package teaselib.core.speechrecognition;

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static teaselib.core.speechrecognition.Confidence.Definite;
import static teaselib.core.speechrecognition.Confidence.High;
import static teaselib.core.speechrecognition.Confidence.Low;
import static teaselib.core.speechrecognition.Confidence.Normal;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfidenceTest {
    private static final Logger logger = LoggerFactory.getLogger(Confidence.class);

    @BeforeClass
    public static void showValues() {
        Confidence[] confidenceValues = { Low, Normal, High, Definite };
        String confidenceDefaults = Arrays.stream(confidenceValues).map(c -> "\t" + c.name() + "=" + c.probability)
                .collect(joining("\t"));
        logger.info("Weighted confidence values: {}", confidenceDefaults);
        for (Float k : Arrays.asList(1.0f, 1.25f, 1.5f, 1.75f, 2.0f)) {
            logger.info("\tk={}", k);
            for (int i = 1; i <= 5; i++) {
                int n = i;
                String values = Arrays.stream(confidenceValues).map(c -> weighted(c, n, k)).collect(joining("\t"));
                logger.info("\t\t{}", values);
            }
        }
    }

    private static String weighted(Confidence confidence, int n, float k) {
        return "f(" + confidence + "," + n + ", " + String.format("%1.2f", k) + ")="
                + String.format("%1.3f", confidence.weighted(n, k));
    }

    @Test
    public void testWeightedConfidenceHigh() {
        Confidence high = Confidence.High;
        float k = 2.0f;
        assertEquals(0.825f, high.weighted(1, k), 0.001);
        assertEquals(0.775f, high.weighted(2, k), 0.001);
        assertEquals(0.750f, high.weighted(3, k), 0.001);
        assertEquals(0.735f, high.weighted(4, k), 0.001);
        assertEquals(0.725f, high.weighted(5, k), 0.001);

        assertEquals(0.702f, high.weighted(10, k), 0.001);

        assertEquals(0.677f, high.weighted(100, k), 0.001);

        assertEquals(high.slightlyRaisedProbability(), high.weighted(1, k), 0.0);
        assertEquals(high.probability, high.weighted(3), 0.0);
        assertEquals(high.slightlyReducedProbability(), high.weighted(Integer.MAX_VALUE, k), 0.0);
    }

}
