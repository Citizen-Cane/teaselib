package teaselib.core.media;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import teaselib.core.TeaseLib;
import teaselib.core.util.ExceptionUtil;

/**
 * Image is a default command. As such, every action has one, but since it doesn't have a state, so we can use the same
 * instance for all actions.
 * 
 * @author someone
 * 
 */
public class RenderDesktopItem implements MediaRenderer {

    private final File file;
    private final TeaseLib teaseLib;

    public RenderDesktopItem(File file, TeaseLib teaseLib) {
        this.file = file;
        this.teaseLib = teaseLib;
    }

    @Override
    public void run() {
        // TODO Perform in separate task to avoid delay
        // TODO Determine if directory or file in resource archive
        // TODO unpack folder containing the file
        // TODO execute in unpacked folder
        teaseLib.transcript.info("Desktop Item = " + file.getAbsolutePath());
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return file.getAbsolutePath();
    }
}
