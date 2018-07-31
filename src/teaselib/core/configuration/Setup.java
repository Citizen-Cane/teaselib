package teaselib.core.configuration;

import java.io.IOException;

public interface Setup {
    Configuration applyTo(Configuration config) throws IOException;
}