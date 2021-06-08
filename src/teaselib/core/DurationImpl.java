package teaselib.core;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import teaselib.util.DurationFormat;

/**
 * An elapsing duration wit h a planned end.
 * 
 * @author Citizen-Cane
 *
 */
public class DurationImpl extends AbstractDuration {

    public DurationImpl(TeaseLib teaseLib) {
        this(teaseLib, 0, TeaseLib.DURATION_TIME_UNIT);
    }

    public DurationImpl(TeaseLib teaseLib, long limit, TimeUnit unit) {
        super(teaseLib, teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT), convertToDuration(limit, unit));
    }

    public DurationImpl(TeaseLib teaseLib, long start, long limit, TimeUnit unit) {
        super(teaseLib, convertToDuration(start, unit), convertToDuration(limit, unit));
    }

    @Override
    public long elapsed(TimeUnit unit) {
        long now = this.teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT);
        return convertToUnit(now - start, unit);
    }

    @Override
    public long since(TimeUnit unit) {
        return 0;
    }

    @Override
    public String toString() {
        return new Date(start(TimeUnit.MILLISECONDS))
                + (limit > 0 ? "+" + DurationFormat.toString(limit, TeaseLib.DURATION_TIME_UNIT) : "");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (limit ^ (limit >>> 32));
        result = prime * result + (int) (start ^ (start >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DurationImpl other = (DurationImpl) obj;
        if (limit != other.limit)
            return false;
        if (start != other.start)
            return false;
        return true;
    }

}