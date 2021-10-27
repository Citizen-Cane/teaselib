package teaselib.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import teaselib.Bondage;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.devices.release.KeyReleaseBaseTest;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.test.TestScript;

public class ScriptEventsPromptTest extends KeyReleaseBaseTest {
    private static final List<Actuator> actuatorMocks = Arrays.asList(new ActuatorMock(2, TimeUnit.HOURS),
            new ActuatorMock(1, TimeUnit.HOURS));

    static final String FOOBAR = "foobar";

    private TestScript script;
    private KeyReleaseSetup keyReleaseSetup;
    private KeyRelease keyRelease;

    @Before
    public void setup() throws IOException {
        script = new TestScript();
        keyReleaseSetup = script.interaction(KeyReleaseSetup.class);
    }

    @After
    public void detachDevice() {
        keyReleaseSetup.deviceInteraction.deviceDisconnected(new DeviceEventMock(keyRelease));
        script.close();
    }

    @Test
    public void testDeviceConnectInvokesPromptHandler() {
        script.debugger.addResponse(FOOBAR, Debugger.Response.Ignore);
        AtomicBoolean triggered = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        keyReleaseSetup.prepare(script.items(Bondage.Chains), items -> {
            triggered.set(true);
            done.countDown();
        });
        script.say(FOOBAR);
        script.reply(() -> {
            keyRelease = new KeyReleaseMock(actuatorMocks);
            keyReleaseSetup.deviceInteraction.deviceConnected(new DeviceEventMock(keyRelease));
            try {
                assertTrue("Awaiting prepare instruction timed out", done.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        }, FOOBAR);

        assertEquals("Prepare instructions not called", true, triggered.get());
    }

}
