package teaselib.core.devices.xinput.stimulation;

import java.util.Date;
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

    public StimulationSamplerTask() {
        runSampleThread();
    }

    public void play(StimulationTargets targets) {
        Objects.requireNonNull(targets);

        StimulationTargets previous = playing.get();
        if (previous != null) {
            prepareToPlayNext();
            long now = System.currentTimeMillis();
            queue(previous.continuedStimulation(targets, now - startTimeMillis));
        } else {
            queue(targets);
        }
    }

    public void playAll(StimulationTargets targets) {
        Objects.requireNonNull(targets);

        prepareToPlayNext();
        queue(targets);
    }

    public void append(StimulationTargets targets) {
        Objects.requireNonNull(targets);

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
     * @param targets
     * @param now
     */
    private void queue(StimulationTargets targets) {
        ensureSampleThreadIsRunning();
        try {
            offer(targets);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    private void ensureSampleThreadIsRunning() {
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
        if (future != null && !future.isCancelled() && !future.isDone()) {
            future.cancel(true);
        } else {
            convertExecutionToRuntimeException();
        }
    }

    public void complete() {
        try {
            while (future != null && !future.isCancelled() && !future.isDone()) {
                if (offer(StimulationTargets.None, 1, TimeUnit.SECONDS))
                    break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
        handleExceptions();
    }

    private void offer(StimulationTargets targets) throws InterruptedException {
        while (!offer(targets, 1, TimeUnit.SECONDS)) {
            handleExceptions();
        }
    }

    private boolean offer(StimulationTargets targets, long duration, TimeUnit unit) throws InterruptedException {
        return playList.offer(targets, duration, unit);
    }

    private void handleExceptions() {
        if (!future.isCancelled() && future.isDone()) {
            convertExecutionToRuntimeException();
        }
    }

    private void convertExecutionToRuntimeException() {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
        }
    }

    // TODO Thread ends when error occurs - non-blocking queue, wait on lock to wait for next batch, submit future again
    private void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                playing.set(playList.take());
                lock.lockInterruptibly();
                startTimeMillis = System.currentTimeMillis();
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
        Date date = new Date();
        while (iterator.hasNext()) {
            Samples samples = iterator.next();
            playSamples(samples);

            long durationMillis = samples.getDurationMillis();
            long timeStampMillis = samples.getTimeStampMillis();
            date.setTime(startTimeMillis + timeStampMillis + durationMillis);
            if (durationMillis == Long.MAX_VALUE && !iterator.hasNext() || playNext.awaitUntil(date)) {
                // skip infinite sample duration at the end of the waveform
                // - stimulation output is set to 0 automatically when the task is done
                // -> this allows for continuous appending of additional stimulations
                // TODO waveform duration is finite, so the infinite delay is irregular -> remove
                break;
            }
        }
    }

    abstract void playSamples(Samples samples);

    abstract void clearSamples();
}
