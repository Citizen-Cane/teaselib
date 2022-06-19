package teaselib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import teaselib.ActorImages.Next;
import teaselib.Config;
import teaselib.Resources;
import teaselib.test.TestScript;

public class SceneBasedImagesTest {

    @Test
    public void testNoImages() throws IOException {
        try (TestScript testScript = new TestScript()) {
            testScript.teaseLib.config.set(Config.Debug.StopOnAssetNotFound, false);
            Resources resources = testScript.resources("");
            var images = new SceneBasedImages(resources);
            assertFalse(images.hasNext());
            for (var next : Next.values()) {
                images.advance(next);
            }
        }
    }

    @Test
    public void testStripteaseSets() throws IOException {
        try (TestScript testScript = new TestScript()) {
            Resources resources = testScript.resources(PictureSetAssetsTest.TEST_PICTURE_SET_FOLDER, Resources.Images);
            assertEquals(12, resources.size());
            var images = new SceneBasedImages(resources, testScript.randomNumbers(0, 1, 2, 3, 4));
            assertTrue(images.hasNext());

            //
            // Set 1

            List<String> take1 = collect(images);
            assertEquals(2, take1.size());
            assertTrue(ascending(take1), "Ascending");

            images.advance(Next.Take);
            List<String> take2 = collect(images);
            assertEquals(2, take2.size());
            assertTrue(ascending(take2));

            images.advance(Next.Take);
            List<String> take3 = collect(images);
            assertEquals(2, take3.size());
            assertTrue(ascending(take3));

            images.advance(Next.Take);
            List<String> take4 = collect(images);
            assertEquals(2, take4.size());
            assertTrue(ascending(take4));

            //
            // Set 2 backwards

            images.advance(Next.Take);
            List<String> take5 = collect(images);
            assertEquals(2, take5.size());
            assertFalse(ascending(take5));

            //
            // Set 3 forward

            images.advance(Next.Take);
            List<String> take6 = collect(images);
            assertEquals(2, take6.size());
            assertTrue(ascending(take6));
        }
    }

    boolean ascending(List<String> strings) {
        return strings.get(0).compareTo(strings.get(1)) < 0;
    }

    List<String> collect(SceneBasedImages images) {
        var resources = new ArrayList<String>();
        while (images.hasNext()) {
            resources.add(images.next());
        }
        return resources;
    }

    List<String> collect(SceneBasedImages images, int n) {
        var resources = new ArrayList<String>();
        for (int i = 0; i < n; i++) {
            resources.add(images.next());
        }
        return resources;
    }
}
