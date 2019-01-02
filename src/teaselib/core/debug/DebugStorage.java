package teaselib.core.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;

import teaselib.core.util.QualifiedName;

public final class DebugStorage extends HashMap<QualifiedName, String> {
    private static final long serialVersionUID = 1L;

    public void printTo(Logger logger) {
        List<Entry<QualifiedName, String>> entryList = new ArrayList<>(entrySet());
        Collections.sort(entryList, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
        if (logger.isInfoEnabled()) {
            logger.info("Storage: {} entries", size());
            for (Entry<QualifiedName, String> entry : entryList) {
                logger.info("{}={}", entry.getKey(), entry.getValue());
            }
        }
    }

}