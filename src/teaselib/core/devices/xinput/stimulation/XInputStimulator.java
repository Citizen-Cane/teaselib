package teaselib.core.devices.xinput.stimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.devices.xinput.XInputDevice;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

public class XInputStimulator implements Stimulator {

    static class SharedState {
        final XInputDevice device;
        int leftMotor = 0;
        int rightMotor = 0;

        public SharedState(XInputDevice device) {
            super();
            this.device = device;
        }

        public void setLeftMotor(int value) {
            leftMotor = value;
            device.setVibration(leftMotor, rightMotor);
        }

        public void setRightMotor(int value) {
            rightMotor = value;
            device.setVibration(leftMotor, rightMotor);
        }
    }

    final StimulationDevice device;
    final SharedState sharedState;
    final int channel;

    private final ExecutorService player = NamedExecutorService.singleThreadedQueue(getClass().getName());
    private final AtomicReference<Optional<Future<Void>>> current = new AtomicReference<>(Optional.empty());
    private final AtomicLong playTime = new AtomicLong(0);

    public ChannelDependency channelDependency = ChannelDependency.Independent;
    // TODO Must be configurable since cannot be detected by hardware
    public Output output = Output.EStim;

    /**
     * Get all stimulators for a device.
     * 
     * @param device
     *            An XInput game controller
     * @return The stimulators share the device, because the outputs can only be set simultaneously
     */
    public static List<XInputStimulator> getStimulators(XInputStimulationDevice device) {
        List<XInputStimulator> stimulators = new ArrayList<>(2);
        XInputStimulator channel0 = new XInputStimulator(device, 0);
        stimulators.add(channel0);
        stimulators.add(new XInputStimulator(channel0, 1));
        return stimulators;
    }

    XInputStimulator(XInputStimulationDevice device, int channel) {
        this.device = device;
        this.sharedState = new SharedState(device.getXInputDevice());
        this.channel = channel;
    }

    XInputStimulator(XInputStimulator sibling, int channel) {
        this.device = sibling.device;
        this.sharedState = sibling.sharedState;
        this.channel = channel;
    }

    private void set(double value) {
        value = Math.max(0.0, value);
        value = Math.min(value, 1.0);
        int strength = (int) (value * XInputDevice.VIBRATION_MAX_VALUE);
        if (channel == 0) {
            sharedState.setLeftMotor(strength);
        } else {
            sharedState.setRightMotor(strength);
        }
    }

    @Override
    public String getDeviceName() {
        return "XBox Gamepad " + sharedState.device.getPlayerNum() + " " + (channel == 0 ? "Left" : "Right")
                + " channel";
    }

    @Override
    public String getLocation() {
        return channel == 0 ? "Left rumble motor" : "Right rumble motor";
    }

    @Override
    public StimulationDevice getDevice() {
        return device;
    }

    @Override
    public ChannelDependency channelDependency() {
        return channelDependency;
    }

    @Override
    public Output output() {
        return output;
    }

    @Override
    public double minimalSignalDuration() {
        return output == Output.Vibration ? 0.15 : 0.02;
    }

    @Override
    public void play(WaveForm waveform, double durationSeconds, double strength) {
        Callable<Void> renderWaveform = () -> {
            try {
                long startTime = System.currentTimeMillis();
                do {
                    for (WaveForm.Entry entry : waveform.values) {
                        set(entry.amplitude);
                        Thread.sleep(entry.durationMillis);
                    }
                } while (System.currentTimeMillis() - startTime < playTime.get());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                set(0);
                current.set(Optional.empty());
            }
            return null;
        };

        synchronized (player) {
            stop();
            playTime.set((long) (durationSeconds * 1000));
            current.set(Optional.of(player.submit(renderWaveform)));
        }
    }

    @Override
    public void extend(double durationSeconds) {
        synchronized (player) {
            if (current != null) {
                playTime.addAndGet((long) durationSeconds * 100);
            }
        }
    }

    @Override
    public void stop() {
        synchronized (player) {
            Optional<Future<Void>> optional = current.get();
            if (optional.isPresent()) {
                if (!optional.get().isDone()) {
                    optional.get().cancel(true);
                }
            }
        }
    }

    @Override
    public void complete() {
        synchronized (player) {
            Optional<Future<Void>> optional = current.get();
            if (optional.isPresent()) {
                try {
                    optional.get().get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ScriptInterruptedException();
                } catch (ExecutionException e) {
                    // Ignore
                }
            }
        }
    }
}
