package teaselib.core;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import teaselib.Answer;
import teaselib.Answer.Meaning;
import teaselib.test.TestScript;

class AskYNTest {

    @Test
    void testYes() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("Yes", Debugger.Response.Choose);

        assertTrue(script.askYN("Yes", "No"));
        assertFalse(script.askYN("No", "Yes"));
    }

    @Test
    void testNo() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("No", Debugger.Response.Choose);

        assertFalse(script.askYN("Yes", "No"));
        assertTrue(script.askYN("No", "Yes"));
    }

    @Test
    void testYesAnswer() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("Yes", Debugger.Response.Choose);

        Answer.Yes yes = Answer.yes("Yes");
        Answer.No no = Answer.no("No");

        assertTrue(script.askYN(yes, no));
        assertTrue(script.askYN(no, yes));

        assertTrue(script.askYN(yes, Answer.resume("No")));
        assertTrue(script.askYN(Answer.resume("No"), yes));

        assertTrue(script.askYN(no, Answer.resume("Yes")));
    }

    @Test
    void testNoAnswer() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("No", Debugger.Response.Choose);

        Answer.Yes yes = Answer.yes("Yes");
        Answer.No no = Answer.no("No");

        assertFalse(script.askYN(yes, no));
        assertFalse(script.askYN(no, yes));

        assertFalse(script.askYN(Answer.resume("Yes"), no));
        assertFalse(script.askYN(no, Answer.resume("Yes")));

        assertFalse(script.askYN(yes, Answer.resume("No")));
    }

    @Test
    void testReplyMultipleYesNo() {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse("No", Debugger.Response.Choose);

        Answer answer = script.reply(Answer.yes("Yes"), Answer.yes("Maybe"), Answer.yes("Perhaps"), Answer.no("No"));
        assertEquals(Meaning.NO, answer.meaning);
    }

    @ParameterizedTest
    @ValueSource(strings = { "Yes", "Maybe", "Perhaps" })
    void testReplyMultipleYes(String choice) {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse(choice, Debugger.Response.Choose);
        Answer answer = script.reply(Answer.yes("Yes"), Answer.yes("Maybe"), Answer.yes("Perhaps"), Answer.no("No"));
        assertEquals(Meaning.YES, answer.meaning);
    }

    @ParameterizedTest
    @ValueSource(strings = { "No", "Never", "Not ever" })
    void testReplyMultipleNo(String choice) {
        TestScript script = TestScript.getOne();
        script.debugger.addResponse(choice, Debugger.Response.Choose);
        Answer answer = script.reply(Answer.yes("Yes"), Answer.no("No"), Answer.no("Never"), Answer.no("Not ever"));
        assertEquals(Meaning.NO, answer.meaning);
    }

}
