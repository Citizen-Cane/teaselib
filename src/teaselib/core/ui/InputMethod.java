/**
 * 
 */
package teaselib.core.ui;

import teaselib.core.ui.PromptQueue.Todo;

/**
 * @author someone
 *
 */
public interface InputMethod {

    void show(Todo todo);

    boolean dismiss(Prompt prompt) throws InterruptedException;

}