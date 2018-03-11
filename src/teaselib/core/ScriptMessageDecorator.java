package teaselib.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import teaselib.AbstractMessage;
import teaselib.Actor;
import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;
import teaselib.core.media.MessageTextAccumulator;
import teaselib.core.media.RenderedMessage;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public class ScriptMessageDecorator {
    private static final long DELAY_BETWEEN_PARAGRAPHS = 500;
    private static final long DELAY_AT_END_OF_MESSAGE = 2000;
    private static final long DELAY_FOR_APPEND = 0;

    static final MessagePart DelayBetweenParagraphs = delay(DELAY_BETWEEN_PARAGRAPHS);
    static final MessagePart DelayAtEndOfPage = delay(DELAY_AT_END_OF_MESSAGE);
    static final MessagePart DelayAfterAppend = delay(DELAY_FOR_APPEND);
    private static Set<MessagePart> generatedDelays = new HashSet<>(
            Arrays.asList(DelayAfterAppend, DelayBetweenParagraphs, DelayAtEndOfPage));

    private final Configuration config;
    private final String displayImage;
    private final Actor actor;
    private final String mood;
    private final ResourceLoader resources;
    private final Function<String, String> expandTextVariables;
    private final TextToSpeechPlayer textToSpeech;

    public ScriptMessageDecorator(Configuration config, String displayImage, Actor actor, String mood,
            ResourceLoader resources, Function<String, String> expandTextVariables,
            Optional<TextToSpeechPlayer> textToSpeech) {
        super();
        this.config = config;
        this.displayImage = displayImage;
        this.actor = actor;
        this.mood = mood;
        this.resources = resources;
        this.expandTextVariables = expandTextVariables;
        this.textToSpeech = textToSpeech.isPresent() ? textToSpeech.get() : null;
    }

    public RenderedMessage.Decorator[] messageModifiers() {
        return new RenderedMessage.Decorator[] { //
                this::filterDebug, this::addTextToSpeech, this::expandTextVariables, this::addActorImages,
                this::applyDelayRules };
    }

    private AbstractMessage filterDebug(AbstractMessage message) {
        AbstractMessage debugFiltered = new AbstractMessage();
        for (MessagePart part : message) {
            if (part.type == Message.Type.DesktopItem
                    && !Boolean.parseBoolean(config.get(Config.Render.InstructionalImages))) {
                // Ignore
            } else if (part.type == Message.Type.Image
                    && !Boolean.parseBoolean(config.get(Config.Render.InstructionalImages))) {
                // Ignore
            } else if (part.type == Message.Type.Sound && !Boolean.parseBoolean(config.get(Config.Render.Sound))) {
                // Ignore
            } else if (part.type == Message.Type.BackgroundSound
                    && !Boolean.parseBoolean(config.get(Config.Render.Sound))) {
                // Ignore
            } else {
                debugFiltered.add(part);
            }
        }
        return debugFiltered;
    }

    private AbstractMessage addTextToSpeech(AbstractMessage message) {
        return textToSpeech != null ? textToSpeech.createSpeechMessage(actor, message, resources) : message;
    }

    private AbstractMessage expandTextVariables(AbstractMessage message) {
        AbstractMessage expandedTextVariables = new AbstractMessage();
        for (MessagePart part : message) {
            if (part.type == Message.Type.Speech && !Message.Type.isSound(part.value)) {
                if (Boolean.parseBoolean(config.get(Config.Render.Speech))) {
                    expandedTextVariables.add(part.type, expandTextVariables.apply(part.value));
                }
            } else if (part.type == Message.Type.Text) {
                expandedTextVariables.add(new MessagePart(part.type, expandTextVariables.apply(part.value)));

            } else {
                expandedTextVariables.add(part);
            }
        }
        return expandedTextVariables;
    }

    public AbstractMessage addActorImages(AbstractMessage message) {
        AbstractMessage parsedMessage = new AbstractMessage();

        if (message.isEmpty()) {
            ensureEmptyMessageContainsDisplayImage(parsedMessage, getActorOrDisplayImage(displayImage, mood));
        } else {
            String imageType = displayImage;
            String nextImage = null;
            String lastMood = null;
            String nextMood = null;

            for (MessagePart part : message) {
                if (part.type == Message.Type.Image) {
                    // Remember what type of image to display
                    // with the next text element
                    if (Message.ActorImage.equalsIgnoreCase(part.value)) {
                        imageType = part.value;
                    } else if (Message.NoImage.equalsIgnoreCase(part.value)) {
                        imageType = part.value;
                    } else {
                        String currentMood;
                        if (nextMood == null) {
                            currentMood = mood;
                        } else {
                            currentMood = nextMood;
                        }
                        // Inject mood if changed
                        if (currentMood != lastMood) {
                            parsedMessage.add(Message.Type.Mood, currentMood);
                            lastMood = currentMood;
                        }
                        imageType = nextImage = getActorOrDisplayImage(part.value, currentMood);
                        parsedMessage.add(part.type, nextImage);
                    }
                } else if (part.type == Message.Type.Keyword) {
                    parsedMessage.add(part);
                } else if (part.type == Message.Type.Mood) {
                    nextMood = part.value;
                } else if (part.type == Message.Type.Text) {
                    // set mood if not done already
                    String currentMood;
                    if (nextMood == null) {
                        currentMood = mood;
                    } else {
                        currentMood = nextMood;
                        nextMood = null;
                    }
                    // Inject mood if changed
                    if (currentMood != lastMood) {
                        parsedMessage.add(Message.Type.Mood, currentMood);
                        lastMood = currentMood;
                    }
                    // Update image if changed
                    if (imageType != nextImage) {
                        nextImage = getActorOrDisplayImage(imageType, currentMood);
                        parsedMessage.add(Message.Type.Image, nextImage);
                    }
                    parsedMessage.add(part);
                } else {
                    parsedMessage.add(part);
                }
            }

            if (parsedMessage.isEmpty()) {
                ensureEmptyMessageContainsDisplayImage(parsedMessage, getActorOrDisplayImage(imageType, mood));
            }
        }

        return parsedMessage;
    }

    private String getActorOrDisplayImage(String imageType, String currentMood) {
        final String nextImage;
        if (Message.ActorImage.equalsIgnoreCase(imageType)
                && !Boolean.parseBoolean(config.get(Config.Render.ActorImages))) {
            nextImage = Message.NoImage;
        } else if (!Message.ActorImage.equalsIgnoreCase(imageType)
                && !Boolean.parseBoolean(config.get(Config.Render.InstructionalImages))) {
            nextImage = Message.NoImage;
        } else if (Message.ActorImage.equalsIgnoreCase(imageType)) {
            if (actor.images.hasNext()) {
                actor.images.hint(currentMood);
                nextImage = actor.images.next();
            } else {
                nextImage = Message.NoImage;
            }
        } else {
            nextImage = imageType;
        }
        return nextImage;
    }

    private static void ensureEmptyMessageContainsDisplayImage(AbstractMessage parsedMessage, String nextImage) {
        parsedMessage.add(Message.Type.Image, nextImage);
    }

    private AbstractMessage applyDelayRules(AbstractMessage message) {
        AbstractMessage lastSection = RenderedMessage.getLastSection(message);
        MessagePart currentDelay = null;
        AbstractMessage messageWithDelays = new AbstractMessage();
        boolean showChoicesApplied = false;

        for (MessagePart messagePart : message) {
            if (messagePart.type == Type.Delay) {
                currentDelay = accumulateDelay(currentDelay, messagePart);
            } else {
                if (messagePart.type == Type.Keyword && Message.ShowChoices.equalsIgnoreCase(messagePart.value)
                        && isGeneratedDelay(currentDelay)) {
                    messageWithDelays.add(messagePart);
                    showChoicesApplied = true;
                } else {
                    showChoicesApplied = injectShowChoices(messageWithDelays, currentDelay, showChoicesApplied);
                    currentDelay = injectDelay(messageWithDelays, currentDelay);

                    if (messagePart.type == Type.Speech) {
                        currentDelay = injectSpeechDelay(messageWithDelays, messagePart, lastSection);
                    } else {
                        messageWithDelays.add(messagePart);
                    }
                }
            }
        }

        injectShowChoices(messageWithDelays, currentDelay, showChoicesApplied);
        injectDelay(messageWithDelays, currentDelay);

        return messageWithDelays;
    }

    private MessagePart injectSpeechDelay(AbstractMessage messageWithDelays, MessagePart messagePart,
            AbstractMessage lastSection) {
        MessagePart currentDelay;
        messageWithDelays.add(messagePart);
        // TODO showing last image immediately when showing Choices
        // Text Speech Image -> Text Speech ShowChoices Image Delay

        if (MessageTextAccumulator.canAppendTo(messagePart.value)) {
            currentDelay = DelayAfterAppend;
        } else {
            currentDelay = lastSection.contains(messagePart) ? DelayAtEndOfPage : DelayBetweenParagraphs;
        }
        return currentDelay;
    }

    private boolean injectShowChoices(AbstractMessage messageWithDelays, MessagePart currentDelay,
            boolean showChoicesApplied) {
        if (currentDelay == DelayAtEndOfPage && !showChoicesApplied) {
            messageWithDelays.add(Type.Keyword, Message.ShowChoices);
            showChoicesApplied = true;
        }
        return showChoicesApplied;
    }

    private MessagePart injectDelay(AbstractMessage messageWithDelays, MessagePart currentDelay) {
        if (currentDelay != null) {
            messageWithDelays.add(currentDelay);
            currentDelay = null;
        }
        return currentDelay;
    }

    private static MessagePart delay(long millis) {
        return new MessagePart(Type.Delay, Double.toString((double) (millis) / 1000.0));
    }

    private MessagePart accumulateDelay(MessagePart currentDelay, MessagePart additionalDelay) {
        if (currentDelay == null) {
            currentDelay = additionalDelay;
        } else if (isGeneratedDelay(currentDelay)) {
            currentDelay = additionalDelay;
        } else {
            currentDelay = new MessagePart(Type.Delay, Double
                    .toString(Double.parseDouble(currentDelay.value) + Double.parseDouble(additionalDelay.value)));
        }
        return currentDelay;
    }

    private boolean isGeneratedDelay(MessagePart delay) {
        return generatedDelays.contains(delay);
    }
}
