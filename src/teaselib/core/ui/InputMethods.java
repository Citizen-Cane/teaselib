package teaselib.core.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * @author Citizen-Cane
 *
 */
public class InputMethods extends ArrayList<InputMethod> {

    private static final long serialVersionUID = 1L;

    public static class Initializers extends HashMap<Class<? extends InputMethod>, InputMethod.Setup> {
        private static final long serialVersionUID = 1L;

        public void setup(InputMethod inputMethod) {
            get(inputMethod.getClass()).apply();
        }

    }

    public InputMethods() {
        super();
    }

    public InputMethods(InputMethod inputMethod) {
        super(Collections.singleton(inputMethod));
    }

    public InputMethods(InputMethods inputMethods) {
        super(inputMethods);
    }

    Initializers initializers(Choices choices) {
        Initializers initializers = new Initializers();
        for (InputMethod inputMethod : this) {
            initializers.put(inputMethod.getClass(), inputMethod.getSetup(choices));
        }
        return initializers;
    }

}
