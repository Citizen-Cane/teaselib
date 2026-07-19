package teaselib.core.configuration;

import java.io.IOException;
import java.util.List;

public interface Setup {
    String ITEM_DEFAULT_STORE_FILENAME = "items.xml";

    String PRONUNCIATION_DIRECTORY = "pronunciation";

    // TODO Results in duplicated code -> define ConfigFile object, then just add in test/production setup 
    String IDENTITY_PROPERTIES = "identities.properties";
    List<String> IDENTITY_PROPERTIES_NAMESPACES = List.of("user.masculine", "user.feminine");

    Configuration applyTo(Configuration config) throws IOException;
}