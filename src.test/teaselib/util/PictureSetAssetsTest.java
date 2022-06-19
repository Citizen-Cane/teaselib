package teaselib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import teaselib.Resources;
import teaselib.test.TestScript;
import teaselib.util.PictureSetAssets.Pose;
import teaselib.util.PictureSetAssets.Scene;
import teaselib.util.PictureSetAssets.Take;

public class PictureSetAssetsTest {

    static final String TEST_PICTURE_SET_FOLDER = "/teaselib/util/Test Picture Set/";

    private static String relative(String key) {
        if (key.length() < TEST_PICTURE_SET_FOLDER.length()) {
            return "";
        } else {
            return key.substring(TEST_PICTURE_SET_FOLDER.length());
        }
    }

    @Test
    public void testPictureSetAssets() throws IOException {
        try (TestScript script = new TestScript(getClass())) {
            Resources pics = script.resources("Test Picture Set/*.*");
            assertEquals(12, pics.size());
            var pictureSet = new PictureSetAssets(pics);
            assertEquals(12, pictureSet.size());
            var sets = new ArrayList<>(pictureSet.values());
            assertEquals(3, sets.size(), "2 defined sets plus top-level picture set");

            var set1 = sets.get(0);
            assertEquals("Set 1 test", relative(set1.key));
            assertEquals(2, set1.assets.size(), "1 defined scene plus 1 top-level scene");

            Scene scene1 = set1.assets.get(0);
            assertEquals(2, scene1.assets.size(), "1 defined pose plus 1 top-level pose");
            assertEquals("Set 1 test/Scene 1 test", relative(scene1.key), "defined scene");
            Pose pose1 = scene1.assets.get(0);
            assertEquals(2, pose1.assets.size(), "One defined and one top leveel pose");
            assertEquals("Set 1 test/Scene 1 test/Pose 1 test", relative(pose1.key), "Defined pose");
            Take take1 = pose1.assets.get(0);
            assertEquals(2, take1.assets.size());
            assertEquals("Set 1 test/Scene 1 test/Pose 1 test/Take 1 test", relative(take1.key));
            assertEquals("Set 1 test/Scene 1 test/Pose 1 test/Take 1 test/hand2.jpg", relative(take1.assets.get(0)));
            assertEquals("Set 1 test/Scene 1 test/Pose 1 test/Take 1 test/hand3.jpg", relative(take1.assets.get(1)));
            Take take2 = pose1.assets.get(1);
            assertEquals(2, take1.assets.size());
            assertEquals("Set 1 test/Scene 1 test/Pose 1 test", relative(take2.key), "Top level take");
            assertEquals("Set 1 test/Scene 1 test/Pose 1 test/golf.jpg", relative(take2.assets.get(0)));
            assertEquals("Set 1 test/Scene 1 test/Pose 1 test/golf2.jpg", relative(take2.assets.get(1)));

            Pose pose2 = scene1.assets.get(1);
            assertEquals(1, pose2.assets.size(), "1 top-level pose");
            assertEquals("Set 1 test/Scene 1 test", relative(pose2.key), "Top level pose");
            Take take3 = pose2.assets.get(0);
            assertEquals(2, take3.assets.size());
            assertEquals("Set 1 test/Scene 1 test", relative(take3.key));
            assertEquals("Set 1 test/Scene 1 test/handsup1.jpg", relative(take3.assets.get(0)));
            assertEquals("Set 1 test/Scene 1 test/handsup2.jpg", relative(take3.assets.get(1)));

            Scene scene2 = set1.assets.get(1);
            assertEquals("Set 1 test", relative(scene2.key), "top level scene");
            Pose pose3 = scene2.assets.get(0);
            assertEquals(1, pose3.assets.size());
            assertEquals("Set 1 test", relative(pose3.key), "Top level scene");
            Take take4 = pose3.assets.get(0);
            assertEquals(2, take4.assets.size());
            assertEquals("Set 1 test", relative(take4.key));
            assertEquals("Set 1 test/hand1.jpg", relative(take4.assets.get(0)));
            assertEquals("Set 1 test/hand2.jpg", relative(take4.assets.get(1)));

            var set2 = sets.get(1);
            assertEquals("Set 2 test", relative(set2.key));
            assertEquals(1, set2.assets.size(), "1 top level scene");
            Scene scene3 = set2.assets.get(0);
            assertEquals("Set 2 test", relative(scene3.key), "top level scene");

            Pose pose4 = scene3.assets.get(0);
            assertEquals(1, pose4.assets.size());
            assertEquals("Set 2 test", relative(pose4.key), "Top level scene");
            Take take5 = pose4.assets.get(0);
            assertEquals(2, take5.assets.size());
            assertEquals("Set 2 test", relative(take5.key));
            assertEquals("Set 2 test/p2.jpg", relative(take5.assets.get(0)));
            assertEquals("Set 2 test/p3.jpg", relative(take5.assets.get(1)));

            var set3 = sets.get(2);
            assertEquals("", relative(set3.key));
            assertEquals(1, set3.assets.size(), "1 top-level scene");
            Scene scene4 = set3.assets.get(0);
            assertEquals("", relative(scene4.key), "top level scene");

            Pose pose5 = scene4.assets.get(0);
            assertEquals(1, pose5.assets.size());
            assertEquals("", relative(pose5.key), "Top level scene");
            Take take6 = pose5.assets.get(0);
            assertEquals(2, take6.assets.size());
            assertEquals("", relative(take6.key));
            assertEquals("p1.jpg", relative(take6.assets.get(0)));
            assertEquals("p1b.jpg", relative(take6.assets.get(1)));
        }
    }

}
