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

import teaselib.core.ResourceLoader;
import teaselib.core.ResourceLoaderTest;
import teaselib.core.util.ReflectionUtils;
import teaselib.core.util.WildcardPattern;

public class ResourceCacheTest {
    private static final Logger logger = LoggerFactory.getLogger(ResourceCacheTest.class);

    private static final String packagePath = ReflectionUtils.getPackagePath(ResourceLoaderTest.class);

    private ZipLocation locationOfFlatResourceArchive() throws IOException {
        return new ZipLocation(ResourceLoader.getProjectPath(getClass()).toPath()
                .resolve(packagePath + "UnpackResourcesTestData_flat.zip"), Paths.get(""));
    }

    private ZipLocation locationOfHierarcalResourceArchive() throws IOException {
        return new ZipLocation(ResourceLoader.getProjectPath(getClass()).toPath()
                .resolve(packagePath + "UnpackResourcesTestData_ResourceRootStructure.zip"), Paths.get(packagePath));
    }

    @Test
    public void testResourceZip() throws IOException {
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
    public void testResourceZipPath() throws IOException {
        ResourceCache resourceCache = new ResourceCache();
        resourceCache.add(locationOfHierarcalResourceArchive());

        List<String> resources = resourceCache.get(WildcardPattern.compile("*resource1.txt"));
        assertFalse(resources.isEmpty());
        assertEquals("/teaselib/core/UnpackResourcesTestData/resource1.txt", resources.get(0));

        try (InputStream is = resourceCache.get(resources.get(0));) {
            assertNotNull(is);
        }
    }

    @Test
    public void testResourceFolder() throws IOException {
        ResourceCache resourceCache = new ResourceCache();
        resourceCache.add(new FolderLocation(Paths.get("bin.test/"), Paths.get("teaselib/core/util/")));

        List<String> resources = resourceCache.get(WildcardPattern.compile("*bar.txt"));
        assertFalse(resources.isEmpty());
        assertEquals("/teaselib/core/util/bar.txt", resources.get(0));

        try (InputStream is = resourceCache.get(resources.get(0));) {
            assertNotNull(is);
        }
    }

    @Test
    public void testResourceFolderPath() throws IOException {
        ResourceCache resourceCache = new ResourceCache();
        resourceCache.add(new FolderLocation(Paths.get("bin.test/"), Paths.get("teaselib/core/util/")));

        List<String> resources = resourceCache.get(WildcardPattern.compile("/teaselib/core/util/*bar.txt"));
        assertFalse(resources.isEmpty());
        assertEquals("/teaselib/core/util/bar.txt", resources.get(0));

        try (InputStream is = resourceCache.get(resources.get(0));) {
            assertNotNull(is);
        }
    }

    @Test
    public void testResourceCache() throws IOException {
        ResourceCache resourceCache = new ResourceCache();
        resourceCache.add(locationOfFlatResourceArchive());
        resourceCache.add(new FolderLocation(Paths.get("bin.test/"), Paths.get("teaselib/core/util/")));

        print(resourceCache.get(WildcardPattern.compile("*.txt")));
    }

    private static void print(List<String> list) {
        for (String string : list) {
            logger.info(string);
        }
    }
}
