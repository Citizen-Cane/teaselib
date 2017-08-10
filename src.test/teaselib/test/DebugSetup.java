package teaselib.test;

import java.io.IOException;

import teaselib.Config;
import teaselib.core.Configuration;
import teaselib.core.Configuration.Setup;
import teaselib.core.util.QualifiedItem;

public final class DebugSetup implements Setup {
    @Override
    public void applyTo(Configuration config) throws IOException {
        config.set(QualifiedItem.of(Config.Debug.IgnoreMissingResources), Boolean.toString(false));
        config.set(QualifiedItem.of(Config.Debug.LogDetails), Boolean.toString(false));
    }
}