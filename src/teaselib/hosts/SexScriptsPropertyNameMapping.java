package teaselib.hosts;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import teaselib.Clothes;
import teaselib.Household;
import teaselib.Sexuality;
import teaselib.Sexuality.Sex;
import teaselib.Toys;
import teaselib.core.Persistence;
import teaselib.core.util.PropertyNameMapping;

/**
 * Map TeaseLib enumerations to SexSripts state, and correct a few naming issues, precisely the use of british vs us
 * english.
 * 
 * All code and comments in teaselib is written (or at least supposed to be written) in american english, but everything
 * relevant to the user is uk english, as a tribute to "english discipline".
 * 
 * @author Citizen-Cane
 *
 */
public class SexScriptsPropertyNameMapping extends PropertyNameMapping {
    private static final String SEXUALITY_SEX = "Sexuality.Sex";
    private static final String INTRO_FEMALE = "intro.female";
    private static final Set<String> LOWER_CASE_NAMES = new HashSet<>(Arrays.asList("Toys", "Clothes", "Household"));

    @Override
    public String mapDomain(String domain, String path, String name) {
        if ("Male".equals(domain) && Clothes.class.getSimpleName().equals(path)) {
            return DefaultDomain;
        } else if ("Female".equals(domain) && Clothes.class.getSimpleName().equals(path)) {
            return DefaultDomain;
        } else {
            return super.mapDomain(domain, path, name);
        }
    }

    @Override
    public String mapPath(String domain, String path, String name) {
        String mappedPath = super.mapPath(domain, path, name);
        if ("Sexuality$Orientation".equalsIgnoreCase(path)) {
            return "intro";
        } else if (Household.class.getSimpleName().equalsIgnoreCase(path)) {
            return "toys";
        } else if (lowerCaseRequired(path)) {
            return mappedPath.toLowerCase();
        } else {
            return mappedPath;
        }
    }

    @Override
    public String mapName(String domain, String path, String name) {
        if (Toys.class.getSimpleName().equals(path) && Toys.Gags.Ball_Gag.name().equalsIgnoreCase(name)) {
            return "ballgag";
        } else if (Household.class.getSimpleName().equals(path)
                && Household.Clothes_Pegs.name().equalsIgnoreCase(name)) {
            return "clothespins";
        } else if ("Sexuality$Orientation".equals(path) && Sexuality.Orientation.LikesMales.name().equals(name)) {
            return "likemale";
        } else if ("Sexuality$Orientation".equals(path) && Sexuality.Orientation.LikesFemales.name().equals(name)) {
            return "likefemale";
        } else {
            String mappedName = super.mapName(domain, path, name);
            if (lowerCaseRequired(path)) {
                return mappedName.toLowerCase();
            } else {
                return mappedName;
            }
        }
    }

    @Override
    public boolean has(String name, Persistence persistence) {
        if (SEXUALITY_SEX.equals(name)) {
            return persistence.has(INTRO_FEMALE);
        } else {
            return super.has(name, persistence);
        }
    }

    @Override
    public String get(String name, Persistence persistence) {
        if (SEXUALITY_SEX.equals(name)) {
            if ("true".equals(persistence.get(INTRO_FEMALE))) {
                return Sex.Female.name();
            } else {
                return Sex.Male.name();
            }
        } else {
            return super.get(name, persistence);
        }
    }

    @Override
    public void set(String name, String value, Persistence persistence) {
        if (SEXUALITY_SEX.equals(name)) {
            if ("Female".equals(value)) {
                persistence.set(INTRO_FEMALE, false);
            } else {
                persistence.set(INTRO_FEMALE, true);
            }
        } else {
            super.set(name, value, persistence);
        }
    }

    @Override
    public void clear(String name, Persistence persistence) {
        if (SEXUALITY_SEX.equals(name)) {
            persistence.clear(INTRO_FEMALE);
        } else {
            super.clear(name, persistence);
        }
    }

    private static boolean lowerCaseRequired(String path) {
        return LOWER_CASE_NAMES.contains(path);
    }
}
