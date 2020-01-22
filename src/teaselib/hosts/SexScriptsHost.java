package teaselib.hosts;

import java.awt.Color;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.WindowConstants;

import org.bytedeco.javacpp.opencv_core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ss.IScript;
import ss.desktop.MainFrame;
import teaselib.core.Audio;
import teaselib.core.Host;
import teaselib.core.Persistence;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.VideoRenderer;
import teaselib.core.VideoRenderer.Type;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.javacv.VideoRendererJavaCV;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.HostInputMethod;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.FileUtilities;
import teaselib.core.util.PropertyNameMappingPersistence;
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
public class SexScriptsHost implements Host, HostInputMethod.Backend {
    static final Logger logger = LoggerFactory.getLogger(SexScriptsHost.class);

    IScript ss;

    private static final boolean renderBackgroundImage = true;

    final MainFrame mainFrame;
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
        String fieldName = "backgroundImage";
        ImageIcon imageIcon = null;
        try {
            mainFrame = getMainFrame();
            imageIcon = getImageIcon(fieldName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }

        backgroundImageIcon = imageIcon;
        if (imageIcon != null) {
            backgroundImage = imageIcon.getImage();
        } else {
            backgroundImage = null;
        }

        // automatically show popup
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
                    Runnable runnable = onQuitHandler;
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

    private void setImage(byte[] imageBytes) {
        Image image = null;
        if (imageBytes != null) {
            try {
                image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (image != null) {
            if (renderBackgroundImage) {
                setBackgroundImage(image);
            } else {
                setImageAdjustedToMaximizeImageSize(image);
            }
        } else {
            setImageInternal(null);
            setBackgroundImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
        }
    }

    private void setImageAdjustedToMaximizeImageSize(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        boolean portrait = width < height;
        if (portrait) {
            // Enlarge the image with alpha pixels left and right to make
            // the text area a bit smaller
            // Improves readability on wide screen displays, as the text is
            // laid out a bit more portrait (instead of landscape)
            // TODO SS scales down when expanding too much
            BufferedImage expanded = new BufferedImage(height, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = (Graphics2D) expanded.getGraphics();
            g2d.drawImage(image, (expanded.getWidth() - width) / 2, 0, null);
            setImageInternal(expanded);
        } else {
            setImageInternal(image);
        }
    }

    private void setImageInternal(Image image) {
        if (image != null) {
            ((ss.desktop.Script) ss).setImage(image, false);

        } else {
            ss.setImage((byte[]) null, 0);
        }
    }

    private void setBackgroundImage(Image image) {
        try {
            ss.desktop.MainFrame mainFrame = getMainFrame();
            // bounds
            Rectangle bounds = getContentBounds(mainFrame);
            // Spacer to keep text at the right
            BufferedImage spacer = new BufferedImage(bounds.width, 16, BufferedImage.TYPE_INT_ARGB);
            setImageInternal(spacer);
            // actual image
            if (image != null) {
                BufferedImage bi = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = (Graphics2D) bi.getGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                // Draw original background image
                g2d.drawImage(backgroundImage, 0, 0, bounds.width, bounds.height, null);
                // TODO scale image into bi
                int width = image.getWidth(null);
                int height = image.getHeight(null);
                if (height > bounds.height) {
                    width = width * bounds.height / height;
                    height = bounds.height;
                }
                if (width > bounds.width) {
                    height = height * bounds.width / width;
                    width = bounds.width;
                }
                int left = 0;
                int top = (bounds.height - height) / 2;
                g2d.drawImage(image, left, top, width, height, null);
                backgroundImageIcon.setImage(bi);
            } else {
                backgroundImageIcon.setImage(backgroundImage);
            }
            // Update
            mainFrame.repaint();
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static Rectangle getContentBounds(ss.desktop.MainFrame mainFrame) {
        Container contentPane = mainFrame.getContentPane();
        Rectangle bounds = contentPane.getBounds();
        Insets insets = contentPane.getInsets();
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;
        return bounds;
    }

    private ImageIcon getImageIcon(String fieldName) throws NoSuchFieldException, IllegalAccessException {
        // Get image icon
        Class<?> mainFrameClass = mainFrame.getClass();
        // Multiple choices are managed via an array of buttons,
        // whereas a single choice is implemented as a single button
        Field backgroundImageField = mainFrameClass.getDeclaredField(fieldName);
        backgroundImageField.setAccessible(true);
        return (ImageIcon) backgroundImageField.get(mainFrame);
    }

    private ss.desktop.MainFrame getMainFrame() throws NoSuchFieldException, IllegalAccessException {
        Class<?> scriptClass = ss.getClass().getSuperclass();
        Field mainField = scriptClass.getDeclaredField("mainWindow");
        mainField.setAccessible(true);
        return (ss.desktop.MainFrame) mainField.get(ss);
    }

    private void show(String message) {
        ss.show(message);
    }

    @Override
    public void show(byte[] imageBytes, String text) {
        EventQueue.invokeLater(() -> {
            setImage(imageBytes);
            intertitleActive = false;
            show(text);
        });
    }

    @Override
    public void showInterTitle(String text) {
        EventQueue.invokeLater(() -> {
            if (!intertitleActive) {
                Image image = backgroundImageIcon.getImage();
                ss.setImage((byte[]) null, 0);
                Rectangle bounds = getContentBounds(mainFrame);
                bounds.x = 0;
                bounds.y = 0;
                BufferedImage interTitleImage = new BufferedImage(bounds.width, bounds.height,
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = (Graphics2D) interTitleImage.getGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                // Draw original background image
                g2d.drawImage(image, 0, 0, bounds.width, bounds.height, null);
                // Compensate for the text not being centered (causes the text area to be not centered anymore)
                int offset = 0;
                g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f));
                int top = bounds.height / 4 + offset;
                int bottom = bounds.height * 3 / 4 + offset;
                // Render the intertitle top, text and bottom areas
                g2d.fillRect(bounds.x, bounds.y, bounds.width, top);
                g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.80f));
                g2d.fillRect(bounds.x, top, bounds.width, bottom - top);
                g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f));
                g2d.fillRect(bounds.x, bottom, bounds.width, bounds.height - bottom);
                backgroundImageIcon.setImage(interTitleImage);
                mainFrame.repaint();

                intertitleActive = true;
            }
            // Show white text, since the background is black
            ss.show("<p style=\"color:#e0e0e0\">" + text + "</p>");
        });
    }

    @Override
    public void endScene() {
        // Keep the image, remove any text to provide feedback
        show("");
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
        Field comboField = mainFrame.getClass().getDeclaredField("comboBox");
        comboField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final javax.swing.JComboBox<String> ssComboBox = (javax.swing.JComboBox<String>) comboField.get(mainFrame);
        return ssComboBox;
    }

    class ShowPopupTask {
        final FutureTask<Boolean> task;
        final AtomicBoolean resetPopupVisibility = new AtomicBoolean(false);
        static final int PollIntervalMillis = 100;

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
                        Thread.sleep(PollIntervalMillis);
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
        ShowPopupTask showPopupTask = new ShowPopupTask();
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
    public VideoRenderer getDisplay(Type displayType) {
        return new VideoRendererJavaCV(displayType) {
            @Override
            protected Point getPosition(Type type, int width, int height) {
                if (type == VideoRenderer.Type.CameraFeedback) {
                    AffineTransform defaultTransform = mainFrame.getGraphicsConfiguration().getDefaultTransform();
                    try {
                        Point2D size = defaultTransform.inverseTransform( //
                                new Point2D.Double(width, height), new Point2D.Double());
                        int x = mainFrame.getX() + (int) (mainFrame.getWidth() - size.getX()) / 2;
                        int y = mainFrame.getY();
                        Point2D transform = defaultTransform.transform( //
                                new Point2D.Double(x, y), new Point2D.Double());
                        return new Point((int) transform.getX(), (int) transform.getY());
                    } catch (NoninvertibleTransformException e) {
                        logger.error(e.getMessage(), e);
                        return new Point(0, 0);
                    }
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        };
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
