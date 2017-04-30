package teaselib.core;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import teaselib.Toys;
import teaselib.util.Item;

public class UserItemsTest {

    @Test
    public void testGet() throws Exception {
        UserItems items = new AbstractUserItems() {

            @Override
            protected Item[] createUserItems(TeaseLib teaseLib, String domain, Object item) {
                throw new UnsupportedOperationException();
            }
        };

        for (Toys toy : Toys.values()) {
            assertNotNull(items.defaults(toy));
        }
    }

}
