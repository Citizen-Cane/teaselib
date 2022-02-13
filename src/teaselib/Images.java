package teaselib;

import java.io.IOException;

import teaselib.util.AnnotatedImage;

public interface Images {

    public boolean contains(String resource);

    public void fetch(String resource);

    AnnotatedImage annotated(String resource) throws IOException, InterruptedException;

}
