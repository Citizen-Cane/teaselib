package teaselib.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PersistTest {

    @Test
    public void testPersistToString() throws Exception {
        String test = "foobar";
        String serialized = Persist.persist(test);

        assertEquals("Class=java.lang.String;Value=foobar", serialized);
    }

    @Test
    public void testPersistFromString() throws Exception {
        String serialized = "Class=java.lang.String;Value=foobar";
        String deserialized = Persist.from(serialized);

        assertEquals("foobar", deserialized);
    }

}
