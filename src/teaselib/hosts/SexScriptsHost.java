package teaselib.hosts;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import ss.IScript;
import ss.desktop.MainFrame;
import teaselib.Host;
import teaselib.ResourceLoader;
import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;
import teaselib.util.Delegate;
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

    private IScript ss;

    private BufferedWriter log = null;
    private final File path = new File("./TeaseLib.log");

    private static final boolean renderBackgroundImage = true;

    private final ImageIcon backgroundImageIcon;
    private final Image backgroundImage;

    public SexScriptsHost(ss.IScript script) {
        this.ss = script;
        String fieldName = "backgroundImage";
        ImageIcon imageIcon = null;
        try {
            MainFrame mainFrame = getMainFrame();
            imageIcon = getImageIcon(mainFrame, fieldName);
        } catch (NoSuchFieldException e) {
            TeaseLib.log(this, e);
            ss.showPopup("Field " + fieldName + " not found");
        } catch (IllegalAccessException e) {
            TeaseLib.log(this, e);
            ss.showPopup("Field " + fieldName + " not accessible");
        }
        backgroundImageIcon = imageIcon;
        if (imageIcon != null) {
            backgroundImage = imageIcon.getImage();
        } else {
            backgroundImage = null;
        }
    }

    @Override
    public long getTime() {
        return ss.getTime();
    }

    @Override
    public int getRandom(int min, int max) {
        return min + ss.getRandom(max - min + 1);
    }

    @Override
    public void log(String line) {
        if (log == null) {
            try {
                log = new BufferedWriter(new FileWriter(path));
            } catch (IOException e) {
                ss.showPopup("Cannot open log file " + path.getAbsolutePath());
            }
        }
        try {
            log.write(line + "\n");
            log.flush();
        } catch (IOException e) {
            ss.showPopup("Cannot write log file " + path.getAbsolutePath());
        }
        System.out.println(line);
    }

    @Override
    public void playSound(ResourceLoader resources, String path)
            throws ScriptInterruptedException, IOException {
        File file = cacheResource(resources, "sounds/", path);
        try {
            ss.playSound(file.getAbsolutePath());
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }

    @Override
    public Object playBackgroundSound(ResourceLoader resources, String path)
            throws IOException {
        File file = cacheResource(resources, "sounds/", path);
        ss.playBackgroundSound(file.getAbsolutePath());
        return path;
    }

    private static File cacheResource(ResourceLoader resources,
            String cacheRootFolder, String path) throws IOException {
        InputStream resource = null;
        File cached = null;
        try {
            String cachedSound = trimCacheFilePath(resources, cacheRootFolder,
                    path);
            resource = resources.getResource(path);
            cached = new File(cacheRootFolder + cachedSound);
            if (!cached.exists()) {
                cached.getParentFile().mkdirs();
                Files.copy(resource, Paths.get(cached.toURI()));
            }
        } finally {
            if (resource != null) {
                resource.close();
            }
        }
        return cached;
    }

    private static String trimCacheFilePath(ResourceLoader resources,
            String cacheRootFolder, String path) {
        String trimmedPath = path;
        if (trimmedPath.toLowerCase().startsWith(cacheRootFolder)) {
            trimmedPath = trimmedPath.substring(cacheRootFolder.length());
        }
        trimmedPath = resources.assetRoot + path;
        return trimmedPath;
    }

    @Override
    public void stopSounds() {
        try {
            ((ss.desktop.Script) ss).stopSoundThreads();
        } catch (Exception e) {
            TeaseLib.log(this, e);
        }
    }

    @Override
    public void stopSound(Object handle) {
        // Just stop all sounds for now
        try {
            ((ss.desktop.Script) ss).stopSoundThreads();
        } catch (Exception e) {
            TeaseLib.log(this, e);
        }
    }

    private void setImage(Image image) {
        if (image != null) {
            if (renderBackgroundImage) {
                setBackgroundImage(image);
            } else {
                setImageAdjustedToMaximizeImageSize(image);
            }
        } else {
            setImageInternal(null);
            setBackgroundImage(new BufferedImage(1, 1,
                    BufferedImage.TYPE_INT_ARGB));
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
            try {
                ss.setImage(null);
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
    }

    private void setBackgroundImage(Image image) {
        try {
            ss.desktop.MainFrame mainFrame = getMainFrame();
            // bounds
            Container contentPane = mainFrame.getContentPane();
            Rectangle bounds = contentPane.getBounds();
            Insets insets = contentPane.getInsets();
            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= insets.left + insets.right;
            bounds.height -= insets.top + insets.bottom;
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
            TeaseLib.log(this, e);
        } catch (SecurityException e) {
            TeaseLib.log(this, e);
        } catch (IllegalArgumentException e) {
            TeaseLib.log(this, e);
        } catch (IllegalAccessException e) {
            TeaseLib.log(this, e);
        }
    }

    private static ImageIcon getImageIcon(ss.desktop.MainFrame mainFrame,
            String fieldName) throws NoSuchFieldException,
            IllegalAccessException {
        // Get image icon
        Class<?> mainFrameClass = mainFrame.getClass();
        // Multiple choices are managed via an array of buttons,
        // whereas a single choice is implemented as a single button
        Field backgroundImageField = mainFrameClass.getDeclaredField(fieldName);
        backgroundImageField.setAccessible(true);
        return (ImageIcon) backgroundImageField.get(mainFrame);
    }

    private ss.desktop.MainFrame getMainFrame() throws NoSuchFieldException,
            IllegalAccessException {
        Class<?> scriptClass = ss.getClass().getSuperclass();
        Field mainField = scriptClass.getDeclaredField("mainWindow");
        mainField.setAccessible(true);
        ss.desktop.MainFrame mainFrame = (ss.desktop.MainFrame) mainField
                .get(ss);
        return mainFrame;
    }

    private void show(String message) {
        ss.show(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.Host#show(java.awt.Image, java.lang.String)
     */
    // TODO Set at once to overcome layout glitches (mostly the delay between
    // displaying the new image and then displaying the text
    @Override
    public void show(Image image, String text) {
        setImage(image);
        show(text);
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
    public void sleep(long milliseconds) {
        try {
            synchronized (this) {
                wait(milliseconds);
            }
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }

    @Override
    public List<Delegate> getClickableChoices(List<String> choices) {
        try {
            // Get main frame
            Class<?> scriptClass = ss.getClass().getSuperclass();
            Field mainField = scriptClass.getDeclaredField("mainWindow");
            mainField.setAccessible(true);
            ss.desktop.MainFrame mainFrame = (ss.desktop.MainFrame) mainField
                    .get(ss);
            // Get buttons
            Class<?> mainFrameClass = mainFrame.getClass();
            List<Delegate> result = new ArrayList<Delegate>(choices.size());
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
            // TODO Only for timeout button?
            buttons.add(ssButton);
            // Check pretty buttons for corresponding text
            for (int index : new Interval(choices)) {
                result.add(index, null);
            }
            // There might be more buttons than expected, probably some kind of
            // caching
            for (final javax.swing.JButton button : buttons) {
                for (int index : new Interval(choices)) {
                    String buttonText = button.getText();
                    if (buttonText.contains(choices.get(index))) {
                        Delegate click = new Delegate() {
                            @Override
                            public void run() {
                                button.doClick();
                            }
                        };
                        result.set(index, click);
                    }
                }
            }
            // If a choice wasn't found, the element at the corresponding index
            // would be null
            return result;
        } catch (IllegalAccessException e) {
            TeaseLib.log(this, e);
        } catch (NoSuchFieldException e) {
            TeaseLib.log(this, e);
        } catch (SecurityException e) {
            TeaseLib.log(this, e);
        }
        return new ArrayList<Delegate>();
    }

    @Override
    public int reply(List<String> choices) throws ScriptInterruptedException {
        try {
            return ss.getSelectedValue(null, choices);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }
}
