package teaselib.hosts;

import teaselib.Clothes;
import teaselib.Toys;
import teaselib.core.util.PropertyNameMapping;

public class SexScriptsPropertyNameMapping extends PropertyNameMapping {

    @Override
    public String mapPath(String domain, String path, String name) {
        return super.mapPath(domain, path, name).toLowerCase();
    }

    private static String mapDomainsToDefaults(String name) {
        if (name.contains(
                ("Male." + Clothes.class.getSimpleName()).toLowerCase()
                        + ".")) {
            name = name.substring(name.indexOf(
                    Clothes.class.getSimpleName().toLowerCase() + "."));
        } else if (name.contains(
                ("Female." + Clothes.class.getSimpleName()).toLowerCase()
                        + ".")) {
            name = name.substring(name.indexOf(
                    Clothes.class.getSimpleName().toLowerCase() + "."));
        }
        return name;
    }

    private static String mapToyName(String name) {
        if (("toys." + Toys.Ball_Gag.name()).equalsIgnoreCase(name)) {
            name = "toys.ballgag";
        }
        return name;
    }

    /**
     * TeaseLib uses a lot of standard properties instead of just plain strings,
     * and those don't match the way SexScripts save it's properties.
     * 
     * @param name
     *            The name of a property
     * @return The actual property name or the original name
     */
    private static String mapName(String name) {
        name = mapDomainsToDefaults(name);
        name = mapClassNames(name);
        name = mapToyName(name);
        return name;
    }

    private static String mapClassNames(String name) {
        return reduceToSimpleName(name, Toys.class);
    }
}
