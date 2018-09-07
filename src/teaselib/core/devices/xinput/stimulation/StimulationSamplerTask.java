package teaselib.core.devices.xinput.stimulation;

import java.util.Iterator;
import java.util.Objects;
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

    private Future<?> future;
    private long startTimeMillis;
    private Samples samples;

    public StimulationSamplerTask() {
        runSampleThread();
    }

    public void play(StimulationTargets targets) {
        Objects.requireNonNull(targets);

        startSampleThread();
        long now = System.currentTimeMillis();
        StimulationTargets previous = playing.get();
        if (previous != null) {
            // TODO continued playing will be slightly off from actual time duration because
            // the sampler ignores execution time between await() calls
            prepareToPlayNext();
            queue(previous.continuedStimulation(targets, now - startTimeMillis), now);
        } else {
            queue(targets, now);
        }
    }

    public void playAll(StimulationTargets targets) {
        prepareToPlayNext();
        queue(targets);
    }

    public void append(StimulationTargets targets) {
        queue(targets);
    }

    private void prepareToPlayNext() {
        lock.lock();
        try {
            playNext.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Play new targets after the current has been finished.
     * 
     * @param newTargets
     * @param now
     */
    private void queue(StimulationTargets newTargets) {
        try {
            playList.put(newTargets);
            startTimeMillis = System.currentTimeMillis();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    /**
     * Play new targets after the current has been finished.
     * 
     * @param newTargets
     * @param now
     */
    private void queue(StimulationTargets newTargets, long now) {
        try {
            playList.put(newTargets);
            startTimeMillis = now;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    private void startSampleThread() {
        if (future == null) {
            runSampleThread();
        } else if (future.isCancelled()) {
            runSampleThread();
        } else if (future.isDone()) {
            handleExceptions();
            runSampleThread();
        }
    }

    private void runSampleThread() {
        this.future = this.executor.submit(this::run);
    }

    public void stop() {
        try {
            if (future != null && !future.isCancelled() && !future.isDone()) {
                future.cancel(true);
            }
            handleExceptions();
        } finally {
            // race condition If cancel() returns before current set starts
            // TODO handleExceptions() does not wait for future to complete
            // playing.set(null);
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

    private void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                playing.set(playList.take());
                lock.lockInterruptibly();
                try {
                    if (playing.get() == StimulationTargets.None) {
                        clearSamples();
                    } else {
                        renderSamples(playing.get());
                    }
                } finally {
                    playing.set(null);
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            clearSamples();
        }
    }

    private void renderSamples(StimulationTargets targets) throws InterruptedException {
        Iterator<Samples> iterator = targets.iterator();
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
