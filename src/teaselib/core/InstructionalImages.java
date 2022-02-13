package teaselib.core;

import java.io.IOException;

import teaselib.CachedImages;
import teaselib.ImageCollection;
import teaselib.Resources;
import teaselib.util.AnnotatedImage;

public class InstructionalImages extends CachedImages implements ImageCollection {

    public InstructionalImages(Resources resources) {
        super(resources);
    }

    @Override
    protected AnnotatedImage annotatedImage(String resource) throws IOException {
        byte[] image = resources.getBytes(resource);
        return new AnnotatedImage(resource, image);
    }

}
