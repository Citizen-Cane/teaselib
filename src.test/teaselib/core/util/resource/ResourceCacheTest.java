package teaselib.core.util.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.util.WildcardPattern;

public class ResourceCacheTest {
    private static final Logger logger = LoggerFactory.getLogger(ResourceCacheTest.class);

    @Test
    public void testResourceZipCache() throws IOException {
        ResourceCache resourceCache = new ResourceCache();
        resourceCache.add(new ZipLocation(Paths.get("bin.test/teaselib/core/UnpackResourcesTestData_flat.zip")));

        List<String> resources = resourceCache.get(WildcardPattern.compile("*resource1.txt"));
        assertFalse(resources.isEmpty());
        assertEquals("/UnpackResourcesTestData/resource1.txt", resources.get(0));

        try (InputStream is = resourceCache.get(resources.get(0));) {
            assertNotNull(is);
        }
    }

    @Test
    public void testResourceFolderCache() throws IOException {
        ResourceCache resourceCache = new ResourceCache();
        resourceCache.add(new FolderLocation(Paths.get("bin.test/teaselib/core/util/")));

        List<String> resources = resourceCache.get(WildcardPattern.compile("*bar.txt"));
        assertFalse(resources.isEmpty());
        assertEquals("/bar.txt", resources.get(0));

        try (InputStream is = resourceCache.get(resources.get(0));) {
            assertNotNull(is);
        }
    }

    @Test
    public void testResourceCache() throws IOException {
        ResourceCache resourceCache = new ResourceCache();
        resourceCache.add(new ZipLocation(Paths.get("bin.test/teaselib/core/UnpackResourcesTestData_flat.zip")));
        resourceCache.add(new FolderLocation(Paths.get("bin.test/teaselib/core/util/")));

        print(resourceCache.get(WildcardPattern.compile("*.txt")));
    }

    private void print(List<String> list) {
        for (String string : list) {
            logger.info(string);
        }
    }
}
