/**
 * 
 */
package teaselib.core.devices.motiondetection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.util.TimeLine;
import teaselib.motiondetection.MotionDetector.Presence;

/**
 * The data set for the motion detection results
 *
 */
public abstract class MotionDetectionResultData
        implements MotionDetectionResult {
    static final int cornerSize = 32;

    protected boolean contourMotionDetected = false;
    protected boolean trackerMotionDetected = false;
    protected boolean motionDetected = false;
    protected final Map<Presence, Rect> presenceIndicators;
    protected final Map<Presence, Presence> negatedRegions;

    protected final TimeLine<Rect> motionRegionHistory = new TimeLine<Rect>();
    protected final TimeLine<Rect> presenceRegionHistory = new TimeLine<Rect>();
    protected final TimeLine<Integer> motionAreaHistory = new TimeLine<Integer>();
    protected final TimeLine<Set<Presence>> indicatorHistory = new TimeLine<Set<Presence>>();

    private final Rect all;

    protected MotionDetectionResultData(Size size) {
        presenceIndicators = buildPresenceIndicatorMap(size);
        negatedRegions = buildNegatedRegions();
        all = new Rect(0, 0, size.width(), size.height());
        clear();
    }

    @SuppressWarnings("resource")
    protected Map<Presence, Rect> buildPresenceIndicatorMap(Size s) {
        Map<Presence, Rect> map = new LinkedHashMap<Presence, Rect>();
        map.put(Presence.Present, new Rect(cornerSize, cornerSize,
                s.width() - 2 * cornerSize, s.height() - 2 * cornerSize));
        // Define a center rectangle half the width
        // and third the height of the capture size
        int cl = s.width() / 2 - s.width() / 4;
        int ct = s.height() / 2 - s.width() / 6;
        int cr = s.width() / 2 + s.width() / 4;
        int cb = s.height() / 2 + s.width() / 6;
        // Center
        map.put(Presence.Center, new Rect(cl, ct, cr - cl, cb - ct));
        map.put(Presence.CenterHorizontal, new Rect(0, ct, s.width(), cb - ct));
        map.put(Presence.CenterVertical, new Rect(cl, 0, cr - cl, s.height()));
        // Sides
        map.put(Presence.Left, new Rect(0, 0, cl, s.height()));
        map.put(Presence.Top, new Rect(0, 0, s.width(), ct));
        map.put(Presence.Right, new Rect(cr, 0, s.width() - cr, s.height()));
        map.put(Presence.Bottom, new Rect(0, cb, s.width(), s.height() - cb));
        // Borders
        map.put(Presence.LeftBorder, new Rect(0, 0, cornerSize, s.height()));
        map.put(Presence.RightBorder,
                new Rect(s.width() - cornerSize, 0, cornerSize, s.height()));
        map.put(Presence.TopBorder, new Rect(0, 0, s.width(), cornerSize));
        map.put(Presence.BottomBorder,
                new Rect(0, s.height() - cornerSize, s.width(), cornerSize));
        return map;
    }

    Map<Presence, Presence> buildNegatedRegions() {
        Map<Presence, Presence> negatedRegions = new LinkedHashMap<Presence, Presence>();
        negatedRegions.put(Presence.Left, Presence.NoLeft);
        negatedRegions.put(Presence.Right, Presence.NoRight);
        negatedRegions.put(Presence.Top, Presence.NoTop);
        negatedRegions.put(Presence.Bottom, Presence.NoBottom);
        negatedRegions.put(Presence.LeftBorder, Presence.NoLeftBorder);
        negatedRegions.put(Presence.RightBorder, Presence.NoRightBorder);
        negatedRegions.put(Presence.TopBorder, Presence.NoTopBorder);
        negatedRegions.put(Presence.BottomBorder, Presence.NoBottomBorder);
        return negatedRegions;
    }

    protected void clear() {
        motionRegionHistory.clear();
        presenceRegionHistory.clear();
        motionAreaHistory.clear();
        indicatorHistory.clear();
        // Set start values instead of testing for empty lists later on
        long timeStamp = 0;
        motionRegionHistory.add(all, timeStamp);
        presenceRegionHistory.add(all, timeStamp);
        motionAreaHistory.add(0, timeStamp);
        indicatorHistory.add(
                new HashSet<Presence>(Arrays.asList(Presence.Shake)),
                timeStamp);
    }
}
