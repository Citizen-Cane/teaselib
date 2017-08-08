package teaselib.core.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

@SuppressWarnings("serial")
public class SortedProperties extends Properties {

    public SortedProperties() {
        super();
    }

    public SortedProperties(Properties defaults) {
        super(defaults);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized Enumeration<Object> keys() {
        Enumeration<Object> keysEnum = super.keys();
        @SuppressWarnings("rawtypes")
        Vector keyList = new Vector();
        while (keysEnum.hasMoreElements()) {
            keyList.add(keysEnum.nextElement());
        }
        Collections.sort(keyList);
        return keyList.elements();
    }
}
