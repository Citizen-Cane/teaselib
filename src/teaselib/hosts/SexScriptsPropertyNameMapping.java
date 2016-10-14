package teaselib.hosts;

import teaselib.Clothes;
import teaselib.Toys;
import teaselib.core.util.PropertyNameMapping;

public class SexScriptsPropertyNameMapping extends PropertyNameMapping {

    @Override
    public String mapDomain(String domain, String path, String name) {
        if ("Male".equals(domain)
                && Clothes.class.getSimpleName().equals(path)) {
            return DefaultDomain;
        } else if ("Female".equals(domain)
                && Clothes.class.getSimpleName().equals(path)) {
            return DefaultDomain;
        } else {
            return super.mapDomain(domain, path, name);
        }
    }

    @Override
    public String mapPath(String domain, String path, String name) {
        String mappedPath = super.mapPath(domain, path, name);
        if (lowerCaseRequired(path)) {
            return mappedPath.toLowerCase();
        } else {
            return mappedPath;
        }
    }

    @Override
    public String mapName(String domain, String path, String name) {
        if (Toys.class.getSimpleName().equals(path)
                && Toys.Ball_Gag.name().equals(name)) {
            return "ballgag";
        } else {
            String mappedName = super.mapName(domain, path, name);
            if (lowerCaseRequired(path)) {
                return mappedName.toLowerCase();
            } else {
                return mappedName;
            }
        }
    }

    private static boolean lowerCaseRequired(String path) {
        boolean lowerCaseRequiredBecauseSexScriptLooksForLowerCaseProperties = "Toys"
                .equals(path) || "Clothes".equals(path);
        return lowerCaseRequiredBecauseSexScriptLooksForLowerCaseProperties;
    }
}
