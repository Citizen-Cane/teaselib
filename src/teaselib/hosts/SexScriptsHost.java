package teaselib.hosts;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
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
import teaselib.TeaseLib;
import teaselib.core.Host;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.events.Delegate;
import teaselib.util.Interval;

/**
 * SexScripts renderer: Workarounds some of SS shortcomings (or it's just my
 * taste :^) TODO sounds are not playable from stream in SS - workarounded by
 * playing sounds myself - same for prerendered speech TODO UI layout wastes a
 * lot of screen estate, sub optimal for portrait images TODO TExt pane is noit
 * fixed, I liked it better the way CM did it - workarounded by adding pixels
 * left and right of portrait images TODO Invisible progress bar eats screen
 * estate TODO Buttons should be layouted vertically, better for longer button
 * texts
 * 
 * @author someone
 * 
 */
public class SexScriptsHost implements Host {
    private static final Logger logger = LoggerFactory
            .getLogger(SexScriptsHost.class);

    private IScript ss;

    private static final boolean renderBackgroundImage = true;

    MainFrame mainFrame = null;
    private final ImageIcon backgroundImageIcon;
    private final Image backgroundImage;

    private final ExecutorService showPopupThreadPool = NamedExecutorService
            .newFixedThreadPool(1, "Show-Choices", 1, TimeUnit.SECONDS);

    private final ExecutorService showChoicesThreadPool = NamedExecutorService
            .newFixedThreadPool(1, "Show-Popup", 1, TimeUnit.HOURS);

    private Runnable onQuitHandler = null;

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
        } catch (NoSuchFieldException e) {
            logger.error(e.getMessage(), e);
            ss.showPopup("Field " + fieldName + " not found");
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
            ss.showPopup("Field " + fieldName + " not accessible");
        }
        backgroundImageIcon = imageIcon;
        if (imageIcon != null) {
            backgroundImage = imageIcon.getImage();
        } else {
            backgroundImage = null;
        }
        // automatically show popup
        final int originalDefaultCloseoperation = mainFrame
                .getDefaultCloseOperation();
        mainFrame.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (onQuitHandler != null) {
                    mainFrame.setDefaultCloseOperation(
                            WindowConstants.DO_NOTHING_ON_CLOSE);
                    Runnable runnable = onQuitHandler;
                    // Execute each quit handler just once
                    onQuitHandler = null;
                    logger.info("Running quit handler "
                            + runnable.getClass().getName());
                    runnable.run();
                } else {
                    mainFrame.setDefaultCloseOperation(
                            originalDefaultCloseoperation);
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }
        });
    }

    @Override
    public void playSound(ResourceLoader resources, String path)
            throws IOException, InterruptedException {
        ss.playSound(resources.unpackToFile(path).getAbsolutePath());
    }

    @Override
    public Object playBackgroundSound(ResourceLoader resources, String path)
            throws IOException {
        ss.playBackgroundSound(resources.unpackToFile(path).getAbsolutePath());
        return path;
    }

    @Override
    // TODO Just stop the sound denoted by the handle
    public void stopSound(Object handle) {
        try {
            // Just stop all sounds for now
            ss.stopSoundThreads();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
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
            setBackgroundImage(
                    new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
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
            BufferedImage expanded = new BufferedImage(height, height,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = (Graphics2D) expanded.getGraphics();
            // g2d.drawRect(0, 0, expanded.getWidth() - 1,
            // expanded.getHeight() -1);
            g2d.drawImage(image, (expanded.getWidth() - width) / 2, 0, null);
            setImageInternal(expanded);
        } else {
            setImageInternal(image);
        }
    }

    private void setImageInternal(Image image)
            throws ScriptInterruptedException {
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
            BufferedImage spacer = new BufferedImage(bounds.width, 16,
                    BufferedImage.TYPE_INT_ARGB);
            setImageInternal(spacer);
            // actual image
            if (image != null) {
                BufferedImage bi = new BufferedImage(bounds.width,
                        bounds.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = (Graphics2D) bi.getGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                // Draw original background image
                g2d.drawImage(backgroundImage, 0, 0, bounds.width,
                        bounds.height, null);
                // todo scale image into bi
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
        } catch (NoSuchFieldException e) {
            logger.error(e.getMessage(), e);
        } catch (SecurityException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
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

    private ImageIcon getImageIcon(String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        // Get image icon
        Class<?> mainFrameClass = mainFrame.getClass();
        // Multiple choices are managed via an array of buttons,
        // whereas a single choice is implemented as a single button
        Field backgroundImageField = mainFrameClass.getDeclaredField(fieldName);
        backgroundImageField.setAccessible(true);
        return (ImageIcon) backgroundImageField.get(mainFrame);
    }

    private ss.desktop.MainFrame getMainFrame()
            throws NoSuchFieldException, IllegalAccessException {
        Class<?> scriptClass = ss.getClass().getSuperclass();
        Field mainField = scriptClass.getDeclaredField("mainWindow");
        mainField.setAccessible(true);
        return (ss.desktop.MainFrame) mainField.get(ss);
    }

    private void show(String message) {
        ss.show(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.Host#show(java.awt.Image, java.lang.String)
     */
    @Override
    public void show(byte[] imageBytes, String text) {
        // TODO
        // Set image and text at once to overcome layout glitches
        // (mostly the delay between displaying the new image
        // and then displaying the text)
        setImage(imageBytes);
        show(text);
    }

    @Override
    public void showInterTitle(String text) {
        // TODO Auto-generated method stub
        try {
            Image image = backgroundImageIcon.getImage();
            ss.setImage((byte[]) null, 0);
            MainFrame mainFrame = getMainFrame();
            Rectangle bounds = new Rectangle(0, 0, image.getWidth(null),
                    image.getHeight(null));// getContentBounds(mainFrame);
            BufferedImage bi = new BufferedImage(bounds.width, bounds.height,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = (Graphics2D) bi.getGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            // Draw original background image
            g2d.drawImage(image, 0, 0, bounds.width, bounds.height, null);
            int offset = -bounds.height / 10;
            g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f));
            int top = bounds.height / 4 + offset;
            int bottom = bounds.height * 3 / 4 + offset;
            g2d.fillRect(bounds.x, bounds.y, bounds.width, top);
            g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.80f));
            g2d.fillRect(bounds.x, top, bounds.width, bottom - top);
            g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f));
            g2d.fillRect(bounds.x, bottom, bounds.width,
                    bounds.height - bottom);
            backgroundImageIcon.setImage(bi);
            mainFrame.repaint();
            ss.show("<p style=\"color:#e0e0e0\">" + text + "</p>");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Boolean> showCheckboxes(String caption, List<String> texts,
            List<Boolean> values, boolean allowCancel)
            throws ScriptInterruptedException {
        List<Boolean> results;
        do {
            try {
                results = ss.getBooleans(caption, texts, values);
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
            // Loop until the user pressed OK -> != null
        } while (results == null && !allowCancel);
        return results;
    }

    @Override
    public List<Delegate> getClickableChoices(List<String> choices) {
        try {
            // Get buttons
            Class<?> mainFrameClass = mainFrame.getClass();
            List<Delegate> clickableChoices = new ArrayList<Delegate>(
                    choices.size());
            // Multiple choices are managed via an array of buttons,
            // whereas a single choice is implemented as a single button
            Field buttonsField = mainFrameClass.getDeclaredField("buttons");
            buttonsField.setAccessible(true);
            javax.swing.JButton[] ssButtons = (javax.swing.JButton[]) buttonsField
                    .get(mainFrame);
            // Must also test the single button
            Field buttonField = mainFrameClass.getDeclaredField("button");
            buttonField.setAccessible(true);
            final javax.swing.JButton ssButton = (javax.swing.JButton) buttonField
                    .get(mainFrame);
            List<javax.swing.JButton> buttons = new ArrayList<javax.swing.JButton>(
                    ssButtons.length + 1);
            for (javax.swing.JButton button : ssButtons) {
                buttons.add(button);
            }
            // TODO Only for ss timed button?
            buttons.add(ssButton);
            // Combobox
            final javax.swing.JComboBox<String> ssComboBox = getComboBox();
            // Init all slots
            for (int index : new Interval(choices)) {
                clickableChoices.add(index, null);
            }
            // Combobox
            final ComboBoxModel<String> model = ssComboBox.getModel();
            for (int j = 0; j < model.getSize(); j++) {
                final int comboboxIndex = j;
                for (final int index : new Interval(choices)) {
                    final String text = model.getElementAt(j);
                    if (text.contains(choices.get(index))) {
                        Delegate click = new Delegate() {
                            @Override
                            public void run() {
                                // Selects but doesn't execute
                                ssComboBox.setSelectedIndex(comboboxIndex);
                            }
                        };
                        clickableChoices.set(index, click);
                    }
                }
            }
            // Check pretty buttons for corresponding text
            // There might be more buttons than expected,
            // probably some kind of caching
            for (final javax.swing.JButton button : buttons) {
                for (int index : new Interval(choices)) {
                    String buttonText = button.getText();
                    final String choice = choices.get(index);
                    if (buttonText.contains(choice)) {
                        Delegate click = new Delegate() {

                            @Override
                            public void run() {
                                logger.info("Clicking on " + choice);
                                button.doClick();
                            }
                        };
                        clickableChoices.set(index, click);
                    }
                }
            }
            // Assign clicks to combo box
            // If there are too many choices, a combo box will be used

            // If a choice wasn't found, the element at the corresponding index
            // would be null
            return clickableChoices;
        } catch (

        IllegalAccessException e)

        {
            logger.error(e.getMessage(), e);
        } catch (

        NoSuchFieldException e)

        {
            logger.error(e.getMessage(), e);
        } catch (

        SecurityException e)

        {
            logger.error(e.getMessage(), e);
        }
        return new ArrayList<Delegate>();

    }

    @Override
    public boolean dismissChoices(List<String> choices) {
        // Just click any choice
        final List<Delegate> clickableChoices = getClickableChoices(choices);
        if (clickableChoices != null) {
            final Delegate delegate = clickableChoices.get(0);
            if (delegate != null) {
                delegate.run();
                return true;
            }
        }
        return false;
        // TODO Doesn't make showChoices to return null, but good enough for now
    }

    public javax.swing.JComboBox<String> getComboBox()
            throws NoSuchFieldException, IllegalAccessException {
        Field comboField = mainFrame.getClass().getDeclaredField("comboBox");
        comboField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final javax.swing.JComboBox<String> ssComboBox = (javax.swing.JComboBox<String>) comboField
                .get(mainFrame);
        return ssComboBox;
    }

    class ShowPopupTask {
        final FutureTask<Boolean> task;
        final AtomicBoolean resetPopupVisibility = new AtomicBoolean(false);
        final static int PollIntervalMillis = 100;

        JComboBox<String> comboBox = null;

        public ShowPopupTask() {
            try {
                comboBox = getComboBox();
            } catch (NoSuchFieldException e) {
                logger.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                logger.error(e.getMessage(), e);
            }
            task = new FutureTask<Boolean>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    // Poll a little
                    for (int i = 1; i < 10; i++) {
                        if (comboBox.isVisible() /* && comboBox.hasFocus() */) {
                            // Work-around the issue that the pop-up cannot be
                            // hidden
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
            comboBox.requestFocus();
            comboBox.setPopupVisible(true);
            resetPopupVisibility.set(true);
        }

        void cleanup() {
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (resetPopupVisibility.get())
                        if (comboBox.isPopupVisible()) {
                            comboBox.setPopupVisible(false);
                        }
                }
            });
        }
    }

    @Override
    public int reply(final List<String> choices)
            throws ScriptInterruptedException {
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
        FutureTask<Integer> showChoices = new FutureTask<Integer>(
                new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return ss.getSelectedValue(null, choices);
                    }
                });
        int result;
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
            List<Delegate> clickableChoices = getClickableChoices(choices);
            if (!clickableChoices.isEmpty()) {
                // Click any button
                Delegate delegate = clickableChoices.get(0);
                while (!showChoices.isDone()) {
                    // Stupid trick to be able to actually click a combo item
                    if (showPopupTask.comboBox.isVisible()) {
                        showPopupTask.showPopup();
                    }
                    delegate.run();
                    TeaseLib.instance().sleep(100, TimeUnit.MILLISECONDS);
                }
            }
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result = -1;
        } finally {
            showPopupTask.cleanup();
        }
        return result;
    }

    @Override
    public void setQuitHandler(Runnable onQuitHandler) {
        this.onQuitHandler = onQuitHandler;
    }
}
