package teaselib.core.util.resource;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ResourceLoader;
import teaselib.core.ResourceUnpackToFolderTest;
import teaselib.core.util.ReflectionUtils;
import teaselib.core.util.WildcardPattern;

public class ResourceCacheTest {
    private static final Logger logger = LoggerFactory.getLogger(ResourceCacheTest.class);

    private ZipLocation locationOfFlatResourceArchive() throws IOException {
        return new ZipLocation(Paths.get(ResourceLoader.getProjectPath(getClass()) + "/"
                + ReflectionUtils.getPackagePath(ResourceUnpackToFolderTest.class)
                + "/UnpackResourcesTestData_flat.zip"));
    }

    @Test
    public void testResourceZipCache() throws IOException {
        ResourceCache resourceCache = new ResourceCache();
        resourceCache.add(locationOfFlatResourceArchive());

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
        resourceCache.add(locationOfFlatResourceArchive());
        resourceCache.add(new FolderLocation(Paths.get("bin.test/teaselib/core/util/")));

        print(resourceCache.get(WildcardPattern.compile("*.txt")));
    }

    private static void print(List<String> list) {
        for (String string : list) {
            logger.info(string);
        }
    }
}
