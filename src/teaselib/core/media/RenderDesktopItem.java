package teaselib.core.media;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import teaselib.TeaseLib;

/**
 * Image is a default command. As such, every action has one, but since it
 * doesn't have a state, so we can use the same instance for all actions.
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
    public void render() throws IOException {
        // TODO Perform in separate task to avoid delay
        // TODO Determine if directory or file in resource archive
        // TODO unpack folder containing the file
        // TODO execute in unpacked folder
        teaseLib.transcript.info("Desktop Item = " + file.getAbsolutePath());
        Desktop.getDesktop().open(file);
    }

    @Override
    public String toString() {
        return file.getAbsolutePath();
    }
}
