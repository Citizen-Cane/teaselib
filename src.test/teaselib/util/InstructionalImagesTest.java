package teaselib.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.Test;

import teaselib.Resources;
import teaselib.core.InstructionalImages;
import teaselib.test.TestScript;

public class InstructionalImagesTest {

    private static final String TEST_PICTURE_SET = "/teaselib/util/Test Picture Set/";

    @Test
    public void testContainsAbsolute() throws IOException, InterruptedException {
        try (TestScript script = new TestScript(getClass())) {
            Resources resources = script.resources("Test Picture Set/*.jpg");
            assertTrue(resources.contains(TEST_PICTURE_SET + "p1.jpg"));
            assertEquals(12, resources.size());

            var images = new InstructionalImages(resources);
            assertTrue(images.contains(TEST_PICTURE_SET + "p1.jpg"));
            assertNotNull(images.annotated(TEST_PICTURE_SET + "p1.jpg"));
            assertTrue(images.contains("p1.jpg"));
            assertNotNull(images.annotated("p1.jpg"));
        }
    }

    @Test
    public void testRemoveIfAbsolute() throws IOException {
        try (TestScript script = new TestScript(getClass())) {
            Resources resources = script.resources("Test Picture Set/*.jpg");
            assertTrue(resources.contains(TEST_PICTURE_SET + "p1.jpg"));
            assertEquals(12, resources.size());

            var images = new InstructionalImages(resources);
            assertTrue(images.contains(TEST_PICTURE_SET + "p1.jpg"));
            assertTrue(images.contains("p1.jpg"));
            resources.removeIf(resource -> resource.equals(TEST_PICTURE_SET + "p1.jpg"));
            assertFalse(images.contains(TEST_PICTURE_SET + "p1.jpg"));
            assertFalse(images.contains("p1.jpg"));
        }
    }

    @Test
    public void testRemoveIfRelative() throws IOException {
        try (TestScript script = new TestScript(getClass())) {
            Resources resources = script.resources("Test Picture Set/*.jpg");
            assertTrue(resources.contains(TEST_PICTURE_SET + "p1.jpg"));
            assertEquals(12, resources.size());

            var images = new InstructionalImages(resources);
            assertTrue(images.contains(TEST_PICTURE_SET + "p1.jpg"));
            assertTrue(images.contains("p1.jpg"));
            resources.removeIf(resource -> resource.equals("p1.jpg"));
            assertFalse(images.contains("p1.jpg"));
            assertFalse(images.contains(TEST_PICTURE_SET + "p1.jpg"));
        }
    }

}
