package teaselib.core.functional;

@FunctionalInterface
public interface TriFunction<A, B, C, T> {

    public T apply(A a, B b, C c);
}