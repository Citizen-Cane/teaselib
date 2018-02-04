package teaselib.core.devices;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import teaselib.Toys;
import teaselib.core.TeaseLib;
import teaselib.core.util.Persist;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.ItemImpl;

public class ActionItemTest {
    public static final class ActionItem extends ItemImpl implements Persist.Persistable {
        final AtomicBoolean success = new AtomicBoolean(false);

        public ActionItem(TeaseLib teaseLib, String devicePath) {
            super(teaseLib, devicePath, TeaseLib.DefaultDomain, devicePath, devicePath);
        }

        @Override
        public List<String> persisted() {
            return Arrays.asList(Persist.persist(item.toString()), Persist.persist(domain), Persist.persist(guid),
                    Persist.persist(displayName));
        }

        public ActionItem(Persist.Storage storage) {
            super(storage.getInstance(TeaseLib.class), storage.next(), storage.next(), storage.next(), storage.next());
        }

        @Override
        public Persistence remove() {
            success.set(true);
            return null;
        }
    }

    @Test
    public void testActionItemQualified() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        ActionItem actionItem = new ActionItem(script.teaseLib, devicePath);
        String action = Persist.persist(actionItem);
        ActionItem restored = (ActionItem) Persist.from(action, clazz -> script.teaseLib);
        assertEquals(devicePath, restored.displayName());

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(action);

        // start(actuator);

        restraints.remove();

        // assertEquals(true, actionItem.success.get());
    }

}
