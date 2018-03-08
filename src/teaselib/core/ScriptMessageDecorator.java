package teaselib.core;

import java.util.Optional;
import java.util.function.Function;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Message;
import teaselib.MessagePart;
import teaselib.core.media.RenderedMessage;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public class ScriptMessageDecorator {
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

    public RenderedMessage.Function[] messageModifiers() {
        return new RenderedMessage.Function[] { //
                this::filterDebug, this::addTextToSpeech, this::expandTextVariables, this::addActorImages,
                this::applyDelayRules };
    }

    private Message filterDebug(Message message) {
        Message debugFiltered = new Message(message.actor);
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

    private Message addTextToSpeech(Message message) {
        return textToSpeech != null ? textToSpeech.createSpeechMessage(message, resources) : message;
    }

    private Message expandTextVariables(Message message) {
        Message expandedTextVariables = new Message(message.actor);
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

    public Message addActorImages(Message message) {
        // Clone the actor to prevent the wrong actor image to be displayed
        // when changing the actor images right after rendering a message.
        // Without cloning one of the new actor images would be displayed
        // with the current message because the actor is shared between
        // script and message
        Message parsedMessage = new Message(new Actor(message.actor));

        // TODO hint actor aspect, camera position, posture

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
                        final String currentMood;
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
                    // } else if (Message.Type.FileTypes.contains(part.type)) {
                    // parsedMessage.add(part.type, part.value);
                } else if (part.type == Message.Type.Keyword) {
                    parsedMessage.add(part);
                } else if (part.type == Message.Type.Mood) {
                    nextMood = part.value;
                } else if (part.type == Message.Type.Text) {
                    // set mood if not done already
                    final String currentMood;
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
                    // Replace text variables
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

    private static void ensureEmptyMessageContainsDisplayImage(Message parsedMessage, String nextImage) {
        parsedMessage.add(Message.Type.Image, nextImage);
    }

    private Message applyDelayRules(Message message) {
        // TODO Apply delay rules like after speech, not after chowChoices etc. and remove the counterpart in
        // RenderMessage
        MessagePart delay = null;
        Message messageWithDelays = new Message(message.actor);
        for (MessagePart messagePart : message) {
            // TODO ...
        }
        return message;
    }
}
