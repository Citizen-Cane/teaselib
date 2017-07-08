package teaselib.core.ui;

import java.util.Map;

/**
 * @author Citizen-Cane
 *
 */
public interface InputMethod {

    void show(Prompt prompt);

    boolean dismiss(Prompt prompt) throws InterruptedException;

    Map<String, Runnable> getHandlers();

}