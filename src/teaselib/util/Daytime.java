package teaselib.util;

/**
 * @author Citizen-Cane
 *
 */
public enum Daytime {
    Morning,
    Forenoon,
    Noon,
    Afternoon,
    Evening,
    Night

    ;

    public Daytime earlier() {
        switch (this) {
        case Morning:
            return Night;
        case Forenoon:
            return Morning;
        case Noon:
            return Forenoon;
        case Afternoon:
            return Noon;
        case Evening:
            return Afternoon;
        case Night:
            return Evening;
        default:
            throw new UnsupportedOperationException(toString());
        }
    }

    public Daytime later() {
        switch (this) {
        case Morning:
            return Forenoon;
        case Forenoon:
            return Noon;
        case Noon:
            return Afternoon;
        case Afternoon:
            return Evening;
        case Evening:
            return Night;
        case Night:
            return Morning;
        default:
            throw new UnsupportedOperationException(toString());
        }
    }

}
