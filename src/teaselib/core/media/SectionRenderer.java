package teaselib.core.media;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;
import teaselib.Mood;
import teaselib.Replay.Position;
import teaselib.core.Closeable;
import teaselib.core.ResourceLoader;
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

    private Batch currentMessageRenderer;
    private MediaRenderer.Threaded currentRenderer = MediaRenderer.None;
    private RenderSound backgroundSoundRenderer;

    public SectionRenderer(TeaseLib teaseLib, MediaRendererQueue renderQueue) {
        this.teaseLib = teaseLib;
        this.renderQueue = renderQueue;
        this.textToSpeechPlayer = new TextToSpeechPlayer(teaseLib.config);
        this.currentMessageRenderer = new Batch(null, Collections.emptyList(), null);
    }

    @Override
    public void close() {
        if (textToSpeechPlayer != null) {
            textToSpeechPlayer.close();
        }
    }

    public boolean append(List<RenderedMessage> messages) {
        synchronized (messages) {
            currentMessageRenderer.messages.addAll(messages);
            boolean stillRunning = !currentMessageRenderer.hasCompletedMandatory();
            return stillRunning;
        }
    }

    public boolean hasMultipleParagraphs() {
        return currentMessageRenderer.accumulatedText.paragraphs.size() > 1;
    }

    public void restoreCurrentRenderer(List<MediaRenderer> replay) {
        for (int i = 0; i < replay.size(); i++) {
            if (replay.get(i) instanceof MessageRenderer m) {
                if (m != currentMessageRenderer) {
                    replay.remove(i);
                    break;
                }
            }
        }
        if (!replay.contains(currentMessageRenderer)) {
            replay.add(currentMessageRenderer);
        }
    }

    final class Batch extends MessageRenderer {
        private Batch(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
            super(SectionRenderer.this.teaseLib, actor, resources, messages);
        }

        @Override
        protected void renderMedia() throws InterruptedException, IOException {
            try {
                boolean emptyMessage = messages.get(0).isEmpty();
                if (emptyMessage) {
                    show(AnnotatedImage.NoImage, "");
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
                }
            } else if (position == Position.FromLastParagraph) {
                // say the last message (likely a paragraph?) again, including the final delay, showAll
                renderFrom(lastTextMessage());
                // TODO FromLastParagraph & FromMandatory are very similar -> get rid of one of them
            } else if (position == Position.FromMandatory) {
                // Render the last paragraph, including the final delay, then showAll
                render(lastParagraph);
            } else if (position == Position.End) {
                // Just show the summary
                showAll();
            } else {
                throw new IllegalStateException(position.toString());
            }
        }

        private void showAll() throws IOException, InterruptedException {
            var image = getImage(actor, displayImage);
            show(image, accumulatedText.paragraphs);
        }

        private void renderAllMessages() throws IOException, InterruptedException {
            accumulatedText = new MessageTextAccumulator();
            currentMessage = 0;
            renderFromCurrentMessage();
        }

        private void renderFrom(int messageIndex) throws IOException, InterruptedException {
            accumulatedText = new MessageTextAccumulator();
            accumulatedText.addAll(messages.subList(0, messageIndex));
            currentMessage = messageIndex;
            renderFromCurrentMessage();
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

        private void renderFromCurrentMessage() throws IOException, InterruptedException {
            while (true) {
                int limit = messages.size();
                while (currentMessage < limit) {
                    RenderedMessage message = messages.get(currentMessage);
                    try {
                        render(message);
                    } finally {
                        currentMessage++;
                    }
                    renderOptionalDefaultDelayBetweenMultipleMessages();
                }

                synchronized (messages) {
                    int size = messages.size();
                    if (limit < size) {
                        mandatoryCompletedAndContinue();
                    } else {
                        break;
                    }
                }
            }
        }

        private void renderOptionalDefaultDelayBetweenMultipleMessages() throws InterruptedException {
            if (textToSpeechPlayer != null) {
                boolean last = currentMessage == messages.size();
                if (!last && !lastParagraph.contains(Type.Delay)) {
                    renderTimeSpannedPart(delay(ScriptMessageDecorator.DELAY_BETWEEN_PARAGRAPHS_SECONDS));
                }
            }
        }

        private void render(RenderedMessage message) throws IOException, InterruptedException {
            MessagePart previousPart = MessagePart.None;
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

                if ((isDurationType && !previousPart.isAnyOf(Message.Type.DelayTypes))
                        || isDisplayTypeAtEnd) {
                    show(getImage(actor, displayImage), accumulatedText.getTail());
                }

                if (part.type == Type.Text) {
                    logTextToTranscript(accumulatedText.getTail(), mood);
                }

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                previousPart = part;
            }

            if (!hasCompletedStart()) {
                throw new IllegalStateException();
            }
        }

        private void logTextToTranscript(String text, String mood) {
            var transcript = new StringBuilder();
            appendMood(mood, transcript);
            if (text != null && !text.isBlank()) {
                transcript.append(text);
            }
            teaseLib.transcript.info(transcript.toString());
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
                        showDesktopItemError(this, accumulatedText, e);
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

    public Batch createStartBatch(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
        Batch batch = new Batch(actor, messages, resources);
        currentMessageRenderer = batch;
        return batch;
    }

    public Batch createBatch(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
        return new Batch(actor, messages, resources);
    }

    private void finalizeRendering(MessageRenderer messageRenderer) throws InterruptedException {
        awaitSectionMandatory();
        if (textToSpeechPlayer != null && !messageRenderer.lastParagraph.contains(Type.Delay)) {
            renderSectionEndDelay();
        }
        awaitSectionAll();
    }

    private void renderSectionEndDelay() throws InterruptedException {
        awaitSectionMandatory();
        renderTimeSpannedPart(delay(ScriptMessageDecorator.DELAY_BETWEEN_SECTIONS_SECONDS));
    }

    private void renderTimeSpannedPart(MediaRenderer.Threaded renderer) throws InterruptedException {
        awaitSectionMandatory();
        renderQueue.submit(renderer);
        this.currentRenderer = renderer;
    }

    private void showDesktopItem(MessagePart part, ResourceLoader resources) throws IOException, InterruptedException {
        var renderDesktopItem = new RenderDesktopItem(teaseLib, resources, part.value);
        awaitSectionAll();
        renderQueue.submit(renderDesktopItem);
    }

    private void showDesktopItemError(MessageRenderer messageRenderer, MessageTextAccumulator accumulatedText, IOException e)
            throws IOException, InterruptedException {
        accumulatedText.add(new MessagePart(Message.Type.Text, e.getMessage()));
        awaitSectionAll();
        AnnotatedImage image = getImage(messageRenderer.actor, messageRenderer.displayImage);
        show(image, e.getMessage());
        messageRenderer.startCompleted();
    }

    private void playSpeech(Actor actor, MessagePart part, String mood, ResourceLoader resources)
            throws IOException, InterruptedException {
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

    private void playSound(MessagePart part, ResourceLoader resources) throws IOException, InterruptedException {
        if (isSoundOutputEnabled()) {
            renderTimeSpannedPart(new RenderSound(resources, part.value, teaseLib));
        }
    }

    private void playSoundAsynchronous(MessagePart part, ResourceLoader resources)
            throws IOException, InterruptedException {
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

    private void awaitSectionMandatory() throws InterruptedException {
        currentRenderer.awaitMandatoryCompleted();
    }

    private void awaitSectionAll() throws InterruptedException {
        currentRenderer.awaitAllCompleted();
        currentRenderer = MediaRenderer.None;
    }

    private AnnotatedImage getImage(Actor actor, String displayImage)
            throws IOException, InterruptedException {
        if (actor.images.contains(displayImage)) {
            teaseLib.transcript.debug("image = '" + displayImage + "'");
            return actorImage(actor, displayImage);
        } else {
            if (!Message.NoImage.equalsIgnoreCase(displayImage)) {
                teaseLib.transcript.info("image = '" + displayImage + "'");
            }
            return instructionalImage(actor, displayImage);
        }
    }

    private static void appendMood(String mood, StringBuilder transcript) {
        if (!Mood.Neutral.equalsIgnoreCase(mood)) {
            transcript.append(mood);
            transcript.append(" ");
        }
    }

    private void show(AnnotatedImage image, String paragraph) {
        show(image, Collections.singletonList(paragraph));
    }

    private void show(AnnotatedImage image, List<String> paragraphs) {
        teaseLib.host.show(image, paragraphs);
        teaseLib.host.show();
    }

    private AnnotatedImage actorImage(Actor actor, String displayImage) throws IOException, InterruptedException {
        if (displayImage != null && !Message.NoImage.equals(displayImage)) {
            try {
                return actor.images.annotated(displayImage);
            } catch (IOException e) {
                handleIOException(e);
                return AnnotatedImage.NoImage;
            }
        } else {
            return AnnotatedImage.NoImage;
        }
    }

    private AnnotatedImage instructionalImage(Actor actor, String displayImage)
            throws IOException, InterruptedException {
        if (displayImage != null && !Message.NoImage.equals(displayImage)) {
            try {
                return actor.instructions.annotated(displayImage);
            } catch (IOException e) {
                handleIOException(e);
                return AnnotatedImage.NoImage;
            }
        } else {
            return AnnotatedImage.NoImage;
        }
    }

    protected void handleIOException(IOException e) throws IOException {
        ExceptionUtil.handleIOException(e, teaseLib.config, logger);
    }

    private void doKeyword(MessageRenderer messageRenderer, MessagePart part) throws InterruptedException {
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

    private void doDelay(MessagePart part) throws InterruptedException {
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

}
