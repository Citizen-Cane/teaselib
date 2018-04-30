package teaselib.core.util.resource;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

interface ResourceLocation extends Closeable {
    Path root();

    Path project();

    List<String> resources() throws IOException;

    InputStream get(String resource) throws IOException;

}