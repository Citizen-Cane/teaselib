/**
 * 
 */
package teaselib.core.devices.motiondetection;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.util.TimeLine;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.motiondetection.ViewPoint;

/**
 * The data set for the motion detection results
 *
 */
public abstract class MotionDetectionResultData implements MotionDetectionResult {
    // TODO size of one or two structuring elements
    static final int CornerSize = 32;
    protected static final Map<ViewPoint, Presence> viewPoint2PresenceRegion = getViewPointRegions();

    private static Map<ViewPoint, Presence> getViewPointRegions() {
        Map<ViewPoint, Presence> m = new EnumMap<>(ViewPoint.class);
        // Include border regions that are prone to body clipping
        // in the as presence region
        m.put(ViewPoint.EyeLevel, Presence.PresenceExtendedVertically);
        m.put(ViewPoint.EyeLevelFar, Presence.Present);
        m.put(ViewPoint.HighAngle, Presence.Present);
        m.put(ViewPoint.LowAngle, Presence.PresenceExtendedVertically);
        return Collections.unmodifiableMap(m);
    }

    protected final Map<Presence, Rect> presenceIndicators;
    protected final Map<Presence, Presence> negatedRegions;

    protected boolean contourMotionDetected = false;
    protected boolean trackerMotionDetected = false;
    protected boolean motionDetected = false;

    protected final TimeLine<Rect> motionRegionHistory = new TimeLine<>();
    protected final TimeLine<Rect> presenceRegionHistory = new TimeLine<>();
    protected final TimeLine<Integer> motionAreaHistory = new TimeLine<>();
    protected final TimeLine<Set<Presence>> indicatorHistory = new TimeLine<>();

    protected ViewPoint viewPoint = ViewPoint.EyeLevel;

    private final Rect all;

    protected MotionDetectionResultData(Size size) {
        presenceIndicators = buildPresenceIndicatorMap(size);
        negatedRegions = buildNegatedRegions();
        all = presenceIndicators.get(Presence.Center);
        clear();
    }

    protected Map<Presence, Rect> buildPresenceIndicatorMap(Size s) {
        Map<Presence, Rect> map = new EnumMap<>(Presence.class);
        map.put(Presence.Present,
                new Rect(CornerSize, CornerSize, s.width() - 2 * CornerSize, s.height() - 2 * CornerSize));
        // The extended region cannot touch the screen border,
        // since the presence region cannot touch the border because
        // findContours ignores a one pixel border around the input mat.
        map.put(Presence.PresenceExtendedVertically,
                new Rect(CornerSize, 1, s.width() - 2 * CornerSize, s.height() - 2));
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
        map.put(Presence.LeftBorder, new Rect(0, 0, CornerSize, s.height()));
        map.put(Presence.RightBorder, new Rect(s.width() - CornerSize, 0, CornerSize, s.height()));
        map.put(Presence.TopBorder, new Rect(0, 0, s.width(), CornerSize));
        map.put(Presence.BottomBorder, new Rect(0, s.height() - CornerSize, s.width(), CornerSize));
        return Collections.unmodifiableMap(map);
    }

    private static Map<Presence, Presence> buildNegatedRegions() {
        Map<Presence, Presence> negatedRegions = new EnumMap<>(Presence.class);
        negatedRegions.put(Presence.Left, Presence.NoLeft);
        negatedRegions.put(Presence.Right, Presence.NoRight);
        negatedRegions.put(Presence.Top, Presence.NoTop);
        negatedRegions.put(Presence.Bottom, Presence.NoBottom);
        negatedRegions.put(Presence.LeftBorder, Presence.NoLeftBorder);
        negatedRegions.put(Presence.RightBorder, Presence.NoRightBorder);
        negatedRegions.put(Presence.TopBorder, Presence.NoTopBorder);
        negatedRegions.put(Presence.BottomBorder, Presence.NoBottomBorder);
        return Collections.unmodifiableMap(negatedRegions);
    }

    protected void clear() {
        motionRegionHistory.clear();
        presenceRegionHistory.clear();
        motionAreaHistory.clear();
        indicatorHistory.clear();
        setStartValuesInsteadOfTestingForEmptyListsLaterOn(0L);
    }

    private void setStartValuesInsteadOfTestingForEmptyListsLaterOn(long timeStamp) {
        motionRegionHistory.add(all, timeStamp);
        presenceRegionHistory.add(all, timeStamp);
        motionAreaHistory.add(0, timeStamp);
        indicatorHistory.add(new HashSet<>(Arrays.asList(Presence.CameraShake)), timeStamp);
    }

    @Override
    public void setViewPoint(ViewPoint viewPoint) {
        this.viewPoint = viewPoint;
    }

    public Set<Presence> getIndicatorHistory(double timeSpan) {
        List<Set<Presence>> presenceTimeline = indicatorHistory.getTimeSpan(timeSpan);
        LinkedHashSet<Presence> indicators = new LinkedHashSet<>();
        for (Set<Presence> set : presenceTimeline) {
            for (Presence item : set) {
                // Add non-existing elements only
                // if not there already to keep the sequence
                // addAll wouldn't keep the sequence, because
                // it would add existing elements at the end
                if (!indicators.contains(item)) {
                    indicators.add(item);
                }
            }
        }
        // remove negated indicators in the result set
        for (Map.Entry<Presence, Presence> entry : negatedRegions.entrySet()) {
            if (indicators.contains(entry.getKey()) && indicators.contains(entry.getValue())) {
                indicators.remove(entry.getValue());
            }
        }
        return indicators;
    }
}
