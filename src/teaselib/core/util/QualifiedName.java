package teaselib.core.util;

public class QualifiedName implements Comparable<QualifiedName> {
    private static final char SEPARATOR = '.';
    public static final String NONE = "";

    public static Object of(String domain, String namespace, String name) {
        return new QualifiedName(domain, namespace, name);
    }

    private final String domain;
    private final String namespace;
    private final String name;

    // TODO Enums, nested classes ($),
    // None is needed to model default domain, sexscripts variable names
    public QualifiedName(String domain, String namespace, String name) {
        super();
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

}
