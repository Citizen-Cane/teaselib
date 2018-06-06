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

        public void setLeftMotor(int strength) {
            leftMotor = strength;
            device.setVibration(leftMotor, rightMotor);
        }

        public void setRightMotor(int strength) {
            rightMotor = strength;
            device.setVibration(leftMotor, rightMotor);
        }

        public void setBothMotors(int strength) {
            leftMotor = strength;
            rightMotor = strength;
            device.setVibration(leftMotor, rightMotor);
        }
    }

    final XInputStimulationDevice device;
    final SharedState sharedState;
    final int channel;

    private final ExecutorService player = NamedExecutorService.singleThreadedQueue(getClass().getName());
    private final AtomicReference<Optional<Future<Void>>> current = new AtomicReference<>(Optional.empty());
    private final AtomicLong playTime = new AtomicLong(0);

    /**
     * Get all stimulators for a device.
     * 
     * @param device
     *            An XInput game controller
     * @return The stimulators share the device, because the outputs can only be set simultaneously
     */
    public static List<XInputStimulator> getStimulators(XInputStimulationDevice device) {
        List<XInputStimulator> stimulators = new ArrayList<>(3);
        XInputStimulator channel0 = new XInputStimulator(device, 0);
        stimulators.add(channel0);
        XInputStimulator channel1 = new XInputStimulator(device, 1);
        stimulators.add(channel1);
        XInputStimulator channel2 = new XInputStimulator(device, 2);
        stimulators.add(channel2);
        return stimulators;
    }

    XInputStimulator(XInputStimulationDevice device, int channel) {
        if (channel < 0 || channel > 2)
            throw new IllegalArgumentException(Integer.toString(channel));

        this.device = device;
        this.sharedState = new SharedState(device.getXInputDevice());
        this.channel = channel;
    }

    XInputStimulator(XInputStimulator sibling, int channel) {
        this.device = sibling.device;
        this.sharedState = sibling.sharedState;
        this.channel = channel;
    }

    void set(double value) {
        int strength = device.vibrationValue(value);
        if (channel == 0) {
            sharedState.setLeftMotor(strength);
        } else if (channel == 1) {
            sharedState.setRightMotor(strength);
        } else if (channel == 2) {
            sharedState.setBothMotors(strength);
        } else {
            throw new IllegalArgumentException(Integer.toString(channel));
        }
    }

    @Override
    public String getName() {
        return device.getName() + " " + getLocation();
    }

    private String getLocation() {
        if (device.output == Output.EStim) {
            return channel == 0 ? "Left channel" : "Right channel";
        } else {
            return channel == 0 ? "Left rumble motor" : "Right rumble motor";
        }
    }

    @Override
    public StimulationDevice getDevice() {
        return device;
    }

    @Override
    public ChannelDependency channelDependency() {
        return device.wiring == Wiring.INFERENCE_CHANNEL ? ChannelDependency.Dependent : ChannelDependency.Independent;
    }

    @Override
    public Output output() {
        return device.output;
    }

    @Override
    public Signal signal() {
        return device.output == Output.EStim ? Signal.Discrete : Signal.Continous;
    }

    @Override
    public double minimalSignalDuration() {
        return output() == Output.Vibration ? 0.15 : 0.02;
    }

    @Override
    public void play(WaveForm waveform, double durationSeconds, double strength) {
        Callable<Void> renderWaveform = () -> {
            try {
                long startTime = System.currentTimeMillis();
                long now = startTime;
                do {
                    for (WaveForm.Sample sample : waveform) {
                        set(sample.getValue());
                        now = System.currentTimeMillis();
                        Thread.sleep(sample.getTimeStampMillis() - now);
                    }
                } while (now - startTime < playTime.get());

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
            if (current.get().isPresent()) {
                playTime.addAndGet((long) durationSeconds * 100);
            }
        }
    }

    @Override
    public void stop() {
        synchronized (player) {
            Optional<Future<Void>> currentPlayer = current.get();
            if (currentPlayer.isPresent() && !currentPlayer.get().isDone()) {
                currentPlayer.get().cancel(true);
            }
        }
    }

    @Override
    public void complete() {
        synchronized (player) {
            Optional<Future<Void>> currentPlayer = current.get();
            if (currentPlayer.isPresent()) {
                try {
                    currentPlayer.get().get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ScriptInterruptedException();
                } catch (ExecutionException e) {
                    // Ignore
                }
            }
        }
    }

    @Override
    public String toString() {
        return device.getDevicePath() + " " + getLocation();
    }
}
