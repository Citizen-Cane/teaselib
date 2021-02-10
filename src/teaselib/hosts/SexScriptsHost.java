package teaselib.hosts;

import java.awt.AlphaComposite;
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;
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
public class SexScriptsHost implements Host, HostInputMethod.Backend {
    static final Logger logger = LoggerFactory.getLogger(SexScriptsHost.class);

    IScript ss;

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

    private void setImage(Image image) {
        if (image != null) {
            setBackgroundImage(image);
        } else {
            setBackgroundImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
        }
    }

    private void setBackgroundImage(Image image) {
        Rectangle bounds = getContentBounds(mainFrame);
        // keep text at the right
        BufferedImage spacer = new BufferedImage(bounds.width, 16, BufferedImage.TYPE_INT_ARGB);
        setImageInternal(spacer);
    }

    private void setImageInternal(Image image) {
        if (image != null) {
            ((ss.desktop.Script) ss).setImage(image, false);
        } else {
            ss.setImage((byte[]) null, 0);
        }
    }

    private void show(BufferedImage image) {
        if (image != null) {
            if (focusLevel < 1.0) {
                float b = 0.20f;
                float m = 0.80f;
                int width = (int) (image.getWidth() * (b + focusLevel * m));
                int height = (int) (image.getHeight() * (b + focusLevel * m));
                BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D resizedg2d = (Graphics2D) resized.getGraphics();
                BufferedImageOp blurOp = getBlurOp(7);
                resizedg2d.drawImage(image, 0, 0, width, height, null);

                BufferedImage blurred = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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

    private BufferedImage renderBackgroundImage(Image image, Rectangle bounds) {
        if (proximity == Proximity.CLOSE) {
            // TODO render tits or boots, depending whether the slave can stand or is kneeling
            boolean isKneeling = false;
            return render(image, isKneeling ? ActorPart.Boots : ActorPart.Torso, bounds);
        } else if (proximity == Proximity.FACE2FACE) {
            // TODO If kneeling, render face at the top of the image
            return render(image, ActorPart.Face, bounds);
        } else {
            return render(image, ActorPart.All, bounds);
        }
    }

    enum ActorPart {
        Face,
        Torso,
        Boots,
        All
    }

    private BufferedImage render(Image image, ActorPart part, Rectangle bounds) {
        BufferedImage bi = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) bi.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        // Draw original background image
        g2d.drawImage(backgroundImage, 0, 0, bounds.width, bounds.height, null);

        float screenWidthFraction = 1.0f;

        int width = (int) (bounds.width * screenWidthFraction);
        int height = bounds.height;
        int left = 0;
        int top = (bounds.height - height) / 2;

        float windowAspect = (float) bounds.width / (float) bounds.height;
        float imageAspect = (float) image.getWidth(null) / (float) image.getHeight(null);

        boolean fillWidth = imageAspect < windowAspect;

        int sx1;
        int columns;
        int rows;
        if (fillWidth) {
            columns = (int) (image.getWidth(null) * 1.0f);
            rows = (int) (image.getWidth(null) * (float) height / width);
            sx1 = 0;
        } else {
            columns = (int) (image.getHeight(null) * ((float) width / (float) height));
            rows = (int) (image.getHeight(null) * 1.0f);
            sx1 = (image.getWidth(null) - columns) / 2;
        }

        int startRow;
        if (fillWidth) {
            // Pan
            if (part == ActorPart.Face) {
                int face = 0;
                startRow = face;
            } else if (part == ActorPart.Boots) {
                // Pan to bottom
                int boots = image.getHeight(null) - rows;
                startRow = boots;
            } else if (part == ActorPart.Torso) {
                int torso = (image.getHeight(null) - rows) / 2;
                startRow = torso;
            } else if (part == ActorPart.All) {
                // TODO show whole image
                // Pan to bottom
                int boots = image.getHeight(null) - rows;
                startRow = boots;
            } else {
                throw new UnsupportedOperationException(part.name());
            }
        } else {
            if (part == ActorPart.Face) {
                // Zoom in face (upper image part)
                float pan = 0.25f;
                float zoom = 1.25f;
                sx1 = sx1 + (int) (sx1 * pan / 2.0f);
                columns = (int) (columns / zoom);
                rows = (int) (rows / zoom);
                startRow = 0;
            } else if (part == ActorPart.Boots) {
                // Zoom to bottom
                startRow = 0;
            } else if (part == ActorPart.Torso) {
                startRow = 0;
            } else if (part == ActorPart.All) {
                // TODO show whole image
                // Zoom to bottom
                startRow = 0;
            } else {
                throw new UnsupportedOperationException(part.name());
            }
        }

        int sy1 = startRow;
        int sx2 = sx1 + columns;
        int sy2 = sy1 + rows;

        g2d.drawImage(image, left, top, width, height, sx1, sy1, sx2, sy2, null);

        // TODO Transform pose.head from s to d -> rewrite rendering with affine transforms
        // if (currentPose != null) {
        // if (currentPose.head.isPresent()) {
        // Point2D head = currentPose.head.get();
        // g2d.setColor(Color.BLUE);
        // int x = (int) (head.getX() * (width - left));
        // int y = (int) (head.getY() * (height - top));
        // int w = width / 8;
        // int h = height / 8;
        // g2d.drawArc(left + x - w, top + y - h, x + w, y + h, 0, 360);
        // }
        // }

        return bi;
    }

    private static BufferedImageOp getBlurOp(int n) {
        return new ConvolveEdgeReflectOp(blurKernel(n));
    }

    private static Kernel blurKernel(int n) {
        int size = n * n;
        float nth = 1.0f / size;
        float[] data = new float[size];
        for (int i = 0; i < size; i++) {
            data[i] = nth;
        }
        return new Kernel(n, n, data);
    }

    public static class ConvolveEdgeReflectOp implements BufferedImageOp, RasterOp {
        private final ConvolveOp convolve;

        public ConvolveEdgeReflectOp(Kernel kernel) {
            this.convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        }

        @Override
        public BufferedImage filter(BufferedImage source, BufferedImage destination) {
            Kernel kernel = convolve.getKernel();
            int borderWidth = kernel.getWidth() / 2;
            int borderHeight = kernel.getHeight() / 2;

            BufferedImage original = addBorder(source, borderWidth, borderHeight);
            return convolve.filter(original, destination) //
                    .getSubimage(borderWidth, borderHeight, source.getWidth(), source.getHeight());
        }

        private static BufferedImage addBorder(BufferedImage image, int borderWidth, int borderHeight) {
            int w = image.getWidth();
            int h = image.getHeight();

            ColorModel cm = image.getColorModel();
            WritableRaster raster = cm.createCompatibleWritableRaster(w + 2 * borderWidth, h + 2 * borderHeight);
            BufferedImage bordered = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
            Graphics2D g = bordered.createGraphics();
            try {
                g.setComposite(AlphaComposite.Src);
                g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
                g.drawImage(image, borderWidth, borderHeight, null);
                g.drawImage(image, borderWidth, 0, borderWidth + w, borderHeight, 0, 0, w, 1, null);
                g.drawImage(image, -w + borderWidth, borderHeight, borderWidth, h + borderHeight, 0, 0, 1, h, null);
                g.drawImage(image, w + borderWidth, borderHeight, 2 * borderWidth + w, h + borderHeight, w - 1, 0, w, h,
                        null);
                g.drawImage(image, borderWidth, borderHeight + h, borderWidth + w, 2 * borderHeight + h, 0, h - 1, w, h,
                        null);
            } finally {
                g.dispose();
            }

            return bordered;
        }

        @Override
        public WritableRaster filter(Raster src, WritableRaster dst) {
            return convolve.filter(src, dst);
        }

        @Override
        public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
            return convolve.createCompatibleDestImage(src, destCM);
        }

        @Override
        public WritableRaster createCompatibleDestRaster(Raster src) {
            return convolve.createCompatibleDestRaster(src);
        }

        @Override
        public Rectangle2D getBounds2D(BufferedImage src) {
            return convolve.getBounds2D(src);
        }

        @Override
        public Rectangle2D getBounds2D(Raster src) {
            return convolve.getBounds2D(src);
        }

        @Override
        public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
            return convolve.getPoint2D(srcPt, dstPt);
        }

        @Override
        public RenderingHints getRenderingHints() {
            return convolve.getRenderingHints();
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

    String currentText = "";
    Image currentImage = null;
    HumanPose.Estimation currentPose = null;
    BufferedImage currentBackgroundImage = null;

    @Override
    public void show(AnnotatedImage actorImage, String text) {
        if (actorImage != null) {
            try {
                currentImage = ImageIO.read(new ByteArrayInputStream(actorImage.bytes));
                currentPose = actorImage.pose;
            } catch (IOException e) {
                currentImage = null;
                currentPose = null;
                logger.error(e.getMessage(), e);
            }
        }

        currentText = text;
        intertitleActive = false;
        setImage(currentImage);
        show();
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

    HumanPose.Proximity proximity = Proximity.FACE2FACE;

    @Override
    public void setUserProximity(HumanPose.Proximity proximity) {
        if (this.proximity != proximity) {
            this.proximity = proximity;
            show();
        }
    }

    @Override
    public void show() {
        Rectangle bounds = getContentBounds(mainFrame);
        if (currentImage != null) {
            currentBackgroundImage = renderBackgroundImage(currentImage, bounds);
        } else {
            currentBackgroundImage = null;
        }

        EventQueue.invokeLater(() -> {
            if (intertitleActive) {
                showInterTitle(currentText);
            } else {
                show(currentBackgroundImage);
                if (currentText == null || currentText.isBlank()) {
                    show("");
                } else {
                    // Border radius does not work - probably a JTextPane issue
                    String html = "<html><head></head>" //
                            + "<body style=\""//
                            + "background-color:rgb(192, 192, 192);"//
                            + "border: 3px solid rgb(192, 192, 192);"//
                            + "border-radius: 5px;" //
                            + "border-top-width: 3px;"//
                            + "border-left-width: 7px;"//
                            + "border-bottom-width: 5px;"//
                            + "border-right-width: 7px;\\\">"//
                            + currentText + "</body>" + "</html>";
                    show(html);
                }
            }
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
