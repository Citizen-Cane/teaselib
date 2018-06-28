package teaselib.core.devices.xinput.stimulation;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.ScriptInterruptedException;
import teaselib.stimulation.ext.StimulationTargets;
import teaselib.stimulation.ext.StimulationTargets.Samples;

public abstract class StimulationSamplerTask {
    final Lock lock = new ReentrantLock();
    final Condition playNext = lock.newCondition();
    final Condition taken = lock.newCondition();

    final BlockingQueue<StimulationTargets> targets = new ArrayBlockingQueue<>(1);
    final AtomicReference<StimulationTargets> playing = new AtomicReference<>(null);
    final Future<?> future;
    long startTimeMillis;
    Samples samples;
    private final Executor executor;

    public StimulationSamplerTask(ExecutorService executor) {
        this.executor = executor;
        this.future = executor.submit(this::run);
    }

    void play(StimulationTargets newTargets) {
        long now = System.currentTimeMillis();
        StimulationTargets previous = playing.get();
        if (previous != null) {
            // TODO continued playing will be slightly off from actual time duration because
            // the sampler ignores execution time between await() calls
            play(previous.continuedStimulation(newTargets, now - startTimeMillis), now);
        } else {
            play(newTargets, now);
        }
    }

    private void play(StimulationTargets newTargets, long now) {
        try {
            lock.lockInterruptibly();
            try {
                playNext.signal();
                targets.put(newTargets);
                while (!targets.isEmpty()) {
                    taken.await();
                }
            } finally {
                lock.unlock();
            }
            startTimeMillis = now;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    public void append(StimulationTargets newTargets) {
        try {
            lock.lockInterruptibly();
            try {
                targets.put(newTargets);
                while (!targets.isEmpty()) {
                    taken.await();
                }
            } finally {
                lock.unlock();
            }
            startTimeMillis = System.currentTimeMillis();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                lock.lockInterruptibly();
                try {
                    if (playing.get() == null) {
                        while (targets.peek() == null) {
                            playNext.await();
                        }
                    }
                    StimulationTargets currentTargets = targets.poll();
                    taken.signal();

                    // breaking is inappropriate and complicates synchronizing
                    // synchronizing on executor doesn't work
                    // -> wait for currentTargets != null, but only end task when interrupted
                    // synchronized (executor) {
                    if (currentTargets == null) {
                        break;
                    }
                    // }
                    renderSamples(currentTargets);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            playSamples(playing.get().zero());
        }
    }

    private void renderSamples(StimulationTargets currentTargets) throws InterruptedException {
        playing.set(currentTargets);
        Iterator<Samples> iterator = currentTargets.iterator();
        while (iterator.hasNext()) {
            samples = iterator.next();
            playSamples(samples);
            long durationMillis = samples.getDurationMillis();
            if (playNext.await(durationMillis, TimeUnit.MILLISECONDS)) {
                break;
            }
        }
    }

    abstract void playSamples(Samples samples);
}