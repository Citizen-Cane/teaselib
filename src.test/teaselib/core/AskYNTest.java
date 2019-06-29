package teaselib.core;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.Answer;
import teaselib.Answer.Meaning;
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

    @Test
    public void testReplyMultipleYesNo() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("No", Debugger.Response.Choose);

        Answer answer = script.reply(Answer.yes("Yes"), Answer.yes("Maybe"), Answer.yes("Perhaps"), Answer.no("No"));
        assertEquals(answer.meaning, Meaning.NO);
    }

    @Test
    public void testReplyMultipleYes1() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("Yes", Debugger.Response.Choose);

        Answer answer = script.reply(Answer.yes("Yes"), Answer.yes("Maybe"), Answer.yes("Perhaps"), Answer.no("No"));
        assertEquals(answer.meaning, Meaning.YES);
    }

    @Test
    public void testReplyMultipleYes2() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("Maybe", Debugger.Response.Choose);

        Answer answer = script.reply(Answer.yes("Yes"), Answer.yes("Maybe"), Answer.yes("Perhaps"), Answer.no("No"));
        assertEquals(answer.meaning, Meaning.YES);
    }

    @Test
    public void testReplyMultipleYes3() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("Perhaps", Debugger.Response.Choose);

        Answer answer = script.reply(Answer.yes("Yes"), Answer.yes("Maybe"), Answer.yes("Perhaps"), Answer.no("No"));
        assertEquals(answer.meaning, Meaning.YES);
    }

}
