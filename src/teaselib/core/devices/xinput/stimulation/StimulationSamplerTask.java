package teaselib.core.devices.xinput.stimulation;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.util.ExceptionUtil;
import teaselib.stimulation.ext.StimulationTargets;
import teaselib.stimulation.ext.StimulationTargets.Samples;

public abstract class StimulationSamplerTask {
    final Lock lock = new ReentrantLock();
    final Condition playNext = lock.newCondition();
    final Condition taken = lock.newCondition();

    private final ExecutorService executor = NamedExecutorService.singleThreadedQueue(getClass().getName());

    final BlockingQueue<StimulationTargets> targets = new ArrayBlockingQueue<>(1);
    final AtomicReference<StimulationTargets> playing = new AtomicReference<>(null);
    Future<?> future;
    long startTimeMillis;
    Samples samples;

    public StimulationSamplerTask() {
        submit();
    }

    void play(StimulationTargets newTargets) {
        start();

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

    private void start() {
        if (future == null) {
            submit();
        } else if (future.isCancelled()) {
            submit();
        } else if (future.isDone()) {
            handleExceptions();
            submit();
        }
    }

    private void submit() {
        this.future = this.executor.submit(this::run);
    }

    void handleExceptions() {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (CancellationException e) {
            // Ignore
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
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

    public void stop() {
        if (future != null && !future.isCancelled() && !future.isDone()) {
            future.cancel(true);
        }
        handleExceptions();
    }

    public void complete() {
        handleExceptions();
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

                    if (currentTargets == null) {
                        break;
                    }

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

            if (durationMillis == Long.MAX_VALUE && !iterator.hasNext()) {
                // skip infinite sample duration at the end of the waveform
                // - stimulation output is set to 0 automatically when the task is done
                // -> this allows for continuous appending of additional stimulations
                // TODO waveform duration is finite, so the infinite delay is irregular -> remove
                break;
            } else if (playNext.await(durationMillis, TimeUnit.MILLISECONDS)) {
                break;
            }
        }
    }

    abstract void playSamples(Samples samples);
}