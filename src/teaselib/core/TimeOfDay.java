package teaselib.core;

import teaselib.util.Daytime;

/**
 * @author Citizen-Cane
 *
 */
public interface TimeOfDay {
    boolean is(Daytime dayTime);

    boolean isEarlierThan(Daytime dayTime);

    boolean isLaterThan(Daytime dayTime);

    boolean isAnyOf(Daytime... daytimes);

    Daytime dayTime();

}
