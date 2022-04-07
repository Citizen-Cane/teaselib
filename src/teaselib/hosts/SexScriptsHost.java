package teaselib.hosts;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static teaselib.core.concurrency.NamedExecutorService.singleThreadedQueue;

import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.IllegalComponentStateException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ss.IScript;
import ss.desktop.MainFrame;
import teaselib.core.Audio;
import teaselib.core.Closeable;
import teaselib.core.Host;
import teaselib.core.Persistence;
import teaselib.core.ResourceLoader;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.PersistenceFilter;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.HostInputMethod;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Result;
import teaselib.core.util.CachedPersistenceImpl;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.FileUtilities;
import teaselib.core.util.PropertyNameMappingPersistence;
import teaselib.core.util.QualifiedName;
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
// but dismissing the combobox after showing the list programmatically

/**
 * @author Citizen-Cane
 *
 */
public class SexScriptsHost implements Host, HostInputMethod.Backend, Closeable {

    static final Logger logger = LoggerFactory.getLogger(SexScriptsHost.class);

    private final IScript ss;
    private final Thread mainThread;
    private final int originalDefaultCloseoperation;
    Consumer<ScriptInterruptedEvent> onQuitHandler = null;

    private final MainFrame mainFrame;
    private final ImageIcon backgroundImageIcon;

    private final List<JButton> ssButtons;
    private final JButton ssButton;
    private final JComboBox<String> ssComboBox;
    private final ShowPopupTask showPopupTask;
    private final Set<JComponent> uiComponents = new HashSet<>();
    private Set<String> activeChoices;
    private FutureTask<Prompt.Result> showChoices;

    private final ExecutorService showChoicesThreadPool = singleThreadedQueue("Show-Choices");
    private final InputMethod inputMethod = new HostInputMethod(singleThreadedQueue(getClass().getSimpleName()), this);

    private final Image backgroundImage;

    RenderState previousFrame = new RenderState();
    RenderState newFrame = new RenderState();
    BufferedImageRenderer renderer;

    public static Host from(IScript script) {
        return new SexScriptsHost(script);
    }

    public SexScriptsHost(ss.IScript script) {
        this.ss = script;
        this.mainThread = Thread.currentThread();
        // Should be set by the host, but SexScript doesn't, so we do
        this.mainThread.setName("TeaseScript main thread");

        // Initialize rendering via background image
        ImageIcon imageIcon;
        try {
            mainFrame = getMainFrame();
            imageIcon = getField("backgroundImage");

            ssButtons = Arrays.asList(getField("buttons"));
            ssButton = getField("button");
            ssComboBox = getField("comboBox");
            showPopupTask = new ShowPopupTask(ssComboBox);

            uiComponents.add(ssButton);
            uiComponents.addAll(ssButtons);
            uiComponents.add(ssComboBox);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }

        backgroundImageIcon = imageIcon;
        if (imageIcon != null) {
            backgroundImage = imageIcon.getImage();
        } else {
            backgroundImage = null;
        }

        renderer = new BufferedImageRenderer(backgroundImage);

        this.originalDefaultCloseoperation = mainFrame.getDefaultCloseOperation();
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
            public void windowClosing(WindowEvent event) {
                if (onQuitHandler != null) {
                    try {
                        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                        logger.info("Running quit handler {}", onQuitHandler.getClass().getName());
                        ScriptInterruptedEvent reason = new ScriptInterruptedEvent(
                                ScriptInterruptedEvent.Reason.WindowClosing);
                        onQuitHandler.accept(reason);
                    } finally {
                        onQuitHandler = null;
                    }
                } else {
                    mainFrame.setDefaultCloseOperation(originalDefaultCloseoperation);
                    mainThread.interrupt();
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

        mainFrame.getJMenuBar().setVisible(false);
        setWindowState();

        mainFrame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) { //
            }

            @Override
            public void keyReleased(KeyEvent e) { //
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    toggleFullScreen();
                }
            }
        });
        mainFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    toggleFullScreen();
                } else {
                    super.mouseClicked(e);
                }
            }
        });
    }

    private void toggleFullScreen() {
        // TODO ThreadDeath in main thread -> script ends
        // EventQueue.invokeLater(() -> {
        // try {
        // setFullscreen(!isFullScreen());
        // } catch (ThreadDeath t) {
        // logger.warn(t.getMessage());
        // }
        // });
    }

    Rectangle normalWindowPosition;

    private void setWindowState() {
        normalWindowPosition = mainFrame.getBounds();
        if (isSemiMaximized()) {
            mainFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        boolean isMaximized = (mainFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
        setFullscreen(isMaximized && !isFullScreen());
    }

    private boolean isSemiMaximized() {
        Rectangle screen = mainFrame.getGraphicsConfiguration().getBounds();
        Rectangle bounds = mainFrame.getBounds();
        boolean fullScreenBounds = bounds.x + bounds.width >= screen.width || bounds.y + bounds.height >= screen.height;
        return fullScreenBounds;
    }

    private boolean isFullScreen() {
        return mainFrame.isUndecorated();
    }

    private void setFullscreen(boolean setFullscreen) {
        if (!mainFrame.isUndecorated() && setFullscreen) {
            mainFrame.setVisible(false);
            normalWindowPosition = mainFrame.getBounds();
            mainFrame.dispose();
            try {
                mainFrame.setUndecorated(true);
                mainFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
            } catch (IllegalComponentStateException e) {
                logger.warn(e.getMessage(), e);
            } finally {
                mainFrame.setVisible(true);
            }
        } else if (mainFrame.isUndecorated() && !setFullscreen) {
            mainFrame.setVisible(false);
            mainFrame.dispose();
            mainFrame.setUndecorated(false);
            mainFrame.setExtendedState(0);
            mainFrame.setBounds(normalWindowPosition);
            mainFrame.setVisible(true);
        }
    }

    @Override
    public void setQuitHandler(Consumer<ScriptInterruptedEvent> onQuitHandler) {
        this.onQuitHandler = onQuitHandler;
    }

    @Override
    public void close() {
        mainFrame.setDefaultCloseOperation(originalDefaultCloseoperation);
        EventQueue.invokeLater(() -> {
            setFullscreen(false);
            mainFrame.getJMenuBar().setVisible(true);
        });
    }

    @Override
    public Persistence persistence(Configuration configuration) throws IOException {
        var path = configuration.getScriptSettingsFolder().resolve("common.properties");
        var common = configuration.createGlobalSettingsFile(path);
        var commonState = new CachedPersistenceImpl(common);

        var inventory = new SexScriptsPersistence(ss);
        var mapping = new SexScriptsPropertyNameMapping();
        var inventoryState = new PropertyNameMappingPersistence(inventory, mapping);

        Predicate<QualifiedName> allButInventory = name -> !name.name.endsWith(".Available");
        return new PersistenceFilter(allButInventory, commonState, inventoryState);
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
                // TODO sometimes displays dialog telling "Sleep Interrupted" (when playing mp3 sound)
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

    enum ActorPart {
        Face,
        Torso,
        Boots,
        All
    }

    private Rectangle getContentBounds() {
        Container contentPane = mainFrame.getContentPane();
        Rectangle bounds = contentPane.getBounds();
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

    @Override
    public void showInterTitle(String text) {
        newFrame.text = text;
        newFrame.isIntertitle = true;
    }

    @Override
    public void show(AnnotatedImage displayImage, List<String> text) {
        if (displayImage != null) {
            if (!displayImage.resource.equals(newFrame.displayImageResource)) {
                try {
                    newFrame.displayImage = ImageIO.read(new ByteArrayInputStream(displayImage.bytes));
                    newFrame.pose = displayImage.pose;
                } catch (IOException e) {
                    newFrame.displayImage = null;
                    newFrame.pose = HumanPose.Estimation.NONE;
                    logger.error(e.getMessage(), e);
                }
                newFrame.repaintSceneImage = true;
                newFrame.displayImageResource = displayImage.resource;
            }
        } else if (newFrame.displayImageResource != null) {
            newFrame.displayImageResource = null;
            newFrame.displayImage = null;
            newFrame.pose = HumanPose.Estimation.NONE;
            newFrame.repaintSceneImage = true;
        }

        newFrame.text = text.stream().collect(Collectors.joining("\n"));

        newFrame.isIntertitle = false;
    }

    @Override
    public void setFocusLevel(float focusLevel) {
        newFrame.focusLevel = focusLevel;
    }

    @Override
    public void setActorProximity(HumanPose.Proximity proximity) {
        if (newFrame.actorProximity != proximity) {
            newFrame.actorProximity = proximity;
            newFrame.repaintSceneImage = true;
        }
    }

    @Override
    public synchronized void show() {
        newFrame.updateFrom(previousFrame);

        // Consider insets for pixel-correct (non-scaled) image size
        Rectangle bounds = getContentBounds();
        Insets insets = mainFrame.getInsets();
        int horizontalAdjustment = insets.left + insets.right;

        bounds.width += horizontalAdjustment;
        renderer.renderBackgound(newFrame, bounds);
        bounds.width -= horizontalAdjustment;

        BufferedImage image = renderer.render(newFrame, bounds);
        previousFrame = newFrame;
        newFrame = newFrame.copy();
        EventQueue.invokeLater(() -> showCurrentImage(image));
    }

    private void showCurrentImage(BufferedImage image) {
        if (image != null) {
            backgroundImageIcon.setImage(image);
        } else {
            backgroundImageIcon.setImage(backgroundImage);
        }
        mainFrame.repaint(100);
    }

    @Override
    public void endScene() {
        // Keep the image, remove any text to provide some feedback
        newFrame.text = "";
        show();
    }

    @Override
    public List<Boolean> showItems(String caption, List<String> texts, List<Boolean> values, boolean allowCancel)
            throws InterruptedException {
        List<Boolean> results;
        do {
            results = ss.getBooleans(caption, texts, values);
            // Loop until the user pressed OK
        } while (results == null && !allowCancel);
        return results;
    }

    private List<Runnable> getClickableChoices(List<Choice> choices) {
        // Init all slots
        List<Runnable> clickableChoices = new ArrayList<>(choices.size());
        for (int index : new Interval(0, choices.size() - 1)) {
            clickableChoices.add(index, null);
        }

        // Combobox items
        ComboBoxModel<String> model = ssComboBox.getModel();
        for (int j = 0; j < model.getSize(); j++) {
            int comboboxIndex = j;
            for (int index : new Interval(0, choices.size() - 1)) {
                String text = model.getElementAt(j);
                if (text != null && text.contains(choices.get(index).display)) {
                    clickableChoices.set(index, () -> {
                        enable(Collections.singleton(ssComboBox), true);
                        ssComboBox.setSelectedIndex(comboboxIndex);
                    });
                }
            }
        }

        List<JButton> buttons = new ArrayList<>(ssButtons.size() + 1);
        for (JButton button : ssButtons) {
            buttons.add(button);
        }
        buttons.add(ssButton);

        // Check pretty buttons for corresponding text
        // There are five buttons to display up to five choices,
        // depending on the available window width
        for (JButton button : buttons) {
            for (int index : new Interval(0, choices.size() - 1)) {
                String buttonText = button.getText();
                String choice = Choice.getDisplay(choices.get(index));
                if (buttonText.contains(choice)) {
                    clickableChoices.set(index, () -> {
                        enable(Collections.singleton(button), true);
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
    }

    @Override
    public boolean dismissChoices(List<Choice> choices) {
        var dismissed = false;
        try {
            dismissed = clickAnyButton(choices);
            enable(uiComponents, false);
            activeChoices = null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return dismissed;
    }

    private boolean clickAnyButton(List<Choice> choices) {
        var clickableChoices = getClickableChoices(choices);
        if (clickableChoices != null && !clickableChoices.isEmpty()) {
            var delegate = clickableChoices.get(0);
            if (delegate != null) {
                clickAnyButton(delegate);
                return true;
            }
        }
        return false;
    }

    private void clickAnyButton(Runnable dismiss) {
        FutureTask<Result> choicesToBeDismissed;
        while ((choicesToBeDismissed = showChoices) != null && !choicesToBeDismissed.isDone()) {
            if (showPopupTask.comboBox.isVisible()) {
                showPopupTask.showPopup();
            }

            try {
                dismiss.run();
            } catch (Exception e) {
                throw ExceptionUtil.asRuntimeException(e);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class ShowPopupTask {

        private final AtomicBoolean resetPopupVisibility = new AtomicBoolean(false);
        private final JComboBox<String> comboBox;

        public ShowPopupTask(JComboBox<String> comboBox) {
            this.comboBox = comboBox;
        }

        void call() {
            EventQueue.invokeLater(() -> {
                if (comboBox.isVisible()) {
                    showPopup();
                }
            });
        }

        void showPopup() {
            comboBox.requestFocus();
            comboBox.setPopupVisible(true);
            resetPopupVisibility.set(true);
        }

        void cleanup() {
            EventQueue.invokeLater(() -> {
                if (resetPopupVisibility.get() && comboBox.isPopupVisible()) {
                    comboBox.setPopupVisible(false);
                }
                comboBox.removeAllItems();
            });
        }

    }

    @Override
    public InputMethod inputMethod() {
        return inputMethod;
    }

    @Override
    public void setup() {
    }

    @Override
    public Prompt.Result reply(Choices choices) throws InterruptedException {
        this.activeChoices = choices.stream().map(Choice::getDisplay).collect(toSet());
        // open the combo box pop-up if necessary in order to
        // allow the user to read prompts without mouse/touch interaction
        showChoices = new FutureTask<>(() -> new Prompt.Result(ss.getSelectedValue(null, choices.toDisplay())));
        Prompt.Result result;
        showChoicesThreadPool.execute(showChoices);

        try {
            result = showChoices.get();
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(e);
        } finally {
            dismissChoices(choices);
            boolean cleanuoPopup = choices.size() > 1;
            if (cleanuoPopup) {
                showPopupTask.cleanup();
            }
            showChoices = null;
            activeChoices = null;
        }
        return result;
    }

    @Override
    public void updateUI(InputMethod.UiEvent event) {
        Set<JComponent> activeComponents;
        try {
            activeComponents = awaitRealizedUI();
        } catch (InterruptedException bailout) {
            Thread.currentThread().interrupt();
            return;
        }

        var unusedComponents = uiComponents.stream().filter(not(activeComponents::contains)).collect(toSet());
        enable(unusedComponents, false);
        enable(activeComponents, event.enabled);
        boolean popupCombobox = activeComponents.contains(ssComboBox) && ssComboBox.isVisible();
        if (popupCombobox && event.enabled) {
            showPopupTask.call();
        }
    }

    private Set<JComponent> awaitRealizedUI() throws InterruptedException {
        Set<JComponent> activeComponents;
        while ((activeComponents = activeComponents()).isEmpty()) {
            Thread.sleep(100);
        }
        return activeComponents;
    }

    private Set<JComponent> activeComponents() {
        if (activeChoices == null) {
            return Collections.emptySet();
        }
        Set<JComponent> components = new HashSet<>();

        ComboBoxModel<String> model = ssComboBox.getModel();
        for (int j = 0; j < model.getSize(); j++) {
            String text = model.getElementAt(j);
            if (activeChoices.contains(text)) {
                components.add(ssComboBox);
                break;
            }
        }

        for (JButton button : ssButtons) {
            if (activeChoices.contains(button.getText())) {
                components.add(button);
            }
        }

        if (activeChoices.contains(ssButton.getText())) {
            components.add(ssButton);
        }

        return components;
    }

    private static void enable(Set<? extends JComponent> elements, boolean enabled) {
        elements.stream().forEach(c -> c.setVisible(enabled));
    }

    @Override
    public File getLocation(Location folder) {
        if (folder == Location.Host)
            return FileUtilities.currentDir();
        else if (folder == Location.TeaseLib)
            return ResourceLoader.getProjectPath(getClass()).getParentFile().getAbsoluteFile();
        else if (folder == Location.User)
            return new File(getLocation(Location.Host).getAbsoluteFile(), "settings");
        else if (folder == Location.Log)
            return getLocation(Location.Host);
        else
            throw new IllegalArgumentException(Objects.toString(folder));
    }

}
