package teaselib.core.jni;

import java.io.File;

import teaselib.core.ResourceLoader;
import teaselib.core.util.Environment;
import teaselib.core.util.Environment.Arch;

public class LibraryLoader {

    private LibraryLoader() { //
    }

    public static void load(String name) {
        String extension;
        Environment system = Environment.SYSTEM;
        if (system == Environment.Windows) {
            extension = "dll";
        } else {
            throw new UnsupportedOperationException("LoadLibrary() " + system + " not supported yet");
        }
        final String architecture;
        Arch arch = Environment.ARCH;
        if (arch == Environment.Arch.x86) {
            architecture = "x86";
        } else if (arch == Environment.Arch.x64) {
            architecture = "x64";
        } else {
            throw new UnsupportedOperationException(
                    "LoadLibrary() processor architecture " + arch + " not supported yet");
        }
        File releaseLocation = ResourceLoader.getProjectPath(LibraryLoader.class);
        try {
            System.load(getAbsolutePath(releaseLocation, name, architecture, extension));
        } catch (UnsatisfiedLinkError e) {
            try {
                File devLocation = new File(releaseLocation.getParentFile(), "lib");
                System.load(getAbsolutePath(devLocation, name, architecture, extension));
            } catch (UnsatisfiedLinkError e1) {
                throw e;
            }
        }
    }

    private static String getAbsolutePath(File folder, String name, final String architecture, String extension) {
        return new File(folder, name + "_" + architecture + "." + extension).getAbsolutePath();
    }

}
