package teaselib.hosts;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author Citizen-Cane
 *
 */
public class AbstractBufferedImageRenderer {

    final Deque<BufferedImage> buffers = new ArrayDeque<>(2);

    public static BufferedImage newOrSameImage(BufferedImage image, Rectangle bounds) {
        if (image == null) {
            return newImage(bounds);
        } else if (bounds.width != image.getWidth() || bounds.height != image.getHeight()) {
            return newImage(bounds);
        } else {
            return image;
        }
    }

    // TODO Surprising in mid-script - should be filled once to capacity and then stay the same size
    // java.util.NoSuchElementException
    // at java.base/java.util.ArrayDeque.removeFirst(ArrayDeque.java:362)
    // at java.base/java.util.ArrayDeque.remove(ArrayDeque.java:523)
    // at teaselib.hosts.AbstractBufferedImageRenderer.nextBuffer(AbstractBufferedImageRenderer.java:29)
    // at teaselib.hosts.SexScriptsHost.render(SexScriptsHost.java:503)
    // at teaselib.hosts.SexScriptsHost.show(SexScriptsHost.java:493)
    // at teaselib.core.ui.AnimatedHost.show(AnimatedHost.java:128)
    // at teaselib.core.ai.perception.ProximitySensor.run(ProximitySensor.java:71)
    // at teaselib.core.ai.perception.ProximitySensor.run(ProximitySensor.java:1)
    // at teaselib.core.ai.perception.HumanPoseDeviceInteraction.addEventListener(HumanPoseDeviceInteraction.java:137)
    // at teaselib.core.Script.startProximitySensor(Script.java:144)
    // at teaselib.core.Script.lambda$23(Script.java:620)
    // at teaselib.core.ui.Prompt.show(Prompt.java:221)
    // at teaselib.core.ui.PromptQueue.activate(PromptQueue.java:193)
    // at teaselib.core.ui.PromptQueue.show(PromptQueue.java:35)
    // at teaselib.core.ui.Shower.showNew(Shower.java:84)
    // at teaselib.core.ui.Shower.show(Shower.java:26)

    public BufferedImage nextBuffer(Rectangle bounds) {
        BufferedImage image;
        if (buffers.size() >= 2) {
            image = newOrSameImage(buffers.remove(), bounds);
        } else {
            image = newImage(bounds);
        }
        buffers.add(image);
        return image;
    }

    public static BufferedImage newImage(Rectangle bounds) {
        return newImage(bounds.width, bounds.height);
    }

    private static BufferedImage newImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * 
     */
    public AbstractBufferedImageRenderer() {
        super();
    }

}