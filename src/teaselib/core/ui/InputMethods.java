package teaselib.core.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Citizen-Cane
 *
 */
public class InputMethods implements Iterable<InputMethod>, teaselib.core.Closeable {

    public static class Initializers extends HashMap<Class<? extends InputMethod>, InputMethod.Setup> {
        private static final long serialVersionUID = 1L;

        public void setup(InputMethod inputMethod) {
            super.get(inputMethod.getClass()).apply();
        }

    }

    private static final Function<Choices, Boolean> Always = choices -> true;

    private final List<InputMethod> elements = new ArrayList<>();
    private final Map<InputMethod, Function<Choices, Boolean>> conditions = new HashMap<>();

    public InputMethods() {
    }

    public InputMethods(InputMethod inputMethod) {
        add(inputMethod);
    }

    public InputMethods(InputMethods inputMethods) {
        inputMethods.elements.stream().forEach(this::add);
    }

    public void add(InputMethod inputMethod) {
        elements.add(inputMethod);
        conditions.put(inputMethod, Always);
    }

    public void add(InputMethod inputMethod, Function<Choices, Boolean> condition) {
        elements.add(inputMethod);
        conditions.put(inputMethod, condition);
    }

    public boolean remove(InputMethod inputMethod) {
        conditions.remove(inputMethod);
        return elements.remove(inputMethod);
    }

    List<InputMethod> selected(Choices choices) {
        return elements.stream().filter(element -> conditions.get(element).apply(choices)).toList();
    }

    @Override
    public Iterator<InputMethod> iterator() {
        return elements.iterator();
    }

    @SuppressWarnings("unchecked")
    public <T extends InputMethod> T get(Class<? extends T> clazz) {
        return (T) elements.stream().filter(o -> o.getClass() == clazz).findAny().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    public <T extends InputMethod> Optional<T> getOptional(Class<? extends T> clazz) {
        return (Optional<T>) elements.stream().filter(o -> o.getClass() == clazz).findAny();
    }

    Initializers initializers(Choices choices) {
        Initializers initializers = new Initializers();
        for (InputMethod inputMethod : elements) {
            initializers.put(inputMethod.getClass(), inputMethod.getSetup(choices));
        }
        return initializers;
    }

    @Override
    public void close() {
        elements.stream().forEach(InputMethod::close);
    }

}
