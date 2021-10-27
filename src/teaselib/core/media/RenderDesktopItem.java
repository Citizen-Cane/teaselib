package teaselib.core.media;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.util.ExceptionUtil;

/**
 * 
 * @author Citizen-Cane
 * 
 */
public class RenderDesktopItem extends MediaRendererThread {
    private final File file;

    public RenderDesktopItem(TeaseLib teaseLib, ResourceLoader resources, String resource) throws IOException {
        super(teaseLib);
        this.file = resources.unpackEnclosingFolder(resource);
    }

    @Override
    public void renderMedia() {
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
