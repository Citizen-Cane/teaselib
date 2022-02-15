package teaselib.core;

import java.io.IOException;
import java.nio.file.Paths;

import teaselib.ActorImages;
import teaselib.CachedImages;
import teaselib.Resources;
import teaselib.core.Host.Location;
import teaselib.util.AnnotatedImage;

public abstract class AbstractImages extends CachedImages implements ActorImages {

    public final PoseCache poseCache;

    protected AbstractImages(Resources resources) {
        super(resources);
        this.poseCache = new PoseCache(
                Paths.get(resources.teaseLib.host.getLocation(Location.User).getAbsolutePath(), "Pose cache"),
                resources.teaseLib, resources.loader);
    }

    @Override
    protected AnnotatedImage annotatedImage(String resource) throws IOException {
        byte[] image = resources.getBytes(resource);
        return poseCache.annotatedImage(resource, image);
    }

    @Override
    public boolean hasNext() {
        return !resources.isEmpty();
    }

}
