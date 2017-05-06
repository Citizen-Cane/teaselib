package teaselib.core;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import teaselib.Toys;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;

public class UserItemsTest {

    @Test
    public void testGet() throws Exception {
        UserItems items = new AbstractUserItems() {

            @Override
            protected Item[] createUserItems(TeaseLib teaseLib, String domain,
                    QualifiedItem<?> item) {
                throw new UnsupportedOperationException();
            }
        };

        for (Toys toy : Toys.values()) {
            assertNotNull(items.defaults(new QualifiedEnum(toy)));
        }
    }

}
