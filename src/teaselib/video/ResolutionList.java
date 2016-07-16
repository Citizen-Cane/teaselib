package teaselib.video;

import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Size;

public class ResolutionList extends ArrayList<Size> {
    private static final long serialVersionUID = 1L;

    public ResolutionList(int capacity) {
        super(capacity);
    }

    public ResolutionList(Size... items) {
        super(items.length);
        for (Size resolution : items) {
            add(resolution);
        }
    }

    /**
     * Find a matching or a similar resolution in a list of resolutions.
     * 
     * @param desired
     *            The desired resolution.
     * @param candidates
     *            A list of resolutions, highest first, lowest last
     * @return A matching resolution equal to the requested, a similar
     *         resolution (either matching width or height), a smaller
     *         resolution, or the last entry in the list.
     * 
     */
    public Size getMatchingOrSimilar(Size desired) {
        for (Size candidate : this) {
            if (candidate.width() <= desired.width()
                    && candidate.height() <= desired.height()) {
                return candidate;
            }
        }
        return last();
    }

    /**
     * Find a size similar to the desired size that fits into the available size
     * n times without remainder.
     * 
     * @param available
     *            The available size
     * @param desired
     *            The desired size
     * 
     * @return A size that fits n times into the available size without
     *         remainder.
     */
    public static Size getSmallestFit(Size available, Size desired) {
        int n = available.width() / desired.width();
        int m = available.height() / desired.height();
        int factor = Math.max(n, m);
        return new Size(available.width() / factor,
                available.height() / factor);
    }

    private Size last() {
        return get(size() - 1);
    }

    @Override
    public boolean contains(Object resolution) {
        if (resolution instanceof Size) {
            Size res = (Size) resolution;
            for (Size candidate : this) {
                if (res.width() == candidate.width()
                        && res.height() == candidate.height()) {
                    return true;
                }
            }
        }
        return false;
    }
}
