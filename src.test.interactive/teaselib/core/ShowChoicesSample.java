/**
 * 
 */
package teaselib.core;

import org.junit.Test;

import teaselib.ScriptFunction;
import teaselib.TeaseScript;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;
import teaselib.test.TestScript;

/**
 * Copy the contents of the run() method into a TeaseScript instance to test
 * interaction on a host with a real user interface.
 * 
 * @author Citizen-Cane
 *
 */
public class ShowChoicesSample extends TeaseScript {

    /**
     * @param teaseLib
     * @param resources
     * @param actor
     * @param namespace
     */
    public ShowChoicesSample() {
        super(new TeaseLib(new DummyHost(), new DummyPersistence()), new ResourceLoader(ShowChoicesSample.class),
                TestScript.TestScriptActor, TestScript.TestScriptNamespace);
    }

    @Test
    public void demonstrateInteraction() {
        run();
    }

    @Override
    public void run() {
        say("In main ");
        if (reply(new ScriptFunction() {
            @Override
            public void run() {
                say("Start of script function 1.");

                say("Answer question level 1.");
                reply("Yes Level 1", "No Level 1");

                reply(new ScriptFunction() {
                    @Override
                    public void run() {
                        say("Start of script function 2.");

                        say("Answer question level 2.");
                        reply("Wow Level 2", "Oh Level 2");

                        reply(new ScriptFunction() {
                            @Override
                            public void run() {
                                say("Start of script function 3.");

                                say("Answer question level 3.");
                                reply("No Level 3", "Wow Level 3", "Oh Level 3");
                                say("End of script function 3");

                            }
                        }, "Stop script function 3");

                        say("End of script function 2");

                    }
                }, "Stop script function 2");

                say("End of script function 1.");

            }
        }, "Stop script function 1").equals("Stop script function 1")) {
            say("Script function 1 stopped");
        } else {
            say("Resuming main script");
        }
        completeAll();
    }
}
