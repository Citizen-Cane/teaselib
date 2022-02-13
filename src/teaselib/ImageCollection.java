package teaselib;

import java.io.IOException;
import java.util.NoSuchElementException;

import teaselib.util.AnnotatedImage;

public interface ImageCollection extends Images {

    public static final ImageCollection None = new ImageCollection() {

        @Override
        public boolean contains(String resource) {
            return false;
        }

        @Override
        public void fetch(String resource) {
            throw new NoSuchElementException(resource);
        }

        @Override
        public AnnotatedImage annotated(String resource) throws IOException, InterruptedException {
            throw new NoSuchElementException(resource);
        }

    };

}
