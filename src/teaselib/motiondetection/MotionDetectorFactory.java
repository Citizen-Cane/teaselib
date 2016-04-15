package teaselib.motiondetection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import teaselib.motiondetection.javacv.MotionDetectorJavaCV;

public class MotionDetectorFactory {

    private static Map<String, MotionDetector> runningMotionDetectors = new HashMap<>();

    public static MotionDetector getDefaultMotionDetector() {
        String defaultId = getLast(getDevices());
        MotionDetector motionDetector = getMotionDetector(defaultId);
        return motionDetector;
    }

    public static MotionDetector getMotionDetector(String id) {
        if (runningMotionDetectors.containsKey(id)) {
            return runningMotionDetectors.get(id);
        } else {
            // TODO refect to create motion detector based on id
            // return getDefaultMotionDetector();
            MotionDetectorJavaCV motionDetector = new MotionDetectorJavaCV(id);
            runningMotionDetectors.put(id, motionDetector);
            return motionDetector;
        }
    }

    public static Set<String> getDevices() {
        Set<String> devices = new LinkedHashSet<>();
        devices.addAll(MotionDetectorSarxos.getDevices());
        devices.addAll(MotionDetectorJavaCV.getDevices());
        return devices;
    }

    private static String getLast(Collection<String> collection) {
        String s = null;
        Iterator<String> it = collection.iterator();
        while (it.hasNext()) {
            s = it.next();
        }
        return s;
    }

    public static String makeId(Class<?> clazz, String name) {
        return clazz.getName() + ":" + name;
    }

    public static String getName(String id) {
        return id.substring(id.indexOf(":") + 1);
    }
}
