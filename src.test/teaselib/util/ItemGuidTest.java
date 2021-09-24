package teaselib.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.Test;

import teaselib.core.util.Persist;
import teaselib.core.util.QualifiedItem;

public class ItemGuidTest {

    @Test
    public void testPersisted() throws Exception {
        QualifiedItem fooBar = QualifiedItem.of("Foo.Bar#foo_bar");
        ItemGuid itemGuid = ItemGuid.from(fooBar);
        assertEquals(ItemGuid.from("foo", "bar", "foo_bar"), itemGuid);

        List<String> persisted = itemGuid.persisted();
        assertEquals(1, persisted.size());
        assertEquals("Foo.Bar#foo_bar", Persist.from(persisted.get(0)));
    }

}
