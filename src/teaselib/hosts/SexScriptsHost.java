package teaselib.hosts;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ss.IScript;
import ss.desktop.MainFrame;
import teaselib.core.Audio;
import teaselib.core.Host;
import teaselib.core.Persistence;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.HostInputMethod;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.FileUtilities;
import teaselib.core.util.PropertyNameMappingPersistence;
import teaselib.util.AnnotatedImage;
import teaselib.util.Interval;

/**
 * SexScripts host:
 * <p>
 * 
 * - same for * prerendered speech
 * 
 * texts
 * 
 * @author Citizen-Cane
 * 
 */

// List of shortcomings of the user interface from TeaseLibs point of view
// TODO sounds are not playable from stream directly - using file cache
// TODO UI layout wastes a lot of screen estate, sub optimal for portrait images
// TODO pre-allocated screen estate for progress bar eats screen estate
// -> rendering images on the background image
// TODO Buttons should be layouted vertically, better for longer prompts
// TODO Text pane is not fixed size, I liked it better the way CM did it
// -> workarounded by adding pixels left and right of portrait images

// List of bugs:
// TODO Only sounds in background may be stopped, but TeaseLib requires interruptible sound threads
// because it usually waits on sounds and pre-recorded speech to be completed
// As a result, pre-recorded speech and non-background sounds can't be interrupted,
// which in turn makes the script response sluggish when canceling script functions or invoking the quit handler
// -> This mostly affects Mine because that script uses pre-recorded speech
// TODO Combobox doesn't respond to speech recognition reliably
// - This is in fact not a speech recognition related problem,
// but dismissing the combobox after showing the list programatically
/**
 * @author admin
 *
 */
public class SexScriptsHost implements Host, HostInputMethod.Backend {
    static final Logger logger = LoggerFactory.getLogger(SexScriptsHost.class);

    IScript ss;

    private final MainFrame mainFrame;

    private final JLabel textLabel;
    private final ImageIcon backgroundImageIcon;

    private final Image backgroundImage;

    // TODO Consolidate, use a single thread pool
    private final ExecutorService showPopupThreadPool = NamedExecutorService.singleThreadedQueue("Show-Choices");
    private final ExecutorService showChoicesThreadPool = NamedExecutorService.singleThreadedQueue("Show-Popup");

    private boolean intertitleActive = false;

    Runnable onQuitHandler = null;

    private final InputMethod inputMethod;

    public static Host from(IScript script) {
        return new SexScriptsHost(script);
    }

    public static Persistence persistence(IScript script) {
        return new PropertyNameMappingPersistence(new SexScriptsPersistence(script),
                new SexScriptsPropertyNameMapping());
    }

    public SexScriptsHost(ss.IScript script) {
        this.ss = script;
        // Should be set by the host, but SexScript doesn't, so we do
        Thread.currentThread().setName("TeaseScript main thread");

        // Initialize rendering via background image
        ImageIcon imageIcon = null;
        try {
            mainFrame = getMainFrame();
            textLabel = getField("label");
            imageIcon = getField("backgroundImage");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }

        backgroundImageIcon = imageIcon;
        if (imageIcon != null) {
            backgroundImage = imageIcon.getImage();
        } else {
            backgroundImage = null;
        }

        // automatically show pop-up
        int originalDefaultCloseoperation = mainFrame.getDefaultCloseOperation();
        mainFrame.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
                // Ignore
            }

            @Override
            public void windowIconified(WindowEvent e) {
                // Ignore
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                // Ignore
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // Ignore
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (onQuitHandler != null) {
                    mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                    var runnable = onQuitHandler;
                    // Execute each quit handler just once
                    onQuitHandler = null;
                    logger.info("Running quit handler {}", runnable.getClass().getName());
                    runnable.run();
                } else {
                    mainFrame.setDefaultCloseOperation(originalDefaultCloseoperation);
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                // Ignore
            }

            @Override
            public void windowActivated(WindowEvent e) {
                // Ignore
            }
        });

        inputMethod = new HostInputMethod(NamedExecutorService.singleThreadedQueue(getClass().getSimpleName()), this);
    }

    @Override
    public Audio audio(ResourceLoader resources, String path) {
        return new Audio() {
            String audioHandle = null;

            @Override
            public void load() throws IOException {
                this.audioHandle = resources.unpackToFile(path).getAbsolutePath();
            }

            @Override
            public void play() throws InterruptedException {
                // TODO interrupting the thread has no effect
                // TODO Doesn't react to stopSoundThreads() either
                ss.playSound(audioHandle);
            }

            @Override
            public void stop() {
                try {
                    ss.stopSoundThreads();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        };
    }

    private void show(BufferedImage image) {
        if (image != null) {
            if (focusLevel < 1.0) {
                float b = 0.20f;
                float m = 0.80f;
                int width = (int) (image.getWidth() * (b + focusLevel * m));
                int height = (int) (image.getHeight() * (b + focusLevel * m));
                var resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D resizedg2d = (Graphics2D) resized.getGraphics();
                BufferedImageOp blurOp = ConvolveEdgeReflectOp.blur(7);
                resizedg2d.drawImage(image, 0, 0, width, height, null);

                var blurred = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D blurredg2d = (Graphics2D) blurred.getGraphics();
                blurredg2d.drawImage(resized, blurOp, 0, 0);
                backgroundImageIcon.setImage(blurred);
            } else {
                backgroundImageIcon.setImage(image);
            }
        } else {
            backgroundImageIcon.setImage(backgroundImage);
        }
        mainFrame.repaint();
    }

    enum ActorPart {
        Face,
        Torso,
        Boots,
        All
    }

    private BufferedImage render(BufferedImage image, HumanPose.Estimation pose, Rectangle bounds) {
        var bi = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) bi.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        // Draw original background image
        g2d.drawImage(backgroundImage, 0, 0, bounds.width, bounds.height, null);

        int estimatedTextAreaX = bounds.width * 14 / 24;
        AffineTransform surface;
        if (actorProximity == Proximity.CLOSE) {
            surface = surfaceTransform(image, bounds, pose.boobs(), 2.5, estimatedTextAreaX);
        } else if (actorProximity == Proximity.FACE2FACE) {
            surface = surfaceTransform(image, bounds, pose.face(), 1.3, estimatedTextAreaX);
        } else if (actorProximity == Proximity.NEAR) {
            surface = surfaceTransform(image, bounds, pose.face(), 1.1, estimatedTextAreaX);
        } else if (actorProximity == Proximity.FAR) {
            surface = surfaceTransform(image, bounds, Optional.empty(), 1.0, estimatedTextAreaX);
        } else if (actorProximity == Proximity.AWAY) {
            // TODO Blur or turn display off
            surface = surfaceTransform(image, bounds, Optional.empty(), 1.0, estimatedTextAreaX);
        } else {
            throw new IllegalArgumentException(actorProximity.toString());
        }

        // debug code
        // surface = scale(image, pose.face(), bounds, 2.0);

        g2d.drawImage(image, surface, null);

        // debug code
        // renderPoseDebugInfo(g2d, Transform.dimension(image), pose, surface, bounds, estimatedTextAreaX);

        return bi;
    }

    private AffineTransform surfaceTransform(BufferedImage image, Rectangle bounds,
            Optional<Rectangle2D.Double> focusArea, double zoom, int textAreaX) {
        var surface = Transform.maxImage(image, bounds, focusArea);

        if (focusArea.isPresent()) {
            Double r = Transform.scale(focusArea.get(), Transform.dimension(image));
            surface = Transform.keepFocusAreaVisible(surface, image, bounds, r);
            if (zoom > 1.0) {
                surface = Transform.zoom(surface, r, zoom);
            }

            Transform.avoidFocusAreaBehindText(surface, bounds, r, textAreaX);
        }

        return surface;
    }

    void renderPoseDebugInfo(Graphics2D g2d, Dimension image, HumanPose.Estimation pose, AffineTransform surface,
            Rectangle bounds, int textAreaInsetRight) {
        g2d.setColor(Color.red);
        Point2D p0 = surface.transform(new Point2D.Double(0.0, 0.0), new Point2D.Double());
        Point2D p1 = surface.transform(new Point2D.Double(image.getWidth(), image.getHeight()), new Point2D.Double());
        g2d.drawRect((int) p0.getX(), (int) p0.getY(), (int) (p1.getX() - p0.getX()), (int) (p1.getY() - p0.getY()));

        Point2D poseHead = pose.head.orElse(new Point2D.Double(0, 0));
        Point2D p = surface.transform(
                new Point2D.Double(poseHead.getX() * image.getWidth(), poseHead.getY() * image.getHeight()),
                new Point2D.Double());

        Optional<Rectangle2D.Double> face = pose.face();
        g2d.setColor(face.isPresent() ? Color.cyan : Color.orange);
        g2d.drawOval((int) p.getX() - 2, (int) p.getY() - 2, 2 * 2, 2 * 2);

        g2d.setColor(face.isPresent() ? Color.blue : Color.red);
        int radius = face.isPresent() ? (int) (image.getWidth() * face.get().width / 2.0f) : 2;
        g2d.drawOval((int) p.getX() - radius, (int) p.getY() - radius, 2 * radius, 2 * radius);

        if (face.isPresent()) {
            g2d.setColor(Color.gray);
            g2d.drawLine(textAreaInsetRight, 0, textAreaInsetRight, bounds.height);
        }
    }

    private static Rectangle getContentBounds(ss.desktop.MainFrame mainFrame) {
        Container contentPane = mainFrame.getContentPane();
        Rectangle bounds = contentPane.getBounds();
        var insets = contentPane.getInsets();
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;
        return bounds;
    }

    private ss.desktop.MainFrame getMainFrame() throws NoSuchFieldException, IllegalAccessException {
        Class<?> scriptClass = ss.getClass().getSuperclass();
        var mainField = scriptClass.getDeclaredField("mainWindow");
        mainField.setAccessible(true);
        return (ss.desktop.MainFrame) mainField.get(ss);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Class<?> mainFrameClass = mainFrame.getClass();
        var field = mainFrameClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(mainFrame);
    }

    private void show(String message) {
        ss.show(message);
    }

    String currentText = "";
    private int estimatedTextFieldHeight = 0;
    private boolean slightlyLargerText = false;

    BufferedImage currentImage = null;
    HumanPose.Estimation currentPose = null;
    BufferedImage currentBackgroundImage = null;

    @Override
    public void show(AnnotatedImage actorImage, List<String> text) {
        if (actorImage != null) {
            try {
                currentImage = ImageIO.read(new ByteArrayInputStream(actorImage.bytes));
                currentPose = actorImage.pose;
            } catch (IOException e) {
                currentImage = null;
                currentPose = HumanPose.Estimation.NONE;
                logger.error(e.getMessage(), e);
            }
        } else {
            currentImage = null;
            currentPose = HumanPose.Estimation.NONE;
        }

        // keep text at the right
        if (currentImage != null) {
            alignTextRight();
        } else {
            centerText();
        }

        String newText = text.stream().collect(Collectors.joining("\n\n"));
        estimatedTextFieldHeight = currentText.isBlank() || text.size() == 1 ? 0
                : textLabel.getHeight() / currentText.length() * (newText.length() - text.size());
        currentText = newText;
        slightlyLargerText = text.size() == 1;

        intertitleActive = false;
        show();
    }

    private void alignTextRight() {
        Rectangle bounds = getContentBounds(mainFrame);
        Image spacer = new BufferedImage(bounds.width, 16, BufferedImage.TYPE_INT_ARGB);
        ((ss.desktop.Script) ss).setImage(spacer, false);
    }

    private void centerText() {
        ss.setImage((byte[]) null, 0);
    }

    float focusLevel = 1.0f;

    @Override
    public void setFocusLevel(float focusLevel) {
        this.focusLevel = focusLevel;
    }

    Point2D gaze = new Point2D.Double(0.5, 0.5); // The middle of the screen, complete image

    @Override
    public void setGaze(Point2D gaze) {
        this.gaze = gaze;
    }

    HumanPose.Proximity actorProximity = Proximity.FAR;

    @Override
    public void setActorProximity(HumanPose.Proximity proximity) {
        if (this.actorProximity != proximity) {
            this.actorProximity = proximity;
            show();
        }
    }

    @Override
    public void show() {
        Rectangle bounds = getContentBounds(mainFrame);
        if (currentImage != null) {
            currentBackgroundImage = render(currentImage, currentPose, bounds);
        } else {
            currentBackgroundImage = null;
        }

        EventQueue.invokeLater(() -> {
            if (intertitleActive) {
                show(html());
            } else {
                show(currentBackgroundImage);
                if (currentText == null || currentText.isBlank()) {
                    show("");
                } else {
                    show(html());
                }
            }
        });
    }

    private String html() {
        // Border radius does not work - probably a JLabel issue
        var html = new StringBuilder();
        html.append("<html><head></head>");

        html.append("<body style=\"");
        html.append(bodyStyle());
        html.append(currentText);

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String bodyStyle() {
        var html = new StringBuilder();

        // text background transparency doens't work because
        // the html implentation of the text label doesn't seem to support it:
        // rgb(r,g,b,a) -> opaque
        // rgba(r,g,b,a) -> transparent, no color lucency

        // setting a translucent color for the text label doesn't work either,
        // because the text label spans the whole panel width, therefore resulting in
        // a horizontal bar.

        float size = Math.min(backgroundImageIcon.getIconWidth() * 3.0f / 4.0f, backgroundImageIcon.getIconHeight());

        if (intertitleActive) {
            html.append("color:rgb(255, 255, 255);");
            int margin = (int) size / 20;
            html.append("margin:" + margin + ";padding:0;");
        } else {
            html.append("background-color:rgb(192, 192, 192, 144);");
            html.append("border: 3px solid rgb(192, 192, 192, 144);");
            html.append("border-radius: 5px;");
            html.append("border-top-width: 3px;");
            html.append("border-left-width: 7px;");
            html.append("border-bottom-width: 5px;");
            html.append("border-right-width: 7px;");
        }

        if (estimatedTextFieldHeight < size) {
            html.append("font-size: ");
            float fontSize = size / 30.0f;
            float factor;
            factor = slightlyLargerText ? 1.25f : 1.0f;
            html.append(Math.max(fontSize * factor, 18));
            html.append("px;");
        }

        html.append("\\\">");

        return html.toString();
    }

    @Override
    public void showInterTitle(String text) {
        if (!intertitleActive) {
            createIntertitleImage(backgroundImageIcon.getImage());
            centerText();
            slightlyLargerText = false;
            intertitleActive = true;
        }
        currentText = text;
        show();
    }

    private void createIntertitleImage(Image image) {
        Rectangle bounds = getContentBounds(mainFrame);
        bounds.x = 0;
        bounds.y = 0;
        var interTitleImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) interTitleImage.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        // Draw original background image
        g2d.drawImage(image, 0, 0, bounds.width, bounds.height, null);
        // Compensate for the text not being centered (causes the text area to be not centered anymore)
        var offset = 0;
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f));
        int top = bounds.height / 4 + offset;
        int bottom = bounds.height * 3 / 4 + offset;
        // Render the intertitle top, text and bottom areas
        g2d.fillRect(bounds.x, bounds.y, bounds.width, top);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.80f));
        g2d.fillRect(bounds.x, top, bounds.width, bottom - top);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f));
        g2d.fillRect(bounds.x, bottom, bounds.width, bounds.height - bottom);

        // TODO replace currentImage and show with show()
        backgroundImageIcon.setImage(interTitleImage);
        mainFrame.repaint();
    }

    @Override
    public void endScene() {
        // Keep the image, remove any text to provide feedback
        currentText = "";
        show(currentText);
    }

    @Override
    public List<Boolean> showCheckboxes(String caption, List<String> texts, List<Boolean> values, boolean allowCancel) {
        List<Boolean> results;
        do {
            try {
                results = ss.getBooleans(caption, texts, values);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
            // Loop until the user pressed OK -> != null
        } while (results == null && !allowCancel);
        return results;
    }

    private List<Runnable> getClickableChoices(List<Choice> choices) {
        try {
            // Get buttons
            Class<?> mainFrameClass = mainFrame.getClass();
            List<Runnable> clickableChoices = new ArrayList<>(choices.size());
            // Multiple choices are managed via an array of buttons,
            // whereas a single choice is implemented as a single button
            Field buttonsField = mainFrameClass.getDeclaredField("buttons");
            buttonsField.setAccessible(true);
            javax.swing.JButton[] ssButtons = (javax.swing.JButton[]) buttonsField.get(mainFrame);
            // Must also test the single button
            Field buttonField = mainFrameClass.getDeclaredField("button");
            buttonField.setAccessible(true);
            final javax.swing.JButton ssButton = (javax.swing.JButton) buttonField.get(mainFrame);
            List<javax.swing.JButton> buttons = new ArrayList<>(ssButtons.length + 1);
            for (javax.swing.JButton button : ssButtons) {
                if (button.isVisible()) {
                    buttons.add(button);
                }
            }
            // TODO Only for ss timed button?
            buttons.add(ssButton);
            // Combobox
            final javax.swing.JComboBox<String> ssComboBox = getComboBox();
            // Init all slots
            for (int index : new Interval(0, choices.size() - 1)) {
                clickableChoices.add(index, null);
            }
            // Combobox
            final ComboBoxModel<String> model = ssComboBox.getModel();
            for (int j = 0; j < model.getSize(); j++) {
                final int comboboxIndex = j;
                for (final int index : new Interval(0, choices.size() - 1)) {
                    final String text = model.getElementAt(j);
                    if (text.contains(Choice.getDisplay(choices.get(index)))) {
                        clickableChoices.set(index, (Runnable) () -> ssComboBox.setSelectedIndex(comboboxIndex));
                    }
                }
            }
            // Check pretty buttons for corresponding text
            // There might be more buttons than expected,
            // probably some kind of caching
            for (final javax.swing.JButton button : buttons) {
                for (int index : new Interval(0, choices.size() - 1)) {
                    String buttonText = button.getText();
                    String choice = Choice.getDisplay(choices.get(index));
                    if (buttonText.contains(choice)) {
                        clickableChoices.set(index, () -> {
                            button.doClick();
                        });
                    }
                }
            }
            // Assign clicks to combo box
            // If there are too many choices, a combo box will be used

            // If a choice wasn't found, the element at the corresponding index
            // would be null
            return clickableChoices;
        } catch (ReflectiveOperationException e) {
            logger.error(e.getMessage(), e);
        }
        return new ArrayList<>();

    }

    @Override
    public boolean dismissChoices(List<Choice> choices) {
        List<Runnable> clickableChoices = getClickableChoices(choices);
        if (clickableChoices != null) {
            Runnable delegate = clickableChoices.get(0);
            if (delegate != null) {
                delegate.run();
                return true;
            }
        }
        return false;
    }

    public javax.swing.JComboBox<String> getComboBox() throws NoSuchFieldException, IllegalAccessException {
        var comboField = mainFrame.getClass().getDeclaredField("comboBox");
        comboField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final javax.swing.JComboBox<String> ssComboBox = (javax.swing.JComboBox<String>) comboField.get(mainFrame);
        return ssComboBox;
    }

    class ShowPopupTask {
        final FutureTask<Boolean> task;
        final AtomicBoolean resetPopupVisibility = new AtomicBoolean(false);
        static final int POLL_INTERVAL_MILLIS = 100;

        JComboBox<String> comboBox = null;

        public ShowPopupTask() {
            try {
                comboBox = getComboBox();
            } catch (ReflectiveOperationException e) {
                logger.error(e.getMessage(), e);
            }
            task = new FutureTask<>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    // Poll a little
                    for (int i = 1; i < 10; i++) {
                        if (comboBox.isVisible() /* && comboBox.hasFocus() */) {
                            // Work-around the issue that the pop-up cannot be hidden
                            // after making it visible when not focused
                            showPopup();
                            return true;
                        }
                        Thread.sleep(POLL_INTERVAL_MILLIS);
                    }
                    return false;
                }
            });
        }

        void showPopup() {
            java.awt.EventQueue.invokeLater(() -> {
                comboBox.requestFocus();
                comboBox.setPopupVisible(true);
                resetPopupVisibility.set(true);
            });
        }

        void cleanup() {
            java.awt.EventQueue.invokeLater(() -> {
                if (resetPopupVisibility.get())
                    if (comboBox.isPopupVisible()) {
                        comboBox.setPopupVisible(false);
                    }
            });
        }
    }

    @Override
    public InputMethod inputMethod() {
        return inputMethod;
    }

    @Override
    public Prompt.Result reply(Choices choices) {
        if (Thread.interrupted()) {
            throw new ScriptInterruptedException();
        }
        // open the combo box pop-up if necessary in order to
        // allow the user to read prompts without mouse/touch interaction
        var showPopupTask = new ShowPopupTask();
        boolean showPopup = choices.size() > 1;
        if (showPopup) {
            showPopupThreadPool.execute(showPopupTask.task);
        }
        // getSelectedValue() won't throw InterruptedException,
        // and won't clean up buttons
        // -> Execute it in a separate thread, and
        // cancel the same way as speech recognition
        FutureTask<Prompt.Result> showChoices = new FutureTask<>(
                () -> new Prompt.Result(ss.getSelectedValue(null, choices.toDisplay())));
        Prompt.Result result;
        showChoicesThreadPool.execute(showChoices);
        try {
            result = showChoices.get();
            if (showPopup) {
                try {
                    showPopupTask.task.get();
                } catch (ExecutionException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        } catch (InterruptedException e) {
            List<Runnable> clickableChoices = getClickableChoices(choices);
            if (!clickableChoices.isEmpty()) {
                // Click any button
                Runnable delegate = clickableChoices.get(0);
                while (!showChoices.isDone()) {
                    // Stupid trick to be able to actually click a combo item
                    if (showPopupTask.comboBox.isVisible()) {
                        showPopupTask.showPopup();
                    }

                    try {
                        delegate.run();
                    } catch (Exception e1) {
                        throw ExceptionUtil.asRuntimeException(e1);
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) { // Ignore
                        Thread.currentThread().interrupt();
                    }
                }
            }
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result = Prompt.Result.DISMISSED;
        } finally {
            showPopupTask.cleanup();
        }
        return result;
    }

    @Override
    public void setQuitHandler(Runnable onQuitHandler) {
        this.onQuitHandler = onQuitHandler;
    }

    @Override
    public File getLocation(Location folder) {
        if (folder == Location.Host)
            return FileUtilities.currentDir();
        else if (folder == Location.TeaseLib)
            return ResourceLoader.getProjectPath(getClass()).getParentFile().getAbsoluteFile();
        else if (folder == Location.User)
            return new File(getLocation(Location.Host).getAbsoluteFile(), "teaselib");
        else if (folder == Location.Log)
            return getLocation(Location.Host);
        else
            throw new IllegalArgumentException(Objects.toString(folder));
    }

}
