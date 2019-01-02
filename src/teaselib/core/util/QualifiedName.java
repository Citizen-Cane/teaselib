package teaselib.core.util;

import java.util.Objects;

public class QualifiedName implements Comparable<QualifiedName> {
    private static final char SEPARATOR = '.';
    public static final String NONE = "";

    protected static final String[] StrippedPackageNames = { "teaselib", "teaselib.scripts" };

    private static String stripPath(String path) {
        for (String packageName : StrippedPackageNames) {
            String string = packageName + ".";
            if (path.startsWith(string)) {
                path = path.substring(string.length());
            }
        }
        return path;
    }

    public static QualifiedName of(String domain, String namespace, String name) {
        return new QualifiedName(domain, stripPath(namespace), name);
    }

    public static QualifiedName of(String domain, Enum<?> item) {
        QualifiedItem qualifiedItem = QualifiedItem.of(item);
        return QualifiedName.of(domain, stripPath(qualifiedItem.namespace()), qualifiedItem.name());
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
        if (domain.equals(NONE)) {
            return namespace + SEPARATOR + name;
        } else if (namespace.equals(NONE)) {
            return domain + SEPARATOR + name;
        } else if (name.equals(NONE)) {
            return domain + SEPARATOR + namespace;
        } else {
            return domain + SEPARATOR + namespace + SEPARATOR + name;
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

    public QualifiedName withDomain(String domain) {
        return QualifiedName.of(domain, this.namespace, this.name);
    }

    public QualifiedName withNamespace(String namespace) {
        return QualifiedName.of(this.domain, namespace, this.name);
    }

    public QualifiedName withName(String name) {
        return QualifiedName.of(this.domain, this.namespace, name);
    }

    public boolean domainEquals(String domain) {
        return this.domain.equalsIgnoreCase(domain);
    }

    public boolean namespaceEquals(String namespace) {
        return this.namespace.equalsIgnoreCase(namespace);
    }

    public boolean nameEquals(String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public boolean equals(Enum<?> item) {
        QualifiedItem qualifiedItem = QualifiedItem.of(item);
        return this.namespace.equalsIgnoreCase(stripPath(qualifiedItem.namespace()))
                && this.name.equalsIgnoreCase(qualifiedItem.name());
    }

    public boolean equalsClass(Class<?> clazz) {
        QualifiedItem qualifiedItem = QualifiedItem.of(ReflectionUtils.normalizedClassName(clazz));
        return this.namespace.equalsIgnoreCase(stripPath(qualifiedItem.namespace()))
                && this.name.equalsIgnoreCase(qualifiedItem.name());
    }

}
