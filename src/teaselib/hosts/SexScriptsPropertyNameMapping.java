package teaselib.hosts;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import teaselib.Clothes;
import teaselib.Household;
import teaselib.Sexuality;
import teaselib.Sexuality.Sex;
import teaselib.Toys;
import teaselib.core.TeaseLib;
import teaselib.core.util.PropertyNameMapping;
import teaselib.core.util.QualifiedName;

/**
 * Map TeaseLib enumerations to SexSripts state, and correct a few naming issues, precisely the use of british vs us
 * english.
 * 
 * All code and comments in teaselib is written (or at least supposed to be written) in us english, but everything
 * relevant to the user is uk english, as a tribute to "english discipline".
 * 
 * @author Citizen-Cane
 *
 */
public class SexScriptsPropertyNameMapping extends PropertyNameMapping {
    public static final QualifiedName SEXUALITY_SEX = new QualifiedName("", "Sexuality", "Sex");
    public static final QualifiedName INTRO_FEMALE = new QualifiedName("", "intro", "female");

    private static final Set<String> LOWER_CASE_NAMES = new HashSet<>(Arrays.asList("Toys", "Clothes", "Household"));

    // @Override
    // public boolean has(Persistence persistence, QualifiedName name) {
    // if (SEXUALITY_SEX.equals(name)) {
    // return persistence.has(INTRO_FEMALE);
    // } else {
    // return persistence.has(name);
    // }
    // }
    //
    // @Override
    // public String get(Persistence persistence, QualifiedName name) {
    // if (SEXUALITY_SEX.equals(name)) {
    // if ("true".equalsIgnoreCase(persistence.get(INTRO_FEMALE))) {
    // return Sex.Female.name();
    // } else {
    // return Sex.Male.name();
    // }
    // } else {
    // return persistence.get(name);
    // }
    // }
    //
    // @Override
    // public void set(Persistence persistence, QualifiedName name, String value) {
    // if (SEXUALITY_SEX.equals(name)) {
    // if (Sex.Female.name().equalsIgnoreCase((value))) {
    // persistence.set(INTRO_FEMALE, false);
    // } else {
    // persistence.set(INTRO_FEMALE, true);
    // }
    // } else {
    // persistence.set(name, value);
    // }
    // }
    //
    // @Override
    // public void clear(Persistence persistence, QualifiedName name) {
    // if (SEXUALITY_SEX.equals(name)) {
    // persistence.clear(INTRO_FEMALE);
    // } else {
    // persistence.clear(name);
    // }
    // }

    // TODO Map Sexuality.Sex to intro.female -> check that key exists and can be cleared
    // TODO For Sexuality.Sex, return intro.female ? Female : Male

    @Override
    protected QualifiedName mapDomainsAndPaths(QualifiedName name) {
        if (name.domainEquals("Male") && name.namespaceEquals(Clothes.class.getSimpleName())) {
            return name.withDomain(TeaseLib.DefaultDomain);
        } else if (name.domainEquals("Female") && name.namespaceEquals(Clothes.class.getSimpleName())) {
            return name.withDomain(TeaseLib.DefaultDomain);
        } else if (name.equals(Sexuality.Orientation.LikesMales)) {
            return QualifiedName.of(name.domain, "intro", "likemale");
        } else if (name.equals(Sexuality.Orientation.LikesFemales)) {
            return QualifiedName.of(name.domain, "intro", "likefemale");
        } else if (name.equalsClass(Sexuality.Sex.class)) {
            return QualifiedName.of(name.domain, INTRO_FEMALE.namespace, INTRO_FEMALE.name);
        } else if (name.namespaceEquals(Household.class.getSimpleName())) {
            return name.withNamespace("toys");
        } else if (lowerCaseRequired(name.namespace)) {
            return name.withNamespace(name.namespace.toLowerCase());
        } else {
            return name;
        }
    }

    @Override
    protected QualifiedName mapNames(QualifiedName name) {
        if (is(Toys.Gag, Toys.Gags.Ball_Gag.name(), name)) {
            return name.withName("ballgag");
        } else if (is(Toys.Cock_Ring, name)) {
            return name.withName("cockring");
        } else if (is(Toys.EStim_Device, name)) {
            return name.withName("estim");
        } else if (is(Household.Clothes_Pegs, name)) {
            return name.withName("clothespins");
        } else if (is(Household.Cigarettes, name)) {
            return name.withName("cigarette");
        } else if (is(Household.Tampons, name)) {
            return name.withName("tampon");
        } else {
            return name;
        }
    }

    @Override
    protected String mapValueFromHost(QualifiedName name, String value) {
        if (INTRO_FEMALE.equals(name)) {
            if ("true".equalsIgnoreCase(value)) {
                return Sex.Female.name();
            } else {
                return Sex.Male.name();
            }
        } else {
            return value;
        }
    }

    @Override
    protected String mapValueToHost(QualifiedName name, String value) {
        if (name.equals(SEXUALITY_SEX)) {
            if (Sex.Female.name().equalsIgnoreCase((value))) {
                return Boolean.toString(true);
            } else {
                return Boolean.toString(false);
            }
        } else {
            return value;
        }
    }

    private static boolean is(Enum<?> item, String guid, QualifiedName name) {
        return item.getClass().getSimpleName().equalsIgnoreCase(name.namespace) && guid.equalsIgnoreCase(name.name);
    }

    private static boolean is(Enum<?> item, QualifiedName name) {
        return item.getClass().getSimpleName().equalsIgnoreCase(name.namespace)
                && item.name().equalsIgnoreCase(name.name);
    }

    private static boolean lowerCaseRequired(String path) {
        return LOWER_CASE_NAMES.contains(path);
    }
}
