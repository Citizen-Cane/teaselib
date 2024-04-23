package teaselib.core.util;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 
 * domain:name.space/name
 * 
 * @author Citizen-Cane
 *
 */
public class QualifiedName implements Comparable<QualifiedName> {
    private static final char DOMAIN_SEPARATOR = ':';
    private static final char NAMESPACE_SEPARATOR = '/';
    public static final String NONE = "";

    protected static final String[] StrippedPackageNames = { "teaselib.scripts", "teaselib" };

    public static List<String> strip(List<String> paths) {
        return paths.stream().map(QualifiedName::strip).collect(toList());
    }

    public static String strip(String path) {
        for (String packageName : StrippedPackageNames) {
            var string = packageName + ".";
            if (path.startsWith(string)) {
                return path.substring(string.length());
            }
        }
        return path;
    }

    public static QualifiedName of(String domain, String namespace, String name) {
        return new QualifiedName(strip(domain), strip(namespace), name);
    }

    public static QualifiedName of(String domain, String namespace, String... names) {
        return new QualifiedName(strip(domain), strip(namespace), path(names));
    }

    public static String path(String... parts) {
        Iterator<String> part = Arrays.asList(parts).iterator();
        StringBuilder qualifiedName = new StringBuilder(part.next().toLowerCase());
        while (part.hasNext()) {
            qualifiedName.append(".");
            qualifiedName.append(part.next().toLowerCase());
        }
        return qualifiedName.toString();
    }

    public static QualifiedName of(String domain, Enum<?> item) {
        var qualifiedItem = QualifiedString.of(item);
        return QualifiedName.of(strip(domain), strip(qualifiedItem.namespace()), qualifiedItem.name());
    }

    public final String domain;
    public final String namespace;
    public final String name;

    public QualifiedName(String domain, String namespace, String name) {
        Objects.requireNonNull(domain);
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(name);

        this.domain = domain;
        this.namespace = namespace;
        this.name = name;
    }

    @Override
    public String toString() {
        // TODO general solution from PropertyNameMapping
        if (domain.equals(NONE) && namespace.equals(NONE)) {
            return name;
        } else if (domain.equals(NONE)) {
            return namespace + NAMESPACE_SEPARATOR + name;
        } else if (namespace.equals(NONE)) {
            return domain + DOMAIN_SEPARATOR + NAMESPACE_SEPARATOR + name;
        } else if (name.equals(NONE)) {
            return domain + DOMAIN_SEPARATOR + namespace;
        } else {
            return domain + DOMAIN_SEPARATOR + namespace + NAMESPACE_SEPARATOR + name;
        }
    }

    @Override
    public int compareTo(QualifiedName o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
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
        QualifiedName other = (QualifiedName) obj;
        if (domain == null) {
            if (other.domain != null)
                return false;
        } else if (!domain.equals(other.domain))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        } else if (!namespace.equals(other.namespace))
            return false;
        return true;
    }

    public QualifiedName withDomain(String newDomain) {
        return QualifiedName.of(newDomain, this.namespace, this.name);
    }

    public QualifiedName withNamespace(String newNamespace) {
        return QualifiedName.of(this.domain, newNamespace, this.name);
    }

    public QualifiedName withName(String newName) {
        return QualifiedName.of(this.domain, this.namespace, newName);
    }

    public boolean domainEquals(String other) {
        return this.domain.equalsIgnoreCase(other);
    }

    public boolean namespaceEquals(String other) {
        return this.namespace.equalsIgnoreCase(other);
    }

    public boolean nameEquals(String other) {
        return this.name.equalsIgnoreCase(other);
    }

    public boolean equals(Enum<?> item) {
        var qualifiedString = QualifiedString.of(item);
        return this.namespace.equalsIgnoreCase(strip(qualifiedString.namespace()))
                && this.name.equalsIgnoreCase(qualifiedString.name());
    }

    public boolean equalsClass(Class<?> clazz) {
        var qualifiedString = QualifiedString.of(ReflectionUtils.qualified(clazz));
        return this.namespace.equalsIgnoreCase(strip(qualifiedString.namespace()))
                && this.name.equalsIgnoreCase(qualifiedString.name());
    }

}
