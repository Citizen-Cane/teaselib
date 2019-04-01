package teaselib.core.speechrecognition.srgs;

import java.util.List;

import org.junit.Test;

public class SequenceUtilTest {

    @Test
    public void testSlice() {
        List<Sequences<String>> slices = SequenceUtil.slice( //
                "My dog jumped over the fence", //
                "The dog looked over the fence", //
                "The dog jumped over the fence", //
                "The dog jumped over the fence");
        System.out.println(slices);
    }

}
