package teaselib.core.devices;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import teaselib.Toys;
import teaselib.core.StateImpl;
import teaselib.core.TeaseLib;
import teaselib.core.util.Persist;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class ActionItemTest {
    public static final class ActionItem extends StateImpl implements Persist.Persistable {
        static final AtomicBoolean Success = new AtomicBoolean(false);

        public ActionItem(TeaseLib teaseLib, String devicePath) {
            super(teaseLib, TeaseLib.DefaultDomain, devicePath);
        }

        @Override
        public List<String> persisted() {
            return Arrays.asList(Persist.persist(domain), Persist.persist(item.toString()));
        }

        public ActionItem(Persist.Storage storage) {
            super(storage.getInstance(TeaseLib.class), storage.next(), storage.next());
        }

        public String devicePath() {
            return item.toString();
        }

        @Override
        public Persistence remove() {
            Success.set(true);
            return this;
        }
    }

    @Test
    public void testActionItemQualified() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        ActionItem actionItem = new ActionItem(script.teaseLib, devicePath);
        String action = Persist.persist(actionItem);
        ActionItem restored = (ActionItem) Persist.from(action, clazz -> script.teaseLib);
        assertEquals(devicePath, restored.devicePath());

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(action);

        // start(actuator);

        restraints.remove();

        assertEquals(true, ActionItem.Success.getAndSet(false));
    }
}
