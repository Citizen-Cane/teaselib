package teaselib.core.media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BinaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;
import teaselib.Mood;
import teaselib.Replay;
import teaselib.Replay.Position;
import teaselib.core.AbstractImages;
import teaselib.core.Closeable;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptEventArgs.BeforeNewMessage.OutlineType;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.util.ExceptionUtil;
import teaselib.util.AnnotatedImage;

public class SectionRenderer implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(SectionRenderer.class);

    private final TeaseLib teaseLib;
    private final MediaRendererQueue renderQueue;
    // TODO Handle message decorator processing here in order to make textToSpeechPlayer private
    public final TextToSpeechPlayer textToSpeechPlayer;

    // TODO workaround to tell section renderer to add the proper delay for the next message
    public OutlineType nextOutlineType;
    private MessageRenderer currentMessageRenderer;
    private MediaRenderer.Threaded currentRenderer = MediaRenderer.None;
    private RenderSound backgroundSoundRenderer;

    public SectionRenderer(TeaseLib teaseLib, MediaRendererQueue renderQueue) {
        this.teaseLib = teaseLib;
        this.renderQueue = renderQueue;
        this.textToSpeechPlayer = new TextToSpeechPlayer(teaseLib.config);
    }

    @Override
    public void close() {
        if (textToSpeechPlayer != null) {
            textToSpeechPlayer.close();
        }
    }

    public MediaRenderer.Threaded say(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
        return createBatch(actor, messages, say, resources);
    }

    public MediaRenderer.Threaded append(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
        return createBatch(actor, messages, append, resources);
    }

    public MediaRenderer.Threaded replace(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
        return createBatch(actor, messages, replace, resources);
    }

    public MediaRenderer.Threaded showAll(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
        return createBatch(actor, messages, showAll, resources);
    }

    private final class Batch extends MessageRenderer {
        private Batch(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
            super(SectionRenderer.this.teaseLib, actor, resources, messages);
        }

        @Override
        protected void renderMedia() throws InterruptedException, IOException {
            try {
                boolean emptyMessage = messages.get(0).isEmpty();
                if (emptyMessage) {
                    show(this, Mood.Neutral);
                    startCompleted();
                } else {
                    play();
                }
                awaitSectionMandatory();
                mandatoryCompleted();
                finalizeRendering(this);
            } catch (InterruptedException | ScriptInterruptedException e) {
                renderQueue.cancel(currentRenderer);
                currentRenderer = MediaRenderer.None;
                if (backgroundSoundRenderer != null) {
                    renderQueue.cancel(backgroundSoundRenderer);
                    backgroundSoundRenderer = null;
                }
                throw e;
            }
        }

        private void play() throws IOException, InterruptedException {
            if (position == Position.FromStart) {
                renderAllMessages();
            } else if (position == Position.FromCurrentPosition) {
                if (currentMessage < messages.size()) {
                    renderFromCurrentMessage();
                } else {
                    showAll(this);
                }
            } else if (position == Position.FromLastParagraph) {
                // say the last paragraph again, including the delay, showAll
                renderFrom(lastTextMessage());
                showAll(this);
            } else if (position == Position.FromMandatory) {
                // Render the last paragraph, includes final delay, then showAll
                render(lastParagraph);
                showAll(this);
            } else if (position == Position.End) {
                // Just show the summary
                showAll(this);
            } else {
                throw new IllegalStateException(position.toString());
            }
        }

        private void renderAllMessages() throws IOException, InterruptedException {
            accumulatedText = new MessageTextAccumulator();
            currentMessage = 0;
            render(messages);
        }

        private void renderFromCurrentMessage() throws IOException, InterruptedException {
            render(messages.subList(currentMessage, messages.size()));
        }

        private void renderFrom(int messageIndex) throws IOException, InterruptedException {
            accumulatedText = new MessageTextAccumulator();
            accumulatedText.addAll(messages.subList(0, messageIndex));
            currentMessage = messageIndex;
            render(messages.subList(messageIndex, messages.size()));
        }

        int lastTextMessage() {
            int i;
            for (i = messages.size() - 1; i >= 0; i--) {
                if (messages.get(i).stream().anyMatch(part -> part.type == Type.Text)) {
                    return i;
                }
            }
            return i;
        }

        private void render(List<RenderedMessage> messages) throws IOException, InterruptedException {
            for (var message : messages) {
                currentMessage = this.messages.indexOf(message);
                try {
                    render(message);
                } finally {
                    currentMessage++;
                }
                renderOptionalDefaultDelayBetweenMultipleMessages();
            }
        }

        private void renderOptionalDefaultDelayBetweenMultipleMessages() {
            if (textToSpeechPlayer != null) {
                boolean last = currentMessage == messages.size() && nextOutlineType != OutlineType.AppendParagraph;
                if (!last && !lastParagraph.contains(Type.Delay)) {
                    renderTimeSpannedPart(delay(ScriptMessageDecorator.DELAY_BETWEEN_PARAGRAPHS_SECONDS));
                }
            }
        }

        private void render(RenderedMessage message) throws IOException, InterruptedException {
            String mood = Mood.Neutral;
            for (Iterator<MessagePart> it = message.iterator(); it.hasNext();) {
                MessagePart part = it.next();
                if (!ManuallyLoggedMessageTypes.contains(part.type)) {
                    teaseLib.transcript.info("" + part.type.name() + " = " + part.value);
                }
                logger.info("{}={}", part.type, part.value);

                if (part.type == Message.Type.Mood) {
                    mood = part.value;
                } else {
                    render(part, mood);
                }

                boolean isDurationType = part.isAnyOf(Message.Type.DelayTypes);
                boolean isDisplayTypeAtEnd = !it.hasNext() && part.isAnyOf(Message.Type.DisplayTypes);

                if (isDurationType || isDisplayTypeAtEnd) {
                    if (!hasCompletedStart()) {
                        startCompleted();
                    }
                }

                if (isDisplayTypeAtEnd) {
                    awaitSectionMandatory();
                }

                if (isDurationType || isDisplayTypeAtEnd) {
                    show(this, mood);
                }

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }

            if (!hasCompletedStart()) {
                throw new IllegalStateException();
            }
        }

        private void render(MessagePart part, String mood) throws IOException, InterruptedException {
            if (part.type == Message.Type.Image) {
                displayImage = part.value;
            } else if (part.type == Message.Type.BackgroundSound) {
                playSoundAsynchronous(part, resources);
            } else if (part.type == Message.Type.Sound) {
                playSound(part, resources);
            } else if (part.type == Message.Type.Speech) {
                playSpeech(actor, part, mood, resources);
            } else if (part.type == Message.Type.DesktopItem) {
                if (isInstructionalImageOutputEnabled()) {
                    try {
                        showDesktopItem(part, resources);
                    } catch (IOException e) {
                        showDesktopItemError(this, accumulatedText, mood, e);
                        throw e;
                    }
                }
            } else if (part.type == Message.Type.Keyword) {
                doKeyword(this, part);
            } else if (part.type == Message.Type.Delay) {
                doDelay(part);
            } else if (part.type == Message.Type.Item) {
                accumulatedText.add(part);
            } else if (part.type == Message.Type.Text) {
                accumulatedText.add(part);
            } else {
                throw new UnsupportedOperationException(part.type + "=" + part.value);
            }
        }

    }

    // TODO make sure batch renderers don't overlap to avoid race conditions with SectionRenderer fields

    Batch createBatch(Actor actor, List<RenderedMessage> messages, BinaryOperator<MessageRenderer> operator,
            ResourceLoader resources) {
        var next = new Batch(actor, messages, resources);
        applyOperator(currentMessageRenderer, next, operator);
        currentMessageRenderer = next;
        return next;
    }

    static MessageRenderer applyOperator(MessageRenderer currentMessageRenderer, MessageRenderer nextMessageRenderer,
            BinaryOperator<MessageRenderer> operator) {
        return currentMessageRenderer == null ? nextMessageRenderer
                : operator.apply(currentMessageRenderer, nextMessageRenderer);
    }

    static BinaryOperator<MessageRenderer> say = (current, next) -> {
        next.previousLastParagraph = next.lastParagraph;
        return next;
    };

    static BinaryOperator<MessageRenderer> append = (current, next) -> {
        next.previousLastParagraph = current.lastParagraph;
        prepend(current.messages, next);
        next.position = Replay.Position.FromCurrentPosition;
        next.currentMessage = current.messages.size();
        return next;
    };

    static BinaryOperator<MessageRenderer> replace = (current, next) -> {
        next.previousLastParagraph = current.lastParagraph;
        List<RenderedMessage> messages = new ArrayList<>(current.messages);
        messages.remove(messages.size() - 1);
        prepend(messages, next);
        next.position = Replay.Position.FromCurrentPosition;
        next.currentMessage = next.messages.size() - 1;
        return next;
    };

    static BinaryOperator<MessageRenderer> showAll = (current, next) -> {
        next.previousLastParagraph = current.lastParagraph;
        prepend(current.messages, next);
        next.position = Replay.Position.FromMandatory;
        next.currentMessage = next.messages.size();
        return next;
    };

    private static void prepend(List<RenderedMessage> messages, MessageRenderer next) {
        messages.forEach(m -> m.forEach(next.accumulatedText::add));
        next.messages.addAll(0, messages);
    }

    private void finalizeRendering(MessageRenderer messageRenderer) {
        awaitSectionMandatory();
        if (nextOutlineType == OutlineType.NewSection && textToSpeechPlayer != null
                && !messageRenderer.lastParagraph.contains(Type.Delay)) {
            renderSectionEndDelay();
        }
        awaitSectionAll();
    }

    private void renderSectionEndDelay() {
        awaitSectionMandatory();
        renderTimeSpannedPart(delay(ScriptMessageDecorator.DELAY_BETWEEN_SECTIONS_SECONDS));
    }

    private void renderTimeSpannedPart(MediaRenderer.Threaded renderer) {
        awaitSectionMandatory();
        renderQueue.submit(renderer);
        this.currentRenderer = renderer;
    }

    private void showDesktopItem(MessagePart part, ResourceLoader resources) throws IOException {
        var renderDesktopItem = new RenderDesktopItem(teaseLib, resources, part.value);
        awaitSectionAll();
        renderQueue.submit(renderDesktopItem);
    }

    private void showDesktopItemError(MessageRenderer messageRenderer, MessageTextAccumulator accumulatedText,
            String mood, IOException e) throws IOException, InterruptedException {
        accumulatedText.add(new MessagePart(Message.Type.Text, e.getMessage()));
        awaitSectionAll();
        show(messageRenderer, mood);
        messageRenderer.startCompleted();
    }

    private void playSpeech(Actor actor, MessagePart part, String mood, ResourceLoader resources) throws IOException {
        if (Message.Type.isSound(part.value)) {
            renderTimeSpannedPart(new RenderPrerecordedSpeech(part.value, resources, teaseLib));
        } else if (TextToSpeechPlayer.isSimulatedSpeech(part.value)) {
            renderTimeSpannedPart(
                    new RenderSpeechDelay(TextToSpeechPlayer.getSimulatedSpeechText(part.value), teaseLib));
        } else if (isSpeechOutputEnabled() && textToSpeechPlayer != null) {
            renderTimeSpannedPart(new RenderTTSSpeech(textToSpeechPlayer, actor, part.value, mood, teaseLib));
        } else {
            renderTimeSpannedPart(new RenderSpeechDelay(part.value, teaseLib));
        }
    }

    private void playSound(MessagePart part, ResourceLoader resources) throws IOException {
        if (isSoundOutputEnabled()) {
            renderTimeSpannedPart(new RenderSound(resources, part.value, teaseLib));
        }
    }

    private void playSoundAsynchronous(MessagePart part, ResourceLoader resources) throws IOException {
        if (isSoundOutputEnabled()) {
            awaitSectionMandatory();
            if (backgroundSoundRenderer != null) {
                renderQueue.cancel(backgroundSoundRenderer);
            }
            backgroundSoundRenderer = new RenderSound(resources, part.value, teaseLib);
            renderQueue.submit(backgroundSoundRenderer);
            // use awaitSoundCompletion keyword to wait for background sound completion
        }
    }

    private void awaitSectionMandatory() {
        currentRenderer.awaitMandatoryCompleted();
    }

    private void awaitSectionAll() {
        currentRenderer.awaitAllCompleted();
        currentRenderer = MediaRenderer.None;
    }

    private void show(MessageRenderer messageRenderer, String mood) throws IOException, InterruptedException {
        show(messageRenderer.actor, messageRenderer.displayImage, messageRenderer.accumulatedText.getTail(), mood);
    }

    private void show(Actor actor, String displayImage, String text, String mood)
            throws IOException, InterruptedException {
        var transcript = new StringBuilder();
        if (actor.images.contains(displayImage)) {
            teaseLib.transcript.debug("image = '" + displayImage + "'");
            if (!Mood.Neutral.equalsIgnoreCase(mood)) {
                transcript.append(mood);
                transcript.append(" ");
            }
        } else if (!Message.NoImage.equalsIgnoreCase(displayImage)) {
            teaseLib.transcript.info("image = '" + displayImage + "'");
        }

        if (text != null && !text.isBlank()) {
            transcript.append(text);
        }
        teaseLib.transcript.info(transcript.toString());

        show(actor, displayImage, Collections.singletonList(text));
    }

    private void showAll(MessageRenderer message) throws IOException, InterruptedException {
        show(message.actor, message.displayImage, message.accumulatedText.paragraphs);
    }

    private void show(Actor actor, String displayImage, List<String> paragraphs)
            throws IOException, InterruptedException {
        if (!Thread.currentThread().isInterrupted()) {
            teaseLib.host.show(annotatedImage(actor, displayImage), paragraphs);
            teaseLib.host.show();
        }
    }

    private AnnotatedImage annotatedImage(Actor actor, String displayImage) throws IOException, InterruptedException {
        if (displayImage != null && !Message.NoImage.equals(displayImage)) {
            try {
                return actor.images.annotated(displayImage);
            } catch (IOException e) {
                handleIOException(e);
                return AnnotatedImage.NoImage;
            } finally {
                if (actor.images instanceof AbstractImages) {
                    ((AbstractImages) actor.images).prefetcher().fetch();
                }
            }
        } else {
            return AnnotatedImage.NoImage;
        }
    }

    protected void handleIOException(IOException e) throws IOException {
        ExceptionUtil.handleIOException(e, teaseLib.config, logger);
    }

    private void doKeyword(MessageRenderer messageRenderer, MessagePart part) {
        String keyword = part.value;
        if (Message.ActorImage.equalsIgnoreCase(keyword)) {
            throw new IllegalStateException(keyword + " must be resolved in pre-parse");
        } else if (Message.NoImage.equalsIgnoreCase(keyword)) {
            throw new IllegalStateException(keyword + " must be resolved in pre-parse");
        } else if (Message.ShowChoices.equalsIgnoreCase(keyword)) {
            awaitSectionMandatory();
            messageRenderer.startCompleted();
            messageRenderer.mandatoryCompleted();
        } else if (Message.AwaitSoundCompletion.equalsIgnoreCase(keyword)) {
            if (backgroundSoundRenderer != null) {
                backgroundSoundRenderer.awaitAllCompleted();
                backgroundSoundRenderer = null;
            }
        } else {
            throw new UnsupportedOperationException(keyword);
        }
    }

    private void doDelay(MessagePart part) {
        String args = part.value;
        if (args.isEmpty()) {
            awaitSectionAll();
        } else {
            double delay = geteDelaySeconds(part.value);
            if (delay > 0) {
                renderTimeSpannedPart(delay(delay));
            }
        }
    }

    private RenderDelay delay(double seconds) {
        boolean logToTranscript = seconds > ScriptMessageDecorator.DELAY_BETWEEN_SECTIONS_SECONDS;
        return new RenderDelay(seconds, logToTranscript, teaseLib);
    }

    private double geteDelaySeconds(String args) {
        double[] argv = getDelayInterval(args);
        if (argv.length == 1) {
            return argv[0];
        } else {
            return teaseLib.random.value(argv[0], argv[1]);
        }
    }

    // TODO Utilities -> Interval
    public static double[] getDelayInterval(String delayInterval) {
        String[] argv = delayInterval.split(" ");
        if (argv.length == 1) {
            return new double[] { Double.parseDouble(delayInterval) };
        } else {
            return new double[] { Double.parseDouble(argv[0]), Double.parseDouble(argv[1]) };
        }
    }

    private boolean isSpeechOutputEnabled() {
        return Boolean.parseBoolean(teaseLib.config.get(Config.Render.Speech));
    }

    private boolean isSoundOutputEnabled() {
        return Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound));
    }

    private boolean isInstructionalImageOutputEnabled() {
        return Boolean.parseBoolean(teaseLib.config.get(Config.Render.InstructionalImages));
    }

    public RenderedMessage lastParagraph() {
        return currentMessageRenderer != null ? currentMessageRenderer.lastParagraph : null;
    }

    public boolean showsMultipleParagraphs() {
        return currentMessageRenderer != null ? currentMessageRenderer.accumulatedText.paragraphs.size() > 1 : false;
    }
}
