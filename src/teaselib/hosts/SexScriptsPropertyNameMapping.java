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

    public SexScriptsPropertyNameMapping(Persistence persistence) {
        super(persistence);
    }

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
        if (is(Toys.Gag, Toys.Gags.Ball_Gag.name(), path, name)) {
            return "ballgag";
        } else if (is(Toys.Cock_Ring, path, name)) {
            return "cockring";
        } else if (is(Toys.EStim_Device, path, name)) {
            return "estim";
        } else if (is(Household.Clothes_Pegs, path, name)) {
            return "clothespins";
        } else if (is(Household.Cigarettes, path, name)) {
            return "cigarette";
        } else if (is(Household.Tampons, path, name)) {
            return "tampon";
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

    private static boolean is(Enum<?> item, String guid, String path, String name) {
        return item.getClass().getSimpleName().equals(path) && guid.equalsIgnoreCase(name);
    }

    private static boolean is(Enum<?> item, String path, String name) {
        return item.getClass().getSimpleName().equals(path) && item.name().equalsIgnoreCase(name);
    }

    @Override
    public boolean has(String name) {
        if (SEXUALITY_SEX.equals(name)) {
            return super.has(INTRO_FEMALE);
        } else {
            return super.has(name);
        }
    }

    @Override
    public String get(String name) {
        if (SEXUALITY_SEX.equals(name)) {
            if ("true".equals(super.get(INTRO_FEMALE))) {
                return Sex.Female.name();
            } else {
                return Sex.Male.name();
            }
        } else {
            return super.get(name);
        }
    }

    @Override
    public void set(String name, String value) {
        if (SEXUALITY_SEX.equals(name)) {
            if ("Female".equals(value)) {
                super.set(INTRO_FEMALE, false);
            } else {
                super.set(INTRO_FEMALE, true);
            }
        } else {
            super.set(name, value);
        }
    }

    @Override
    public void clear(String name) {
        if (SEXUALITY_SEX.equals(name)) {
            super.clear(INTRO_FEMALE);
        } else {
            super.clear(name);
        }
    }

    private static boolean lowerCaseRequired(String path) {
        return LOWER_CASE_NAMES.contains(path);
    }
}
