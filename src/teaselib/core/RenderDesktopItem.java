package teaselib.core;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import teaselib.TeaseLib;

/**
 * Image is a default command. As such, every action has one, but since it
 * doesn't have a state, so we can use the same instance for all actions.
 * 
 * @author someone
 * 
 */
public class RenderDesktopItem implements MediaRenderer {

    private final URI uri;

    private final TeaseLib teaseLib;

    public RenderDesktopItem(URI uri, TeaseLib teaseLib) {
        this.uri = uri;
        this.teaseLib = teaseLib;
    }

    @Override
    public void render() throws IOException {
        // TODO Perform in separate task to avoid delay
        // TODO Determine if directory or file in resource archive
        // TODO unpack folder containing the file
        // TODO execute in unpacked folder
        teaseLib.transcript.info("Desktop Item = " + uri.toString());
        String absolutePath = new File(uri).getPath();
        Desktop.getDesktop().open(new File(absolutePath));
    }

    @Override
    public String toString() {
        return uri.toString();
    }
}
