package teaselib.core.media;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.function.BinaryOperator;

import teaselib.Actor;
import teaselib.Replay;
import teaselib.core.AbstractMessage;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptInterruptedException;

public abstract class MessageRenderer implements Runnable {
    final Actor actor;
    final List<RenderedMessage> messages;
    final ResourceLoader resources;
    final BinaryOperator<MessageRenderer> operator;

    Future<Void> thisTask = null;
    RendererFacade renderer = new RendererFacade();
    Replay.Position position = Replay.Position.FromCurrentPosition;
    MessageTextAccumulator accumulatedText = new MessageTextAccumulator();

    int currentMessage = 0;
    String displayImage = null;
    AbstractMessage lastSection;

    public MessageRenderer(Actor actor, List<RenderedMessage> messages, BinaryOperator<MessageRenderer> operator,
            ResourceLoader resources) {
        this.actor = actor;
        this.resources = resources;
        this.messages = messages;
        this.operator = operator;

        this.lastSection = RenderedMessage.getLastSection(getLastMessage());
    }

    private RenderedMessage getLastMessage() {
        return getLastMessage(this.messages);
    }

    private static RenderedMessage getLastMessage(List<RenderedMessage> messages) {
        return messages.get(messages.size() - 1);
    }

    RenderedMessage getMandatory() {
        return RenderedMessage.getLastSection(getLastMessage());
    }

    RenderedMessage getEnd() {
        return stripAudio(RenderedMessage.getLastSection(getLastMessage()));
    }

    private static RenderedMessage stripAudio(AbstractMessage message) {
        return message.stream().filter(part -> !SectionRenderer.SoundTypes.contains(part.type))
                .collect(RenderedMessage.collector());
    }

    // TODO Contains duplicated code from ThreadedMediaRenderer
    class RendererFacade implements MediaRenderer.Threaded {
        final CountDownLatch completedStart = new CountDownLatch(1);
        final CountDownLatch completedMandatory = new CountDownLatch(1);;
        final CountDownLatch completedAll = new CountDownLatch(1);;

        private long startMillis = 0;

        @Override
        public void run() {
            startMillis = System.currentTimeMillis();
            MessageRenderer.this.run();
        }

        @Override
        public void completeStart() {
            try {
                // TODO Blocks in PCM tests because the message renderer task is cancelled,
                // before startCompleted is reached - executor is idle
                completedStart.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        }

        @Override
        public void completeMandatory() {
            try {
                completedMandatory.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        }

        @Override
        public void completeAll() {
            try {
                completedAll.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        }

        protected final void startCompleted() {
            completedStart.countDown();
            if (SectionRenderer.logger.isDebugEnabled()) {
                SectionRenderer.logger.debug(getClass().getSimpleName() + " completed start after "
                        + String.format("%.2f seconds", getElapsedSeconds()));
            }
        }

        protected final void mandatoryCompleted() {
            completedMandatory.countDown();
            if (SectionRenderer.logger.isDebugEnabled()) {
                SectionRenderer.logger.debug("{} completed mandatory after {}", getClass().getSimpleName(),
                        String.format("%.2f seconds", getElapsedSeconds()));
            }
        }

        protected final void allCompleted() {
            completedAll.countDown();
            if (SectionRenderer.logger.isDebugEnabled()) {
                SectionRenderer.logger.debug("{} completed all after {}", getClass().getSimpleName(),
                        getElapsedSecondsFormatted());
            }
        }

        @Override
        public boolean hasCompletedStart() {
            return completedStart.getCount() == 0;
        }

        @Override
        public boolean hasCompletedMandatory() {
            return completedMandatory.getCount() == 0;
        }

        @Override
        public boolean hasCompletedAll() {
            return completedAll.getCount() == 0;
        }

        public String getElapsedSecondsFormatted() {
            return String.format("%.2f", getElapsedSeconds());
        }

        private double getElapsedSeconds() {
            return (System.currentTimeMillis() - startMillis) / 1000.0;
        }

        public Future<Void> getTask() {
            return MessageRenderer.this.thisTask;
        }
    }

}