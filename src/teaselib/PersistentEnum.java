package teaselib;

/**
 * @author Citizen-Cane
 *
 */
public interface PersistentEnum<T> {

    T fromString(String value);

    String toString(T value);

    public interface Bool extends PersistentEnum<Boolean> {
        @Override
        default Boolean fromString(String value) {
            return Boolean.parseBoolean(value);
        }

        @Override
        default String toString(Boolean value) {
            return Boolean.toString(value);
        }
    }

    // TODO keep only long
    public interface Number extends PersistentEnum<Integer> {
        @Override
        default Integer fromString(String value) {
            return Integer.parseInt(value);
        }

        @Override
        default String toString(Integer value) {
            return Integer.toString(value);
        }
    }

    public interface LongNumber extends PersistentEnum<Long> {
        @Override
        default Long fromString(String value) {
            return Long.parseLong(value);
        }

        @Override
        default String toString(Long value) {
            return Long.toString(value);
        }
    }

    public interface FloatNumber extends PersistentEnum<Double> {
        @Override
        default Double fromString(String value) {
            return Double.parseDouble(value);
        }

        @Override
        default String toString(Double value) {
            return Double.toString(value);
        }
    }

    public interface StringValue extends PersistentEnum<String> {
        @Override
        default String fromString(String value) {
            return value;
        }

        @Override
        default String toString(String value) {
            return value;
        }
    }

}
