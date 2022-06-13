package teaselib.core.media;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import teaselib.Actor;
import teaselib.ActorImages.Next;
import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;
import teaselib.core.AbstractMessage;
import teaselib.core.ResourceLoader;
import teaselib.core.configuration.Configuration;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public class ScriptMessageDecorator {

    private static final long DELAY_BETWEEN_PARAGRAPHS_MILLIS = 750;
    private static final long DELAY_FOR_APPEND_MILLIS = 0;

    static final double DELAY_BETWEEN_PARAGRAPHS_SECONDS = DELAY_BETWEEN_PARAGRAPHS_MILLIS / 1000.0;
    static final double DELAY_BETWEEN_SECTIONS_SECONDS = 2.0;

    static final MessagePart DelayBetweenParagraphs = delay(DELAY_BETWEEN_PARAGRAPHS_MILLIS);
    static final MessagePart DelayAfterAppend = delay(DELAY_FOR_APPEND_MILLIS);

    private static Set<MessagePart> generatedDelays = new HashSet<>(
            Arrays.asList(DelayAfterAppend, DelayBetweenParagraphs));

    private final Configuration config;
    private final String displayImage;
    private final Actor actor;
    private final String mood;
    private final ResourceLoader resources;
    private final UnaryOperator<String> expandTextVariables;
    private final TextToSpeechPlayer textToSpeech;

    public ScriptMessageDecorator(Configuration config, String displayImage, Actor actor, String mood,
            ResourceLoader resources, UnaryOperator<String> expandTextVariables) {
        this(config, displayImage, actor, mood, resources, expandTextVariables, Optional.empty());
    }

    public ScriptMessageDecorator(Configuration config, String displayImage, Actor actor, String mood,
            ResourceLoader resources, UnaryOperator<String> expandTextVariables,
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

    public RenderedMessage.Decorator[] all() {
        if (textToSpeech != null) {
            return new RenderedMessage.Decorator[] { this::filterDebug, this::applyDelayRules, this::addTextToSpeech,
                    this::expandTextVariables, this::addActorImages };
        } else {
            return new RenderedMessage.Decorator[] { this::filterDebug, this::applyDelayRules,
                    this::expandTextVariables, this::addActorImages };
        }
    }

    public RenderedMessage.Decorator[] justText() {
        return new RenderedMessage.Decorator[] { this::filterDebug, this::applyDelayRules, this::expandTextVariables };
    }

    private AbstractMessage filterDebug(AbstractMessage message) {
        var debugFiltered = new AbstractMessage();
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
        return textToSpeech.createSpeechMessage(actor, message, resources);
    }

    private AbstractMessage expandTextVariables(AbstractMessage message) {
        var expandedTextVariables = new AbstractMessage();
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
        var parsedMessage = new AbstractMessage();

        if (message.isEmpty()) {
            ensureMessageContainsDisplayImage(parsedMessage, fetchImage(displayImage, mood));
        } else {
            String imageType = displayImage;
            String nextImage = null;
            String lastMood = null;
            String nextMood = null;
            MessagePart text = null;
            MessagePart previousDurationPart = null;

            for (MessagePart part : message) {
                if (part.type == Message.Type.Image) {
                    imageType = part.value;
                    previousDurationPart = null;
                } else if (part.type == Message.Type.Keyword) {
                    parsedMessage.add(part);
                    // previousDurationPart = null;
                } else if (part.type == Message.Type.Mood) {
                    nextMood = part.value;
                    previousDurationPart = null;
                } else if (part.type.isAnyOf(Message.Type.TextTypes)) {
                    text = part;
                    previousDurationPart = null;
                } else if (part.type.isAnyOf(Message.Type.DelayTypes)) {
                    if (previousDurationPart == null) {
                        // set mood if not done already
                        String currentMood;
                        if (nextMood == null) {
                            currentMood = mood;
                        } else {
                            currentMood = nextMood;
                            nextMood = null;
                        }
                        // Inject mood if changed
                        if (currentMood != null && !currentMood.equalsIgnoreCase(lastMood)) {
                            parsedMessage.add(Message.Type.Mood, currentMood);
                        }
                        lastMood = currentMood;
                        // Update image if changed
                        if (!imageType.equalsIgnoreCase(nextImage)) {
                            nextImage = fetchImage(imageType, currentMood);
                            parsedMessage.add(Message.Type.Image, nextImage);
                        }
                        // Optional text
                        if (text != null) {
                            parsedMessage.add(text);
                            text = null;
                        }
                        // All rendered with DurationType
                        parsedMessage.add(part);

                        previousDurationPart = part;
                    } else {
                        parsedMessage.add(part);
                        previousDurationPart = null;
                    }
                } else {
                    parsedMessage.add(part);
                    previousDurationPart = null;
                }
            }

            MessagePart lastPart = message.get(message.size() - 1);
            if (lastPart.type.isAnyOf(Message.Type.DisplayTypes)) {
                // Inject mood if changed
                String currentMood;
                if (nextMood == null) {
                    currentMood = mood;
                } else {
                    currentMood = nextMood;
                    nextMood = null;
                }
                if (!currentMood.equalsIgnoreCase(lastMood)) {
                    parsedMessage.add(Message.Type.Mood, currentMood);
                }
                // Update image if changed
                if (!imageType.equalsIgnoreCase(nextImage)) {
                    nextImage = fetchImage(imageType, currentMood);
                    parsedMessage.add(Message.Type.Image, nextImage);
                }
                // Optional text
                if (text != null) {
                    parsedMessage.add(text);
                    text = null;
                }
            }

            if (nextImage == null) {
                ensureMessageContainsDisplayImage(parsedMessage, fetchImage(imageType, mood));
            }
        }

        return parsedMessage;

    }

    private String fetchImage(String imageType, String currentMood) {
        final String nextImage;
        // Don't render images
        if (Message.ActorImage.equalsIgnoreCase(imageType)
                && !Boolean.parseBoolean(config.get(Config.Render.ActorImages))) {
            nextImage = Message.NoImage;
        } else if (!Message.ActorImage.equalsIgnoreCase(imageType)
                && !Boolean.parseBoolean(config.get(Config.Render.InstructionalImages))) {
            nextImage = Message.NoImage;
        } else if (Message.NoImage.equalsIgnoreCase(imageType)) {
            nextImage = Message.NoImage;
        } else {
            // choose and fetch image
            if (Message.ActorImage.equalsIgnoreCase(imageType)) {
                nextImage = fetchActorImage(currentMood);
            } else {
                nextImage = fetchInstructionalImage(imageType);
            }
        }
        return nextImage;
    }

    private String fetchActorImage(String currentMood) {
        String nextImage;
        if (actor.images.hasNext()) {
            nextImage = actor.images.next(currentMood);
            actor.images.fetch(nextImage);
        } else {
            actor.images.advance(Next.Take, currentMood);
            if (actor.images.hasNext()) {
                nextImage = actor.images.next(currentMood);
                actor.images.fetch(nextImage);
            } else {
                nextImage = Message.NoImage;
            }
        }
        return nextImage;
    }

    private String fetchInstructionalImage(String imageType) {
        String nextImage = imageType;
        actor.instructions.fetch(nextImage);
        return nextImage;
    }

    private static void ensureMessageContainsDisplayImage(AbstractMessage parsedMessage, String nextImage) {
        parsedMessage.add(Message.Type.Image, nextImage);
    }

    private AbstractMessage applyDelayRules(AbstractMessage message) {
        AbstractMessage lastSection = RenderedMessage.getLastParagraph(message);
        MessagePart currentDelay = null;
        var messageWithDelays = new AbstractMessage();

        for (MessagePart messagePart : message) {
            if (messagePart.type == Type.Delay) {
                currentDelay = accumulateDelay(currentDelay, messagePart);
            } else {
                if (messagePart.type == Type.Keyword && Message.ShowChoices.equalsIgnoreCase(messagePart.value)
                        && isGeneratedDelay(currentDelay)) {
                    messageWithDelays.add(messagePart);
                } else {
                    if (messagePart.type == Type.Text || messagePart.type == Type.Image
                            || messagePart.type == Type.Mood) {
                        injectDelay(messageWithDelays, currentDelay);
                        currentDelay = null;
                    }

                    if (messagePart.type == Type.Speech) {
                        currentDelay = accumulateDelay(currentDelay,
                                injectSpeechDelay(messageWithDelays, messagePart, lastSection));
                    } else if (messagePart.type == Type.Text && !lastSection.contains(messagePart)) {
                        messageWithDelays.add(messagePart);
                        if (MessageTextAccumulator.canAppendTo(messagePart.value)) {
                            currentDelay = accumulateDelay(currentDelay, DelayAfterAppend);
                        } else {
                            currentDelay = accumulateDelay(currentDelay, DelayBetweenParagraphs);
                        }
                    } else {
                        messageWithDelays.add(messagePart);
                    }
                }
            }
        }

        injectDelay(messageWithDelays, currentDelay);
        return messageWithDelays;
    }

    private static MessagePart injectSpeechDelay(AbstractMessage messageWithDelays, MessagePart speech,
            AbstractMessage lastSection) {
        MessagePart currentDelay;
        messageWithDelays.add(speech);

        if (MessageTextAccumulator.canAppendTo(speech.value)) {
            currentDelay = DelayAfterAppend;
        } else if (lastSection.contains(speech)) {
            currentDelay = null;
        } else {
            currentDelay = DelayBetweenParagraphs;
        }
        return currentDelay;
    }

    private static void injectDelay(AbstractMessage messageWithDelays, MessagePart delay) {
        if (delay != null) {
            messageWithDelays.add(delay);
        }
    }

    private static MessagePart delay(long millis) {
        return new MessagePart(Type.Delay, Double.toString(millis / 1000.0));
    }

    static MessagePart accumulateDelay(MessagePart currentDelay, MessagePart additionalDelay) {
        MessagePart newDelay;
        if (currentDelay == null) {
            newDelay = additionalDelay;
        } else if (additionalDelay == null) {
            newDelay = currentDelay;
        } else if (isGeneratedDelay(currentDelay)) {
            newDelay = additionalDelay;
        } else {
            double[] currentDelayValues = getDelayInterval(currentDelay.value);
            double[] additionalDelayValues = getDelayInterval(additionalDelay.value);
            String newInterval;
            if (currentDelayValues.length == additionalDelayValues.length) {
                if (currentDelayValues.length == 1) {
                    newInterval = Double.toString(currentDelayValues[0] + additionalDelayValues[0]);
                } else {
                    newInterval = Double.toString(currentDelayValues[0] + additionalDelayValues[0]) + " "
                            + Double.toString(currentDelayValues[1] + additionalDelayValues[1]);
                }
            } else if (currentDelayValues.length == 2 && additionalDelayValues.length == 1) {
                newInterval = Double.toString(currentDelayValues[0] + additionalDelayValues[0]) + " "
                        + Double.toString(currentDelayValues[1] + additionalDelayValues[0]);
            } else if (currentDelayValues.length == 1 && additionalDelayValues.length == 2) {
                newInterval = Double.toString(currentDelayValues[0] + additionalDelayValues[0]) + " "
                        + Double.toString(currentDelayValues[0] + additionalDelayValues[1]);
            } else {
                throw new IllegalArgumentException(currentDelay.value + " or " + additionalDelay.value);
            }
            newDelay = new MessagePart(Type.Delay, newInterval);
        }
        return newDelay;

    }

    private static boolean isGeneratedDelay(MessagePart delay) {
        return generatedDelays.contains(delay);
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

}
