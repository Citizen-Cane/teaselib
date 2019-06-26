package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.Answer;
import teaselib.test.TestScript;

public class AskYNTest {

    @Test
    public void testYes() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("Yes", Debugger.Response.Choose);

        assertTrue(script.askYN("Yes", "No"));
        assertFalse(script.askYN("No", "Yes"));
    }

    @Test
    public void testNo() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("No", Debugger.Response.Choose);

        assertFalse(script.askYN("Yes", "No"));
        assertTrue(script.askYN("No", "Yes"));
    }

    @Test
    public void testYesAnswer() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("Yes", Debugger.Response.Choose);

        Answer yes = Answer.yes("Yes");
        Answer no = Answer.no("No");

        assertTrue(script.askYN(yes, no));
        assertTrue(script.askYN(no, yes));

        assertTrue(script.askYN(yes, Answer.resume("No")));
        assertTrue(script.askYN(Answer.resume("No"), yes));

        assertTrue(script.askYN(no, Answer.resume("Yes")));
    }

    @Test
    public void testNoAnswer() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("No", Debugger.Response.Choose);

        Answer yes = Answer.yes("Yes");
        Answer no = Answer.no("No");

        assertFalse(script.askYN(yes, no));
        assertFalse(script.askYN(no, yes));

        assertFalse(script.askYN(Answer.resume("Yes"), no));
        assertFalse(script.askYN(no, Answer.resume("Yes")));

        assertFalse(script.askYN(yes, Answer.resume("No")));
    }

    @Test
    public void testResumYes() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("Yes", Debugger.Response.Choose);

        Answer yes = Answer.resume("Yes");
        Answer no = Answer.resume("No");

        assertTrue(script.askYN(yes, no));
        assertFalse(script.askYN(no, yes));
    }

    @Test
    public void testResumNo() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("No", Debugger.Response.Choose);

        Answer yes = Answer.resume("Yes");
        Answer no = Answer.resume("No");

        assertFalse(script.askYN(yes, no));
        assertTrue(script.askYN(no, yes));
    }

}
