package teaselib.core.devices.xinput.stimulation;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
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
    private final ExecutorService executor = NamedExecutorService.singleThreadedQueue(getClass().getName());
    private final BlockingQueue<StimulationTargets> playList = new SynchronousQueue<>();
    private final AtomicReference<StimulationTargets> playing = new AtomicReference<>(null);
    private final Lock lock = new ReentrantLock();
    private final Condition playNext = lock.newCondition();

    Future<?> future;
    long startTimeMillis;
    Samples samples;

    public StimulationSamplerTask() {
        submit();
    }

    void play(StimulationTargets newTargets) {
        if (newTargets == null) {
            throw new NullPointerException();
        }

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

    /**
     * Play new targets after the current has been finished.
     * 
     * @param newTargets
     * @param now
     */
    private void play(StimulationTargets newTargets, long now) {
        try {
            playList.put(newTargets);
            startTimeMillis = now;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
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

    private void handleExceptions() {
        try {
            if (!future.isCancelled() && future.isDone()) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
        }
    }

    public void append(StimulationTargets newTargets) {
        try {
            cancelCurrentlyPlaying();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
        play(newTargets);
    }

    private void cancelCurrentlyPlaying() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            playNext.signal();
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        try {
            if (future != null && !future.isCancelled() && !future.isDone()) {
                future.cancel(true);
            }
            handleExceptions();
        } finally {
            playing.set(null);
        }
    }

    public void complete() {
        try {
            playList.put(StimulationTargets.None);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
        handleExceptions();
    }

    void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                StimulationTargets currentTargets = playList.take();
                lock.lockInterruptibly();
                try {
                    if (currentTargets == StimulationTargets.None) {
                        clearSamples();
                    } else {
                        renderSamples(currentTargets);
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            clearSamples();
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
            } else if (durationMillis == 0) {
                // TODO Investigate 0 durations - should be skipped, but should also not be submitted by iterator
                continue;
            } else if (playNext.await(durationMillis, TimeUnit.MILLISECONDS)) {
                break;
            }
        }
    }

    abstract void playSamples(Samples samples);

    abstract void clearSamples();
}
