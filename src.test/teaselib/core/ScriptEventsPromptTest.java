package teaselib.core;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import teaselib.Bondage;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.devices.release.KeyReleaseBaseTest;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.test.TestScript;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class ScriptEventsPromptTest extends KeyReleaseBaseTest {
    private static final List<Actuator> actuatorMocks = Arrays.asList(new ActuatorMock(2, TimeUnit.HOURS),
            new ActuatorMock(1, TimeUnit.HOURS));

    static final String FOOBAR = "foobar";

    private TestScript script;
    private KeyReleaseSetup keyReleaseSetup;
    private KeyRelease keyRelease;

    @BeforeEach
    public void setup() throws IOException {
        script = new TestScript();
        keyReleaseSetup = script.interaction(KeyReleaseSetup.class);
    }

    @AfterEach
    public void detachDevice() {
        keyReleaseSetup.deviceInteraction.deviceDisconnected(new DeviceEventMock(keyRelease));
        script.close();
    }

    @Test
    void testDeviceConnectInvokesPromptHandler() {
        script.debugger.addResponse(FOOBAR, Debugger.Response.Ignore);
        AtomicBoolean triggered = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        keyReleaseSetup.prepare(script.items(Bondage.Chains).inventory(), items -> {
            triggered.set(true);
            done.countDown();
        });
        script.say(FOOBAR);
        script.reply(() -> {
            keyRelease = new KeyReleaseMock(actuatorMocks);
            keyReleaseSetup.deviceInteraction.deviceConnected(new DeviceEventMock(keyRelease));
            try {
                Assertions.assertTrue(done.await(5, TimeUnit.SECONDS), "Awaiting prepare instruction timed out");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        }, FOOBAR);

        Assertions.assertTrue(triggered.get(), "Prepare instructions not called");
    }

}
