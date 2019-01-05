package teaselib.core.configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface Setup {
    static final String ITEM_DEFAULT_STORE_FILENAME = "items.xml";

    static final String PRONUNCIATION_DIRECTORY = "pronunciation";

    // TODO Results in duplicated code -> define ConfigFile object, then just add in test/production setup 
    static final String IDENTITY_PROPERTIES = "identities.properties";
    static final List<String> IDENTITY_PROPERTIES_NAMESPACES = Collections
            .unmodifiableList(Arrays.asList("user.masculine", "user.feminine"));

    Configuration applyTo(Configuration config) throws IOException;
}